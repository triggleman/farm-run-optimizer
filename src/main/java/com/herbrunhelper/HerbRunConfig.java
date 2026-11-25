package com.herbrunhelper;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(HerbRunConfig.GROUP)
public interface HerbRunConfig extends Config
{
	String GROUP = "herbrunhelper";

	@ConfigSection(name = "Run", description = "What the run includes", position = 0)
	String runSection = "run";

	@ConfigSection(name = "Navigation", description = "Showing where to go", position = 1)
	String navSection = "nav";

	@ConfigSection(name = "Bank", description = "Bank highlighting", position = 2)
	String bankSection = "bank";

	@ConfigItem(keyName = "includeHerbs", name = "Herb run", description = "Include herb patches", position = 1, section = runSection)
	default boolean includeHerbs()
	{
		return true;
	}

	@ConfigItem(keyName = "includeTrees", name = "Tree run", description = "Include tree patches", position = 2, section = runSection)
	default boolean includeTrees()
	{
		return true;
	}

	@ConfigItem(keyName = "includeFruitTrees", name = "Fruit tree run", description = "Include fruit tree patches", position = 3, section = runSection)
	default boolean includeFruitTrees()
	{
		return false;
	}

	@ConfigItem(keyName = "includeAllotments", name = "Allotment run", description = "Include allotment patches (potatoes up to snape grass) - great before you unlock herbs and trees", position = 4, section = runSection)
	default boolean includeAllotments()
	{
		return false;
	}

	@ConfigItem(keyName = "includeHardwood", name = "Hardwood run", description = "Include hardwood patches (teak, mahogany, camphor, ironwood, rosewood)", position = 3, section = runSection)
	default boolean includeHardwood()
	{
		return false;
	}

	@ConfigItem(keyName = "includeBushes", name = "Bush run", description = "Include bush patches (redberries up to poison ivy): Champions' Guild, Rimmington, Ardougne, Etceteria, Farming Guild", position = 4, section = runSection)
	default boolean includeBushes()
	{
		return false;
	}

	@ConfigItem(keyName = "includeHops", name = "Hops run", description = "Include hops patches (barley up to wildblood): Lumbridge, Seers' Village, Yanille, Entrana, Aldarin", position = 4, section = runSection)
	default boolean includeHops()
	{
		return false;
	}

	@ConfigItem(keyName = "includeCactus", name = "Cactus run", description = "Include cactus patches (cactus, potato cactus): Al Kharid, Farming Guild", position = 4, section = runSection)
	default boolean includeCactus()
	{
		return false;
	}

	@ConfigItem(keyName = "includeCalquat", name = "Calquat run", description = "Include calquat patches: Tai Bwo Wannai, Kastori", position = 4, section = runSection)
	default boolean includeCalquat()
	{
		return false;
	}

	@ConfigItem(keyName = "includeSpiritTrees", name = "Spirit tree run", description = "Include spirit tree patches: Port Sarim, Etceteria, Brimhaven, Hosidius, Farming Guild (how many you can plant depends on your Farming level)", position = 4, section = runSection)
	default boolean includeSpiritTrees()
	{
		return false;
	}

	@ConfigItem(keyName = "includeCelastrus", name = "Celastrus run", description = "Include the Farming Guild celastrus patch", position = 4, section = runSection)
	default boolean includeCelastrus()
	{
		return false;
	}

	@ConfigItem(keyName = "includeRedwood", name = "Redwood run", description = "Include the Farming Guild redwood patch", position = 4, section = runSection)
	default boolean includeRedwood()
	{
		return false;
	}

	@ConfigItem(keyName = "payGardeners", name = "Pay gardeners", description = "Bring payment so gardeners protect your trees", position = 3, section = runSection)
	default boolean payGardeners()
	{
		return true;
	}

	@ConfigItem(keyName = "useCompost", name = "Bring compost", description = "Include compost in the withdraw list", position = 4, section = runSection)
	default boolean useCompost()
	{
		return true;
	}

	@ConfigItem(keyName = "activeTimersOnly", name = "Timers: planted patches only", description = "Hide empty and weedy patches from the timer list", position = 5, section = runSection)
	default boolean activeTimersOnly()
	{
		return true;
	}

