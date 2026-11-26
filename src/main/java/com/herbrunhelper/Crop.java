package com.herbrunhelper;

/**
 * Everything the runs plant: herbs, allotments, trees, fruit trees and hardwood trees.
 *
 * Constant names match the crop names in patch-states.tsv. Growth stages follow RuneLite's
 * Time Tracking plugin (Produce.java): a crop finishes after (stages - 1) growth
 * ticks of its patch type's tick length. Gardener payments are from the OSRS Wiki and can be
 * handed over noted.
 */
public enum Crop
{
	// Herbs: 20-minute ticks
	GUAM(PatchType.HERB, "Guam", 9, "Guam seed", 5, null, 0, 249),
	MARRENTILL(PatchType.HERB, "Marrentill", 14, "Marrentill seed", 5, null, 0, 251),
	TARROMIN(PatchType.HERB, "Tarromin", 19, "Tarromin seed", 5, null, 0, 253),
	HARRALANDER(PatchType.HERB, "Harralander", 26, "Harralander seed", 5, null, 0, 255),
	RANARR(PatchType.HERB, "Ranarr", 32, "Ranarr seed", 5, null, 0, 257),
	TOADFLAX(PatchType.HERB, "Toadflax", 38, "Toadflax seed", 5, null, 0, 2998),
	IRIT(PatchType.HERB, "Irit", 44, "Irit seed", 5, null, 0, 259),
	AVANTOE(PatchType.HERB, "Avantoe", 50, "Avantoe seed", 5, null, 0, 261),
	KWUARM(PatchType.HERB, "Kwuarm", 56, "Kwuarm seed", 5, null, 0, 263),
	SNAPDRAGON(PatchType.HERB, "Snapdragon", 62, "Snapdragon seed", 5, null, 0, 3000),
	HUASCA(PatchType.HERB, "Huasca", 65, "Huasca seed", 5, null, 0, 30097),
	CADANTINE(PatchType.HERB, "Cadantine", 67, "Cadantine seed", 5, null, 0, 265),
	LANTADYME(PatchType.HERB, "Lantadyme", 73, "Lantadyme seed", 5, null, 0, 2481),
	DWARF_WEED(PatchType.HERB, "Dwarf weed", 79, "Dwarf weed seed", 5, null, 0, 267),
	TORSTOL(PatchType.HERB, "Torstol", 85, "Torstol seed", 5, null, 0, 269),

	// Allotments: 10-minute ticks, 3 seeds per patch
	POTATO(PatchType.ALLOTMENT, "Potato", 1, "Potato seed", 5, "Compost", 2, 1942),
	ONION(PatchType.ALLOTMENT, "Onion", 5, "Onion seed", 5, "Potatoes(10)", 1, 1957),
	CABBAGE(PatchType.ALLOTMENT, "Cabbage", 7, "Cabbage seed", 5, "Onions(10)", 1, 1965),
	TOMATO(PatchType.ALLOTMENT, "Tomato", 12, "Tomato seed", 5, "Cabbages(10)", 2, 1982),
	SWEETCORN(PatchType.ALLOTMENT, "Sweetcorn", 20, "Sweetcorn seed", 7, "Jute fibre", 10, 5986),
	STRAWBERRY(PatchType.ALLOTMENT, "Strawberry", 31, "Strawberry seed", 7, "Apples(5)", 1, 5504),
	WATERMELON(PatchType.ALLOTMENT, "Watermelon", 47, "Watermelon seed", 9, "Curry leaf", 10, 5982),
	SNAPE_GRASS(PatchType.ALLOTMENT, "Snape grass", 61, "Snape grass seed", 8, "Jangerberries", 5, 231),

