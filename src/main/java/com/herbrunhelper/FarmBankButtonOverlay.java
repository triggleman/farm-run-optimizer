package com.herbrunhelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A small growing-garden attention effect around the bank's Farm run button while its tab is
 * closed: a breathing green halo, a slow ring of tiny leaves and a few golden fireflies rising
 * from the button.
 */
class FarmBankButtonOverlay extends Overlay
{
	private static final Color LEAF = new Color(120, 230, 90);
	private static final Color GOLD = new Color(255, 215, 90);
	private static final Color SPORE = new Color(220, 255, 170);

	private final FarmBankButton button;
	private final HerbRunConfig config;

	@Inject
	FarmBankButtonOverlay(FarmBankButton button, HerbRunConfig config)
	{
		this.button = button;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.animateBankButton())
		{
			return null;
		}
		Rectangle bounds = button.attentionBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return null;
		}

		long now = System.currentTimeMillis();
		double pulse = 0.5 + 0.5 * Math.sin(now / 420.0);
		int cx = bounds.x + bounds.width / 2;
		int cy = bounds.y + bounds.height / 2;

		// Warm the button itself a little, so it visibly lights up and dims.
		int sheen = (int) (24 + 22 * pulse);
		graphics.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), sheen));
		graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);

		// A soft green halo that breathes around the frame.
		Stroke oldStroke = graphics.getStroke();
		graphics.setStroke(new BasicStroke(1.4f));
		for (int ring = 0; ring < 3; ring++)
		{
			int pad = 2 + ring;
			int alpha = (int) ((18 + 28 * pulse) / (ring + 1));
			graphics.setColor(new Color(LEAF.getRed(), LEAF.getGreen(), LEAF.getBlue(), alpha));
			graphics.drawRoundRect(bounds.x - pad, bounds.y - pad,
				bounds.width + pad * 2, bounds.height + pad * 2, 8, 8);
		}
		graphics.setStroke(oldStroke);

		// A slow ring of tiny leaves, alternating in brightness.
		int leaves = 8;
		for (int i = 0; i < leaves; i++)
		{
			double angle = now / 1250.0 + i * Math.PI * 2 / leaves;
			double orbit = 14 + 1.5 * pulse;
			int x = cx + (int) Math.round(Math.cos(angle) * orbit);
			int y = cy + (int) Math.round(Math.sin(angle) * orbit * 0.78);
			int alpha = (int) (90 + 90 * (0.5 + 0.5 * Math.sin(now / 260.0 + i)));
			drawLeaf(graphics, x, y, alpha);
		}

		// Fireflies rising from the bottom edge and fading out.
		for (int i = 0; i < 4; i++)
		{
			double t = ((now % 1900) / 1900.0 + i / 4.0) % 1.0;
			int x = bounds.x + 3 + (int) Math.round((bounds.width - 6) * ((i * 0.37 + 0.1) % 1.0))
				+ (int) Math.round(Math.sin(now / 260.0 + i * 2) * 2);
			int y = bounds.y + bounds.height - 1 - (int) Math.round(t * 18);
			int alpha = Math.max(0, (int) (210 * Math.sin(Math.PI * t)));
			Color colour = i % 2 == 0 ? GOLD : SPORE;
			graphics.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha));
			graphics.fillOval(x, y, 3, 3);
			graphics.setColor(new Color(255, 255, 240, Math.max(0, alpha / 2)));
			graphics.fillRect(x + 1, y + 1, 1, 1);
		}
		return null;
	}

	private static void drawLeaf(Graphics2D graphics, int x, int y, int alpha)
	{
		alpha = Math.max(0, Math.min(255, alpha));
		graphics.setColor(new Color(LEAF.getRed(), LEAF.getGreen(), LEAF.getBlue(), alpha));
		graphics.fillRect(x, y - 1, 3, 3);
		graphics.fillRect(x - 1, y, 1, 1);
		graphics.fillRect(x + 3, y, 1, 1);
		graphics.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), alpha / 2));
		graphics.fillRect(x + 1, y - 2, 1, 1);
	}
}
