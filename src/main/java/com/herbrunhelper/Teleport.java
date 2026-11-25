package com.herbrunhelper;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Every way of getting to a patch. Pure Java (no RuneLite) so availability can be tested.
 *
 * iconItemId is only used for the picture in the sidebar (-1 = show text instead).
 * Items are matched by name prefix, so every charge variant counts.
 */
public enum Teleport
{
	// ---- Jewellery and other items
	EXPLORERS_RING(Kind.ITEM, "Explorer's ring 2+", 13126, 0, null, null, null,
		"Explorer's ring 2", "Explorer's ring 3", "Explorer's ring 4"),
	// Only cloak 2+ has the teleport to the farming patch (cloak 1 goes to the monastery).
	ARDOUGNE_CLOAK(Kind.ITEM, "Ardougne cloak 2+", 13122, 0, null, null, null,
		"Ardougne cloak 2", "Ardougne cloak 3", "Ardougne cloak 4"),
	// Every Ardougne cloak (1 to 4, and the Ardougne max cape) teleports to Kandarin Monastery,
	// right beside the Ardougne bush patch.
	ARDOUGNE_CLOAK_MONASTERY(Kind.ITEM, "Ardougne cloak (Kandarin Monastery)", 13121, 0, null, null, null,
		"Ardougne cloak 1", "Ardougne cloak 2", "Ardougne cloak 3", "Ardougne cloak 4", "Ardougne max cape"),
	ECTOPHIAL(Kind.ITEM, "Ectophial", 4251, 0, null, null, null, "Ectophial"),
	XERICS_TALISMAN(Kind.ITEM, "Xeric's talisman", 13393, 0, null, null, null, "Xeric's talisman"),
	KHAREDSTS_MEMOIRS(Kind.ITEM, "Kharedst's memoirs / Book of the dead", 21760, 0, null, null, null,
		"Kharedst's memoirs", "Book of the dead"),
	SKILLS_NECKLACE(Kind.ITEM, "Skills necklace", 11105, 0, null, null, null, "Skills necklace("),
	FARMING_CAPE(Kind.ITEM, "Farming cape", 9810, 0, null, null, null, "Farming cape"),
	STONY_BASALT(Kind.ITEM, "Stony basalt", 22601, 0, null, null, null, "Stony basalt"),
	ICY_BASALT(Kind.ITEM, "Icy basalt", 22599, 0, null, null, null, "Icy basalt"),
	ROYAL_SEED_POD(Kind.ITEM, "Royal seed pod", 19564, 0, null, null, null, "Royal seed pod"),
	QUETZAL_WHISTLE(Kind.ITEM, "Quetzal whistle (Hunter Guild)", 29271, 0, null, null, null,
		"Basic quetzal whistle", "Enhanced quetzal whistle", "Perfected quetzal whistle"),
	HUNTER_CAPE(Kind.ITEM, "Hunter cape (Hunter Guild)", 9948, 0, null, null, null, "Hunter cape"),
	PENDANT_OF_ATES(Kind.ITEM, "Pendant of ates", 29893, 0, null, null, null, "Pendant of ates"),
	TELEPORT_CRYSTAL(Kind.ITEM, "Teleport crystal", 6099, 0, null, null, null, "Teleport crystal"),
	AMULET_OF_GLORY(Kind.ITEM, "Amulet of glory (Al Kharid)", 1712, 0, null, null, null, "Amulet of glory("),
	RING_OF_WEALTH(Kind.ITEM, "Ring of wealth (Miscellania)", 11980, 0, null, null, null, "Ring of wealth ("),
	DIGSITE_PENDANT(Kind.ITEM, "Digsite pendant", 11194, 0, null, null, null, "Digsite pendant"),

	// ---- Teleport tablets
	VARROCK_TAB(Kind.TABLET, "Varrock teleport tablet", 8007, 0, null, null, null, "Varrock teleport"),
	LUMBRIDGE_TAB(Kind.TABLET, "Lumbridge teleport tablet", 8008, 0, null, null, null, "Lumbridge teleport"),
	FALADOR_TAB(Kind.TABLET, "Falador teleport tablet", 8009, 0, null, null, null, "Falador teleport"),
	CAMELOT_TAB(Kind.TABLET, "Camelot teleport tablet", 8010, 0, null, null, null, "Camelot teleport"),
	ARDOUGNE_TAB(Kind.TABLET, "Ardougne teleport tablet", 8011, 0, null, null, null, "Ardougne teleport"),
	TROLLHEIM_TAB(Kind.TABLET, "Trollheim teleport tablet", 11747, 0, null, null, null, "Trollheim teleport"),
	FENKENSTRAIN_TAB(Kind.TABLET, "Fenkenstrain's castle teleport", 19621, 0, null, null, null, "Fenkenstrain's castle teleport"),
	HARMONY_TAB(Kind.TABLET, "Harmony island teleport", 19625, 0, null, null, null, "Harmony island teleport"),
	BRIMHAVEN_TAB(Kind.TABLET, "Brimhaven teleport tablet", 11745, 0, null, null, null, "Brimhaven teleport"),

