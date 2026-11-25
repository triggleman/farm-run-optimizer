package com.herbrunhelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.ItemComposition;

/** Detects items the player can put on, using the item's own inventory actions. */
final class Wearables
{
	enum Slot
	{
		WEAPON,
		SHIELD,
		HEAD,
		CAPE,
		NECK,
		BODY,
		LEGS,
		HANDS,
		FEET,
		RING,
		AMMO,
		UNKNOWN
	}

	private Wearables()
	{
	}

	static Slot slot(String itemName)
	{
		if (itemName == null)
		{
			return Slot.UNKNOWN;
		}
		String name = itemName.toLowerCase();
		if (name.contains("staff") || name.contains("wand") || name.contains("secateurs")
			|| name.contains("reprisal"))
		{
			return Slot.WEAPON;
		}
		if (name.contains("tome of") || name.contains("book of") || name.contains("defender")
			|| name.contains("shield"))
		{
			return Slot.SHIELD;
		}
		if (name.endsWith(" cape") || name.contains("cloak") || name.contains("ava's"))
		{
			return Slot.CAPE;
		}
		if (name.contains("necklace") || name.contains("amulet of") || name.contains("pendant")
			|| name.contains("talisman"))
		{
			return Slot.NECK;
		}
		if (name.contains("ring"))
		{
			return Slot.RING;
		}
		if (name.contains("blessing"))
		{
			return Slot.AMMO;
		}
		if (name.contains("headgear") || name.contains("helmet") || name.contains("helm")
			|| name.contains("hood") || name.contains("mask") || name.endsWith(" hat"))
		{
			return Slot.HEAD;
		}
		if (name.contains("gloves") || name.contains("gauntlets") || name.contains("vambraces"))
		{
			return Slot.HANDS;
		}
		if (name.contains("boots"))
		{
			return Slot.FEET;
		}
		if (name.contains("platebody") || name.contains("chainbody") || name.contains("robe top")
			|| name.endsWith(" body") || name.contains("graceful top"))
		{
			return Slot.BODY;
		}
		if (name.contains("platelegs") || name.contains("legs") || name.contains("skirt")
			|| name.contains("robe bottom") || name.contains("graceful legs"))
		{
			return Slot.LEGS;
		}
		return Slot.UNKNOWN;
	}

	/**
	 * One wearable per equipment slot. A staff needed for runes beats magic secateurs,
	 * whose effect still works from the inventory.
	 */
	static Set<String> choosePrincipals(Set<String> wearable, RunPlan plan)
	{
		Set<String> runItems = new HashSet<>();
		for (RunPlan.Withdrawal w : plan.withdrawals)
		{
			runItems.add(w.itemName);
		}
		for (RunPlan.BankStop stop : plan.bankStops)
		{
			for (RunPlan.Withdrawal w : stop.withdrawals)
			{
				runItems.add(w.itemName);
			}
		}
		for (RunPlan.Withdrawal w : plan.optionalItems)
		{
			runItems.add(w.itemName);
		}
		Set<String> teleportItems = new HashSet<>();
		for (RunPlan.RouteStep step : plan.route)
		{
			if (step.teleportItem != null)
			{
				teleportItems.add(step.teleportItem);
			}
		}
		Set<String> staff = new HashSet<>();
		Set<String> runeStaff = new HashSet<>();
		for (RunPlan.Withdrawal w : plan.withdrawals)
		{
			String note = w.note == null ? "" : w.note.toLowerCase();
			String name = w.itemName == null ? "" : w.itemName.toLowerCase();
			if ((note.contains("wield") || note.contains("worn"))
				&& slot(w.itemName) == Slot.WEAPON && !name.contains("secateurs"))
			{
				staff.add(w.itemName);
				if (!note.contains("fairy"))
				{
					runeStaff.add(w.itemName);
				}
			}
		}

		Map<Slot, List<String>> bySlot = new EnumMap<>(Slot.class);
		for (String name : wearable)
		{
			if (!runItems.contains(name) && !teleportItems.contains(name))
			{
				continue;
			}
			Slot slot = slot(name);
			if (slot != Slot.UNKNOWN)
			{
				bySlot.computeIfAbsent(slot, k -> new ArrayList<>()).add(name);
			}
		}

		Set<String> principals = new HashSet<>();
		for (Map.Entry<Slot, List<String>> entry : bySlot.entrySet())
		{
			List<String> names = entry.getValue();
			names.sort(String::compareTo);
			String chosen = null;
			if (entry.getKey() == Slot.WEAPON)
			{
				for (String name : names)
				{
					if (runeStaff.contains(name))
					{
						chosen = name;
						break;
					}
				}
				if (chosen == null)
				{
					for (String name : names)
					{
						if (staff.contains(name))
						{
							chosen = name;
							break;
						}
					}
				}
				if (chosen == null)
				{
					for (String name : names)
					{
						if ("Magic secateurs".equals(name))
						{
							chosen = name;
							break;
						}
					}
				}
			}
			else
			{
				for (String name : names)
				{
					if (teleportItems.contains(name))
					{
						chosen = name;
						break;
					}
				}
			}
			if (chosen == null)
			{
				chosen = names.get(0);
			}
			principals.add(chosen);
		}
		return principals;
	}

	static boolean isWearable(ItemComposition composition)
	{
		if (composition == null)
		{
			return false;
		}
		String[] actions = composition.getInventoryActions();
		if (actions == null)
		{
			return false;
		}
		for (String action : actions)
		{
			if ("Wear".equalsIgnoreCase(action) || "Wield".equalsIgnoreCase(action)
				|| "Equip".equalsIgnoreCase(action))
			{
				return true;
			}
		}
		return false;
	}
}
