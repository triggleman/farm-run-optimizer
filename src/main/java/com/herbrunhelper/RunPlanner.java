package com.herbrunhelper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure planning logic with no RuneLite dependencies, so it can be unit tested on its own.
 *
 * Input: Farming level, which patches the player has unlocked, and every item they own
 * (bank + inventory + equipment) as exact item name -> quantity.
 */
public final class RunPlanner
{
	public static final class Options
	{
		public final Set<PatchType> runs = EnumSet.of(PatchType.HERB, PatchType.TREE);
		public boolean useCompost = true;
		public boolean bringRake = true;
		/**
		 * Patches known to have no weeds (something growing, a stump, or dead): no rake needed
		 * there. Patches not in here might be weedy, so a rake is brought if any are on the run.
		 */
		public Set<FarmPatch> weedFree = EnumSet.noneOf(FarmPatch.class);
		public boolean payGardeners = true;
		public boolean bankKnown = true;
		public int inventorySlots = 28;
		/** Crops you've unticked: never planted. */
		public Set<Crop> disabledCrops = EnumSet.noneOf(Crop.class);
		/** Patches you've unticked: skipped. */
		public Set<FarmPatch> disabledPatches = EnumSet.noneOf(FarmPatch.class);
		/** Patches with something of yours growing or ready: always visited, and get seeds first. */
		public Set<FarmPatch> usedPatches = EnumSet.noneOf(FarmPatch.class);
		/** Crops in the order to plant them (e.g. most profitable first); empty = highest level first. */
		public List<Crop> cropRank = new ArrayList<>();
		/** Farming Guild contract crop to plant in the guild patch, or null. */
		public Crop contractCrop;
		/** Patches known to be diseased (bring plant cure / secateurs). */
		public Set<FarmPatch> diseased = EnumSet.noneOf(FarmPatch.class);
		/** Only plan the patches that are ready (or were just done this run). */
		public boolean onlyReady = false;
		/** Finish one run (herbs, trees...) before starting the next, instead of mixing stops. */
		public boolean keepRunsSeparate = true;
		/**
		 * The player's own stop order (moved with the up / down buttons). Empty = automatic.
		 * Stops not in the list keep their automatic place, after the listed ones.
		 */
		public List<FarmPatch> customOrder = new ArrayList<>();
		/** Patches with something still growing: millis until ready, or -1 if unknown. */
		public Map<FarmPatch, Long> notReady = new java.util.EnumMap<>(FarmPatch.class);
		/** Patches you've just harvested/planted on this run, kept on the list as done. */
		public Set<FarmPatch> doneThisRun = EnumSet.noneOf(FarmPatch.class);
		/** What was just planted in each done patch (so compost / payment steps still show). */
		public Map<FarmPatch, Crop> plantedCrops = new java.util.EnumMap<>(FarmPatch.class);
		/** What's growing in each patch now (for the XP estimate of what you'll harvest). */
		public Map<FarmPatch, Crop> growingCrops = new java.util.EnumMap<>(FarmPatch.class);
		/** Average herbs per patch, for the XP estimate. */
		public double herbYield = HerbPrices.DEFAULT_YIELD;
		/** Levels, quests, spellbook, disabled teleports, spells/tabs, fairy chaining. */
		public Teleport.Context teleports = new Teleport.Context();
	}

	/** Coins the gardener charges to clear a fully grown tree so you can replant. */
	public static final int TREE_REMOVAL_COINS = 200;

	private static final String[] COMPOST_BEST_FIRST = {"Ultracompost", "Supercompost", "Compost"};

	/** Items you can wear instead of carrying, by equipment slot (first owned in a slot is worn). */
	private static final String[][] WEAR_SLOTS = {
		{"Magic secateurs", "Dramen staff", "Lunar staff"},
		{"Skills necklace", "Digsite pendant", "Pendant of ates", "Xeric's talisman"},
		{"Farming cape", "Ardougne cloak", "Hunter cape", "Construct. cape", "Max cape"},
		{"Explorer's ring"},
	};

	private RunPlanner()
	{
	}

