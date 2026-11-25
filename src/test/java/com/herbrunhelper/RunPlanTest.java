package com.herbrunhelper;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RunPlanTest
{
	@Test
	public void bottomlessCompostBucketIsNotNoted()
	{
		RunPlan.Withdrawal bucket = new RunPlan.Withdrawal("Bottomless compost bucket", 1, "");
		assertFalse(bucket.noted);
	}

	@Test
	public void compostIsStillNoted()
	{
		RunPlan.Withdrawal compost = new RunPlan.Withdrawal("Ultracompost", 1, "");
		assertTrue(compost.noted);
	}
}
