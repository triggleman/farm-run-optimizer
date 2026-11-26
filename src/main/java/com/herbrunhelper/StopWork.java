package com.herbrunhelper;

import java.util.ArrayList;
import java.util.List;

/** The to-do list for one stop, from what the plugin knows about the patch. Pure Java. */
public final class StopWork
{
	private StopWork()
	{
	}

	public static List<String> describe(RunPlan.RouteStep step, PatchTimers.Planting planting,
		PatchTimers.Status status, long now)
	{
		List<String> out = new ArrayList<>();
		PatchType type = step.patch.getType();
		boolean ready = planting != null && planting.isReady(now);
		String seed = step.crop == null ? null : step.crop.getSeedName().toLowerCase();

		if (!type.isTreeLike())
		{
			if (ready)
			{
				out.add("Harvest the " + planting.crop.getDisplayName().toLowerCase()
					+ (type == PatchType.FLOWER ? "" : " (note them at the leprechaun)"));
			}
			else if (status == PatchTimers.Status.DISEASED)
			{
				out.add("Diseased: cure with plant cure, or dig it up");
			}
			else if (status == PatchTimers.Status.DEAD)
			{
				out.add("Dead: dig it up with your spade");
			}
			else if (status == PatchTimers.Status.WEEDS)
			{
				out.add("Rake the weeds");
			}
			else if (planting == null && status == null)
			{
				out.add("Harvest if anything's grown");
			}
		}
		else
		{
			String tree = plantName(type, planting != null ? planting.crop : null);
			if (ready)
			{
				out.add("Check the " + tree + "'s health");
				String pick = pickLine(type, planting.crop);
				if (pick != null)
				{
					out.add(pick);
				}
				if (seed != null)
				{
					out.add(clearLine(type));
				}
				else
				{
					out.add("Leave it standing: nothing to replant (it won't die)");
				}
			}
			else if (status == PatchTimers.Status.STUMP)
			{
				out.add("Dig up the stump with your spade");
			}
			else if (status == PatchTimers.Status.DISEASED)
			{
				out.add("Diseased: prune it with secateurs");
			}
			else if (status == PatchTimers.Status.DEAD)
			{
				out.add("Dead: dig it up with your spade");
			}
			else if (status == PatchTimers.Status.WEEDS)
			{
				out.add("Rake the weeds");
			}
			else if (planting == null && status == null)
			{
				out.add(seed != null ? "If a grown " + tree + " is there: check its health, then "
					+ clearLine(type).substring(0, 1).toLowerCase() + clearLine(type).substring(1)
					: "If a grown " + tree + " is there: check its health");
			}
		}

		if (seed != null)
		{
			if (step.crop.getSeedsPerPatch() > 1)
			{
				out.add("Plant " + step.crop.getSeedsPerPatch() + " " + seed + "s");
			}
			else
			{
				String article = type != PatchType.HERB ? "the " : "aeiou".indexOf(seed.charAt(0)) >= 0 ? "an " : "a ";
				out.add("Plant " + article + seed);
			}
		}
		else if (!type.isTreeLike())
		{
			out.add("No seed left to plant here");
		}
		if (step.compost != null && seed != null)
		{
			out.add(step.compost.startsWith("Bottomless") ? "Use your bottomless compost bucket" : "Add " + step.compost.toLowerCase());
		}
		if (step.paymentItem != null && seed != null)
		{
			StringBuilder pay = new StringBuilder("Pay the gardener " + step.paymentQuantity + " x " + step.paymentItem);
			String[][] extra = step.crop.getExtraPayments();
			for (int i = 0; i < extra.length; i++)
			{
				pay.append(i == extra.length - 1 ? " and " : ", ").append(extra[i][1]).append(" x ").append(extra[i][0]);
			}
			out.add(pay + " to protect it");
		}
		return out;
	}

	/** "maple tree", "redberry bush", "potato cactus", "spirit tree"... */
	static String plantName(PatchType type, Crop crop)
	{
		String name = crop == null ? null : crop.getDisplayName().toLowerCase();
		switch (type)
		{
			case BUSH:
				return name == null ? "bush" : name + " bush";
			case CACTUS:
				return name == null ? "cactus" : name;
			case SPIRIT_TREE:
				return "spirit tree";
			default:
				return name == null ? "tree" : name + " tree";
		}
	}

	/** What there is to take from a grown plant before clearing it, if anything. */
	static String pickLine(PatchType type, Crop crop)
	{
		switch (type)
		{
			case FRUIT_TREE:
				return "Pick the fruit if you want it (note at the leprechaun)";
			case BUSH:
				return "Pick the berries (note at the leprechaun)";
			case CACTUS:
				return crop == Crop.POTATO_CACTUS ? "Pick the potato cacti" : "Pick the cactus spines";
			case CALQUAT:
				return "Pick the calquat fruit if you want it";
			case CELASTRUS:
				return "Harvest the bark (it doesn't grow back)";
			default:
				return null;
		}
	}

	/** How to get rid of the grown plant so you can replant. */
	static String clearLine(PatchType type)
	{
		switch (type)
		{
			case BUSH:
				return "Dig up the bush with your spade";
			case CACTUS:
				return "Dig up the cactus with your spade";
			case SPIRIT_TREE:
				return "Dig it up with your spade (it can't be cut down)";
			case CELASTRUS:
			case REDWOOD:
				return "Chop it down, then dig up the stump with your spade";
			default:
				return "Pay the gardener " + RunPlanner.TREE_REMOVAL_COINS + " coins to chop it down";
		}
	}

	/**
	 * Is this to-do line the step the plugin is pointing you at right now (the action shown at
	 * the top of the panel and highlighted in game)?
	 */
	public static boolean isCurrent(String line, String action)
	{
		if (line == null || action == null)
		{
			return false;
		}
		String l = line.toLowerCase();
		String a = action.toLowerCase();
		if (a.startsWith("rake"))
		{
			return l.startsWith("rake");
		}
		if (a.startsWith("dig"))
		{
			return l.contains("dig");
		}
		if (a.startsWith("check the"))
		{
			return l.startsWith("check the") || l.startsWith("if a grown");
		}
		if (a.startsWith("pay the gardener to clear"))
		{
			return l.contains("chop it down") || l.contains("to clear it");
		}
		if (a.startsWith("pay the gardener"))
		{
			return l.contains("to protect it");
		}
		if (a.startsWith("pick") || a.startsWith("harvest"))
		{
			return l.startsWith(a.split(" ")[0]);
		}
		if (a.startsWith("chop it down"))
		{
			return l.startsWith("chop it down");
		}
		if (a.startsWith("plant"))
		{
			return l.startsWith("plant");
		}
		if (a.startsWith("add compost"))
		{
			return l.startsWith("add ") || l.startsWith("use your bottomless");
		}
		return false;
	}
}
