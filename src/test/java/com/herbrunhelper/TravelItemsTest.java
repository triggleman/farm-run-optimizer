package com.herbrunhelper;

import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TravelItemsTest
{
	@Test
	public void findsAnOwnedTeleportItem()
	{
		assertEquals("Salve graveyard teleport",
			TravelItems.find("Salve graveyard teleport", Set.of("Salve graveyard teleport")));
	}

	@Test
	public void findsTheOwnedChargeVariant()
	{
		assertEquals("Skills necklace(6)",
			TravelItems.find("Skills necklace(6)", Set.of("Skills necklace(6)", "Skills necklace(1)")));
	}

	@Test
	public void doesNotMistakeASpellForATablet()
	{
		assertNull(TravelItems.find("Varrock Teleport", Set.of("Varrock teleport")));
	}

	@Test
	public void knowsAnItemEvenWhenItIsNotOwned()
	{
		assertEquals("Teleport to house", TravelItems.find("Teleport to house", Set.of()));
	}

	@Test
	public void mapsTabletDisplayToTheTeleportItem()
	{
		assertEquals("Salve graveyard teleport", TravelItems.find("Salve Graveyard tablet", Set.of()));
		assertEquals("Catherby teleport", TravelItems.find("Catherby tablet", Set.of()));
		assertEquals("Kourend castle teleport", TravelItems.find("Kourend Castle tablet", Set.of()));
		assertEquals("Fenkenstrain's castle teleport",
			TravelItems.find("Fenkenstrain's Castle tablet", Set.of()));
		assertEquals("Teleport to house",
			TravelItems.find("Teleport to House tablet: Outside the house", Set.of()));
	}

	@Test
	public void mapsTabletDisplayToAnOwnedVariant()
	{
		assertEquals("Salve graveyard teleport",
			TravelItems.find("Salve Graveyard tablet", Set.of("Salve graveyard teleport")));
	}

	@Test
	public void prefersTheOwnedVariantWithMostCharges()
	{
		assertEquals("Ring of wealth (5)", TravelItems.find("Ring of wealth: Falador Park",
			Set.of("Ring of wealth", "Ring of wealth (2)", "Ring of wealth (5)")));
		assertEquals("Amulet of glory(6)", TravelItems.find("Amulet of glory: Edgeville",
			Set.of("Amulet of glory(1)", "Amulet of glory(6)")));
	}

	@Test
	public void prefersTheOwnedHighestTier()
	{
		assertEquals("Ardougne cloak 4", TravelItems.find("Ardougne cloak: Ardougne Farm",
			Set.of("Ardougne cloak 3", "Ardougne cloak 4")));
	}

	@Test
	public void resolvesTheVariantCarriedRightNow()
	{
		assertEquals("Ring of wealth (3)", TravelItems.resolve("Ring of wealth (5)",
			Set.of("Ring of wealth (4)", "Ring of wealth (3)"), Set.of("Ring of wealth (3)")));
		assertEquals("Ring of wealth (4)", TravelItems.resolve("Ring of wealth (5)",
			Set.of("Ring of wealth (4)"), Set.of()));
		assertEquals("Ring of wealth", TravelItems.resolve("Ring of wealth (5)", Set.of(), Set.of()));
	}

	@Test
	public void ignoresFairyRings()
	{
		assertNull(TravelItems.find("Fairy ring CIR", Set.of()));
	}
}
