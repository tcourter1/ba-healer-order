package com.bahealerorder.healer.ttk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class HealerTtkState
{
	private static final int FOOD_DAMAGE = 4;
	private static final int UNKNOWN_TICK = -1;

	private final int npcIndex;
	private final List<Integer> foodTicks = new ArrayList<>();
	private int healerOrder;
	private int spawnTick;
	private int maxHp;
	private int firstPoisonTick = UNKNOWN_TICK;

	HealerTtkState(int npcIndex, int healerOrder, int spawnTick, int maxHp)
	{
		this.npcIndex = npcIndex;
		this.healerOrder = healerOrder;
		this.spawnTick = spawnTick;
		this.maxHp = maxHp;
	}

	void updateSpawn(int healerOrder, int spawnTick, int maxHp)
	{
		this.healerOrder = healerOrder;
		this.spawnTick = spawnTick;
		this.maxHp = maxHp;
	}

	void recordFoodConsumed(int tick, int waveStartTick)
	{
		foodTicks.add(tick);
		Collections.sort(foodTicks);

		if (!foodTicks.isEmpty())
		{
			firstPoisonTick = calculateFirstPoisonTick(foodTicks.get(0), waveStartTick);
		}
	}

	Optional<HealerTtkResult> getTtk(int currentTick)
	{
		if (maxHp <= 0 || foodTicks.isEmpty() || firstPoisonTick < 0)
		{
			return Optional.empty();
		}

		int deathTick = calculateDeathTick();
		return deathTick < 0 ? Optional.empty() : Optional.of(new HealerTtkResult(deathTick));
	}

	boolean hasPoisonedHealerWithUnknownTtk()
	{
		return maxHp > 0 && !foodTicks.isEmpty() && firstPoisonTick >= 0 && calculateDeathTick() < 0;
	}

	private int calculateDeathTick()
	{
		int hp = maxHp;
		int foodIndex = 0;
		int lastFoodTick = UNKNOWN_TICK;
		int poisonTick = firstPoisonTick;

		while (true)
		{
			int nextFoodTick = foodIndex < foodTicks.size() ? foodTicks.get(foodIndex) : Integer.MAX_VALUE;
			boolean poisonCanHit = lastFoodTick >= 0
					&& poisonTick <= lastFoodTick + HealerPoisonModel.TOTAL_POISON_HITS * HealerPoisonModel.TICKS_PER_POISON_HIT;
			int nextPoisonTick = poisonCanHit ? poisonTick : Integer.MAX_VALUE;

			if (nextFoodTick == Integer.MAX_VALUE && nextPoisonTick == Integer.MAX_VALUE)
			{
				return UNKNOWN_TICK;
			}

			if (nextPoisonTick <= nextFoodTick)
			{
				int damage = HealerPoisonModel.getDamageAtPoisonTick(lastFoodTick, poisonTick);
				if (damage > 0)
				{
					hp -= damage;
					if (hp <= 0) return poisonTick;
				}

				poisonTick += HealerPoisonModel.TICKS_PER_POISON_HIT;
				continue;
			}

			while (foodIndex < foodTicks.size() && foodTicks.get(foodIndex) == nextFoodTick)
			{
				hp -= FOOD_DAMAGE;
				lastFoodTick = nextFoodTick;
				foodIndex++;

				if (hp <= 0) return nextFoodTick;
			}
		}
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

	int getNpcIndex()
	{
		return npcIndex;
	}

	int getHealerOrder()
	{
		return healerOrder;
	}

	int getSpawnTick()
	{
		return spawnTick;
	}

	int getConfirmedFoodCount()
	{
		return foodTicks.size();
	}

	int getFirstPoisonTick()
	{
		return firstPoisonTick;
	}

	Integer getMaxHp()
	{
		return maxHp;
	}
}
