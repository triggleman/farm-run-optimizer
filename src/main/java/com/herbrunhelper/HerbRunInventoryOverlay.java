package com.herbrunhelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.ItemComposition;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Outlines the inventory item your current step needs and marks run items to wear. */
public class HerbRunInventoryOverlay extends WidgetItemOverlay
{
	private static final Color WEAR = new Color(255, 150, 40);

	private final HerbRunPlugin plugin;
	private final HerbRunConfig config;
	private final ItemManager itemManager;

	@Inject
	HerbRunInventoryOverlay(HerbRunPlugin plugin, HerbRunConfig config, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		boolean mark = config.markWearables();
		boolean highlight = config.highlightInventory() && plugin.getStepItem() != null;
		if (!mark && !highlight)
		{
			return;
		}
		Rectangle bounds = widgetItem.getCanvasBounds();
		// Noted items (gardener payments) canonicalize to the unnoted item's name.
		ItemComposition canonical = itemManager.getItemComposition(itemManager.canonicalize(itemId));
		if (canonical == null)
		{
			return;
		}
		String name = canonical.getName();

		if (config.markWearables() && plugin.isWearItem(name))
		{
			drawWear(graphics, bounds);
		}

		if (!highlight || !plugin.isStepItem(name))
		{
			return;
		}
		Color c = config.stepColor();
		graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
		graphics.fill(bounds);
		graphics.setColor(c);
		graphics.setStroke(new BasicStroke(2));
		graphics.draw(bounds);
	}

	/** A small orange WEAR tag under an equippable run item. */
	private static void drawWear(Graphics2D g, Rectangle b)
	{
		g.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = g.getFontMetrics();
		String text = "WEAR";
		int w = fm.stringWidth(text) + 6;
		int h = fm.getAscent() + 2;
		int x = b.x + (b.width - w) / 2;
		int y = b.y + b.height + 2;
		g.setColor(new Color(0, 0, 0, 170));
		g.fillRoundRect(x, y, w, h, 4, 4);
		g.setColor(WEAR);
		g.drawString(text, x + 3, y + fm.getAscent() + 1);
	}
}
