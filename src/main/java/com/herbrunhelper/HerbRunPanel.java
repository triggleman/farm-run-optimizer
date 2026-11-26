package com.herbrunhelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Sidebar panel: run toggles, ready summary, collapsible patch timers, the "take with you"
 * grid, wear tips, a card per stop (gold = next) with its to-do list, bank stop cards,
 * the Travel section and a footer credit.
 */
public class HerbRunPanel extends PluginPanel
{
	/** Shown in the footer so it's easy to tell which build is running. */
	static final String VERSION = "1.20.0";
	private static final String CREDIT = "<html><center>Made by ToadFx<br>v" + VERSION + "</center></html>";

	private static final Color GREEN = new Color(90, 200, 90);
	private static final Color YELLOW = new Color(255, 210, 70);
	private static final Color GOLD = new Color(255, 200, 60);
	private static final Color ORANGE = ColorScheme.BRAND_ORANGE;
	private static final Color MUTED = new Color(150, 150, 150);
	private static final Color RED = new Color(230, 80, 70);
	private static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;

	private static final int DRAMEN_STAFF_ICON = 772;
	private static final int LAW_RUNE_ICON = 563;

	/** Items worth wearing instead of carrying, with the tip to show. */
	private static final String[][] WEARABLE_TIPS = {
		{"Magic secateurs", "Wield your magic secateurs for +10% herbs."},
		{"Skills necklace", "Wear your skills necklace to save an inventory slot."},
		{"Farming cape", "Wear your farming cape to save an inventory slot."},
		{"Explorer's ring", "Wear your Explorer's ring to save an inventory slot."},
		{"Ardougne cloak", "Wear your Ardougne cloak to save an inventory slot."},
		{"Dramen staff", "Wield your dramen staff to use fairy rings."},
		{"Lunar staff", "Wield your lunar staff to use fairy rings."},
	};

	private final ItemManager itemManager;
	private final PatchTimers timers;
	private final Runnable onResetTimers;
	private final Consumer<Teleport> onToggleTeleport;
	private final Consumer<Boolean> onUseSpells;
	private final Consumer<Boolean> onUseTabs;
	private final Consumer<Crop> onToggleCrop;
	private final Consumer<FarmPatch> onTogglePatch;
	private final BiConsumer<String, Boolean> onSetting;
	private Map<String, Boolean> settings = Collections.emptyMap();
	private boolean settingsExpanded;
	private FarmPatch hintPatch;
	private String hintAction;
	private String hintItem;

	private final JPanel runsSection = new JPanel();
	/** A note about the Shortest Path plugin (not installed / turned off), or empty. */
	private final JPanel noticeSection = new JPanel();
	private String shortestPathNotice;
	private final JPanel socialsRow = SocialLinks.row();
	private final JPanel liveSection = new JPanel();
	private BiConsumer<PatchType, Boolean> onRunChanged;
	private final JPanel planSection = new JPanel();
	private final Timer refresh;

	private RunPlan plan;
	private Map<String, Integer> carried = Collections.emptyMap();
	private Set<String> worn = Collections.emptySet();
	private Map<String, Integer> nameToId = Collections.emptyMap();
	private FarmPatch nextPatch;
	private Set<Teleport> disabled = EnumSet.noneOf(Teleport.class);
	private boolean useSpells = true;
	private boolean useTabs = true;

	private final Map<PatchType, Boolean> expanded = new EnumMap<>(PatchType.class);
	private boolean travelExpanded;
	private boolean historyExpanded;
	private Map<Crop, Integer> herbProfit = new java.util.EnumMap<>(Crop.class);
	private double herbYield = HerbPrices.DEFAULT_YIELD;
	/** Expected herbs a patch for each herb (OSRS Wiki method), and what that assumes. */
	private Map<Crop, Double> herbYields = new java.util.EnumMap<>(Crop.class);
	private String herbNote;
	private List<RunHistory.Run> historyRuns = new ArrayList<>();
	private RunHistory.Run currentRun;
	private long totalProfit;
	private FarmingContract contract;
	private Runnable onClearHistory = () -> { };
	private net.runelite.client.game.SpriteManager spriteManager;
	private boolean showAllTravel;
	private final Map<PatchType, Boolean> runExpanded = new EnumMap<>(PatchType.class);
	private Set<Crop> offCrops = EnumSet.noneOf(Crop.class);
	private Set<FarmPatch> offPatches = EnumSet.noneOf(FarmPatch.class);
	private Set<PatchType> runs = EnumSet.noneOf(PatchType.class);

	public HerbRunPanel(ItemManager itemManager, PatchTimers timers, Runnable onResetTimers,
		BiConsumer<PatchType, Boolean> onRunChanged, Consumer<Teleport> onToggleTeleport,
		Consumer<Boolean> onUseSpells, Consumer<Boolean> onUseTabs, Consumer<Crop> onToggleCrop,
		Consumer<FarmPatch> onTogglePatch, BiConsumer<String, Boolean> onSetting)
	{
		this.onSetting = onSetting;
		this.onRunChanged = onRunChanged;
		this.onToggleCrop = onToggleCrop;
		this.onTogglePatch = onTogglePatch;
		this.itemManager = itemManager;
		this.timers = timers;
		this.onResetTimers = onResetTimers;
		this.onToggleTeleport = onToggleTeleport;
		this.onUseSpells = onUseSpells;
		this.onUseTabs = onUseTabs;
		for (PatchType t : PatchType.values())
		{
			expanded.put(t, true);
			// no entry = automatic: open while the run is on
		}

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(6, 6, 6, 6));

		Column root = new Column();

		JLabel title = new JLabel(new ImageIcon(ImageUtil.loadImageResource(HerbRunPanel.class, "panel_banner.png")));
		title.setToolTipText("Farm Run Optimizer");
		title.setHorizontalAlignment(SwingConstants.CENTER);
		root.addRow(title, 4);

		noticeSection.setOpaque(false);
		noticeSection.setLayout(new BorderLayout());
		root.addRow(noticeSection, 0);
		liveSection.setOpaque(false);
		runsSection.setOpaque(false);
		planSection.setOpaque(false);
		root.addRow(liveSection, 4);
		root.addRow(runsSection, 4);
		root.addRow(planSection, 0);

		root.addRow(new JSeparator(), 4);
		root.addRow(socialsRow, 4);
		JLabel credit = new JLabel(CREDIT, SwingConstants.CENTER);
		credit.setFont(FontManager.getRunescapeSmallFont());
		credit.setForeground(MUTED);
		root.addRow(credit, 0);

		add(root, BorderLayout.NORTH);