	// ---- Standard spellbook spells (runes are added to the bank list)
	VARROCK_SPELL(Kind.SPELL, "Varrock Teleport", -1, 25, null, null, "Law rune:1,Air rune:3,Fire rune:1"),
	LUMBRIDGE_SPELL(Kind.SPELL, "Lumbridge Teleport", -1, 31, null, null, "Law rune:1,Air rune:3,Earth rune:1"),
	FALADOR_SPELL(Kind.SPELL, "Falador Teleport", -1, 37, null, null, "Law rune:1,Air rune:3,Water rune:1"),
	CAMELOT_SPELL(Kind.SPELL, "Camelot Teleport", -1, 45, null, null, "Law rune:1,Air rune:5"),
	KOUREND_SPELL(Kind.SPELL, "Kourend Castle Teleport", -1, 48, "Client of Kourend", null,
		"Law rune:2,Water rune:1,Fire rune:1"),
	ARDOUGNE_SPELL(Kind.SPELL, "Ardougne Teleport", -1, 51, "Plague City", null, "Law rune:2,Water rune:2"),
	TROLLHEIM_SPELL(Kind.SPELL, "Trollheim Teleport", -1, 61, "Eadgar's Ruse", null, "Law rune:2,Fire rune:2"),
	CIVITAS_SPELL(Kind.SPELL, "Civitas illa Fortis Teleport", -1, 54, "Twilight's Promise", null,
		"Law rune:2,Earth rune:1,Fire rune:1"),

	// ---- Fairy rings
	FAIRY_BLR(Kind.FAIRY_RING, "Fairy ring BLR", -1, 0, null, "BLR", null),
	FAIRY_ALQ(Kind.FAIRY_RING, "Fairy ring ALQ", -1, 0, null, "ALQ", null),
	FAIRY_AKR(Kind.FAIRY_RING, "Fairy ring AKR", -1, 0, null, "AKR", null),
	FAIRY_CIR(Kind.FAIRY_RING, "Fairy ring CIR", -1, 0, null, "CIR", null),
	FAIRY_AJP(Kind.FAIRY_RING, "Fairy ring AJP", -1, 0, null, "AJP", null),
	FAIRY_AIS(Kind.FAIRY_RING, "Fairy ring AIS", -1, 0, null, "AIS", null),
	FAIRY_CIQ(Kind.FAIRY_RING, "Fairy ring CIQ", -1, 0, null, "CIQ", null),
	FAIRY_AIQ(Kind.FAIRY_RING, "Fairy ring AIQ", -1, 0, null, "AIQ", null),
	FAIRY_ALS(Kind.FAIRY_RING, "Fairy ring ALS", -1, 0, null, "ALS", null),
	FAIRY_CIP(Kind.FAIRY_RING, "Fairy ring CIP", -1, 0, null, "CIP", null),
	FAIRY_CKR(Kind.FAIRY_RING, "Fairy ring CKR", -1, 0, null, "CKR", null),

	// ---- Other
	SPIRIT_TREE(Kind.OTHER, "Spirit tree", 6063, 0, null, null, null);

	/** Items whose uncharged state is a separate, unusable item name. */
	private static final Set<Teleport> CHARGEABLE = EnumSet.of(
		XERICS_TALISMAN, SKILLS_NECKLACE, AMULET_OF_GLORY, RING_OF_WEALTH,
		DIGSITE_PENDANT, TELEPORT_CRYSTAL, PENDANT_OF_ATES);

	/** Items whose charged state is the plain item name, with "(inert)" for no charges. */
	private static final Set<Teleport> BASE_IS_CHARGED = EnumSet.of(
		XERICS_TALISMAN, PENDANT_OF_ATES);

	public enum Kind
	{
		ITEM("Jewellery & items"),
		TABLET("Tablets"),
		SPELL("Spells (standard spellbook)"),
		FAIRY_RING("Fairy rings"),
		OTHER("Other");

		private final String title;

		Kind(String title)
		{
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}
	}

	/** Whether the player can use a teleport right now, and why not. */
	public static final class Availability
	{
		public final boolean available;
		public final String reason;
		/** For items: the exact owned item name to bring. */
		public final String ownedItem;

		Availability(boolean available, String reason, String ownedItem)
		{
			this.available = available;
			this.reason = reason;
			this.ownedItem = ownedItem;
		}
	}

	/** Everything availability depends on. The plugin fills this from the game. */
	public static final class Context
	{
		public Map<String, Integer> owned = Collections.emptyMap();
		public int magicLevel = 1;
		public boolean standardSpellbook = true;
		public Set<String> completedQuests = Collections.emptySet();
		public boolean fairyRingsUnlocked;
		/** Lumbridge Elite diary lets you use fairy rings without a staff. */
		public boolean staffless;
		public Set<Teleport> disabled = EnumSet.noneOf(Teleport.class);
		/** Allow standard spellbook teleports (runes). Spells are used over tabs when both are on. */
		public boolean useSpells = true;
		/** Allow teleport tablets. */
		public boolean useTabs = true;
		/** Use fairy rings back-to-back once you're on the network. */
		public boolean chainFairyRings = true;
	}

