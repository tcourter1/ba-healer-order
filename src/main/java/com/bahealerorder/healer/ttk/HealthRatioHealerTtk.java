package com.bahealerorder.healer.ttk;

import java.util.OptionalInt;

final class HealthRatioHealerTtk
{
	private static final int FOOD_DAMAGE = 4;
	private static final HealerPoisonModel POISON_MODEL = new HealerPoisonModel();

	private int firstPoisonTick;
	private int lastFoodTick;
	private int currentHp;
	private int currentHpTick;

	HealthRatioHealerTtk(int currentHp, int currentHpTick, int firstPoisonTick, int lastFoodTick)
	{
		this.currentHp = currentHp;
		this.currentHpTick = currentHpTick;
		this.firstPoisonTick = firstPoisonTick;
		this.lastFoodTick = lastFoodTick;
	}

	void recordFoodConsumed(int tick, int firstPoisonTick)
	{
		currentHp = Math.max(currentHp - FOOD_DAMAGE, 0);
		currentHpTick = tick;
		this.firstPoisonTick = firstPoisonTick;
		lastFoodTick = Math.max(lastFoodTick, tick);
	}

	boolean observeHp(int tick, int hp)
	{
		if (hp >= currentHp) return false;

		currentHp = hp;
		currentHpTick = tick;
		return true;
	}

	OptionalInt calculateDeathTick()
	{
		return POISON_MODEL.calculateDeathTick(currentHp, firstPoisonTick, lastFoodTick, currentHpTick);
	}
}
