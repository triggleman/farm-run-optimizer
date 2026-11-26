/*
 * Patch regions and varbit ids match RuneLite's Time Tracking plugin (FarmingWorld),
 * Copyright (c) 2018, NotFoxtrot and Copyright (c) 2018 Abex, BSD 2-Clause; see NOTICE.
 * Checked by scripts/verify_patches.py. Patch tiles are the farming patch points from
 * RuneLite's World Map plugin (FarmingPatchLocation); the plugin also learns each patch's exact
 * tile from the game the first time you see it. Directions follow the OSRS Wiki farming guides.
 */
package com.herbrunhelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Every patch the runs visit, in route order (nearby patches back-to-back). The planner moves
 * fairy-ring stops so they're chained together. Each patch lists its ways in, fastest first.
 *
 * regions: the map regions the game reports this patch in (exact, from Time Tracking).
 * varbit: the game variable holding the patch's state (exact, from Time Tracking).
 * x/y: patch tile from RuneLite's world map (allotment areas mark the middle of the area).
 *   Replaced by the exact tile once the plugin has seen the patch in game.
 * nearbyRing: fairy ring you can walk to from the patch (starts a fairy chain).
 * bank: nearest bank, used when the run needs a bank stop.
 */
public enum FarmPatch
{
	// ---- Trees around Varrock / Lumbridge / Falador
	VARROCK_TREE("Varrock tree", PatchType.TREE, Requirement.NONE, 3229, 3459, r(12854, 12853), 4771, null, "Varrock East bank",
		via(Teleport.VARROCK_TAB, "run east to the palace garden"),
		via(Teleport.VARROCK_SPELL, "run east to the palace garden")),
	LUMBRIDGE_TREE("Lumbridge tree", PatchType.TREE, Requirement.NONE, 3193, 3231, r(12594, 12850), 4771, null, "Lumbridge Castle bank (top floor)",
		via(Teleport.LUMBRIDGE_TAB, "run west over the bridge"),
		via(Teleport.LUMBRIDGE_SPELL, "run west over the bridge"),
		walk("Home Teleport, then run west", null)),
	FALADOR_TREE("Falador tree (park)", PatchType.TREE, Requirement.NONE, 3004, 3373, r(11828, 12084), 4771, null, "Falador West bank",
		via(Teleport.FALADOR_TAB, "run east into the park"),
		via(Teleport.FALADOR_SPELL, "run east into the park")),
	TAVERLEY_TREE("Taverley tree", PatchType.TREE, Requirement.NONE, 2936, 3438, r(11573, 11829), 4771, null, "Falador West bank",
		walk("Walk west from Falador park to Taverley", "FALADOR_TREE"),
		house(HouseLocation.TAVERLEY, "run south-east through Taverley", false),
		via(Teleport.FALADOR_TAB, "run west to Taverley"),
		via(Teleport.FALADOR_SPELL, "run west to Taverley")),
	FALADOR_HERB("Falador herb", PatchType.HERB, Requirement.NONE, 3058, 3311, r(12083), 4774, null, "Falador East bank",
		via(Teleport.EXPLORERS_RING, "cabbage patch teleport, run north"),
		walk("Walk south from Falador park", "FALADOR_TREE"),
		via(Teleport.FALADOR_TAB, "run south-east"),
		via(Teleport.FALADOR_SPELL, "run south-east")),

	// ---- Gnome Stronghold / Tree Gnome Village
	GNOME_TREE("Gnome Stronghold tree", PatchType.TREE, Requirement.NONE, 2436, 3415, r(9781, 9525, 9526, 9782), 4771, null, "Grand Tree bank",
		via(Teleport.ROYAL_SEED_POD, "Grand Tree, run south-west"),
		via(Teleport.SPIRIT_TREE, "Varrock Teleport, spirit tree at the Grand Exchange to Gnome Stronghold, run south-west")),
	GNOME_FRUIT("Gnome Stronghold fruit tree", PatchType.FRUIT_TREE, Requirement.NONE, 2476, 3446, r(9781, 9525, 9526, 9782), 4772, null, "Grand Tree bank",
		walk("Head north-east through the Stronghold to the fruit tree patch", "GNOME_TREE"),
		via(Teleport.ROYAL_SEED_POD, "Grand Tree, fruit tree patch beside it"),
		via(Teleport.SPIRIT_TREE, "Varrock Teleport, spirit tree at the Grand Exchange to Gnome Stronghold, fruit tree patch beside the Grand Tree")),
	TREE_GNOME_VILLAGE_FRUIT("Tree Gnome Village fruit tree", PatchType.FRUIT_TREE, Requirement.NONE, 2490, 3180, r(9777, 10033), 4771, null, "Grand Tree bank",
		walk("Spirit tree to Tree Gnome Village, get Elkoy to guide you out of the maze", "GNOME_FRUIT"),
		walk("Spirit tree to Tree Gnome Village, get Elkoy to guide you out of the maze", "GNOME_TREE"),
		via(Teleport.FAIRY_CIQ, "head north-west to the patch"),
		via(Teleport.SPIRIT_TREE, "Varrock Teleport, spirit tree at the Grand Exchange to Tree Gnome Village, get Elkoy to guide you out")),