	// Trees: 40-minute ticks
	OAK(PatchType.TREE, "Oak", 15, "Oak sapling", 5, "Tomatoes(5)", 1, 1521),
	WILLOW(PatchType.TREE, "Willow", 30, "Willow sapling", 7, "Apples(5)", 1, 1519),
	MAPLE(PatchType.TREE, "Maple", 45, "Maple sapling", 9, "Oranges(5)", 1, 1517),
	YEW(PatchType.TREE, "Yew", 60, "Yew sapling", 11, "Cactus spine", 10, 1515),
	MAGIC(PatchType.TREE, "Magic", 75, "Magic sapling", 13, "Coconut", 25, 1513),

	// Fruit trees: 160-minute ticks (16 hours)
	APPLE(PatchType.FRUIT_TREE, "Apple", 27, "Apple sapling", 7, "Sweetcorn", 9, 1955),
	BANANA(PatchType.FRUIT_TREE, "Banana", 33, "Banana sapling", 7, "Apples(5)", 4, 1963),
	ORANGE(PatchType.FRUIT_TREE, "Orange", 39, "Orange sapling", 7, "Strawberries(5)", 3, 2108),
	CURRY(PatchType.FRUIT_TREE, "Curry", 42, "Curry sapling", 7, "Bananas(5)", 5, 5970),
	PINEAPPLE(PatchType.FRUIT_TREE, "Pineapple", 51, "Pineapple sapling", 7, "Watermelon", 10, 2114),
	PAPAYA(PatchType.FRUIT_TREE, "Papaya", 57, "Papaya sapling", 7, "Pineapple", 10, 5972),
	PALM(PatchType.FRUIT_TREE, "Palm", 68, "Palm sapling", 7, "Papaya fruit", 15, 5974),
	DRAGONFRUIT(PatchType.FRUIT_TREE, "Dragonfruit", 81, "Dragonfruit sapling", 7, "Coconut", 15, 22929),

	// Hardwood: 640-minute ticks
	TEAK(PatchType.HARDWOOD, "Teak", 35, "Teak sapling", 8, "Limpwurt root", 15, 6333),
	MAHOGANY(PatchType.HARDWOOD, "Mahogany", 55, "Mahogany sapling", 9, "Yanillian hops", 25, 6332),
	// Sailing update hardwoods (19 Nov 2025): stages from RuneLite's Produce, payments from the OSRS Wiki.
	CAMPHOR(PatchType.HARDWOOD, "Camphor", 66, "Camphor sapling", 9, "White berries", 10, 32904),
	IRONWOOD(PatchType.HARDWOOD, "Ironwood", 80, "Ironwood sapling", 9, "Curry leaf", 10, 32907),
	ROSEWOOD(PatchType.HARDWOOD, "Rosewood", 92, "Rosewood sapling", 10, "Dragonfruit", 8, 32910),

	// Bushes: stages and ticks from RuneLite's Produce; levels, payments from the OSRS Wiki.
	REDBERRIES(PatchType.BUSH, "Redberry", 10, "Redberry seed", 6, "Cabbages(10)", 4, 1951),
	CADAVABERRIES(PatchType.BUSH, "Cadavaberry", 22, "Cadavaberry seed", 7, "Tomatoes(5)", 3, 753),
	DWELLBERRIES(PatchType.BUSH, "Dwellberry", 36, "Dwellberry seed", 8, "Strawberries(5)", 3, 2126),
	JANGERBERRIES(PatchType.BUSH, "Jangerberry", 48, "Jangerberry seed", 9, "Watermelon", 6, 247),
	WHITEBERRIES(PatchType.BUSH, "Whiteberry", 59, "Whiteberry seed", 9, "Mushroom", 8, 239),
	POISON_IVY(PatchType.BUSH, "Poison ivy", 70, "Poison ivy seed", 9, null, 0, 6018), // immune to disease

