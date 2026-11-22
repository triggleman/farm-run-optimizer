package com.herbrunhelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Works out the cheapest way to carry the runes a run's spells need.
 *
 * <p>It knows about the things the game lets you use instead of carrying every rune:
 * <ul>
 *   <li>staves, battlestaves and tomes that supply a rune type for free (a smoke battlestaff
 *       covers air and fire, a lava battlestaff covers earth and fire, and so on);</li>
 *   <li>the rune pouch, which carries three rune types in one inventory slot (four for the
 *       divine rune pouch);</li>
 *   <li>combination runes (dust, mist, mud, lava, steam, smoke, sunfire, aether), which count
 *       as both of their parts.</li>
 * </ul>
 *
 * <p>Everything is by exact item name, like the rest of the planner, so it can be unit tested
 * without RuneLite. Pure Java.
 */
public final class RuneSupply
{
	/** Everything the calculation needs. The plugin fills this from the game. */
	public static final class Context
	{
		/** Spell runes the route needs, by rune name (e.g. "Law rune" -> 4). */
		public Map<String, Integer> need = Collections.emptyMap();
		/** Everything owned: bank + inventory + equipment. */
		public Map<String, Integer> owned = Collections.emptyMap();
		/** Carried: inventory + equipment (not the bank, not the pouch). */
		public Map<String, Integer> carried = Collections.emptyMap();
		/** Worn equipment. */
		public Map<String, Integer> equipped = Collections.emptyMap();
		/** Runes inside the rune pouch, by name. */
		public Map<String, Integer> pouch = Collections.emptyMap();
		public boolean pouchCarried;
		public String pouchName = "Rune pouch";
		/** Three for a rune pouch, four for a divine rune pouch. */
		public int pouchCapacity = 3;
		/** True when the run includes herbs, so magic secateurs have extra value. */
		public boolean herbs;
		public boolean ownsMagicSecateurs;
	}

	/** One line to show in "Take with you". */
	public static final class Result
	{
		public final List<RunPlan.Withdrawal> withdrawals = new ArrayList<>();
		/** Shown in the bank's Farm run view but not required (the pouch when it's already carried). */
		public final List<RunPlan.Withdrawal> optional = new ArrayList<>();
		public final List<String> notes = new ArrayList<>();
		public final List<String> warnings = new ArrayList<>();
		public String summary = "";
	}

	/**
	 * Staves, battlestaves and tomes that give unlimited runes. Prefix -> the runes it covers.
	 * Prefixes are lower case and matched with startsWith, so charged/pretty variants count too.
	 */
	private static final String[][] INFINITE_RUNE_ITEMS = {
		{"staff of air", "Air rune"},
		{"air battlestaff", "Air rune"},
		{"mystic air staff", "Air rune"},
		{"mystic air battlestaff", "Air rune"},
		{"staff of water", "Water rune"},
		{"water battlestaff", "Water rune"},
		{"mystic water staff", "Water rune"},
		{"mystic water battlestaff", "Water rune"},
		{"kodai wand", "Water rune"},
		{"tome of water", "Water rune"},
		{"staff of earth", "Earth rune"},
		{"earth battlestaff", "Earth rune"},
		{"mystic earth staff", "Earth rune"},
		{"mystic earth battlestaff", "Earth rune"},
		{"tome of earth", "Earth rune"},
		{"staff of fire", "Fire rune"},
		{"fire battlestaff", "Fire rune"},
		{"mystic fire staff", "Fire rune"},
		{"mystic fire battlestaff", "Fire rune"},
		{"tome of fire", "Fire rune"},
		{"lava battlestaff", "Earth rune", "Fire rune"},
		{"mystic lava staff", "Earth rune", "Fire rune"},
		{"mud battlestaff", "Water rune", "Earth rune"},
		{"mystic mud staff", "Water rune", "Earth rune"},
		{"steam battlestaff", "Water rune", "Fire rune"},
		{"mystic steam staff", "Water rune", "Fire rune"},
		{"twinflame staff", "Water rune", "Fire rune"},
		{"smoke battlestaff", "Air rune", "Fire rune"},
		{"mystic smoke staff", "Air rune", "Fire rune"},
		{"mist battlestaff", "Air rune", "Water rune"},
		{"mystic mist staff", "Air rune", "Water rune"},
		{"dust battlestaff", "Air rune", "Earth rune"},
		{"mystic dust staff", "Air rune", "Earth rune"},
		{"bryophyta's staff", "Nature rune"},
		{"nature's reprisal", "Nature rune"},
	};

