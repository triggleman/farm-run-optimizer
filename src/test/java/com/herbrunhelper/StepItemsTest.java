package com.herbrunhelper;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class StepItemsTest
{
	private static RunPlan.RouteStep step(FarmPatch patch, Crop crop)
	{
		RunPlan.RouteStep step = new RunPlan.RouteStep(patch, null, null, null, false);
		step.crop = crop;
		return step;
	}

	@Test
	public void diseasedHerbNeedsPlantCure()
	{
		StepItems.Next next = StepItems.next(step(FarmPatch.CATHERBY_HERB, Crop.RANARR),
			null, PatchTimers.Status.DISEASED, null, System.currentTimeMillis());

		assertNotNull(next);
		assertEquals("Plant cure", next.item);
	}

	@Test
	public void diseasedTreeNeedsSecateurs()
	{
		StepItems.Next next = StepItems.next(step(FarmPatch.VARROCK_TREE, Crop.OAK),
			null, PatchTimers.Status.DISEASED, null, System.currentTimeMillis());

		assertNotNull(next);
		assertEquals("Secateurs", next.item);
	}

	@Test
	public void growingCropDoesNotAskToPlantAgain()
	{
		long now = System.currentTimeMillis();
		PatchTimers.Planting growing = new PatchTimers.Planting(Crop.RANARR, now - 60_000L);

		StepItems.Next next = StepItems.next(step(FarmPatch.CATHERBY_HERB, Crop.RANARR),
			growing, null, null, now);

		assertNull(next);
	}

	@Test
	public void emptyPatchAsksForTheSeed()
	{
		StepItems.Next next = StepItems.next(step(FarmPatch.CATHERBY_HERB, Crop.RANARR),
			null, PatchTimers.Status.EMPTY, null, System.currentTimeMillis());

		assertNotNull(next);
		assertEquals("Ranarr seed", next.item);
		assertEquals("Plant", next.action);
	}

	@Test
	public void weedsComeFirst()
	{
		StepItems.Next next = StepItems.next(step(FarmPatch.CATHERBY_HERB, Crop.RANARR),
			null, PatchTimers.Status.WEEDS, null, System.currentTimeMillis());

		assertNotNull(next);
		assertEquals("Rake", next.item);
	}
}
