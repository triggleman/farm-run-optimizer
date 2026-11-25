package com.herbrunhelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.ScriptID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.Text;

/**
 * The Farm run bank tab. This is deliberately the same mechanism Quest Helper and Farm Run
 * Autopilot use: hide the bank's own item widgets, reuse them for the run's items, and add our
 * own headers and labels as children of the bank item container. No Bank Tags tag and no widget
 * moving on every bank rebuild.
 */
@Singleton
class FarmBankTab
{
	private static final int ITEMS_PER_ROW = 8;
	private static final int ITEM_VERTICAL_SPACING = 36;
	private static final int ITEM_HORIZONTAL_SPACING = 48;
	private static final int ITEM_ROW_START = 51;
	private static final int LINE_VERTICAL_SPACING = 5;
	private static final int LINE_HEIGHT = 2;
	private static final int TEXT_HEIGHT = 15;
	private static final int ICON_SIZE = 10;
	private static final int PLANNED_ROW_SPACING = 50;
	private static final int ITEM_WIDTH = 36;
	private static final int ITEM_HEIGHT = 32;
	private static final int LABEL_WIDTH = 46;
	private static final int LABEL_HEIGHT = 14;
	private static final int FADED = 120;
	private static final int BANK_FILLER = 6512;
	private static final int LINE_SPRITE = 897;
	private static final int CHECK_SPRITE = 1217;
	private static final int CROSS_SPRITE = 1216;
	private static final int WITHDRAW_QUANTITY_VARBIT = 6590;
	private static final int WITHDRAW_X_VARBIT = 3960;
	private static final int CHARGES_VARBIT = 16125;
	private static final int PLACEHOLDER_VARBIT = 3755;
	private static final int CHARGES_PARAM = 2257;
	private static final Color HEADER = new Color(228, 216, 162);
	private static final Color CARRIED = new Color(76, 193, 82);
	private static final Color IN_STORAGE = new Color(232, 197, 58);
	private static final Color MISSING = new Color(224, 85, 85);
	private static final Color DEPOSIT = new Color(255, 152, 31);
	private static final String REST_OF_BANK = "Everything else";
	private static final String DEPOSIT_FIRST = "Deposit first (outlined in your inventory)";
	private static final int DEPOSIT_LINE_CHARS = 70;
	private static final String TITLE = "Tab <col=ff0000>Farm run</col>";

	private final Client client;
	private final ClientThread clientThread;
	private final ChatMessageManager chatMessageManager;
	private final FarmBankTabButton button;
	private final List<Widget> addedWidgets = new ArrayList<>();
	private final Map<Widget, BankSupplyLine> lineByWidget = new HashMap<>();
	private int originalContainerChildren = -1;
	private volatile Supplier<List<BankSupplyLine>> plan = Collections::emptyList;
	private volatile Supplier<List<String>> deposit = Collections::emptyList;
	private volatile boolean enabled;

