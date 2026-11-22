package com.herbrunhelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Shows where to go on the minimap: a marker on the target when it's on the minimap, otherwise an
 * arrow near the edge pointing towards it. Works without the Shortest Path plugin.
 */
class MinimapArrowOverlay extends Overlay
{
	/** How far out (tiles) to place the edge arrow; stays inside the minimap at any zoom. */
	private static final int EDGE_TILES = 13;

	private final Client client;
	private final HerbRunPlugin plugin;
	private final HerbRunConfig config;

	@Inject
	MinimapArrowOverlay(Client client, HerbRunPlugin plugin, HerbRunConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Shortest Path draws its own path/arrow on the minimap. Don't draw a second one.
		if (plugin.shortestPathActive())
		{
			return null;
		}
		WorldPoint target = plugin.getNavTarget();
		Player player = client.getLocalPlayer();
		if (target == null || player == null)
		{
			return null;
		}
		WorldPoint me = player.getWorldLocation();
		LocalPoint meLocal = player.getLocalLocation();
		if (me == null || meLocal == null || me.getPlane() != target.getPlane())
		{
			return null;
		}
		Point centre = Perspective.localToMinimap(client, meLocal);
		if (centre == null)
		{
			return null;
		}
		Color color = config.patchColor();
		if (!plugin.showArrow())
		{
			return null;
		}
		int dx = target.getX() - me.getX();
		int dy = target.getY() - me.getY();
		double dist = Math.sqrt((double) dx * dx + (double) dy * dy);
		if (dist < 1 || dist > 120)
		{
			return null; // further than that you teleport first, so no arrow across the map
		}
		if (dist <= EDGE_TILES)
		{
			// On the minimap: a ring marker on the spot.
			LocalPoint lp = LocalPoint.fromWorld(client, target);
			Point p = lp == null ? null : Perspective.localToMinimap(client, lp);
			if (p != null)
			{
				graphics.setStroke(new BasicStroke(2));
				graphics.setColor(Color.BLACK);
				graphics.drawOval(p.getX() - 5, p.getY() - 5, 10, 10);
				graphics.setColor(color);
				graphics.fillOval(p.getX() - 4, p.getY() - 4, 8, 8);
			}
			return null;
		}
		// Off the minimap: an arrow part-way out, pointing towards the target. Working it out from
		// a real tile in that direction keeps it right however the minimap is turned.
		double ux = dx / dist;
		double uy = dy / dist;
		WorldPoint towards = new WorldPoint(me.getX() + (int) Math.round(ux * EDGE_TILES),
			me.getY() + (int) Math.round(uy * EDGE_TILES), me.getPlane());
		LocalPoint lp = LocalPoint.fromWorld(client, towards);
		Point tip = lp == null ? null : Perspective.localToMinimap(client, lp);
		if (tip == null)
		{
			return null;
		}
		double ax = tip.getX() - centre.getX();
		double ay = tip.getY() - centre.getY();
		double len = Math.sqrt(ax * ax + ay * ay);
		if (len < 1)
		{
			return null;
		}
		ax /= len;
		ay /= len;
		double px = -ay;
		double py = ax;
		int tx = tip.getX();
		int ty = tip.getY();
		int[] xs = {tx, (int) (tx - ax * 10 + px * 6), (int) (tx - ax * 10 - px * 6)};
		int[] ys = {ty, (int) (ty - ay * 10 + py * 6), (int) (ty - ay * 10 - py * 6)};
		Polygon arrow = new Polygon(xs, ys, 3);
		graphics.setStroke(new BasicStroke(2));
		graphics.setColor(Color.BLACK);
		graphics.draw(arrow);
		graphics.setColor(color);
		graphics.fill(arrow);
		return null;
	}
}
