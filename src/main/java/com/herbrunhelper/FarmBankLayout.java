package com.herbrunhelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.QuantityFormatter;

/**
 * Lays the Farm run bank tab out in sections (Travel, Runes, Seeds & saplings, Payments,
 * Tools & compost). It only moves the tag view's own item widgets: their bank slot index and
 * item id stay the same, so withdrawing still works. The labels and placeholder icons are drawn
 * by {@link FarmBankViewOverlay}, clipped to the bank's item area.
 */
@Singleton
class FarmBankLayout
{
	static final int PER_ROW = 8;
	static final int X_SPACING = 48;
	static final int Y_SPACING = 64;
	static final int START_X = 51;

	private static final Color HEADER = new Color(228, 216, 162);
	private static final Color CARRIED = new Color(90, 200, 90);
	private static final Color IN_BANK = new Color(255, 210, 70);
	private static final Color MISSING = new Color(230, 80, 70);
	private static final Color NOTED = new Color(255, 210, 70);
	private static final Color UNNOTED = new Color(165, 165, 165);
	private static final Color LINE = new Color(90, 80, 55);

	/** One thing the overlay draws, at coordinates relative to the bank item area. */
	static final class Draw
	{
		static final int TEXT = 0;
		static final int ICON = 1;
		static final int RECT = 2;

		final int kind;
		final int x;
		final int y;
		final int width;
		final int height;
		final String text;
		final int colour;
		final int fontId;
		final int itemId;
		final boolean faded;

		private Draw(int kind, int x, int y, int width, int height, String text, int colour,
			int fontId, int itemId, boolean faded)
		{
			this.kind = kind;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.text = text;
			this.colour = colour;
			this.fontId = fontId;
			this.itemId = itemId;
			this.faded = faded;
		}

		static Draw text(String text, int x, int y, Color colour, int fontId)
		{
			return new Draw(TEXT, x, y, 0, 0, text, colour.getRGB(), fontId, -1, false);
		}

		static Draw icon(int itemId, int x, int y, boolean faded)
		{
			return new Draw(ICON, x, y, 0, 0, null, 0, 0, itemId, faded);
		}

		static Draw rect(int x, int y, int width, int height, Color colour)
		{
			return new Draw(RECT, x, y, width, height, null, colour.getRGB(), 0, -1, false);
		}
	}

	private final Client client;
	private final ClientThread clientThread;
	private final FarmBankButton button;
	private final HerbRunPlugin plugin;
	private volatile List<Draw> draws = Collections.emptyList();
	/** Scroll position captured before a bank rebuild, restored after it. */
	private int savedScroll = -1;
	/** Whether the Farm run view was already showing before this bank build. */
	private boolean activeLastBuild;

