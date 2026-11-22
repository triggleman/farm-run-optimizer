package com.herbrunhelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Outlines the teleport spell for your next stop in the spellbook (e.g. Falador Teleport). */
class SpellHighlightOverlay extends Overlay
{
	private static final Map<String, Integer> SPELLS = Map.of(
		"Varrock Teleport", InterfaceID.MagicSpellbook.VARROCK_TELEPORT,
		"Lumbridge Teleport", InterfaceID.MagicSpellbook.LUMBRIDGE_TELEPORT,
		"Falador Teleport", InterfaceID.MagicSpellbook.FALADOR_TELEPORT,
		"Camelot Teleport", InterfaceID.MagicSpellbook.CAMELOT_TELEPORT,
		"Kourend Castle Teleport", InterfaceID.MagicSpellbook.KOUREND_TELEPORT,
		"Ardougne Teleport", InterfaceID.MagicSpellbook.ARDOUGNE_TELEPORT,
		"Trollheim Teleport", InterfaceID.MagicSpellbook.TROLLHEIM_TELEPORT,
		"Civitas illa Fortis Teleport", InterfaceID.MagicSpellbook.FORTIS_TELEPORT);

	private final Client client;
	private final HerbRunPlugin plugin;
	private final HerbRunConfig config;

	@Inject
	SpellHighlightOverlay(Client client, HerbRunPlugin plugin, HerbRunConfig config)
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
		String spell = plugin.getSpellToHighlight();
		if (spell == null)
		{
			return null;
		}
		Widget w = findSpell(spell);
		if (w == null)
		{
			return null;
		}
		Rectangle bounds = w.getBounds();
		if (bounds == null || bounds.width <= 0)
		{
			return null;
		}
		Color c = config.stepColor();
		graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
		graphics.fill(bounds);
		graphics.setColor(c);
		graphics.setStroke(new BasicStroke(2));
		graphics.draw(bounds);
		return null;
	}

	/** The spell's icon in the open spellbook, by the game's component id. */
	private Widget findSpell(String spell)
	{
		Integer component = SPELLS.get(spell);
		if (component == null)
		{
			return null;
		}
		Widget w = client.getWidget(component);
		return w == null || w.isHidden() ? null : w;
	}
}