	// Hops: 4 seeds (jute 3).
	BARLEY(PatchType.HOPS, "Barley", 3, "Barley seed", 5, "Compost", 3, 6006),
	HAMMERSTONE(PatchType.HOPS, "Hammerstone", 4, "Hammerstone seed", 5, "Marigolds", 1, 5994),
	// Flowers: 5-minute ticks, 1 seed, can't be protected (OSRS Wiki). Nothing to pay.
	MARIGOLD(PatchType.FLOWER, "Marigold", 2, "Marigold seed", 5, null, 0, 6010),
	ROSEMARY(PatchType.FLOWER, "Rosemary", 11, "Rosemary seed", 5, null, 0, 6014),
	NASTURTIUM(PatchType.FLOWER, "Nasturtium", 24, "Nasturtium seed", 5, null, 0, 6012),
	WOAD(PatchType.FLOWER, "Woad", 25, "Woad seed", 5, null, 0, 1793),
	LIMPWURT(PatchType.FLOWER, "Limpwurt", 26, "Limpwurt seed", 5, null, 0, 225),
	WHITE_LILY(PatchType.FLOWER, "White lily", 58, "White lily seed", 5, null, 0, 22932),

	ASGARNIAN(PatchType.HOPS, "Asgarnian", 8, "Asgarnian seed", 6, "Onions(10)", 1, 5996),
	JUTE(PatchType.HOPS, "Jute", 13, "Jute seed", 6, "Barley malt", 6, 5931),
	YANILLIAN(PatchType.HOPS, "Yanillian", 16, "Yanillian seed", 7, "Tomatoes(5)", 1, 5998),
	KRANDORIAN(PatchType.HOPS, "Krandorian", 21, "Krandorian seed", 8, "Cabbages(10)", 3, 6000),
	WILDBLOOD(PatchType.HOPS, "Wildblood", 28, "Wildblood seed", 9, "Nasturtiums", 1, 6002),

	// Cactus (potato cactus grows every 10 minutes, not 80).
	CACTUS(PatchType.CACTUS, "Cactus", 55, "Cactus seed", 8, "Cadava berries", 6, 6016),
	POTATO_CACTUS(PatchType.CACTUS, "Potato cactus", 64, "Potato cactus seed", 8, "Snape grass", 8, 3138),

	// Special trees.
	CALQUAT(PatchType.CALQUAT, "Calquat", 72, "Calquat sapling", 9, "Poison ivy berries", 8, 5980),
	SPIRIT_TREE(PatchType.SPIRIT_TREE, "Spirit tree", 83, "Spirit sapling", 13, "Monkey nuts", 5, 6063),
	CELASTRUS(PatchType.CELASTRUS, "Celastrus", 85, "Celastrus sapling", 6, "Potato cactus", 8, 22935),
	REDWOOD(PatchType.REDWOOD, "Redwood", 90, "Redwood sapling", 11, "Dragonfruit", 6, 19669);

	private final PatchType type;
	private final String displayName;
	private final int level;
	private final String seedName;
	private final int stages;
	private final String paymentItem;
	private final int paymentQuantity;
	/** Item id of the produce, used as the crop's picture (game item ids, as in RuneLite's ItemID). */
	private final int iconItemId;

	Crop(PatchType type, String displayName, int level, String seedName, int stages,
		String paymentItem, int paymentQuantity, int iconItemId)
	{
		this.iconItemId = iconItemId;
		this.type = type;
		this.displayName = displayName;
		this.level = level;
		this.seedName = seedName;
		this.stages = stages;
		this.paymentItem = paymentItem;
		this.paymentQuantity = paymentQuantity;
	}

	/** Allotments take 3 seeds, hops 4 (jute 3); everything else 1 seed or sapling. */
	public int getSeedsPerPatch()
	{
		if (type == PatchType.HOPS)
		{
			return this == JUTE ? 3 : 4;
		}
		return type == PatchType.ALLOTMENT ? 3 : 1;
	}