	@ConfigItem(keyName = "onlyReady", name = "Only ready patches", description = "When some patches are ready, leave the still-growing ones off the run", position = 6, section = runSection)
	default boolean onlyReady()
	{
		return true;
	}

	@ConfigItem(keyName = "profitFirst", name = "Most profitable herb first", description = "Plant the herb seed with the best GE profit per patch first, instead of the highest level", position = 7, section = runSection)
	default boolean profitFirst()
	{
		return true;
	}

	@ConfigItem(keyName = "plantContract", name = "Plant farming contract", description = "Put your Farming Guild contract crop in the guild patch", position = 8, section = runSection)
	default boolean plantContract()
	{
		return true;
	}

	@ConfigItem(keyName = "notifyDisease", name = "Notify diseased/dead", description = "Notification when a patch is found diseased or dead", position = 9, section = runSection)
	default boolean notifyDisease()
	{
		return true;
	}

	@ConfigItem(keyName = "showReadyOverlay", name = "Run-ready box", description = "Small on-screen box showing which runs are ready", position = 10, section = runSection)
	default boolean showReadyOverlay()
	{
		return true;
	}

	@ConfigItem(keyName = "showHistory", name = "Show run history", description = "Show the run history and profit section", position = 11, section = runSection)
	default boolean showHistory()
	{
		return true;
	}

	@ConfigItem(keyName = "bringRake", name = "Bring rake", description = "Include a rake for weedy patches", position = 5, section = runSection)
	default boolean bringRake()
	{
		return true;
	}

	@ConfigItem(keyName = "useBoostedLevel", name = "Use boosted level", description = "Plan with your boosted Farming level (e.g. after a garden pie)", position = 6, section = runSection)
	default boolean useBoostedLevel()
	{
		return false;
	}

	@ConfigItem(keyName = "harmonyUnlocked", name = "Morytania Elite done", description = "Tick if you've done the Morytania Elite Diary (unlocks Harmony Island)", position = 7, section = runSection)
	default boolean harmonyUnlocked()
	{
		return false;
	}

	@ConfigItem(keyName = "notifyReady", name = "Notify when ready", description = "Send a notification when a tracked patch is fully grown", position = 8, section = runSection)
	default boolean notifyReady()
	{
		return true;
	}

	@ConfigItem(keyName = "useSpells", name = "Use spells (runes)",
		description = "Use standard spellbook teleports. Used instead of tabs when both are on.",
		position = 0, section = navSection)
	default boolean useSpells()
	{
		return true;
	}

	@ConfigItem(keyName = "useTabs", name = "Use teleport tabs",
		description = "Use teleport tablets (only where you can't cast the spell, if spells are on)",
		position = 0, section = navSection)
	default boolean useTabs()
	{
		return true;
	}

	@ConfigItem(keyName = "houseLocation", name = "House location",
		description = "Where your player-owned house portal is. Teleport to House and house tabs are used for patches near it "
			+ "(e.g. Hosidius, Taverley, Brimhaven, Yanille, Rimmington, Aldarin). A Construction cape works at every portal.",
		position = 1, section = navSection)
	default HouseLocation houseLocation()
	{
		return HouseLocation.NONE;
	}

	@ConfigItem(keyName = "chainFairyRings", name = "Chain fairy rings",
		description = "Once you're at a fairy ring, do every fairy-ring patch back to back (e.g. cloak to Ardougne, then BLR > ALQ > AKR > AJP)",
		position = 1, section = navSection)
	default boolean chainFairyRings()
	{
		return true;
	}

	@ConfigItem(keyName = "staffless", name = "Lumbridge Elite done",
		description = "Tick if you've done the Lumbridge Elite Diary (fairy rings without a staff)",
		position = 5, section = navSection)
	default boolean staffless()
	{
		return false;
	}

	@ConfigItem(keyName = "disabledCrops", name = "", description = "", hidden = true)
	default String disabledCrops()
	{
		return "";
	}

	@ConfigItem(keyName = "disabledPatches", name = "", description = "", hidden = true)
	default String disabledPatches()
	{
		return "";
	}

	@ConfigItem(keyName = "disabledTeleports", name = "", description = "", hidden = true)
	default String disabledTeleports()
	{
		return "";
	}

