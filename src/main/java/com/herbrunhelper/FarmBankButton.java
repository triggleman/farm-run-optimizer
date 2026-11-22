package com.herbrunhelper;

import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ScriptEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.plugins.banktags.BankTagsService;

/**
 * The farming button in the bank title bar. It opens the "Farm run" Bank Tags view with the run's
 * items and checklist. Client-side only: nothing is sent to the server.
 */
@Singleton
class FarmBankButton
{
	static final String TAG = "Farm run";
	private static final int BUTTON_SIZE = 25;
	/** Left of Quest Helper's button (x 408), so the two sit in a row without overlapping. */
	private static final int BUTTON_X = 380;
	private static final int BUTTON_Y = 5;

	private final Client client;
	private final BankTagsService bankTagsService;
	private final HerbRunConfig config;
	private Widget background;
	private Widget icon;
	private boolean pulse;

	@Inject
	FarmBankButton(Client client, BankTagsService bankTagsService, HerbRunConfig config)
	{
		this.client = client;
		this.bankTagsService = bankTagsService;
		this.config = config;
	}

	/** Adds the button when the bank opens. */
	void init()
	{
		Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		if (bank == null || bank.isHidden())
		{
			return;
		}
		// Same geometry as Quest Helper's bank button: a 25px square with a 19px icon at +3,+3.
		background = create(bank, SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL, BUTTON_SIZE, BUTTON_X, BUTTON_Y);
		// A spade/shovel is a simple, centred farming tool. The farming skill icon has uneven
		// transparent padding and looked off-centre in the 19px square.
		icon = create(bank, SpriteID.Staticons2.SHOVEL, BUTTON_SIZE - 6, BUTTON_X + 3, BUTTON_Y + 3);
		for (Widget part : new Widget[]{background, icon})
		{
			part.setAction(1, "View Farm run");
			part.setOnOpListener((JavaScriptCallback) this::onClick);
		}
		updateSprite();
	}

	boolean isActive()
	{
		return TAG.equals(bankTagsService.getActiveTag());
	}

	/**
	 * Where the button is on screen while its tab is closed, so the attention effect can be
	 * drawn around it; null when there is nothing to point at.
	 */
	Rectangle attentionBounds()
	{
		if (isActive() || background == null || background.isHidden())
		{
			return null;
		}
		Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		if (bank == null || bank.isHidden())
		{
			return null;
		}
		Rectangle bounds = background.getBounds();
		return bounds == null || bounds.width <= 0 || bounds.height <= 0 ? null : bounds;
	}

	/** Called each game tick to keep the pressed look in step with the bank. */
	void onGameTick()
	{
		// While the tab is closed, flash the button gently so the player notices it.
		pulse = !isActive() && config.animateBankButton()
			&& (System.currentTimeMillis() / 800) % 2 == 0;
		updateSprite();
	}

	void destroy()
	{
		if (isActive())
		{
			bankTagsService.closeBankTag();
		}
		if (icon != null)
		{
			icon.setHidden(true);
		}
		if (background != null)
		{
			background.setHidden(true);
		}
		background = null;
		icon = null;
	}

	private void onClick(ScriptEvent event)
	{
		if (event.getOp() != 2)
		{
			return;
		}
		if (isActive())
		{
			bankTagsService.closeBankTag();
		}
		else
		{
			bankTagsService.openBankTag(TAG, BankTagsService.OPTION_NO_LAYOUT);
		}
		updateSprite();
	}

	private void updateSprite()
	{
		if (background == null)
		{
			return;
		}
		int sprite = isActive() || pulse ? SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL_SELECTED
			: SpriteID.Miscgraphics3.UNKNOWN_BUTTON_SQUARE_SMALL;
		if (background.getSpriteId() != sprite)
		{
			background.setSpriteId(sprite);
			background.revalidate();
		}
		String name = isActive() ? TAG + " (on)" : TAG;
		if (!name.equals(background.getName()))
		{
			background.setName(name);
			if (icon != null)
			{
				icon.setName(name);
			}
		}
	}

	private static Widget create(Widget parent, int spriteId, int size, int x, int y)
	{
		Widget widget = parent.createChild(-1, WidgetType.GRAPHIC);
		widget.setOriginalWidth(size);
		widget.setOriginalHeight(size);
		widget.setOriginalX(x);
		widget.setOriginalY(y);
		widget.setSpriteId(spriteId);
		widget.setHasListener(true);
		widget.setName(TAG);
		widget.revalidate();
		return widget;
	}
}
