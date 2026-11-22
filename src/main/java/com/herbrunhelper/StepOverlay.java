package com.herbrunhelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Quest Helper-style info box for the current step: "Getting ready" with the missing items
 * before the run, then the action and item at each patch. It stays in one corner instead of
 * following the character around the screen.
 */
class StepOverlay extends OverlayPanel
{
	private static final Color GOLD = new Color(255, 200, 60);
	private static final Color MUTED = new Color(175, 175, 175);
	private static final Color ITEM = new Color(255, 215, 90);
	private static final Color GREEN = new Color(90, 200, 90);
	private static final Color SOURCE = new Color(255, 210, 70);

	private final HerbRunPlugin plugin;
	private final HerbRunConfig config;
	private final Client client;

	@Inject
	StepOverlay(HerbRunPlugin plugin, HerbRunConfig config, Client client)
	{
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showStepOverlay())
		{
			return null;
		}
		// The bank has its own Farm run view; don't put the infobox over it.
		Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		if (bank != null && !bank.isHidden())
		{
			return null;
		}
		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(235, 0));

		if (config.waitUntilReady() && !plugin.isReadyToRun())
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Farm run: getting ready").color(GOLD).build());
			for (HerbRunPlugin.ReadyItem item : plugin.getReadyItems())
			{
				String prefix = item.state == HerbRunPlugin.ReadyItem.CARRIED ? "\u2714 "
					: item.state == HerbRunPlugin.ReadyItem.AT_SOURCE ? "\u2022 " : "\u2716 ";
				Color colour = item.state == HerbRunPlugin.ReadyItem.CARRIED ? GREEN
					: item.state == HerbRunPlugin.ReadyItem.AT_SOURCE ? SOURCE : Color.WHITE;
				panelComponent.getChildren().add(LineComponent.builder()
					.left(prefix + item.text).leftColor(colour).build());
			}
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Green = carried. Take the rest from your bank.").leftColor(MUTED).build());
			return super.render(graphics);
		}

		String action = plugin.getStepAction();
		if (action == null || action.isEmpty())
		{
			return null;
		}
		panelComponent.getChildren().add(TitleComponent.builder().text("Farm run").color(GOLD).build());
		panelComponent.getChildren().add(LineComponent.builder().left(action).leftColor(Color.WHITE).build());
		String item = plugin.getStepItem();
		if (item != null && !item.isEmpty())
		{
			panelComponent.getChildren().add(LineComponent.builder().left(item).leftColor(ITEM).build());
		}
		FarmPatch next = plugin.getNextPatch();
		if (next != null)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Next: " + next.getDisplayName()).leftColor(MUTED).build());
		}
		return super.render(graphics);
	}
}
