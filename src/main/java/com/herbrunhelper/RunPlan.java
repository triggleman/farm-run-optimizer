package com.herbrunhelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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

	public static final class Withdrawal
	{
		public final String itemName;
		public final int quantity;
		public final String note;

		Withdrawal(String itemName, int quantity, String note)
		{
			this.itemName = itemName;
			this.quantity = quantity;
			this.note = note;
		}
	}

	/** A bank visit in the middle of the run because everything won't fit in one inventory. */
	public static final class BankStop
	{
		/** Index into route: the bank comes just before this stop. */
		public final int beforeStep;
		public final String bank;
		public final List<Withdrawal> withdrawals = new ArrayList<>();

		BankStop(int beforeStep, String bank)
		{
			this.beforeStep = beforeStep;
			this.bank = bank;
		}
	}

	public int farmingLevel;
	public boolean bankKnown;
	/** True when the stops follow the player's own order (up / down buttons). */
	public boolean customOrder;
	/** Best crop you can plant, per patch type. */
	public final Map<PatchType, Crop> best = new EnumMap<>(PatchType.class);
	/** Next crop you'll unlock, per patch type. */
	public final Map<PatchType, Crop> next = new EnumMap<>(PatchType.class);
	public final List<FarmPatch> lockedPatches = new ArrayList<>();
	/** Every patch your quests/levels unlock, whether or not its run is on. */
	public final java.util.Set<FarmPatch> unlocked = java.util.EnumSet.noneOf(FarmPatch.class);
	/** Unlocked patches you've unticked. */
	public final List<FarmPatch> skippedPatches = new ArrayList<>();
	public final List<RouteStep> route = new ArrayList<>();
	/** What to take from the bank at the start. */
	public final List<Withdrawal> withdrawals = new ArrayList<>();
	public final List<BankStop> bankStops = new ArrayList<>();
	public final List<String> warnings = new ArrayList<>();
	/** Friendly reminders, e.g. pot your tree seeds for later runs. */
	public final List<String> tips = new ArrayList<>();
	/** Availability of every teleport, for the Travel section. */
	public final Map<Teleport, Teleport.Availability> availability = new EnumMap<>(Teleport.class);
	/** Inventory slots the first leg needs (for the "fits" line). */
	public int slotsNeeded;
	/** About how much Farming XP the run gives (harvests / health checks, planting, compost). */
	public double estimatedXp;
}
