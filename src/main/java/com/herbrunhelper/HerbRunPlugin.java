package com.herbrunhelper;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.TileObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.EnumComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.EnumID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDependency(BankTagsPlugin.class)
@PluginDescriptor(
	name = "Farm Run Optimizer",
	description = "Plans every farm run (herbs, allotments, trees, fruit trees, hardwood, bushes, hops, cactus, calquat, spirit trees, celastrus, redwood): what to bring, route order, what to do at each patch, and every growth timer",
	tags = {"farming", "farm run", "herb run", "tree run", "fruit tree", "hardwood", "herbs", "patches", "timers", "fairy ring", "route", "planner"}
)
public class HerbRunPlugin extends Plugin
{
	/** Rune pouch slots (the divine pouch uses the same six, with more capacity in game). */
	private static final int[] RUNE_POUCH_TYPE_VARBITS = {
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6
	};
	private static final int[] RUNE_POUCH_QTY_VARBITS = {
		VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6
	};

	private static final int QUEST_CHECK_INTERVAL_TICKS = 100;
	private static final int PATCH_SYNC_INTERVAL_TICKS = 2;
	private static final String TIMERS_KEY = "timers";
	private static final String CLOCK_KEY = "growthClock";
	private static final String TIME_TRACKING_GROUP = "timetracking";
	private static final String[] TELEPORT_QUESTS = {"Plague City", "Eadgar's Ruse", "Client of Kourend", "Twilight's Promise"};
	private static final String FAIRYTALE_II = "Fairytale II - Cure a Queen";

	@Inject
	private Client client;

	@Inject
	private HerbRunConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private Notifier notifier;

	@Inject
	private HerbRunBankOverlay bankOverlay;

	@Inject
	private PatchNavigationOverlay navigationOverlay;

	@Inject
	private HerbRunInventoryOverlay inventoryOverlay;

	@Inject
	private RunReadyOverlay readyOverlay;

	@Inject
	private LeadHintOverlay leadHintOverlay;

	@Inject
	private SpellHighlightOverlay spellHighlightOverlay;

	@Inject
	private MinimapArrowOverlay minimapArrowOverlay;

	@Inject
	private StepOverlay stepOverlay;

	@Inject
	private TagManager tagManager;

	@Inject
	private FarmBankTag farmBankTag;

	@Inject
	private FarmBankButton farmBankButton;

	@Inject
	private FarmBankLayout farmBankLayout;

	@Inject
	private FarmBankViewOverlay farmBankViewOverlay;

	@Inject
	private FarmBankButtonOverlay farmBankButtonOverlay;

	@Inject
	private net.runelite.client.game.SpriteManager spriteManager;
	@Inject
	private net.runelite.client.plugins.PluginManager pluginManager;
	/** Told you about the Shortest Path plugin this session (in chat). */
	private boolean toldAboutShortestPath;

	private static final String HISTORY_KEY = "runHistory";
	private static final String CURRENT_RUN_KEY = "currentRun";
	private final RunHistory history = new RunHistory();
	private volatile FarmingContract contract;
	private final Map<FarmPatch, PatchTimers.Status> lastStatus = new EnumMap<>(FarmPatch.class);
	private boolean statusesPrimed;
	private int lastBankTick = -10;
	private int lastFarmingXp;
	private final Map<Crop, Integer> herbProfit = new EnumMap<>(Crop.class);
	private volatile List<RunReadyOverlay.Line> readyLines = Collections.emptyList();

	private HerbRunPanel panel;
	private NavigationButton navButton;

	// name -> qty snapshots and name -> item id, refreshed whenever a container changes
	private final Map<String, Integer> bankItems = new HashMap<>();
	private final Map<String, Integer> inventoryItems = new HashMap<>();
	private final Map<String, Integer> equipmentItems = new HashMap<>();
	private final Map<String, Integer> bankNameToId = new HashMap<>();
	private final Map<String, Integer> inventoryNameToId = new HashMap<>();
	private final Map<String, Integer> equipmentNameToId = new HashMap<>();
	private boolean bankSeen;
	/** Runes inside the rune pouch, by name, read from its varbits. */
	private final Map<String, Integer> runePouchItems = new HashMap<>();
	private boolean runePouchCarried;
	private String runePouchName = "Rune pouch";
	private int runePouchCapacity = 3;
	/** Seeds and saplings in the Farming Guild's seed vault, once it has been opened. */
	private final Map<String, Integer> seedVaultItems = new HashMap<>();
	/** Tools, compost and plant cures stored at the tool leprechaun. */
	private final Map<String, Integer> leprechaunItems = new HashMap<>();

	private final Set<String> completedQuests = new HashSet<>();
	private boolean fairyRingsUnlocked;
	private int ticksSinceQuestCheck = QUEST_CHECK_INTERVAL_TICKS;
	private int ticksSincePatchSync;
	private boolean dirty = true;
	private boolean timersLoaded;

	private final PatchTimers timers = new PatchTimers();
	private final GrowthClock clock = new GrowthClock();
	private final Map<FarmPatch, Integer> lastSeenValue = new EnumMap<>(FarmPatch.class);
	private final Set<FarmPatch> notifiedReady = EnumSet.noneOf(FarmPatch.class);

	/** Farming patch objects in the loaded area, with the varbit each one shows. */
	private final Map<GameObject, Integer> patchObjects = new HashMap<>();
	/** Exact patch tiles learned from the game (saved, so the arrow/path are exact next time). */
	private final Map<FarmPatch, WorldPoint> learnedTiles = new EnumMap<>(FarmPatch.class);
	private static final String TILES_KEY = "patchTiles";
	/** The game's farming patch varbits (FARMING_TRANSMIT_A..N). */
	private static final Set<Integer> FARMING_VARBITS = new HashSet<>(java.util.Arrays.asList(
		4771, 4772, 4773, 4774, 4775, 7904, 7905, 7906, 7907, 7908, 7909, 7910, 7911, 7912));
	private volatile Set<Integer> highlightIds = Collections.emptySet();
	private volatile FarmPatch nextPatch;
	private WorldPoint shortestPathTarget;
	/** Patch size when the target is a patch centre: Shortest Path gets the tiles around it. */
	private int shortestPathPatchSize;
	/** Send Shortest Path the same target again (after a teleport or a settings change). */
	private boolean shortestPathResend;
	private WorldPoint lastPlayerTile;
	/** Where to go right now: the patch, or the fairy ring to use first. */
	private volatile WorldPoint navTarget;
	private volatile String navRingLabel;
	/** Gardeners (NPCs with a "Pay" option) currently loaded. */
	private final Set<NPC> gardeners = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private volatile FarmPatch payPatch;
	private volatile String payLabel;
	/** Fairy ring objects currently loaded, to outline the real ring. */
	private final Set<TileObject> fairyRingObjects = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private final Set<TileObject> spiritTreeObjects = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/** What to click first (fairy ring or spirit tree) and what to pick there. */
	private volatile TileObject navLeadObject;
	private volatile String navLeadHint;
	/** A walkable tile beside each tree patch (where the gardener stands), for the walking path. */
	private final Map<FarmPatch, WorldPoint> approachTiles = new EnumMap<>(FarmPatch.class);
	private static final String APPROACH_KEY = "gardenerTiles";
	private boolean ourHintArrow;
	/** Grown trees the game says still need their health checked (seen while you were nearby). */
	private final Set<FarmPatch> healthCheckDue = java.util.EnumSet.noneOf(FarmPatch.class);
	private RunPlan lastPlan;
	/** True when everything the first leg needs is carried, so guidance can start. */
	private volatile boolean readyToRun = true;
	/** Once the run has started, using an item must not send the guide back to getting ready. */
	private boolean runStarted;
	/** Every first-leg item, kept in the getting-ready box with its carried state. */
	static final class ReadyItem
	{
		static final int MISSING = 0;
		static final int CARRIED = 1;
		static final int AT_SOURCE = 2;

		final String text;
		final int state;

		ReadyItem(String text, int state)
		{
			this.text = text;
			this.state = state;
		}
	}

	private volatile List<ReadyItem> readyItems = Collections.emptyList();
	/** Teleport items Shortest Path's own route uses, so the bring list asks for them. */
	private final Set<String> shortestPathItems = new LinkedHashSet<>();
	private final Set<String> shortestPathPending = new HashSet<>();
	private String shortestPathQueryKey;
	private boolean shortestPathBankOpenLast;
	private boolean bankOpenLastTick;
	private int shortestPathRound;
	private Boolean shortestPathOn;
	private int ticksSinceShortestPathCheck;
	/** Bank view details, by bank item id, and how many of each are carried. */
	private volatile Map<Integer, RunPlan.Withdrawal> bankLinesById = Collections.emptyMap();
	private volatile List<RunPlan.Withdrawal> bankLines = Collections.emptyList();
	private volatile Map<String, Integer> carriedForBank = Collections.emptyMap();

	/** What's happened at each stop since you planted there (for the step highlight). */
	private final Map<FarmPatch, StepItems.Progress> progress = new EnumMap<>(FarmPatch.class);
	private volatile String stepItem;
	/** More items the current step needs at once (the spirit tree's other payments). */
	private volatile java.util.Set<String> stepExtras = java.util.Collections.emptySet();
	/** What's on each grown plant you've seen (berries left, harvested...). */
	private final Map<FarmPatch, StepItems.Grown> grownStates = new EnumMap<>(FarmPatch.class);
	private FarmPatch stepPatch;
	private String stepAction;

