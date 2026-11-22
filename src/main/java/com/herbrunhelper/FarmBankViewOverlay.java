package com.herbrunhelper;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Draws the Farm run bank view's section headers, "/ N" labels, (noted)/(unnoted) text and the
 * icons for items the bank doesn't hold. Everything is clipped to the bank's item area, so it
 * never spills over the game or the rest of the interface when the list scrolls.
 */
class FarmBankViewOverlay extends Overlay
{
	private final Client client;
	private final FarmBankLayout layout;
	private final FarmBankButton button;
	private final ItemManager itemManager;
	private final Font regular = FontManager.getRunescapeFont();
	private final Font small = FontManager.getRunescapeSmallFont();

	@Inject
	FarmBankViewOverlay(Client client, FarmBankLayout layout, FarmBankButton button, ItemManager itemManager)
	{
		this.client = client;
		this.layout = layout;
		this.button = button;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!button.isActive())
		{
			return null;
		}
		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (items == null || items.isHidden())
		{
			return null;
		}
		Point base = items.getCanvasLocation();
		if (base == null)
		{
			return null;
		}
		int scroll = items.getScrollY();
		Shape oldClip = graphics.getClip();
		graphics.clip(items.getBounds());

		for (FarmBankLayout.Draw draw : layout.getDraws())
		{
			int x = base.getX() + draw.x;
			int y = base.getY() + draw.y - scroll;
			if (draw.kind == FarmBankLayout.Draw.TEXT)
			{
				Font font = font(draw.fontId);
				graphics.setFont(font);
				FontMetrics metrics = graphics.getFontMetrics(font);
				int baseline = y + metrics.getAscent();
				graphics.setColor(new Color(0, 0, 0, 180));
				graphics.drawString(draw.text, x + 1, baseline + 1);
				graphics.setColor(new Color(draw.colour));
				graphics.drawString(draw.text, x, baseline);
			}
			else if (draw.kind == FarmBankLayout.Draw.RECT)
			{
				graphics.setColor(new Color(draw.colour));
				graphics.fillRect(x, y, draw.width, draw.height);
			}
			else if (draw.kind == FarmBankLayout.Draw.ICON)
			{
				AsyncBufferedImage image = itemManager.getImage(draw.itemId);
				if (image == null)
				{
					continue;
				}
				Composite old = graphics.getComposite();
				if (draw.faded)
				{
					graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
				}
				graphics.drawImage(image, x, y, null);
				graphics.setComposite(old);
			}
		}

		graphics.setClip(oldClip);
		return null;
	}

	private Font font(int fontId)
	{
		if (fontId == FontID.BOLD_12)
		{
			return FontManager.getRunescapeBoldFont();
		}
		if (fontId == FontID.PLAIN_11)
		{
			return small;
		}
		return regular;
	}
}
