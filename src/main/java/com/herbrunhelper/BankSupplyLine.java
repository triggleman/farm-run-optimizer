package com.herbrunhelper;

import java.util.Arrays;

/** One row of the Farm run bank tab, in the same shape Farm Run Autopilot uses. */
final class BankSupplyLine
{
	enum Status
	{
		CARRIED,
		IN_STORAGE,
		MISSING
	}

	final RunPlan.Group group;
	final String name;
	final int need;
	final int have;
	final int carried;
	final int[] itemIds;
	final String where;
	final boolean wear;
	final boolean noted;

	BankSupplyLine(RunPlan.Group group, String name, int need, int have, int carried, int[] itemIds,
		String where, boolean wear, boolean noted)
	{
		this.group = group;
		this.name = name;
		this.need = need;
		this.have = have;
		this.carried = carried;
		this.itemIds = itemIds;
		this.where = where;
		this.wear = wear;
		this.noted = noted;
	}

	Status status()
	{
		if (carried >= need)
		{
			return Status.CARRIED;
		}
		return have >= need ? Status.IN_STORAGE : Status.MISSING;
	}

	@Override
	public String toString()
	{
		return "BankSupplyLine(group=" + group + ", name=" + name + ", need=" + need + ", have=" + have
			+ ", carried=" + carried + ", itemIds=" + Arrays.toString(itemIds) + ", wear=" + wear
			+ ", noted=" + noted + ")";
	}
}
