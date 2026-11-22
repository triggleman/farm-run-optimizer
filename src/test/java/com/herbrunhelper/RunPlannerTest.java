package com.herbrunhelper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RunPlannerTest
{
	private static RunPlan.RouteStep step(RunPlan plan, FarmPatch patch)
	{
		for (RunPlan.RouteStep s : plan.route)
		{
			if (s.patch == patch)
			{
				return s;
			}
		}
		return null;
	}

	private static List<String> withdrawalNames(RunPlan plan)
	{
		List<String> names = new ArrayList<>();
		for (RunPlan.Withdrawal w : plan.withdrawals)
		{
			names.add(w.itemName);
		}
		return names;
	}

	@Test
	public void growingPatchIsNotGivenANewSeed()
	{
		Set<FarmPatch> unlocked = EnumSet.of(FarmPatch.CATHERBY_HERB, FarmPatch.FARMING_GUILD_HERB);
		Map<String, Integer> owned = new HashMap<>();
		owned.put("Ranarr seed", 10);

		RunPlanner.Options options = new RunPlanner.Options();
		options.runs.clear();
		options.runs.add(PatchType.HERB);
		options.bankKnown = true;
		options.onlyReady = false;
		options.usedPatches.add(FarmPatch.FARMING_GUILD_HERB);
		options.notReady.put(FarmPatch.FARMING_GUILD_HERB, 3_600_000L);

		RunPlan plan = RunPlanner.plan(99, unlocked, owned, options);

		RunPlan.RouteStep guild = step(plan, FarmPatch.FARMING_GUILD_HERB);
		assertNotNull("the growing patch is still visited for its timer", guild);
		assertNull("a growing patch must not be replanted", guild.crop);

		RunPlan.RouteStep catherby = step(plan, FarmPatch.CATHERBY_HERB);
		assertNotNull(catherby);
		assertEquals(Crop.RANARR, catherby.crop);
	}

	@Test
	public void deadSpiritTreeDoesNotUseTheLivingLimit()
	{
		Set<FarmPatch> unlocked = EnumSet.of(FarmPatch.PORT_SARIM_SPIRIT,
			FarmPatch.BRIMHAVEN_SPIRIT, FarmPatch.HOSIDIUS_SPIRIT);
		Map<String, Integer> owned = new HashMap<>();
		owned.put("Spirit sapling", 1);

		RunPlanner.Options options = new RunPlanner.Options();
		options.runs.clear();
		options.runs.add(PatchType.SPIRIT_TREE);
		options.bankKnown = true;
		options.onlyReady = false;
		// Two live trees (the maximum at level 88), plus one dead one that must be replanted.
		options.livingPatches.add(FarmPatch.PORT_SARIM_SPIRIT);
		options.livingPatches.add(FarmPatch.BRIMHAVEN_SPIRIT);
		options.usedPatches.add(FarmPatch.PORT_SARIM_SPIRIT);
		options.usedPatches.add(FarmPatch.BRIMHAVEN_SPIRIT);
		options.usedPatches.add(FarmPatch.HOSIDIUS_SPIRIT);
		options.growingCrops.put(FarmPatch.HOSIDIUS_SPIRIT, Crop.SPIRIT_TREE);

		RunPlan plan = RunPlanner.plan(93, unlocked, owned, options);

		RunPlan.RouteStep hosidius = step(plan, FarmPatch.HOSIDIUS_SPIRIT);
		assertNotNull(hosidius);
		assertEquals("a dead tree is cleared and replaced", Crop.SPIRIT_TREE, hosidius.crop);
	}

	@Test
	public void legSlotsIgnoreDoneStops()
	{
		RunPlan.RouteStep done = new RunPlan.RouteStep(FarmPatch.VARROCK_TREE, null, null, null, false);
		done.crop = Crop.OAK;
		done.done = true;
		done.compost = "Ultracompost";
		done.paymentItem = "Tomatoes(5)";
		done.paymentQuantity = 1;
		done.coins = 200;

		List<RunPlan.RouteStep> leg = new ArrayList<>();
		leg.add(done);

		assertEquals(0, RunPlanner.legSlots(leg));
	}

	@Test
	public void fixedSlotsUseThePlannedSlotCount()
	{
		List<RunPlan.Withdrawal> fixed = new ArrayList<>();
		fixed.add(new RunPlan.Withdrawal("Smoke battlestaff", 1, "wield it", 0));
		fixed.add(new RunPlan.Withdrawal("Rune pouch", 1, "one slot", 1));
		fixed.add(new RunPlan.Withdrawal("Law rune", 2, "in the pouch", 0));

		assertEquals(1, RunPlanner.fixedSlots(fixed));
	}

	@Test
	public void hopsOnlyRunBringsASeedDibber()
	{
		Set<FarmPatch> unlocked = EnumSet.of(FarmPatch.LUMBRIDGE_HOPS);
		Map<String, Integer> owned = new HashMap<>();
		owned.put("Barley seed", 4);
		owned.put("Spade", 1);
		owned.put("Rake", 1);

		RunPlanner.Options options = new RunPlanner.Options();
		options.runs.clear();
		options.runs.add(PatchType.HOPS);
		options.bankKnown = true;

		RunPlan plan = RunPlanner.plan(99, unlocked, owned, options);

		assertTrue(withdrawalNames(plan).contains("Seed dibber"));
	}

	@Test
	public void nearestOrderStartsAtTheClosestPatch()
	{
		List<RunPlan.RouteStep> steps = new ArrayList<>();
		steps.add(new RunPlan.RouteStep(FarmPatch.FALADOR_HERB, null, null, null, false));
		steps.add(new RunPlan.RouteStep(FarmPatch.CATHERBY_HERB, null, null, null, false));

		RunPlanner.Options options = new RunPlanner.Options();
		options.playerX = 2800;
		options.playerY = 3460;

		List<RunPlan.RouteStep> ordered = RunPlanner.nearestOrder(steps, options);

		assertEquals(FarmPatch.CATHERBY_HERB, ordered.get(0).patch);
	}

	@Test
	public void customOrderFollowsTheSavedList()
	{
		List<RunPlan.RouteStep> steps = new ArrayList<>();
		steps.add(new RunPlan.RouteStep(FarmPatch.CATHERBY_HERB, null, null, null, false));
		steps.add(new RunPlan.RouteStep(FarmPatch.FALADOR_HERB, null, null, null, false));

		RunPlanner.Options options = new RunPlanner.Options();
		options.customOrder.add(FarmPatch.FALADOR_HERB);
		options.customOrder.add(FarmPatch.CATHERBY_HERB);

		List<RunPlan.RouteStep> ordered = RunPlanner.customOrder(steps, options);

		assertEquals(FarmPatch.FALADOR_HERB, ordered.get(0).patch);
		assertEquals(FarmPatch.CATHERBY_HERB, ordered.get(1).patch);
	}

	@Test
	public void legSlotsReserveRoomForProduce()
	{
		RunPlan.RouteStep a = new RunPlan.RouteStep(FarmPatch.CATHERBY_HERB, null, null, null, false);
		a.crop = Crop.RANARR;
		RunPlan.RouteStep b = new RunPlan.RouteStep(FarmPatch.FALADOR_HERB, null, null, null, false);
		b.crop = Crop.RANARR;
		List<RunPlan.RouteStep> same = new ArrayList<>();
		same.add(a);
		same.add(b);
		// Ranarr seeds stack in one slot + one slot for the grimy herbs.
		assertEquals(2, RunPlanner.legSlots(same));

		b.crop = Crop.TORSTOL;
		// Two seed types and two produce types.
		assertEquals(4, RunPlanner.legSlots(same));
	}

	@Test
	public void shortestPathGetsTheTilesAroundAPatch()
	{
		java.util.Set<net.runelite.api.coords.WorldPoint> ring =
			HerbRunPlugin.tilesAround(new net.runelite.api.coords.WorldPoint(3200, 3200, 0), 3);
		assertEquals(16, ring.size());
		assertTrue(ring.contains(new net.runelite.api.coords.WorldPoint(3198, 3200, 0)));
	}

	@Test
	public void spiritTreeLimitMatchesTheWiki()
	{
		assertEquals(0, RunPlanner.spiritTreeLimit(82));
		assertEquals(1, RunPlanner.spiritTreeLimit(83));
		assertEquals(2, RunPlanner.spiritTreeLimit(88));
		assertEquals(3, RunPlanner.spiritTreeLimit(93));
		assertEquals(4, RunPlanner.spiritTreeLimit(96));
		assertTrue(RunPlanner.spiritTreeLimit(99) > 4);
	}
}
