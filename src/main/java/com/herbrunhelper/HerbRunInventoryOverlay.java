package com.herbrunhelper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Outlines the inventory item your current step needs (seed, compost, payment...). */
public class HerbRunInventoryOverlay extends WidgetItemOverlay
{
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
		if (!config.highlightInventory() || plugin.getStepItem() == null)
		{
			return;
		}
		// Noted items (gardener payments) canonicalize to the unnoted item's name.
		String name = itemManager.getItemComposition(itemManager.canonicalize(itemId)).getName();
		if (!plugin.isStepItem(name))
		{
			return;
		}
		// A corner marker rather than a full border: it doesn't stack with another plugin's
		// outline on the same item (Quest Helper highlights its required items too).
		marker(graphics, widgetItem.getCanvasBounds(), config.stepColor());
	}

	static void marker(Graphics2D graphics, Rectangle bounds, Color color)
	{
		int size = Math.min(10, Math.min(bounds.width, bounds.height));
		int x = bounds.x + 1;
		int y = bounds.y + 1;
		graphics.setColor(color);
		graphics.fillPolygon(new int[]{x, x + size, x}, new int[]{y, y, y + size}, 3);
	}
}
