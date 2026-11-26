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
import net.runelite.api.ItemContainer;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Perspective;
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
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@net.runelite.client.plugins.PluginDependency(net.runelite.client.plugins.banktags.BankTagsPlugin.class)
@PluginDescriptor(
	name = "Farm Run Optimizer",
	description = "Plans herb, tree, fruit tree and hardwood farm runs: what to bring, the fastest route, what to do at each patch, and every growth timer",
	tags = {"farming", "farm run", "herb run", "tree run", "fruit tree", "hardwood", "herbs", "patches", "timers", "fairy ring", "route", "planner"}
)
public class HerbRunPlugin extends Plugin
{
	// Raw container IDs (stable in the game cache): inventory, worn equipment, bank
	private static final int INVENTORY = 93;
	private static final int EQUIPMENT = 94;
	private static final int BANK = 95;

	private static final int QUEST_CHECK_INTERVAL_TICKS = 100;
	private static final int PATCH_SYNC_INTERVAL_TICKS = 2;
	/** Hint arrows only render inside the loaded area. */
	private static final int HINT_ARROW_RANGE = 50;
	private static final String TIMERS_KEY = "timers";
	private static final String CLOCK_KEY = "growthClock";
	private static final String TIME_TRACKING_GROUP = "timetracking";
	/** Standard spellbook = 0. */
	private static final int SPELLBOOK_VARBIT = 4070;
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
	private NowNextOverlay nowNextOverlay;
	@Inject
	private BankSupplyOverlay bankSupplyOverlay;
	@Inject
	private FarmBankButton farmBankButton;
	@Inject
	private FarmBankButtonOverlay farmBankButtonOverlay;
	@Inject
	private net.runelite.client.callback.ClientThread clientThread;
	@Inject
	private net.runelite.client.plugins.banktags.TagManager tagManager;
	@Inject
	private net.runelite.client.plugins.banktags.BankTagsService bankTagsService;
	private final RunBankTag runBankTag = new RunBankTag();
	/** Per bank item: how many to take and "noted" / "unnoted", for the bank labels. */
	private volatile Map<Integer, String> bankLabels = Collections.emptyMap();

	@Inject
	private SpellHighlightOverlay spellHighlightOverlay;