	@Provides
	HerbRunConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HerbRunConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new HerbRunPanel(itemManager, timers, this::resetTimers, this::setRun,
			this::toggleTeleport, this::setUseSpells, this::setUseTabs, this::toggleCrop, this::togglePatch,
			this::setSetting, this::moveStop);
		panel.setOnClearHistory(this::clearHistory);
		panel.setSpriteManager(spriteManager);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Farm Run Optimizer")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(bankOverlay);
		overlayManager.add(navigationOverlay);
		overlayManager.add(inventoryOverlay);
		overlayManager.add(readyOverlay);
		overlayManager.add(leadHintOverlay);
		overlayManager.add(spellHighlightOverlay);
		overlayManager.add(minimapArrowOverlay);
		overlayManager.add(stepOverlay);
		overlayManager.add(farmBankViewOverlay);
		overlayManager.add(farmBankButtonOverlay);
		// The bank button opens this tag: the bank shows only what the run still needs.
		tagManager.registerTag(FarmBankButton.TAG, farmBankTag);
		dirty = true;
		timersLoaded = false;
		ticksSinceQuestCheck = QUEST_CHECK_INTERVAL_TICKS;
		loadTiles();
		loadTileMap(APPROACH_KEY, approachTiles);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(bankOverlay);
		overlayManager.remove(navigationOverlay);
		overlayManager.remove(inventoryOverlay);
		overlayManager.remove(readyOverlay);
		overlayManager.remove(leadHintOverlay);
		overlayManager.remove(spellHighlightOverlay);
		overlayManager.remove(minimapArrowOverlay);
		overlayManager.remove(stepOverlay);
		overlayManager.remove(farmBankViewOverlay);
		overlayManager.remove(farmBankButtonOverlay);
		farmBankLayout.shutDown();
		farmBankButton.destroy();
		tagManager.unregisterTag(FarmBankButton.TAG);
		saveHistory(); // keeps the run in progress for next time
		lastStatus.clear();
		statusesPrimed = false;
		lastFarmingXp = 0;
		contract = null;
		progress.clear();
		stepItem = null;
		panel.stop();
		clearNavigation();
		bankItems.clear();
		inventoryItems.clear();
		equipmentItems.clear();
		bankNameToId.clear();
		inventoryNameToId.clear();
		equipmentNameToId.clear();
		patchObjects.clear();
		gardeners.clear();
		fairyRingObjects.clear();
		spiritTreeObjects.clear();
		lastSeenValue.clear();
		bankSeen = false;
		highlightIds = Collections.emptySet();
		nextPatch = null;
	}

	// ================================================================ events

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGGED_IN:
				ticksSinceQuestCheck = QUEST_CHECK_INTERVAL_TICKS;
				readRunePouch();
				leprechaunItems.clear();
				leprechaunItems.putAll(Leprechaun.read(client));
				dirty = true;
				break;
			case LOADING:
				patchObjects.clear();
				lastSeenValue.clear();
				break;
			case LOGIN_SCREEN:
				// Another account may log in next: forget everything account-specific, or the
				// first ticks plan with the previous character's bank, quests and progress.
				bankItems.clear();
				bankNameToId.clear();
				inventoryItems.clear();
				inventoryNameToId.clear();
				equipmentItems.clear();
				equipmentNameToId.clear();
				runePouchItems.clear();
				runePouchCarried = false;
				seedVaultItems.clear();
				leprechaunItems.clear();
				shortestPathItems.clear();
				shortestPathPending.clear();
				shortestPathQueryKey = null;
				shortestPathBankOpenLast = false;
				shortestPathOn = null;
				runStarted = false;
				readyToRun = true;
				bankSeen = false;
				timersLoaded = false;
				notifiedReady.clear();
				completedQuests.clear();
				fairyRingsUnlocked = false;
				ticksSinceQuestCheck = QUEST_CHECK_INTERVAL_TICKS;
				contract = null;
				lastStatus.clear();
				statusesPrimed = false;
				lastFarmingXp = 0;
				progress.clear();
				grownStates.clear();
				healthCheckDue.clear();
				doneAt.clear();
				lastReady = null;
				lastPlan = null;
				stepItem = null;
				stepPatch = null;
				stepAction = null;
				clearNavigation();
				break;
			default:
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() == Skill.FARMING || event.getSkill() == Skill.MAGIC)
		{
			dirty = true;
		}
		if (event.getSkill() == Skill.FARMING)
		{
			int xp = event.getXp();
			// Count Farming XP gained at a patch towards the run (not Tithe Farm, contracts, etc.).
			if (lastFarmingXp > 0 && xp > lastFarmingXp && timersLoaded && patchNearPlayer() != null)
			{
				history.gainedXp(xp - lastFarmingXp, System.currentTimeMillis());
				saveHistory();
			}
			lastFarmingXp = xp;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int id = event.getVarbitId();
		for (int varbit : RUNE_POUCH_TYPE_VARBITS)
		{
			if (varbit == id)
			{
				readRunePouch();
				dirty = true;
				return;
			}
		}
		for (int varbit : RUNE_POUCH_QTY_VARBITS)
		{
			if (varbit == id)
			{
				readRunePouch();
				dirty = true;
				return;
			}
		}
		for (int varbit : Leprechaun.WATCHED)
		{
			if (varbit == id)
			{
				leprechaunItems.clear();
				leprechaunItems.putAll(Leprechaun.read(client));
				dirty = true;
				return;
			}
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = event.getMenuOption();
		if (option == null)
		{
			return;
		}
		String lower = option.toLowerCase();
		// Capture the list position before the bank starts rebuilding after a withdrawal/deposit.
		if (lower.contains("withdraw") || lower.contains("deposit"))
		{
			farmBankLayout.saveScroll();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id == InventoryID.BANK)
		{
			snapshot(event.getItemContainer(), bankItems, bankNameToId);
			lastBankTick = client.getTickCount();
			bankSeen = true;
			dirty = true;
		}
		else if (id == InventoryID.INV)
		{
			Map<String, Integer> before = new HashMap<>(inventoryItems);
			snapshot(event.getItemContainer(), inventoryItems, inventoryNameToId);
			updatePouchState();
			noticeHarvest(before);
			noticeItemsUsed();
			dirty = true;
		}
		else if (id == InventoryID.WORN)
		{
			snapshot(event.getItemContainer(), equipmentItems, equipmentNameToId);
			dirty = true;
		}
		else if (id == InventoryID.SEED_VAULT)
		{
			snapshot(event.getItemContainer(), seedVaultItems, new HashMap<>());
			dirty = true;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		String lower = event.getMessage().toLowerCase();
		if (lower.contains("you treat the"))
		{
			// "You treat the herb patch with ultracompost."
			for (StepItems.Progress p : progress.values())
			{
				p.composted = true;
			}
			return;
		}
		Crop crop = Crop.fromPlantMessage(event.getMessage());
		Player player = client.getLocalPlayer();
		if (crop == null || player == null)
		{
			return;
		}
		WorldPoint where = player.getWorldLocation();
		FarmPatch patch = patchAt(where, crop.getType());
		if (patch == null)
		{
			log.debug("Planted {} but no known patch at {}", crop, where);
			return;
		}
		if (progress.containsKey(patch) && System.currentTimeMillis() - progress.get(patch).plantedAt < 60_000L)
		{
			return; // already seen from the patch itself
		}
		planted(patch, crop);
	}

	/**
	 * You've just planted this patch: start its timer and the compost / pay-the-gardener steps.
	 * Called from the "You plant..." message, or from the patch itself turning newly planted (so
	 * it works even when the game words the message differently, like for saplings).
	 */
	private void planted(FarmPatch patch, Crop crop)
	{
		long now = System.currentTimeMillis();
		timers.planted(patch, crop, now);
		history.planted(patch, now);
		saveHistory();
		RunPlan.RouteStep step = stepFor(patch);
		// Keep the other stops' compost/payment progress: planting a second patch must not
		// forget that the first one still needs compost and its gardener payment.
		progress.entrySet().removeIf(e -> now - e.getValue().plantedAt >= StepItems.PROGRESS_WINDOW_MILLIS);
		progress.put(patch, new StepItems.Progress(now,
			step == null || step.compost == null ? 0 : inventoryItems.getOrDefault(step.compost, 0),
			step == null || step.paymentItem == null ? 0 : inventoryItems.getOrDefault(step.paymentItem, 0)));
		notifiedReady.remove(patch);
		saveTimers();
		dirty = true;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (Gardeners.ALL.contains(npc.getId()) || hasPayOption(npc))
		{
			gardeners.add(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		gardeners.remove(event.getNpc());
	}

	private static boolean hasPayOption(NPC npc)
	{
		NPCComposition comp = npc.getComposition();
		if (comp == null || comp.getActions() == null)
		{
			return false;
		}
		for (String a : comp.getActions())
		{
			if (a != null && a.startsWith("Pay"))
			{
				return true;
			}
		}
		return false;
	}

	/** The gardener to pay right now (closest one with a Pay option to the patch), or null. */
	NPC getGardenerToPay()
	{
		FarmPatch patch = payPatch;
		if (patch == null)
		{
			return null;
		}
		WorldPoint t = tileFor(patch);
		int wanted = Gardeners.idFor(patch);
		for (NPC npc : gardeners)
		{
			if (npc.getId() == wanted)
			{
				return npc; // this patch's own gardener
			}
		}
		NPC best = null;
		int bestDist = 20;
		for (NPC npc : gardeners)
		{
			WorldPoint w = npc.getWorldLocation();
			if (w == null || w.getPlane() != t.getPlane())
			{
				continue;
			}
			int d = dist(w, t.getX(), t.getY());
			if (d <= bestDist)
			{
				bestDist = d;
				best = npc;
			}
		}
		return best;
	}

	String getPayLabel()
	{
		return payLabel;
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject obj = event.getGameObject();
		ObjectComposition def = client.getObjectDefinition(obj.getId());
		if (noteTravelObject(obj, def))
		{
			return;
		}
		if (def == null || !FARMING_VARBITS.contains(def.getVarbitId()))
		{
			return;
		}
		int varbit = def.getVarbitId();
		patchObjects.put(obj, varbit);
		WorldPoint loc = obj.getWorldLocation();
		FarmPatch patch = patchFor(loc, varbit);
		if (patch != null && !loc.equals(learnedTiles.get(patch)))
		{
			learnedTiles.put(patch, loc);
			saveTiles();
		}
	}

	/** The patch an object belongs to: same varbit and one of the patch's regions. */
	private static FarmPatch patchFor(WorldPoint loc, int varbit)
	{
		int region = FarmPatch.regionOf(loc.getX(), loc.getY());
		FarmPatch best = null;
		int bestDist = Integer.MAX_VALUE;
		for (FarmPatch p : FarmPatch.values())
		{
			if (p.getVarbit() != varbit || !p.inRegion(region))
			{
				continue;
			}
			int d = Math.max(Math.abs(p.getX() - loc.getX()), Math.abs(p.getY() - loc.getY()));
			if (d < bestDist)
			{
				best = p;
				bestDist = d;
			}
		}
		return best;
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		patchObjects.remove(event.getGameObject());
		fairyRingObjects.remove(event.getGameObject());
		spiritTreeObjects.remove(event.getGameObject());
	}

	// Fairy rings are flat on the ground, so the game sends them as ground objects.
	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		GroundObject obj = event.getGroundObject();
		noteTravelObject(obj, client.getObjectDefinition(obj.getId()));
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		fairyRingObjects.remove(event.getGroundObject());
		spiritTreeObjects.remove(event.getGroundObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		DecorativeObject obj = event.getDecorativeObject();
		noteTravelObject(obj, client.getObjectDefinition(obj.getId()));
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		fairyRingObjects.remove(event.getDecorativeObject());
		spiritTreeObjects.remove(event.getDecorativeObject());
	}

	/** Remembers fairy rings and spirit trees (by name, following "impostor" objects). */
	private boolean noteTravelObject(TileObject obj, ObjectComposition def)
	{
		if (def == null)
		{
			return false;
		}
		String name = def.getName();
		if (def.getImpostorIds() != null)
		{
			ObjectComposition real = def.getImpostor();
			if (real != null)
			{
				name = real.getName();
			}
		}
		if (name == null)
		{
			return false;
		}
		if (name.toLowerCase().startsWith("fairy ring"))
		{
			// Only real rings, at a known ring spot (some scenery shares the name).
			if (FairyRings.isRingAt(obj.getWorldLocation().getX(), obj.getWorldLocation().getY()))
			{
				fairyRingObjects.add(obj);
			}
			return true;
		}
		if (name.equalsIgnoreCase("Spirit tree"))
		{
			spiritTreeObjects.add(obj);
			return true;
		}
		return false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!HerbRunConfig.GROUP.equals(event.getGroup()) || TIMERS_KEY.equals(event.getKey())
			|| CLOCK_KEY.equals(event.getKey()) || TILES_KEY.equals(event.getKey()) || APPROACH_KEY.equals(event.getKey()))
		{
			return;
		}
		dirty = true;
		shortestPathOn = null;
		if (!config.useShortestPath())
		{
			clearShortestPath();
			shortestPathItems.clear();
			shortestPathPending.clear();
			shortestPathQueryKey = null;
		}
		if ("pathMinigameTeleports".equals(event.getKey()))
		{
			shortestPathResend = true; // send the route again with the new setting
		}
		if (!config.gameHintArrow())
		{
			clearHintArrow();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (++ticksSinceShortestPathCheck >= 20)
		{
			ticksSinceShortestPathCheck = 0;
			shortestPathOn = config.useShortestPath() && shortestPathNotice() == null;
		}
		Widget bankWidget = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		boolean bankOpenNow = bankWidget != null && !bankWidget.isHidden();
		if (bankOpenNow != bankOpenLastTick)
		{
			bankOpenLastTick = bankOpenNow;
			dirty = true; // refresh the tab and re-ask Shortest Path on open/close
		}
		if (!toldAboutShortestPath && client.getGameState() == GameState.LOGGED_IN)
		{
			toldAboutShortestPath = true;
			String notice = shortestPathNotice();
			if (notice != null)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"<col=0099ff>Farm Run Optimizer:</col> " + notice, null);
			}
		}
		if (!timersLoaded)
		{
			loadTimers();
		}
		if (++ticksSinceQuestCheck >= QUEST_CHECK_INTERVAL_TICKS)
		{
			ticksSinceQuestCheck = 0;
			refreshRequirements();
			if (timersLoaded)
			{
				importTimeTracking();
			}
		}
		if (timersLoaded && ++ticksSincePatchSync >= PATCH_SYNC_INTERVAL_TICKS)
		{
			ticksSincePatchSync = 0;
			syncNearbyPatches();
		}
		checkReadyNotifications();
		trackReadyPatches();
		if (timersLoaded)
		{
			checkDiseaseNotifications();
			readContract();
			if (history.tick(System.currentTimeMillis()))
			{
				saveHistory();
				dirty = true;
			}
			updateReadyLines();
		}
		noticeGardenerPaid();
		if (dirty)
		{
			dirty = false;
			recompute();
		}
		updateNavigation();
		updateStepItem();
		farmBankButton.onGameTick();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			farmBankButton.init();
			dirty = true; // refresh the Farm run tab and re-ask Shortest Path on open
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			// The next bank open must ask Shortest Path again; the route may have changed.
			shortestPathBankOpenLast = false;
			dirty = true;
		}
	}

	@Subscribe
	public void onPluginMessage(PluginMessage event)
	{
		if (!"shortestpath".equals(event.getNamespace()) || !"result".equals(event.getName()))
		{
			return;
		}
		Object id = event.getData() == null ? null : event.getData().get("id");
		if (id == null || !shortestPathPending.remove(String.valueOf(id)))
		{
			return;
		}
		boolean changed = false;
		Object transports = event.getData().get("transports");
		if (transports instanceof List)
		{
			Set<String> owned = ownedItemNames();
			for (Object transport : (List<?>) transports)
			{
				if (!(transport instanceof Map))
				{
					continue;
				}
				Object display = ((Map<?, ?>) transport).get("displayInfo");
				String item = TravelItems.find(display == null ? null : String.valueOf(display), owned);
				if (item != null && shortestPathItems.add(item))
				{
					changed = true;
				}
			}
		}
		if (changed)
		{
			dirty = true;
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING)
		{
			farmBankLayout.onBankBuilt();
		}
	}

	// ================================================================ harvest log, disease, contract, overlay

	/** Counts produce that lands in your inventory while you're standing at a patch. */
	private void noticeHarvest(Map<String, Integer> before)
	{
		if (!timersLoaded || client.getTickCount() - lastBankTick <= 1)
		{
			return; // bank withdrawals aren't harvests
		}
		FarmPatch patch = patchNearPlayer();
		if (patch == null)
		{
			return;
		}
		Set<Integer> produceIds = new HashSet<>();
		for (Crop c : Crop.values())
		{
			if (c.getType() == patch.getType())
			{
				produceIds.add(HerbPrices.produceId(c));
			}
		}
		long now = System.currentTimeMillis();
		boolean changed = false;
		for (Map.Entry<String, Integer> e : inventoryItems.entrySet())
		{
			int gained = e.getValue() - before.getOrDefault(e.getKey(), 0);
			Integer id = inventoryNameToId.get(e.getKey());
			if (gained <= 0 || id == null)
			{
				continue;
			}
			int canonical = itemManager.canonicalize(id);
			if (produceIds.contains(canonical))
			{
				history.harvested(patch, e.getKey(), gained, itemManager.getItemPrice(canonical),
					patch.getType() == PatchType.HERB, seedPriceForProduce(canonical), now);
				changed = true;
			}
		}
		if (changed)
		{
			saveHistory();
			dirty = true;
		}
	}

	/** GE price of the seed that grew this produce (herb seeds; saplings if you hold one). */
	private long seedPriceForProduce(int produceId)
	{
		for (Crop c : Crop.values())
		{
			if (HerbPrices.produceId(c) == produceId)
			{
				int seedId = HerbPrices.seedId(c) > 0 ? HerbPrices.seedId(c)
					: inventoryNameToId.getOrDefault(c.getSeedName(), bankNameToId.getOrDefault(c.getSeedName(), -1));
				return seedId > 0 ? itemManager.getItemPrice(itemManager.canonicalize(seedId)) : 0;
			}
		}
		return 0;
	}

	/**
	 * The patch of this type you're standing at: the closest by its real (loaded) position, so the
	 * two allotments side by side are told apart.
	 */
	private FarmPatch patchAt(WorldPoint where, PatchType type)
	{
		int region = FarmPatch.regionOf(where.getX(), where.getY());
		FarmPatch best = null;
		int bestDist = Integer.MAX_VALUE;
		for (FarmPatch p : FarmPatch.values())
		{
			if (p.getType() != type || !p.inRegion(region))
			{
				continue;
			}
			WorldPoint t = tileFor(p);
			int d = dist(where, t.getX(), t.getY());
			if (d < bestDist)
			{
				bestDist = d;
				best = p;
			}
		}
		return best;
	}

	/** The closest patch within a few tiles of you, or null. */
	private FarmPatch patchNearPlayer()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		WorldPoint me = player.getWorldLocation();
		int region = FarmPatch.regionOf(me.getX(), me.getY());
		FarmPatch best = null;
		int bestDist = 12;
		for (FarmPatch p : FarmPatch.values())
		{
			if (!p.inRegion(region))
			{
				continue;
			}
			WorldPoint t = tileFor(p);
			int d = Math.max(Math.abs(t.getX() - me.getX()), Math.abs(t.getY() - me.getY()));
			if (d <= bestDist)
			{
				bestDist = d;
				best = p;
			}
		}
		return best;
	}

	private void checkDiseaseNotifications()
	{
		for (FarmPatch p : FarmPatch.values())
		{
			PatchTimers.Status st = timers.getStatus(p);
			PatchTimers.Status was = lastStatus.get(p);
			if (statusesPrimed && st != was && config.notifyDisease()
				&& (st == PatchTimers.Status.DISEASED || st == PatchTimers.Status.DEAD))
			{
				Crop crop = timers.getStatusCrop(p);
				notifier.notify(p.getDisplayName() + (crop == null ? "" : " (" + crop.getDisplayName() + ")")
					+ (st == PatchTimers.Status.DISEASED ? " is diseased! Cure it before it dies." : " has died."));
			}
			if (st == null)
			{
				lastStatus.remove(p);
			}
			else
			{
				lastStatus.put(p, st);
			}
		}
		statusesPrimed = true;
	}

	private void readContract()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		FarmingContract now = FarmingContract.of(client.getVarbitValue(FarmingContract.TYPE_VARBIT),
			client.getVarbitValue(FarmingContract.COMPLETE_VARBIT));
		FarmingContract was = contract;
		boolean same = was == null ? now == null : now != null && was.value == now.value && was.complete == now.complete;
		if (!same)
		{
			contract = now;
			dirty = true;
		}
	}

	FarmingContract getContract()
	{
		return contract;
	}

	List<RunReadyOverlay.Line> getReadyLines()
	{
		return readyLines;
	}

	boolean showReadyOverlay()
	{
		return config.showReadyOverlay();
	}

	/** What the on-screen "run now?" box says: ready counts per run, disease, contract. */
	private void updateReadyLines()
	{
		long now = System.currentTimeMillis();
		List<RunReadyOverlay.Line> lines = new ArrayList<>();
		Set<PatchType> runs = runs();
		Set<FarmPatch> off = readSet(config.disabledPatches(), FarmPatch.class);
		int sick = 0;
		for (PatchType type : PatchType.values())
		{
			int ready = 0;
			int tracked = 0;
			long soonest = Long.MAX_VALUE;
			for (FarmPatch p : FarmPatch.values())
			{
				if (p.getType() != type || off.contains(p))
				{
					continue;
				}
				PatchTimers.Planting pl = timers.get(p);
				PatchTimers.Status st = timers.getStatus(p);
				if (st == PatchTimers.Status.DISEASED || st == PatchTimers.Status.DEAD)
				{
					sick++;
				}
				if (pl == null)
				{
					continue;
				}
				tracked++;
				if (pl.isReady(now))
				{
					ready++;
				}
				else
				{
					soonest = Math.min(soonest, pl.millisLeft(now));
				}
			}
			if (!runs.contains(type) || tracked == 0)
			{
				continue;
			}
			if (ready > 0)
			{
				lines.add(new RunReadyOverlay.Line(type.getDisplayName(), ready + " of " + tracked + " ready",
					new Color(90, 200, 90)));
			}
			else if (soonest != Long.MAX_VALUE)
			{
				lines.add(new RunReadyOverlay.Line(type.getDisplayName(), "in " + shortTime(soonest), Color.WHITE));
			}
		}
		if (sick > 0)
		{
			lines.add(new RunReadyOverlay.Line("Diseased/dead", String.valueOf(sick), new Color(230, 80, 70)));
		}
		FarmingContract c = contract;
		if (c != null)
		{
			lines.add(new RunReadyOverlay.Line("Contract", c.complete ? "claim reward" : c.name,
				c.complete ? new Color(90, 200, 90) : new Color(255, 200, 60)));
		}
		readyLines = lines;
	}

	private static String shortTime(long ms)
	{
		long min = Math.max(1, (ms + 59_999) / 60_000);
		return min < 60 ? min + "m" : (min / 60) + "h " + (min % 60) + "m";
	}

	private void saveHistory()
	{
		if (configManager.getRSProfileKey() != null)
		{
			configManager.setRSProfileConfiguration(HerbRunConfig.GROUP, HISTORY_KEY, history.serialize());
			configManager.setRSProfileConfiguration(HerbRunConfig.GROUP, CURRENT_RUN_KEY, history.serializeCurrent());
		}
	}

	/** Called from the panel (Swing thread). */
	private void clearHistory()
	{
		history.clear();
		saveHistory();
		dirty = true;
	}

	private static final long DONE_KEEP_MS = 30 * 60_000L;
	private final Map<FarmPatch, Long> doneAt = new EnumMap<>(FarmPatch.class);
	private Set<FarmPatch> lastReady = null;

	/**
	 * Replans when a patch becomes ready, and remembers patches you've just done so they stay
	 * on the list (green) instead of the plan jumping back to a full run.
	 */
	private void trackReadyPatches()
	{
		if (!timersLoaded)
		{
			return;
		}
		long now = System.currentTimeMillis();
		Set<FarmPatch> ready = EnumSet.noneOf(FarmPatch.class);
		for (FarmPatch p : FarmPatch.values())
		{
			if (timers.needsVisit(p, now))
			{
				ready.add(p);
			}
		}
		if (lastReady != null)
		{
			for (FarmPatch p : lastReady)
			{
				if (!ready.contains(p))
				{
					doneAt.put(p, now);
				}
			}
			if (!lastReady.equals(ready))
			{
				dirty = true;
			}
		}
		if (doneAt.entrySet().removeIf(e -> ready.contains(e.getKey()) || now - e.getValue() > DONE_KEEP_MS))
		{
			dirty = true;
		}
		lastReady = ready;
	}

	// ================================================================ accessors for overlays

	Set<Integer> getHighlightIds()
	{
		return highlightIds;
	}

	/** True when the first leg's items are carried (or "Wait until ready" is off). */
	boolean isReadyToRun()
	{
		return readyToRun;
	}

	List<ReadyItem> getReadyItems()
	{
		return readyItems;
	}

	Map<Integer, RunPlan.Withdrawal> getBankLinesById()
	{
		return bankLinesById;
	}

	List<RunPlan.Withdrawal> getBankLines()
	{
		return bankLines;
	}

	Map<String, Integer> getCarriedForBank()
	{
		return carriedForBank;
	}

	Map<String, Integer> getLeprechaunItems()
	{
		return leprechaunItems;
	}

	Map<String, Integer> getSeedVaultItems()
	{
		return seedVaultItems;
	}

	FarmPatch getNextPatch()
	{
		return nextPatch;
	}

	/** The loaded game object for this patch, or null if it isn't in view. */
	GameObject objectFor(FarmPatch patch)
	{
		for (Map.Entry<GameObject, Integer> e : patchObjects.entrySet())
		{
			if (patchFor(e.getKey().getWorldLocation(), e.getValue()) == patch)
			{
				return e.getKey();
			}
		}
		return null;
	}

	/**
	 * Best known tile for a patch: the patch object itself when it's loaded, else the tile
	 * learned from the game earlier, else the built-in tile.
	 */
	WorldPoint tileFor(FarmPatch patch)
	{
		GameObject obj = objectFor(patch);
		if (obj != null)
		{
			return obj.getWorldLocation();
		}
		WorldPoint learned = learnedTiles.get(patch);
		return learned != null ? learned : new WorldPoint(patch.getX(), patch.getY(), 0);
	}

	private void loadTiles()
	{
		learnedTiles.clear();
		String raw = configManager.getConfiguration(HerbRunConfig.GROUP, TILES_KEY);
		if (raw == null || raw.isEmpty())
		{
			return;
		}
		for (String part : raw.split(";"))
		{
			String[] c = part.split(":");
			try
			{
				learnedTiles.put(FarmPatch.valueOf(c[0]),
					new WorldPoint(Integer.parseInt(c[1]), Integer.parseInt(c[2]), Integer.parseInt(c[3])));
			}
			catch (RuntimeException ignored)
			{
				// old or bad entry
			}
		}
	}

	private void saveTileMap(String key, Map<FarmPatch, WorldPoint> map)
	{
		StringBuilder sb = new StringBuilder();
		map.forEach((p, w) -> sb.append(sb.length() > 0 ? ";" : "").append(p.name()).append(':')
			.append(w.getX()).append(':').append(w.getY()).append(':').append(w.getPlane()));
		configManager.setConfiguration(HerbRunConfig.GROUP, key, sb.toString());
	}

	private void loadTileMap(String key, Map<FarmPatch, WorldPoint> map)
	{
		map.clear();
		String raw = configManager.getConfiguration(HerbRunConfig.GROUP, key);
		if (raw == null || raw.isEmpty())
		{
			return;
		}
		for (String part : raw.split(";"))
		{
			String[] c = part.split(":");
			try
			{
				map.put(FarmPatch.valueOf(c[0]),
					new WorldPoint(Integer.parseInt(c[1]), Integer.parseInt(c[2]), Integer.parseInt(c[3])));
			}
			catch (RuntimeException ignored)
			{
				// old or bad entry
			}
		}
	}

	private void saveTiles()
	{
		StringBuilder sb = new StringBuilder();
		learnedTiles.forEach((p, w) -> sb.append(sb.length() > 0 ? ";" : "").append(p.name()).append(':')
			.append(w.getX()).append(':').append(w.getY()).append(':').append(w.getPlane()));
		configManager.setConfiguration(HerbRunConfig.GROUP, TILES_KEY, sb.toString());
	}

	String getStepItem()
	{
		return stepItem;
	}

	/** The current step's action, for the box under the player. */
	String getStepAction()
	{
		return stepAction;
	}

	/** True if this item should be highlighted for the current step. */
	boolean isStepItem(String name)
	{
		if (name == null)
		{
			return false;
		}
		return name.equals(stepItem) || stepExtras.contains(name)
			// Magic secateurs can prune a diseased tree wherever plain secateurs can.
			|| ("Secateurs".equals(stepItem) && name.startsWith("Magic secateurs"));
	}

	/**
	 * What the game shows on a grown plant: produce still to pick (bushes, cactus, celastrus
	 * bark), a harvested celastrus to chop, or nothing left. Null if not grown or not one of those.
	 */
	static StepItems.Grown grownState(PatchType type, int value, PatchStates.Decoded d)
	{
		if (d.state != PatchStates.State.READY || d.checkHealth)
		{
			return null;
		}
		switch (type)
		{
			case BUSH:
			case CACTUS:
				// Harvestable values count the produce left; the first one is the empty plant.
				return d.stage > 0 ? StepItems.Grown.PRODUCE : StepItems.Grown.CLEAR;
			case CELASTRUS:
				// 14-16: bark to harvest; 17: harvested, chop it down (RuneLite's PatchImplementation).
				if (value >= 14 && value <= 16)
				{
					return StepItems.Grown.PRODUCE;
				}
				return value == 17 ? StepItems.Grown.CHOP : null;
			default:
				return null;
		}
	}

	// ================================================================ step highlight

	private RunPlan.RouteStep stepFor(FarmPatch patch)
	{
		if (lastPlan == null)
		{
			return null;
		}
		for (RunPlan.RouteStep s : lastPlan.route)
		{
			if (s.patch == patch)
			{
				return s;
			}
		}
		return null;
	}

	/** The NPC dialogue box (chat head on the left): RuneLite's InterfaceID.ChatLeft. */
	private static final int NPC_DIALOGUE_TEXT = InterfaceID.ChatLeft.TEXT;

	/**
	 * The gardener saying they'll look after the patch means you've paid, whatever you paid with:
	 * "That'll do nicely. Leave it with me - I'll make sure that patch grows for you." (or that
	 * they're already looking after it). Checked every tick while a payment step is open.
	 */
	private void noticeGardenerPaid()
	{
		boolean waiting = false;
		for (StepItems.Progress p : progress.values())
		{
			waiting |= !p.paid;
		}
		if (!waiting)
		{
			return;
		}
		Widget w = client.getWidget(NPC_DIALOGUE_TEXT);
		if (w == null || w.isHidden() || w.getText() == null)
		{
			return;
		}
		if (StepItems.saysProtected(w.getText()))
		{
			for (StepItems.Progress p : progress.values())
			{
				p.paid = true;
			}
			dirty = true;
		}
	}

	/** Compost or payment leaving the inventory after planting means that step is done. */
	private void noticeItemsUsed()
	{
		for (Map.Entry<FarmPatch, StepItems.Progress> e : progress.entrySet())
		{
			RunPlan.RouteStep step = stepFor(e.getKey());
			StepItems.Progress p = e.getValue();
			if (step == null)
			{
				continue;
			}
			if (!p.composted && step.compost != null && !step.compost.startsWith("Bottomless")
				&& inventoryItems.getOrDefault(step.compost, 0) < p.compostBaseline)
			{
				p.composted = true;
				dirty = true;
			}
			if (!p.paid && step.paymentItem != null
				&& inventoryItems.getOrDefault(step.paymentItem, 0) < p.paymentBaseline)
			{
				p.paid = true;
				dirty = true;
			}
		}
	}

	/**
	 * Works out which stop you're at (the route patch in the area you're standing in, else the
	 * next stop) and which inventory item its current step needs.
	 */
	private void updateStepItem()
	{
		Player player = client.getLocalPlayer();
		if (lastPlan == null || player == null)
		{
			stepItem = null;
			stepAction = null;
			return;
		}
		if (config.waitUntilReady() && !readyToRun)
		{
			stepItem = null;
			stepAction = null;
			stepPatch = null;
			spellToHighlight = null;
			return;
		}
		long now = System.currentTimeMillis();
		WorldPoint wp = player.getWorldLocation();
		int region = FarmPatch.regionOf(wp.getX(), wp.getY());

		RunPlan.RouteStep focus = null;
		for (RunPlan.RouteStep s : lastPlan.route)
		{
			if (!s.patch.inRegion(region))
			{
				continue;
			}
			StepItems.Progress p = progress.get(s.patch);
			if (p != null && StepItems.next(s, timers.get(s.patch), timers.getStatus(s.patch), p, now) != null)
			{
				focus = s;
				break;
			}
			if (focus == null && timers.needsVisit(s.patch, now))
			{
				focus = s;
			}
		}
		boolean travelling = false;
		if (focus == null && nextPatch != null)
		{
			focus = stepFor(nextPatch);
			// Not at the next patch yet: the first thing to do is the teleport.
			WorldPoint t = tileFor(nextPatch);
			travelling = focus != null && focus.teleport != null && dist(wp, t.getX(), t.getY()) > 25;
		}

		StepItems.Next next = focus == null ? null
			: StepItems.next(focus, timers.get(focus.patch), timers.getStatus(focus.patch), progress.get(focus.patch), now,
				healthCheckDue.contains(focus.patch), grownStates.get(focus.patch));
		String spell = null;
		if (travelling)
		{
			Teleport.Kind kind = focus.teleport.getKind();
			if (kind == Teleport.Kind.SPELL)
			{
				spell = focus.teleport.getLabel();
				next = new StepItems.Next(null, "Cast " + spell);
			}
			else if ((kind == Teleport.Kind.ITEM || kind == Teleport.Kind.TABLET) && focus.teleportItem != null)
			{
				next = new StepItems.Next(focus.teleportItem, "Teleport");
			}
			else if (focus.teleport == Teleport.SPIRIT_TREE && nearest(spiritTreeObjects, wp, 40) == null
				&& dist(wp, GE_SPIRIT_TREE.getX(), GE_SPIRIT_TREE.getY()) > 120)
			{
				// First get to the Grand Exchange spirit tree: Varrock Teleport.
				if (config.useSpells() && client.getRealSkillLevel(Skill.MAGIC) >= 25)
				{
					spell = Teleport.VARROCK_SPELL.getLabel();
					next = new StepItems.Next(null, "Cast Varrock Teleport, then the spirit tree by the GE");
				}
				else if (inventoryItems.containsKey("Varrock teleport"))
				{
					next = new StepItems.Next("Varrock teleport", "Teleport to Varrock, then the spirit tree by the GE");
				}
			}
		}
		spellToHighlight = config.highlightInventory() ? spell : null;
		String item = next == null ? null : next.item;
		String action = next == null ? null : next.action;
		FarmPatch patch = next == null ? null : focus.patch;
		stepItem = item;
		Set<String> extras = new HashSet<>();
		if (item != null && action != null && action.startsWith("Pay the gardener") && !action.contains("clear")
			&& focus.crop != null)
		{
			for (String[] extra : focus.crop.getExtraPayments())
			{
				extras.add(extra[0]); // e.g. the spirit tree's monkey bar and ground tooth
			}
		}
		stepExtras = extras;
		if (action != null && action.startsWith("Pay the gardener") && config.highlightGardener())
		{
			payPatch = patch;
			payLabel = action.contains("clear") ? "Pay " + RunPlanner.TREE_REMOVAL_COINS + " coins to chop it down"
				: "Pay: " + (focus.paymentQuantity > 1 ? focus.paymentQuantity + " x " : "") + focus.paymentItem;
		}
		else
		{
			payPatch = null;
			payLabel = null;
		}
		if (patch != stepPatch || !java.util.Objects.equals(action, stepAction))
		{
			stepPatch = patch;
			stepAction = action;
			SwingUtilities.invokeLater(() -> panel.setStepHint(patch, action, item));
		}
	}

	private volatile String spellToHighlight;

	/** The teleport spell to outline in the spellbook (e.g. "Falador Teleport"), or null. */
	String getSpellToHighlight()
	{
		return spellToHighlight;
	}

	// ================================================================ items, quests, planning

	/** Item id for an exact item name, using RuneLite's item cache. -1 if unknown. */
	private final Map<String, Integer> itemIdCache = new HashMap<>();

	private int itemIdByName(String name)
	{
		Integer cached = itemIdCache.get(name);
		if (cached != null)
		{
			return cached;
		}
		int id = -1;
		for (net.runelite.http.api.item.ItemPrice price : itemManager.search(name))
		{
			if (price.getName().equalsIgnoreCase(name))
			{
				id = price.getId();
				break;
			}
		}
		if (id > 0)
		{
			itemIdCache.put(name, id);
		}
		return id;
	}

	private void addBankTagId(Set<Integer> ids, RunPlan.Withdrawal w, Map<String, Integer> nameToId)
	{
		// A bank placeholder with quantity 0 is not stock: don't put it in the tag view.
		if (bankItems.getOrDefault(w.itemName, 0) <= 0 || isPlaceholderName(w.itemName))
		{
			return;
		}
		int id = bankLineId(w.itemName, nameToId);
		if (id > 0)
		{
			ids.add(id);
		}
	}

	private int bankLineId(String name, Map<String, Integer> nameToId)
	{
		Integer id = nameToId.get(name);
		if (id == null || id <= 0)
		{
			id = itemIdByName(name);
		}
		return id == null ? -1 : id;
	}

	private void putBankLine(Map<Integer, RunPlan.Withdrawal> lines, RunPlan.Withdrawal w,
		Map<String, Integer> nameToId)
	{
		int id = bankLineId(w.itemName, nameToId);
		if (id > 0)
		{
			lines.merge(id, w, (a, b) -> a.quantity >= b.quantity ? a : b);
		}
	}

	private void snapshot(ItemContainer container, Map<String, Integer> target, Map<String, Integer> nameToId)
	{
		target.clear();
		nameToId.clear();
		if (container == null)
		{
			return;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			// Bank placeholders are items with zero stock. They must not count as owned,
			// even if the client reports a quantity of 1 for the placeholder slot.
			ItemComposition composition = itemManager.getItemComposition(item.getId());
			if (composition != null && composition.getPlaceholderTemplateId() != -1
				&& composition.getPlaceholderId() != -1)
			{
				continue;
			}
			int canonical = itemManager.canonicalize(item.getId());
			String name = itemManager.getItemComposition(canonical).getName();
			target.merge(name, item.getQuantity(), Integer::sum);
			nameToId.putIfAbsent(name, item.getId());
		}
	}

	/**
	 * Reads the runes inside the rune pouch from the game's varbits, so a pouch holding law,
	 * air and fire counts as those runes without withdrawing them one stack per type.
	 */
	private void readRunePouch()
	{
		runePouchItems.clear();
		EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		if (runes == null)
		{
			return;
		}
		for (int i = 0; i < RUNE_POUCH_TYPE_VARBITS.length; i++)
		{
			int quantity = client.getVarbitValue(RUNE_POUCH_QTY_VARBITS[i]);
			int rune = client.getVarbitValue(RUNE_POUCH_TYPE_VARBITS[i]);
			if (quantity <= 0 || rune < 0)
			{
				continue;
			}
			int itemId = runes.getIntValue(rune);
			if (itemId <= 0)
			{
				continue;
			}
			String name = itemManager.getItemComposition(itemManager.canonicalize(itemId)).getName();
			runePouchItems.merge(name, quantity, Integer::sum);
		}
		updatePouchState();
	}

	/** Which pouch (if any) is being carried, and how many rune types it holds. */
	private void updatePouchState()
	{
		boolean divine = inventoryItems.containsKey("Divine rune pouch");
		runePouchCarried = divine || inventoryItems.containsKey("Rune pouch");
		runePouchName = divine ? "Divine rune pouch" : "Rune pouch";
		runePouchCapacity = divine ? 4 : 3;
	}

	/** Must run on the client thread (Quest.getState runs a client script). */
	private void refreshRequirements()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		Set<String> wanted = new HashSet<>();
		Collections.addAll(wanted, TELEPORT_QUESTS);
		for (FarmPatch.Requirement r : FarmPatch.Requirement.values())
		{
			if (r.questName() != null)
			{
				wanted.add(r.questName());
			}
		}
		Set<String> before = new HashSet<>(completedQuests);
		boolean fairyBefore = fairyRingsUnlocked;
		completedQuests.clear();
		fairyRingsUnlocked = false;
		// Looked up by name so a renamed enum constant can't break the build.
		for (Quest quest : Quest.values())
		{
			String name = quest.getName();
			if (wanted.contains(name) && quest.getState(client) == QuestState.FINISHED)
			{
				completedQuests.add(name);
			}
			else if (FAIRYTALE_II.equals(name) && quest.getState(client) != QuestState.NOT_STARTED)
			{
				fairyRingsUnlocked = true;
			}
		}
		if (!before.equals(completedQuests) || fairyBefore != fairyRingsUnlocked)
		{
			dirty = true;
		}
	}

	private int farmingLevel()
	{
		return config.useBoostedLevel()
			? client.getBoostedSkillLevel(Skill.FARMING)
			: client.getRealSkillLevel(Skill.FARMING);
	}

	private Set<PatchType> runs()
	{
		Set<PatchType> runs = EnumSet.noneOf(PatchType.class);
		for (PatchType t : PatchType.values())
		{
			if (runOn(t))
			{
				runs.add(t);
			}
		}
		return runs;
	}

	private boolean runOn(PatchType t)
	{
		switch (t)
		{
			case HERB:
				return config.includeHerbs();
			case TREE:
				return config.includeTrees();
			case FRUIT_TREE:
				return config.includeFruitTrees();
			case HARDWOOD:
				return config.includeHardwood();
			case ALLOTMENT:
				return config.includeAllotments();
			case BUSH:
				return config.includeBushes();
			case HOPS:
				return config.includeHops();
			case CACTUS:
				return config.includeCactus();
			case CALQUAT:
				return config.includeCalquat();
			case SPIRIT_TREE:
				return config.includeSpiritTrees();
			case CELASTRUS:
				return config.includeCelastrus();
			default:
				return config.includeRedwood();
		}
	}

	/**
	 * Says when something on the bring list is only in the seed vault or at the tool leprechaun,
	 * so counting those stores doesn't silently plan a trip the player doesn't know about.
	 */
	private void addStorageTips(RunPlan plan, RunPlanner.Options options)
	{
		Set<String> added = new HashSet<>();
		for (RunPlan.Withdrawal w : plan.withdrawals)
		{
			int carried = options.carried.getOrDefault(w.itemName, 0);
			int inBank = bankItems.getOrDefault(w.itemName, 0);
			if (carried + inBank >= w.quantity)
			{
				continue;
			}
			int atLeprechaun = leprechaunItems.getOrDefault(w.itemName, 0);
			int inVault = seedVaultItems.getOrDefault(w.itemName, 0);
			if (atLeprechaun > 0 && added.add("leprechaun:" + w.itemName))
			{
				plan.tips.add(w.itemName + " is at the tool leprechaun: withdraw it from a leprechaun ("
					+ atLeprechaun + " there).");
			}
			if (inVault > 0 && added.add("vault:" + w.itemName))
			{
				plan.tips.add(w.itemName + " is in the seed vault at the Farming Guild: collect it there ("
					+ inVault + " there).");
			}
		}
	}

	/**
	 * Asks Shortest Path for its own route over each leg (while no line is being followed), so
	 * the teleport items it will use can be added to the bring list before the run starts.
	 */
	private void queryShortestPathItems(RunPlan plan)
	{
		if (!config.useShortestPath())
		{
			shortestPathItems.clear();
			shortestPathPending.clear();
			shortestPathQueryKey = null;
			return;
		}
		Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		boolean bankOpen = bank != null && !bank.isHidden();
		if (bankOpen && !shortestPathBankOpenLast)
		{
			// Re-ask when the bank opens: the player's items and the route may have changed.
			shortestPathQueryKey = null;
		}
		shortestPathBankOpenLast = bankOpen;
		if (!bankOpen && shortestPathTarget != null)
		{
			return; // a line is being followed: don't cancel it with a query
		}
		List<FarmPatch> stops = new ArrayList<>();
		for (RunPlan.RouteStep step : plan.route)
		{
			if (!stops.contains(step.patch))
			{
				stops.add(step.patch);
			}
		}
		if (stops.isEmpty())
		{
			return;
		}
		StringBuilder key = new StringBuilder();
		for (FarmPatch patch : stops)
		{
			key.append(patch.name()).append(',');
		}
		if (key.toString().equals(shortestPathQueryKey))
		{
			return;
		}
		shortestPathQueryKey = key.toString();
		shortestPathItems.clear();
		shortestPathPending.clear();
		shortestPathRound++;
		WorldPoint from = null;
		for (int i = 0; i < stops.size(); i++)
		{
			FarmPatch patch = stops.get(i);
			WorldPoint tile = tileFor(patch);
			if (tile == null)
			{
				continue;
			}
			String id = "farmrun-items-" + shortestPathRound + "-" + i;
			Map<String, Object> data = new HashMap<>();
			data.put("id", id);
			if (from != null)
			{
				data.put("start", from);
			}
			data.put("target", tilesAround(tile, patch.getType().patchSize()));
			shortestPathPending.add(id);
			eventBus.post(new PluginMessage("shortestpath", "query", data));
			from = tile;
		}
	}

	/** Adds the items Shortest Path's route uses that our own plan didn't know about. */
	private void addShortestPathItems(RunPlan plan)
	{
		Set<String> owned = ownedItemNames();
		Set<String> carried = new HashSet<>(inventoryItems.keySet());
		carried.addAll(equipmentItems.keySet());
		if (runePouchCarried)
		{
			carried.addAll(runePouchItems.keySet());
		}
		for (String name : shortestPathItems)
		{
			// Shortest Path keeps the variant it first saw (e.g. Ring of wealth (5)). Resolve it
			// again every recompute so a ring that dropped to (4) is still recognised as carried.
			String item = TravelItems.resolve(name, owned, carried);
			if (hasWithdrawal(plan, item))
			{
				continue;
			}
			int have = inventoryItems.getOrDefault(item, 0) + equipmentItems.getOrDefault(item, 0);
			if (runePouchCarried)
			{
				have += runePouchItems.getOrDefault(item, 0);
			}
			plan.withdrawals.add(new RunPlan.Withdrawal(item, 1, have > 0 ? 0 : 1,
				"Shortest Path route", have > 0 ? 0 : 1, RunPlan.Group.TRAVEL, false));
		}
	}

	private static boolean hasWithdrawal(RunPlan plan, String name)
	{
		String base = baseName(name);
		for (RunPlan.Withdrawal w : plan.withdrawals)
		{
			if (w.itemName.equals(name) || baseName(w.itemName).equals(base))
			{
				return true;
			}
		}
		for (RunPlan.Withdrawal w : plan.optionalItems)
		{
			if (w.itemName.equals(name) || baseName(w.itemName).equals(base))
			{
				return true;
			}
		}
		for (RunPlan.BankStop stop : plan.bankStops)
		{
			for (RunPlan.Withdrawal w : stop.withdrawals)
			{
				if (w.itemName.equals(name) || baseName(w.itemName).equals(base))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static String baseName(String name)
	{
		String lower = name.toLowerCase(Locale.ROOT);
		int bracket = lower.indexOf('(');
		return bracket > 0 ? lower.substring(0, bracket).trim() : lower;
	}

	private Set<String> ownedItemNames()
	{
		Set<String> names = new HashSet<>();
		for (Map.Entry<String, Integer> e : bankItems.entrySet())
		{
			if (e.getValue() != null && e.getValue() > 0 && !isPlaceholderName(e.getKey()))
			{
				names.add(e.getKey());
			}
		}
		addOwned(names, inventoryItems);
		addOwned(names, equipmentItems);
		return names;
	}

	/** True when the bank entry is a zero-stock placeholder, not a real item. */
	private boolean isPlaceholderName(String name)
	{
		Integer id = bankNameToId.get(name);
		if (id == null)
		{
			return false;
		}
		ItemComposition composition = itemManager.getItemComposition(id);
		return composition != null && composition.getPlaceholderTemplateId() != -1
			&& composition.getPlaceholderId() != -1;
	}

	private static void addOwned(Set<String> names, Map<String, Integer> items)
	{
		for (Map.Entry<String, Integer> e : items.entrySet())
		{
			if (e.getValue() != null && e.getValue() > 0)
			{
				names.add(e.getKey());
			}
		}
	}

	private void updateReadyState(RunPlan plan)
	{
		List<ReadyItem> items = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		if (config.waitUntilReady())
		{
			for (RunPlan.Withdrawal w : plan.withdrawals)
			{
				int carried = inventoryItems.getOrDefault(w.itemName, 0)
					+ equipmentItems.getOrDefault(w.itemName, 0);
				if (runePouchCarried)
				{
					carried += runePouchItems.getOrDefault(w.itemName, 0);
				}
				// At a tool leprechaun or in the seed vault is close enough: the tip says where.
				boolean atSource = leprechaunItems.getOrDefault(w.itemName, 0) >= w.quantity
					|| seedVaultItems.getOrDefault(w.itemName, 0) >= w.quantity;
				int state = carried >= w.quantity ? ReadyItem.CARRIED
					: atSource ? ReadyItem.AT_SOURCE : ReadyItem.MISSING;
				String text = w.quantity + " x " + w.itemName;
				if (carried > 0 && carried < w.quantity)
				{
					text += " (" + carried + "/" + w.quantity + ")";
				}
				items.add(new ReadyItem(text, state));
				if (state == ReadyItem.MISSING)
				{
					missing.add((w.quantity - carried) + " x " + w.itemName);
				}
			}
		}
		readyItems = items;
		boolean allCarried = missing.isEmpty();
		// Arm the run when everything is carried. After that, consuming a teleport or ring
		// charge must not pull the guide back to "getting ready".
		if (allCarried)
		{
			runStarted = true;
		}
		// The run is over when no patch still needs a visit; a later run can prepare again.
		if (plan.route.isEmpty())
		{
			runStarted = false;
		}
		readyToRun = allCarried || runStarted;
		if (!readyToRun)
		{
			plan.tips.add(0, "Getting ready: take the items marked in your bank first.");
		}
	}

	private void recompute()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		int level = farmingLevel();

		Set<FarmPatch> unlocked = EnumSet.noneOf(FarmPatch.class);
		for (FarmPatch patch : FarmPatch.values())
		{
			FarmPatch.Requirement req = patch.getRequirement();
			boolean ok;
			switch (req)
			{
				case NONE:
					ok = true;
					break;
				case FARMING_45:
					ok = level >= 45;
					break;
				case FARMING_65:
					ok = level >= 65;
					break;
				case FARMING_85:
					ok = level >= 85;
					break;
				case MORYTANIA_ELITE:
					ok = config.harmonyUnlocked();
					break;
				default:
					ok = completedQuests.contains(req.questName());
			}
			if (ok)
			{
				unlocked.add(patch);
			}
		}

		Map<String, Integer> owned = new HashMap<>();
		bankItems.forEach((k, v) ->
		{
			if (!isPlaceholderName(k))
			{
				owned.put(k, v);
			}
		});
		inventoryItems.forEach((k, v) -> owned.merge(k, v, Integer::sum));
		equipmentItems.forEach((k, v) -> owned.merge(k, v, Integer::sum));
		// Seeds in the Farming Guild's vault and tools/compost at the leprechaun count as held,
		// but need a stop to collect them; the tips below say so.
		seedVaultItems.forEach((k, v) -> owned.merge(k, v, Integer::sum));
		leprechaunItems.forEach((k, v) -> owned.merge(k, v, Integer::sum));

		RunPlanner.Options options = new RunPlanner.Options();
		options.routeMode = RunPlanner.RouteMode.valueOf(config.routeOrder().name());
		options.customOrder = readList(config.customRoute(), FarmPatch.class);
		Player me = client.getLocalPlayer();
		if (me != null)
		{
			options.playerX = me.getWorldLocation().getX();
			options.playerY = me.getWorldLocation().getY();
			options.playerPlane = me.getWorldLocation().getPlane();
		}
		options.carried.putAll(inventoryItems);
		equipmentItems.forEach((k, v) -> options.carried.merge(k, v, Integer::sum));
		if (runePouchCarried)
		{
			runePouchItems.forEach((k, v) -> options.carried.merge(k, v, Integer::sum));
		}
		options.equipped.putAll(equipmentItems);
		options.pouchRunes.putAll(runePouchCarried ? runePouchItems : Collections.emptyMap());
		options.pouchCarried = runePouchCarried;
		options.pouchName = runePouchName;
		options.pouchCapacity = runePouchCapacity;
		options.runs.clear();
		options.runs.addAll(runs());
		options.payGardeners = config.payGardeners();
		options.useCompost = config.useCompost();
		options.bringRake = config.bringRake();
		options.onlyReady = config.onlyReady();
		options.herbYield = history.averageHerbYield();
		for (FarmPatch p : FarmPatch.values())
		{
			PatchTimers.Planting growing = timers.get(p);
			PatchTimers.Status status = timers.getStatus(p);
			Crop crop = growing != null ? growing.crop : timers.getStatusCrop(p);
			if (crop != null && status != PatchTimers.Status.DEAD
				&& status != PatchTimers.Status.STUMP && status != PatchTimers.Status.DISEASED)
			{
				options.growingCrops.put(p, crop);
				options.livingPatches.add(p);
			}
		}
		long nowMs = System.currentTimeMillis();
		for (FarmPatch p : FarmPatch.values())
		{
			if (!timers.needsVisit(p, nowMs))
			{
				PatchTimers.Planting pl = timers.get(p);
				options.notReady.put(p, pl == null ? -1L : pl.millisLeft(nowMs));
			}
		}
		options.doneThisRun.addAll(doneAt.keySet());
		for (FarmPatch p : doneAt.keySet())
		{
			PatchTimers.Planting pl = timers.get(p);
			if (pl != null && pl.crop != null)
			{
				options.plantedCrops.put(p, pl.crop);
			}
		}
		for (FarmPatch p : FarmPatch.values())
		{
			if (timers.getStatus(p) == PatchTimers.Status.DISEASED)
			{
				options.diseased.add(p);
			}
		}
		FarmingContract c = contract;
		options.contractCrop = c != null && !c.complete && config.plantContract() ? c.crop : null;
		options.disabledPatches = readSet(config.disabledPatches(), FarmPatch.class);
		double herbYield = history.averageHerbYield();
		herbProfit.clear();
		// Expected herbs per patch the OSRS Wiki way (see HerbYield), averaged over your herb patches.
		HerbYield.Setup setup = new HerbYield.Setup();
		setup.level = farmingLevel();
		setup.magicSecateurs = owned.containsKey("Magic secateurs");
		setup.farmingCape = client.getRealSkillLevel(Skill.FARMING) >= 99
			&& (owned.containsKey("Farming cape") || owned.containsKey("Farming cape(t)") || owned.containsKey("Max cape"));
		String compost = config.useCompost() ? bestCompost(owned) : null;
		setup.compostTier = HerbYield.compostTier(compost);
		long compostCost = compost == null ? 0
			: compost.startsWith("Bottomless") ? itemManager.getItemPrice(ULTRACOMPOST_ID) / 2
			: itemManager.getItemPrice(compostId(compost));
		List<FarmPatch> herbPatches = new ArrayList<>();
		for (FarmPatch p : unlocked)
		{
			if (p.getType() == PatchType.HERB && !options.disabledPatches.contains(p))
			{
				herbPatches.add(p);
			}
		}
		herbYieldByCrop.clear();
		for (Crop crop : Crop.values())
		{
			int herb = HerbPrices.grimyId(crop);
			if (herb <= 0 || !HerbYield.known(crop))
			{
				continue;
			}
			double sum = 0;
			for (FarmPatch p : herbPatches)
			{
				sum += HerbYield.expected(crop, setup, herbDiaryBonus(p), herbDiseaseFree(p));
			}
			double yield = herbPatches.isEmpty() ? HerbYield.expected(crop, setup, 0, false) : sum / herbPatches.size();
			herbYieldByCrop.put(crop, yield);
			long herbPrice = itemManager.getItemPrice(herb);
			long seedPrice = itemManager.getItemPrice(HerbPrices.seedId(crop));
			if (herbPrice > 0)
			{
				herbProfit.put(crop, HerbPrices.profitPerPatch(yield, herbPrice, seedPrice + compostCost));
			}
		}
		herbProfitNote = herbNote(setup, compost, herbPatches);
		if (config.profitFirst())
		{
			options.cropRank = HerbPrices.rankByProfit(herbProfit);
		}
		options.bankKnown = bankSeen;
		options.disabledCrops = readSet(config.disabledCrops(), Crop.class);
		for (FarmPatch p : FarmPatch.values())
		{
			PatchTimers.Status st = timers.getStatus(p);
			if (timers.get(p) != null || (st != null && st != PatchTimers.Status.EMPTY && st != PatchTimers.Status.WEEDS))
			{
				options.usedPatches.add(p);
			}
		}
		Teleport.Context ctx = options.teleports;
		ctx.magicLevel = client.getRealSkillLevel(Skill.MAGIC);
		ctx.standardSpellbook = client.getVarbitValue(VarbitID.SPELLBOOK) == 0;
		ctx.completedQuests = new HashSet<>(completedQuests);
		ctx.fairyRingsUnlocked = fairyRingsUnlocked;
		ctx.staffless = config.staffless();
		ctx.disabled = readSet(config.disabledTeleports(), Teleport.class);
		ctx.useSpells = config.useSpells();
		ctx.useTabs = config.useTabs();
		ctx.chainFairyRings = config.chainFairyRings();

		RunPlan plan = RunPlanner.plan(level, unlocked, owned, options);
		queryShortestPathItems(plan);
		addShortestPathItems(plan);
		addStorageTips(plan, options);
		updateReadyState(plan);
		lastPlan = plan;

		// Highlight what's still to take from the bank (first leg).
		Set<Integer> ids = new HashSet<>();
		for (RunPlan.Withdrawal w : plan.withdrawals)
		{
			int carried = inventoryItems.getOrDefault(w.itemName, 0) + equipmentItems.getOrDefault(w.itemName, 0);
			if (runePouchCarried)
			{
				carried += runePouchItems.getOrDefault(w.itemName, 0);
			}
			Integer bankId = bankNameToId.get(w.itemName);
			if (bankId != null && w.toWithdraw > 0 && carried < w.quantity)
			{
				ids.add(bankId);
			}
		}
		highlightIds = ids;

		Map<String, Integer> carried = new HashMap<>(inventoryItems);
		equipmentItems.forEach((k, v) -> carried.merge(k, v, Integer::sum));
		if (runePouchCarried)
		{
			runePouchItems.forEach((k, v) -> carried.merge(k, v, Integer::sum));
		}
		Map<String, Integer> nameToId = new HashMap<>(equipmentNameToId);
		inventoryNameToId.forEach(nameToId::putIfAbsent);
		bankNameToId.forEach(nameToId::putIfAbsent);
		// Items only in the seed vault, the leprechaun's store or the rune pouch still need a
		// picture in "Take with you".
		for (String name : leprechaunItems.keySet())
		{
			nameToId.putIfAbsent(name, itemIdByName(name));
		}
		for (String name : seedVaultItems.keySet())
		{
			nameToId.putIfAbsent(name, itemIdByName(name));
		}
		for (String name : runePouchItems.keySet())
		{
			nameToId.putIfAbsent(name, itemIdByName(name));
		}
		// The "Farm run" bank view: every item the run still needs, first leg and later ones.
		Set<Integer> bankTagIds = new HashSet<>();
		for (RunPlan.Withdrawal w : plan.withdrawals)
		{
			addBankTagId(bankTagIds, w, nameToId);
		}
		for (RunPlan.BankStop stop : plan.bankStops)
		{
			for (RunPlan.Withdrawal w : stop.withdrawals)
			{
				addBankTagId(bankTagIds, w, nameToId);
			}
		}
		for (RunPlan.Withdrawal w : plan.optionalItems)
		{
			addBankTagId(bankTagIds, w, nameToId);
		}
		if (farmBankTag.setItems(bankTagIds) && farmBankButton.isActive())
		{
			// Re-layout the widgets already in the tag view. Never reopen the tag here: that
			// rebuilds the bank and visibly jumps the list to the top before we can restore it.
			farmBankLayout.refresh();
		}

		// Details drawn over the Farm run bank tab: / N and "noted" under each item.
		Map<String, Integer> carriedForView = new HashMap<>(inventoryItems);
		equipmentItems.forEach((k, v) -> carriedForView.merge(k, v, Integer::sum));
		if (runePouchCarried)
		{
			runePouchItems.forEach((k, v) -> carriedForView.merge(k, v, Integer::sum));
		}
		List<RunPlan.Withdrawal> allLines = new ArrayList<>(plan.withdrawals);
		for (RunPlan.BankStop stop : plan.bankStops)
		{
			allLines.addAll(stop.withdrawals);
		}
		allLines.addAll(plan.optionalItems);
		Map<Integer, RunPlan.Withdrawal> linesById = new HashMap<>();
		for (RunPlan.Withdrawal w : allLines)
		{
			putBankLine(linesById, w, nameToId);
		}
		bankLinesById = linesById;
		bankLines = allLines;
		carriedForBank = carriedForView;
		if (farmBankButton.isActive())
		{
			farmBankLayout.refresh();
		}
		Set<String> worn = new HashSet<>(equipmentItems.keySet());
		Set<PatchType> runs = options.runs.isEmpty() ? EnumSet.noneOf(PatchType.class) : EnumSet.copyOf(options.runs);
		Set<Teleport> disabled = EnumSet.noneOf(Teleport.class);
		disabled.addAll(ctx.disabled);
		boolean useSpells = ctx.useSpells;
		boolean useTabs = ctx.useTabs;
		Set<Crop> offCrops = options.disabledCrops;
		Set<FarmPatch> offPatches = options.disabledPatches;
		Map<Crop, Integer> profitCopy = new EnumMap<>(Crop.class);
		Map<Crop, Double> yieldCopy = new EnumMap<>(herbYieldByCrop);
		String noteCopy = herbProfitNote;
		profitCopy.putAll(herbProfit);
		List<RunHistory.Run> historyRuns = history.runs();
		RunHistory.Run currentRun = history.current();
		long totalProfit = history.totalProfit();
		FarmingContract contractNow = contract;
		Map<String, Boolean> settings = new java.util.LinkedHashMap<>();
		settings.put("shortestPathLine", config.useShortestPath());
		settings.put("gameHintArrow", config.gameHintArrow());
		settings.put("showStepOverlay", config.showStepOverlay());
		settings.put("waitUntilReady", config.waitUntilReady());
		settings.put("showSocials", config.showSocials());
		settings.put("pathMinigameTeleports", config.pathMinigameTeleports());
		settings.put("highlightPatch", config.highlightPatch());
		settings.put("highlightGardener", config.highlightGardener());
		settings.put("pointerArrow", config.hintArrow());
		settings.put("highlightInventory", config.highlightInventory());
		settings.put("highlightBank", config.highlightBank());
		settings.put("showBankDetails", config.showBankDetails());
		settings.put("notifyReady", config.notifyReady());
		settings.put("chainFairyRings", config.chainFairyRings());
		settings.put("payGardeners", config.payGardeners());
		settings.put("useCompost", config.useCompost());
		settings.put("onlyReady", config.onlyReady());
		settings.put("activeTimersOnly", config.activeTimersOnly());
		settings.put("profitFirst", config.profitFirst());
		settings.put("plantContract", config.plantContract());
		settings.put("notifyDisease", config.notifyDisease());
		settings.put("showReadyOverlay", config.showReadyOverlay());
		settings.put("showHistory", config.showHistory());
		settings.put("bringRake", config.bringRake());
		settings.put("importTimeTracking", config.importTimeTracking());
		settings.put("useBoostedLevel", config.useBoostedLevel());
		settings.put("staffless", config.staffless());
		settings.put("harmonyUnlocked", config.harmonyUnlocked());
		settings.put("showTimers", config.showTimers());
		settings.put("showTravel", config.showTravel());
		String spNotice = shortestPathNotice();
		SwingUtilities.invokeLater(() ->
		{
			panel.setShortestPathNotice(spNotice);
			panel.setSettings(settings);
			panel.setRuns(runs);
			panel.setPicks(offCrops, offPatches);
			panel.setExtras(profitCopy, herbYield, historyRuns, currentRun, totalProfit, contractNow);
			panel.setHerbYields(yieldCopy, noteCopy);
			panel.setSources(new HashMap<>(seedVaultItems), new HashMap<>(leprechaunItems));
			panel.setPlan(plan, carried, worn, nameToId, disabled, useSpells, useTabs);
		});
	}

	// ================================================================ patch state sync

	/**
	 * Reads the state of every farming patch in the area you're standing in, straight from the
	 * game's farming variables (as RuneLite's Time Tracking does), and corrects the timers.
	 * Also learns your account's growth-tick offset when a patch changes stage in front of you.
	 */
	private void syncNearbyPatches()
	{
		Player player = client.getLocalPlayer();
		if (client.getGameState() != GameState.LOGGED_IN || player == null)
		{
			return;
		}
		WorldPoint wp = player.getWorldLocation();
		int region = FarmPatch.regionOf(wp.getX(), wp.getY());
		long now = System.currentTimeMillis();
		boolean changed = false;
		boolean clockChanged = false;
		for (FarmPatch patch : FarmPatch.values())
		{
			if (!patch.inRegion(region))
			{
				continue;
			}
			int value = client.getVarbitValue(patch.getVarbit());
			PatchStates.Decoded d = PatchStates.get().decode(patch.getType(), value);
			StepItems.Grown g = grownState(patch.getType(), value, d);
			if (g != null)
			{
				grownStates.put(patch, g);
			}
			else
			{
				grownStates.remove(patch);
			}
			if (d.checkHealth)
			{
				healthCheckDue.add(patch);
			}
			else
			{
				healthCheckDue.remove(patch);
			}

			Integer last = lastSeenValue.put(patch, value);
			if (last != null && last != value && d.state == PatchStates.State.GROWING && d.crop != null && d.stage == 0
				&& !(progress.containsKey(patch) && now - progress.get(patch).plantedAt < 60_000L))
			{
				PatchStates.Decoded before = PatchStates.get().decode(patch.getType(), last);
				if (before.state != PatchStates.State.GROWING || before.crop != d.crop || before.stage != 0)
				{
					planted(patch, d.crop); // just planted (whatever the chat said)
				}
			}
			if (last != null && last != value && d.crop != null
				&& (d.state == PatchStates.State.GROWING || d.state == PatchStates.State.READY))
			{
				PatchStates.Decoded before = PatchStates.get().decode(patch.getType(), last);
				if (before.crop == d.crop && before.state == PatchStates.State.GROWING && d.stage > before.stage)
				{
					// Learn per crop, not per patch type: potato cactus ticks every 10 minutes
					// while the cactus patch itself ticks every 80.
					clock.learn(d.crop, now);
					clockChanged = true;
				}
			}

			PatchTimers.Planting existing = timers.get(patch);
			boolean justPlanted = existing != null && now - existing.plantedAtMillis < 2 * 60_000L;
			changed |= apply(patch, d, existing, justPlanted, now, -1);
		}
		if (clockChanged)
		{
			configManager.setRSProfileConfiguration(HerbRunConfig.GROUP, CLOCK_KEY, clock.serialize());
		}
		if (changed)
		{
			saveTimers();
			dirty = true;
		}
	}

	/** changedAt < 0 means "seen live right now"; otherwise when that stage started (import). */
	private boolean apply(FarmPatch patch, PatchStates.Decoded d, PatchTimers.Planting existing,
		boolean justPlanted, long now, long changedAt)
	{
		long tolerance = GrowthClock.tickMillis(patch.getType()) + 5 * 60_000L;
		switch (d.state)
		{
			case GROWING:
			{
				if (d.crop == null)
				{
					return setStatusIfNew(patch, PatchTimers.Status.GROWING_UNKNOWN, null);
				}
				long est = changedAt < 0
					? clock.readyFromLive(d.crop, d.stage, now)
					: GrowthClock.readyFromChange(d.crop, d.stage, changedAt);
				if (existing == null || existing.crop != d.crop
					|| Math.abs(existing.readyAtMillis() - est) > tolerance)
				{
					timers.syncReadyAt(patch, d.crop, est);
					// A new crop/timer means its next maturity should still notify.
					notifiedReady.remove(patch);
					return true;
				}
				return false;
			}
			case READY:
			{
				Crop crop = d.crop != null ? d.crop : existing != null ? existing.crop : null;
				if (crop == null)
				{
					return false;
				}
				if (existing == null || existing.crop != crop || !existing.isReady(now))
				{
					timers.syncReadyAt(patch, crop, changedAt < 0 ? now : Math.min(changedAt, now));
					notifiedReady.remove(patch);
					return true;
				}
				return false;
			}
			case STUMP:
				return setStatusIfNew(patch, PatchTimers.Status.STUMP, d.crop);
			case EMPTY:
				// Weeds stage 0 = fully raked; anything above means weeds to rake.
				return !justPlanted && setStatusIfNew(patch,
					d.stage > 0 ? PatchTimers.Status.WEEDS : PatchTimers.Status.EMPTY, null);
			case DISEASED:
				return setStatusIfNew(patch, PatchTimers.Status.DISEASED, d.crop);
			case DEAD:
				return setStatusIfNew(patch, PatchTimers.Status.DEAD, d.crop);
			default:
				return false;
		}
	}

	private boolean setStatusIfNew(FarmPatch patch, PatchTimers.Status status, Crop crop)
	{
		if (timers.getStatus(patch) == status && timers.getStatusCrop(patch) == crop)
		{
			return false;
		}
		timers.setStatus(patch, status, crop);
		return true;
	}

	/**
	 * RuneLite's Time Tracking plugin saves each patch's last seen value and when it changed
	 * ("value:unixSeconds" under "regionId.varbitId"). Use it for patches we know nothing newer about.
	 */
	private void importTimeTracking()
	{
		if (!config.importTimeTracking() || configManager.getRSProfileKey() == null)
		{
			return;
		}
		long now = System.currentTimeMillis();
		boolean changed = false;
		for (FarmPatch patch : FarmPatch.values())
		{
			String raw = configManager.getRSProfileConfiguration(TIME_TRACKING_GROUP,
				patch.getRegionId() + "." + patch.getVarbit());
			if (raw == null)
			{
				continue;
			}
			String[] parts = raw.split(":");
			if (parts.length < 2)
			{
				continue;
			}
			int value;
			long changedAt;
			try
			{
				value = Integer.parseInt(parts[0]);
				changedAt = Long.parseLong(parts[1]) * 1000L;
			}
			catch (NumberFormatException e)
			{
				continue;
			}
			PatchTimers.Planting existing = timers.get(patch);
			if (existing != null && existing.plantedAtMillis >= changedAt - 60_000L)
			{
				continue; // what we have is newer
			}
			PatchStates.Decoded d = PatchStates.get().decode(patch.getType(), value);
			if (d.state == PatchStates.State.UNKNOWN || d.state == PatchStates.State.EMPTY)
			{
				continue;
			}
			changed |= apply(patch, d, existing, false, now, changedAt);
		}
		if (changed)
		{
			saveTimers();
			dirty = true;
		}
	}

	// ================================================================ navigation

	/** The stop the travel state is for, and whether you've already teleported for it. */
	private FarmPatch travelFor;
	private boolean travelDone;
	private WorldPoint travelLastTile;

	/**
	 * Remembers when you've done the teleport for the next stop, so you're not sent back to a
	 * fairy ring or spirit tree after arriving: you're at the destination ring, or a teleport
	 * just brought you much closer to the patch.
	 */
	private void updateTravelDone(FarmPatch next, RunPlan.RouteStep step, WorldPoint me, WorldPoint patch)
	{
		if (next != travelFor)
		{
			travelFor = next;
			travelDone = false;
		}
		if (next == null || me == null || patch == null)
		{
			travelLastTile = me;
			return;
		}
		if (step != null && step.teleport != null && step.teleport.getFairyCode() != null)
		{
			int[] dest = FairyRings.tile(step.teleport.getFairyCode());
			if (dest != null && me.getPlane() == 0 && dist(me, dest[0], dest[1]) <= 20)
			{
				travelDone = true;
			}
		}
		WorldPoint before = travelLastTile;
		if (before != null && (before.getPlane() != me.getPlane()
			|| dist(before, me.getX(), me.getY()) > TRAVEL_JUMP_TILES))
		{
			int was = dist(before, patch.getX(), patch.getY());
			int now = dist(me, patch.getX(), patch.getY());
			if (now <= MAX_TRAVEL_ARRIVAL_TILES && now < was - TRAVEL_JUMP_TILES)
			{
				travelDone = true;
			}
		}
		travelLastTile = me;
	}

	private static final int TRAVEL_JUMP_TILES = 30;
	private static final int MAX_TRAVEL_ARRIVAL_TILES = 200;

	private void updateNavigation()
	{
		if (lastPlan == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		long now = System.currentTimeMillis();
		FarmPatch next = null;
		int nextIndex = -1;
		for (int i = 0; i < lastPlan.route.size(); i++)
		{
			RunPlan.RouteStep step = lastPlan.route.get(i);
			if (timers.needsVisit(step.patch, now))
			{
				next = step.patch;
				nextIndex = i;
				break;
			}
		}
		// Wait until the first leg's items are carried: no line, arrow or patch outline before that.
		if (config.waitUntilReady() && !readyToRun)
		{
			next = null;
			nextIndex = -1;
		}
		if (next != nextPatch)
		{
			nextPatch = next;
			FarmPatch shown = next;
			SwingUtilities.invokeLater(() -> panel.setNextPatch(shown));
		}

		// A bank visit before the next patch: walk there first, unless you're already at it.
		Player bankPlayer = client.getLocalPlayer();
		for (RunPlan.BankStop bank : lastPlan.bankStops)
		{
			if (bank.beforeStep == nextIndex && bank.bankX >= 0 && bankPlayer != null
				&& (bankPlayer.getWorldLocation().getPlane() != bank.bankPlane
					|| dist(bankPlayer.getWorldLocation(), bank.bankX, bank.bankY) > 8))
			{
				WorldPoint bankTile = new WorldPoint(bank.bankX, bank.bankY, bank.bankPlane);
				navTarget = bankTile;
				navLeadObject = null;
				navLeadHint = "Bank first: " + bank.bank;
				navRingLabel = "Bank: " + bank.bank;
				sendShortestPath(bankTile);
				shortestPathResend = false;
				// leading = true: point at the bank itself, not at the next patch's gardener.
				updateGameHintArrow(next, null, true);
				return;
			}
		}

		WorldPoint target = next == null ? null : tileFor(next);
		WorldPoint patchTarget = target;
		String ringLabel = null;
		TileObject leadObj = null;
		String leadHint = null;
		Player player = client.getLocalPlayer();
		WorldPoint me = player == null ? null : player.getWorldLocation();
		RunPlan.RouteStep step = next == null ? null : stepFor(next);
		updateTravelDone(next, step, me, target);
		if (step != null && step.teleport != null && me != null && me.getPlane() == 0 && !travelDone)
		{
			String code = step.teleport.getFairyCode();
			if (code != null)
			{
				// Fairy ring step: lead you to a ring (the nearest one loaded, or the one named in the
				// directions), until you've teleported.
				int[] dest = FairyRings.tile(code);
				if ((dest == null || dist(me, dest[0], dest[1]) > 20) && dist(me, target.getX(), target.getY()) > 25)
				{
					TileObject ringObj = nearest(fairyRingObjects, me, 40);
					if (ringObj != null && dest != null && ringObj.getWorldLocation() != null
						&& dist(ringObj.getWorldLocation(), dest[0], dest[1]) <= 3)
					{
						ringObj = null; // that's the ring you arrive at, not the one to use
					}
					int i = lastPlan.route.indexOf(step);
					int[] ring = FairyRings.tile(FairyRings.startRing(step, i > 0 ? lastPlan.route.get(i - 1) : null));
					if (ringObj != null)
					{
						target = ringObj.getWorldLocation();
					}
					else if (ring != null && dist(me, ring[0], ring[1]) <= RING_LEAD_RANGE)
					{
						target = new WorldPoint(ring[0], ring[1], 0);
					}
					if (ringObj != null || (ring != null && dist(me, ring[0], ring[1]) <= RING_LEAD_RANGE))
					{
						leadObj = ringObj;
						ringLabel = "Fairy ring: dial " + code;
						leadHint = "Dial " + code + " for " + next.getDisplayName();
					}
				}
			}
			else if (step.teleport == Teleport.SPIRIT_TREE && dist(me, target.getX(), target.getY()) > 40)
			{
				// Spirit tree step: lead you to the nearest spirit tree (or the one by the Grand
				// Exchange after Varrock Teleport) and say which destination to pick.
				TileObject tree = nearest(spiritTreeObjects, me, 40);
				String where = spiritTreeDestination(next);
				if (tree != null)
				{
					target = tree.getWorldLocation();
					leadObj = tree;
					ringLabel = "Spirit tree: " + where;
					leadHint = "Spirit tree: pick " + where;
				}
				else if (dist(me, GE_SPIRIT_TREE.getX(), GE_SPIRIT_TREE.getY()) <= 120)
				{
					target = GE_SPIRIT_TREE;
					ringLabel = "Spirit tree: " + where;
					leadHint = "Spirit tree: pick " + where;
				}
			}
		}
		navLeadObject = leadObj;
		navLeadHint = leadHint;
		navTarget = target;
		navRingLabel = ringLabel;

		// Shortest Path plugin: always aim it at the patch itself, so it plans the whole trip
		// (including the fairy ring or spirit tree) with your own Shortest Path settings and draws
		// it. Post again whenever the tile changes.
		// After a teleport, ask Shortest Path again from where you landed: a route worked out
		// from the other side of the map can come back "could not be reached" and isn't retried.
		if (me != null)
		{
			if (lastPlayerTile != null && (lastPlayerTile.getPlane() != me.getPlane()
				|| dist(me, lastPlayerTile.getX(), lastPlayerTile.getY()) > 15))
			{
				shortestPathResend = true;
			}
			lastPlayerTile = me;
		}
		WorldPoint pathTarget = patchTarget;
		boolean aroundPatch = next != null;
		if (next != null && next.getType().hasGardenerPayment())
		{
			learnApproach(next);
			WorldPoint approach = Gardeners.sharedAcrossTypes(next) ? null : approachTiles.get(next);
			if (approach != null)
			{
				pathTarget = approach; // the patch itself can't be walked on
				aroundPatch = false;
			}
		}
		sendShortestPath(pathTarget, aroundPatch ? next.getType().patchSize() : 0);
		shortestPathResend = false;

		updateGameHintArrow(next, step, ringLabel != null);
	}

	private void sendShortestPath(WorldPoint target)
	{
		sendShortestPath(target, 0);
	}

	/**
	 * Sends Shortest Path one target, unless it's already been given the same one. When
	 * {@code patchSize} is positive the target is the centre of a patch, which can't be stood
	 * on: send the tiles around it and let Shortest Path go to whichever side is reachable.
	 */
	private void sendShortestPath(WorldPoint target, int patchSize)
	{
		if (!config.useShortestPath())
		{
			clearShortestPath();
			return;
		}
		if (target == null)
		{
			clearShortestPath();
			return;
		}
		if (!shortestPathResend && patchSize == shortestPathPatchSize
			&& java.util.Objects.equals(target, shortestPathTarget))
		{
			return;
		}
		Map<String, Object> data = new HashMap<>();
		data.put("target", patchSize > 0 ? tilesAround(target, patchSize) : target);
		// Our own settings for the line, applied by Shortest Path to this route only
		// (cleared again when we clear the path).
		Map<String, Object> overrides = new HashMap<>();
		overrides.put("useTeleportationMinigames", config.pathMinigameTeleports());
		data.put("config", overrides);
		eventBus.post(new PluginMessage("shortestpath", "path", data));
		shortestPathTarget = target;
		shortestPathPatchSize = patchSize;
	}

	/** The tiles around a patch (outside its footprint), for Shortest Path. */
	static java.util.Set<WorldPoint> tilesAround(WorldPoint centre, int size)
	{
		int minX = centre.getX() - size / 2;
		int minY = centre.getY() - size / 2;
		int maxX = minX + size - 1;
		int maxY = minY + size - 1;
		java.util.Set<WorldPoint> out = new java.util.LinkedHashSet<>();
		for (int x = minX - 1; x <= maxX + 1; x++)
		{
			for (int y = minY - 1; y <= maxY + 1; y++)
			{
				if (x < minX || x > maxX || y < minY || y > maxY)
				{
					out.add(new WorldPoint(x, y, centre.getPlane()));
				}
			}
		}
		return out;
	}

	/** The hint arrow waits until you're this close to a stop that starts with a teleport. */
	private static final int TELEPORT_FIRST_TILES = 60;
	/** Never point the hint arrow at something further away than this. */
	private static final int MAX_ARROW_TILES = 250;

	private static final int RING_LEAD_RANGE = 120;
	/** The spirit tree just north-west of the Grand Exchange. */
	private static final WorldPoint GE_SPIRIT_TREE = new WorldPoint(3185, 3508, 0);

	private static int dist(WorldPoint p, int x, int y)
	{
		return Math.max(Math.abs(p.getX() - x), Math.abs(p.getY() - y));
	}

	private static TileObject nearest(Set<TileObject> objects, WorldPoint me, int range)
	{
		TileObject best = null;
		int bestDist = range;
		for (TileObject o : objects)
		{
			WorldPoint w = o.getWorldLocation();
			int d = dist(me, w.getX(), w.getY());
			if (w.getPlane() == me.getPlane() && d <= bestDist)
			{
				bestDist = d;
				best = o;
			}
		}
		return best;
	}

	private static String spiritTreeDestination(FarmPatch patch)
	{
		switch (patch)
		{
			case GNOME_TREE:
			case GNOME_FRUIT:
				return "Gnome Stronghold";
			case TREE_GNOME_VILLAGE_FRUIT:
				return "Tree Gnome Village";
			default:
				return patch.getDisplayName();
		}
	}

	/**
	 * Remembers where this patch's gardener stands: always a walkable tile right beside the patch,
	 * so it's what the walking line aims at. Learned once per patch and saved.
	 */
	private void learnApproach(FarmPatch patch)
	{
		if (approachTiles.containsKey(patch))
		{
			return;
		}
		int wanted = Gardeners.idFor(patch);
		for (NPC npc : gardeners)
		{
			if (npc.getId() == wanted && npc.getWorldLocation() != null)
			{
				approachTiles.put(patch, npc.getWorldLocation());
				saveTileMap(APPROACH_KEY, approachTiles);
				return;
			}
		}
	}

	boolean showArrow()
	{
		return config.hintArrow();
	}

	/** Where to go right now (the patch, or the fairy ring / spirit tree first), or null. */
	WorldPoint getNavTarget()
	{
		return navTarget;
	}

	private static final int FAIRY_LOG_SCROLLBAR = InterfaceID.FairyringsLog.SCROLLBAR;
	private static final int FAIRY_LOG_CONTENTS_ID = InterfaceID.FairyringsLog.CONTENTS;
	private static final String FAIRY_LOG_TAG = "(Farm Run) ";
	/** The code the travel log was last scrolled to while it's open (scroll once, then leave it). */
	private String fairyLogScrolled;

	/**
	 * When the fairy ring travel log is open, mark the code for your next stop in it (green, with
	 * "(Farm Run)" in front) and scroll the list to it once, the way Shortest Path does.
	 */
	@Subscribe
	public void onClientTick(net.runelite.api.events.ClientTick tick)
	{
		Widget contents = client.getWidget(InterfaceID.FairyringsLog.CONTENTS);
		if (contents == null || contents.isHidden())
		{
			fairyLogScrolled = null;
			return;
		}
		String hint = navLeadHint;
		if (hint == null || !hint.startsWith("Dial ") || hint.length() < 8)
		{
			return;
		}
		String code = hint.substring(5, 8);
		Widget found = null;
		Widget faves = client.getWidget(InterfaceID.FairyringsLog.FAVES);
		if (faves != null && faves.getStaticChildren() != null)
		{
			found = findCode(faves.getStaticChildren(), code);
		}
		boolean inList = false;
		if (found == null && contents.getDynamicChildren() != null)
		{
			found = findCode(contents.getDynamicChildren(), code);
			inList = found != null;
		}
		if (found == null)
		{
			return;
		}
		found.setTextColor(0x00FF00);
		String text = found.getText();
		if (text != null && !text.contains(FAIRY_LOG_TAG))
		{
			found.setText(FAIRY_LOG_TAG + text);
		}
		if (inList && !code.equals(fairyLogScrolled))
		{
			fairyLogScrolled = code;
			int y = Math.max(0, Math.min(found.getRelativeY(), contents.getScrollHeight() - contents.getHeight()));
			contents.setScrollY(y);
			contents.revalidateScroll();
			client.runScript(ScriptID.UPDATE_SCROLLBAR, FAIRY_LOG_SCROLLBAR, FAIRY_LOG_CONTENTS_ID, y);
		}
	}

	/** The log entry showing this code (plain, or already tagged by us or Shortest Path). */
	private static Widget findCode(Widget[] widgets, String code)
	{
		for (Widget w : widgets)
		{
			if (w == null || w.getText() == null)
			{
				continue;
			}
			String t = w.getText().trim();
			if (t.equals(code) || t.endsWith(") " + code))
			{
				return w;
			}
		}
		return null;
	}

	/** The hint to show on screen: when the fairy ring interface is open, or you're at the ring/tree. */
	String getOnScreenLeadHint()
	{
		String hint = navLeadHint;
		if (hint == null)
		{
			return null;
		}
		if (hint.startsWith("Dial"))
		{
			for (int child = 0; child < 8; child++)
			{
				Widget w = client.getWidget(InterfaceID.FAIRYRINGS, child);
				if (w != null && !w.isHidden())
				{
					return hint;
				}
			}
		}
		TileObject o = navLeadObject;
		Player player = client.getLocalPlayer();
		if (hint.startsWith("Bank") && player != null && navTarget != null
			&& player.getWorldLocation().getPlane() == navTarget.getPlane()
			&& dist(player.getWorldLocation(), navTarget.getX(), navTarget.getY()) <= 20)
		{
			return hint;
		}
		if (o != null && player != null)
		{
			WorldPoint me = player.getWorldLocation();
			WorldPoint t = o.getWorldLocation();
			if (me.getPlane() == t.getPlane() && dist(me, t.getX(), t.getY()) <= 6)
			{
				return hint;
			}
		}
		return null;
	}

	/** The real fairy ring to outline, when heading to one and it's loaded. */
	TileObject getNavRingObject()
	{
		return navRingLabel == null ? null : navLeadObject;
	}

	/** The fairy ring to walk to first, or null when heading straight for the patch. */
	WorldPoint getNavRing()
	{
		return navRingLabel == null ? null : navTarget;
	}

	String getNavRingLabel()
	{
		return navRingLabel;
	}

	private void checkReadyNotifications()
	{
		if (!config.notifyReady())
		{
			return;
		}
		long now = System.currentTimeMillis();
		for (Map.Entry<FarmPatch, PatchTimers.Planting> e : timers.snapshot().entrySet())
		{
			if (e.getValue().isReady(now) && notifiedReady.add(e.getKey()))
			{
				// Only notify for patches that became ready while you were playing.
				if (now - e.getValue().readyAtMillis() < 60_000L)
				{
					notifier.notify(e.getKey().getDisplayName() + " (" + e.getValue().crop.getDisplayName() + ") is ready!");
				}
				dirty = true;
			}
		}
	}

	private void clearNavigation()
	{
		clearHintArrow();
		clearShortestPath();
	}

	private void clearHintArrow()
	{
		if (ourHintArrow)
		{
			// Only clear it if it's still ours (another plugin may have set its own since).
			boolean stillOurs = (hintNpc != null && hintNpc == client.getHintArrowNpc())
				|| (hintPoint != null && hintPoint.equals(client.getHintArrowPoint()));
			if (stillOurs)
			{
				client.clearHintArrow();
			}
			ourHintArrow = false;
		}
		hintNpc = null;
		hintPoint = null;
	}

	/** What the game hint arrow is on (one of the two), so it's only set again when that changes. */
	private NPC hintNpc;
	private WorldPoint hintPoint;

	/**
	 * The game's own hint arrow, on the next thing to go to: the gardener to pay
	 * (it follows them), the fairy ring or spirit tree to use, else the patch (or the gardener's
	 * spot). It's set only when that target changes, so it doesn't jump about, and not before a
	 * stop's teleport (nothing nearby to point at yet).
	 */
	private void updateGameHintArrow(FarmPatch next, RunPlan.RouteStep step, boolean leading)
	{
		Player player = client.getLocalPlayer();
		WorldPoint target = navTarget;
		if (!config.gameHintArrow() || next == null || target == null || player == null)
		{
			clearHintArrow();
			return;
		}
		WorldPoint me = player.getWorldLocation();
		int away = dist(me, target.getX(), target.getY());
		if (me.getPlane() != target.getPlane() || away > MAX_ARROW_TILES
			|| (!leading && !travelDone && step != null && step.teleport != null && away > TELEPORT_FIRST_TILES))
		{
			clearHintArrow();
			return;
		}
		NPC gardener = !leading && payLabel != null ? getGardenerToPay() : null;
		if (gardener != null)
		{
			if (gardener != hintNpc || !ourHintArrow)
			{
				client.setHintArrow(gardener);
				hintNpc = gardener;
				hintPoint = null;
				ourHintArrow = true;
			}
			return;
		}
		WorldPoint point = target;
		TileObject lead = leading ? navLeadObject : null;
		GameObject patchObj = leading ? null : objectFor(next);
		if (lead != null && lead.getWorldLocation() != null)
		{
			point = lead.getWorldLocation();
		}
		else if (patchObj != null && patchObj.getWorldLocation() != null)
		{
			point = patchObj.getWorldLocation();
		}
		if (!point.equals(hintPoint) || hintNpc != null || !ourHintArrow)
		{
			client.setHintArrow(point);
			hintPoint = point;
			hintNpc = null;
			ourHintArrow = true;
		}
	}

	/**
	 * A note to show when the walking line can't appear: the Shortest Path plugin isn't installed,
	 * or it's turned off. Null when it's there and on (or you've turned our line setting off).
	 */
	/** Cached: Shortest Path is installed, enabled and our walking setting is on. */
	boolean shortestPathActive()
	{
		if (shortestPathOn == null)
		{
			shortestPathOn = config.useShortestPath() && shortestPathNotice() == null;
		}
		return shortestPathOn;
	}

	String shortestPathNotice()
	{
		if (!config.useShortestPath())
		{
			return null;
		}
		for (Plugin p : pluginManager.getPlugins())
		{
			PluginDescriptor d = p.getClass().getAnnotation(PluginDescriptor.class);
			if (d != null && "Shortest Path".equals(d.name()))
			{
				return pluginManager.isPluginEnabled(p) ? null
					: "Turn on the Shortest Path plugin (it's installed but off) to get a walking line to each stop.";
			}
		}
		return "Install Shortest Path from the Plugin Hub to get a walking line to each stop. Farm Run Optimizer sends it every patch.";
	}

	@Subscribe
	public void onPluginChanged(net.runelite.client.events.PluginChanged event)
	{
		dirty = true; // Shortest Path installed or turned on/off: update the panel note
	}

	@Subscribe
	public void onExternalPluginsChanged(net.runelite.client.events.ExternalPluginsChanged event)
	{
		dirty = true;
	}

	private static final int COMPOST_ID = ItemID.BUCKET_COMPOST;
	private static final int SUPERCOMPOST_ID = ItemID.BUCKET_SUPERCOMPOST;
	private static final int ULTRACOMPOST_ID = ItemID.BUCKET_ULTRACOMPOST;
	// Achievement diary completion varbits (RuneLite's Varbits.DIARY_*).
	private static final int DIARY_KANDARIN_MEDIUM = VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE;
	private static final int DIARY_KANDARIN_HARD = VarbitID.KANDARIN_DIARY_HARD_COMPLETE;
	private static final int DIARY_KANDARIN_ELITE = VarbitID.KANDARIN_DIARY_ELITE_COMPLETE;
	private static final int DIARY_KOUREND_EASY = VarbitID.KOUREND_DIARY_EASY_COMPLETE;
	private static final int DIARY_KOUREND_HARD = VarbitID.KOUREND_DIARY_HARD_COMPLETE;

	/** Expected herbs per patch for each herb (OSRS Wiki method), for the panel. */
	private final Map<Crop, Double> herbYieldByCrop = new EnumMap<>(Crop.class);
	private String herbProfitNote;

	private static int compostId(String name)
	{
		return name.startsWith("Ultracompost") ? ULTRACOMPOST_ID : name.startsWith("Supercompost") ? SUPERCOMPOST_ID : COMPOST_ID;
	}

	/** The best compost you have anywhere (bottomless bucket first), or null. */
	private static String bestCompost(Map<String, Integer> owned)
	{
		for (String name : owned.keySet())
		{
			if (name.startsWith("Bottomless compost bucket") && owned.get(name) > 0)
			{
				return name;
			}
		}
		for (String c : new String[]{"Ultracompost", "Supercompost", "Compost"})
		{
			if (owned.getOrDefault(c, 0) > 0)
			{
				return c;
			}
		}
		return null;
	}

	/** The wiki's diary boost to the chance to save, for this herb patch. Client thread. */
	private int herbDiaryBonus(FarmPatch p)
	{
		switch (p)
		{
			case CATHERBY_HERB:
				return client.getVarbitValue(DIARY_KANDARIN_ELITE) == 1 ? 25
					: client.getVarbitValue(DIARY_KANDARIN_HARD) == 1 ? 17
					: client.getVarbitValue(DIARY_KANDARIN_MEDIUM) == 1 ? 10 : 0;
			case HOSIDIUS_HERB:
			case FARMING_GUILD_HERB:
				return client.getVarbitValue(DIARY_KOUREND_HARD) == 1 ? 10 : 0;
			default:
				return 0;
		}
	}

	/** Herb patches that never get diseased: Troll Stronghold, Weiss, Harmony, Hosidius with the easy Kourend diary. */
	private boolean herbDiseaseFree(FarmPatch p)
	{
		switch (p)
		{
			case TROLL_HERB:
			case WEISS_HERB:
			case HARMONY_HERB:
				return true;
			case HOSIDIUS_HERB:
				return client.getVarbitValue(DIARY_KOUREND_EASY) == 1;
			default:
				return false;
		}
	}

	/** One line under the herb profit list: what the numbers assume. */
	private String herbNote(HerbYield.Setup s, String compost, List<FarmPatch> patches)
	{
		List<String> parts = new ArrayList<>();
		parts.add("level " + s.level);
		parts.add(compost == null ? "no compost" : compost.startsWith("Bottomless") ? "bottomless bucket (ultracompost)" : compost.toLowerCase());
		if (s.magicSecateurs)
		{
			parts.add("magic secateurs");
		}
		if (s.farmingCape)
		{
			parts.add("Farming cape");
		}
		int bonus = 0;
		int safe = 0;
		for (FarmPatch p : patches)
		{
			bonus += herbDiaryBonus(p) > 0 ? 1 : 0;
			safe += herbDiseaseFree(p) ? 1 : 0;
		}
		if (bonus > 0)
		{
			parts.add(bonus + " diary-boosted patch" + (bonus == 1 ? "" : "es"));
		}
		if (safe > 0)
		{
			parts.add(safe + " disease-free");
		}
		return "OSRS Wiki method over your " + patches.size() + " herb patches: " + String.join(", ", parts)
			+ ". Counts the chance a patch dies; minus seed and compost.";
	}

	private void clearShortestPath()
	{
		if (shortestPathTarget != null)
		{
			eventBus.post(new PluginMessage("shortestpath", "clear"));
			shortestPathTarget = null;
			shortestPathPatchSize = 0;
		}
	}

	// ================================================================ saving

	private void loadTimers()
	{
		if (configManager.getRSProfileKey() == null)
		{
			return; // profile not known yet; try again next tick
		}
		timers.load(configManager.getRSProfileConfiguration(HerbRunConfig.GROUP, TIMERS_KEY));
		history.load(configManager.getRSProfileConfiguration(HerbRunConfig.GROUP, HISTORY_KEY));
		history.loadCurrent(configManager.getRSProfileConfiguration(HerbRunConfig.GROUP, CURRENT_RUN_KEY),
			System.currentTimeMillis());
		clock.load(configManager.getRSProfileConfiguration(HerbRunConfig.GROUP, CLOCK_KEY));
		timersLoaded = true;
		importTimeTracking();
		notifiedReady.clear();
		long now = System.currentTimeMillis();
		timers.snapshot().forEach((patch, planting) ->
		{
			if (planting.isReady(now))
			{
				notifiedReady.add(patch); // already ready before login: don't spam notifications
			}
		});
		dirty = true;
	}

	private void saveTimers()
	{
		configManager.setRSProfileConfiguration(HerbRunConfig.GROUP, TIMERS_KEY, timers.serialize());
	}

	/** Called from the panel's reset button (Swing thread). */
	private void resetTimers()
	{
		timers.load("");
		notifiedReady.clear();
		saveTimers();
		dirty = true;
	}

	// ================================================================ settings from the panel

	/** Reads a comma-separated list of enum names saved in config. Unknown names are skipped. */
	private static <E extends Enum<E>> Set<E> readSet(String raw, Class<E> type)
	{
		Set<E> out = EnumSet.noneOf(type);
		if (raw != null)
		{
			for (String part : raw.split(","))
			{
				try
				{
					if (!part.trim().isEmpty())
					{
						out.add(Enum.valueOf(type, part.trim()));
					}
				}
				catch (IllegalArgumentException ignored)
				{
					// renamed or removed in a newer version
				}
			}
		}
		return out;
	}

	/** Like {@link #readSet} but keeps the order the names were saved in. */
	private static <E extends Enum<E>> List<E> readList(String raw, Class<E> type)
	{
		List<E> out = new ArrayList<>();
		if (raw != null)
		{
			for (String part : raw.split(","))
			{
				try
				{
					if (!part.trim().isEmpty())
					{
						out.add(Enum.valueOf(type, part.trim()));
					}
				}
				catch (IllegalArgumentException ignored)
				{
					// renamed or removed in a newer version
				}
			}
		}
		return out;
	}

	/** Called by the up/down arrows on a stop card (Swing thread). */
	private void moveStop(FarmPatch patch, int delta)
	{
		if (lastPlan == null)
		{
			return;
		}
		List<FarmPatch> order = new ArrayList<>();
		for (RunPlan.RouteStep step : lastPlan.route)
		{
			if (step.anchor == null && !order.contains(step.patch))
			{
				order.add(step.patch);
			}
		}
		int from = order.indexOf(patch);
		int to = from + delta;
		if (from < 0 || to < 0 || to >= order.size())
		{
			return;
		}
		order.add(to, order.remove(from));
		StringBuilder sb = new StringBuilder();
		for (FarmPatch p : order)
		{
			if (sb.length() > 0)
			{
				sb.append(',');
			}
			sb.append(p.name());
		}
		configManager.setConfiguration(HerbRunConfig.GROUP, "customRoute", sb.toString());
		configManager.setConfiguration(HerbRunConfig.GROUP, "routeOrder", HerbRunConfig.RouteOrder.CUSTOM.name());
		dirty = true;
	}

	private <E extends Enum<E>> void toggleInSet(String key, String raw, E value, Class<E> type)
	{
		Set<E> set = readSet(raw, type);
		if (!set.remove(value))
		{
			set.add(value);
		}
		StringBuilder sb = new StringBuilder();
		for (E e : set)
		{
			sb.append(sb.length() > 0 ? "," : "").append(e.name());
		}
		configManager.setConfiguration(HerbRunConfig.GROUP, key, sb.toString());
	}

	/** Called from a Travel tile click (Swing thread). Saving fires ConfigChanged. */
	private void toggleTeleport(Teleport teleport)
	{
		toggleInSet("disabledTeleports", config.disabledTeleports(), teleport, Teleport.class);
	}

	private void toggleCrop(Crop crop)
	{
		toggleInSet("disabledCrops", config.disabledCrops(), crop, Crop.class);
	}

	private void togglePatch(FarmPatch patch)
	{
		toggleInSet("disabledPatches", config.disabledPatches(), patch, FarmPatch.class);
	}

	/** Called from the panel's run checkboxes. */
	private void setRun(PatchType type, boolean on)
	{
		configManager.setConfiguration(HerbRunConfig.GROUP, type.getConfigKey(), on);
	}

	/** Called from the panel's Settings section. Saving fires ConfigChanged. */
	private void setSetting(String key, boolean value)
	{
		configManager.setConfiguration(HerbRunConfig.GROUP, key, value);
	}

	private void setUseSpells(boolean use)
	{
		configManager.setConfiguration(HerbRunConfig.GROUP, "useSpells", use);
	}

	private void setUseTabs(boolean use)
	{
		configManager.setConfiguration(HerbRunConfig.GROUP, "useTabs", use);
	}
}
