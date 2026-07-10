package com.bahealerorder.healer.ttk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import lombok.AccessLevel;
import lombok.Getter;

final class DeterministicHealerTtk
{
	private static final int FOOD_DAMAGE = 4;
	private static final int UNKNOWN_TICK = -1;

	private final List<Integer> foodTicks = new ArrayList<>();
	private int spawnTick;
	@Getter(AccessLevel.PACKAGE)
	private int maxHp;
	@Getter(AccessLevel.PACKAGE)
	private int firstPoisonTick = UNKNOWN_TICK;
	@Getter(AccessLevel.PACKAGE)
	private int lastFoodTick = UNKNOWN_TICK;

	DeterministicHealerTtk(int spawnTick, int maxHp)
	{
		this.spawnTick = spawnTick;
		this.maxHp = maxHp;
	}

	void updateSpawn(int spawnTick, int maxHp)
	{
		this.spawnTick = spawnTick;
		this.maxHp = maxHp;
	}

	void recordFoodConsumed(int tick, int waveStartTick)
	{
		foodTicks.add(tick);
		Collections.sort(foodTicks);
		lastFoodTick = Math.max(lastFoodTick, tick);
		firstPoisonTick = calculateFirstPoisonTick(foodTicks.get(0), waveStartTick);
	}

	OptionalInt calculateDeathTick()
	{
		if (!hasPoisonModel()) return OptionalInt.empty();

		int hp = maxHp;
		int foodIndex = 0;
		int latestFoodTick = UNKNOWN_TICK;
		int poisonTick = firstPoisonTick;

		while (true)
		{
			int nextFoodTick = foodIndex < foodTicks.size() ? foodTicks.get(foodIndex) : Integer.MAX_VALUE;
			boolean poisonCanHit = latestFoodTick >= 0
					&& poisonTick <= latestFoodTick + HealerPoisonModel.TOTAL_POISON_HITS * HealerPoisonModel.TICKS_PER_POISON_HIT;
			int nextPoisonTick = poisonCanHit ? poisonTick : Integer.MAX_VALUE;

			if (nextFoodTick == Integer.MAX_VALUE && nextPoisonTick == Integer.MAX_VALUE) return OptionalInt.empty();

			if (nextPoisonTick <= nextFoodTick)
			{
				hp -= HealerPoisonModel.getDamageAtPoisonTick(latestFoodTick, poisonTick);
				if (hp <= 0) return OptionalInt.of(poisonTick);

				poisonTick += HealerPoisonModel.TICKS_PER_POISON_HIT;
				continue;
			}

			while (foodIndex < foodTicks.size() && foodTicks.get(foodIndex) == nextFoodTick)
			{
				hp -= FOOD_DAMAGE;
				latestFoodTick = nextFoodTick;
				foodIndex++;

				if (hp <= 0) return OptionalInt.of(nextFoodTick);
			}
		}
	}

	boolean hasPoisonModel()
	{
		return maxHp > 0 && !foodTicks.isEmpty() && firstPoisonTick >= 0 && lastFoodTick >= 0;
	}

	int getConfirmedFoodCount()
	{
		return foodTicks.size();
	}

	private int calculateFirstPoisonTick(int firstFoodTick, int waveStartTick)
	{
		if (waveStartTick >= 0 && firstFoodTick - spawnTick <= HealerPoisonModel.TICKS_PER_POISON_HIT)
		{
			int ticksSinceWaveStart = Math.max(firstFoodTick - waveStartTick, 0);
			return waveStartTick + (ticksSinceWaveStart / HealerPoisonModel.TICKS_PER_POISON_HIT + 1) * HealerPoisonModel.TICKS_PER_POISON_HIT;
		}

		return firstFoodTick + HealerPoisonModel.TICKS_PER_POISON_HIT;
	}
}