	/** Combination runes: names and the base runes they count as. */
	private static final String[][] COMBINATION_RUNES = {
		{"dust rune", "Air rune", "Earth rune"},
		{"mist rune", "Air rune", "Water rune"},
		{"mud rune", "Water rune", "Earth rune"},
		{"lava rune", "Earth rune", "Fire rune"},
		{"steam rune", "Water rune", "Fire rune"},
		{"smoke rune", "Air rune", "Fire rune"},
		{"sunfire rune", "Fire rune"},
		{"aether rune", "Soul rune"},
	};

	private RuneSupply()
	{
	}

	public static Result plan(Context ctx)
	{
		Result result = new Result();
		Map<String, Integer> need = new LinkedHashMap<>();
		ctx.need.forEach((rune, qty) ->
		{
			if (rune != null && qty != null && qty > 0)
			{
				need.merge(rune, qty, Integer::sum);
			}
		});
		if (need.isEmpty())
		{
			return result;
		}

		// Build what the player already carries, including anything inside a carried pouch.
		Map<String, Integer> carried = new HashMap<>(ctx.carried);
		if (ctx.pouchCarried)
		{
			ctx.pouch.forEach((rune, qty) -> carried.merge(rune, qty, Integer::sum));
		}

		// 1. A staff or tome that covers needed runes is better than carrying those runes.
		String staff = chooseStaff(ctx, need, carried);
		Set<String> free = new LinkedHashSet<>();
		if (staff != null)
		{
			Set<String> covers = staffRunes(staff);
			covers.retainAll(need.keySet());
			if (!covers.isEmpty())
			{
				boolean equipped = ctx.equipped.getOrDefault(staff, 0) > 0;
				// Worn items take no inventory slot; one in the bank is withdrawn and wielded.
				result.withdrawals.add(new RunPlan.Withdrawal(staff, 1,
					equipped ? "worn" : "wield it", 0));
				result.notes.add(staff + " supplies " + join(covers) + " for free");
				free.addAll(covers);
				need.keySet().removeAll(covers);
			}
		}

		// 2. Combination runes, first the ones already carried, then ones in the bank.
		Map<String, Integer> shortfall = new LinkedHashMap<>();
		need.forEach((rune, qty) -> shortfall.put(rune, Math.max(0, qty - carried.getOrDefault(rune, 0))));
		useCombinationRunes(ctx, carried, shortfall, result, true);
		useCombinationRunes(ctx, ctx.owned, shortfall, result, false);

		// 3. The rune pouch: one slot for up to three (or four) rune types.
		int remainingTypes = 0;
		for (int qty : shortfall.values())
		{
			if (qty > 0)
			{
				remainingTypes++;
			}
		}
		if (ctx.pouchCarried && !ctx.pouch.isEmpty())
		{
			List<String> inside = new ArrayList<>();
			ctx.pouch.forEach((rune, qty) ->
			{
				if (qty > 0)
				{
					inside.add(qty + " " + rune.toLowerCase(Locale.ROOT));
				}
			});
			if (!inside.isEmpty())
			{
				result.notes.add(ctx.pouchName + " already holds " + String.join(", ", inside));
			}
		}
		boolean pouchUseful = ctx.pouchCarried || remainingTypes >= 2;
		boolean recommendPouch = !ctx.pouchCarried && pouchUseful && ownedPouch(ctx) != null;
		if (recommendPouch)
		{
			result.withdrawals.add(new RunPlan.Withdrawal(ownedPouch(ctx), 1,
				"put the runes inside (one slot)", 1));
			result.notes.add(ownedPouch(ctx) + ": one slot for up to " + ctx.pouchCapacity + " rune types");
		}

		// 4. Whatever is still missing is carried as base runes.
		int pouchFree = ctx.pouchCarried
			? Math.max(0, ctx.pouchCapacity - ctx.pouch.size())
			: recommendPouch ? ctx.pouchCapacity : 0;
		for (Map.Entry<String, Integer> e : shortfall.entrySet())
		{
			String rune = e.getKey();
			int qty = e.getValue();
			if (qty <= 0)
			{
				continue;
			}
			boolean pouchSlot = pouchFree > 0;
			String note;
			int slots;
			if (pouchSlot)
			{
				slots = 0;
				note = ctx.pouchCarried ? "in your rune pouch" : "put in the rune pouch";
				pouchFree--;
			}
			else if (ctx.pouchCarried)
			{
				slots = 1;
				note = "rune pouch is full";
			}
			else
			{
				slots = 1;
				note = "spell runes";
			}
			int total = need.getOrDefault(rune, qty);
			result.withdrawals.add(new RunPlan.Withdrawal(rune, total, qty, note, slots));
			int have = ctx.owned.getOrDefault(rune, 0);
			if (have < qty)
			{
				result.warnings.add("Short " + (qty - have) + " " + rune.toLowerCase(Locale.ROOT)
					+ " (a staff, tome, rune pouch or combination rune may cover it).");
			}
		}

		if (ctx.pouchCarried && ctx.pouch.size() >= ctx.pouchCapacity && remainingTypes > ctx.pouch.size())
		{
			result.warnings.add("Your rune pouch is full. Make room for "
				+ (remainingTypes - ctx.pouch.size()) + " more rune type"
				+ (remainingTypes - ctx.pouch.size() == 1 ? "" : "s") + ".");
		}

		// The pouch is worth showing even when it isn't a required withdrawal, so it has a line
		// in the bank's Farm run view (a carried pouch is shown ticked).
		String pouch = ownedPouch(ctx);
		if (pouch != null && !need.isEmpty() && !recommendPouch)
		{
			boolean carriedPouch = ctx.carried.getOrDefault(pouch, 0) > 0
				|| (ctx.pouchCarried && pouch.equals(ctx.pouchName));
			result.optional.add(new RunPlan.Withdrawal(pouch, 1, carriedPouch ? 0 : 1,
				carriedPouch ? "carried; one slot for its runes" : "put the spell runes inside; one slot",
				carriedPouch ? 0 : 1, RunPlan.Group.RUNES, false));
		}

		result.summary = summary(ctx, free, shortfall, staff, recommendPouch);
		return result;
	}

