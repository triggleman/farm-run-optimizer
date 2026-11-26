package com.herbrunhelper;

import java.util.EnumMap;
import java.util.Map;

/**
 * Farming experience per crop (OSRS Wiki): planting, harvesting each item, and checking a
 * grown tree's, bush's or cactus's health. Used for the "about X XP" estimate before a run.
 */
public final class FarmingXp
{
	/** Rough crops per allotment patch with compost. */
	public static final double ALLOTMENT_YIELD = 10.0;
	/** Rough hops per patch (at least 3, more with compost and level). */
	public static final double HOPS_YIELD = 6.0;

	private static final Map<Crop, double[]> XP = new EnumMap<>(Crop.class);

	static
	{
		// {plant, harvest each, check health}
		put(Crop.GUAM, 11, 12.5, 0);
		put(Crop.MARRENTILL, 13.5, 15, 0);
		put(Crop.TARROMIN, 16, 18, 0);
		put(Crop.HARRALANDER, 21.5, 24, 0);
		put(Crop.RANARR, 27, 30.5, 0);
		put(Crop.TOADFLAX, 34, 38.5, 0);
		put(Crop.IRIT, 43, 48.5, 0);
		put(Crop.AVANTOE, 54.5, 61.5, 0);
		put(Crop.KWUARM, 69, 78, 0);
		put(Crop.SNAPDRAGON, 87.5, 98.5, 0);
		put(Crop.HUASCA, 86.5, 110, 0);
		put(Crop.CADANTINE, 106.5, 120, 0);
		put(Crop.LANTADYME, 134.5, 151.5, 0);
		put(Crop.DWARF_WEED, 170.5, 192, 0);
		put(Crop.TORSTOL, 199.5, 224.5, 0);

		put(Crop.POTATO, 8, 9, 0);
		put(Crop.ONION, 9.5, 10.5, 0);
		put(Crop.CABBAGE, 10, 11.5, 0);
		put(Crop.TOMATO, 12.5, 14, 0);
		put(Crop.SWEETCORN, 17, 19, 0);
		put(Crop.STRAWBERRY, 26, 29, 0);
		put(Crop.WATERMELON, 48.5, 54.5, 0);
		put(Crop.SNAPE_GRASS, 82, 82, 0);

		put(Crop.OAK, 14, 0, 467.3);
		put(Crop.WILLOW, 25, 0, 1456.5);
		put(Crop.MAPLE, 45, 0, 3403.4);
		put(Crop.YEW, 81, 0, 7069.9);
		put(Crop.MAGIC, 145.5, 0, 13768.3);

		put(Crop.APPLE, 22, 8.5, 1199.5);
		put(Crop.BANANA, 28, 10.5, 1750.5);
		put(Crop.ORANGE, 35.5, 13.5, 2470.2);
		put(Crop.CURRY, 40, 15, 2906.9);
		put(Crop.PINEAPPLE, 57, 21.5, 4605.7);
		put(Crop.PAPAYA, 72, 27, 6146.4);
		put(Crop.PALM, 110.5, 41.5, 10150.1);
		put(Crop.DRAGONFRUIT, 140, 70, 17335);

		put(Crop.TEAK, 35, 0, 7290);
		put(Crop.MAHOGANY, 63, 0, 15720);
		put(Crop.CAMPHOR, 88, 0, 17840);
		put(Crop.IRONWOOD, 145, 0, 20380);
		put(Crop.ROSEWOOD, 252, 0, 23100);
		put(Crop.REDBERRIES, 11.5, 4.5, 64);
		put(Crop.CADAVABERRIES, 18, 7, 102.5);
		put(Crop.DWELLBERRIES, 31.5, 12, 177.5);
		put(Crop.JANGERBERRIES, 50.5, 19, 284.5);
		put(Crop.WHITEBERRIES, 78, 29, 437.5);
		put(Crop.POISON_IVY, 120, 45, 675);
		put(Crop.BARLEY, 8.5, 9.5, 0);
		put(Crop.MARIGOLD, 8.5, 47, 0);
		put(Crop.ROSEMARY, 12, 66.5, 0);
		put(Crop.NASTURTIUM, 19.5, 111, 0);
		put(Crop.WOAD, 20.5, 115.5, 0);
		put(Crop.LIMPWURT, 21.5, 120, 0);
		put(Crop.WHITE_LILY, 42, 250, 0);
		put(Crop.HAMMERSTONE, 9, 10, 0);
		put(Crop.ASGARNIAN, 10.9, 12, 0);
		put(Crop.JUTE, 13, 14.5, 0);
		put(Crop.YANILLIAN, 14.5, 16, 0);
		put(Crop.KRANDORIAN, 17.5, 19.5, 0);
		put(Crop.WILDBLOOD, 23, 26, 0);
		put(Crop.CACTUS, 66.5, 25, 374);
		put(Crop.POTATO_CACTUS, 68, 68, 230);
		put(Crop.CALQUAT, 129.5, 48.5, 12096);
		put(Crop.SPIRIT_TREE, 199.5, 0, 19301);
		put(Crop.CELASTRUS, 204, 23.5, 14130);
		put(Crop.REDWOOD, 230, 0, 22450);
	}

	private FarmingXp()
	{
	}

	private static void put(Crop crop, double plant, double harvest, double check)
	{
		XP.put(crop, new double[]{plant, harvest, check});
	}

	public static double plant(Crop crop)
	{
		double[] x = XP.get(crop);
		return x == null ? 0 : x[0];
	}

	/** XP for what you collect from a grown crop: the harvest, or the tree's health check. */
	public static double grown(Crop crop, double herbYield)
	{
		double[] x = XP.get(crop);
		if (x == null)
		{
			return 0;
		}
		switch (crop.getType())
		{
			case HERB:
				return x[1] * herbYield;
			case ALLOTMENT:
				return x[1] * ALLOTMENT_YIELD;
			case FLOWER:
				return x[1]; // one harvest's worth, given when the patch is cleared
			case HOPS:
				return x[1] * HOPS_YIELD;
			default:
				return x[2]; // fruit you pick is optional, so only the health check counts
		}
	}

	public static double compost(String compost)
	{
		if (compost == null)
		{
			return 0;
		}
		switch (compost)
		{
			case "Ultracompost":
				return 36;
			case "Supercompost":
				return 26;
			case "Compost":
				return 18;
			default:
				return compost.startsWith("Bottomless") ? 36 : 0;
		}
	}

	/** 12345 -> "12.3k". */
	public static String format(double xp)
	{
		long v = Math.round(xp);
		return v >= 10_000 ? (v / 1000) + "k" : v >= 1000 ? String.format("%.1fk", v / 1000.0) : String.valueOf(v);
	}
}