	@Inject
	private MinimapArrowOverlay minimapArrowOverlay;

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
			this::setSetting);
		panel.setOnClearHistory(this::clearHistory);
		panel.setSpriteManager(spriteManager);
		panel.setOnMoveStop(this::moveStop, () ->
		{
			configManager.unsetConfiguration(HerbRunConfig.GROUP, "customOrder");
			dirty = true;
		});
		panel.setOnHouseLocation(h ->
		{
			configManager.setConfiguration(HerbRunConfig.GROUP, "houseLocation", h);
			dirty = true;
		});
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
		overlayManager.add(nowNextOverlay);
		overlayManager.add(bankSupplyOverlay);
		overlayManager.add(farmBankButtonOverlay);
		clientThread.invokeLater(() -> farmBankButton.init()); // if the bank is already open
		tagManager.registerTag(RunBankTag.NAME, runBankTag);
		overlayManager.add(spellHighlightOverlay);
		overlayManager.add(minimapArrowOverlay);
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
		overlayManager.remove(nowNextOverlay);
		overlayManager.remove(bankSupplyOverlay);
		overlayManager.remove(farmBankButtonOverlay);
		clientThread.invokeLater(() -> farmBankButton.destroy());
		tagManager.unregisterTag(RunBankTag.NAME);
		overlayManager.remove(spellHighlightOverlay);
		overlayManager.remove(minimapArrowOverlay);
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
		nowText = null;
		nextText = null;
		supplyRows = Collections.emptyList();
	}

	// ================================================================ events

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGGED_IN:
				ticksSinceQuestCheck = QUEST_CHECK_INTERVAL_TICKS;
				dirty = true;
				break;
			case LOADING:
				patchObjects.clear();
				lastSeenValue.clear();
				break;
			case LOGIN_SCREEN:
				// Another account may log in next.
				bankItems.clear();
				bankNameToId.clear();
				bankSeen = false;
				timersLoaded = false;
				notifiedReady.clear();
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
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id == BANK)
		{
			snapshot(event.getItemContainer(), bankItems, bankNameToId);
			lastBankTick = client.getTickCount();
			bankSeen = true;
			dirty = true;
		}
		else if (id == INVENTORY)
		{
			Map<String, Integer> before = new HashMap<>(inventoryItems);
			snapshot(event.getItemContainer(), inventoryItems, inventoryNameToId);
			noticeHarvest(before);
			noticeItemsUsed();
			dirty = true;
		}
		else if (id == EQUIPMENT)
		{
			snapshot(event.getItemContainer(), equipmentItems, equipmentNameToId);
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
		progress.clear();
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
		for (NPC npc : gardeners)
		{
			if (Gardeners.isGardenerOf(patch, npc.getId()))
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
		if (!config.useShortestPath())
		{
			clearShortestPath();
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
		farmBankButton.onGameTick();
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

	/** True if this item should be highlighted for the current step. */
	boolean isStepItem(String name)
	{
		return name != null && (name.equals(stepItem) || stepExtras.contains(name));
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

	/** The NPC dialogue box (chat head on the left) and its text: RuneLite's InterfaceID.ChatLeft. */
	private static final int NPC_DIALOGUE_GROUP = 231;
	private static final int NPC_DIALOGUE_TEXT = 6;

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
		net.runelite.api.widgets.Widget w = client.getWidget(NPC_DIALOGUE_GROUP, NPC_DIALOGUE_TEXT);
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
			nowText = null;
			nextText = null;
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
		java.util.Set<String> extras = new java.util.HashSet<>();
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
		updateNowNext(focus, action, item, travelling);
		if (patch != stepPatch || !java.util.Objects.equals(action, stepAction))
		{
			stepPatch = patch;
			stepAction = action;
			SwingUtilities.invokeLater(() -> panel.setStepHint(patch, action, item));
		}
	}

	/** One line of the bank's supply list. */
	static final class SupplyRow
	{
		final String name;
		final int need;
		final int carried;
		final int inBank;
		final int itemId;

		SupplyRow(String name, int need, int carried, int inBank, int itemId)
		{
			this.name = name;
			this.need = need;
			this.carried = carried;
			this.inBank = inBank;
			this.itemId = itemId;
		}
	}

	private volatile List<SupplyRow> supplyRows = Collections.emptyList();
	private volatile int supplyBankStops;
	private volatile String nowText;
	private volatile String nextText;
	private volatile String stopCountText;

	List<SupplyRow> getSupplyRows()
	{
		return supplyRows;
	}

	int getSupplyBankStops()
	{
		return supplyBankStops;
	}

	/** True while the bank shows just the run's items. */
	boolean isRunTagOpen()
	{
		return RunBankTag.NAME.equals(bankTagsService.getActiveTag());
	}

	/** The label for a bank item ("10 noted", "6 unnoted", "\u2713"), or null if it's not for this run. */
	String getBankLabel(int itemId)
	{
		return bankLabels.get(itemId);
	}

	String getNowText()
	{
		return nowText;
	}

	String getNextText()
	{
		return nextText;
	}

	String getStopCountText()
	{
		return stopCountText;
	}

	/**
	 * The top-of-screen box: what to do now and the stop after. "Now" is the fairy ring code or
	 * spirit tree to pick, else the current step at a patch (with what to use), else the teleport
	 * or walk to the next stop. "Next" is the following stop that still needs a visit.
	 */
	private void updateNowNext(RunPlan.RouteStep focus, String action, String item, boolean travelling)
	{
		RunPlan plan = lastPlan;
		if (plan == null || (focus == null && nextPatch == null))
		{
			nowText = null;
			nextText = null;
			stopCountText = null;
			return;
		}
		long now = System.currentTimeMillis();
		RunPlan.RouteStep at = focus != null ? focus : stepFor(nextPatch);
		String here = at == null ? "" : at.patch.getDisplayName();
		String text;
		String lead = navLeadHint;
		if (lead != null)
		{
			text = lead;
		}
		else if (action != null && !travelling)
		{
			String what = payLabel != null && action.startsWith("Pay the gardener") ? payLabel
				: item != null && !action.toLowerCase().contains(item.toLowerCase()) ? action + " (" + item + ")" : action;
			text = what + " at " + here;
		}
		else if (travelling && at != null && at.teleportLabel != null)
		{
			text = at.teleportLabel + " to " + here;
		}
		else if (at != null && timers.needsVisit(at.patch, now))
		{
			text = "Go to " + here + " (harvest / check it)";
		}
		else
		{
			text = here.isEmpty() ? null : "Go to " + here;
		}
		nowText = text;

		// The stop after this one that still needs a visit.
		int index = at == null ? -1 : plan.route.indexOf(at);
		String after = null;
		int left = 0;
		for (int i = 0; i < plan.route.size(); i++)
		{
			RunPlan.RouteStep s = plan.route.get(i);
			boolean todo = timers.needsVisit(s.patch, now) || s == at;
			if (todo)
			{
				left++;
			}
			if (after == null && i > index && timers.needsVisit(s.patch, now))
			{
				after = s.patch.getDisplayName() + (s.teleport != null ? " (" + s.teleport.getLabel() + ")" : "");
			}
		}
		nextText = after != null ? after : index >= 0 ? "Last stop of this run" : null;
		int total = plan.route.size();
		stopCountText = total > 0 && index >= 0 ? "stop " + (index + 1) + " of " + total : null;
	}

	private volatile String spellToHighlight;

	/** The teleport spell to outline in the spellbook (e.g. "Falador Teleport"), or null. */
	String getSpellToHighlight()
	{
		return spellToHighlight;
	}

	// ================================================================ items, quests, planning

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
			int canonical = itemManager.canonicalize(item.getId());
			String name = itemManager.getItemComposition(canonical).getName();
			target.merge(name, item.getQuantity(), Integer::sum);
			nameToId.putIfAbsent(name, item.getId());
		}
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
			case FLOWER:
				return config.includeFlowers();
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

	/** The saved stop order: patch names separated by commas (unknown names skipped). */
	private static List<FarmPatch> readOrder(String saved)
	{
		List<FarmPatch> out = new ArrayList<>();
		if (saved == null || saved.isEmpty())
		{
			return out;
		}
		for (String name : saved.split(","))
		{
			try
			{
				out.add(FarmPatch.valueOf(name.trim()));
			}
			catch (IllegalArgumentException ignored)
			{
				// a patch from an older version
			}
		}
		return out;
	}

	/** Up / down on a stop card: swap it with its neighbour and save that as your order. */
	private void moveStop(FarmPatch patch, int delta)
	{
		RunPlan plan = lastPlan;
		if (plan == null)
		{
			return;
		}
		List<FarmPatch> order = new ArrayList<>();
		for (RunPlan.RouteStep s : plan.route)
		{
			order.add(s.patch);
		}
		int i = order.indexOf(patch);
		int j = i + delta;
		if (i < 0 || j < 0 || j >= order.size())
		{
			return;
		}
		order.set(i, order.get(j));
		order.set(j, patch);
		// Keep patches saved earlier that aren't on this run, so a later run still uses them.
		for (FarmPatch p : readOrder(config.customOrder()))
		{
			if (!order.contains(p))
			{
				order.add(p);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (FarmPatch p : order)
		{
			sb.append(sb.length() == 0 ? "" : ",").append(p.name());
		}
		configManager.setConfiguration(HerbRunConfig.GROUP, "customOrder", sb.toString());
		dirty = true;
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

		Map<String, Integer> owned = new HashMap<>(bankItems);
		inventoryItems.forEach((k, v) -> owned.merge(k, v, Integer::sum));
		equipmentItems.forEach((k, v) -> owned.merge(k, v, Integer::sum));

		RunPlanner.Options options = new RunPlanner.Options();
		options.runs.clear();
		options.runs.addAll(runs());
		options.payGardeners = config.payGardeners();
		options.useCompost = config.useCompost();
		options.bringRake = config.bringRake();
		options.onlyReady = config.onlyReady();
		options.keepRunsSeparate = config.keepRunsSeparate();
		options.customOrder = readOrder(config.customOrder());
		options.herbYield = history.averageHerbYield();
		for (FarmPatch p : FarmPatch.values())
		{
			PatchTimers.Planting growing = timers.get(p);
			Crop crop = growing != null ? growing.crop : timers.getStatusCrop(p);
			if (crop != null)
			{
				options.growingCrops.put(p, crop);
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
			PatchTimers.Status status = timers.getStatus(p);
			if (status == PatchTimers.Status.DISEASED)
			{
				options.diseased.add(p);
			}
			// Something growing, a stump or a dead crop: no weeds to rake there.
			if (timers.get(p) != null || status == PatchTimers.Status.GROWING_UNKNOWN
				|| status == PatchTimers.Status.STUMP || status == PatchTimers.Status.DISEASED
				|| status == PatchTimers.Status.DEAD)
			{
				options.weedFree.add(p);
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
		ctx.standardSpellbook = client.getVarbitValue(SPELLBOOK_VARBIT) == 0;
		ctx.completedQuests = new HashSet<>(completedQuests);
		ctx.fairyRingsUnlocked = fairyRingsUnlocked;
		ctx.staffless = config.staffless();
		ctx.disabled = readSet(config.disabledTeleports(), Teleport.class);
		ctx.useSpells = config.useSpells();
		ctx.useTabs = config.useTabs();
		ctx.chainFairyRings = config.chainFairyRings();
		ctx.houseLocation = config.houseLocation();

		RunPlan plan = RunPlanner.plan(level, unlocked, owned, options);
		lastPlan = plan;

		// Highlight what's still to take from the bank (first leg).
		Set<Integer> ids = new HashSet<>();
		for (RunPlan.Withdrawal w : plan.withdrawals)
		{
			int carried = inventoryItems.getOrDefault(w.itemName, 0) + equipmentItems.getOrDefault(w.itemName, 0);
			Integer bankId = bankNameToId.get(w.itemName);
			if (bankId != null && carried < w.quantity)
			{
				ids.add(bankId);
			}
		}
		highlightIds = ids;
		List<SupplyRow> rows = new ArrayList<>();
		for (RunPlan.Withdrawal w : plan.withdrawals)
		{
			int carriedQty = inventoryItems.getOrDefault(w.itemName, 0) + equipmentItems.getOrDefault(w.itemName, 0);
			Integer id = bankNameToId.get(w.itemName);
			if (id == null)
			{
				id = inventoryNameToId.get(w.itemName);
			}
			if (id == null)
			{
				id = equipmentNameToId.get(w.itemName);
			}
			rows.add(new SupplyRow(w.itemName, w.quantity, carriedQty, bankItems.getOrDefault(w.itemName, 0), id == null ? -1 : id));
		}
		supplyRows = rows;
		supplyBankStops = plan.bankStops.size();
		// Gardener payments can be handed over noted; compost, saplings, tools and teleport items
		// have to be unnoted to use. Stackable or unnotable items don't need a label.
		Set<String> payments = new HashSet<>();
		for (RunPlan.RouteStep st : plan.route)
		{
			if (st.paymentItem != null)
			{
				payments.add(st.paymentItem);
			}
			if (st.crop != null)
			{
				for (String[] extra : st.crop.getExtraPayments())
				{
					payments.add(extra[0]);
				}
			}
		}
		payments.remove("Coins");
		Map<Integer, String> labels = new HashMap<>();
		Set<Integer> tagIds = new HashSet<>();
		for (SupplyRow r : rows)
		{
			if (r.itemId <= 0)
			{
				continue;
			}
			tagIds.add(r.itemId);
			net.runelite.api.ItemComposition comp = itemManager.getItemComposition(r.itemId);
			boolean notable = comp != null && comp.getLinkedNoteId() != -1;
			boolean stackable = comp != null && comp.isStackable();
			String how = !notable ? null : payments.contains(r.name) ? "noted" : stackable ? null : "unnoted";
			int left = r.need - r.carried;
			labels.put(r.itemId, left <= 0 ? "\u2713" : left + (how == null ? "" : " " + how));
		}
		bankLabels = labels;
		if (runBankTag.set(tagIds) && isRunTagOpen())
		{
			// Refresh the open view with the new item list.
			bankTagsService.openBankTag(RunBankTag.NAME, net.runelite.client.plugins.banktags.BankTagsService.OPTION_NO_LAYOUT);
		}

		Map<String, Integer> carried = new HashMap<>(inventoryItems);
		equipmentItems.forEach((k, v) -> carried.merge(k, v, Integer::sum));
		Map<String, Integer> nameToId = new HashMap<>(equipmentNameToId);
		inventoryNameToId.forEach(nameToId::putIfAbsent);
		bankNameToId.forEach(nameToId::putIfAbsent);
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
		settings.put("showSocials", config.showSocials());
		settings.put("pathMinigameTeleports", config.pathMinigameTeleports());
		settings.put("highlightPatch", config.highlightPatch());
		settings.put("highlightGardener", config.highlightGardener());
		settings.put("pointerArrow", config.hintArrow());
		settings.put("highlightInventory", config.highlightInventory());
		settings.put("highlightBank", config.highlightBank());
		settings.put("notifyReady", config.notifyReady());
		settings.put("chainFairyRings", config.chainFairyRings());
		settings.put("payGardeners", config.payGardeners());
		settings.put("useCompost", config.useCompost());
		settings.put("onlyReady", config.onlyReady());
		settings.put("keepRunsSeparate", config.keepRunsSeparate());
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
		HouseLocation houseNow = config.houseLocation();
		SwingUtilities.invokeLater(() ->
		{
			panel.setShortestPathNotice(spNotice);
			panel.setSettings(settings);
			panel.setRuns(runs);
			panel.setPicks(offCrops, offPatches);
			panel.setExtras(profitCopy, herbYield, historyRuns, currentRun, totalProfit, contractNow);
			panel.setHerbYields(yieldCopy, noteCopy);
			panel.setHouseLocation(houseNow);
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
					clock.learn(patch.getType(), now);
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
		for (RunPlan.RouteStep step : lastPlan.route)
		{
			if (timers.needsVisit(step.patch, now))
			{
				next = step.patch;
				break;
			}
		}
		if (next != nextPatch)
		{
			nextPatch = next;
			FarmPatch shown = next;
			SwingUtilities.invokeLater(() -> panel.setNextPatch(shown));
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
		if (next != null && next.getType().hasGardenerPayment())
		{
			learnApproach(next);
			WorldPoint approach = Gardeners.sharedAcrossTypes(next) ? null : approachTiles.get(next);
			int[] walkTo = Gardeners.walkTo(next);
			if (approach == null && walkTo != null)
			{
				// Not learned yet: a walkable tile beside the patch, so Shortest Path can reach it.
				approach = new WorldPoint(walkTo[0], walkTo[1], walkTo[2]);
			}
			if (approach != null)
			{
				pathTarget = approach; // the patch itself can't be walked on
			}
		}
		if (config.useShortestPath() && (shortestPathResend || !java.util.Objects.equals(pathTarget, shortestPathTarget)))
		{
			if (pathTarget == null)
			{
				clearShortestPath();
			}
			else
			{
				Map<String, Object> data = new HashMap<>();
				data.put("target", pathTarget);
				// Our own settings for the line, applied by Shortest Path to this route only
				// (cleared again when we clear the path).
				Map<String, Object> overrides = new HashMap<>();
				overrides.put("useTeleportationMinigames", config.pathMinigameTeleports());
				data.put("config", overrides);
				eventBus.post(new PluginMessage("shortestpath", "path", data));
				shortestPathTarget = pathTarget;
			}
		}
		shortestPathResend = false;

		updateGameHintArrow(next, step, ringLabel != null);
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
		for (NPC npc : gardeners)
		{
			if (Gardeners.isGardenerOf(patch, npc.getId()) && npc.getWorldLocation() != null)
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

	/** The object to click first (fairy ring / spirit tree), or null. */
	TileObject getNavLeadObject()
	{
		return navLeadObject;
	}

	/** "Dial AIS for Nemus Retreat tree" / "Spirit tree: pick Gnome Stronghold", or null. */
	String getNavLeadHint()
	{
		return navLeadHint;
	}

	private static final int FAIRY_RING_INTERFACE = 398;
	// Fairy ring travel log (the list beside the dial), as used by the Shortest Path plugin.
	private static final int FAIRY_LOG_GROUP = 381;
	private static final int FAIRY_LOG_CONTENTS = 8;
	private static final int FAIRY_LOG_FAVES = 9;
	private static final int FAIRY_LOG_SCROLLBAR = 0x017d_00a6;
	private static final int FAIRY_LOG_CONTENTS_ID = 0x017d_0008;
	private static final int UPDATE_SCROLLBAR_SCRIPT = 72;
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
		net.runelite.api.widgets.Widget contents = client.getWidget(FAIRY_LOG_GROUP, FAIRY_LOG_CONTENTS);
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
		net.runelite.api.widgets.Widget found = null;
		net.runelite.api.widgets.Widget faves = client.getWidget(FAIRY_LOG_GROUP, FAIRY_LOG_FAVES);
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
			client.runScript(UPDATE_SCROLLBAR_SCRIPT, FAIRY_LOG_SCROLLBAR, FAIRY_LOG_CONTENTS_ID, y);
		}
	}

	/** The log entry showing this code (plain, or already tagged by us or Shortest Path). */
	private static net.runelite.api.widgets.Widget findCode(net.runelite.api.widgets.Widget[] widgets, String code)
	{
		for (net.runelite.api.widgets.Widget w : widgets)
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
				net.runelite.api.widgets.Widget w = client.getWidget(FAIRY_RING_INTERFACE, child);
				if (w != null && !w.isHidden())
				{
					return hint;
				}
			}
		}
		TileObject o = navLeadObject;
		Player player = client.getLocalPlayer();
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

	/** The loaded fairy ring object near a ring's map position, or null. */
	private TileObject ringObjectNear(int x, int y)
	{
		TileObject best = null;
		int bestDist = 8;
		for (TileObject o : fairyRingObjects)
		{
			WorldPoint w = o.getWorldLocation();
			int d = Math.max(Math.abs(w.getX() - x), Math.abs(w.getY() - y));
			if (w.getPlane() == 0 && d <= bestDist)
			{
				bestDist = d;
				best = o;
			}
		}
		return best;
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
	public void onWidgetLoaded(net.runelite.api.events.WidgetLoaded event)
	{
		if (event.getGroupId() == net.runelite.api.gameval.InterfaceID.BANKMAIN)
		{
			farmBankButton.init(); // Romte1's Farm run button in the bank title bar
		}
	}

	/** The bank finished (re)building: space out the items while the Farm run tag is open. */
	@Subscribe
	public void onScriptPostFired(net.runelite.api.events.ScriptPostFired event)
	{
		if (event.getScriptId() == net.runelite.api.ScriptID.BANKMAIN_FINISHBUILDING
			&& config.bankSupplyTab() && isRunTagOpen())
		{
			// After Bank Tags has laid out its own view.
			clientThread.invokeAtTickEnd(() -> RunBankLayout.apply(client));
		}
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

	private static final int COMPOST_ID = 6032;
	private static final int SUPERCOMPOST_ID = 6034;
	private static final int ULTRACOMPOST_ID = 21483;
	// Achievement diary completion varbits (RuneLite's Varbits.DIARY_*).
	private static final int DIARY_KANDARIN_MEDIUM = 4476;
	private static final int DIARY_KANDARIN_HARD = 4477;
	private static final int DIARY_KANDARIN_ELITE = 4478;
	private static final int DIARY_KOUREND_EASY = 7925;
	private static final int DIARY_KOUREND_HARD = 7927;

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