	@ConfigItem(keyName = "importTimeTracking", name = "Import Time Tracking timers",
		description = "Read herb timers RuneLite's built-in Time Tracking plugin has already saved",
		position = 9, section = runSection)
	default boolean importTimeTracking()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightGardener", name = "Highlight gardener to pay", description = "Outline the gardener when the next step is paying them", position = 2, section = navSection)
	default boolean highlightGardener()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightPatch", name = "Highlight next patch", description = "Outline the next patch on the ground", position = 1, section = navSection)
	default boolean highlightPatch()
	{
		return true;
	}

	@ConfigItem(keyName = "gameHintArrow", name = "Game hint arrow",
		description = "Use the game's own yellow hint arrow (also shown on the minimap) on the next patch, fairy ring, spirit tree or gardener to pay",
		position = 2, section = navSection)
	default boolean gameHintArrow()
	{
		return true;
	}

	@ConfigItem(keyName = "pointerArrow", name = "Arrow over target", description = "A steady arrow over the next patch, fairy ring, spirit tree or gardener (drawn by this plugin, not the game's blinking hint arrow)", position = 2, section = navSection)
	default boolean hintArrow()
	{
		return true;
	}

	@ConfigItem(keyName = "shortestPathLine", name = "Shortest Path walking line",
		description = "Send each stop to the Shortest Path plugin (if installed): it plans the whole trip with your own Shortest Path settings and draws the walking line.",
		position = 3, section = navSection)
	default boolean useShortestPath()
	{
		return true;
	}

	@ConfigItem(keyName = "pathMinigameTeleports", name = "Path: minigame teleports",
		description = "Let the Shortest Path line use minigame teleports (e.g. Rat Pits). Off keeps the line closer to this plugin's plan.",
		position = 4, section = navSection)
	default boolean pathMinigameTeleports()
	{
		return false;
	}

	@ConfigItem(keyName = "showNowNext", name = "Now / next box",
		description = "A box at the top of the screen with what to do right now and the stop after that",
		position = 6, section = navSection)
	default boolean showNowNext()
	{
		return true;
	}

	@ConfigItem(keyName = "bankSupplyTab", name = "Supply tab in bank",
		description = "A Farm run tab on the bank's title bar; click it to show just the run's items (with how many to take, noted or unnoted)",
		position = 7, section = navSection)
	default boolean bankSupplyTab()
	{
		return true;
	}

	@ConfigItem(keyName = "bankTabGlow", name = "Animated bank button",
		description = "A glow, a ring of leaves and fireflies around the Farm run bank button while it's closed (animation by Romte1)",
		position = 8, section = navSection)
	default boolean bankTabGlow()
	{
		return true;
	}

	@ConfigItem(keyName = "showSocials", name = "Channel links in panel",
		description = "Show buttons to ToadFx's YouTube, Twitch, Kick and TikTok at the bottom of the panel",
		position = 5, section = navSection)
	default boolean showSocials()
	{
		return true;
	}

	@ConfigItem(keyName = "patchColor", name = "Patch colour", description = "Colour of the next-patch highlight", position = 4, section = navSection)
	default Color patchColor()
	{
		return new Color(0, 200, 255);
	}

	@ConfigItem(keyName = "highlightInventory", name = "Highlight step items",
		description = "Outline the inventory item your current step needs (seed, then compost, then payment)",
		position = 6, section = navSection)
	default boolean highlightInventory()
	{
		return true;
	}

	@ConfigItem(keyName = "stepColor", name = "Step item colour", description = "Colour of the inventory step highlight",
		position = 7, section = navSection)
	default Color stepColor()
	{
		return new Color(255, 200, 60);
	}

	@ConfigItem(keyName = "showTravel", name = "Show Travel section", description = "Show the Travel section in the panel", hidden = true)
	default boolean showTravel()
	{
		return true;
	}

	@ConfigItem(keyName = "showTimers", name = "Show patch timers", description = "Show the patch timer list in the panel", hidden = true)
	default boolean showTimers()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightBank", name = "Highlight in bank", description = "Outline the items to withdraw when your bank is open", position = 1, section = bankSection)
	default boolean highlightBank()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightColor", name = "Highlight colour", description = "Colour of the bank highlight", position = 2, section = bankSection)
	default Color highlightColor()
	{
		return new Color(80, 220, 80);
	}
}
