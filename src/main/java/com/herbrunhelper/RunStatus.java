package com.herbrunhelper;

import java.util.Map;

/**
 * Where one run stands right now: how many patches are ready, when the next one will be, and
 * how far it has grown. The panel headers, the "Next up" bars and the run-ready notification
 * all use this, so they always agree. Pure Java.
 */
final class RunStatus
{
	final PatchType type;
	/** Patches with something in them that we know about. */
	int tracked;
	/** Patches that need you now: grown, diseased, dead or a stump to replant. */
	int ready;
	/** Time until the next growing patch is ready, or -1 if none has a timer. */
	long soonestLeft = -1;
	/** Time until the last growing patch is ready (when the whole run is ready), or -1. */
	long lastLeft = -1;
	/** How far the next-ready patch has grown, 0 to 1 (1 when something is ready). */
	double progress;

	private RunStatus(PatchType type)
	{
		this.type = type;
	}

	static RunStatus of(PatchType type, Iterable<FarmPatch> patches, Map<FarmPatch, PatchTimers.Planting> planted,
		java.util.function.Function<FarmPatch, PatchTimers.Status> status, long now)
	{
		RunStatus r = new RunStatus(type);
		double bestProgress = 0;
		for (FarmPatch p : patches)
		{
			if (p.getType() != type)
			{
				continue;
			}
			PatchTimers.Planting pl = planted.get(p);
			if (pl != null)
			{
				r.tracked++;
				if (pl.isReady(now))
				{
					r.ready++;
					continue;
				}
				long left = pl.millisLeft(now);
				if (r.soonestLeft < 0 || left < r.soonestLeft)
				{
					r.soonestLeft = left;
					long total = Math.max(1, pl.readyAtMillis() - pl.plantedAtMillis);
					bestProgress = Math.min(1, Math.max(0, 1 - (double) left / total));
				}
				r.lastLeft = Math.max(r.lastLeft, left);
				continue;
			}
			PatchTimers.Status st = status.apply(p);
			if (st == null || st == PatchTimers.Status.EMPTY || st == PatchTimers.Status.WEEDS)
			{
				continue; // nothing growing there
			}
			r.tracked++;
			if (st != PatchTimers.Status.GROWING_UNKNOWN)
			{
				r.ready++; // diseased, dead or a stump: needs a visit
			}
		}
		r.progress = r.ready > 0 ? 1 : bestProgress;
		return r;
	}

	/** Every patch we know about is ready (and there's at least one). */
	boolean allReady()
	{
		return tracked > 0 && ready == tracked;
	}

	/** Short text: "5/7 ready", "15m 29s", "growing" or "" (nothing planted). */
	String shortText()
	{
		if (ready > 0)
		{
			return ready + "/" + tracked + " ready";
		}
		if (soonestLeft >= 0)
		{
			return PatchTimers.formatDuration(soonestLeft);
		}
		return tracked > 0 ? "growing" : "";
	}

	/** For sorting "Next up": ready runs first, then by time left, then runs with nothing timed. */
	long sortKey()
	{
		if (ready > 0)
		{
			return -1;
		}
		return soonestLeft >= 0 ? soonestLeft : Long.MAX_VALUE;
	}
}