	/** The staff/tome to wield, or null when carrying runes is at least as good. */
	private static String chooseStaff(Context ctx, Map<String, Integer> need, Map<String, Integer> carried)
	{
		String best = null;
		int bestScore = 0;
		for (String item : ctx.owned.keySet())
		{
			Set<String> covers = new LinkedHashSet<>(staffRunes(item));
			covers.retainAll(need.keySet());
			// A rune the pouch or inventory already covers doesn't need a staff.
			covers.removeIf(rune -> carried.getOrDefault(rune, 0) >= need.getOrDefault(rune, 0));
			if (covers.isEmpty())
			{
				continue;
			}
			boolean equipped = ctx.equipped.getOrDefault(item, 0) > 0;
			int score = covers.size() * 10 + (equipped ? 1 : 0);
			// On a herb run, magic secateurs are worth more than the slots a staff saves. Only
			// use a staff the player already has on; otherwise leave the weapon slot for them.
			if (!equipped && ctx.herbs && ctx.ownsMagicSecateurs)
			{
				continue;
			}
			if (score > bestScore)
			{
				bestScore = score;
				best = item;
			}
		}
		return best;
	}

	/**
	 * Spends combination runes on the biggest shortfall they can cover. When {@code carriedOnly}
	 * is false, runes in the bank are added to "take with you" as well.
	 */
	private static void useCombinationRunes(Context ctx, Map<String, Integer> source,
		Map<String, Integer> shortfall, Result result, boolean carriedOnly)
	{
		List<String[]> combos = new ArrayList<>();
		for (String[] combo : COMBINATION_RUNES)
		{
			int covers = 0;
			for (int i = 1; i < combo.length; i++)
			{
				if (shortfall.getOrDefault(combo[i], 0) > 0)
				{
					covers += shortfall.get(combo[i]);
				}
			}
			if (covers > 0)
			{
				combos.add(combo);
			}
		}
		combos.sort(Comparator.comparingInt((String[] c) ->
		{
			int total = 0;
			for (int i = 1; i < c.length; i++)
			{
				total += shortfall.getOrDefault(c[i], 0);
			}
			return total;
		}).reversed());

		for (String[] combo : combos)
		{
			int available = 0;
			for (Map.Entry<String, Integer> e : source.entrySet())
			{
				if (e.getKey().toLowerCase(Locale.ROOT).startsWith(combo[0]))
				{
					available += e.getValue();
				}
			}
			if (!carriedOnly && ctx.pouchCarried)
			{
				for (Map.Entry<String, Integer> e : ctx.pouch.entrySet())
				{
					if (e.getKey().toLowerCase(Locale.ROOT).startsWith(combo[0]))
					{
						available += e.getValue();
					}
				}
			}
			if (available <= 0)
			{
				continue;
			}
			int largest = 0;
			int needy = 0;
			for (int i = 1; i < combo.length; i++)
			{
				int qty = shortfall.getOrDefault(combo[i], 0);
				largest = Math.max(largest, qty);
				if (qty > 0)
				{
					needy++;
				}
			}
			// A banked combination rune only earns its line when it covers more than one rune.
			if (!carriedOnly && needy < 2)
			{
				continue;
			}
			int use = Math.min(available, largest);
			if (use <= 0)
			{
				continue;
			}
			int alreadyCarried = 0;
			for (Map.Entry<String, Integer> e : ctx.carried.entrySet())
			{
				if (e.getKey().toLowerCase(Locale.ROOT).startsWith(combo[0]))
				{
					alreadyCarried += e.getValue();
				}
			}
			if (ctx.pouchCarried)
			{
				for (Map.Entry<String, Integer> e : ctx.pouch.entrySet())
				{
					if (e.getKey().toLowerCase(Locale.ROOT).startsWith(combo[0]))
					{
						alreadyCarried += e.getValue();
					}
				}
			}
			int toBring = Math.max(0, use - alreadyCarried);
			String name = displayName(ctx, combo[0]);
			if (toBring > 0)
			{
				// A combination rune already inside the carried pouch shares its slot; one from
				// the bank takes a slot of its own (conservative when a pouch is suggested).
				result.withdrawals.add(new RunPlan.Withdrawal(name, toBring,
					"covers " + join2(combo), ctx.pouchCarried ? 0 : 1));
			}
			result.notes.add(name + " covers " + join2(combo));
			for (int i = 1; i < combo.length; i++)
			{
				int covered = Math.min(use, shortfall.getOrDefault(combo[i], 0));
				if (covered > 0)
				{
					shortfall.put(combo[i], shortfall.get(combo[i]) - covered);
				}
			}
		}
	}