	public static RunPlan plan(int farmingLevel, Set<FarmPatch> unlockedPatches,
		Map<String, Integer> owned, Options options)
	{
		RunPlan plan = new RunPlan();
		plan.farmingLevel = farmingLevel;
		plan.bankKnown = options.bankKnown;
		plan.unlocked.addAll(unlockedPatches);

		Teleport.Context ctx = options.teleports;
		ctx.owned = owned;
		for (Teleport t : Teleport.values())
		{
			plan.availability.put(t, t.check(ctx));
		}

		for (Crop crop : Crop.values())
		{
			if (crop.getLevel() <= farmingLevel)
			{
				plan.best.put(crop.getType(), crop);
			}
			else if (!plan.next.containsKey(crop.getType()))
			{
				plan.next.put(crop.getType(), crop);
			}
		}

		Set<PatchType> doing = EnumSet.noneOf(PatchType.class);
		for (PatchType t : options.runs)
		{
			if (plan.best.containsKey(t))
			{
				doing.add(t);
			}
			else
			{
				Crop first = plan.next.get(t);
				plan.warnings.add(t.getDisplayName() + " need level " + first.getLevel() + " Farming ("
					+ first.getDisplayName() + ").");
			}
		}

		Set<FarmPatch> included = EnumSet.noneOf(FarmPatch.class);
		for (FarmPatch patch : FarmPatch.values())
		{
			if (!doing.contains(patch.getType()))
			{
				continue;
			}
			if (!unlockedPatches.contains(patch))
			{
				plan.lockedPatches.add(patch);
				continue;
			}
			if (options.disabledPatches.contains(patch))
			{
				plan.skippedPatches.add(patch);
				continue;
			}
			included.add(patch);
		}

		if (options.onlyReady)
		{
			keepReadyPatches(plan, included, options);
		}

		// Decide what goes in each patch first: seeds go to the patches you use, then the rest.
		// A patch with nothing to plant and nothing of yours growing is left off the run.
		Map<FarmPatch, Crop> crops = new java.util.EnumMap<>(FarmPatch.class);
		for (PatchType t : doing)
		{
			allocateCrops(plan, t, included, owned, options, crops);
		}
		// Drop patches with nothing to plant, unless there's still something to do there:
		// harvesting herbs, checking a grown tree's health (XP), or curing a diseased tree.
		included.removeIf(p -> !crops.containsKey(p)
			&& (!options.usedPatches.contains(p)
				|| (p.getType().isTreeLike() && options.notReady.containsKey(p)
					&& !options.doneThisRun.contains(p) && !options.diseased.contains(p))));
		for (PatchType t : doing)
		{
			if (t.usesSaplings())
			{
				addSeedlingTips(plan, t, owned, options);
			}
		}

		List<RunPlan.RouteStep> auto = new ArrayList<>();
		if (options.keepRunsSeparate)
		{
			// One run at a time, in the panel's order; walks only link stops of the same run.
			for (PatchType t : PatchType.values())
			{
				Set<FarmPatch> mine = EnumSet.noneOf(FarmPatch.class);
				for (FarmPatch p : included)
				{
					if (p.getType() == t)
					{
						mine.add(p);
					}
				}
				List<RunPlan.RouteStep> picked = new ArrayList<>();
				for (FarmPatch patch : mine)
				{
					picked.add(pickTeleport(patch, plan, ctx, mine));
				}
				auto.addAll(orderRoute(picked, ctx));
			}
		}
		else
		{
			List<RunPlan.RouteStep> picked = new ArrayList<>();
			for (FarmPatch patch : included)
			{
				picked.add(pickTeleport(patch, plan, ctx, included));
			}
			auto.addAll(orderRoute(picked, ctx));
		}
		plan.route.addAll(options.customOrder.isEmpty() ? auto : customRoute(auto, plan, ctx, options.customOrder));
		plan.customOrder = !options.customOrder.isEmpty();

		if (plan.route.isEmpty())
		{
			if (plan.warnings.isEmpty())
			{
				plan.warnings.add("No patches to visit.");
			}
			return plan;
		}

		if (!options.bankKnown)
		{
			plan.warnings.add("Open your bank once so the plugin can see what you own.");
		}

		for (RunPlan.RouteStep step : plan.route)
		{
			step.crop = crops.get(step.patch);
			step.done = options.doneThisRun.contains(step.patch);
			if (step.crop != null && step.patch.getType().hasGardenerPayment() && options.payGardeners
				&& step.crop.getPaymentItem() != null)
			{
				step.paymentItem = step.crop.getPaymentItem();
				step.paymentQuantity = step.crop.getPaymentQuantity();
			}
		}
		paymentWarnings(plan, owned);
		String bottomless = assignCompost(plan, owned, options);
		for (RunPlan.RouteStep s : plan.route)
		{
			// Only pay to clear a tree you're replacing; with no sapling, leave it standing.
			if (s.patch.getType().getClear() == PatchType.Clear.GARDENER && s.crop != null
				&& !options.doneThisRun.contains(s.patch))
			{
				s.coins = TREE_REMOVAL_COINS;
			}
		}

		List<RunPlan.Withdrawal> fixed = fixedItems(plan, owned, options, bottomless,
			doing.contains(PatchType.HERB) || doing.contains(PatchType.ALLOTMENT) || doing.contains(PatchType.FLOWER));
		plan.estimatedXp = estimateXp(plan, options);
		splitIntoTrips(plan, fixed, options);
		return plan;
	}

	private static boolean usable(FarmPatch.Route r, RunPlan plan, Teleport.Context ctx, Set<FarmPatch> included)
	{
		if (r.teleport == null)
		{
			// A walk only works if the patch it starts from is on this run.
			return r.anchor == null || included.contains(r.anchorPatch());
		}
		return r.houseOk(ctx.houseLocation)
			&& plan.availability.get(r.teleport).available && !ctx.disabled.contains(r.teleport);
	}

