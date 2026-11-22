package com.herbrunhelper;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

/**
 * The tool leprechaun's store, read from the player's varbits (the game sends them everywhere, so
 * no visit is needed). Large counts are split between a base varbit and an overflow varbit, as
 * the game's own farming_tools_getstored script works them out.
 */
final class Leprechaun
{
	private Leprechaun()
	{
	}

	/** varbit ids that change when the leprechaun's store changes. */
	static final int[] WATCHED = {
		VarbitID.FARMING_TOOLS_RAKE, VarbitID.FARMING_TOOLS_EXTRARAKES,
		VarbitID.FARMING_TOOLS_DIBBER, VarbitID.FARMING_TOOLS_EXTRADIBBERS,
		VarbitID.FARMING_TOOLS_SPADE, VarbitID.FARMING_TOOLS_EXTRASPADES,
		VarbitID.FARMING_TOOLS_SECATEURS, VarbitID.FARMING_TOOLS_EXTRASECATEURS,
		VarbitID.FARMING_TOOLS_FAIRYSECATEURS,
		VarbitID.FARMING_TOOLS_COMPOST, VarbitID.FARMING_TOOLS_EXTRACOMPOST,
		VarbitID.FARMING_TOOLS_SUPERCOMPOST, VarbitID.FARMING_TOOLS_EXTRASUPERCOMPOST,
		VarbitID.FARMING_TOOLS_ULTRACOMPOST,
		VarbitID.FARMING_TOOLS_PLANTCURE,
		VarbitID.FARMING_TOOLS_BOTTOMLESS_BUCKET_TYPE,
		VarbitID.FARMING_TOOLS_BOTTOMLESS_BUCKET_QUANTITY,
	};

	static Map<String, Integer> read(Client client)
	{
		Map<String, Integer> items = new LinkedHashMap<>();
		put(items, "Rake", total(client, VarbitID.FARMING_TOOLS_RAKE, VarbitID.FARMING_TOOLS_EXTRARAKES, 1));
		put(items, "Seed dibber", total(client, VarbitID.FARMING_TOOLS_DIBBER, VarbitID.FARMING_TOOLS_EXTRADIBBERS, 1));
		put(items, "Spade", total(client, VarbitID.FARMING_TOOLS_SPADE, VarbitID.FARMING_TOOLS_EXTRASPADES, 1));
		put(items, "Secateurs", total(client, VarbitID.FARMING_TOOLS_SECATEURS, VarbitID.FARMING_TOOLS_EXTRASECATEURS, 1));
		put(items, "Magic secateurs", client.getVarbitValue(VarbitID.FARMING_TOOLS_FAIRYSECATEURS));
		put(items, "Compost", total(client, VarbitID.FARMING_TOOLS_COMPOST, VarbitID.FARMING_TOOLS_EXTRACOMPOST, 8));
		put(items, "Supercompost", total(client, VarbitID.FARMING_TOOLS_SUPERCOMPOST,
			VarbitID.FARMING_TOOLS_EXTRASUPERCOMPOST, 8));
		put(items, "Ultracompost", client.getVarbitValue(VarbitID.FARMING_TOOLS_ULTRACOMPOST));
		put(items, "Plant cure", client.getVarbitValue(VarbitID.FARMING_TOOLS_PLANTCURE));

		// The bottomless bucket stored at the leprechaun. Type 1 is an empty bucket; 2/3/4 are
		// compost, supercompost and ultracompost (4 confirmed in game).
		int bucketType = client.getVarbitValue(VarbitID.FARMING_TOOLS_BOTTOMLESS_BUCKET_TYPE);
		int bucketQuantity = client.getVarbitValue(VarbitID.FARMING_TOOLS_BOTTOMLESS_BUCKET_QUANTITY);
		if (bucketType >= 2 && bucketQuantity > 0)
		{
			put(items, "Bottomless compost bucket", 1);
		}
		return items;
	}

	private static int total(Client client, int baseVarbit, int extraVarbit, int extraShift)
	{
		int base = client.getVarbitValue(baseVarbit);
		int extra = client.getVarbitValue(extraVarbit);
		return base + (extra << extraShift);
	}

	private static void put(Map<String, Integer> items, String name, int quantity)
	{
		if (quantity > 0)
		{
			items.merge(name, quantity, Integer::sum);
		}
	}
}
