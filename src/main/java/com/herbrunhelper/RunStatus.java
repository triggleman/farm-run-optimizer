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
	/** How far the run has grown towards its last patch being ready, 0 to 1 (1 only when all are ready). */
	double progress;

	private RunStatus(PatchType type)
	{
		this.type = type;
	}

	static RunStatus of(PatchType type, Iterable<FarmPatch> patches, Map<FarmPatch, PatchTimers.Planting> planted,
		java.util.function.Function<FarmPatch, PatchTimers.Status> status, long now)
	{
		RunStatus r = new RunStatus(type);
		double lastProgress = 0;
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
				}
				if (left > r.lastLeft)
				{
					// The bar follows the patch that will be ready last.
					r.lastLeft = left;
					long total = Math.max(1, pl.readyAtMillis() - pl.plantedAtMillis);
					lastProgress = Math.min(1, Math.max(0, 1 - (double) left / total));
				}
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
		r.progress = r.allReady() ? 1 : lastProgress;
		return r;
	}

	/** Every patch we know about is ready (and there's at least one). */
	boolean allReady()
	{
		return tracked > 0 && ready == tracked;
	}

	/**
	 * Short text: "7/7 ready" when the whole run is ready, "5/7 · 14m 59s" while some still grow
	 * (time until the last one), "1h 59m" when none are ready yet, "growing", or "" (nothing planted).
	 */
	String shortText()
	{
		if (allReady())
		{
			return ready + "/" + tracked + " ready";
		}
		if (ready > 0)
		{
			return ready + "/" + tracked + (lastLeft >= 0 ? " \u00b7 " + PatchTimers.formatDuration(lastLeft) : " ready");
		}
		if (lastLeft >= 0)
		{
			return PatchTimers.formatDuration(lastLeft);
		}
		return tracked > 0 ? "growing" : "";
	}

	/** For sorting "Next up": fully ready runs first, then by time until the whole run is ready. */
	long sortKey()
	{
		if (allReady())
		{
			return -1;
		}
		return lastLeft >= 0 ? lastLeft : Long.MAX_VALUE;
	}
}