	private static String displayName(Context ctx, String lowerPrefix)
	{
		for (String item : ctx.owned.keySet())
		{
			if (item.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
			{
				return item;
			}
		}
		for (String item : ctx.pouch.keySet())
		{
			if (item.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
			{
				return item;
			}
		}
		String[] words = lowerPrefix.split(" ");
		StringBuilder sb = new StringBuilder();
		for (String w : words)
		{
			if (sb.length() > 0)
			{
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
		}
		return sb.toString();
	}

	private static Set<String> staffRunes(String itemName)
	{
		if (itemName == null)
		{
			return Collections.emptySet();
		}
		String lower = itemName.toLowerCase(Locale.ROOT);
		Set<String> out = new LinkedHashSet<>();
		for (String[] entry : INFINITE_RUNE_ITEMS)
		{
			if (lower.startsWith(entry[0]))
			{
				for (int i = 1; i < entry.length; i++)
				{
					out.add(entry[i]);
				}
			}
		}
		return out;
	}

	private static String ownedPouch(Context ctx)
	{
		for (String name : new String[]{"Divine rune pouch", "Rune pouch"})
		{
			if (ctx.owned.getOrDefault(name, 0) > 0 || name.equals(ctx.pouchName) && ctx.pouchCarried)
			{
				return name;
			}
		}
		return null;
	}

	private static String summary(Context ctx, Set<String> free, Map<String, Integer> shortfall,
		String staff, boolean pouch)
	{
		List<String> parts = new ArrayList<>();
		for (Map.Entry<String, Integer> e : shortfall.entrySet())
		{
			if (e.getValue() > 0)
			{
				parts.add(e.getValue() + " " + e.getKey().toLowerCase(Locale.ROOT));
			}
		}
		StringBuilder sb = new StringBuilder();
		if (!parts.isEmpty())
		{
			sb.append(String.join(", ", parts));
		}
		List<String> help = new ArrayList<>();
		if (staff != null)
		{
			help.add(staff);
		}
		if (pouch)
		{
			help.add("rune pouch");
		}
		if (!free.isEmpty())
		{
			help.add("covers " + join(free));
		}
		if (!help.isEmpty())
		{
			sb.append(sb.length() == 0 ? "" : " — ").append(String.join("; ", help));
		}
		return sb.toString();
	}

	private static String join(Set<String> runes)
	{
		List<String> names = new ArrayList<>();
		for (String rune : runes)
		{
			names.add(rune.toLowerCase(Locale.ROOT));
		}
		return String.join(" and ", names);
	}

	private static String join2(String[] combo)
	{
		List<String> names = new ArrayList<>();
		for (int i = 1; i < combo.length; i++)
		{
			names.add(combo[i].toLowerCase(Locale.ROOT));
		}
		return String.join(" + ", names);
	}
}
