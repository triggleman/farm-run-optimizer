package com.herbrunhelper;

/**
 * Where your player-owned house portal is. House teleports (the spell and the tablet) land at
 * this portal; the Construction cape can pick any portal. Pure Java so the planner can test it.
 */
public enum HouseLocation
{
	NONE("Not set"),
	RIMMINGTON("Rimmington"),
	TAVERLEY("Taverley"),
	POLLNIVNEACH("Pollnivneach"),
	HOSIDIUS("Hosidius"),
	RELLEKKA("Rellekka"),
	ALDARIN("Aldarin"),
	BRIMHAVEN("Brimhaven"),
	YANILLE("Yanille"),
	PRIFDDINAS("Prifddinas");

	private final String displayName;

	HouseLocation(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
