package com.herbrunhelper;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GrowthClockTest
{
	@Test
	public void potatoCactusUsesItsOwnTenMinuteTick()
	{
		GrowthClock clock = new GrowthClock();
		long changedAt = 1_700_000_000_000L;
		clock.learn(Crop.POTATO_CACTUS, changedAt);

		long now = changedAt + 60_000L;
		long next = clock.nextTick(Crop.POTATO_CACTUS, now);

		assertTrue("the next tick is soon, not up to 80 minutes away", next - now <= 10 * 60_000L);
		assertEquals(Math.floorMod(changedAt, 10 * 60_000L), Math.floorMod(next, 10 * 60_000L));
	}

	@Test
	public void phasesSurviveASaveAndLoad()
	{
		GrowthClock before = new GrowthClock();
		long changedAt = 1_700_000_000_000L;
		before.learn(Crop.POTATO_CACTUS, changedAt);
		before.learn(PatchType.HERB, changedAt);
		long now = changedAt + 5 * 60_000L;

		GrowthClock after = new GrowthClock();
		after.load(before.serialize());

		assertEquals(before.nextTick(Crop.POTATO_CACTUS, now), after.nextTick(Crop.POTATO_CACTUS, now));
		assertEquals(before.nextTick(Crop.RANARR, now), after.nextTick(Crop.RANARR, now));
	}
}
