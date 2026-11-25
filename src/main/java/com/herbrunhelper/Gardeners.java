package com.herbrunhelper;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** The gardener NPC at each patch (ids from RuneLite's Time Tracking FarmingWorld). */
final class Gardeners
{
	private static final Map<FarmPatch, Integer> IDS = new EnumMap<>(FarmPatch.class);
	static final Set<Integer> ALL = new HashSet<>();

	static
	{
		put(FarmPatch.VARROCK_TREE, 11957);
		put(FarmPatch.LUMBRIDGE_TREE, 2681);
		put(FarmPatch.FALADOR_TREE, 2679);
		put(FarmPatch.TAVERLEY_TREE, 2678);
		put(FarmPatch.GNOME_TREE, 2687);
		put(FarmPatch.NEMUS_RETREAT_TREE, 14514);
		put(FarmPatch.FARMING_GUILD_TREE, 8534);
		put(FarmPatch.GNOME_FRUIT, 2682);
		put(FarmPatch.TREE_GNOME_VILLAGE_FRUIT, 2683);
		put(FarmPatch.CATHERBY_FRUIT, 2670);
		put(FarmPatch.BRIMHAVEN_FRUIT, 2669);
		put(FarmPatch.LLETYA_FRUIT, 2689);
		put(FarmPatch.FARMING_GUILD_FRUIT, 8533);
		put(FarmPatch.KASTORI_FRUIT, 14516);
		put(FarmPatch.FOSSIL_HARDWOOD_EAST, 7754);
		put(FarmPatch.FOSSIL_HARDWOOD_MIDDLE, 7755);
		put(FarmPatch.FOSSIL_HARDWOOD_WEST, 7756);
		put(FarmPatch.LOCUS_OASIS_HARDWOOD, 13401);
		put(FarmPatch.FALADOR_ALLOTMENT_A, 2663);
		put(FarmPatch.FALADOR_ALLOTMENT_B, 2663);
		put(FarmPatch.CATHERBY_ALLOTMENT_A, 2664);
		put(FarmPatch.CATHERBY_ALLOTMENT_B, 2664);
		put(FarmPatch.ARDOUGNE_ALLOTMENT_A, 2665);
		put(FarmPatch.ARDOUGNE_ALLOTMENT_B, 2665);
		put(FarmPatch.MORYTANIA_ALLOTMENT_A, 2666);
		put(FarmPatch.MORYTANIA_ALLOTMENT_B, 2666);
		put(FarmPatch.HOSIDIUS_ALLOTMENT_A, 6921);
		put(FarmPatch.HOSIDIUS_ALLOTMENT_B, 6921);
		put(FarmPatch.VARLAMORE_ALLOTMENT_A, 12766);
		put(FarmPatch.VARLAMORE_ALLOTMENT_B, 12766);
		put(FarmPatch.GUILD_ALLOTMENT_A, 8535);
		put(FarmPatch.GUILD_ALLOTMENT_B, 8535);
		put(FarmPatch.CHAMPIONS_GUILD_BUSH, 2674);
		put(FarmPatch.RIMMINGTON_BUSH, 2675);
		put(FarmPatch.ARDOUGNE_BUSH, 2677);
		put(FarmPatch.ETCETERIA_BUSH, 2676);
		put(FarmPatch.GUILD_BUSH, 8535);
		put(FarmPatch.LUMBRIDGE_HOPS, 2672);
		put(FarmPatch.SEERS_HOPS, 2673);
		put(FarmPatch.YANILLE_HOPS, 2671);
		put(FarmPatch.ENTRANA_HOPS, 2667);
		put(FarmPatch.ALDARIN_HOPS, 14030);
		put(FarmPatch.AL_KHARID_CACTUS, 310);
		put(FarmPatch.GUILD_CACTUS, 8535);
		put(FarmPatch.TAI_BWO_WANNAI_CALQUAT, 2688);
		put(FarmPatch.KASTORI_CALQUAT, 14515);
		put(FarmPatch.PORT_SARIM_SPIRIT, 2684);
		put(FarmPatch.ETCETERIA_SPIRIT, 2685);
		put(FarmPatch.BRIMHAVEN_SPIRIT, 2686);
		put(FarmPatch.HOSIDIUS_SPIRIT, 6814);
		put(FarmPatch.GUILD_SPIRIT, 8537);
		put(FarmPatch.GUILD_CELASTRUS, 8629);
		put(FarmPatch.GUILD_REDWOOD, 8536);
	}

	/**
	 * A walkable tile right beside a patch, for patches whose own tile can't be stood on, used
	 * until the gardener's spot has been learned. Checked in game.
	 */
	private static final Map<FarmPatch, int[]> WALK_TO = new EnumMap<>(FarmPatch.class);

	static
	{
		WALK_TO.put(FarmPatch.LOCUS_OASIS_HARDWOOD, new int[]{1686, 2974, 0});
	}

	private Gardeners()
	{
	}

	/** Built-in walkable tile beside the patch (x, y, plane), or null. */
	static int[] walkTo(FarmPatch patch)
	{
		return WALK_TO.get(patch);
	}

	private static void put(FarmPatch patch, int id)
	{
		IDS.put(patch, id);
		ALL.add(id);
	}

	/**
	 * Does this patch's gardener also look after a different kind of patch (the Farming Guild's
	 * Alan: allotments, bush and cactus)? Then where they stand says little about this patch.
	 */
	static boolean sharedAcrossTypes(FarmPatch patch)
	{
		int id = idFor(patch);
		if (id < 0)
		{
			return false;
		}
		for (Map.Entry<FarmPatch, Integer> e : IDS.entrySet())
		{
			if (e.getValue() == id && e.getKey().getType() != patch.getType())
			{
				return true;
			}
		}
		return false;
	}

	/** The gardener's NPC id for this patch, or -1. */
	static int idFor(FarmPatch patch)
	{
		return IDS.getOrDefault(patch, -1);
	}

	/** Other ids the same gardener can spawn as (e.g. Marcellus changes with his quest). */
	private static final Map<FarmPatch, int[]> ALTERNATES = new EnumMap<>(FarmPatch.class);

	static
	{
		// Marcellus: 13401 is his quest-state parent; 12935 / 12936 are what actually spawns.
		ALTERNATES.put(FarmPatch.LOCUS_OASIS_HARDWOOD, new int[]{12936, 12935});
		for (int[] ids : ALTERNATES.values())
		{
			for (int id : ids)
			{
				ALL.add(id);
			}
		}
	}

	/** Whether an NPC id is this patch's gardener. */
	static boolean isGardenerOf(FarmPatch patch, int npcId)
	{
		if (npcId == idFor(patch))
		{
			return true;
		}
		int[] alt = ALTERNATES.get(patch);
		if (alt != null)
		{
			for (int id : alt)
			{
				if (id == npcId)
				{
					return true;
				}
			}
		}
		return false;
	}
}
