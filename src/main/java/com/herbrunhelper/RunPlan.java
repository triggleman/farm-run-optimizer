package com.herbrunhelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The result of planning: what's unlocked, the route, what to bring, and any bank stops. */
public final class RunPlan
{
	public static final class RouteStep
	{
		public final FarmPatch patch;
		/** The teleport used, or null for a walk / nothing available. */
		public final Teleport teleport;
		public final String teleportLabel;
		/** Exact item name to bring, or null for a spell / walk / no teleport found. */
		public final String teleportItem;
		public final boolean hasTeleport;
		/** For walks: the patch this walk starts from. */
		public FarmPatch anchor;

		/** What to plant here, or null if you're out of seeds/saplings. */
		public Crop crop;
		/** Planted already on this run: still shows compost/payment, but nothing to bring for it. */
		public boolean done;
		/** Compost to use here (an item name, or a bottomless bucket), or null. */
		public String compost;
		/** Gardener payment for this stop (trees), or null. */
		public String paymentItem;
		public int paymentQuantity;
		/** Coins for the gardener to clear a grown tree. */
		public int coins;

		RouteStep(FarmPatch patch, Teleport teleport, String teleportLabel, String teleportItem, boolean hasTeleport)
		{
			this.patch = patch;
			this.teleport = teleport;
			this.teleportLabel = teleportLabel;
			this.teleportItem = teleportItem;
			this.hasTeleport = hasTeleport;
		}
	}

	public enum Group
	{
		TRAVEL("Travel"),
		RUNES("Runes"),
		SEEDS("Seeds & saplings"),
		PAYMENTS("Payments"),
		TOOLS("Tools & compost");

		private final String label;

		Group(String label)
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}
	}

	public static final class Withdrawal
	{
		public final String itemName;
		/** The total needed for the leg/run. */
		public final int quantity;
		/** How much is still to withdraw after what you already carry (rune pouch included). */
		public final int toWithdraw;
		public final String note;
		/** Inventory slots this line takes. Worn items and runes inside a rune pouch take none. */
		public final int slots;
		/** Which section of the bank's Farm run view it belongs to. */
		public final Group group;
		/** True when the item is normally carried noted (payments, compost, plant cure). */
		public final boolean noted;

		Withdrawal(String itemName, int quantity, String note)
		{
			this(itemName, quantity, note, 1);
		}

		Withdrawal(String itemName, int quantity, String note, int slots)
		{
			this(itemName, quantity, quantity, note, slots);
		}

		Withdrawal(String itemName, int quantity, int toWithdraw, String note, int slots)
		{
			this(itemName, quantity, toWithdraw, note, slots, inferGroup(itemName, note),
				inferNoted(itemName, note));
		}

		Withdrawal(String itemName, int quantity, int toWithdraw, String note, int slots,
			Group group, boolean noted)
		{
			this.itemName = itemName;
			this.quantity = quantity;
			this.toWithdraw = Math.max(0, toWithdraw);
			this.note = note;
			this.slots = Math.max(0, slots);
			this.group = group;
			this.noted = noted;
		}

		private static Group inferGroup(String itemName, String note)
		{
			String name = itemName == null ? "" : itemName.toLowerCase();
			String text = note == null ? "" : note.toLowerCase();
			if (name.endsWith("rune") || name.contains("rune pouch") || name.endsWith(" pouch"))
			{
				return Group.RUNES;
			}
			if (name.contains("seed") || name.contains("sapling") || name.contains("seedling"))
			{
				return Group.SEEDS;
			}
			if (text.contains("payment") || text.contains("pay the") || name.equals("coins"))
			{
				return Group.PAYMENTS;
			}
			if (name.contains("compost") || name.contains("plant cure") || name.contains("secateurs")
				|| name.equals("spade") || name.equals("rake") || name.equals("seed dibber")
				|| name.contains("bottomless"))
			{
				return Group.TOOLS;
			}
			return Group.TRAVEL;
		}

		private static boolean inferNoted(String itemName, String note)
		{
			String name = itemName == null ? "" : itemName.toLowerCase();
			String text = note == null ? "" : note.toLowerCase();
			if (name.equals("coins"))
			{
				return false; // coins stack, they aren't noted
			}
			return text.contains("noted") || text.contains("payment")
				|| name.contains("compost") || name.contains("plant cure");
		}
	}

	/** A bank visit in the middle of the run because everything won't fit in one inventory. */
	public static final class BankStop
	{
		/** Index into route: the bank comes just before this stop. */
		public final int beforeStep;
		public final String bank;
		/** A tile inside the bank, or -1 when the name isn't known. */
		public final int bankX;
		public final int bankY;
		public final int bankPlane;
		public final List<Withdrawal> withdrawals = new ArrayList<>();

		BankStop(int beforeStep, String bank)
		{
			this.beforeStep = beforeStep;
			this.bank = bank;
			int[] tile = Banks.tile(bank);
			this.bankX = tile == null ? -1 : tile[0];
			this.bankY = tile == null ? -1 : tile[1];
			this.bankPlane = tile == null ? -1 : tile[2];
		}
	}

	public int farmingLevel;
	public boolean bankKnown;
	/** Best crop you can plant, per patch type. */
	public final Map<PatchType, Crop> best = new EnumMap<>(PatchType.class);
	/** Next crop you'll unlock, per patch type. */
	public final Map<PatchType, Crop> next = new EnumMap<>(PatchType.class);
	public final List<FarmPatch> lockedPatches = new ArrayList<>();
	/** Every patch your quests/levels unlock, whether or not its run is on. */
	public final Set<FarmPatch> unlocked = EnumSet.noneOf(FarmPatch.class);
	/** Unlocked patches you've unticked. */
	public final List<FarmPatch> skippedPatches = new ArrayList<>();
	public final List<RouteStep> route = new ArrayList<>();
	/** What to take from the bank at the start. */
	public final List<Withdrawal> withdrawals = new ArrayList<>();
	/** Useful extras shown in the bank view but not required (the rune pouch when already carried). */
	public final List<Withdrawal> optionalItems = new ArrayList<>();
	public final List<BankStop> bankStops = new ArrayList<>();
	public final List<String> warnings = new ArrayList<>();
	/** Friendly reminders, e.g. pot your tree seeds for later runs. */
	public final List<String> tips = new ArrayList<>();
	/** What covers the spell runes (staff, tome, rune pouch, combination runes). */
	public final List<String> runeNotes = new ArrayList<>();
	public String runeSummary = "";
	/** Availability of every teleport, for the Travel section. */
	public final Map<Teleport, Teleport.Availability> availability = new EnumMap<>(Teleport.class);
	/** Inventory slots the first leg needs (for the "fits" line). */
	public int slotsNeeded;
	/** About how much Farming XP the run gives (harvests / health checks, planting, compost). */
	public double estimatedXp;
}