	// ---- Catherby / Brimhaven / Ardougne
	CATHERBY_HERB("Catherby herb", PatchType.HERB, Requirement.NONE, 2813, 3463, r(11062, 11061, 11317, 11318), 4774, null, "Catherby bank",
		via(Teleport.CAMELOT_TAB, "run east to Catherby"),
		via(Teleport.CAMELOT_SPELL, "run east to Catherby")),
	CATHERBY_FRUIT("Catherby fruit tree", PatchType.FRUIT_TREE, Requirement.NONE, 2860, 3433, r(11317), 4771, null, "Catherby bank",
		walk("Follow the beach east to the fruit tree patch", "CATHERBY_HERB"),
		via(Teleport.CAMELOT_TAB, "head through Catherby to the fruit tree patch"),
		via(Teleport.CAMELOT_SPELL, "head through Catherby to the fruit tree patch")),
	BRIMHAVEN_FRUIT("Brimhaven fruit tree", PatchType.FRUIT_TREE, Requirement.NONE, 2765, 3213, r(11058, 11057), 4771, null, "Ardougne South bank",
		house(HouseLocation.BRIMHAVEN, "run north to the fruit tree patch", true),
		via(Teleport.BRIMHAVEN_TAB, "patch is just north"),
		via(Teleport.ARDOUGNE_TAB, "boat from Ardougne docks to Brimhaven (30 coins), patch north-west"),
		via(Teleport.ARDOUGNE_SPELL, "boat from Ardougne docks to Brimhaven (30 coins), patch north-west")),
	ARDOUGNE_HERB("Ardougne herb", PatchType.HERB, Requirement.NONE, 2670, 3374, r(10548), 4774, "BLR", "Ardougne North bank",
		beatsFairy(Teleport.ARDOUGNE_CLOAK, "farm teleport (cloak 2+)"),
		beatsFairy(Teleport.ARDOUGNE_TAB, "run north-east"),
		beatsFairy(Teleport.ARDOUGNE_SPELL, "run north-east"),
		via(Teleport.FAIRY_BLR, "run west")),

	// ---- Fairy-ring chain patches
	MORYTANIA_HERB("Morytania herb", PatchType.HERB, Requirement.PRIEST_IN_PERIL, 3605, 3529, r(14391, 14390), 4774, "ALQ", "Port Phasmatys bank",
		beatsFairy(Teleport.ECTOPHIAL, "run west"),
		via(Teleport.FAIRY_ALQ, "run north"),
		via(Teleport.FENKENSTRAIN_TAB, "run east")),
	HOSIDIUS_HERB("Hosidius herb", PatchType.HERB, Requirement.NONE, 1738, 3550, r(6967, 6711), 4774, "AKR", "Hosidius kitchen bank",
		via(Teleport.XERICS_TALISMAN, "Xeric's Glade, run south-west"),
		via(Teleport.KHAREDSTS_MEMOIRS, "Hosidius, run south-east"),
		house(HouseLocation.HOSIDIUS, "run north to the farming patches", false),
		via(Teleport.FAIRY_AKR, "run west"),
		via(Teleport.KOUREND_SPELL, "run south-east")),
	FARMING_GUILD_TREE("Farming Guild tree", PatchType.TREE, Requirement.FARMING_65, 1232, 3736, r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 7905, "CIR", "Farming Guild bank",
		beatsFairy(Teleport.FARMING_CAPE, null),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild"),
		via(Teleport.FAIRY_CIR, "run south-west")),
	FARMING_GUILD_HERB("Farming Guild herb", PatchType.HERB, Requirement.FARMING_65, 1238, 3726, r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 4775, "CIR", "Farming Guild bank",
		walk("Walk from the guild tree patch", "FARMING_GUILD_TREE"),
		beatsFairy(Teleport.FARMING_CAPE, "run west"),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild, run west"),
		via(Teleport.FAIRY_CIR, "run south-west")),
	FARMING_GUILD_FRUIT("Farming Guild fruit tree", PatchType.FRUIT_TREE, Requirement.FARMING_85, 1242, 3758, r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 7909, "CIR", "Farming Guild bank",
		walk("Into the guild's top tier, north side", "FARMING_GUILD_HERB"),
		walk("Into the guild's top tier, north side", "FARMING_GUILD_TREE"),
		beatsFairy(Teleport.FARMING_CAPE, "run north"),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild, run north"),
		via(Teleport.FAIRY_CIR, "head into the guild, top tier")),

	// ---- Varlamore
	VARLAMORE_HERB("Varlamore herb (Ortus Farm)", PatchType.HERB, Requirement.CHILDREN_OF_THE_SUN, 1582, 3094,
		r(6192, 6191, 6193, 6447, 6448, 6449), 4774, "AJP", "Civitas illa Fortis bank",
		via(Teleport.QUETZAL_WHISTLE, "Hunter Guild, run north"),
		via(Teleport.HUNTER_CAPE, "Hunter Guild, run north-east"),
		via(Teleport.FAIRY_AJP, "run north-west"),
		via(Teleport.CIVITAS_SPELL, "quetzal to the Hunter Guild, run north")),
	LOCUS_OASIS_HARDWOOD("Locus Oasis hardwood", PatchType.HARDWOOD, Requirement.CHILDREN_OF_THE_SUN, 1687, 2972, r(6702, 6446), 4771, "AJP", null,
		via(Teleport.FAIRY_AJP, "Avium Savannah, patch at the oasis")),
	KASTORI_FRUIT("Kastori fruit tree", PatchType.FRUIT_TREE, Requirement.CHILDREN_OF_THE_SUN, 1350, 3057, r(5423, 5167, 5424), 4772, null, null,
		via(Teleport.PENDANT_OF_ATES, "Kastori"),
		via(Teleport.QUETZAL_WHISTLE, "Kastori"),
		walk("Quetzal from the Hunter Guild to Kastori", "VARLAMORE_HERB")),
	NEMUS_RETREAT_TREE("Nemus Retreat tree", PatchType.TREE, Requirement.CHILDREN_OF_THE_SUN, 1367, 3322, r(5427, 5428, 5684), 4771, null, null,
		via(Teleport.PENDANT_OF_ATES, "Nemus Retreat"),
		via(Teleport.QUETZAL_WHISTLE, "then head to Nemus Retreat"),
		via(Teleport.FAIRY_AIS, "Auburnvale, then west to Nemus Retreat")),

