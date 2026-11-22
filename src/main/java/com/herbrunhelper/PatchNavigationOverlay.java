package com.herbrunhelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Highlights the next patch on the ground. It snaps to the real patch object when one is
 * loaded near the target; otherwise it outlines the target tile.
 */
public class PatchNavigationOverlay extends Overlay
{
	private final Client client;
	private final HerbRunPlugin plugin;
	private final HerbRunConfig config;

	@Inject
	PatchNavigationOverlay(Client client, HerbRunPlugin plugin, HerbRunConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		renderGardener(graphics);
		FarmPatch next = plugin.getNextPatch();
		if (!config.highlightPatch() || next == null)
		{
			return null;
		}
		WorldPoint target = plugin.tileFor(next);
		Color color = config.patchColor();
		Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);

		// Heading to a fairy ring first: mark the ring and say what to dial.
		WorldPoint ring = plugin.getNavRing();
		if (ring != null)
		{
			LocalPoint lp = LocalPoint.fromWorld(client, ring);
			if (lp == null)
			{
				return null;
			}
			net.runelite.api.TileObject ringObj = plugin.getNavRingObject();
			Shape ringHull = null;
			if (ringObj != null)
			{
				try
				{
					ringHull = ringObj.getClickbox();
					if (ringHull == null)
					{
						ringHull = ringObj.getCanvasTilePoly();
					}
				}
				catch (RuntimeException ex)
				{
					// The object can be stale for a frame while the scene reloads.
					ringHull = null;
				}
			}
			if (ringHull != null)
			{
				graphics.setColor(fill);
				graphics.fill(ringHull);
				graphics.setColor(color);
				graphics.setStroke(new BasicStroke(2));
				graphics.draw(ringHull);
				try
				{
					lp = ringObj.getLocalLocation();
				}
				catch (RuntimeException ignored)
				{
					// Keep the tile we already resolved from the world point.
				}
			}
			else
			{
				Polygon tile = Perspective.getCanvasTilePoly(client, lp);
				if (tile != null)
				{
					OverlayUtil.renderPolygon(graphics, tile, color);
				}
			}
			String text = plugin.getNavRingLabel();
			Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, text, 150);
			if (textLoc != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLoc, text, color);
				if (ringObj != null && !gardenerArrow)
				{
					drawArrow(graphics, textLoc, graphics.getFontMetrics().stringWidth(text), color);
				}
			}
			return null;
		}

		// The real patch object (matched by its farming varbit), when it's loaded.
		GameObject best = plugin.objectFor(next);

		LocalPoint labelPoint = null;
		if (best != null)
		{
			// Some grown trees have no model outline: fall back to the click area, then the
			// patch's floor area, so the patch is always marked.
			Shape hull = null;
			LocalPoint bestLocal = null;
			try
			{
				hull = best.getConvexHull();
				if (hull == null)
				{
					hull = best.getClickbox();
				}
				bestLocal = best.getLocalLocation();
				if (hull == null)
				{
					int size = next.getType().patchSize();
					hull = bestLocal == null ? null : Perspective.getCanvasTileAreaPoly(client, bestLocal, size);
				}
			}
			catch (RuntimeException ex)
			{
				// The object can be stale for a frame while the scene reloads.
				hull = null;
				bestLocal = null;
			}
			if (hull != null)
			{
				graphics.setColor(fill);
				graphics.fill(hull);
				graphics.setColor(color);
				graphics.setStroke(new BasicStroke(2));
				graphics.draw(hull);
			}
			labelPoint = bestLocal;
		}

		if (labelPoint == null)
		{
			labelPoint = LocalPoint.fromWorld(client, target);
			if (labelPoint == null)
			{
				return null; // not in the loaded area yet
			}
			Polygon tile = Perspective.getCanvasTilePoly(client, labelPoint);
			if (tile != null)
			{
				OverlayUtil.renderPolygon(graphics, tile, color);
			}
		}

		if (labelPoint != null)
		{
			String text = next.getDisplayName();
			Point textLoc = Perspective.getCanvasTextLocation(client, graphics, labelPoint, text, 150);
			if (textLoc != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLoc, text, color);
				// The arrow only goes on the real patch once it's loaded, never on a guessed tile.
				if (best != null && !gardenerArrow)
				{
					drawArrow(graphics, textLoc, graphics.getFontMetrics().stringWidth(text), color);
				}
			}
		}
		return null;
	}

	/** Set each frame: the arrow is on the gardener instead of the patch. */
	private boolean gardenerArrow;

	/**
	 * A steady arrow pointing down at a label, drawn by this plugin (the game's own hint arrow
	 * blinks and drifts). {@code textLoc} is the label's baseline-left point.
	 */
	private void drawArrow(Graphics2D graphics, Point textLoc, int textWidth, Color color)
	{
		if (!plugin.showArrow())
		{
			return;
		}
		int cx = textLoc.getX() + textWidth / 2;
		int tipY = textLoc.getY() - graphics.getFontMetrics().getAscent() - 4;
		int[] xs = {cx, cx - 9, cx - 4, cx - 4, cx + 4, cx + 4, cx + 9};
		int[] ys = {tipY, tipY - 10, tipY - 10, tipY - 22, tipY - 22, tipY - 10, tipY - 10};
		Polygon arrow = new Polygon(xs, ys, xs.length);
		graphics.setStroke(new BasicStroke(2));
		graphics.setColor(Color.BLACK);
		graphics.draw(arrow);
		graphics.setColor(color);
		graphics.fill(arrow);
	}

	/** Outlines the gardener you need to pay, with what to give them. */
	private void renderGardener(Graphics2D graphics)
	{
		gardenerArrow = false;
		net.runelite.api.NPC npc = plugin.getGardenerToPay();
		String text = plugin.getPayLabel();
		if (npc == null || text == null)
		{
			return;
		}
		gardenerArrow = true;
		Color gold = new Color(255, 200, 60);
		Shape hull = null;
		LocalPoint lp = null;
		int height = 0;
		try
		{
			hull = npc.getConvexHull();
			lp = npc.getLocalLocation();
			height = npc.getLogicalHeight();
		}
		catch (RuntimeException ex)
		{
			// The NPC can be stale for a frame while the scene reloads.
			return;
		}
		if (hull != null)
		{
			graphics.setColor(new Color(255, 200, 60, 40));
			graphics.fill(hull);
			graphics.setColor(gold);
			graphics.setStroke(new BasicStroke(2));
			graphics.draw(hull);
		}
		if (lp != null)
		{
			Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, text, height + 40);
			if (textLoc != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLoc, text, gold);
				drawArrow(graphics, textLoc, graphics.getFontMetrics().stringWidth(text), gold);
			}
		}
	}
}
