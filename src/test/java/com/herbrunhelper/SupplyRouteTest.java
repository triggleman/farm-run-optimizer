package com.herbrunhelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Full-route supply tests for the same policy as Shortest Path's "Inventory and Bank"
 * teleport setting: every owned item in the bank is available, but only inventory and
 * equipment are carried, so bank-only teleports and runes must appear on the bring list.
 */
public class SupplyRouteTest
{
	private static RunPlanner.Options options(PatchType... runs)
	{
		RunPlanner.Options options = new RunPlanner.Options();
		options.runs.clear();
		options.runs.addAll(Arrays.asList(runs));
		options.bankKnown = true;
		options.onlyReady = false;
		options.inventorySlots = 28;
		options.teleports.magicLevel = 99;
		options.teleports.standardSpellbook = true;
		options.teleports.fairyRingsUnlocked = true;
		options.teleports.staffless = true;
		options.teleports.useSpells = true;
		options.teleports.useTabs = true;
		options.teleports.chainFairyRings = true;
		options.teleports.completedQuests = new HashSet<>(Arrays.asList(
			"Plague City", "Eadgar's Ruse", "Client of Kourend", "Twilight's Promise"));
		return options;
	}

	private static Map<String, Integer> bank()
	{
		Map<String, Integer> items = new LinkedHashMap<>();
		items.put("Magic sapling", 5);
		items.put("Papaya sapling", 4);
		items.put("Ranarr seed", 20);
		items.put("Coconut", 25);
		items.put("Pineapple", 10);
		items.put("Ultracompost", 10);
		items.put("Spade", 1);
		items.put("Rake", 1);
		items.put("Seed dibber", 1);
		items.put("Magic secateurs", 1);
		items.put("Dramen staff", 1);
		items.put("Smoke battlestaff", 1);
		items.put("Rune pouch", 1);
		items.put("Law rune", 100);
		items.put("Air rune", 100);
		items.put("Water rune", 100);
		items.put("Earth rune", 100);
		items.put("Fire rune", 100);
		items.put("Coins", 1000);

		// Teleport items. Some are deliberately bank-only so the test proves the planner
		// withdraws them instead of assuming they are in the inventory.
		items.put("Skills necklace(6)", 1);
		items.put("Ring of wealth (5)", 1);
		items.put("Ring of wealth (2)", 1);
		items.put("Amulet of glory(6)", 1);
		items.put("Ardougne cloak 4", 1);
		items.put("Explorer's ring 4", 1);
		items.put("Farming cape", 1);
		items.put("Ectophial", 1);
		items.put("Xeric's talisman", 1);
		items.put("Kharedst's memoirs", 1);
		items.put("Digsite pendant", 1);
		items.put("Teleport crystal", 1);
		items.put("Royal seed pod", 1);
		items.put("Quetzal whistle", 1);
		items.put("Pendant of ates", 1);
		items.put("Stony basalt", 1);
		items.put("Icy basalt", 1);
		items.put("Varrock teleport", 5);
		items.put("Lumbridge teleport", 5);
		items.put("Falador teleport", 5);
		items.put("Camelot teleport", 5);
		items.put("Ardougne teleport", 5);
		items.put("Trollheim teleport", 5);
		items.put("Brimhaven teleport", 5);
		items.put("Fenkenstrain's castle teleport", 5);
		items.put("Harmony island teleport", 5);
		items.put("Kourend castle teleport", 5);
		items.put("Civitas illa fortis teleport", 5);
		items.put("Salve graveyard teleport", 5);
		items.put("Teleport to house", 5);
		return items;
	}

	private static Map<String, Integer> carried()
	{
		Map<String, Integer> items = new LinkedHashMap<>();
		items.put("Law rune", 2);
		items.put("Air rune", 6);
		items.put("Fire rune", 1);
		items.put("Rune pouch", 1);
		items.put("Dramen staff", 1);
		items.put("Magic secateurs", 1);
		items.put("Spade", 1);
		items.put("Rake", 1);
		items.put("Seed dibber", 1);
		items.put("Ultracompost", 2);
		items.put("Coconut", 2);
		items.put("Pineapple", 2);
		items.put("Magic sapling", 1);
		items.put("Papaya sapling", 1);
		items.put("Ranarr seed", 1);
		return items;
	}