	// ---- Elsewhere
	LLETYA_FRUIT("Lletya fruit tree", PatchType.FRUIT_TREE, Requirement.MOURNINGS_END, 2347, 3162, r(9265, 11103), 4771, null, "Lletya bank",
		via(Teleport.TELEPORT_CRYSTAL, "Lletya")),
	TROLL_HERB("Troll Stronghold herb", PatchType.HERB, Requirement.MY_ARMS_BIG_ADVENTURE, 2826, 3694, r(11321), 4771, null, null,
		via(Teleport.STONY_BASALT, null),
		via(Teleport.TROLLHEIM_TAB, "climb to the stronghold roof"),
		via(Teleport.TROLLHEIM_SPELL, "climb to the stronghold roof")),
	WEISS_HERB("Weiss herb", PatchType.HERB, Requirement.MAKING_FRIENDS_WITH_MY_ARM, 2848, 3934, r(11325), 4771, null, null,
		via(Teleport.ICY_BASALT, null)),
	HARMONY_HERB("Harmony Island herb", PatchType.HERB, Requirement.MORYTANIA_ELITE, 3789, 2837, r(15148), 4772, null, null,
		via(Teleport.HARMONY_TAB, null)),
	FOSSIL_HARDWOOD_EAST("Fossil Island hardwood (east)", PatchType.HARDWOOD, Requirement.BONE_VOYAGE, 3715, 3835, r(14651, 14650, 14652, 14906, 14907, 14908, 15162, 15163, 15164), 4771, null, "Museum Camp bank",
		via(Teleport.DIGSITE_PENDANT, "Fossil Island, patches by the camp")),
	FOSSIL_HARDWOOD_MIDDLE("Fossil Island hardwood (middle)", PatchType.HARDWOOD, Requirement.BONE_VOYAGE, 3708, 3833, r(14651, 14650, 14652, 14906, 14907, 14908, 15162, 15163, 15164), 4772, null, "Museum Camp bank",
		walk("Next patch along", "FOSSIL_HARDWOOD_EAST"),
		via(Teleport.DIGSITE_PENDANT, "Fossil Island, patches by the camp")),
	FOSSIL_HARDWOOD_WEST("Fossil Island hardwood (west)", PatchType.HARDWOOD, Requirement.BONE_VOYAGE, 3702, 3837, r(14651, 14650, 14652, 14906, 14907, 14908, 15162, 15163, 15164), 4773, null, "Museum Camp bank",
		walk("Next patch along", "FOSSIL_HARDWOOD_MIDDLE"),
		via(Teleport.DIGSITE_PENDANT, "Fossil Island, patches by the camp")),

	// ---- Bushes (1 seed; check health, pick the berries, dig up to replant)
	CHAMPIONS_GUILD_BUSH("Champions' Guild bush", PatchType.BUSH, Requirement.NONE, 3182, 3356, r(12596), 4771, null, "Varrock West bank",
		via(Teleport.VARROCK_TAB, "run south-west past the Champions' Guild"),
		via(Teleport.VARROCK_SPELL, "run south-west past the Champions' Guild")),
	RIMMINGTON_BUSH("Rimmington bush", PatchType.BUSH, Requirement.NONE, 2938, 3223, r(11570, 11826), 4771, null, "Draynor Village bank",
		house(HouseLocation.RIMMINGTON, "run west to the bush patch", true),
		via(Teleport.EXPLORERS_RING, "cabbage patch teleport, run south-west to Rimmington"),
		via(Teleport.FAIRY_AIQ, "run north to Rimmington"),
		via(Teleport.FALADOR_TAB, "run south to Rimmington"),
		via(Teleport.FALADOR_SPELL, "run south to Rimmington")),
	ARDOUGNE_BUSH("Ardougne bush", PatchType.BUSH, Requirement.NONE, 2615, 3224, r(10290, 10546), 4771, null, "Ardougne South bank",
		via(Teleport.ARDOUGNE_CLOAK_MONASTERY, "Kandarin Monastery teleport (any Ardougne cloak, even cloak 1), bush is just east"),
		via(Teleport.ARDOUGNE_TAB, "run south past the zoo"),
		via(Teleport.ARDOUGNE_SPELL, "run south past the zoo")),
	ETCETERIA_BUSH("Etceteria bush", PatchType.BUSH, Requirement.FREMENNIK_TRIALS, 2589, 3862, r(10300), 4771, null, null,
		via(Teleport.FAIRY_CIP, "run east over the bridge to Etceteria"),
		via(Teleport.RING_OF_WEALTH, "run east over the bridge to Etceteria")),
	GUILD_BUSH("Farming Guild bush", PatchType.BUSH, Requirement.FARMING_45, 1259, 3729, r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 4772, "CIR", "Farming Guild bank",
		walk("Walk from the guild herb patch", "FARMING_GUILD_HERB"),
		beatsFairy(Teleport.FARMING_CAPE, "east wing"),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild, east wing"),
		via(Teleport.FAIRY_CIR, "run south-west, then the guild's east wing")),

