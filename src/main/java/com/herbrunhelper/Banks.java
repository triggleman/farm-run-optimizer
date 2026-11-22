package com.herbrunhelper;

/**
 * A tile inside the bank a stop names, so the next bank visit can be walked to as well as read.
 * Only the banks the route names are listed; an unknown name falls back to the panel's text.
 */
final class Banks
{
	private Banks()
	{
	}

	/** {x, y, plane} for a bank name, or null. */
	static int[] tile(String name)
	{
		if (name == null)
		{
			return null;
		}
		switch (name)
		{
			case "Al Kharid bank":
				return new int[]{3269, 3167, 0};
			case "Ardougne North bank":
				return new int[]{2618, 3332, 0};
			case "Ardougne South bank":
				return new int[]{2655, 3283, 0};
			case "Catherby bank":
				return new int[]{2809, 3441, 0};
			case "Civitas illa Fortis bank":
				return new int[]{1648, 3118, 0};
			case "Draynor Village bank":
				return new int[]{3093, 3243, 0};
			case "Falador East bank":
				return new int[]{3013, 3355, 0};
			case "Falador West bank":
				return new int[]{2946, 3368, 0};
			case "Farming Guild bank":
				return new int[]{1249, 3758, 0};
			case "Grand Tree bank":
				return new int[]{2445, 3424, 1};
			case "Hosidius kitchen bank":
				return new int[]{1674, 3615, 0};
			case "Lletya bank":
				return new int[]{2352, 3162, 0};
			case "Lumbridge Castle bank (top floor)":
				return new int[]{3209, 3222, 2};
			case "Museum Camp bank":
				return new int[]{3768, 3899, 0};
			case "Port Phasmatys bank":
				return new int[]{3687, 3466, 0};
			case "Seers' Village bank":
				return new int[]{2722, 3493, 0};
			case "Varrock East bank":
				return new int[]{3253, 3420, 0};
			case "Varrock West bank":
				return new int[]{3185, 3436, 0};
			case "Yanille bank":
				return new int[]{2613, 3092, 0};
			default:
				return null;
		}
	}
}
