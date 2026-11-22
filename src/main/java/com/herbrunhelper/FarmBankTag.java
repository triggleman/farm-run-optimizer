package com.herbrunhelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.client.plugins.banktags.BankTag;

/** The item set behind the bank's "Farm run" view. Bank Tags asks it what to show. */
final class FarmBankTag implements BankTag
{
	private volatile Set<Integer> items = Collections.emptySet();

	/** Returns true when the set changed, so the open view can be rebuilt. */
	synchronized boolean setItems(Set<Integer> next)
	{
		if (items.equals(next))
		{
			return false;
		}
		items = Collections.unmodifiableSet(new HashSet<>(next));
		return true;
	}

	@Override
	public boolean contains(int itemId)
	{
		return items.contains(itemId);
	}
}
