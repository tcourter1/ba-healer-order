package com.bahealerorder.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
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
}
