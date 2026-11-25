package com.herbrunhelper;

import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WearablesTest
{
	@Test
	public void staffBeatsMagicSecateurs()
	{
		RunPlan plan = new RunPlan();
		plan.withdrawals.add(new RunPlan.Withdrawal("Magic secateurs", 1,
			"+10% crop yield; wield it", 1));
		plan.withdrawals.add(new RunPlan.Withdrawal("Dust battlestaff", 1, "wield it", 1));

		Set<String> principals = Wearables.choosePrincipals(
			Set.of("Magic secateurs", "Dust battlestaff"), plan);

		assertTrue(principals.contains("Dust battlestaff"));
		assertFalse("secateurs still work from the inventory",
			principals.contains("Magic secateurs"));
	}

	@Test
	public void magicSecateursAreWornWhenNoStaffIsNeeded()
	{
		RunPlan plan = new RunPlan();
		plan.withdrawals.add(new RunPlan.Withdrawal("Magic secateurs", 1,
			"+10% crop yield; wield it", 1));

		Set<String> principals = Wearables.choosePrincipals(Set.of("Magic secateurs"), plan);

		assertEquals(Set.of("Magic secateurs"), principals);
	}

	@Test
	public void onlyOneItemPerSlotIsChosen()
	{
		RunPlan plan = new RunPlan();
		plan.withdrawals.add(new RunPlan.Withdrawal("Skills necklace(6)", 1, "teleport", 1));
		plan.withdrawals.add(new RunPlan.Withdrawal("Xeric's talisman", 1, "teleport", 1));
		plan.withdrawals.add(new RunPlan.Withdrawal("Farming cape", 1, "teleport", 1));
		plan.withdrawals.add(new RunPlan.Withdrawal("Explorer's ring 4", 1, "teleport", 1));

		Set<String> principals = Wearables.choosePrincipals(
			Set.of("Skills necklace(6)", "Xeric's talisman", "Farming cape", "Explorer's ring 4"),
			plan);

		assertEquals("one neck, one cape and one ring", 3, principals.size());
		assertTrue(principals.contains("Farming cape"));
		assertTrue(principals.contains("Explorer's ring 4"));
	}

	@Test
	public void unknownWearablesAreNotChosen()
	{
		RunPlan plan = new RunPlan();
		plan.withdrawals.add(new RunPlan.Withdrawal("Mystery item", 1, "", 1));

		assertTrue(Wearables.choosePrincipals(Set.of("Mystery item"), plan).isEmpty());
	}
}