	// ---- Hops (4 seeds, jute 3; harvesting empties the patch)
	LUMBRIDGE_HOPS("Lumbridge hops", PatchType.HOPS, Requirement.NONE, 3224, 3313, r(12851), 4771, null, "Lumbridge Castle bank (top floor)",
		via(Teleport.LUMBRIDGE_TAB, "run north past the farms"),
		via(Teleport.LUMBRIDGE_SPELL, "run north past the farms"),
		walk("Walk south-east from the Champions' Guild bush", "CHAMPIONS_GUILD_BUSH")),
	SEERS_HOPS("Seers' Village hops", PatchType.HOPS, Requirement.NONE, 2661, 3523, r(10551, 10550), 4771, "ALS", "Seers' Village bank",
		via(Teleport.FAIRY_ALS, "run north past McGrubor's Wood"),
		via(Teleport.CAMELOT_TAB, "run west past Seers' Village"),
		via(Teleport.CAMELOT_SPELL, "run west past Seers' Village")),
	YANILLE_HOPS("Yanille hops", PatchType.HOPS, Requirement.NONE, 2572, 3102, r(10288), 4771, "CIQ", "Yanille bank",
		house(HouseLocation.YANILLE, "run north-east to the hops patch", true),
		via(Teleport.FAIRY_CIQ, "run south to Yanille")),
	ENTRANA_HOPS("Entrana hops", PatchType.HOPS, Requirement.NONE, 2812, 3334, r(11060, 11316), 4771, null, null,
		via(Teleport.FALADOR_TAB, "run south to Port Sarim, monks' boat to Entrana (no weapons or armour)"),
		via(Teleport.FALADOR_SPELL, "run south to Port Sarim, monks' boat to Entrana (no weapons or armour)"),
		via(Teleport.EXPLORERS_RING, "cabbage patch teleport, run south to Port Sarim, monks' boat (no weapons or armour)")),
	ALDARIN_HOPS("Aldarin hops", PatchType.HOPS, Requirement.CHILDREN_OF_THE_SUN, 1367, 2938, r(5421, 5165, 5166, 5422, 5677, 5678), 4771, null, null,
		house(HouseLocation.ALDARIN, "walk to the hops patch", true),
		via(Teleport.CIVITAS_SPELL, "quetzal from Civitas illa Fortis to Aldarin, run north")),

	// ---- Cactus (1 seed; check health, pick, dig up to replant)
	AL_KHARID_CACTUS("Al Kharid cactus", PatchType.CACTUS, Requirement.NONE, 3313, 3201, r(13106, 13362, 13105), 4771, null, "Al Kharid bank",
		via(Teleport.AMULET_OF_GLORY, "run south-east to the cactus patch"),
		via(Teleport.LUMBRIDGE_TAB, "run east through the Al Kharid toll gate"),
		via(Teleport.LUMBRIDGE_SPELL, "run east through the Al Kharid toll gate")),
	GUILD_CACTUS("Farming Guild cactus", PatchType.CACTUS, Requirement.FARMING_45, 1264, 3745, r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 7904, "CIR", "Farming Guild bank",
		walk("Walk from the guild bush patch", "GUILD_BUSH"),
		beatsFairy(Teleport.FARMING_CAPE, "east wing"),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild, east wing"),
		via(Teleport.FAIRY_CIR, "run south-west, then the guild's east wing")),

	// ---- Calquat (sapling; check health, pay the gardener to clear)
	TAI_BWO_WANNAI_CALQUAT("Tai Bwo Wannai calquat", PatchType.CALQUAT, Requirement.NONE, 2795, 3102, r(11056), 4771, "CKR", null,
		via(Teleport.FAIRY_CKR, "run north to Tai Bwo Wannai"),
		house(HouseLocation.BRIMHAVEN, "run south to Tai Bwo Wannai", false),
		via(Teleport.BRIMHAVEN_TAB, "run south to Tai Bwo Wannai")),
	KASTORI_CALQUAT("Kastori calquat", PatchType.CALQUAT, Requirement.CHILDREN_OF_THE_SUN, 1366, 3033, r(5423, 5167, 5424), 4771, null, null,
		walk("Walk from the Kastori fruit tree", "KASTORI_FRUIT"),
		via(Teleport.PENDANT_OF_ATES, "Kastori"),
		via(Teleport.QUETZAL_WHISTLE, "Kastori")),

	// ---- Spirit trees (sapling; check health, dig up to replant). Tiles: Shortest Path's patch boxes.
	PORT_SARIM_SPIRIT("Port Sarim spirit tree", PatchType.SPIRIT_TREE, Requirement.NONE, 3060, 3258, r(12082, 12083), 4771, null, "Draynor Village bank",
		via(Teleport.EXPLORERS_RING, "cabbage patch teleport, run south"),
		via(Teleport.FALADOR_TAB, "run south to Port Sarim"),
		via(Teleport.FALADOR_SPELL, "run south to Port Sarim")),
	ETCETERIA_SPIRIT("Etceteria spirit tree", PatchType.SPIRIT_TREE, Requirement.FREMENNIK_TRIALS, 2613, 3857, r(10300), 4772, null, null,
		walk("Walk east from the Etceteria bush", "ETCETERIA_BUSH"),
		via(Teleport.FAIRY_CIP, "run east over the bridge to Etceteria"),
		via(Teleport.RING_OF_WEALTH, "run east over the bridge to Etceteria")),
	BRIMHAVEN_SPIRIT("Brimhaven spirit tree", PatchType.SPIRIT_TREE, Requirement.NONE, 2802, 3203, r(11058, 11057), 4772, null, "Ardougne South bank",
		walk("Walk east from the Brimhaven fruit tree", "BRIMHAVEN_FRUIT"),
		house(HouseLocation.BRIMHAVEN, "run north-east", false),
		via(Teleport.BRIMHAVEN_TAB, "run east"),
		via(Teleport.ARDOUGNE_TAB, "boat from Ardougne docks to Brimhaven (30 coins), run east"),
		via(Teleport.ARDOUGNE_SPELL, "boat from Ardougne docks to Brimhaven (30 coins), run east")),
	HOSIDIUS_SPIRIT("Hosidius spirit tree", PatchType.SPIRIT_TREE, Requirement.NONE, 1693, 3542, r(6967, 6711), 7904, null, "Hosidius kitchen bank",
		walk("Walk west from the Hosidius herb patch", "HOSIDIUS_HERB"),
		via(Teleport.XERICS_TALISMAN, "Xeric's Glade, run south-west"),
		via(Teleport.KHAREDSTS_MEMOIRS, "Hosidius, run south-west"),
		house(HouseLocation.HOSIDIUS, "run north-west", false),
		via(Teleport.KOUREND_SPELL, "run south")),
	GUILD_SPIRIT("Farming Guild spirit tree", PatchType.SPIRIT_TREE, Requirement.FARMING_85, 1253, 3750, r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 4771, "CIR", "Farming Guild bank",
		walk("Into the guild's top tier", "FARMING_GUILD_FRUIT"),
		beatsFairy(Teleport.FARMING_CAPE, "top tier"),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild, top tier"),
		via(Teleport.FAIRY_CIR, "run south-west, then the guild's top tier")),

