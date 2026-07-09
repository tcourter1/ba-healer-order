package com.bahealerorder.healer.codes;

public enum HealerTimingMode
{
	NONE("No timing"),
	AT_OR_AFTER("At/after"),
	BEFORE("Before"),
	EXACT("Exactly at");

	private final String label;

	HealerTimingMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
