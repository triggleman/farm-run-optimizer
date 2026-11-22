package com.herbrunhelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import net.runelite.client.util.Text;

/**
 * Recognises the inventory item behind a Shortest Path transport's display text, so the run's
 * bring list can include the teleport items Shortest Path is going to use (e.g. a Salve
 * graveyard teleport) from the start.
 *
 * <p>Matching is case-sensitive on purpose for spells: tablets and jewellery are lower-case
 * ("Varrock teleport"), while spells are title-case ("Varrock Teleport") and need runes, which
 * the planner already handles. Shortest Path labels tablets as "X tablet", so that form is
 * matched case-insensitively after turning "tablet" into "teleport".
 */
final class TravelItems
{
	private static final String[] KNOWN =
	{
		"Salve graveyard teleport",
		"Teleport to house",
		"Kharyrll teleport",
		"Fenkenstrain's castle teleport",
		"Harmony island teleport",
		"Trollheim teleport",
		"Catherby teleport",
		"Kourend castle teleport",
		"Civitas illa Fortis teleport",
		"Ardougne teleport",
		"Varrock teleport",
		"Lumbridge teleport",
		"Falador teleport",
		"Camelot teleport",
		"Brimhaven teleport",
		"Watchtower teleport",
		"Grand Exchange teleport",
		"Skills necklace",
		"Amulet of glory",
		"Ring of dueling",
		"Games necklace",
		"Combat bracelet",
		"Ring of wealth",
		"Digsite pendant",
		"Xeric's talisman",
		"Pendant of ates",
		"Teleport crystal",
		"Quetzal whistle",
		"Royal seed pod",
		"Ectophial",
		"Stony basalt",
		"Icy basalt",
		"Ardougne cloak",
		"Explorer's ring",
		"Farming cape",
		"Fishing cape",
		"Hunter cape",
		"Crafting cape",
		"Max cape",
		"Construct. cape",
		"Achievement diary cape",
		"Rada's blessing",
		"Giantsoul amulet",
		"Camulet",
		"Desert amulet",
		"Morytania legs",
		"Karamja gloves",
		"Wilderness sword",
		"Kharedst's memoirs",
		"Book of the dead",
		"Drakan's medallion",
		"Fremennik sea boots",
		"Kandarin headgear",
		"Western banner",
		"Ring of returning",
		"Burning amulet",
		"Necklace of passage",
		"Ring of the elements",
		"Ring of shadows",
		"Slayer ring",
		"Sailors' amulet",
		"Enchanted lyre",
		"Pharaoh's sceptre",
		"Chronicle",
	};

	/** Display names that do not contain the in-game item name. */
	private static final String[][] ALIASES =
	{
		{"construction cape", "Construct. cape"},
	};

	private TravelItems()
	{
	}

	/** The item name used by a transport, preferring an exact owned variant, or null. */
	static String find(String display, Collection<String> owned)
	{
		if (display == null)
		{
			return null;
		}
		String text = Text.removeTags(display);
		String lower = text.toLowerCase(Locale.ROOT);
		// Shortest Path labels tablets as "X tablet", while the item is named "X teleport".
		// Only relax the case for tablets so a spell ("Varrock Teleport") is not mistaken for one.
		boolean tablet = lower.contains("tablet");
		String match = tablet ? text.replaceAll("(?i)\\btablet\\b", "teleport") : text;
		String best = bestMatch(match, owned, tablet);
		if (best == null)
		{
			best = bestMatch(match, Arrays.asList(KNOWN), tablet);
		}
		if (best != null)
		{
			String variant = bestOwnedVariant(best, owned);
			return variant != null ? variant : best;
		}
		for (String[] alias : ALIASES)
		{
			if (lower.contains(alias[0]))
			{
				String variant = bestOwnedVariant(alias[1], owned);
				return variant != null ? variant : alias[1];
			}
		}
		return null;
	}

	/**
	 * Resolves a wanted item to what the player has right now: a carried charged variant first,
	 * then the best variant in the bank, and the base name only when no variant exists. This is
	 * what keeps a ring of wealth usable as its charges count down (5 → 4 → 3).
	 */
	static String resolve(String wanted, Collection<String> owned, Collection<String> carried)
	{
		if (wanted == null)
		{
			return null;
		}
		String base = baseOf(wanted);
		String best = bestOwnedVariant(base, carried);
		if (best == null)
		{
			best = bestOwnedVariant(base, owned);
		}
		return best != null ? best : base;
	}

	/** "Ring of wealth (4)" -> "Ring of wealth"; "Ardougne cloak 4" -> "Ardougne cloak". */
	static String baseOf(String name)
	{
		if (name == null)
		{
			return null;
		}
		String text = name.trim();
		int open = text.lastIndexOf('(');
		if (open > 0 && text.endsWith(")"))
		{
			return text.substring(0, open).trim();
		}
		int space = text.lastIndexOf(' ');
		if (space > 0 && parseNumber(text.substring(space + 1)) >= 0)
		{
			return text.substring(0, space).trim();
		}
		return text;
	}

	/**
	 * The owned charged/tiered variant of a base item, preferring the highest number:
	 * "Ring of wealth (5)" over "Ring of wealth (2)", or "Ardougne cloak 4" over "Ardougne cloak 3".
	 */
	static String bestOwnedVariant(String base, Collection<String> owned)
	{
		String best = null;
		String fallback = null;
		int bestNumber = -1;
		for (String name : owned)
		{
			if (name.length() <= base.length() || !name.startsWith(base))
			{
				continue;
			}
			String rest = name.substring(base.length()).trim();
			if (rest.startsWith("(") && rest.endsWith(")") && rest.length() >= 2)
			{
				rest = rest.substring(1, rest.length() - 1).trim();
			}
			else if (!Character.isDigit(rest.isEmpty() ? 0 : rest.charAt(0)))
			{
				continue;
			}
			int number = parseNumber(rest);
			if (number < 0)
			{
				if (fallback == null)
				{
					fallback = name; // e.g. an imbued/no-charge variant
				}
				continue;
			}
			if (number > bestNumber)
			{
				bestNumber = number;
				best = name;
			}
		}
		return best != null ? best : fallback;
	}

	private static int parseNumber(String text)
	{
		if (text.isEmpty())
		{
			return -1;
		}
		int value = 0;
		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if (c < '0' || c > '9')
			{
				return -1;
			}
			value = value * 10 + (c - '0');
		}
		return value;
	}

	private static String bestMatch(String text, Collection<String> names, boolean ignoreCase)
	{
		String lower = ignoreCase ? text.toLowerCase(Locale.ROOT) : null;
		String best = null;
		for (String name : names)
		{
			boolean contains = ignoreCase
				? lower.contains(name.toLowerCase(Locale.ROOT))
				: text.contains(name);
			if (contains && (best == null || name.length() > best.length()))
			{
				best = name;
			}
		}
		return best;
	}
}
