package com.herbrunhelper;

import java.util.EnumMap;
import java.util.Map;

/**
 * When crops will be ready. Crops grow on fixed clock ticks (20 min herbs, 40 min trees,
 * 160 min fruit trees, 640 min hardwood) shifted by a per-account offset. The offset is learned
 * by watching a patch change stage while you stand near it (the approach RuneLite's Time
 * Tracking uses). Pure Java so it can be tested.
 */
public final class GrowthClock
{
	private final Map<PatchType, Long> phaseMillis = new EnumMap<>(PatchType.class);
	/** Phases by growth tick length, so potato cactus (10 min) doesn't share the cactus phase (80 min). */
	private final Map<Integer, Long> phaseByTick = new java.util.HashMap<>();

	public synchronized void learn(PatchType type, long changedAt)
	{
		long tick = tickMillis(type);
		long phase = Math.floorMod(changedAt, tick);
		phaseMillis.put(type, phase);
		phaseByTick.put(type.getTickMinutes(), phase);
	}

	/** Learn the phase at the crop's own tick length (potato cactus is the exception). */
	public synchronized void learn(Crop crop, long changedAt)
	{
		long tick = tickMillis(crop);
		long phase = Math.floorMod(changedAt, tick);
		phaseByTick.put(crop.getTickMinutes(), phase);
		// Keep the patch type's entry lined up for anything that still reads it.
		phaseMillis.put(crop.getType(), Math.floorMod(phase, tickMillis(crop.getType())));
	}

	public synchronized boolean knows(PatchType type)
	{
		return phaseMillis.containsKey(type);
	}

	public synchronized String serialize()
	{
		StringBuilder sb = new StringBuilder();
		phaseMillis.forEach((t, p) -> sb.append(sb.length() > 0 ? ";" : "").append(t.name()).append(':').append(p));
		phaseByTick.forEach((tick, p) -> sb.append(sb.length() > 0 ? ";" : "").append('#').append(tick).append(':').append(p));
		return sb.toString();
	}

	public synchronized void load(String data)
	{
		phaseMillis.clear();
		phaseByTick.clear();
		if (data == null || data.isEmpty())
		{
			return;
		}
		for (String part : data.split(";"))
		{
			String[] kv = part.split(":");
			if (kv.length != 2)
			{
				continue;
			}
			try
			{
				if (kv[0].startsWith("#"))
				{
					phaseByTick.put(Integer.parseInt(kv[0].substring(1)), Long.parseLong(kv[1]));
				}
				else
				{
					long value = Long.parseLong(kv[1]);
					PatchType type = PatchType.valueOf(kv[0]);
					phaseMillis.put(type, value);
					phaseByTick.put(type.getTickMinutes(), value);
				}
			}
			catch (RuntimeException ignored)
			{
				// old or bad entry
			}
		}
	}

	public static long tickMillis(PatchType type)
	{
		return type.getTickMinutes() * 60_000L;
	}

	/** The next growth tick after now. */
	public synchronized long nextTick(PatchType type, long now)
	{
		long tick = tickMillis(type);
		long phase = phaseMillis.getOrDefault(type, 0L);
		return Math.floorDiv(now - phase, tick) * tick + phase + tick;
	}

	/** A crop's own tick length (potato cactus grows faster than its patch type). */
	public static long tickMillis(Crop crop)
	{
		return crop.getTickMinutes() * 60_000L;
	}

	/** The crop's next growth tick after now, using the phase learned for its tick length. */
	public synchronized long nextTick(Crop crop, long now)
	{
		long tick = tickMillis(crop);
		Long learned = phaseByTick.get(crop.getTickMinutes());
		long phase = learned != null ? Math.floorMod(learned, tick)
			: Math.floorMod(phaseMillis.getOrDefault(crop.getType(), 0L), tick);
		return Math.floorDiv(now - phase, tick) * tick + phase + tick;
	}

	/** Ready time for a crop seen at this growth stage right now. */
	public long readyFromLive(Crop crop, int stage, long now)
	{
		int last = crop.getStages() - 1;
		if (stage >= last)
		{
			return now;
		}
		return nextTick(crop, now) + (long) (last - 1 - stage) * tickMillis(crop);
	}

	/** Ready time for a crop that reached this stage at changedAt. */
	public static long readyFromChange(Crop crop, int stage, long changedAt)
	{
		int last = crop.getStages() - 1;
		return changedAt + (long) Math.max(0, last - stage) * tickMillis(crop);
	}
}