	// ---- Farming Guild top tier: celastrus and redwood
	GUILD_CELASTRUS("Farming Guild celastrus", PatchType.CELASTRUS, Requirement.FARMING_85, 1244, 3752, r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 7910, "CIR", "Farming Guild bank",
		walk("Next to the guild fruit tree", "FARMING_GUILD_FRUIT"),
		beatsFairy(Teleport.FARMING_CAPE, "top tier"),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild, top tier"),
		via(Teleport.FAIRY_CIR, "run south-west, then the guild's top tier")),
	GUILD_REDWOOD("Farming Guild redwood", PatchType.REDWOOD, Requirement.FARMING_85, 1233, 3754, r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 7907, "CIR", "Farming Guild bank",
		walk("West side of the guild's top tier", "FARMING_GUILD_FRUIT"),
		beatsFairy(Teleport.FARMING_CAPE, "top tier, west side"),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild, top tier, west side"),
		via(Teleport.FAIRY_CIR, "run south-west, then the guild's top tier")),

	// ---- Allotments (3 seeds each; the gardener can protect them)
	FALADOR_ALLOTMENT_A("Falador allotment (north-west)", PatchType.ALLOTMENT, Requirement.NONE, 3049, 3312,
		r(12083), 4771, null, "Falador East bank",
		walk("Next to the herb patch", "FALADOR_HERB"),
		via(Teleport.EXPLORERS_RING, "cabbage patch teleport, run north"),
		walk("Walk south from Falador park", "FALADOR_TREE"),
		via(Teleport.FALADOR_TAB, "run south-east"),
		via(Teleport.FALADOR_SPELL, "run south-east")),
	FALADOR_ALLOTMENT_B("Falador allotment (south-east)", PatchType.ALLOTMENT, Requirement.NONE, 3055, 3306,
		r(12083), 4772, null, "Falador East bank",
		walk("The other allotment, across the patch", "FALADOR_ALLOTMENT_A"),
		walk("Next to the herb patch", "FALADOR_HERB"),
		via(Teleport.EXPLORERS_RING, "cabbage patch teleport, run north"),
		walk("Walk south from Falador park", "FALADOR_TREE"),
		via(Teleport.FALADOR_TAB, "run south-east"),
		via(Teleport.FALADOR_SPELL, "run south-east")),
	CATHERBY_ALLOTMENT_A("Catherby allotment (north)", PatchType.ALLOTMENT, Requirement.NONE, 2807, 3465,
		r(11062, 11061, 11317, 11318), 4771, null, "Catherby bank",
		walk("Next to the herb patch", "CATHERBY_HERB"),
		via(Teleport.CAMELOT_TAB, "run east to Catherby"),
		via(Teleport.CAMELOT_SPELL, "run east to Catherby")),
	CATHERBY_ALLOTMENT_B("Catherby allotment (south)", PatchType.ALLOTMENT, Requirement.NONE, 2813, 3459,
		r(11062, 11061, 11317, 11318), 4772, null, "Catherby bank",
		walk("The other allotment, across the patch", "CATHERBY_ALLOTMENT_A"),
		walk("Next to the herb patch", "CATHERBY_HERB"),
		via(Teleport.CAMELOT_TAB, "run east to Catherby"),
		via(Teleport.CAMELOT_SPELL, "run east to Catherby")),
	ARDOUGNE_ALLOTMENT_A("Ardougne allotment (north)", PatchType.ALLOTMENT, Requirement.NONE, 2660, 3378,
		r(10548), 4771, "BLR", "Ardougne North bank",
		walk("Next to the herb patch", "ARDOUGNE_HERB"),
		beatsFairy(Teleport.ARDOUGNE_CLOAK, "farm teleport (cloak 2+)"),
		beatsFairy(Teleport.ARDOUGNE_TAB, "run north-east"),
		beatsFairy(Teleport.ARDOUGNE_SPELL, "run north-east"),
		via(Teleport.FAIRY_BLR, "run west")),
	ARDOUGNE_ALLOTMENT_B("Ardougne allotment (south)", PatchType.ALLOTMENT, Requirement.NONE, 2666, 3372,
		r(10548), 4772, "BLR", "Ardougne North bank",
		walk("The other allotment, across the patch", "ARDOUGNE_ALLOTMENT_A"),
		walk("Next to the herb patch", "ARDOUGNE_HERB"),
		beatsFairy(Teleport.ARDOUGNE_CLOAK, "farm teleport (cloak 2+)"),
		beatsFairy(Teleport.ARDOUGNE_TAB, "run north-east"),
		beatsFairy(Teleport.ARDOUGNE_SPELL, "run north-east"),
		via(Teleport.FAIRY_BLR, "run west")),
	MORYTANIA_ALLOTMENT_A("Morytania allotment (north-west)", PatchType.ALLOTMENT, Requirement.PRIEST_IN_PERIL, 3595, 3527,
		r(14391, 14390), 4771, "ALQ", "Port Phasmatys bank",
		walk("Next to the herb patch", "MORYTANIA_HERB"),
		beatsFairy(Teleport.ECTOPHIAL, "run west"),
		via(Teleport.FAIRY_ALQ, "run north"),
		via(Teleport.FENKENSTRAIN_TAB, "run east")),
	MORYTANIA_ALLOTMENT_B("Morytania allotment (south-east)", PatchType.ALLOTMENT, Requirement.PRIEST_IN_PERIL, 3601, 3521,
		r(14391, 14390), 4772, "ALQ", "Port Phasmatys bank",
		walk("The other allotment, across the patch", "MORYTANIA_ALLOTMENT_A"),
		walk("Next to the herb patch", "MORYTANIA_HERB"),
		beatsFairy(Teleport.ECTOPHIAL, "run west"),
		via(Teleport.FAIRY_ALQ, "run north"),
		via(Teleport.FENKENSTRAIN_TAB, "run east")),
	HOSIDIUS_ALLOTMENT_A("Hosidius allotment (north-east)", PatchType.ALLOTMENT, Requirement.NONE, 1726, 3561,
		r(6967, 6711), 4771, "AKR", "Hosidius kitchen bank",
		walk("Next to the herb patch", "HOSIDIUS_HERB"),
		via(Teleport.XERICS_TALISMAN, "Xeric's Glade, run south-west"),
		via(Teleport.KHAREDSTS_MEMOIRS, "Hosidius, run south-east"),
		house(HouseLocation.HOSIDIUS, "run north to the farming patches", false),
		via(Teleport.FAIRY_AKR, "run west"),
		via(Teleport.KOUREND_SPELL, "run south-east")),
	HOSIDIUS_ALLOTMENT_B("Hosidius allotment (south-west)", PatchType.ALLOTMENT, Requirement.NONE, 1732, 3555,
		r(6967, 6711), 4772, "AKR", "Hosidius kitchen bank",
		walk("The other allotment, across the patch", "HOSIDIUS_ALLOTMENT_A"),
		walk("Next to the herb patch", "HOSIDIUS_HERB"),
		via(Teleport.XERICS_TALISMAN, "Xeric's Glade, run south-west"),
		via(Teleport.KHAREDSTS_MEMOIRS, "Hosidius, run south-east"),
		house(HouseLocation.HOSIDIUS, "run north to the farming patches", false),
		via(Teleport.FAIRY_AKR, "run west"),
		via(Teleport.KOUREND_SPELL, "run south-east")),
	VARLAMORE_ALLOTMENT_A("Varlamore (Ortus Farm) allotment (north)", PatchType.ALLOTMENT, Requirement.CHILDREN_OF_THE_SUN, 1584, 3104,
		r(6192, 6191, 6193, 6447, 6448, 6449), 4771, "AJP", "Civitas illa Fortis bank",
		walk("Next to the herb patch", "VARLAMORE_HERB"),
		via(Teleport.QUETZAL_WHISTLE, "Hunter Guild, run north"),
		via(Teleport.HUNTER_CAPE, "Hunter Guild, run north-east"),
		via(Teleport.FAIRY_AJP, "run north-west"),
		via(Teleport.CIVITAS_SPELL, "quetzal to the Hunter Guild, run north")),
	VARLAMORE_ALLOTMENT_B("Varlamore (Ortus Farm) allotment (south)", PatchType.ALLOTMENT, Requirement.CHILDREN_OF_THE_SUN, 1590, 3098,
		r(6192, 6191, 6193, 6447, 6448, 6449), 4772, "AJP", "Civitas illa Fortis bank",
		walk("The other allotment, across the patch", "VARLAMORE_ALLOTMENT_A"),
		walk("Next to the herb patch", "VARLAMORE_HERB"),
		via(Teleport.QUETZAL_WHISTLE, "Hunter Guild, run north"),
		via(Teleport.HUNTER_CAPE, "Hunter Guild, run north-east"),
		via(Teleport.FAIRY_AJP, "run north-west"),
		via(Teleport.CIVITAS_SPELL, "quetzal to the Hunter Guild, run north")),
	GUILD_ALLOTMENT_A("Farming Guild allotment (north)", PatchType.ALLOTMENT, Requirement.FARMING_45, 1266, 3733,
		r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 4773, "CIR", "Farming Guild bank",
		walk("Walk from the guild herb patch", "FARMING_GUILD_HERB"),
		walk("Walk from the guild tree patch", "FARMING_GUILD_TREE"),
		beatsFairy(Teleport.FARMING_CAPE, "run east"),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild, run east"),
		via(Teleport.FAIRY_CIR, "run south-west, then into the guild")),
	GUILD_ALLOTMENT_B("Farming Guild allotment (south)", PatchType.ALLOTMENT, Requirement.FARMING_45, 1272, 3727,
		r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 4774, "CIR", "Farming Guild bank",
		walk("The other allotment, across the patch", "GUILD_ALLOTMENT_A"),
		walk("Walk from the guild herb patch", "FARMING_GUILD_HERB"),
		walk("Walk from the guild tree patch", "FARMING_GUILD_TREE"),
		beatsFairy(Teleport.FARMING_CAPE, "run east"),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild, run east"),
		via(Teleport.FAIRY_CIR, "run south-west, then into the guild")),

