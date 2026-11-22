package com.herbrunhelper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TeleportTest
{
	@Test
	public void readsChargesFromTheItemName()
	{
		assertEquals(6, Teleport.chargesOf("Skills necklace(6)"));
		assertEquals(5, Teleport.chargesOf("Ring of wealth (5)"));
		assertEquals(0, Teleport.chargesOf("Ectophial"));
		assertEquals(0, Teleport.chargesOf("Teleport crystal (w)"));
	}

	@Test
	public void picksTheChargedVariantInTheBank()
	{
		Map<String, Integer> owned = new LinkedHashMap<>();
		owned.put("Skills necklace(1)", 1);
		owned.put("Skills necklace(6)", 1);

		Teleport.Context ctx = new Teleport.Context();
		ctx.owned = owned;

		Teleport.Availability availability = Teleport.SKILLS_NECKLACE.check(ctx);

		assertEquals("Skills necklace(6)", availability.ownedItem);
	}
}