	private static RunPlan.RouteStep pickTeleport(FarmPatch patch, RunPlan plan, Teleport.Context ctx,
		Set<FarmPatch> included)
	{
		List<FarmPatch.Route> usable = new ArrayList<>();
		for (FarmPatch.Route r : patch.getRoutes())
		{
			if (usable(r, plan, ctx, included))
			{
				usable.add(r);
			}
		}

		// Spells / tabs toggles. When both are allowed and a spell works here, skip the tab.
		boolean spellHere = false;
		List<FarmPatch.Route> filtered = new ArrayList<>();
		for (FarmPatch.Route r : usable)
		{
			Teleport.Kind k = r.teleport == null ? null : r.teleport.getKind();
			if ((k == Teleport.Kind.SPELL && !ctx.useSpells) || (k == Teleport.Kind.TABLET && !ctx.useTabs))
			{
				continue;
			}
			spellHere |= k == Teleport.Kind.SPELL;
			filtered.add(r);
		}
		usable = new ArrayList<>();
		for (FarmPatch.Route r : filtered)
		{
			if (!(spellHere && r.teleport != null && r.teleport.getKind() == Teleport.Kind.TABLET))
			{
				usable.add(r);
			}
		}

		FarmPatch.Route chosen = null;

		// A walk from the patch you just did always wins (you're already there).
		for (FarmPatch.Route r : usable)
		{
			if (r.teleport == null && r.anchor != null)
			{
				chosen = r;
				break;
			}
		}

		if (chosen == null && ctx.chainFairyRings)
		{
			FarmPatch.Route fairy = null;
			boolean beaten = false;
			for (FarmPatch.Route r : usable)
			{
				if (r.beatsFairy)
				{
					beaten = true;
					break;
				}
				if (r.teleport != null && r.teleport.getKind() == Teleport.Kind.FAIRY_RING)
				{
					fairy = r;
					break;
				}
			}
			if (!beaten && fairy != null)
			{
				chosen = fairy;
			}
		}

		if (chosen == null && !usable.isEmpty())
		{
			chosen = usable.get(0);
		}

		if (chosen == null)
		{
			// Suggest a normal teleport, not a house portal you may not have.
			FarmPatch.Route best = patch.getRoutes().get(0);
			for (FarmPatch.Route r : patch.getRoutes())
			{
				if (r.house == null)
				{
					best = r;
					break;
				}
			}
			return new RunPlan.RouteStep(patch, null, "No teleport available (best: " + best.describe() + ")", null, false);
		}
		String item = null;
		if (chosen.teleport != null && (chosen.teleport.getKind() == Teleport.Kind.ITEM
			|| chosen.teleport.getKind() == Teleport.Kind.TABLET))
		{
			item = plan.availability.get(chosen.teleport).ownedItem;
		}
		RunPlan.RouteStep step = new RunPlan.RouteStep(patch, chosen.teleport, chosen.describe(), item, true);
		step.anchor = chosen.teleport == null ? chosen.anchorPatch() : null;
		return step;
	}

	private static boolean isFairy(RunPlan.RouteStep s)
	{
		return s.teleport != null && s.teleport.getKind() == Teleport.Kind.FAIRY_RING;
	}

	/**
	 * Keeps the base order, but pulls every fairy-ring stop together into one chain that starts
	 * right after a patch with a fairy ring nearby (e.g. Ardougne cloak, then walk to BLR).
	 * Walk stops stay glued to the stop before them. Fairy directions say which code to dial.
	 */
	static List<RunPlan.RouteStep> orderRoute(List<RunPlan.RouteStep> steps, Teleport.Context ctx)
	{
		// Group into blocks: a stop plus any walks that follow it.
		List<List<RunPlan.RouteStep>> blocks = new ArrayList<>();
		for (RunPlan.RouteStep s : steps)
		{
			List<RunPlan.RouteStep> home = s.anchor == null ? null : blockWith(blocks, s.anchor);
			if (home != null)
			{
				home.add(s);
			}
			else
			{
				List<RunPlan.RouteStep> b = new ArrayList<>();
				b.add(s);
				blocks.add(b);
			}
		}

		List<List<RunPlan.RouteStep>> fairy = new ArrayList<>();
		List<List<RunPlan.RouteStep>> other = new ArrayList<>();
		for (List<RunPlan.RouteStep> b : blocks)
		{
			(isFairy(b.get(0)) ? fairy : other).add(b);
		}

		List<List<RunPlan.RouteStep>> ordered = new ArrayList<>();
		String startRing = null;
		if (fairy.isEmpty())
		{
			ordered.addAll(other);
		}
		else
		{
			// Start the chain from the patch whose ring is the shortest walk away.
			int starter = -1;
			int bestRank = Integer.MAX_VALUE;
			for (int i = 0; i < other.size(); i++)
			{
				RunPlan.RouteStep head = other.get(i).get(0);
				String ring = head.patch.getNearbyRing();
				if (head.hasTeleport && ring != null && ringWalkRank(ring) < bestRank)
				{
					starter = i;
					bestRank = ringWalkRank(ring);
				}
			}
			if (starter < 0)
			{
				ordered.addAll(fairy);
				ordered.addAll(other);
			}
			else
			{
				ordered.addAll(other.subList(0, starter + 1));
				ordered.addAll(fairy);
				ordered.addAll(other.subList(starter + 1, other.size()));
				startRing = other.get(starter).get(0).patch.getNearbyRing();
			}
		}

		List<RunPlan.RouteStep> flat = new ArrayList<>();
		for (List<RunPlan.RouteStep> b : ordered)
		{
			flat.addAll(b);
		}
		return fairyDirections(flat, startRing != null);
	}

	/**
	 * Your own order: stops you've placed come first in that order, the rest follow in their
	 * automatic order. Travel is picked again for each stop from where you're coming from, so a
	 * "walk from the next patch" is only used when that patch really is the stop before.
	 */
	static List<RunPlan.RouteStep> customRoute(List<RunPlan.RouteStep> auto, RunPlan plan, Teleport.Context ctx,
		List<FarmPatch> custom)
	{
		List<FarmPatch> order = new ArrayList<>();
		Set<FarmPatch> onRun = EnumSet.noneOf(FarmPatch.class);
		for (RunPlan.RouteStep s : auto)
		{
			onRun.add(s.patch);
		}
		for (FarmPatch p : custom)
		{
			if (onRun.contains(p) && !order.contains(p))
			{
				order.add(p);
			}
		}
		for (RunPlan.RouteStep s : auto)
		{
			if (!order.contains(s.patch))
			{
				order.add(s.patch);
			}
		}
		Map<FarmPatch, Crop> crops = new java.util.EnumMap<>(FarmPatch.class);
		for (RunPlan.RouteStep s : auto)
		{
			if (s.crop != null)
			{
				crops.put(s.patch, s.crop);
			}
		}
		List<RunPlan.RouteStep> out = new ArrayList<>();
		FarmPatch prev = null;
		for (FarmPatch p : order)
		{
			Set<FarmPatch> from = prev == null ? EnumSet.noneOf(FarmPatch.class) : EnumSet.of(prev);
			RunPlan.RouteStep step = pickTeleport(p, plan, ctx, from);
			if ((step.teleport != null || !step.hasTeleport) && prev != null && nextTo(prev, p))
			{
				// Same farm as the stop before (herb, then flower...): just walk over.
				step = new RunPlan.RouteStep(p, null, "Walk over from the " + prev.getDisplayName(), null, true);
				step.anchor = prev;
			}
			step.crop = crops.get(p);
			out.add(step);
			prev = p;
		}
		return fairyDirections(out, true);
	}