	// ---- Flowers (1 seed, can't be protected). Each sits between its site's two allotments;
	// built-in tiles are approximate, the exact patch is found from the game once you're there.
	FALADOR_FLOWER("Falador flower", PatchType.FLOWER, Requirement.NONE, 3052, 3309,
		r(12083), 4773, null, "Falador East bank",
		walk("Between the allotments", "FALADOR_ALLOTMENT_A"),
		walk("Next to the herb patch", "FALADOR_HERB"),
		via(Teleport.EXPLORERS_RING, "cabbage patch teleport, run north"),
		walk("Walk south from Falador park", "FALADOR_TREE"),
		via(Teleport.FALADOR_TAB, "run south-east"),
		via(Teleport.FALADOR_SPELL, "run south-east")),
	CATHERBY_FLOWER("Catherby flower", PatchType.FLOWER, Requirement.NONE, 2810, 3462,
		r(11062, 11061, 11317, 11318), 4773, null, "Catherby bank",
		walk("Between the allotments", "CATHERBY_ALLOTMENT_A"),
		walk("Next to the herb patch", "CATHERBY_HERB"),
		via(Teleport.CAMELOT_TAB, "run east to Catherby"),
		via(Teleport.CAMELOT_SPELL, "run east to Catherby")),
	ARDOUGNE_FLOWER("Ardougne flower", PatchType.FLOWER, Requirement.NONE, 2663, 3375,
		r(10548), 4773, "BLR", "Ardougne North bank",
		walk("Between the allotments", "ARDOUGNE_ALLOTMENT_A"),
		walk("Next to the herb patch", "ARDOUGNE_HERB"),
		beatsFairy(Teleport.ARDOUGNE_CLOAK, "farm teleport (cloak 2+)"),
		beatsFairy(Teleport.ARDOUGNE_TAB, "run north-east"),
		beatsFairy(Teleport.ARDOUGNE_SPELL, "run north-east"),
		via(Teleport.FAIRY_BLR, "run west")),
	MORYTANIA_FLOWER("Morytania flower", PatchType.FLOWER, Requirement.PRIEST_IN_PERIL, 3598, 3524,
		r(14391, 14390), 4773, "ALQ", "Port Phasmatys bank",
		walk("Between the allotments", "MORYTANIA_ALLOTMENT_A"),
		walk("Next to the herb patch", "MORYTANIA_HERB"),
		beatsFairy(Teleport.ECTOPHIAL, "run west"),
		via(Teleport.FAIRY_ALQ, "run north"),
		via(Teleport.FENKENSTRAIN_TAB, "run east")),
	HOSIDIUS_FLOWER("Hosidius flower", PatchType.FLOWER, Requirement.NONE, 1729, 3558,
		r(6967, 6711), 4773, "AKR", "Hosidius kitchen bank",
		walk("Between the allotments", "HOSIDIUS_ALLOTMENT_A"),
		walk("Next to the herb patch", "HOSIDIUS_HERB"),
		via(Teleport.XERICS_TALISMAN, "Xeric's Glade, run south-west"),
		via(Teleport.KHAREDSTS_MEMOIRS, "Hosidius, run south-east"),
		house(HouseLocation.HOSIDIUS, "run north to the farming patches", false),
		via(Teleport.FAIRY_AKR, "run west"),
		via(Teleport.KOUREND_SPELL, "run south-east")),
	VARLAMORE_FLOWER("Varlamore (Ortus Farm) flower", PatchType.FLOWER, Requirement.CHILDREN_OF_THE_SUN, 1587, 3101,
		r(6192, 6191, 6193, 6447, 6448, 6449), 4773, "AJP", "Civitas illa Fortis bank",
		walk("Between the allotments", "VARLAMORE_ALLOTMENT_A"),
		walk("Next to the herb patch", "VARLAMORE_HERB"),
		via(Teleport.QUETZAL_WHISTLE, "Hunter Guild, run north"),
		via(Teleport.HUNTER_CAPE, "Hunter Guild, run north-east"),
		via(Teleport.FAIRY_AJP, "run north-west"),
		via(Teleport.CIVITAS_SPELL, "quetzal to the Hunter Guild, run north")),
	GUILD_FLOWER("Farming Guild flower", PatchType.FLOWER, Requirement.FARMING_45, 1269, 3730,
		r(4922, 4665, 4666, 4667, 4921, 4923, 5177, 5178, 5179), 7906, "CIR", "Farming Guild bank",
		walk("Between the allotments", "GUILD_ALLOTMENT_A"),
		walk("Walk from the guild herb patch", "FARMING_GUILD_HERB"),
		walk("Walk from the guild tree patch", "FARMING_GUILD_TREE"),
		beatsFairy(Teleport.FARMING_CAPE, "run east"),
		via(Teleport.SKILLS_NECKLACE, "Farming Guild, run east"),
		via(Teleport.FAIRY_CIR, "run south-west, then into the guild")),
	KASTORI_FLOWER("Kastori flower", PatchType.FLOWER, Requirement.CHILDREN_OF_THE_SUN, 1356, 3045,
		r(5423, 5167, 5424), 4773, null, null,
		walk("Walk from the Kastori fruit tree", "KASTORI_FRUIT"),
		via(Teleport.PENDANT_OF_ATES, "Kastori"),
		via(Teleport.QUETZAL_WHISTLE, "Kastori"),
		walk("Quetzal from the Hunter Guild to Kastori", "VARLAMORE_HERB"));