	private final Kind kind;
	private final String label;
	private final int iconItemId;
	private final int magicLevel;
	private final String quest;
	private final String fairyCode;
	private final Map<String, Integer> runes;
	private final String[] itemPrefixes;

	Teleport(Kind kind, String label, int iconItemId, int magicLevel, String quest, String fairyCode,
		String runes, String... itemPrefixes)
	{
		this.kind = kind;
		this.label = label;
		this.iconItemId = iconItemId;
		this.magicLevel = magicLevel;
		this.quest = quest;
		this.fairyCode = fairyCode;
		this.runes = parseRunes(runes);
		this.itemPrefixes = itemPrefixes;
	}

	public Kind getKind()
	{
		return kind;
	}

	public String getLabel()
	{
		return label;
	}

	public int getIconItemId()
	{
		return iconItemId;
	}

	public String getFairyCode()
	{
		return fairyCode;
	}

	public Map<String, Integer> getRunes()
	{
		return runes;
	}

	/** Short text for a tile with no icon. */
	public String getShortLabel()
	{
		if (fairyCode != null)
		{
			return fairyCode;
		}
		String l = label.replace(" Teleport", "").replace(" teleport tablet", "").replace(" teleport", "");
		return l.length() > 9 ? l.substring(0, 9) : l;
	}

	public boolean matchesItem(String itemName)
	{
		String lower = itemName.toLowerCase();
		for (String prefix : itemPrefixes)
		{
			if (lower.startsWith(prefix.toLowerCase()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Charges written in the item's name, e.g. "Skills necklace(6)" or "Ring of wealth (5)".
	 * Uncharged names score 0, so a charged version in the bank wins over a 1-charge one.
	 */
	static int chargesOf(String itemName)
	{
		if (itemName == null)
		{
			return 0;
		}
		int open = itemName.lastIndexOf('(');
		int close = itemName.lastIndexOf(')');
		if (open < 0 || close <= open + 1)
		{
			return 0;
		}
		String inside = itemName.substring(open + 1, close).trim();
		try
		{
			return Integer.parseInt(inside);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	private static boolean isDepletedName(String name)
	{
		String lower = name.toLowerCase();
		return lower.contains("inert") || lower.contains("empty") || lower.contains("uncharged")
			|| lower.contains("(0)") || lower.endsWith(" 0");
	}

	public Availability check(Context ctx)
	{
		switch (kind)
		{
			case ITEM:
			case TABLET:
				String best = null;
				int bestCharges = -1;
				boolean needsCharges = CHARGEABLE.contains(this);
				for (Map.Entry<String, Integer> e : ctx.owned.entrySet())
				{
					if (e.getValue() <= 0 || !matchesItem(e.getKey()))
					{
						continue;
					}
					int charges = chargesOf(e.getKey());
					if (needsCharges && charges <= 0
						&& !(BASE_IS_CHARGED.contains(this) && !isDepletedName(e.getKey())))
					{
						// Inert/empty/uncharged variants have no teleport.
						continue;
					}
					if (charges > bestCharges)
					{
						bestCharges = charges;
						best = e.getKey();
					}
				}
				return best == null ? new Availability(false, "You don't own a charged one", null)
					: new Availability(true, "You own " + best, best);
			case SPELL:
				if (!ctx.standardSpellbook)
				{
					return new Availability(false, "Switch to the standard spellbook", null);
				}
				if (ctx.magicLevel < magicLevel)
				{
					return new Availability(false, "Needs " + magicLevel + " Magic", null);
				}
				if (quest != null && !ctx.completedQuests.contains(quest))
				{
					return new Availability(false, "Needs " + quest, null);
				}
				return new Availability(true, runes.isEmpty() ? "Check the runes in your spellbook" : "Level " + magicLevel + " Magic", null);
			case FAIRY_RING:
				if (!ctx.fairyRingsUnlocked)
				{
					return new Availability(false, "Start Fairytale II to unlock fairy rings", null);
				}
				if (!ctx.staffless && staffName(ctx.owned) == null)
				{
					return new Availability(false, "Needs a dramen or lunar staff", null);
				}
				return new Availability(true, "Code " + fairyCode, null);
			default:
				return new Availability(true, "", null);
		}
	}

	/** The staff to bring for fairy rings, or null if none owned. */
	public static String staffName(Map<String, Integer> owned)
	{
		for (String staff : new String[]{"Dramen staff", "Lunar staff"})
		{
			if (owned.getOrDefault(staff, 0) > 0)
			{
				return staff;
			}
		}
		return null;
	}

	private static Map<String, Integer> parseRunes(String spec)
	{
		Map<String, Integer> out = new LinkedHashMap<>();
		if (spec != null)
		{
			for (String part : spec.split(","))
			{
				String[] kv = part.split(":");
				out.put(kv[0], Integer.parseInt(kv[1]));
			}
		}
		return Collections.unmodifiableMap(out);
	}
}