	/** Close enough to walk between (the patches of one farm, a guild tier). */
	static boolean nextTo(FarmPatch a, FarmPatch b)
	{
		return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())) <= 20;
	}

	/** Rewrite fairy ring directions based on where you're coming from. */
	private static List<RunPlan.RouteStep> fairyDirections(List<RunPlan.RouteStep> ordered, boolean walkToRing)
	{
		List<RunPlan.RouteStep> out = new ArrayList<>();
		RunPlan.RouteStep prev = null;
		{
			for (RunPlan.RouteStep s : ordered)
			{
				RunPlan.RouteStep step = s;
				if (isFairy(s))
				{
					String code = s.teleport.getFairyCode();
					String rest = s.teleportLabel.substring(s.teleport.getLabel().length());
					String from;
					if (prev != null && isFairy(prev))
					{
						from = "Back to the ring, dial " + code;
					}
					else if (prev != null && walkToRing && prev.patch.getNearbyRing() != null)
					{
						from = "Walk " + direction(prev.patch, FairyRings.tile(prev.patch.getNearbyRing()))
							+ "to fairy ring " + prev.patch.getNearbyRing() + ", dial " + code;
					}
					else
					{
						from = "From any fairy ring, dial " + code;
					}
					step = new RunPlan.RouteStep(s.patch, s.teleport, from + rest, null, true);
					step.crop = s.crop;
				}
				out.add(step);
				prev = step;
			}
		}
		return out;
	}

	/** Lower = the ring is closer to its patch (ALQ is right by the Morytania patch; BLR is a walk). */
	private static int ringWalkRank(String ring)
	{
		switch (ring)
		{
			case "ALQ":
				return 1;
			case "AKR":
				return 2;
			case "CIR":
				return 3;
			case "AJP":
				return 4;
			default:
				return 9;
		}
	}

	private static List<RunPlan.RouteStep> blockWith(List<List<RunPlan.RouteStep>> blocks, FarmPatch patch)
	{
		for (List<RunPlan.RouteStep> b : blocks)
		{
			for (RunPlan.RouteStep s : b)
			{
				if (s.patch == patch)
				{
					return b;
				}
			}
		}
		return null;
	}

	/**
	 * Hands out the best seeds/saplings you own to this type's patches: patches you already use
	 * first, then the rest in route order. Patches left without one simply aren't in the map.
	 */
	private static void allocateCrops(RunPlan plan, PatchType type, Set<FarmPatch> included,
		Map<String, Integer> owned, Options options, Map<FarmPatch, Crop> out)
	{
		List<Crop> plantable = new ArrayList<>();
		for (Crop crop : Crop.values())
		{
			if (crop.getType() == type && crop.getLevel() <= plan.farmingLevel
				&& !options.disabledCrops.contains(crop))
			{
				plantable.add(crop);
			}
		}
		if (plantable.isEmpty())
		{
			plan.warnings.add("No " + type.getDisplayName().toLowerCase()
				+ " ticked to plant. Pick some in the run's section.");
			return;
		}
		List<Crop> preference = new ArrayList<>(plantable);
		java.util.Collections.reverse(preference);
		if (!options.cropRank.isEmpty())
		{
			// Stable sort: ranked crops first in rank order, the rest stay highest level first.
			preference.sort(java.util.Comparator.comparingInt(c ->
			{
				int i = options.cropRank.indexOf(c);
				return i < 0 ? Integer.MAX_VALUE : i;
			}));
		}
		List<Crop> queue = new ArrayList<>();
		for (Crop crop : preference)
		{
			int have = owned.getOrDefault(crop.getSeedName(), 0) / crop.getSeedsPerPatch();
			for (int n = 0; n < have; n++)
			{
				queue.add(crop);
			}
		}
		// Patches you've just planted on this run don't need another seed.
		List<FarmPatch> order = new ArrayList<>();
		for (FarmPatch p : included)
		{
			if (p.getType() == type && options.usedPatches.contains(p) && !options.doneThisRun.contains(p))
			{
				order.add(p);
			}
		}
		for (FarmPatch p : included)
		{
			if (p.getType() == type && !options.usedPatches.contains(p) && !options.doneThisRun.contains(p))
			{
				order.add(p);
			}
		}
		for (FarmPatch p : included)
		{
			if (p.getType() == type && options.doneThisRun.contains(p) && options.plantedCrops.containsKey(p))
			{
				out.put(p, options.plantedCrops.get(p));
			}
		}
		Crop contract = options.contractCrop;
		if (contract != null && contract.getType() == type)
		{
			FarmPatch guild = type == PatchType.HERB ? FarmPatch.FARMING_GUILD_HERB
				: type == PatchType.TREE ? FarmPatch.FARMING_GUILD_TREE
				: type == PatchType.FRUIT_TREE ? FarmPatch.FARMING_GUILD_FRUIT
				: type == PatchType.ALLOTMENT ? FarmPatch.GUILD_ALLOTMENT_A
				: type == PatchType.FLOWER ? FarmPatch.GUILD_FLOWER
				: type == PatchType.BUSH ? FarmPatch.GUILD_BUSH
				: type == PatchType.CACTUS ? FarmPatch.GUILD_CACTUS
				: type == PatchType.CELASTRUS ? FarmPatch.GUILD_CELASTRUS
				: type == PatchType.REDWOOD ? FarmPatch.GUILD_REDWOOD : null;
			if (guild != null && order.contains(guild))
			{
				if (owned.getOrDefault(contract.getSeedName(), 0) >= contract.getSeedsPerPatch() && contract.getLevel() <= plan.farmingLevel)
				{
					out.put(guild, contract);
					queue.remove(contract);
					order.remove(guild);
				}
				else
				{
					plan.warnings.add("Farming contract: bring " + aOrAn(contract.getSeedName()) + " "
						+ contract.getSeedName().toLowerCase() + " for the Farming Guild patch.");
				}
			}
		}
		if (type == PatchType.SPIRIT_TREE)
		{
			// A spirit tree is kept once planted (grown, it's a teleport), so patches with one in
			// aren't replanted, and each counts towards how many you may have planted at once,
			// which depends on your Farming level (OSRS Wiki).
			int allowed = spiritTreeLimit(plan.farmingLevel);
			int occupied = 0;
			for (FarmPatch p : FarmPatch.values())
			{
				if (p.getType() == PatchType.SPIRIT_TREE && options.growingCrops.containsKey(p))
				{
					occupied++;
				}
			}
			order.removeIf(options.growingCrops::containsKey);
			int cap = Math.max(0, allowed - occupied);
			if (order.size() > cap)
			{
				plan.warnings.add("Spirit trees: at Farming " + plan.farmingLevel + " you can have " + allowed
					+ " planted at once" + (occupied > 0 ? " (" + occupied + " already planted)" : "")
					+ ", so " + (cap == 0 ? "none are" : "only " + cap + " " + (cap == 1 ? "is" : "are")) + " planted this run.");
				order = new ArrayList<>(order.subList(0, cap));
			}
		}
		int next = 0;
		int unplanted = 0;
		for (FarmPatch p : order)
		{
			if (next < queue.size())
			{
				out.put(p, queue.get(next++));
			}
			else
			{
				unplanted++;
			}
		}
		if (unplanted > 0 && !order.isEmpty())
		{
			Crop best = plantable.get(plantable.size() - 1);
			String what = type.seedWord();
			String patchName = type.patchName();
			String rest = !type.isTreeLike() ? "the others are only harvested"
				: "the others are just checked for health if grown (and left standing), or left off";
			plan.warnings.add((order.size() == unplanted ? "No " + what + " for your " + patchName + " patches: "
				+ (!type.isTreeLike() ? "patches are only harvested" : "grown ones are just checked for health and left standing")
				: "Only enough " + what + " for " + (order.size() - unplanted) + " of " + order.size()
				+ " " + patchName + " patches: " + rest) + ". Best you can plant: " + best.getSeedName() + ".");
		}
	}

	/** Spirit trees you may have planted at once: 1 at 83, 2 at 88, 3 at 93, 4 at 96, any at 99. */
	static int spiritTreeLimit(int farmingLevel)
	{
		if (farmingLevel >= 99)
		{
			return Integer.MAX_VALUE / 2;
		}
		return farmingLevel >= 96 ? 4 : farmingLevel >= 93 ? 3 : farmingLevel >= 88 ? 2 : farmingLevel >= 83 ? 1 : 0;
	}

	/** Remind you to pot and water tree seeds, so you have saplings for later runs. */
	/**
	 * Leaves growing patches off the run when at least one patch is ready, so one ready herb
	 * doesn't turn into a full run. When nothing is ready the full run is shown for planning.
	 */
	static void keepReadyPatches(RunPlan plan, Set<FarmPatch> included, Options options)
	{
		List<FarmPatch> waiting = new ArrayList<>();
		boolean anyReady = false;
		for (FarmPatch p : included)
		{
			if (options.doneThisRun.contains(p) || !options.notReady.containsKey(p))
			{
				anyReady = true;
			}
			else
			{
				waiting.add(p);
			}
		}
		if (waiting.isEmpty())
		{
			return;
		}
		FarmPatch soonest = null;
		long soonestMs = Long.MAX_VALUE;
		for (FarmPatch p : waiting)
		{
			long ms = options.notReady.get(p);
			if (ms >= 0 && ms < soonestMs)
			{
				soonestMs = ms;
				soonest = p;
			}
		}
		String next = soonest == null ? "" : " Next up: " + soonest.getDisplayName() + " in "
			+ roughTime(soonestMs) + ".";
		if (anyReady)
		{
			included.removeAll(waiting);
			plan.tips.add(waiting.size() + (waiting.size() == 1 ? " patch is" : " patches are")
				+ " still growing, so only the ready ones are on this run." + next);
		}
		else
		{
			plan.tips.add("Nothing is ready yet." + next + " This is the full run for when they are.");
		}
	}

	/** "south " / "north-west " from a patch to a tile (with a rough distance when it's a walk), or "". */
	static String direction(FarmPatch from, int[] to)
	{
		if (to == null)
		{
			return "";
		}
		int dx = to[0] - from.getX();
		int dy = to[1] - from.getY();
		int d = Math.max(Math.abs(dx), Math.abs(dy));
		if (d < 4)
		{
			return "";
		}
		String ns = Math.abs(dy) * 2 < Math.abs(dx) ? "" : dy > 0 ? "north" : "south";
		String ew = Math.abs(dx) * 2 < Math.abs(dy) ? "" : dx > 0 ? "east" : "west";
		String dir = ns.isEmpty() ? ew : ew.isEmpty() ? ns : ns + "-" + ew;
		return dir + (d >= 15 ? " (about " + (Math.round(d / 5.0) * 5) + " tiles) " : " ");
	}

	/** About how much XP the run gives: what you collect from grown crops, planting, and compost. */
	static double estimateXp(RunPlan plan, Options options)
	{
		double xp = 0;
		for (RunPlan.RouteStep s : plan.route)
		{
			if (s.done)
			{
				continue;
			}
			Crop growing = options.growingCrops.get(s.patch);
			if (growing != null && !options.notReady.containsKey(s.patch))
			{
				xp += FarmingXp.grown(growing, options.herbYield);
			}
			if (s.crop != null)
			{
				xp += FarmingXp.plant(s.crop) + FarmingXp.compost(s.compost);
			}
		}
		return xp;
	}

	private static String aOrAn(String word)
	{
		return "AEIOUaeiou".indexOf(word.charAt(0)) >= 0 ? "an" : "a";
	}

	/** "about 30 min" / "about 2h 10m" - rounded up, so the tip doesn't need to tick. */
	private static String roughTime(long ms)
	{
		long min = Math.max(1, (ms + 59_999) / 60_000);
		return "about " + (min < 60 ? min + " min" : (min / 60) + "h " + (min % 60) + "m");
	}

	static void addSeedlingTips(RunPlan plan, PatchType type, Map<String, Integer> owned, Options options)
	{
		for (Crop c : Crop.values())
		{
			if (c.getType() != type || c.getLevel() > plan.farmingLevel || options.disabledCrops.contains(c))
			{
				continue;
			}
			String name = c.getDisplayName().toLowerCase();
			int seeds = owned.getOrDefault(c.getTreeSeedName(), 0);
			int seedlings = owned.getOrDefault(c.getSeedlingName(), 0);
			int watered = owned.getOrDefault(c.getWateredSeedlingName(), 0);
			if (seeds > 0)
			{
				plan.tips.add("You have " + seeds + " " + c.getTreeSeedName().toLowerCase() + (seeds > 1 ? "s" : "")
					+ ": plant them in plant pots filled with soil (use a gardening trowel) and water them."
					+ " They grow into " + name + " saplings for later runs.");
			}
			if (seedlings > 0)
			{
				plan.tips.add("Water your " + seedlings + " " + name + " seedling" + (seedlings > 1 ? "s" : "")
					+ " with a watering can so they grow into saplings.");
			}
			if (watered > 0)
			{
				plan.tips.add(watered + " watered " + name + " seedling" + (watered > 1 ? "s are" : " is")
					+ " growing into saplings. Keep them in your bank or inventory.");
			}
		}
	}

	private static void paymentWarnings(RunPlan plan, Map<String, Integer> owned)
	{
		Map<String, Integer> need = new LinkedHashMap<>();
		for (RunPlan.RouteStep s : plan.route)
		{
			if (s.paymentItem != null && !s.done)
			{
				need.merge(s.paymentItem, s.paymentQuantity, Integer::sum);
				for (String[] extra : s.crop.getExtraPayments())
				{
					need.merge(extra[0], Integer.parseInt(extra[1]), Integer::sum);
				}
			}
		}
		need.forEach((item, qty) ->
		{
			int have = owned.getOrDefault(item, 0);
			if (have < qty)
			{
				plan.warnings.add("Short " + (qty - have) + " " + item + " to pay the gardeners.");
			}
		});
	}

	/** Returns the bottomless bucket's name if used, else assigns compost per stop best-first. */
	private static String assignCompost(RunPlan plan, Map<String, Integer> owned, Options options)
	{
		if (!options.useCompost)
		{
			return null;
		}
		for (String name : owned.keySet())
		{
			if (name.startsWith("Bottomless compost bucket") && owned.get(name) > 0)
			{
				for (RunPlan.RouteStep s : plan.route)
				{
					if (s.crop != null)
					{
						s.compost = name;
					}
				}
				return name;
			}
		}
		Map<String, Integer> left = new LinkedHashMap<>();
		for (String c : COMPOST_BEST_FIRST)
		{
			left.put(c, owned.getOrDefault(c, 0));
		}
		int missing = 0;
		for (RunPlan.RouteStep s : plan.route)
		{
			if (s.crop == null)
			{
				continue; // nothing being planted here
			}
			if (s.done)
			{
				// Just planted: point at the best compost you're carrying, without reserving it.
				for (String c : COMPOST_BEST_FIRST)
				{
					if (owned.getOrDefault(c, 0) > 0)
					{
						s.compost = c;
						break;
					}
				}
				continue;
			}
			for (String c : COMPOST_BEST_FIRST)
			{
				if (left.get(c) > 0)
				{
					s.compost = c;
					left.merge(c, -1, Integer::sum);
					break;
				}
			}
			if (s.compost == null)
			{
				missing++;
			}
		}
		if (missing > 0)
		{
			plan.warnings.add("Short " + missing + " compost.");
		}
		return null;
	}

	/** Tools, teleports, runes, staff: carried for the whole run. */
	private static List<RunPlan.Withdrawal> fixedItems(RunPlan plan, Map<String, Integer> owned, Options options,
		String bottomless, boolean herbs)
	{
		List<RunPlan.Withdrawal> out = new ArrayList<>();
		if (herbs)
		{
			requireTool(plan, out, owned, "Seed dibber");
		}
		requireTool(plan, out, owned, "Spade");
		if (options.bringRake && mightHaveWeeds(plan, options))
		{
			requireTool(plan, out, owned, "Rake");
		}
		if (bottomless != null)
		{
			out.add(new RunPlan.Withdrawal(bottomless, 1, "make sure it's filled"));
		}
		if (herbs && owned.getOrDefault("Magic secateurs", 0) > 0)
		{
			out.add(new RunPlan.Withdrawal("Magic secateurs", 1, "+10% herb yield, wield it"));
		}

		// Diseased patches: plant cure for herbs, secateurs to prune trees.
		int sickHerbs = 0;
		boolean sickTree = false;
		for (RunPlan.RouteStep step : plan.route)
		{
			if (options.diseased.contains(step.patch))
			{
				if (!step.patch.getType().isTreeLike())
				{
					sickHerbs++;
				}
				else
				{
					sickTree = true;
				}
			}
		}
		if (sickHerbs > 0)
		{
			out.add(new RunPlan.Withdrawal("Plant cure", sickHerbs, "for the diseased herb patch"));
			if (owned.getOrDefault("Plant cure", 0) < sickHerbs)
			{
				plan.warnings.add("Buy " + sickHerbs + " plant cure (Farming shops or the GE) for your diseased herbs.");
			}
		}
		if (sickTree && owned.getOrDefault("Magic secateurs", 0) == 0)
		{
			requireTool(plan, out, owned, "Secateurs");
		}

		// Teleport items and tablets
		Map<String, Integer> uses = new LinkedHashMap<>();
		Map<String, String> firstPatch = new LinkedHashMap<>();
		Map<String, Integer> runes = new LinkedHashMap<>();
		boolean fairy = false;
		boolean unknownRunes = false;
		for (RunPlan.RouteStep step : plan.route)
		{
			if (step.teleportItem != null)
			{
				uses.merge(step.teleportItem, 1, Integer::sum);
				firstPatch.putIfAbsent(step.teleportItem, step.patch.getDisplayName());
			}
			if (step.teleport == null)
			{
				continue;
			}
			if (step.teleport.getKind() == Teleport.Kind.SPELL)
			{
				unknownRunes |= step.teleport.getRunes().isEmpty();
				step.teleport.getRunes().forEach((rune, qty) -> runes.merge(rune, qty, Integer::sum));
			}
			else if (step.teleport.getKind() == Teleport.Kind.FAIRY_RING)
			{
				fairy = true;
			}
			else if (step.teleport == Teleport.SPIRIT_TREE)
			{
				// Getting to the spirit tree by the Grand Exchange: Varrock Teleport (spell or tab).
				Teleport.Context ctx = options.teleports;
				if (ctx.useSpells && ctx.standardSpellbook && ctx.magicLevel >= 25)
				{
					Teleport.VARROCK_SPELL.getRunes().forEach((rune, qty) -> runes.merge(rune, qty, Integer::sum));
				}
				else if (owned.getOrDefault("Varrock teleport", 0) > 0)
				{
					uses.merge("Varrock teleport", 1, Integer::sum);
					firstPatch.putIfAbsent("Varrock teleport", step.patch.getDisplayName());
				}
			}
		}
		for (Map.Entry<String, Integer> e : uses.entrySet())
		{
			String name = e.getKey();
			int qty = name.toLowerCase().endsWith(" teleport") ? e.getValue() : 1;
			out.add(new RunPlan.Withdrawal(name, qty, firstPatch.get(name)));
		}
		for (Map.Entry<String, Integer> e : runes.entrySet())
		{
			out.add(new RunPlan.Withdrawal(e.getKey(), e.getValue(), "spell runes"));
			int have = owned.getOrDefault(e.getKey(), 0);
			if (have < e.getValue())
			{
				plan.warnings.add("Short " + (e.getValue() - have) + " " + e.getKey()
					+ " (fine if a staff or rune pouch covers it).");
			}
		}
		if (unknownRunes)
		{
			plan.warnings.add("Check the runes for Kourend/Civitas teleports in your spellbook.");
		}
		if (fairy && !options.teleports.staffless)
		{
			String staff = Teleport.staffName(owned);
			if (staff != null)
			{
				out.add(new RunPlan.Withdrawal(staff, 1, "wield it for fairy rings"));
			}
		}
		return out;
	}

	/** True if any stop still to do could have weeds (so the rake is worth carrying). */
	static boolean mightHaveWeeds(RunPlan plan, Options options)
	{
		for (RunPlan.RouteStep s : plan.route)
		{
			if (!s.done && !options.weedFree.contains(s.patch))
			{
				return true;
			}
		}
		return false;
	}

	private static void requireTool(RunPlan plan, List<RunPlan.Withdrawal> out, Map<String, Integer> owned, String tool)
	{
		out.add(new RunPlan.Withdrawal(tool, 1, ""));
		// Before the bank's been seen this session, the "open your bank" warning covers it.
		if (plan.bankKnown && owned.getOrDefault(tool, 0) <= 0)
		{
			plan.warnings.add("You don't seem to own a " + tool + " (unless it's in a tool leprechaun store).");
		}
	}

	/** Slots the fixed items take, counting one wearable per equipment slot as worn. */
	static int fixedSlots(List<RunPlan.Withdrawal> fixed)
	{
		int slots = 0;
		boolean[] worn = new boolean[WEAR_SLOTS.length];
		outer:
		for (RunPlan.Withdrawal w : fixed)
		{
			for (int i = 0; i < WEAR_SLOTS.length; i++)
			{
				for (String prefix : WEAR_SLOTS[i])
				{
					if (w.itemName.startsWith(prefix) && !worn[i])
					{
						worn[i] = true;
						continue outer;
					}
				}
			}
			slots++;
		}
		return slots;
	}

	/** Slots one leg's supplies take: saplings and unnoted compost one each; seeds, payments, coins stack. */
	static int legSlots(List<RunPlan.RouteStep> leg)
	{
		Set<String> stackTypes = new LinkedHashSet<>();
		int slots = 0;
		for (RunPlan.RouteStep s : leg)
		{
			if (s.crop != null && !s.done)
			{
				if (s.patch.getType().seedsStack())
				{
					stackTypes.add(s.crop.getSeedName());
				}
				else
				{
					slots++;
				}
			}
			if (s.compost != null && !s.compost.startsWith("Bottomless"))
			{
				slots++;
			}
			if (s.paymentItem != null)
			{
				stackTypes.add(s.paymentItem + " (noted)");
				for (String[] extra : s.crop.getExtraPayments())
				{
					stackTypes.add(extra[0] + " (noted)");
				}
			}
			if (s.coins > 0)
			{
				stackTypes.add("Coins");
			}
		}
		return slots + stackTypes.size();
	}

	/**
	 * Splits the route into legs that each fit in one inventory, adding a bank stop before
	 * each new leg. The first leg's supplies plus the fixed items are what you take at the start.
	 */
	static void splitIntoTrips(RunPlan plan, List<RunPlan.Withdrawal> fixed, Options options)
	{
		int fixedSlots = fixedSlots(fixed);
		List<List<RunPlan.RouteStep>> legs = new ArrayList<>();
		List<Integer> legStart = new ArrayList<>();
		List<RunPlan.RouteStep> leg = new ArrayList<>();
		for (int i = 0; i < plan.route.size(); i++)
		{
			RunPlan.RouteStep s = plan.route.get(i);
			leg.add(s);
			if (leg.size() > 1 && fixedSlots + legSlots(leg) > options.inventorySlots)
			{
				leg.remove(leg.size() - 1);
				legs.add(leg);
				legStart.add(i - leg.size());
				leg = new ArrayList<>();
				leg.add(s);
			}
		}
		legs.add(leg);
		legStart.add(plan.route.size() - leg.size());

		plan.withdrawals.addAll(supplies(legs.get(0)));
		plan.withdrawals.addAll(fixed);
		plan.slotsNeeded = fixedSlots + legSlots(legs.get(0));

		for (int l = 1; l < legs.size(); l++)
		{
			int before = legStart.get(l);
			String bank = null;
			for (int i = before - 1; i >= 0 && bank == null; i--)
			{
				bank = plan.route.get(i).patch.getBank();
			}
			RunPlan.BankStop stop = new RunPlan.BankStop(before, bank == null ? "the nearest bank" : bank);
			stop.withdrawals.addAll(supplies(legs.get(l)));
			plan.bankStops.add(stop);
		}
	}

	private static List<RunPlan.Withdrawal> supplies(List<RunPlan.RouteStep> leg)
	{
		Map<String, Integer> seeds = new LinkedHashMap<>();
		Map<String, Integer> compost = new LinkedHashMap<>();
		Map<String, Integer> pay = new LinkedHashMap<>();
		Map<String, String> notes = new LinkedHashMap<>();
		int coins = 0;
		for (RunPlan.RouteStep s : leg)
		{
			if (s.done)
			{
				continue; // planted already: nothing more to bring for it
			}
			if (s.crop != null)
			{
				seeds.merge(s.crop.getSeedName(), s.crop.getSeedsPerPatch(), Integer::sum);
				notes.put(s.crop.getSeedName(), "level " + s.crop.getLevel());
			}
			if (s.compost != null && !s.compost.startsWith("Bottomless"))
			{
				compost.merge(s.compost, 1, Integer::sum);
			}
			if (s.paymentItem != null)
			{
				pay.merge(s.paymentItem, s.paymentQuantity, Integer::sum);
				for (String[] extra : s.crop.getExtraPayments())
				{
					pay.merge(extra[0], Integer.parseInt(extra[1]), Integer::sum);
				}
			}
			coins += s.coins;
		}
		List<RunPlan.Withdrawal> out = new ArrayList<>();
		seeds.forEach((k, v) -> out.add(new RunPlan.Withdrawal(k, v, notes.get(k))));
		compost.forEach((k, v) -> out.add(new RunPlan.Withdrawal(k, v, "")));
		pay.forEach((k, v) ->
		{
			// Same item used two ways (e.g. compost for potatoes): one line with the total.
			for (int i = 0; i < out.size(); i++)
			{
				RunPlan.Withdrawal w = out.get(i);
				if (w.itemName.equals(k))
				{
					out.set(i, new RunPlan.Withdrawal(k, w.quantity + v, "for the patches and to pay the gardeners"));
					return;
				}
			}
			out.add(new RunPlan.Withdrawal(k, v, "gardener payment, noted is fine"));
		});
		if (coins > 0)
		{
			out.add(new RunPlan.Withdrawal("Coins", coins, "gardener clears grown trees, "
				+ TREE_REMOVAL_COINS + " each"));
		}
		return out;
	}
}