	private static int[] r(int... regions)
	{
		return regions;
	}

	/** One way into a patch: a teleport plus directions, or a walk (teleport == null). */
	public static final class Route
	{
		public final Teleport teleport;
		public final String note;
		/** For walks: the patch you must have just done (enum name), or null for "from anywhere". */
		public final String anchor;
		/** True if this beats chaining a fairy ring (e.g. Ardougne cloak straight to the patch). */
		public final boolean beatsFairy;
		/**
		 * For house teleports: the house portal this route starts from. The spell and tablet only
		 * work when your house is there; the Construction cape can pick any portal.
		 */
		public final HouseLocation house;

		Route(Teleport teleport, String note, String anchor, boolean beatsFairy)
		{
			this(teleport, note, anchor, beatsFairy, null);
		}

		Route(Teleport teleport, String note, String anchor, boolean beatsFairy, HouseLocation house)
		{
			this.teleport = teleport;
			this.note = note;
			this.anchor = anchor;
			this.beatsFairy = beatsFairy;
			this.house = house;
		}

		/** Whether this route works with your house where it is. */
		public boolean houseOk(HouseLocation yours)
		{
			return house == null || teleport == Teleport.HOUSE_CAPE || house == yours;
		}

		public FarmPatch anchorPatch()
		{
			return anchor == null ? null : FarmPatch.valueOf(anchor);
		}