	@Inject
	FarmBankTab(Client client, ClientThread clientThread, ChatMessageManager chatMessageManager,
		FarmBankTabButton button)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.chatMessageManager = chatMessageManager;
		this.button = button;
	}

	void startUp(Supplier<List<BankSupplyLine>> plan, Supplier<List<String>> deposit)
	{
		this.plan = plan;
		this.deposit = deposit;
	}

	boolean isBankOpen()
	{
		return button.isBankOpen();
	}

	boolean isTabOpen()
	{
		return button.isActive() && button.isBankOpen();
	}

	void setAttention(boolean attention)
	{
		button.setAttention(attention);
	}

	void onGameTick()
	{
		if (enabled)
		{
			button.onGameTick();
		}
	}

	void shutDown()
	{
		setEnabled(false);
	}

	/**
	 * Widgets may only be touched on the client thread, and the plugin can be enabled from the
	 * config panel (AWT thread), so the actual init/destroy is always deferred.
	 */
	void setEnabled(boolean enabled)
	{
		if (this.enabled == enabled)
		{
			return;
		}
		this.enabled = enabled;
		clientThread.invokeLater(() -> applyEnabled(enabled));
	}

	private void applyEnabled(boolean enabled)
	{
		if (this.enabled != enabled)
		{
			return; // a newer toggle superseded this one
		}
		if (enabled)
		{
			button.init();
		}
		else
		{
			button.destroy();
			removeAddedWidgets();
		}
	}

	void onWidgetLoaded(WidgetLoaded event)
	{
		if (enabled && event.getGroupId() == InterfaceID.BANKMAIN)
		{
			button.reload();
		}
	}

	void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if ("getSearchingTagTab".equals(event.getEventName()) && button.isActive())
		{
			client.getIntStack()[client.getIntStackSize() - 1] = 1;
		}
	}

	void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING)
		{
			resetItemWidgetSizes();
			Widget title = client.getWidget(InterfaceID.Bankmain.TITLE);
			if (button.isActive() && title != null)
			{
				title.setText(TITLE);
			}
		}
		else if (event.getScriptId() == ScriptID.BANKMAIN_SEARCH_TOGGLE)
		{
			button.onSearchToggled();
		}
	}

	void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_SEARCHING)
		{
			if (button.isActive())
			{
				client.getIntStack()[client.getIntStackSize() - 1] = 1;
			}
			return;
		}
		if (event.getScriptId() != ScriptID.BANKMAIN_FINISHBUILDING)
		{
			return;
		}
		if (enabled)
		{
			// The bank's title bar is only ready once building finishes, so retry here in case
			// the WidgetLoaded init ran too early.
			button.init();
		}
		removeAddedWidgets();
		if (!button.isActive())
		{
			return;
		}
		Widget itemContainer = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (itemContainer == null)
		{
			return;
		}
		Widget[] children = itemContainer.getChildren();
		if (children != null && originalContainerChildren == -1)
		{
			originalContainerChildren = children.length;
		}
		Widget[] itemWidgets = itemContainer.getDynamicChildren();
		clientThread.invokeAtTickEnd(() -> layOut(itemContainer, itemWidgets, plan.get()));
	}

	void onMenuOptionClicked(MenuOptionClicked event)
	{
		button.onMenuOptionClicked(event);
		if (event.getParam1() != InterfaceID.Bankmain.ITEMS || !button.isActive())
		{
			return;
		}
		MenuEntry menu = event.getMenuEntry();
		if ("Details".equals(menu.getOption()))
		{
			event.consume();
			BankSupplyLine line = lineByWidget.get(event.getWidget());
			if (line != null)
			{
				showDetails(line);
			}
			return;
		}
		Widget widget = menu.getWidget();
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (widget != null && widget.getItemId() > -1 && bank != null)
		{
			int slot = bank.find(widget.getItemId());
			if (slot > -1 && menu.getParam0() != slot)
			{
				menu.setParam0(slot);
			}
		}
	}

	void refresh()
	{
		if (!button.isActive())
		{
			return;
		}
		Widget itemContainer = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (itemContainer == null || itemContainer.isHidden())
		{
			return;
		}
		removeAddedWidgets();
		layOut(itemContainer, itemContainer.getDynamicChildren(), plan.get());
	}

	private void layOut(Widget itemContainer, Widget[] itemWidgets, List<BankSupplyLine> lines)
	{
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (bank == null)
		{
			return;
		}
		lineByWidget.clear();
		hideBankWidgets(itemContainer, itemWidgets);

		List<BankSupplyLine> equipment = new ArrayList<>();
		List<BankSupplyLine> inventory = new ArrayList<>();
		Set<Integer> planned = new HashSet<>();
		for (BankSupplyLine line : lines)
		{
			(line.wear ? equipment : inventory).add(line);
			for (int id : line.itemIds)
			{
				planned.add(id);
			}
		}
		equipment.sort(Comparator.comparingInt(line -> groupRank(line.group)));
		inventory.sort(Comparator.comparingInt(line -> groupRank(line.group)));

		LinkedHashSet<Integer> rest = new LinkedHashSet<>();
		for (Item item : bank.getItems())
		{
			if (item.getId() <= -1 || item.getId() == BANK_FILLER || planned.contains(item.getId()))
			{
				continue;
			}
			rest.add(item.getId());
		}

		List<PendingText> pending = new ArrayList<>();
		int height = 0;
		List<String> toDeposit = deposit.get();
		if (!toDeposit.isEmpty())
		{
			height = addHeader(itemContainer, DEPOSIT_FIRST, height);
			for (String line : wrap(String.join(", ", toDeposit), DEPOSIT_LINE_CHARS))
			{
				addedWidgets.add(createText(itemContainer, line, DEPOSIT.getRGB(), 384, TEXT_HEIGHT,
					ITEM_ROW_START, height));
				height += TEXT_HEIGHT;
			}
			height += LINE_VERTICAL_SPACING;
		}

		int[] nextWidget = {0};
		if (!equipment.isEmpty())
		{
			height = addHeader(itemContainer, "Equipment", height);
			height = drawSection(itemContainer, equipment, height, nextWidget, pending, bank);
		}
		if (!inventory.isEmpty())
		{
			height = addHeader(itemContainer, "Inventory", height);
			height = drawSection(itemContainer, inventory, height, nextWidget, pending, bank);
		}

		if (!rest.isEmpty())
		{
			height = addHeader(itemContainer, REST_OF_BANK, height);
			int added = 0;
			Iterator<Integer> iterator = rest.iterator();
			while (iterator.hasNext())
			{
				Widget widget = itemContainer.getChild(nextWidget[0]++);
				if (widget == null)
				{
					break;
				}
				int id = iterator.next();
				drawItem(widget, id, bank, null);
				placeItem(widget, added++, height, ITEM_VERTICAL_SPACING);
			}
			height = rowsEnd(height, added, ITEM_VERTICAL_SPACING);
		}

		for (PendingText pendingText : pending)
		{
			int textX = pendingText.x + (pendingText.spriteId == -1 ? 2 : ICON_SIZE + 2);
			Widget label = createText(itemContainer, pendingText.text, pendingText.colour, 34,
				LABEL_HEIGHT, textX, pendingText.y);
			label.setFontId(496);
			label.setYTextAlignment(1);
			label.revalidate();
			addedWidgets.add(label);
			if (pendingText.spriteId != -1)
			{
				addedWidgets.add(createIcon(itemContainer, pendingText.spriteId,
					pendingText.x, pendingText.y + 2));
			}
		}

		itemContainer.setScrollHeight(Math.max(height, itemContainer.getHeight()));
		int scroll = itemContainer.getScrollY();
		clientThread.invokeLater(() -> client.runScript(ScriptID.UPDATE_SCROLLBAR,
			InterfaceID.Bankmain.SCROLLBAR, InterfaceID.Bankmain.ITEMS, scroll));
	}

	private int drawSection(Widget itemContainer, List<BankSupplyLine> lines, int height,
		int[] nextWidget, List<PendingText> pending, ItemContainer bank)
	{
		int added = 0;
		for (BankSupplyLine line : lines)
		{
			Widget widget = itemContainer.getChild(nextWidget[0]++);
			if (widget == null)
			{
				break;
			}
			drawItem(widget, displayItem(line, bank), bank, line);
			placeItem(widget, added++, height, PLANNED_ROW_SPACING);
			pending.add(neededText(widget, line));
		}
		return rowsEnd(height, added, PLANNED_ROW_SPACING);
	}

	private static int groupRank(RunPlan.Group group)
	{
		if (group == null)
		{
			return 5;
		}
		switch (group)
		{
			case TOOLS:
				return 0;
			case SEEDS:
				return 1;
			case RUNES:
				return 2;
			case TRAVEL:
				return 3;
			case PAYMENTS:
				return 4;
			default:
				return 5;
		}
	}

	static List<String> wrap(String text, int width)
	{
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			if (line.length() > 0 && line.length() + 1 + word.length() > width)
			{
				lines.add(line.toString());
				line.setLength(0);
			}
			if (line.length() > 0)
			{
				line.append(' ');
			}
			line.append(word);
		}
		if (line.length() > 0)
		{
			lines.add(line.toString());
		}
		return lines;
	}

	private static int displayItem(BankSupplyLine line, ItemContainer bank)
	{
		int best = line.itemIds[0];
		int bestCount = 0;
		for (int id : line.itemIds)
		{
			int count = bank.count(id);
			if (count <= bestCount)
			{
				continue;
			}
			best = id;
			bestCount = count;
		}
		return best;
	}

	private void drawItem(Widget widget, int itemId, ItemContainer bank, BankSupplyLine line)
	{
		int qty = bank.count(itemId);
		ItemComposition def = client.getItemDefinition(itemId);
		widget.setItemId(itemId);
		widget.setItemQuantity(qty);
		widget.setItemQuantityMode(1);
		widget.setDragDeadTime(1000);
		widget.setName("<col=ff9040>" + def.getName() + "</col>");
		widget.clearActions();
		if (def.getPlaceholderTemplateId() >= 0 && def.getPlaceholderId() >= 0)
		{
			widget.setOpacity(FADED);
			widget.setAction(7, "Release");
			widget.setAction(9, "Examine");
		}
		else if (qty == 0)
		{
			widget.setOpacity(FADED);
			widget.setItemQuantity(0);
			widget.setItemQuantityMode(0);
			widget.setAction(1, "Details");
		}
		else
		{
			setWithdrawActions(widget, def);
			widget.setOpacity(0);
		}
		widget.setOnDragListener(ScriptID.BANKMAIN_DRAGSCROLL, ScriptEvent.WIDGET_ID,
			ScriptEvent.WIDGET_INDEX, ScriptEvent.MOUSE_X, ScriptEvent.MOUSE_Y,
			InterfaceID.Bankmain.SCROLLBAR, 0);
		widget.setOnDragCompleteListener((JavaScriptCallback) ev ->
		{
		});
		if (line != null)
		{
			lineByWidget.put(widget, line);
		}
		widget.setHidden(false);
		widget.revalidate();
	}

	private void setWithdrawActions(Widget widget, ItemComposition def)
	{
		int quantityType = client.getVarbitValue(WITHDRAW_QUANTITY_VARBIT);
		int requested = client.getVarbitValue(WITHDRAW_X_VARBIT);
		String suffix;
		switch (quantityType)
		{
			case 1:
				suffix = "5";
				break;
			case 2:
				suffix = "10";
				break;
			case 3:
				suffix = Integer.toString(Math.max(1, requested));
				break;
			case 4:
				suffix = "All";
				break;
			default:
				suffix = "1";
		}
		widget.setAction(0, "Withdraw-" + suffix);
		if (quantityType != 0)
		{
			widget.setAction(1, "Withdraw-1");
		}
		widget.setAction(2, "Withdraw-5");
		widget.setAction(3, "Withdraw-10");
		if (requested > 0)
		{
			widget.setAction(4, "Withdraw-" + requested);
		}
		widget.setAction(5, "Withdraw-X");
		widget.setAction(6, "Withdraw-All");
		widget.setAction(7, "Withdraw-All-but-1");
		if (client.getVarbitValue(CHARGES_VARBIT) == 1 && def.getIntValue(CHARGES_PARAM) != -1)
		{
			widget.setAction(8, "Configure-Charges");
		}
		if (client.getVarbitValue(PLACEHOLDER_VARBIT) == 0)
		{
			widget.setAction(9, "Placeholder");
		}
		widget.setAction(10, "Examine");
	}

	private static PendingText neededText(Widget widget, BankSupplyLine line)
	{
		String needed = QuantityFormatter.quantityToStackSize(line.need)
			+ (line.noted ? " noted" : "");
		int x = widget.getOriginalX() - 5;
		int y = widget.getOriginalY() + ITEM_HEIGHT + 1;
		Color colour;
		int sprite;
		switch (line.status())
		{
			case CARRIED:
				colour = CARRIED;
				sprite = CHECK_SPRITE;
				break;
			case IN_STORAGE:
				colour = IN_STORAGE;
				sprite = -1;
				break;
			default:
				colour = MISSING;
				sprite = CROSS_SPRITE;
		}
		return new PendingText(needed, colour.getRGB(), x, y, sprite);
	}

	private void showDetails(BankSupplyLine line)
	{
		String message = "You need " + QuantityFormatter.formatNumber(line.need) + " x "
			+ Text.removeTags(line.name) + ". "
			+ (line.where == null || line.where.isEmpty() ? "You don't have any." : "You have " + line.where + ".");
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.ITEM_EXAMINE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	private void hideBankWidgets(Widget itemContainer, Widget[] itemWidgets)
	{
		for (int i = 0; i < itemWidgets.length; i++)
		{
			Widget widget = itemContainer.getChild(i);
			if (widget == null)
			{
				continue;
			}
			boolean item = !widget.isSelfHidden() && widget.getItemId() > -1
				&& widget.getItemId() != BANK_FILLER;
			String text = widget.getText();
			boolean tabDivider = widget.getSpriteId() == LINE_SPRITE
				|| (text != null && text.contains("Tab"));
			if (!item && !tabDivider)
			{
				continue;
			}
			widget.setHidden(true);
		}
	}

	private void resetItemWidgetSizes()
	{
		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (container == null || container.getChildren() == null)
		{
			return;
		}
		for (Widget child : container.getChildren())
		{
			if (child.getOriginalHeight() < ITEM_HEIGHT)
			{
				break;
			}
			if (child.getOriginalWidth() == ITEM_WIDTH && child.getOriginalHeight() == ITEM_HEIGHT)
			{
				continue;
			}
			child.setOriginalWidth(ITEM_WIDTH);
			child.setOriginalHeight(ITEM_HEIGHT);
			child.revalidate();
		}
	}

	private void removeAddedWidgets()
	{
		if (originalContainerChildren == -1 || addedWidgets.isEmpty())
		{
			return;
		}
		Widget parent = addedWidgets.get(0).getParent();
		if (parent != null && parent.getChildren() != null)
		{
			parent.setChildren(Arrays.copyOf(parent.getChildren(), originalContainerChildren));
			parent.revalidate();
		}
		addedWidgets.clear();
	}

	private int addHeader(Widget container, String title, int height)
	{
		addedWidgets.add(createLine(container, height));
		addedWidgets.add(createText(container, title, HEADER.getRGB(), 435, TEXT_HEIGHT,
			ITEM_ROW_START, height + LINE_VERTICAL_SPACING));
		return height + LINE_VERTICAL_SPACING + TEXT_HEIGHT;
	}

	private static int rowsEnd(int height, int items, int rowSpacing)
	{
		int rows = (items + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW;
		return height + rows * rowSpacing;
	}

	private static void placeItem(Widget widget, int index, int sectionTop, int rowSpacing)
	{
		int y = sectionTop + index / ITEMS_PER_ROW * rowSpacing;
		int x = index % ITEMS_PER_ROW * ITEM_HORIZONTAL_SPACING + ITEM_ROW_START;
		if (widget.getOriginalY() != y || widget.getOriginalX() != x)
		{
			widget.setOriginalY(y);
			widget.setOriginalX(x);
			widget.revalidate();
		}
	}

	private static Widget createLine(Widget container, int y)
	{
		Widget widget = container.createChild(-1, WidgetType.GRAPHIC);
		widget.setOriginalWidth(384);
		widget.setOriginalHeight(LINE_HEIGHT);
		widget.setOriginalX(ITEM_ROW_START);
		widget.setOriginalY(y);
		widget.setSpriteId(LINE_SPRITE);
		widget.revalidate();
		return widget;
	}

	private static Widget createText(Widget container, String text, int colour, int width, int height,
		int x, int y)
	{
		Widget widget = container.createChild(-1, WidgetType.TEXT);
		widget.setOriginalWidth(width);
		widget.setOriginalHeight(height);
		widget.setOriginalX(x);
		widget.setOriginalY(y);
		widget.setText(text);
		widget.setFontId(494);
		widget.setTextColor(colour);
		widget.setTextShadowed(true);
		widget.revalidate();
		return widget;
	}

	private static Widget createIcon(Widget container, int spriteId, int x, int y)
	{
		Widget widget = container.createChild(-1, WidgetType.GRAPHIC);
		widget.setOriginalWidth(ICON_SIZE);
		widget.setOriginalHeight(ICON_SIZE);
		widget.setOriginalX(x);
		widget.setOriginalY(y);
		widget.setSpriteId(spriteId);
		widget.revalidate();
		return widget;
	}

	private static final class PendingText
	{
		final String text;
		final int colour;
		final int x;
		final int y;
		final int spriteId;

		PendingText(String text, int colour, int x, int y, int spriteId)
		{
			this.text = text;
			this.colour = colour;
			this.x = x;
			this.y = y;
			this.spriteId = spriteId;
		}
	}
}