	private static Map<String, Integer> owned(Map<String, Integer> bank, Map<String, Integer> carried)
	{
		Map<String, Integer> owned = new HashMap<>(bank);
		carried.forEach((name, quantity) -> owned.merge(name, quantity, Integer::sum));
		return owned;
	}

	private static List<RunPlan.Withdrawal> allWithdrawals(RunPlan plan)
	{
		List<RunPlan.Withdrawal> all = new ArrayList<>(plan.withdrawals);
		for (RunPlan.BankStop stop : plan.bankStops)
		{
			all.addAll(stop.withdrawals);
		}
		return all;
	}

	private static boolean hasWithdrawal(RunPlan plan, String name)
	{
		for (RunPlan.Withdrawal w : allWithdrawals(plan))
		{
			if (w.itemName.equals(name) && w.toWithdraw > 0)
			{
				return true;
			}
		}
		return false;
	}

	private static boolean covered(RunPlan plan, RunPlanner.Options options, String item)
	{
		if (item == null)
		{
			return true;
		}
		String base = TravelItems.baseOf(item);
		for (String carried : options.carried.keySet())
		{
			if (base.equals(TravelItems.baseOf(carried)))
			{
				return true;
			}
		}
		for (RunPlan.Withdrawal w : allWithdrawals(plan))
		{
			if (w.toWithdraw > 0 && base.equals(TravelItems.baseOf(w.itemName)))
			{
				return true;
			}
		}
		return false;
	}

	private static void assertEveryTeleportCovered(RunPlan plan, RunPlanner.Options options)
	{
		for (RunPlan.RouteStep step : plan.route)
		{
			if (step.teleportItem != null)
			{
				assertTrue("missing teleport item " + step.teleportItem + " for " + step.patch,
					covered(plan, options, step.teleportItem));
			}
		}
	}

	private static boolean hasRuneShortage(RunPlan plan)
	{
		for (String warning : plan.warnings)
		{
			if (warning.startsWith("Short ") && warning.contains("rune"))
			{
				return true;
			}
		}
		return false;
	}

	@Test
	public void wearableTeleportTakesNoInventorySlot()
	{
		Set<FarmPatch> unlocked = EnumSet.of(FarmPatch.FARMING_GUILD_TREE);
		Map<String, Integer> carried = carried();
		RunPlanner.Options options = options(PatchType.TREE);
		options.carried.putAll(carried);
		options.wearable.add("Skills necklace(6)");
		options.teleports.disabled = EnumSet.allOf(Teleport.class);
		options.teleports.disabled.remove(Teleport.SKILLS_NECKLACE);

		RunPlan plan = RunPlanner.plan(99, unlocked, owned(bank(), carried), options);

		RunPlan.Withdrawal necklace = allWithdrawals(plan).stream()
			.filter(w -> w.itemName.equals("Skills necklace(6)")).findFirst().orElse(null);
		assertTrue("the necklace is used for the Farming Guild tree", necklace != null);
		assertEquals("a worn necklace takes no inventory slot", 0, necklace.slots);
	}

