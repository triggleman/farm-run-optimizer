package com.herbrunhelper;

import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.plugins.bank.BankSearch;

/**
 * The button in the bank title bar that opens the Farm run tab. This is the same bank-tab
 * mechanism Quest Helper and Farm Run Autopilot use: a normal bank-tab button, not a Bank Tags
 * tag.
 */
@Singleton
class FarmBankTabButton
{
	static final String NAME = "farm run optimizer";
	private static final String VIEW_TAB = "View tab ";
	private static final int POTION_STORE_VARBIT = 4150;
	private static final int POTION_STORE_TAB = 15;
	private static final int BUTTON_SIZE = 25;
	private static final int BUTTON_X = 380;
	private static final int BUTTON_Y = 5;
	private static final int TAB_BUTTON = 195;
	private static final int TAB_BUTTON_ACTIVE = 196;
	private static final int TAB_ICON = 217;
	private static final int SEARCH_SPRITE = 170;
	private static final int SOUND_TAB = 2266;

	private final Client client;
	private final ClientThread clientThread;
	private final BankSearch bankSearch;
	private final ChatMessageManager chatMessageManager;
	private final HerbRunConfig config;
	private boolean active;
	private boolean attention;
	private int pulseTicks;
	private Widget background;
	private Widget icon;

	@Inject
	FarmBankTabButton(Client client, ClientThread clientThread, BankSearch bankSearch,
		ChatMessageManager chatMessageManager, HerbRunConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.bankSearch = bankSearch;
		this.chatMessageManager = chatMessageManager;
		this.config = config;
	}

	void init()
	{
		if (isBankHidden())
		{
			return;
		}
		Widget parent = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		if (parent == null || isChildOf(parent, background))
		{
			return; // already built for the bank that is open
		}
		background = createGraphic(parent, NAME, TAB_BUTTON, BUTTON_SIZE, BUTTON_X, BUTTON_Y);
		icon = createGraphic(parent, NAME, TAB_ICON, BUTTON_SIZE - 6, BUTTON_X + 3, BUTTON_Y + 3);
		for (Widget part : new Widget[]{background, icon})
		{
			part.setAction(1, VIEW_TAB);
			part.setOnOpListener((Object[]) new Object[]{(JavaScriptCallback) this::onClick});
		}
		if (active)
		{
			active = false;
			clientThread.invokeLater(this::activate);
		}
	}

	void setAttention(boolean attention)
	{
		this.attention = attention;
	}

	/** The bank interface was loaded again; the old widgets belong to the previous one. */
	void reload()
	{
		Widget parent = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		if (isChildOf(parent, icon))
		{
			icon.setHidden(true);
		}
		if (isChildOf(parent, background))
		{
			background.setHidden(true);
		}
		background = null;
		icon = null;
		init();
	}

	void onGameTick()
	{
		if (background == null || background.isHidden() || isBankHidden())
		{
			return;
		}
		boolean lit = active || (attention || config.bankTabGlow()) && pulseTicks++ % 2 == 0;
		int sprite = lit ? TAB_BUTTON_ACTIVE : TAB_BUTTON;
		if (background.getSpriteId() != sprite)
		{
			background.setSpriteId(sprite);
			background.revalidate();
		}
	}

	Rectangle callingBounds()
	{
		if (!attention || active || background == null || background.isHidden() || isBankHidden())
		{
			return null;
		}
		return background.getBounds();
	}

	/** The button's on-screen bounds while the bank is open, or null if it isn't showing. */
	Rectangle bounds()
	{
		if (background == null || background.isHidden() || isBankHidden())
		{
			return null;
		}
		Rectangle b = background.getBounds();
		return b == null || b.width <= 0 ? null : b;
	}

	boolean isBankOpen()
	{
		return !isBankHidden();
	}

	void destroy()
	{
		if (active)
		{
			close();
			bankSearch.reset(true);
		}
		if (icon != null)
		{
			icon.setHidden(true);
		}
		if (background != null)
		{
			background.setHidden(true);
		}
		icon = null;
		background = null;
		active = false;
	}

	void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!active || isBankHidden())
		{
			return;
		}
		String option = event.getMenuOption();
		String target = event.getMenuTarget();
		Widget clicked = event.getWidget();
		boolean own = clicked == background || clicked == icon
			|| (target != null && (target.contains(NAME)
				|| target.toLowerCase().contains("farm run")));
		boolean otherTab = option.startsWith("View tab") && !own;
		if (otherTab || option.equals("View all items") || option.startsWith("View tag tab")
			|| option.startsWith("Potion store"))
		{
			close();
		}
	}

	void onSearchToggled()
	{
		if (active)
		{
			close();
			client.setVarcStrValue(359, "");
			client.setVarcIntValue(5, 0);
		}
	}

	private void onClick(ScriptEvent event)
	{
		if (event.getOp() != 2)
		{
			return;
		}
		if (client.getVarbitValue(POTION_STORE_VARBIT) == POTION_STORE_TAB)
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage("Close the potion store first, then open the farm run tab.")
				.build());
			return;
		}
		client.setVarbit(POTION_STORE_VARBIT, 0);
		if (active)
		{
			close();
			bankSearch.reset(true);
		}
		else
		{
			activate();
		}
		client.playSoundEffect(SOUND_TAB);
	}

	private void activate()
	{
		if (active || background == null)
		{
			return;
		}
		background.setSpriteId(TAB_BUTTON_ACTIVE);
		background.revalidate();
		active = true;
		bankSearch.reset(true);
		clearSearchButton();
	}

	private void close()
	{
		active = false;
		if (background != null)
		{
			background.setSpriteId(TAB_BUTTON);
			background.revalidate();
		}
	}

	private void clearSearchButton()
	{
		Widget search = client.getWidget(InterfaceID.Bankmain.SEARCH);
		if (search != null)
		{
			search.setOnTimerListener((Object[]) null);
			search.setSpriteId(SEARCH_SPRITE);
		}
	}

	private boolean isBankHidden()
	{
		Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		return bank == null || bank.isHidden();
	}

	private static boolean isChildOf(Widget parent, Widget child)
	{
		if (parent == null || child == null)
		{
			return false;
		}
		Widget[] children = parent.getChildren();
		if (children == null)
		{
			return false;
		}
		for (Widget candidate : children)
		{
			if (candidate == child)
			{
				return true;
			}
		}
		return false;
	}

	private static Widget createGraphic(Widget parent, String name, int spriteId, int size, int x, int y)
	{
		Widget widget = parent.createChild(-1, WidgetType.GRAPHIC);
		widget.setOriginalWidth(size);
		widget.setOriginalHeight(size);
		widget.setOriginalX(x);
		widget.setOriginalY(y);
		widget.setSpriteId(spriteId);
		widget.setHasListener(true);
		widget.setName(name);
		widget.revalidate();
		return widget;
	}

	boolean isActive()
	{
		return active;
	}
}
