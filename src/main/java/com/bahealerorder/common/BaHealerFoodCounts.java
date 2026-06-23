package com.bahealerorder.common;

import java.util.Objects;

public class BaHealerFoodCounts
{
	public static final int FOOD_NONE = 0;
	public static final int FOOD_TOFU = 1;
	public static final int FOOD_WORMS = 2;
	public static final int FOOD_MEAT = 3;

	private final int tofu;
	private final int worms;
	private final int meat;
	private final int calledFood;

	public BaHealerFoodCounts(int tofu, int worms, int meat, int calledFood)
	{
		this.tofu = tofu;
		this.worms = worms;
		this.meat = meat;
		this.calledFood = calledFood;
	}

	public int getTofu()
	{
		return tofu;
	}

	public int getWorms()
	{
		return worms;
	}

	public int getMeat()
	{
		return meat;
	}

	public int getCalledFood()
	{
		return calledFood;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other) return true;
		if (!(other instanceof BaHealerFoodCounts)) return false;

		BaHealerFoodCounts that = (BaHealerFoodCounts) other;
		return tofu == that.tofu
				&& worms == that.worms
				&& meat == that.meat
				&& calledFood == that.calledFood;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(tofu, worms, meat, calledFood);
	}
}