	@Test
	public void treeRunUsesBankAndInventoryTeleports()
	{
		Set<FarmPatch> unlocked = EnumSet.of(FarmPatch.VARROCK_TREE, FarmPatch.LUMBRIDGE_TREE,
			FarmPatch.FALADOR_TREE, FarmPatch.TAVERLEY_TREE, FarmPatch.GNOME_TREE);
		Map<String, Integer> carried = carried();
		RunPlanner.Options options = options(PatchType.TREE);
		options.carried.putAll(carried);
		options.equipped.put("Dramen staff", 1);
		options.equipped.put("Magic secateurs", 1);

		RunPlan plan = RunPlanner.plan(99, unlocked, owned(bank(), carried), options);

		assertFalse("the tree run should have stops", plan.route.isEmpty());
		for (RunPlan.RouteStep step : plan.route)
		{
			assertTrue("only tree patches in a tree run", step.patch.getType() == PatchType.TREE);
		}
		assertTrue("more saplings must come from the bank", hasWithdrawal(plan, "Magic sapling"));
		assertTrue("a bank-only teleport must be withdrawn", allWithdrawals(plan).stream()
			.anyMatch(w -> w.toWithdraw > 0 && w.group == RunPlan.Group.TRAVEL
				&& !options.carried.containsKey(w.itemName)));
		assertEveryTeleportCovered(plan, options);
		assertFalse("all runes are in the bank", hasRuneShortage(plan));
	}

	@Test
	public void treeAndFruitTreeRunCoversBothTypes()
	{
		Set<FarmPatch> unlocked = EnumSet.of(FarmPatch.VARROCK_TREE, FarmPatch.LUMBRIDGE_TREE,
			FarmPatch.FALADOR_TREE, FarmPatch.TAVERLEY_TREE, FarmPatch.GNOME_TREE,
			FarmPatch.GNOME_FRUIT, FarmPatch.TREE_GNOME_VILLAGE_FRUIT,
			FarmPatch.CATHERBY_FRUIT, FarmPatch.BRIMHAVEN_FRUIT);
		Map<String, Integer> carried = carried();
		RunPlanner.Options options = options(PatchType.TREE, PatchType.FRUIT_TREE);
		options.carried.putAll(carried);
		options.equipped.put("Dramen staff", 1);
		options.equipped.put("Magic secateurs", 1);

		RunPlan plan = RunPlanner.plan(99, unlocked, owned(bank(), carried), options);

		boolean tree = false;
		boolean fruit = false;
		for (RunPlan.RouteStep step : plan.route)
		{
			tree |= step.patch.getType() == PatchType.TREE;
			fruit |= step.patch.getType() == PatchType.FRUIT_TREE;
		}
		assertTrue("tree patches are planned", tree);
		assertTrue("fruit tree patches are planned", fruit);
		assertTrue("tree saplings come from the bank", hasWithdrawal(plan, "Magic sapling"));
		assertTrue("fruit saplings come from the bank", hasWithdrawal(plan, "Papaya sapling"));
		assertEveryTeleportCovered(plan, options);
		assertFalse(hasRuneShortage(plan));
	}

	@Test
	public void spellRunesComeFromTheBankWhenNoPouchIsCarried()
	{
		Set<FarmPatch> unlocked = EnumSet.of(FarmPatch.FALADOR_HERB, FarmPatch.CATHERBY_HERB,
			FarmPatch.VARROCK_TREE, FarmPatch.LUMBRIDGE_TREE, FarmPatch.GNOME_FRUIT);
		Map<String, Integer> carried = carried();
		carried.remove("Rune pouch");
		carried.remove("Dramen staff");
		RunPlanner.Options options = options(PatchType.HERB, PatchType.TREE, PatchType.FRUIT_TREE);
		options.carried.putAll(carried);
		options.equipped.put("Magic secateurs", 1);

		RunPlan plan = RunPlanner.plan(99, unlocked, owned(bank(), carried), options);

		assertTrue("at least one spell teleport is planned", plan.route.stream()
			.anyMatch(s -> s.teleport != null && s.teleport.getKind() == Teleport.Kind.SPELL));
		assertTrue("a spell rune must be withdrawn from the bank",
			hasWithdrawal(plan, "Law rune") || hasWithdrawal(plan, "Air rune")
				|| hasWithdrawal(plan, "Water rune") || hasWithdrawal(plan, "Earth rune")
				|| hasWithdrawal(plan, "Fire rune"));
		assertFalse("the bank has enough runes", hasRuneShortage(plan));
		assertEveryTeleportCovered(plan, options);
	}

