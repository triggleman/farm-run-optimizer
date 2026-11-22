package com.herbrunhelper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RuneSupplyTest
{
	private static RuneSupply.Context context(Map<String, Integer> need, Map<String, Integer> owned)
	{
		RuneSupply.Context ctx = new RuneSupply.Context();
		ctx.need = need;
		ctx.owned = owned;
		ctx.carried = new LinkedHashMap<>();
		ctx.equipped = new LinkedHashMap<>();
		ctx.pouch = new LinkedHashMap<>();
		return ctx;
	}

	private static boolean has(RuneSupply.Result result, String prefix)
	{
		for (RunPlan.Withdrawal w : result.withdrawals)
		{
			if (w.itemName.toLowerCase().startsWith(prefix.toLowerCase()))
			{
				return true;
			}
		}
		return false;
	}

	@Test
	public void smokeStaffRemovesAirAndFireLines()
	{
		Map<String, Integer> need = new LinkedHashMap<>();
		need.put("Law rune", 2);
		need.put("Air rune", 6);
		need.put("Fire rune", 1);
		Map<String, Integer> owned = new LinkedHashMap<>();
		owned.put("Smoke battlestaff", 1);
		owned.put("Law rune", 2);

		RuneSupply.Context ctx = context(need, owned);
		ctx.carried = Map.of("Law rune", 2);

		RuneSupply.Result result = RuneSupply.plan(ctx);

		assertTrue("the staff is suggested", has(result, "Smoke battlestaff"));
		assertFalse("air is covered", has(result, "Air rune"));
		assertFalse("fire is covered", has(result, "Fire rune"));
		assertFalse("law is already carried", has(result, "Law rune"));
	}

	@Test
	public void runePouchCountsItsRunes()
	{
		RuneSupply.Context ctx = context(Map.of("Law rune", 2), Map.of("Rune pouch", 1));
		ctx.carried = Map.of("Rune pouch", 1);
		ctx.pouch = Map.of("Law rune", 2);
		ctx.pouchCarried = true;

		RuneSupply.Result result = RuneSupply.plan(ctx);

		assertTrue("nothing left to take", result.withdrawals.isEmpty());
		assertTrue(result.notes.toString().contains("Rune pouch"));
	}

	@Test
	public void runePouchIsOneSlotForSeveralTypes()
	{
		RuneSupply.Context ctx = context(Map.of("Law rune", 2, "Fire rune", 1),
			Map.of("Rune pouch", 1, "Law rune", 2, "Fire rune", 1));
		ctx.carried = Map.of("Rune pouch", 1);
		ctx.pouchCarried = true;

		RuneSupply.Result result = RuneSupply.plan(ctx);

		assertFalse(result.withdrawals.isEmpty());
		for (RunPlan.Withdrawal w : result.withdrawals)
		{
			assertEquals("runes in the carried pouch take no separate slot", 0, w.slots);
		}
	}

	@Test
	public void combinationRunesCoverBothParts()
	{
		RuneSupply.Context ctx = context(Map.of("Water rune", 2, "Fire rune", 2),
			Map.of("Steam rune", 2));
		ctx.carried = Map.of("Steam rune", 2);

		RuneSupply.Result result = RuneSupply.plan(ctx);

		assertFalse("no base runes are needed", has(result, "Water rune") || has(result, "Fire rune"));
		assertTrue(result.notes.toString().contains("Steam rune"));
	}

	@Test
	public void runeLineShowsWhatIsStillMissing()
	{
		RuneSupply.Context ctx = context(Map.of("Law rune", 5), Map.of("Law rune", 2));
		ctx.carried = Map.of("Law rune", 2);

		RuneSupply.Result result = RuneSupply.plan(ctx);

		RunPlan.Withdrawal line = result.withdrawals.stream()
			.filter(w -> w.itemName.equals("Law rune")).findFirst().orElse(null);
		assertTrue(line != null);
		assertEquals("total need", 5, line.quantity);
		assertEquals("still to withdraw", 3, line.toWithdraw);
	}

	@Test
	public void staffIsNotAddedWhenThePouchAlreadyCoversTheRune()
	{
		RuneSupply.Context ctx = context(Map.of("Air rune", 2),
			Map.of("Smoke battlestaff", 1, "Rune pouch", 1));
		ctx.carried = Map.of("Rune pouch", 1);
		ctx.pouch = Map.of("Air rune", 2);
		ctx.pouchCarried = true;

		RuneSupply.Result result = RuneSupply.plan(ctx);

		assertFalse("the pouch already has the air runes", has(result, "Smoke battlestaff"));
	}

	@Test
	public void herbRunKeepsMagicSecateursInsteadOfAnUnequippedStaff()
	{
		RuneSupply.Context ctx = context(Map.of("Air rune", 1, "Fire rune", 1),
			Map.of("Smoke battlestaff", 1, "Magic secateurs", 1));
		ctx.herbs = true;
		ctx.ownsMagicSecateurs = true;

		RuneSupply.Result result = RuneSupply.plan(ctx);

		assertFalse("secateurs stay in the weapon slot", has(result, "Smoke battlestaff"));
	}

	@Test
	public void equippedStaffIsUsedOnHerbRuns()
	{
		RuneSupply.Context ctx = context(Map.of("Air rune", 1, "Fire rune", 1),
			Map.of("Smoke battlestaff", 1, "Magic secateurs", 1));
		ctx.herbs = true;
		ctx.ownsMagicSecateurs = true;
		ctx.equipped = Map.of("Smoke battlestaff", 1);

		RuneSupply.Result result = RuneSupply.plan(ctx);

		assertTrue("an equipped staff costs nothing", has(result, "Smoke battlestaff"));
		assertFalse(has(result, "Air rune"));
	}
}
