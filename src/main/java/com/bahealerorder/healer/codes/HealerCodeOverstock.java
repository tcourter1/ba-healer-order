package com.bahealerorder.healer.codes;

public enum HealerCodeOverstock
{
	REGULAR("Regular"),
	ONE_X("1x"),
	TWO_X("2x"),
	THREE_X("3x"),
	FOUR_X("4x"),
	FIVE_X("5x");

	private final String label;

	HealerCodeOverstock(String label)
	{
		this.label = label;
	}

	public static HealerCodeOverstock valueOrRegular(HealerCodeOverstock overstock)
	{
		return overstock == null ? REGULAR : overstock;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