	/**
	 * Other things the gardener wants on top of {@link #getPaymentItem()}: name and quantity
	 * pairs. Spirit trees: 5 monkey nuts, plus 1 monkey bar and 1 ground tooth (OSRS Wiki).
	 */
	public String[][] getExtraPayments()
	{
		if (this == SPIRIT_TREE)
		{
			return new String[][]{{"Monkey bar", "1"}, {"Ground tooth", "1"}};
		}
		return new String[0][];
	}

	/** Minutes between growth ticks: the patch type's, except potato cactus (10, per RuneLite). */
	public int getTickMinutes()
	{
		return this == POTATO_CACTUS ? 10 : type.getTickMinutes();
	}

	public PatchType getType()
	{
		return type;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getLevel()
	{
		return level;
	}

	/** Herb seed or tree sapling name, as it appears in the bank. */
	public String getSeedName()
	{
		return seedName;
	}

	/** Growth stages including the finished one. */
	public int getStages()
	{
		return stages;
	}

	public int getGrowthMinutes()
	{
		return (stages - 1) * getTickMinutes();
	}

	public int getIconItemId()
	{
		return iconItemId;
	}

	/** Tree seed (before potting), or null for herbs. From the OSRS Wiki. */
	public String getTreeSeedName()
	{
		switch (this)
		{
			case OAK:
				return "Acorn";
			case WILLOW:
				return "Willow seed";
			case MAPLE:
				return "Maple seed";
			case YEW:
				return "Yew seed";
			case MAGIC:
				return "Magic seed";
			case APPLE:
				return "Apple tree seed";
			case BANANA:
				return "Banana tree seed";
			case ORANGE:
				return "Orange tree seed";
			case CURRY:
				return "Curry tree seed";
			case PINEAPPLE:
				return "Pineapple seed";
			case PAPAYA:
				return "Papaya tree seed";
			case PALM:
				return "Palm tree seed";
			case DRAGONFRUIT:
				return "Dragonfruit tree seed";
			case TEAK:
				return "Teak seed";
			case MAHOGANY:
				return "Mahogany seed";
			case CAMPHOR:
				return "Camphor seed";
			case IRONWOOD:
				return "Ironwood seed";
			case ROSEWOOD:
				return "Rosewood seed";
			case CALQUAT:
				return "Calquat tree seed";
			case SPIRIT_TREE:
				return "Spirit seed";
			case CELASTRUS:
				return "Celastrus seed";
			case REDWOOD:
				return "Redwood tree seed";
			default:
				return null;
		}
	}

	/** Seedling in a plant pot before watering, e.g. "Maple seedling". Null for seeds planted directly. */
	public String getSeedlingName()
	{
		if (!type.usesSaplings())
		{
			return null;
		}
		return this == SPIRIT_TREE ? "Spirit seedling" : displayName + " seedling";
	}

	/** Watered seedling, growing into a sapling, e.g. "Maple seedling (w)". Null for seeds planted directly. */
	public String getWateredSeedlingName()
	{
		String s = getSeedlingName();
		return s == null ? null : s + " (w)";
	}

	public String getPaymentItem()
	{
		return paymentItem;
	}

	public int getPaymentQuantity()
	{
		return paymentQuantity;
	}

	/** Crop by its patch-states.tsv name, or null for weeds/crops the plugin doesn't plant. */
	public static Crop fromTableName(String name)
	{
		try
		{
			return valueOf(name);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	/**
	 * The crop whose seed/sapling appears in a chat message like "You plant a Ranarr seed...".
	 * Picks the longest match, so "pineapple sapling" isn't read as "apple sapling".
	 */
	public static Crop fromPlantMessage(String message)
	{
		String lower = message.toLowerCase();
		if (!lower.contains("you plant"))
		{
			return null;
		}
		Crop best = null;
		for (Crop crop : values())
		{
			String seed = crop.seedName.toLowerCase();
			if (lower.contains(seed) && (best == null || seed.length() > best.seedName.length()))
			{
				best = crop;
			}
		}
		return best;
	}
}
