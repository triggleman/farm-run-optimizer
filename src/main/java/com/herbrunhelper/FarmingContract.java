package com.herbrunhelper;

/**
 * Farming Guild contracts, read from the game's contract varbit. The numbers match RuneLite's
 * Time Tracking plugin (Produce.java, contractVarbitValue).
 */
public final class FarmingContract
{
	/** Varbit holding which crop the current contract is for (0 = no contract). */
	public static final int TYPE_VARBIT = 7963;
	/** Varbit that's set once the contract crop has been grown (go claim the reward). */
	public static final int COMPLETE_VARBIT = 7961;

	private static final String[] NAMES = new String[52];
	private static final String[] PATCHES = new String[52];

	static
	{
		put(1, "Redberry", "bush");
		put(2, "Cadavaberry", "bush");
		put(3, "Dwellberry", "bush");
		put(4, "Jangerberry", "bush");
		put(5, "Whiteberry", "bush");
		put(6, "Poison ivy", "bush");
		put(7, "Oak", "tree");
		put(8, "Willow", "tree");
		put(9, "Maple", "tree");
		put(10, "Yew", "tree");
		put(11, "Magic", "tree");
		put(12, "Apple", "fruit tree");
		put(13, "Banana", "fruit tree");
		put(14, "Orange", "fruit tree");
		put(15, "Curry", "fruit tree");
		put(16, "Pineapple", "fruit tree");
		put(17, "Papaya", "fruit tree");
		put(18, "Palm", "fruit tree");
		put(19, "Dragonfruit", "fruit tree");
		put(20, "Guam", "herb");
		put(21, "Marrentill", "herb");
		put(22, "Tarromin", "herb");
		put(23, "Harralander", "herb");
		put(24, "Ranarr", "herb");
		put(25, "Toadflax", "herb");
		put(26, "Irit", "herb");
		put(27, "Avantoe", "herb");
		put(28, "Kwuarm", "herb");
		put(29, "Snapdragon", "herb");
		put(30, "Cadantine", "herb");
		put(31, "Lantadyme", "herb");
		put(32, "Dwarf weed", "herb");
		put(33, "Torstol", "herb");
		put(34, "Marigold", "flower");
		put(35, "Rosemary", "flower");
		put(36, "Nasturtium", "flower");
		put(37, "Woad", "flower");
		put(38, "Limpwurt", "flower");
		put(39, "White lily", "flower");
		put(40, "Potato", "allotment");
		put(41, "Onion", "allotment");
		put(42, "Cabbage", "allotment");
		put(43, "Tomato", "allotment");
		put(44, "Sweetcorn", "allotment");
		put(45, "Strawberry", "allotment");
		put(46, "Watermelon", "allotment");
		put(47, "Snape grass", "allotment");
		put(48, "Cactus", "cactus");
		put(49, "Potato cactus", "cactus");
		put(50, "Redwood", "redwood");
		put(51, "Celastrus", "celastrus");
	}

	private static void put(int value, String name, String patch)
	{
		NAMES[value] = name;
		PATCHES[value] = patch;
	}

	public final int value;
	public final String name;
	public final String patchKind;
	/** The crop if it's one this plugin plants (herb, tree, fruit tree), otherwise null. */
	public final Crop crop;
	public final boolean complete;

	private FarmingContract(int value, boolean complete)
	{
		this.value = value;
		this.name = NAMES[value];
		this.patchKind = PATCHES[value];
		this.complete = complete;
		Crop match = null;
		for (Crop c : Crop.values())
		{
			if (c.getDisplayName().equalsIgnoreCase(name) && kind(c.getType()).equals(patchKind))
			{
				match = c;
			}
		}
		this.crop = match;
	}

	private static String kind(PatchType t)
	{
		return t.patchName();
	}

	/** Null when there's no contract (or the value is unknown). */
	public static FarmingContract of(int typeValue, int completeValue)
	{
		if (typeValue <= 0 || typeValue >= NAMES.length || NAMES[typeValue] == null)
		{
			return null;
		}
		return new FarmingContract(typeValue, completeValue > 0);
	}

	/** The Farming Guild patch the contract crop goes in, if it's one of the run's patch types. */
	public FarmPatch guildPatch()
	{
		if (crop == null)
		{
			return null;
		}
		switch (crop.getType())
		{
			case HERB:
				return FarmPatch.FARMING_GUILD_HERB;
			case TREE:
				return FarmPatch.FARMING_GUILD_TREE;
			case FRUIT_TREE:
				return FarmPatch.FARMING_GUILD_FRUIT;
			case ALLOTMENT:
				return FarmPatch.GUILD_ALLOTMENT_A;
			case FLOWER:
				return FarmPatch.GUILD_FLOWER;
			case BUSH:
				return FarmPatch.GUILD_BUSH;
			case CACTUS:
				return FarmPatch.GUILD_CACTUS;
			case CELASTRUS:
				return FarmPatch.GUILD_CELASTRUS;
			case REDWOOD:
				return FarmPatch.GUILD_REDWOOD;
			default:
				return null;
		}
	}

	public String describe()
	{
		if (complete)
		{
			return "Contract done: talk to Guildmaster Jane for your seed pack and a new contract.";
		}
		String where = "in the Farming Guild " + patchKind + " patch";
		return "Grow " + name.toLowerCase() + " " + where + ".";
	}
}