		Column msg = new Column();
		msg.addRow(wrapped("Log in to see your farm run.", MUTED), 0);
		replace(liveSection, msg);
		refresh = new Timer(1000, e -> rebuildLive());
		refresh.start();
	}

	// ================================================================ called by the plugin

	void stop()
	{
		refresh.stop();
	}

	void setSettings(Map<String, Boolean> settings)
	{
		this.settings = settings;
		boolean socials = setting("showSocials");
		if (socialsRow.isVisible() != socials)
		{
			socialsRow.setVisible(socials);
			revalidate();
		}
	}

	/** Shown at the top while the Shortest Path plugin isn't installed or is off; null hides it. */
	void setShortestPathNotice(String notice)
	{
		if (java.util.Objects.equals(notice, shortestPathNotice))
		{
			return;
		}
		shortestPathNotice = notice;
		noticeSection.removeAll();
		if (notice != null)
		{
			JPanel card = new JPanel(new BorderLayout());
			card.setBackground(new Color(58, 50, 28));
			card.setBorder(new EmptyBorder(5, 6, 5, 6));
			card.add(wrapped("\u26A0 " + esc(notice), ORANGE), BorderLayout.CENTER);
			JPanel gap = new JPanel(new BorderLayout());
			gap.setOpaque(false);
			gap.setBorder(new EmptyBorder(0, 0, 4, 0));
			gap.add(card, BorderLayout.CENTER);
			noticeSection.add(gap, BorderLayout.CENTER);
		}
		noticeSection.revalidate();
		noticeSection.repaint();
	}

	/** What the stop you're at needs right now, shown in a banner above the stops. */
	void setStepHint(FarmPatch patch, String action, String item)
	{
		this.hintPatch = patch;
		this.hintAction = action;
		this.hintItem = item;
		rebuildPlan();
	}

	private boolean setting(String key)
	{
		return settings.getOrDefault(key, true);
	}

	void setPicks(Set<Crop> offCrops, Set<FarmPatch> offPatches)
	{
		this.offCrops = offCrops;
		this.offPatches = offPatches;
	}

	void setRuns(Set<PatchType> runs)
	{
		this.runs = runs;
	}

	void setPlan(RunPlan plan, Map<String, Integer> carried, Set<String> worn, Map<String, Integer> nameToId,
		Set<Teleport> disabled, boolean useSpells, boolean useTabs)
	{
		this.plan = plan;
		this.carried = carried;
		this.worn = worn;
		this.nameToId = nameToId;
		this.disabled = disabled;
		this.useSpells = useSpells;
		this.useTabs = useTabs;
		rebuildLive();
		rebuildPlan();
	}

	private java.util.function.BiConsumer<FarmPatch, Integer> onMoveStop = (p, d) -> { };
	private Runnable onResetOrder = () -> { };

	void setOnMoveStop(java.util.function.BiConsumer<FarmPatch, Integer> listener, Runnable reset)
	{
		onMoveStop = listener;
		onResetOrder = reset;
	}

	private HouseLocation houseLocation = HouseLocation.NONE;
	private Consumer<HouseLocation> onHouseLocation = h -> { };

	void setHouseLocation(HouseLocation h)
	{
		houseLocation = h == null ? HouseLocation.NONE : h;
	}

	void setOnHouseLocation(Consumer<HouseLocation> listener)
	{
		onHouseLocation = listener;
	}

	void setSpriteManager(net.runelite.client.game.SpriteManager sm)
	{
		this.spriteManager = sm;
	}

	/** The spellbook picture for a teleport spell (RuneLite SpriteID), or -1. */
	private static int spellSprite(Teleport t)
	{
		if (t == null)
		{
			return -1;
		}
		switch (t)
		{
			case VARROCK_SPELL:
				return 27;
			case LUMBRIDGE_SPELL:
				return 30;
			case FALADOR_SPELL:
				return 33;
			case CAMELOT_SPELL:
				return 37;
			case HOUSE_SPELL:
				return 355;
			case ARDOUGNE_SPELL:
				return 54;
			case TROLLHEIM_SPELL:
				return 323;
			case KOUREND_SPELL:
				return 360;
			case CIVITAS_SPELL:
				return 367;
			default:
				return -1;
		}
	}

	/** Loads a spell picture into a label (sprites load on the client thread). */
	private void setSprite(JLabel label, int spriteId, float alpha, boolean lock)
	{
		if (spriteManager == null || spriteId < 0)
		{
			return;
		}
		spriteManager.getSpriteAsync(spriteId, 0, img ->
			SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(dimIcon(img, alpha, 0, lock)))));
	}

	void setOnClearHistory(Runnable r)
	{
		this.onClearHistory = r;
	}

	void setExtras(Map<Crop, Integer> herbProfit, double herbYield, List<RunHistory.Run> runs,
		RunHistory.Run current, long totalProfit, FarmingContract contract)
	{
		this.herbProfit = herbProfit;
		this.herbYield = herbYield;
		this.historyRuns = runs;
		this.currentRun = current;
		this.totalProfit = totalProfit;
		this.contract = contract;
	}

	void setHerbYields(Map<Crop, Double> yields, String note)
	{
		this.herbYields = yields;
		this.herbNote = note;
	}

	void setNextPatch(FarmPatch next)
	{
		this.nextPatch = next;
		rebuildPlan();
	}

	// ================================================================ live section (every second)

	private void rebuildLive()
	{
		if (plan == null)
		{
			return;
		}
		long now = System.currentTimeMillis();
		Map<FarmPatch, PatchTimers.Planting> planted = timers.snapshot();
		Column col = new Column();

		Map<PatchType, List<FarmPatch>> byType = new EnumMap<>(PatchType.class);
		for (PatchType type : PatchType.values())
		{
			List<FarmPatch> patches = patchesOfType(type, planted);
			if (!patches.isEmpty())
			{
				byType.put(type, patches);
			}
		}
		byType.forEach((type, patches) -> col.addRow(summaryRow(type, patches, planted, now), 2));

		if (!byType.isEmpty() && setting("showTimers"))
		{
			col.addRow(sectionTitle("Patch timers"), 2);
			byType.forEach((type, patches) -> col.addRow(timerGroup(type, patches, planted, now), 4));
		}

		if (timers.hasAnything())
		{
			JButton reset = new JButton("Reset timers");
			reset.setFont(FontManager.getRunescapeSmallFont());
			reset.setFocusPainted(false);
			reset.addActionListener(e ->
			{
				if (JOptionPane.showConfirmDialog(this, "Clear every patch timer?", "Reset timers",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
				{
					onResetTimers.run();
					rebuildLive();
				}
			});
			col.addRow(reset, 6);
		}

		replace(liveSection, col);
	}

	private List<FarmPatch> patchesOfType(PatchType type, Map<FarmPatch, PatchTimers.Planting> planted)
	{
		List<FarmPatch> out = new ArrayList<>();
		for (FarmPatch p : FarmPatch.values())
		{
			if (p.getType() != type)
			{
				continue;
			}
			boolean inRoute = plan.route.stream().anyMatch(s -> s.patch == p);
			if (inRoute || planted.containsKey(p) || timers.getStatus(p) != null)
			{
				out.add(p);
			}
		}
		return out;
	}

	private JComponent summaryRow(PatchType type, List<FarmPatch> patches,
		Map<FarmPatch, PatchTimers.Planting> planted, long now)
	{
		int ready = 0;
		long soonest = Long.MAX_VALUE;
		int tracked = 0;
		for (FarmPatch p : patches)
		{
			PatchTimers.Planting pl = planted.get(p);
			if (pl == null)
			{
				PatchTimers.Status st = timers.getStatus(p);
				if (st != null)
				{
					tracked++;
					if (st != PatchTimers.Status.GROWING_UNKNOWN)
					{
						ready++;
					}
				}
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
		String status;
		Color color;
		if (ready > 0)
		{
			status = ready + " need you";
			color = GREEN;
		}
		else if (soonest != Long.MAX_VALUE)
		{
			status = "ready in " + PatchTimers.formatDuration(soonest);
			color = YELLOW;
		}
		else
		{
			status = tracked > 0 ? "growing" : "not planted yet";
			color = MUTED;
		}
		return row(label(type.getDisplayName() + ":", Color.WHITE, false), label(status, color, false));
	}

	private JComponent timerGroup(PatchType type, List<FarmPatch> allPatches,
		Map<FarmPatch, PatchTimers.Planting> planted, long now)
	{
		// Only patches with something in them (growing, ready, diseased, dead, a stump), unless
		// the setting says to show them all.
		List<FarmPatch> patches = new ArrayList<>();
		for (FarmPatch p : allPatches)
		{
			PatchTimers.Status st = timers.getStatus(p);
			boolean active = planted.get(p) != null || (st != null && st != PatchTimers.Status.WEEDS
				&& st != PatchTimers.Status.EMPTY);
			if (active || !setting("activeTimersOnly"))
			{
				patches.add(p);
			}
		}
		int hidden = allPatches.size() - patches.size();
		int due = 0;
		for (FarmPatch p : patches)
		{
			if (timers.needsVisit(p, now))
			{
				due++;
			}
		}
		boolean open = expanded.get(type);

		Column group = new Column();
		JLabel left = label((open ? "▼  " : "▶  ") + type.getDisplayName(), Color.WHITE, true);
		JLabel right = label(due + " of " + patches.size() + " due", due > 0 ? GREEN : MUTED, false);
		JPanel header = row(left, right);
		header.setOpaque(true);
		header.setBackground(CARD);
		header.setBorder(new EmptyBorder(4, 6, 4, 6));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				expanded.put(type, !open);
				rebuildLive();
			}
		});
		group.addRow(header, 0);

		if (open)
		{
			Column rows = new Column();
			rows.setBorder(new EmptyBorder(3, 8, 3, 6));
			for (FarmPatch p : patches)
			{
				rows.addRow(row(patchNameLabel(p, planted.get(p)),
					patchStatus(p, planted.get(p), now)), 2);
			}
			if (hidden > 0)
			{
				rows.addRow(label(patches.isEmpty() ? "Nothing planted yet" : "+" + hidden + " empty patch"
					+ (hidden == 1 ? "" : "es") + " hidden", MUTED, false), 0);
			}
			group.addRow(rows, 0);
		}
		return group;
	}

	/** Patch name with what's growing there on a second, dimmer line. */
	private JLabel patchNameLabel(FarmPatch p, PatchTimers.Planting pl)
	{
		Crop crop = pl != null ? pl.crop : timers.getStatusCrop(p);
		String color = p == nextPatch ? "#ffc83c" : "#ffffff";
		String html = "<html><font color='" + color + "'>" + esc(shortName(p)) + "</font>"
			+ (crop == null ? "" : "<br><font color='#8fbf8f'>" + esc(crop.getDisplayName()) + "</font>")
			+ "</html>";
		JLabel l = new JLabel(html);
		l.setFont(FontManager.getRunescapeSmallFont());
		return l;
	}

	private JLabel patchStatus(FarmPatch p, PatchTimers.Planting pl, long now)
	{
		if (pl != null)
		{
			JLabel l;
			if (pl.isReady(now))
			{
				l = label(!p.getType().isTreeLike() ? "ready to pick" : "fully grown", GREEN, false);
			}
			else
			{
				l = label("ready in " + PatchTimers.formatDuration(pl.millisLeft(now)), Color.WHITE, false);
			}
			l.setToolTipText(pl.crop.getDisplayName());
			return l;
		}
		PatchTimers.Status st = timers.getStatus(p);
		if (st == null)
		{
			JLabel l = label("not tracked", MUTED, false);
			l.setToolTipText("Walk past this patch or plant it to start tracking");
			return l;
		}
		Color c = st == PatchTimers.Status.DISEASED || st == PatchTimers.Status.DEAD ? RED
			: st == PatchTimers.Status.GROWING_UNKNOWN ? Color.WHITE : ORANGE;
		JLabel l = label(st.getLabel(), c, false);
		Crop sc = timers.getStatusCrop(p);
		if (sc != null)
		{
			l.setToolTipText(sc.getDisplayName());
		}
		return l;
	}

	// ================================================================ plan section (on change)

	private void rebuildPlan()
	{
		if (plan == null)
		{
			return;
		}
		long now = System.currentTimeMillis();
		Column col = new Column();
		col.setBorder(new EmptyBorder(6, 0, 4, 0));

		StringBuilder unlocks = new StringBuilder("Farming " + plan.farmingLevel);
		plan.next.forEach((t, c) ->
		{
			if (plan.route.stream().anyMatch(s -> s.patch.getType() == t))
			{
				unlocks.append(" • ").append(c.getDisplayName()).append(" at ").append(c.getLevel());
			}
		});
		col.addRow(wrapped(unlocks.toString(), MUTED), 2);
		if (plan.estimatedXp >= 1)
		{
			col.addRow(wrapped("This run: about <font color='#7fd4ff'>" + FarmingXp.format(plan.estimatedXp)
				+ " Farming XP</font>", Color.WHITE), 6);
		}
		else
		{
			col.addRow(new JLabel(" "), 4);
		}

		for (String w : plan.warnings)
		{
			col.addRow(wrapped("⚠ " + esc(w), ORANGE), 2);
		}
		for (String tip : plan.tips)
		{
			col.addRow(wrapped("\u2139 " + esc(tip), new Color(127, 212, 255)), 2);
		}
		if (contract != null)
		{
			int icon = contract.crop != null ? contract.crop.getIconItemId() : -1;
			col.addRow(iconCard(icon, "<b>Farming contract</b><br>" + esc(contract.describe()),
				contract.complete ? GREEN : GOLD, Color.WHITE), 4);
		}
		if (currentRun != null && setting("showHistory"))
		{
			long profit = currentRun.profit();
			col.addRow(wrapped("So far: " + currentRun.patches.size() + " patch"
				+ (currentRun.patches.size() == 1 ? "" : "es") + " • <font color='"
				+ (profit >= 0 ? "#5ac85a" : "#e65046") + "'>" + (profit >= 0 ? "+" : "")
				+ HerbPrices.gp(profit) + " gp</font>"
				+ (currentRun.xp > 0 ? " • <font color='#7fd4ff'>+" + FarmingXp.format(currentRun.xp) + " XP</font>" : ""),
				Color.WHITE), 4);
		}

		// Take with you
		if (!plan.withdrawals.isEmpty())
		{
			col.addRow(sectionTitle("Take with you"), 2);
			col.addRow(itemGrid(plan.withdrawals, true), 2);
			Color fit = plan.slotsNeeded <= 28 ? MUTED : ORANGE;
			col.addRow(wrapped("About " + plan.slotsNeeded + "/28 inventory slots"
				+ (plan.bankStops.isEmpty() ? "" : " • " + plan.bankStops.size() + " bank stop"
				+ (plan.bankStops.size() > 1 ? "s" : "") + " on the way"), fit), 8);
		}

		// Wear tips
		List<JComponent> tips = new ArrayList<>();
		for (RunPlan.Withdrawal w : plan.withdrawals)
		{
			if (worn.contains(w.itemName))
			{
				continue;
			}
			for (String[] tip : WEARABLE_TIPS)
			{
				if (w.itemName.startsWith(tip[0]))
				{
					tips.add(iconCard(idOf(w.itemName), esc(tip[1]), null, YELLOW));
					break;
				}
			}
		}
		if (!tips.isEmpty())
		{
			col.addRow(sectionTitle("Wear"), 2);
			for (JComponent t : tips)
			{
				col.addRow(t, 3);
			}
			col.addRow(spacer(), 4);
		}

		if (hintPatch != null && hintAction != null)
		{
			col.addRow(nowBanner(), 6);
		}

		// Every stop in order: done ones as green ticked rows, the rest as full cards
		// (gold = next), with bank stops where the inventory runs out.
		if (plan.customOrder)
		{
			col.addRow(customOrderRow(), 4);
		}
		boolean anyTodo = false;
		int number = 0;
		boolean nextShown = false;
		boolean thenShown = false;
		for (int idx = 0; idx < plan.route.size(); idx++)
		{
			RunPlan.RouteStep step = plan.route.get(idx);
			number++;
			for (RunPlan.BankStop b : plan.bankStops)
			{
				if (b.beforeStep == idx)
				{
					col.addRow(bankCard(b), 4);
				}
			}
			if (!timers.needsVisit(step.patch, now))
			{
				col.addRow(doneCard(step, number, now), 3);
				continue;
			}
			if (!nextShown)
			{
				col.addRow(sectionTitle("Next stop"), 2);
			}
			else if (!thenShown)
			{
				col.addRow(sectionTitle("Then"), 2);
				thenShown = true;
			}
			col.addRow(stopCard(step, number, !nextShown, now, idx, plan.route.size()), 4);
			nextShown = true;
			anyTodo = true;
		}
		if (!plan.route.isEmpty() && !anyTodo)
		{
			col.addRow(wrapped("Everything is planted and growing. Check back when a timer says ready.", GREEN), 6);
		}
		col.addRow(spacer(), 4);


		if (setting("showTravel"))
		{
			col.addRow(travelSection(), 6);
		}

		if (setting("showHistory"))
		{
			col.addRow(historySection(), 6);
		}
		col.addRow(settingsSection(), 6);

		replace(planSection, col);

		Column runsCol = new Column();
		runsCol.addRow(sectionTitle("Runs"), 2);
		for (PatchType t : PatchType.values())
		{
			runsCol.addRow(runSection(t, now), 3);
		}
		replace(runsSection, runsCol);
	}

	private JComponent stopCard(RunPlan.RouteStep step, int number, boolean first, long now, int idx, int total)
	{
		String travel = esc(step.teleportLabel);
		if (step.teleport != null && step.teleport.getFairyCode() != null)
		{
			String code = step.teleport.getFairyCode();
			travel = travel.replace(code, "<b><font color='#7fd4ff'>" + code + "</font></b>");
		}
		StringBuilder html = new StringBuilder();
		html.append("<b>").append(number).append(". ").append(esc(step.patch.getDisplayName())).append("</b><br>");
		html.append("<font color='#c8c8c8'>").append(travel).append("</font>");
		boolean marked = false;
		for (String line : StopWork.describe(step, timers.get(step.patch), timers.getStatus(step.patch), now))
		{
			if (!marked && step.patch == hintPatch && StopWork.isCurrent(line, hintAction))
			{
				// The step you're on now: bright, with an arrow.
				marked = true;
				html.append("<br><font color='#ffffff'><b>&#9654; ").append(esc(line)).append("</b></font>");
			}
			else
			{
				html.append("<br><font color='#ffb040'>• ").append(esc(line)).append("</font>");
			}
		}

		JPanel card = (JPanel) iconCard(iconIdFor(step.teleport, step.teleportItem), spellSprite(step.teleport), html.toString(),
			first ? GOLD : null, Color.WHITE);

		// Items used at this stop
		List<RunPlan.Withdrawal> items = new ArrayList<>();
		if (step.crop != null)
		{
			items.add(new RunPlan.Withdrawal(step.crop.getSeedName(), 1, ""));
		}
		if (step.compost != null && !step.compost.startsWith("Bottomless"))
		{
			items.add(new RunPlan.Withdrawal(step.compost, 1, ""));
		}
		if (step.paymentItem != null && step.crop != null)
		{
			items.add(new RunPlan.Withdrawal(step.paymentItem, step.paymentQuantity, "gardener payment"));
		}
		JPanel bottom = new JPanel(new BorderLayout(4, 0));
		bottom.setOpaque(false);
		if (!items.isEmpty())
		{
			JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
			strip.setOpaque(false);
			for (RunPlan.Withdrawal w : items)
			{
				strip.add(smallIcon(w));
			}
			bottom.add(strip, BorderLayout.CENTER);
		}
		// Up / down: do the run in your own order.
		JPanel move = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		move.setOpaque(false);
		move.add(moveButton("\u25B2", "Move this stop up", idx > 0, step.patch, -1));
		move.add(moveButton("\u25BC", "Move this stop down", idx < total - 1, step.patch, 1));
		bottom.add(move, BorderLayout.EAST);
		card.add(bottom, BorderLayout.SOUTH);
		return card;
	}

	private JComponent moveButton(String arrow, String tip, boolean enabled, FarmPatch patch, int delta)
	{
		JLabel b = new JLabel(arrow, SwingConstants.CENTER);
		b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
		b.setPreferredSize(new Dimension(18, 16));
		b.setOpaque(true);
		b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		b.setForeground(enabled ? Color.WHITE : new Color(90, 90, 90));
		if (enabled)
		{
			b.setToolTipText(tip);
			b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			b.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					onMoveStop.accept(patch, delta);
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					b.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				}
			});
		}
		return b;
	}

	/** "Your own order · Reset" above the stops once you've moved one. */
	private JComponent customOrderRow()
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		JLabel label = new JLabel("Stops in your own order");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(new Color(127, 212, 255));
		JLabel reset = new JLabel("Reset");
		reset.setFont(FontManager.getRunescapeSmallFont());
		reset.setForeground(GOLD);
		reset.setToolTipText("Go back to the automatic (fastest) order");
		reset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		reset.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				onResetOrder.run();
			}
		});
		row.add(label, BorderLayout.CENTER);
		row.add(reset, BorderLayout.EAST);
		return row;
	}

	private JComponent nowBanner()
	{
		int id = idOf(hintItem);
		String html = "<b>NOW \u2022 " + esc(hintPatch.getDisplayName()) + "</b><br>"
			+ "<font color='#ffc83c'>" + esc(hintAction) + (hintItem == null ? "" : ": " + esc(hintItem)) + "</font>"
			+ (hintItem == null ? "" : "<br><font color='#a0a0a0'>Highlighted in your inventory</font>");
		JComponent card = iconCard(id, html, GOLD, Color.WHITE);
		card.setBackground(new Color(58, 50, 28));
		return card;
	}

	private static final String[][] SETTINGS = {
		{"shortestPathLine", "Shortest Path walking line", "Send each stop to Shortest Path (if installed) for its walking line"},
		{"showNowNext", "Now / next box", "A box at the top of the screen: what to do now and the stop after"},
		{"bankSupplyTab", "Farm run bank tab", "A tab on the bank title bar that shows just the run's items to withdraw"},
		{"gameHintArrow", "Game hint arrow", "The game's yellow arrow (also on the minimap) over the next patch, ring, tree or gardener"},
		{"pathMinigameTeleports", "Path: minigame teleports", "Let Shortest Path's line use minigame teleports like Rat Pits"},
		{"keepRunsSeparate", "Keep runs separate", "Finish one run (herbs, allotments, trees...) before starting the next, instead of mixing their stops"},
		{"highlightPatch", "Highlight next patch", "Outline the next patch on the ground"},
		{"highlightGardener", "Highlight gardener to pay", "Outline the gardener when you need to pay them"},
		{"pointerArrow", "Arrow over target", "A steady arrow over the next patch, ring, tree or gardener"},
		{"highlightInventory", "Highlight step items", "Outline the seed / compost / payment your current step needs"},
		{"highlightBank", "Highlight in bank", "Outline what to withdraw when your bank is open"},
		{"notifyReady", "Notify when ready", "Notification when a patch finishes growing"},
		{"notifyDisease", "Notify diseased/dead", "Notification when a patch is found diseased or dead"},
		{"showReadyOverlay", "Run-ready box", "Small on-screen box showing which runs are ready"},
		{"profitFirst", "Most profitable herb first", "Plant the herb with the best GE profit first"},
		{"plantContract", "Plant farming contract", "Put your contract crop in the Farming Guild patch"},
		{"showHistory", "Run history & profit", "Show this run's profit and your run history"},
		{"chainFairyRings", "Chain fairy rings", "Do fairy-ring patches back to back"},
		{"payGardeners", "Pay gardeners", "Bring payments so gardeners protect your trees"},
		{"useCompost", "Bring compost", "Add compost to the bank list"},
		{"activeTimersOnly", "Timers: planted patches only", "Hide empty and weedy patches from the timer list"},
		{"onlyReady", "Only ready patches", "When some patches are ready, leave the growing ones off the run"},
		{"bringRake", "Bring a rake", "Add a rake to the bank list"},
		{"importTimeTracking", "Import Time Tracking", "Use timers RuneLite's Time Tracking already saved"},
		{"useBoostedLevel", "Use boosted Farming", "Plan with your boosted level (e.g. garden pie)"},
		{"staffless", "Lumbridge Elite done", "Fairy rings without a staff"},
		{"harmonyUnlocked", "Morytania Elite done", "Unlocks the Harmony Island herb patch"},
		{"showTimers", "Show patch timers", "Show the timer list at the top"},
		{"showTravel", "Show Travel section", "Show your teleports section"},
		{"showSocials", "Channel links", "Buttons to ToadFx's YouTube, Twitch, Kick and TikTok at the bottom"},
	};

	private JComponent settingsSection()
	{
		Column col = new Column();
		JLabel toggle = label((settingsExpanded ? "\u25BC  " : "\u25B6  ") + "Settings", Color.WHITE, true);
		toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		toggle.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				settingsExpanded = !settingsExpanded;
				rebuildPlan();
			}
		});
		col.addRow(toggle, 3);
		if (!settingsExpanded)
		{
			return col;
		}
		Column body = new Column();
		body.setOpaque(true);
		body.setBackground(new Color(34, 34, 34));
		body.setBorder(new EmptyBorder(6, 3, 6, 2));
		for (String[] st : SETTINGS)
		{
			String key = st[0];
			boolean on = setting(key);
			JCheckBox box = new JCheckBox(st[1], on);
			box.setOpaque(false);
			box.setFocusPainted(false);
			box.setFont(FontManager.getRunescapeSmallFont());
			box.setForeground(on ? GREEN : MUTED);
			box.setToolTipText(st[2]);
			box.addActionListener(e -> onSetting.accept(key, box.isSelected()));
			body.addRow(box, 1);
		}
		body.addRow(wrapped("Colours and more are in RuneLite's settings for the plugin (wrench icon).", MUTED), 0);
		col.addRow(body, 0);
		return col;
	}

	/** A finished stop: green, ticked, with its countdown. */
	private JComponent doneCard(RunPlan.RouteStep step, int number, long now)
	{
		PatchTimers.Planting pl = timers.get(step.patch);
		String when = pl == null ? "growing" : pl.crop.getDisplayName() + ", ready in "
			+ PatchTimers.formatDuration(pl.millisLeft(now));
		JLabel l = new JLabel("<html><body style='width:140px'>\u2713 <b>" + number + ". "
			+ esc(step.patch.getDisplayName()) + "</b> <font color='#a8e0a8'>" + esc(when) + "</font></body></html>");
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(GREEN);
		l.setOpaque(true);
		l.setBackground(new Color(36, 58, 36));
		l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, GREEN),
			new EmptyBorder(4, 6, 4, 6)));
		return l;
	}

	/** One run: what you can plant (click to pick) and its patches (tick to include). */
	private JComponent runSection(PatchType type, long now)
	{
		Column col = new Column();
		boolean on = runs.contains(type);
		boolean open = runExpanded.containsKey(type) ? runExpanded.get(type) : on;
		Crop first = null;
		for (Crop c : Crop.values())
		{
			if (c.getType() == type)
			{
				first = c;
				break;
			}
		}
		boolean levelOk = first != null && plan.farmingLevel >= first.getLevel();
		String status = !levelOk ? "needs " + first.getLevel() + " Farming" : on ? "on" : "off";

		JCheckBox onBox = new JCheckBox();
		onBox.setSelected(on);
		onBox.setOpaque(false);
		onBox.setFocusPainted(false);
		onBox.setEnabled(levelOk);
		onBox.setToolTipText(on ? "Click to leave this run out" : "Click to include this run");
		onBox.addActionListener(e -> onRunChanged.accept(type, onBox.isSelected()));
		JLabel name = label((open ? "\u25BC " : "\u25B6 ") + type.getDisplayName() + " run", on ? Color.WHITE : MUTED, true);
		name.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		name.setToolTipText(open ? "Hide crops and patches" : "Show crops and patches");
		name.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				runExpanded.put(type, !open);
				rebuildPlan();
			}
		});
		JPanel leftSide = new JPanel(new BorderLayout(2, 0));
		leftSide.setOpaque(false);
		leftSide.add(onBox, BorderLayout.WEST);
		leftSide.add(name, BorderLayout.CENTER);
		JLabel right = label(status, !levelOk ? MUTED : on ? GREEN : MUTED, false);
		JPanel header = row(leftSide, right);
		header.setOpaque(true);
		header.setBackground(CARD);
		header.setBorder(new EmptyBorder(3, 2, 3, 6));
		col.addRow(header, 0);
		if (!open)
		{
			return col;
		}

		Column body = new Column();
		body.setOpaque(true);
		body.setBackground(new Color(34, 34, 34));
		body.setBorder(new EmptyBorder(8, 3, 10, 2));
		body.addRow(sectionTitle(type == PatchType.HERB ? "Herbs to plant"
			: type == PatchType.ALLOTMENT ? "Crops to plant (3 seeds each)"
			: type == PatchType.FLOWER ? "Flowers to plant"
			: type == PatchType.HOPS ? "Hops to plant (4 seeds each, jute 3)"
			: !type.usesSaplings() ? type.getDisplayName() + " to plant" : "Trees to plant"), 4);
		JPanel crops = new JPanel(new GridLayout(0, 4, 6, 6));
		crops.setOpaque(false);
		for (Crop c : Crop.values())
		{
			if (c.getType() == type)
			{
				crops.add(cropTile(c));
			}
		}
		body.addRow(crops, 8);
		body.addRow(wrapped("Click to pick. " + (type == PatchType.HERB && setting("profitFirst")
			? "The most profitable picked seed you own is planted first." : "The best picked seed you own is planted first."),
			MUTED), 10);
		if (type == PatchType.HERB && !herbProfit.isEmpty())
		{
			body.addRow(sectionTitle("Profit per patch"), 2);
			int shown = 0;
			for (Crop c : HerbPrices.rankByProfit(herbProfit))
			{
				if (c.getLevel() > plan.farmingLevel || shown >= 4)
				{
					continue;
				}
				int pr = herbProfit.get(c);
				Double y = herbYields.get(c);
				JLabel herbName = label((shown == 0 ? "\u2605 " : "   ") + c.getDisplayName()
					+ (y == null ? "" : String.format(" (%.1f)", y)), shown == 0 ? GOLD : Color.WHITE, shown == 0);
				JLabel gp = label((pr >= 0 ? "+" : "") + HerbPrices.gp(pr), pr >= 0 ? GREEN : RED, false);
				body.addRow(row(herbName, gp), 1);
				shown++;
			}
			body.addRow(wrapped(herbNote != null ? "GE prices, (herbs a patch). " + herbNote
				: String.format("GE prices, about %.1f herbs a patch, minus the seed.", herbYield), MUTED), 10);
		}

		body.addRow(sectionTitle("Patches"), 4);
		JPanel patches = new JPanel(new GridLayout(0, 2, 4, 4));
		patches.setOpaque(false);
		for (FarmPatch p : FarmPatch.values())
		{
			if (p.getType() != type)
			{
				continue;
			}
			boolean locked = !plan.unlocked.contains(p) || !levelOk;
			if (locked)
			{
				JLabel l = new JLabel(pickerName(p), new ImageIcon(lockIcon()), SwingConstants.LEFT);
				l.setIconTextGap(4);
				l.setFont(FontManager.getRunescapeSmallFont());
				l.setForeground(new Color(110, 110, 110));
				l.setToolTipText("Locked: needs " + (levelOk ? p.getRequirement().getDescription()
					: first.getLevel() + " Farming"));
				patches.add(l);
				continue;
			}
			boolean picked = !offPatches.contains(p);
			JCheckBox box = new JCheckBox(pickerName(p), picked);
			box.setOpaque(false);
			box.setFocusPainted(false);
			box.setIconTextGap(3);
			box.setBorder(new EmptyBorder(1, 0, 1, 0));
			box.setFont(FontManager.getRunescapeSmallFont());
			box.setForeground(picked ? GREEN : MUTED);
			box.setToolTipText(picked ? "Click to skip this patch" : "Click to include this patch");
			box.addActionListener(e -> onTogglePatch.accept(p));
			patches.add(box);
		}
		body.addRow(patches, 0);
		col.addRow(body, 0);
		return col;
	}

	/** Picked = full colour + green border; not picked = dimmed; locked = dimmed + lock + level. */
	private JComponent cropTile(Crop c)
	{
		boolean locked = plan.farmingLevel < c.getLevel();
		boolean picked = !locked && !offCrops.contains(c);

		JLabel tile = new JLabel();
		tile.setPreferredSize(new Dimension(42, 42));
		tile.setHorizontalAlignment(SwingConstants.CENTER);
		tile.setOpaque(true);
		tile.setBackground(picked ? new Color(40, 56, 40) : CARD);
		tile.setBorder(BorderFactory.createLineBorder(picked ? GREEN : new Color(64, 64, 64), picked ? 2 : 1));

		AsyncBufferedImage img = itemManager.getImage(c.getIconItemId());
		float alpha = picked ? 1f : locked ? 0.3f : 0.45f;
		Runnable paint = () -> tile.setIcon(new ImageIcon(cropIcon(img, alpha, locked ? c.getLevel() : -1)));
		paint.run();
		img.onLoaded(paint);

		if (locked)
		{
			tile.setToolTipText(c.getDisplayName() + " - locked, needs " + c.getLevel() + " Farming");
		}
		else
		{
			Integer pr = herbProfit.get(c);
			tile.setToolTipText(c.getDisplayName() + " (level " + c.getLevel() + ")"
				+ (pr == null ? "" : ", about " + (pr >= 0 ? "+" : "") + HerbPrices.gp(pr) + " gp a patch") + " - "
				+ (picked ? "picked, click to stop planting it" : "click to plant it"));
			tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			tile.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					onToggleCrop.accept(c);
				}
			});
		}
		return tile;
	}

	/** The crop picture at the given opacity; locked crops get a padlock and their level. */
	private static java.awt.image.BufferedImage cropIcon(java.awt.image.BufferedImage img, float alpha, int lockedLevel)
	{
		return dimIcon(img, alpha, lockedLevel, lockedLevel > 0);
	}

	/** An item picture at the given opacity, with an optional padlock (and level number when > 0). */
	private static java.awt.image.BufferedImage dimIcon(java.awt.image.BufferedImage img, float alpha, int lockedLevel,
		boolean showLock)
	{
		java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(36, 36, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = out.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
		g.drawImage(img, (36 - img.getWidth()) / 2, (36 - img.getHeight()) / 2, null);
		g.setComposite(java.awt.AlphaComposite.SrcOver);
		if (showLock)
		{
			g.drawImage(lockIcon(), 24, 0, null);
		}
		if (lockedLevel > 0)
		{
			g.setFont(FontManager.getRunescapeSmallFont());
			String lvl = String.valueOf(lockedLevel);
			g.setColor(Color.BLACK);
			g.drawString(lvl, 2, 35);
			g.setColor(YELLOW);
			g.drawString(lvl, 1, 34);
		}
		g.dispose();
		return out;
	}

	private static java.awt.image.BufferedImage lockImage;

	/** A small gold padlock, drawn once. */
	private static java.awt.image.BufferedImage lockIcon()
	{
		if (lockImage == null)
		{
			java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(11, 12, java.awt.image.BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D g = img.createGraphics();
			g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g.setStroke(new java.awt.BasicStroke(1.6f));
			g.setColor(new Color(40, 30, 10));
			g.drawArc(2, 0, 7, 8, 0, 180);
			g.setColor(new Color(200, 170, 80));
			g.drawArc(2, 1, 7, 7, 0, 180);
			g.setColor(new Color(40, 30, 10));
			g.fillRoundRect(0, 5, 11, 7, 2, 2);
			g.setColor(new Color(230, 190, 70));
			g.fillRoundRect(1, 6, 9, 5, 2, 2);
			g.setColor(new Color(60, 45, 10));
			g.fillRect(5, 7, 1, 3);
			g.dispose();
			lockImage = img;
		}
		return lockImage;
	}

	private JComponent bankCard(RunPlan.BankStop b)
	{
		JPanel card = new JPanel(new BorderLayout(0, 3));
		card.setBackground(CARD);
		card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, ORANGE),
			new EmptyBorder(5, 6, 5, 6)));
		JLabel text = new JLabel("<html><body style='width:140px'><b>Bank at " + esc(b.bank) + "</b><br>"
			+ "<font color='#c8c8c8'>Your inventory is full by here. Withdraw:</font></body></html>");
		text.setFont(FontManager.getRunescapeSmallFont());
		text.setForeground(Color.WHITE);
		card.add(text, BorderLayout.NORTH);
		card.add(itemGrid(b.withdrawals, false), BorderLayout.CENTER);
		return card;
	}

	private JComponent itemGrid(List<RunPlan.Withdrawal> list, boolean showCarried)
	{
		JPanel grid = new JPanel(new GridLayout(0, 4, 4, 4));
		grid.setOpaque(false);
		for (RunPlan.Withdrawal w : list)
		{
			grid.add(itemTile(w, showCarried));
		}
		return grid;
	}

	private JComponent itemTile(RunPlan.Withdrawal w, boolean showCarried)
	{
		boolean done = showCarried && carried.getOrDefault(w.itemName, 0) >= w.quantity;
		int id = idOf(w.itemName);

		JLabel tile = new JLabel();
		tile.setPreferredSize(new Dimension(42, 38));
		tile.setHorizontalAlignment(SwingConstants.CENTER);
		tile.setOpaque(true);
		tile.setBackground(CARD);
		Color border = done ? GREEN : id > 0 ? ORANGE : RED;
		tile.setBorder(BorderFactory.createLineBorder(border, done ? 2 : 1));

		String tip = w.quantity + " x " + w.itemName + (w.note.isEmpty() ? "" : " (" + w.note + ")");
		if (done)
		{
			tip += " - carrying";
		}
		else if (id <= 0)
		{
			tip += " - not in your bank";
		}
		tile.setToolTipText(tip);

		if (id > 0)
		{
			itemManager.getImage(id, w.quantity, w.quantity > 1).addTo(tile);
		}
		else
		{
			tile.setText("<html><center>" + w.quantity + "<br>?</center></html>");
			tile.setFont(FontManager.getRunescapeSmallFont());
			tile.setForeground(MUTED);
		}
		return tile;
	}

	private JComponent smallIcon(RunPlan.Withdrawal w)
	{
		int id = idOf(w.itemName);
		JLabel l = new JLabel();
		l.setPreferredSize(new Dimension(34, 30));
		l.setToolTipText(w.quantity + " x " + w.itemName + (w.note.isEmpty() ? "" : " (" + w.note + ")"));
		if (id > 0)
		{
			itemManager.getImage(id, w.quantity, w.quantity > 1).addTo(l);
		}
		else
		{
			l.setText(w.quantity + "x");
			l.setFont(FontManager.getRunescapeSmallFont());
			l.setForeground(MUTED);
		}
		return l;
	}

	private int idOf(String itemName)
	{
		Integer id = itemName == null ? null : nameToId.get(itemName);
		return id == null ? -1 : id;
	}

	/** Picture for a teleport: the owned item, else a known icon, else a rune/staff. */
	private int iconIdFor(Teleport t, String ownedItem)
	{
		if (ownedItem != null && nameToId.containsKey(ownedItem))
		{
			return nameToId.get(ownedItem);
		}
		if (t == null)
		{
			return -1;
		}
		if (t.getKind() == Teleport.Kind.FAIRY_RING)
		{
			return DRAMEN_STAFF_ICON;
		}
		if (t.getKind() == Teleport.Kind.SPELL)
		{
			return LAW_RUNE_ICON;
		}
		return t.getIconItemId();
	}

	// ================================================================ history

	private JComponent historySection()
	{
		Column col = new Column();
		JLabel toggle = label((historyExpanded ? "\u25BC  " : "\u25B6  ") + "Run history", Color.WHITE, true);
		toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		toggle.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				historyExpanded = !historyExpanded;
				rebuildPlan();
			}
		});
		long shownTotal = totalProfit + (currentRun == null ? 0 : currentRun.profit());
		boolean anything = !historyRuns.isEmpty() || currentRun != null;
		JLabel total = label(anything ? (shownTotal >= 0 ? "+" : "") + HerbPrices.gp(shownTotal) : "",
			shownTotal >= 0 ? GREEN : RED, false);
		col.addRow(row(toggle, total), 4);
		if (!historyExpanded)
		{
			return col;
		}
		if (currentRun != null)
		{
			long p = currentRun.profit();
			JLabel left = label("This run (" + currentRun.patches.size() + "p)", GOLD, true);
			JLabel right = label((p >= 0 ? "+" : "") + HerbPrices.gp(p), p >= 0 ? GREEN : RED, false);
			JPanel line = row(left, right);
			StringBuilder tip = new StringBuilder("<html>");
			currentRun.produce.forEach((name, qty) -> tip.append(esc(name)).append(" x ").append(qty).append("<br>"));
			tip.append("Value ").append(HerbPrices.gp(currentRun.value)).append(", seeds ")
				.append(HerbPrices.gp(currentRun.cost)).append("<br>Saved to history 20 min after your last harvest or planting</html>");
			left.setToolTipText(tip.toString());
			col.addRow(line, 2);
		}
		if (historyRuns.isEmpty())
		{
			col.addRow(wrapped(currentRun != null
				? "Your run is being logged. It moves into the history 20 minutes after your last harvest or planting."
				: "No runs logged yet. Harvests and plantings are logged as you farm; "
				+ "a run ends after 20 minutes without farming.", MUTED), 4);
			return col;
		}
		long avg = totalProfit / historyRuns.size();
		long totalXp = 0;
		for (RunHistory.Run r : historyRuns)
		{
			totalXp += r.xp;
		}
		col.addRow(wrapped(historyRuns.size() + " runs • average " + (avg >= 0 ? "+" : "") + HerbPrices.gp(avg)
			+ (totalXp > 0 ? " and " + FarmingXp.format((double) totalXp / historyRuns.size()) + " XP" : "")
			+ " • " + String.format("%.1f", herbYield) + " herbs a patch", MUTED), 4);
		java.text.SimpleDateFormat when = new java.text.SimpleDateFormat("EEE h:mma");
		int shown = 0;
		for (RunHistory.Run r : historyRuns)
		{
			if (shown++ >= 10)
			{
				break;
			}
			long p = r.profit();
			JLabel left = label(when.format(new java.util.Date(r.start)).replace("AM", "am").replace("PM", "pm")
				+ "  " + r.patches.size() + "p", Color.WHITE, false);
			JLabel right = new JLabel("<html><font color='" + (p >= 0 ? "#5ac85a" : "#e65046") + "'>"
				+ (p >= 0 ? "+" : "") + HerbPrices.gp(p) + "</font>"
				+ (r.xp > 0 ? " <font color='#7fd4ff'>" + FarmingXp.format(r.xp) + "xp</font>" : "") + "</html>");
			right.setFont(FontManager.getRunescapeSmallFont());
			JPanel line = row(left, right);
			StringBuilder tip = new StringBuilder("<html>");
			r.produce.forEach((name, qty) -> tip.append(esc(name)).append(" x ").append(qty).append("<br>"));
			tip.append("Value ").append(HerbPrices.gp(r.value)).append(", seeds ").append(HerbPrices.gp(r.cost)).append("</html>");
			line.setToolTipText(tip.toString());
			left.setToolTipText(tip.toString());
			col.addRow(line, 1);
		}
		JButton clear = new JButton("Clear history");
		clear.setFont(FontManager.getRunescapeSmallFont());
		clear.setFocusPainted(false);
		clear.addActionListener(e ->
		{
			if (JOptionPane.showConfirmDialog(this, "Delete your run history?", "Run history",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			{
				onClearHistory.run();
			}
		});
		col.addRow(clear, 4);
		return col;
	}

	// ================================================================ travel

	private JComponent travelSection()
	{
		Column col = new Column();
		JLabel toggle = label((travelExpanded ? "▼  " : "▶  ") + "Travel", Color.WHITE, true);
		toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		toggle.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				travelExpanded = !travelExpanded;
				rebuildPlan();
			}
		});
		col.addRow(toggle, 3);
		if (!travelExpanded)
		{
			return col;
		}

		col.addRow(optionBox("Use spells (runes)", useSpells,
			"Teleport with standard spellbook spells. Runes get added to the bank list.", onUseSpells), 0);
		col.addRow(optionBox("Use teleport tabs", useTabs,
			"Use teleport tablets. If spells are on too, tabs are only used where you can't cast the spell.", onUseTabs), 4);
		col.addRow(houseRow(), 4);
		JCheckBox all = new JCheckBox("Show all travel methods", showAllTravel);
		all.setOpaque(false);
		all.setFocusPainted(false);
		all.setFont(FontManager.getRunescapeSmallFont());
		all.setToolTipText("Also list tabs, spells and fairy rings, not just your items");
		all.addActionListener(e ->
		{
			showAllTravel = all.isSelected();
			rebuildPlan();
		});
		col.addRow(all, 4);
		col.addRow(wrapped("Bright + green = used on this run. Green edge = available. Faded with a lock = not "
			+ "unlocked/owned yet (hover to see why). Red = turned off. Click to turn off/on.", MUTED), 4);

		Teleport.Kind[] kinds = showAllTravel ? Teleport.Kind.values()
			: new Teleport.Kind[]{Teleport.Kind.ITEM, Teleport.Kind.OTHER};
		for (Teleport.Kind kind : kinds)
		{
			JPanel grid = new JPanel(new GridLayout(0, 4, 4, 4));
			grid.setOpaque(false);
			for (Teleport t : Teleport.values())
			{
				if (t.getKind() == kind)
				{
					grid.add(teleportTile(t));
				}
			}
			if (kind == Teleport.Kind.OTHER)
			{
				grid.add(fairyRingTile());
			}
			col.addRow(sectionTitle(kind.getTitle()), 1);
			col.addRow(grid, 4);
		}
		return col;
	}

	/** "House: [location]" so Teleport to House, house tabs and the Construction cape can be used. */
	private JComponent houseRow()
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		JLabel label = new JLabel("House");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(Color.WHITE);
		JComboBox<HouseLocation> box = new JComboBox<>(HouseLocation.values());
		box.setSelectedItem(houseLocation);
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setFocusable(false);
		String tip = "Where your house portal is. Teleport to House and house tabs get used for patches "
			+ "near it. A Construction cape works at every portal.";
		box.setToolTipText(tip);
		label.setToolTipText(tip);
		box.addActionListener(e ->
		{
			HouseLocation picked = (HouseLocation) box.getSelectedItem();
			if (picked != null && picked != houseLocation)
			{
				houseLocation = picked;
				onHouseLocation.accept(picked);
			}
		});
		row.add(label, BorderLayout.WEST);
		row.add(box, BorderLayout.CENTER);
		return row;
	}

	private JCheckBox optionBox(String text, boolean selected, String tooltip, Consumer<Boolean> onChange)
	{
		JCheckBox box = new JCheckBox(text, selected);
		box.setOpaque(false);
		box.setFocusPainted(false);
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setToolTipText(tooltip);
		box.addActionListener(e -> onChange.accept(box.isSelected()));
		return box;
	}

	private JComponent teleportTile(Teleport t)
	{
		Teleport.Availability av = plan.availability.get(t);
		boolean available = av != null && av.available;
		boolean off = disabled.contains(t);
		boolean usable = available && !off;
		boolean used = plan.route.stream().anyMatch(s -> s.teleport == t);

		JLabel tile = new JLabel();
		tile.setPreferredSize(new Dimension(42, 38));
		tile.setHorizontalAlignment(SwingConstants.CENTER);
		tile.setOpaque(true);
		tile.setBackground(usable ? CARD : ColorScheme.DARK_GRAY_COLOR);
		Color border = used ? GREEN : usable ? new Color(70, 110, 70) : off && available ? RED : new Color(70, 70, 70);
		tile.setBorder(BorderFactory.createLineBorder(border, used ? 2 : 1));
		tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		boolean textOnly = t.getKind() == Teleport.Kind.FAIRY_RING || t.getKind() == Teleport.Kind.SPELL;
		int iconId = textOnly ? -1 : iconIdFor(t, av == null ? null : av.ownedItem);
		int sprite = spellSprite(t);
		if (sprite >= 0 && spriteManager != null)
		{
			setSprite(tile, sprite, used ? 1f : usable ? 0.8f : available ? 0.45f : 0.3f, !available);
		}
		else if (iconId > 0)
		{
			AsyncBufferedImage img = itemManager.getImage(iconId);
			float alpha = used ? 1f : usable ? 0.8f : available ? 0.45f : 0.3f;
			Runnable paint = () -> tile.setIcon(new ImageIcon(dimIcon(img, alpha, 0, !available)));
			paint.run();
			img.onLoaded(paint);
		}
		else
		{
			boolean code = t.getFairyCode() != null;
			tile.setText(t.getShortLabel());
			tile.setFont(code ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
			Color fg = code ? new Color(127, 212, 255) : Color.WHITE;
			tile.setForeground(usable ? fg : new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), available ? 120 : 80));
		}

		String reason = av == null ? "" : av.reason;
		tile.setToolTipText("<html><b>" + esc(t.getLabel()) + "</b><br>"
			+ (available ? esc(reason) : "<font color='#e65046'>" + esc(reason) + "</font>") + "<br>"
			+ (used ? "<font color='#5ac85a'>Used on this run</font><br>" : "")
			+ (off ? "Turned OFF - click to turn on" : "Click to turn off") + "</html>");
		tile.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				onToggleTeleport.accept(t);
			}
		});
		return tile;
	}

	/** One dramen staff tile that turns every fairy ring on or off together. */
	private JComponent fairyRingTile()
	{
		List<Teleport> rings = new ArrayList<>();
		for (Teleport t : Teleport.values())
		{
			if (t.getKind() == Teleport.Kind.FAIRY_RING)
			{
				rings.add(t);
			}
		}
		Teleport.Availability av = plan.availability.get(rings.get(0));
		boolean available = rings.stream().anyMatch(t -> plan.availability.get(t) != null && plan.availability.get(t).available);
		boolean off = rings.stream().allMatch(disabled::contains);
		boolean usable = available && !off;
		boolean used = plan.route.stream().anyMatch(s -> s.teleport != null && s.teleport.getKind() == Teleport.Kind.FAIRY_RING);

		JLabel tile = new JLabel();
		tile.setPreferredSize(new Dimension(42, 38));
		tile.setHorizontalAlignment(SwingConstants.CENTER);
		tile.setOpaque(true);
		tile.setBackground(usable ? CARD : ColorScheme.DARK_GRAY_COLOR);
		Color border = used ? GREEN : usable ? new Color(70, 110, 70) : off && available ? RED : new Color(70, 70, 70);
		tile.setBorder(BorderFactory.createLineBorder(border, used ? 2 : 1));
		tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		AsyncBufferedImage img = itemManager.getImage(DRAMEN_STAFF_ICON);
		float alpha = used ? 1f : usable ? 0.8f : available ? 0.45f : 0.3f;
		Runnable paint = () -> tile.setIcon(new ImageIcon(dimIcon(img, alpha, 0, !available)));
		paint.run();
		img.onLoaded(paint);

		String reason = rings.stream().map(plan.availability::get)
			.filter(a -> a != null && a.available == available).map(a -> a.reason)
			.findFirst().orElse(av == null ? "" : av.reason);
		tile.setToolTipText("<html><b>Fairy rings</b><br>"
			+ (available ? esc(reason) : "<font color='#e65046'>" + esc(reason) + "</font>") + "<br>"
			+ (used ? "<font color='#5ac85a'>Used on this run - codes are on each stop</font><br>" : "")
			+ (off ? "Turned OFF - click to turn on" : "Click to turn off") + "</html>");
		tile.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				for (Teleport t : rings)
				{
					// Turning off: flip the ones that are on. Turning on: flip the ones that are off.
					if (off == disabled.contains(t))
					{
						onToggleTeleport.accept(t);
					}
				}
			}
		});
		return tile;
	}

	// ================================================================ small helpers

	/** A dark card with an optional item icon on the left and HTML text. */
	private JComponent iconCard(int iconId, String html, Color accent, Color textColor)
	{
		return iconCard(iconId, -1, html, accent, textColor);
	}

	/** Like iconCard, but shows a spellbook picture instead of the item when spriteId is set. */
	private JComponent iconCard(int iconId, int spriteId, String html, Color accent, Color textColor)
	{
		JPanel card = new JPanel(new BorderLayout(6, 2));
		card.setBackground(CARD);
		EmptyBorder pad = new EmptyBorder(5, 6, 5, 6);
		card.setBorder(accent == null ? pad
			: BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, accent), pad));

		if (spriteId >= 0 && spriteManager != null)
		{
			JLabel icon = new JLabel();
			icon.setPreferredSize(new Dimension(36, 32));
			icon.setVerticalAlignment(SwingConstants.TOP);
			setSprite(icon, spriteId, 1f, false);
			card.add(icon, BorderLayout.WEST);
			iconId = Math.max(iconId, 1); // keep the text width for an icon
		}
		else if (iconId > 0)
		{
			JLabel icon = new JLabel();
			icon.setPreferredSize(new Dimension(36, 32));
			icon.setVerticalAlignment(SwingConstants.TOP);
			itemManager.getImage(iconId).addTo(icon);
			card.add(icon, BorderLayout.WEST);
		}

		JLabel text = new JLabel("<html><body style='width:" + (iconId > 0 ? 105 : 145) + "px'>"
			+ html + "</body></html>");
		text.setFont(FontManager.getRunescapeSmallFont());
		text.setForeground(textColor);
		card.add(text, BorderLayout.CENTER);
		return card;
	}

	private static void replace(JPanel holder, JComponent content)
	{
		holder.removeAll();
		holder.setLayout(new BorderLayout());
		holder.add(content, BorderLayout.CENTER);
		holder.revalidate();
		holder.repaint();
	}

	private static JPanel row(JComponent left, JComponent right)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(left, BorderLayout.WEST);
		row.add(right, BorderLayout.EAST);
		return row;
	}

	private static JLabel label(String text, Color color, boolean bold)
	{
		JLabel l = new JLabel(text);
		l.setFont(bold ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
		l.setForeground(color);
		return l;
	}

	private static JLabel wrapped(String html, Color color)
	{
		JLabel l = new JLabel("<html><body style='width:150px'>" + html + "</body></html>");
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(color);
		return l;
	}

	private static JLabel sectionTitle(String text)
	{
		JLabel l = new JLabel(text.toUpperCase());
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(MUTED);
		l.setBorder(new EmptyBorder(4, 0, 2, 0));
		return l;
	}

	private static JComponent spacer()
	{
		JPanel p = new JPanel();
		p.setOpaque(false);
		p.setPreferredSize(new Dimension(1, 2));
		return p;
	}

	/** Compact names for the two-column patch pickers. */
	private static String pickerName(FarmPatch p)
	{
		return shortName(p).replace("Troll Stronghold", "Trollheim").replace("Harmony Island", "Harmony")
			.replace("Tree Gnome Village", "Gnome Village").replace("Gnome Stronghold", "Stronghold")
			.replace("Farming Guild", "Guild").replace("Fossil Island", "Fossil")
			.replace(" (north-west)", " NW").replace(" (south-east)", " SE").replace(" (north-east)", " NE")
			.replace(" (south-west)", " SW").replace(" (north)", " N").replace(" (south)", " S");
	}

	/** Shorter names inside a group that's already labelled by type. */
	private static String shortName(FarmPatch p)
	{
		return p.getDisplayName().replace(" fruit tree", "").replace(" hardwood", "").replace(" herb", "")
			.replace(" tree", "").replace(" (Ortus Farm)", "").replace(" allotment (", " (");
	}

	private static String esc(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** Vertical stack that stretches children to full width. */
	private static final class Column extends JPanel
	{
		private int rowIndex;

		Column()
		{
			super(new GridBagLayout());
			setOpaque(false);
		}

		void addRow(JComponent c, int gapBelow)
		{
			GridBagConstraints g = new GridBagConstraints();
			g.gridx = 0;
			g.gridy = rowIndex++;
			g.weightx = 1;
			g.fill = GridBagConstraints.HORIZONTAL;
			g.anchor = GridBagConstraints.NORTHWEST;
			g.insets = new Insets(0, 0, gapBelow, 0);
			super.add(c, g);
		}
	}
}
