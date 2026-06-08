package com.bahealerorder.healer.ttk;

import java.util.Optional;

class HealerTtkState
{
	private static final int FOOD_DAMAGE = 4;

	private final int npcIndex;
	private final HealerPoisonModel poisonModel;
	private int healerOrder;
	private int spawnTick;
	private int confirmedFoodCount;
	private int firstPoisonTick = -1;
	private int lastFoodTick = -1;
	private int currentHpTick = -1;
	private Integer currentHp;
	private Integer maxHp;

	HealerTtkState(int npcIndex, int healerOrder, int spawnTick, HealerPoisonModel poisonModel)
	{
		this.npcIndex = npcIndex;
		this.healerOrder = healerOrder;
		this.spawnTick = spawnTick;
		this.poisonModel = poisonModel;
	}

	void updateSpawn(int healerOrder, int spawnTick)
	{
		this.healerOrder = healerOrder;
		this.spawnTick = spawnTick;
	}

	void recordFoodConsumed(int tick, int waveStartTick)
	{
		confirmedFoodCount++;
		if (firstPoisonTick < 0)
		{
			firstPoisonTick = calculateFirstPoisonTick(tick, waveStartTick);
		}
		lastFoodTick = tick;

		if (currentHp != null)
		{
			currentHp = Math.max(currentHp - FOOD_DAMAGE, 0);
			currentHpTick = tick;
		}
	}

	void observeHp(int tick, ObservedHealerHp hp)
	{
		if (currentHp == null || hp.getCurrentHp() < currentHp)
		{
			currentHp = hp.getCurrentHp();
			currentHpTick = tick;
		}

		maxHp = hp.getMaxHp();
	}

	Optional<HealerTtkResult> getTtk(int currentTick)
	{
		if (currentHp == null || confirmedFoodCount <= 0 || firstPoisonTick < 0 || lastFoodTick < 0 || currentHpTick < 0)
		{
			return Optional.empty();
		}

		return poisonModel.calculateDeathTick(currentHp, firstPoisonTick, lastFoodTick, currentHpTick)
				.stream()
				.mapToObj(HealerTtkResult::new)
				.findFirst();
	}

	boolean hasPoisonedHealerWithUnknownTtk()
	{
		if (currentHp == null || confirmedFoodCount <= 0 || firstPoisonTick < 0 || lastFoodTick < 0 || currentHpTick < 0)
		{
			return false;
		}

		return !poisonModel.calculateDeathTick(currentHp, firstPoisonTick, lastFoodTick, currentHpTick).isPresent();
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
		return confirmedFoodCount;
	}

	int getFirstPoisonTick()
	{
		return firstPoisonTick;
	}

	Integer getCurrentHp()
	{
		return currentHp;
	}

	Integer getMaxHp()
	{
		return maxHp;
	}
}