	@Test
	public void herbTreeAndFruitTreeRunCoversRunesAndTeleports()
	{
		Set<FarmPatch> unlocked = EnumSet.of(FarmPatch.FALADOR_HERB, FarmPatch.CATHERBY_HERB,
			FarmPatch.ARDOUGNE_HERB, FarmPatch.HOSIDIUS_HERB,
			FarmPatch.VARROCK_TREE, FarmPatch.LUMBRIDGE_TREE, FarmPatch.FALADOR_TREE,
			FarmPatch.TAVERLEY_TREE, FarmPatch.GNOME_TREE,
			FarmPatch.GNOME_FRUIT, FarmPatch.TREE_GNOME_VILLAGE_FRUIT,
			FarmPatch.CATHERBY_FRUIT, FarmPatch.BRIMHAVEN_FRUIT);
		Map<String, Integer> carried = carried();
		carried.remove("Dramen staff");
		carried.put("Smoke battlestaff", 1);
		RunPlanner.Options options = options(PatchType.HERB, PatchType.TREE, PatchType.FRUIT_TREE);
		options.carried.putAll(carried);
		options.equipped.put("Smoke battlestaff", 1);
		options.equipped.put("Magic secateurs", 1);
		options.pouchCarried = true;
		options.pouchName = "Rune pouch";
		options.pouchCapacity = 3;
		options.pouchRunes.put("Law rune", 100);
		options.pouchRunes.put("Water rune", 100);
		options.pouchRunes.put("Earth rune", 100);

		RunPlan plan = RunPlanner.plan(99, unlocked, owned(bank(), carried), options);

		assertFalse("the mixed run should have stops", plan.route.isEmpty());
		assertTrue("at least one spell teleport is planned", plan.route.stream()
			.anyMatch(s -> s.teleport != null && s.teleport.getKind() == Teleport.Kind.SPELL));
		assertTrue("herb seeds come from the bank", hasWithdrawal(plan, "Ranarr seed"));
		assertTrue("tree saplings come from the bank", hasWithdrawal(plan, "Magic sapling"));
		assertTrue("fruit saplings come from the bank", hasWithdrawal(plan, "Papaya sapling"));
		assertEveryTeleportCovered(plan, options);
		assertFalse("the staff and pouch cover the spell runes", hasRuneShortage(plan));
		for (String rune : Arrays.asList("Law rune", "Air rune", "Water rune", "Earth rune", "Fire rune"))
		{
			assertFalse("no base " + rune + " is needed when the staff and pouch cover it",
				hasWithdrawal(plan, rune));
		}
	}

	@Test
	public void hosidiusDiaryRemovesTheGardenerPayment()
	{
		Map<String, Integer> owned = new HashMap<>();
		owned.put("Watermelon seed", 3);
		owned.put("Curry leaf", 10);

		RunPlanner.Options free = options(PatchType.ALLOTMENT);
		free.freeHosidiusGardener = true;
		RunPlan plan = RunPlanner.plan(99, EnumSet.of(FarmPatch.HOSIDIUS_ALLOTMENT_A), owned, free);
		RunPlan.RouteStep step = plan.route.stream()
			.filter(s -> s.patch == FarmPatch.HOSIDIUS_ALLOTMENT_A)
			.findFirst().orElse(null);
		assertTrue("the Hosidius allotment is planned", step != null);
		assertNull("the easy Kourend diary means no gardener payment", step.paymentItem);

		RunPlanner.Options paid = options(PatchType.ALLOTMENT);
		paid.freeHosidiusGardener = false;
		plan = RunPlanner.plan(99, EnumSet.of(FarmPatch.HOSIDIUS_ALLOTMENT_A), owned, paid);
		step = plan.route.stream()
			.filter(s -> s.patch == FarmPatch.HOSIDIUS_ALLOTMENT_A)
			.findFirst().orElse(null);
		assertTrue("without the benefit the payment is still planned", step != null);
		assertEquals("Curry leaf", step.paymentItem);
	}
}
