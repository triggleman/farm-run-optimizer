package com.herbrunhelper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TeleportTest
{
	@Test
	public void inertXericsTalismanIsNotAvailable()
	{
		Teleport.Context ctx = new Teleport.Context();
		ctx.owned = Map.of("Xeric's talisman (inert)", 1);

		assertFalse(Teleport.XERICS_TALISMAN.check(ctx).available);
	}

	@Test
	public void chargedXericsTalismanIsAvailable()
	{
		Teleport.Context ctx = new Teleport.Context();
		ctx.owned = Map.of("Xeric's talisman", 1);

		assertTrue(Teleport.XERICS_TALISMAN.check(ctx).available);
		assertEquals("Xeric's talisman", Teleport.XERICS_TALISMAN.check(ctx).ownedItem);
	}

	@Test
	public void chargedXericsTalismanWinsOverTheInertOne()
	{
		Map<String, Integer> owned = new LinkedHashMap<>();
		owned.put("Xeric's talisman (inert)", 1);
		owned.put("Xeric's talisman", 1);

		Teleport.Context ctx = new Teleport.Context();
		ctx.owned = owned;

		assertEquals("Xeric's talisman", Teleport.XERICS_TALISMAN.check(ctx).ownedItem);
	}

	@Test
	public void picksTheChargedVariantInTheBank()
	{
		Map<String, Integer> owned = new LinkedHashMap<>();
		owned.put("Skills necklace(1)", 1);
		owned.put("Skills necklace(6)", 1);
		owned.put("Skills necklace(0)", 1);

		Teleport.Context ctx = new Teleport.Context();
		ctx.owned = owned;

		Teleport.Availability availability = Teleport.SKILLS_NECKLACE.check(ctx);

		assertEquals("Skills necklace(6)", availability.ownedItem);
	}

	@Test
	public void zeroChargeJewelleryIsNotAvailable()
	{
		Teleport.Context ctx = new Teleport.Context();
		ctx.owned = Map.of("Ring of wealth (0)", 1);

		assertFalse(Teleport.RING_OF_WEALTH.check(ctx).available);
	}
}
