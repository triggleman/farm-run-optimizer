package com.herbrunhelper;

/**
 * Kinds of patch, each its own run. Growth tick lengths and table names match RuneLite's Time
 * Tracking plugin (Produce / PatchImplementation); how each is cleared follows the OSRS Wiki.
 */
public enum PatchType
{
	HERB("Herbs", "HERB", 20, "includeHerbs", Clear.HARVEST),
	ALLOTMENT("Allotments", "ALLOTMENT", 10, "includeAllotments", Clear.HARVEST),
	FLOWER("Flowers", "FLOWER", 5, "includeFlowers", Clear.HARVEST),
	TREE("Trees", "TREE", 40, "includeTrees", Clear.GARDENER),
	FRUIT_TREE("Fruit trees", "FRUIT_TREE", 160, "includeFruitTrees", Clear.GARDENER),
	HARDWOOD("Hardwood", "HARDWOOD_TREE", 640, "includeHardwood", Clear.GARDENER),
	BUSH("Bushes", "BUSH", 20, "includeBushes", Clear.SPADE),
	HOPS("Hops", "HOPS", 10, "includeHops", Clear.HARVEST),
	CACTUS("Cactus", "CACTUS", 80, "includeCactus", Clear.SPADE),
	CALQUAT("Calquat", "CALQUAT", 160, "includeCalquat", Clear.GARDENER),
	SPIRIT_TREE("Spirit trees", "SPIRIT_TREE", 320, "includeSpiritTrees", Clear.SPADE),
	CELASTRUS("Celastrus", "CELASTRUS", 160, "includeCelastrus", Clear.SPADE),
	REDWOOD("Redwood", "REDWOOD", 640, "includeRedwood", Clear.SPADE);

	/** How a grown crop is got rid of before replanting. */
	public enum Clear
	{
		/** Harvesting empties the patch (herbs, allotments, hops). */
		HARVEST,
		/** Pay the gardener coins to chop it down. */
		GARDENER,
		/** Dig it up with a spade (after harvesting or chopping, where there's anything to take). */
		SPADE
	}

	private final String displayName;
	/** Name used for this type in patch-states.tsv. */
	private final String tableName;
	/** Minutes between growth ticks (a crop can have its own; see Crop). */
	private final int tickMinutes;
	/** Settings key for "include this run". */
	private final String configKey;
	private final Clear clear;

	PatchType(String displayName, String tableName, int tickMinutes, String configKey, Clear clear)
	{
		this.displayName = displayName;
		this.tableName = tableName;
		this.tickMinutes = tickMinutes;
		this.configKey = configKey;
		this.clear = clear;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getTableName()
	{
		return tableName;
	}

	public int getTickMinutes()
	{
		return tickMinutes;
	}

	public String getConfigKey()
	{
		return configKey;
	}

	public Clear getClear()
	{
		return clear;
	}

	/**
	 * Grown crops that stay in the patch: their health is checked when grown, and they must be
	 * cleared (gardener or spade) before replanting. Everything but herbs, allotments and hops.
	 */
	public boolean isTreeLike()
	{
		return clear != Clear.HARVEST;
	}

	/** Planted as a sapling (grown from a seed in a plant pot) rather than seeds. */
	public boolean usesSaplings()
	{
		return this == TREE || this == FRUIT_TREE || this == HARDWOOD || this == CALQUAT
			|| this == SPIRIT_TREE || this == CELASTRUS || this == REDWOOD;
	}

	/** Gardeners can be paid to protect these crops (not herbs or flowers). */
	public boolean hasGardenerPayment()
	{
		return this != HERB && this != FLOWER;
	}

	/** Seeds that stack in one inventory slot (not saplings). */
	public boolean seedsStack()
	{
		return !usesSaplings();
	}

	/** Tiles across a typical patch of this type (for outlines when the game gives none). */
	public int patchSize()
	{
		return usesSaplings() ? 3 : 2;
	}

	/** "seeds" or "saplings". */
	public String seedWord()
	{
		return usesSaplings() ? "saplings" : "seeds";
	}

	/** Singular patch name for sentences: "herb", "bush", "spirit tree"... */
	public String patchName()
	{
		switch (this)
		{
			case HERB:
				return "herb";
			case ALLOTMENT:
				return "allotment";
			case FLOWER:
				return "flower";
			case TREE:
				return "tree";
			case FRUIT_TREE:
				return "fruit tree";
			case HARDWOOD:
				return "hardwood";
			case BUSH:
				return "bush";
			case HOPS:
				return "hops";
			case CACTUS:
				return "cactus";
			case CALQUAT:
				return "calquat";
			case SPIRIT_TREE:
				return "spirit tree";
			case CELASTRUS:
				return "celastrus";
			default:
				return "redwood";
		}
	}
}