	@Inject
	FarmBankLayout(Client client, ClientThread clientThread, FarmBankButton button, HerbRunPlugin plugin)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.button = button;
		this.plugin = plugin;
	}

	/** Captures the list position before a withdrawal/deposit rebuilds the bank. */
	void saveScroll()
	{
		if (!activeLastBuild)
		{
			return; // opening the view: start at the top
		}
		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (items != null && !items.isHidden())
		{
			savedScroll = items.getScrollY();
		}
	}

	/** The bank finished building (including the Bank Tags tab). */
	void onBankBuilt()
	{
		boolean active = button.isActive();
		if (!active)
		{
			draws = Collections.emptyList();
			savedScroll = -1;
			activeLastBuild = false;
			return;
		}
		if (!activeLastBuild)
		{
			savedScroll = -1; // opening the view: start at the top
		}
		activeLastBuild = true;
		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (items == null)
		{
			return;
		}
		Widget[] children = items.getDynamicChildren();
		clientThread.invokeAtTickEnd(() -> layOut(items, children, true));
	}

	/** The plan changed while the tab is open. */
	void refresh()
	{
		if (!button.isActive())
		{
			draws = Collections.emptyList();
			return;
		}
		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (items == null || items.isHidden())
		{
			return;
		}
		layOut(items, items.getDynamicChildren(), false);
	}

	void shutDown()
	{
		draws = Collections.emptyList();
	}

	List<Draw> getDraws()
	{
		return draws;
	}

	private void layOut(Widget items, Widget[] children, boolean afterBuild)
	{
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (bank == null || children == null)
		{
			return;
		}
		// Read this before setScrollHeight/revalidate, which can reset the widget's scroll.
		int beforeScroll = items.getScrollY();
		Widget title = client.getWidget(InterfaceID.Bankmain.TITLE);
		if (title != null && button.isActive())
		{
			title.setText("Tab <col=ff0000>Farm run</col>");
		}

		Map<Integer, Widget> byItemId = new HashMap<>();
		for (Widget child : children)
		{
			if (child == null)
			{
				continue;
			}
			int id = child.getItemId();
			if (id > 0)
			{
				byItemId.putIfAbsent(id, child);
			}
			boolean item = id > 0;
			boolean tagHeader = child.getSpriteId() == SpriteID.TRADEBACKING_DARK
				|| (child.getText() != null && child.getText().contains("Tab"));
			if (item || tagHeader)
			{
				child.setHidden(true);
			}
		}

		Map<RunPlan.Group, List<RunPlan.Withdrawal>> sections = new EnumMap<>(RunPlan.Group.class);
		Map<RunPlan.Withdrawal, Integer> idByLine = new HashMap<>();
		plugin.getBankLinesById().forEach((id, line) -> idByLine.put(line, id));
		for (RunPlan.Withdrawal line : plugin.getBankLines())
		{
			sections.computeIfAbsent(line.group, k -> new ArrayList<>()).add(line);
		}

		List<Draw> out = new ArrayList<>();
		int height = 4;
		out.add(Draw.text("/ N needed  \u2022  green carried, yellow bank/leprechaun, red missing",
			START_X, height, HEADER, FontID.PLAIN_12));
		height += 18;

		for (RunPlan.Group group : RunPlan.Group.values())
		{
			List<RunPlan.Withdrawal> lines = sections.get(group);
			if (lines == null || lines.isEmpty())
			{
				continue;
			}
			// Section header and a thin line under it.
			out.add(Draw.rect(START_X, height, PER_ROW * X_SPACING, 1, LINE));
			out.add(Draw.text(group.getLabel(), START_X, height + 3, HEADER, FontID.PLAIN_12));
			height += 20;

			List<RunPlan.Withdrawal> ordered = new ArrayList<>(lines);
			if (group == RunPlan.Group.RUNES)
			{
				// The pouch saves the most slots, so it goes before the loose runes.
				ordered.sort((a, b) -> Boolean.compare(isPouch(b.itemName), isPouch(a.itemName)));
			}
			int placed = 0;
			for (RunPlan.Withdrawal line : ordered)
			{
				Integer id = idByLine.get(line);
				Widget widget = id == null ? null : byItemId.get(id);
				// A bank placeholder has a widget but zero stock; it is not "in bank".
				int stocked = id == null ? 0 : stocked(bank, id);
				boolean inBank = widget != null && stocked > 0;
				int index = placed++;
				int x = START_X + (index % PER_ROW) * X_SPACING;
				int y = height + (index / PER_ROW) * Y_SPACING;
				if (inBank)
				{
					place(widget, index, height);
					widget.setItemQuantity(stocked);
					widget.setItemQuantityMode(ItemQuantityMode.ALWAYS);
					widget.setName(itemName(line));
					widget.setHidden(false);
					widget.revalidate();
				}
				else if (id != null && id > 0)
				{
					int have = plugin.getCarriedForBank().getOrDefault(line.itemName, 0);
					out.add(Draw.icon(id, x, y, have < line.quantity));
				}
				addLabel(out, line, x, y, inBank);
			}
			height = rowsEnd(height, placed);
		}

		draws = out;
		items.setScrollHeight(Math.max(height, items.getHeight()));
		// After a bank rebuild the game may have jumped to the top: restore the position the
		// player had before it. A plain refresh must keep exactly what is on screen now, unless
		// the rebuild already reset it to 0 while a saved position is waiting.
		int current = beforeScroll;
		int scroll = afterBuild
			? (savedScroll >= 0 ? savedScroll : current)
			: (current == 0 && savedScroll > 0 ? savedScroll : current);
		savedScroll = scroll;
		items.setScrollY(scroll);
		client.setVarcIntValue(VarClientID.BANK_SCROLLPOS, scroll);
		clientThread.invokeLater(() ->
		{
			// Reapply once more after the bank's own rebuild/render finishes.
			items.setScrollY(scroll);
			client.setVarcIntValue(VarClientID.BANK_SCROLLPOS, scroll);
			client.runScript(ScriptID.UPDATE_SCROLLBAR,
				InterfaceID.Bankmain.SCROLLBAR, InterfaceID.Bankmain.ITEMS, scroll);
		});
	}

	private void addLabel(List<Draw> out, RunPlan.Withdrawal line, int x, int y, boolean inBank)
	{
		int have = plugin.getCarriedForBank().getOrDefault(line.itemName, 0);
		int atLeprechaun = plugin.getLeprechaunItems().getOrDefault(line.itemName, 0);
		int inVault = plugin.getSeedVaultItems().getOrDefault(line.itemName, 0);
		boolean carried = have >= line.quantity;
		boolean source = atLeprechaun >= line.quantity || inVault >= line.quantity;
		Color colour = carried ? CARRIED : inBank || source ? IN_BANK : MISSING;

		out.add(Draw.text("/ " + QuantityFormatter.quantityToStackSize(line.quantity),
			x - 5, y + 37, colour, FontID.BOLD_12));
		// A small state dot at the right of the number.
		out.add(Draw.rect(x + 34, y + 39, 7, 7, colour));
		out.add(Draw.text(line.noted ? "(noted)" : "(unnoted)", x - 5, y + 51,
			line.noted ? NOTED : UNNOTED, FontID.PLAIN_11));
	}

	private static boolean isPouch(String itemName)
	{
		String name = itemName == null ? "" : itemName.toLowerCase();
		return name.contains("rune pouch") || name.endsWith(" pouch");
	}

	private static String itemName(RunPlan.Withdrawal line)
	{
		return "<col=ff9040>" + line.itemName + (line.noted ? " (noted)" : " (unnoted)") + "</col>";
	}

	private static void place(Widget widget, int index, int top)
	{
		int x = START_X + (index % PER_ROW) * X_SPACING;
		int y = top + (index / PER_ROW) * Y_SPACING;
		widget.setOriginalX(x);
		widget.setOriginalY(y);
	}

	/** Stock of a real bank item, ignoring placeholders with zero charges/quantity. */
	private int stocked(ItemContainer bank, int id)
	{
		int count = 0;
		for (Item item : bank.getItems())
		{
			if (item.getId() != id || item.getQuantity() <= 0)
			{
				continue;
			}
			ItemComposition composition = client.getItemDefinition(item.getId());
			if (composition != null && composition.getPlaceholderTemplateId() != -1
				&& composition.getPlaceholderId() != -1)
			{
				continue;
			}
			count += item.getQuantity();
		}
		return count;
	}

	private static int rowsEnd(int height, int placed)
	{
		return height + ((placed + PER_ROW - 1) / PER_ROW) * Y_SPACING;
	}
}
