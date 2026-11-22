package com.herbrunhelper;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BanksTest
{
	@Test
	public void knownBanksHaveATile()
	{
		int[] catherby = Banks.tile("Catherby bank");
		assertNotNull(catherby);
		assertEquals(3, catherby.length);
		assertNull(Banks.tile("Nowhere bank"));
		assertNull(Banks.tile(null));
	}

	@Test
	public void bankStopCarriesItsTile()
	{
		RunPlan.BankStop stop = new RunPlan.BankStop(3, "Falador East bank");
		assertEquals(3013, stop.bankX);
		assertEquals(3355, stop.bankY);
		assertEquals(0, stop.bankPlane);
	}
}