		public String describe()
		{
			if (teleport == null)
			{
				return note;
			}
			return teleport.getLabel() + (note == null ? "" : ", " + note);
		}
	}

	private static Route via(Teleport t, String note)
	{
		return new Route(t, note, null, false);
	}

	private static Route beatsFairy(Teleport t, String note)
	{
		return new Route(t, note, null, true);
	}

	private static Route walk(String note, String anchor)
	{
		return new Route(null, note, anchor, false);
	}

	/** Placeholder for "from the house portal at X": expanded into cape, spell and tablet routes. */
	private static Route house(HouseLocation where, String note, boolean beatsFairy)
	{
		return new Route(null, note, null, beatsFairy, where);
	}

	/** House placeholders become three real routes: Construction cape first (free), then spell, then tablet. */
	private static List<Route> expand(Route[] routes)
	{
		List<Route> out = new ArrayList<>();
		for (Route r : routes)
		{
			if (r.teleport == null && r.house != null)
			{
				// The cape picks the portal directly; the spell and tablet land in your house
				// (unless your house options say "outside"), so you leave by its portal.
				String where = r.house.getDisplayName();
				out.add(new Route(Teleport.HOUSE_CAPE, where + " house portal, " + r.note, null, r.beatsFairy, r.house));
				String leave = "leave by the " + where + " portal, " + r.note;
				out.add(new Route(Teleport.HOUSE_SPELL, leave, null, r.beatsFairy, r.house));
				out.add(new Route(Teleport.HOUSE_TAB, leave, null, r.beatsFairy, r.house));
			}
			else
			{
				out.add(r);
			}
		}
		return Collections.unmodifiableList(out);
	}

	public enum Requirement
	{
		NONE(""),
		PRIEST_IN_PERIL("Priest in Peril"),
		MY_ARMS_BIG_ADVENTURE("My Arm's Big Adventure"),
		MAKING_FRIENDS_WITH_MY_ARM("Making Friends with My Arm"),
		CHILDREN_OF_THE_SUN("Children of the Sun"),
		BONE_VOYAGE("Bone Voyage"),
		FREMENNIK_TRIALS("The Fremennik Trials"),
		MOURNINGS_END("Mourning's End Part I"),
		FARMING_45("45 Farming"),
		FARMING_65("65 Farming"),
		FARMING_85("85 Farming"),
		MORYTANIA_ELITE("Morytania Elite Diary");

		private final String description;

		Requirement(String description)
		{
			this.description = description;
		}

		public String getDescription()
		{
			return description;
		}

		/** The quest name, for requirements that are quests. */
		public String questName()
		{
			switch (this)
			{
				case PRIEST_IN_PERIL:
				case MY_ARMS_BIG_ADVENTURE:
				case MAKING_FRIENDS_WITH_MY_ARM:
				case CHILDREN_OF_THE_SUN:
				case BONE_VOYAGE:
				case FREMENNIK_TRIALS:
				case MOURNINGS_END:
					return description;
				default:
					return null;
			}
		}
	}

	private final String displayName;
	private final PatchType type;
	private final Requirement requirement;
	private final int x;
	private final int y;
	private final int[] regions;
	private final int varbit;
	private final String nearbyRing;
	private final String bank;
	private final List<Route> routes;

	FarmPatch(String displayName, PatchType type, Requirement requirement, int x, int y, int[] regions,
		int varbit, String nearbyRing, String bank, Route... routes)
	{
		this.displayName = displayName;
		this.type = type;
		this.requirement = requirement;
		this.x = x;
		this.y = y;
		this.regions = regions;
		this.varbit = varbit;
		this.nearbyRing = nearbyRing;
		this.bank = bank;
		this.routes = expand(routes);
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public PatchType getType()
	{
		return type;
	}

	public Requirement getRequirement()
	{
		return requirement;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getVarbit()
	{
		return varbit;
	}

	/** The main region, used as Time Tracking's storage key. */
	public int getRegionId()
	{
		return regions[0];
	}

	public boolean inRegion(int regionId)
	{
		for (int r : regions)
		{
			if (r == regionId)
			{
				return true;
			}
		}
		return false;
	}

	/** Fairy ring code you can walk to from this patch, or null. */
	public String getNearbyRing()
	{
		return nearbyRing;
	}

	/** Nearest bank, or null if none is close. */
	public String getBank()
	{
		return bank;
	}

	public List<Route> getRoutes()
	{
		return routes;
	}

	/** Same formula the game uses. */
	public static int regionOf(int x, int y)
	{
		return ((x >> 6) << 8) | (y >> 6);
	}

	/**
	 * The patch of this type you're at: matched by the game's region first (exact), then the
	 * closest by tile when several share a region (Fossil Island). Null if none.
	 */
	public static FarmPatch at(int px, int py, PatchType type)
	{
		int region = regionOf(px, py);
		FarmPatch best = null;
		int bestDist = Integer.MAX_VALUE;
		for (FarmPatch p : values())
		{
			if ((type != null && p.type != type) || !p.inRegion(region))
			{
				continue;
			}
			int d = Math.max(Math.abs(p.x - px), Math.abs(p.y - py));
			if (d < bestDist)
			{
				best = p;
				bestDist = d;
			}
		}
		return best;
	}
}
