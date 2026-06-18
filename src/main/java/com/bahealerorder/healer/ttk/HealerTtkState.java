package com.bahealerorder.healer.ttk;

import java.util.OptionalInt;

class HealerTtkState
{
	private final DeterministicHealerTtk deterministicTtk;
	private HealthRatioHealerTtk healthRatioTtk;
	private boolean publishable;

	HealerTtkState(int spawnTick, int maxHp)
	{
		deterministicTtk = new DeterministicHealerTtk(spawnTick, maxHp);
	}

	void updateSpawn(int spawnTick, int maxHp)
	{
		deterministicTtk.updateSpawn(spawnTick, maxHp);
	}

	void recordFoodConsumed(int tick, int waveStartTick, boolean publishable)
	{
		deterministicTtk.recordFoodConsumed(tick, waveStartTick);
		this.publishable |= publishable;

		if (healthRatioTtk != null)
		{
			healthRatioTtk.recordFoodConsumed(tick, deterministicTtk.getFirstPoisonTick());
		}
	}

	boolean observeHp(int tick, int healthRatio, int healthScale)
	{
		if (healthRatioTtk == null) return false;

		OptionalInt estimatedHp = HealerHealthEstimator.estimate(healthRatio, healthScale, deterministicTtk.getMaxHp());
		return estimatedHp.isPresent() && healthRatioTtk.observeHp(tick, estimatedHp.getAsInt());
	}

	boolean switchToHealthRatioTtk(int tick, int healthRatio, int healthScale)
	{
		if (!deterministicTtk.hasPoisonModel()) return false;

		OptionalInt estimatedHp = HealerHealthEstimator.estimate(healthRatio, healthScale, deterministicTtk.getMaxHp());
		if (!estimatedHp.isPresent()) return false;

		HealerTtkPrediction previous = getPrediction();

		if (healthRatioTtk == null)
		{
			healthRatioTtk = new HealthRatioHealerTtk(
					estimatedHp.getAsInt(),
					tick,
					deterministicTtk.getFirstPoisonTick(),
					deterministicTtk.getLastFoodTick()
			);
		}
		else
		{
			healthRatioTtk.observeHp(tick, estimatedHp.getAsInt());
		}

		publishable = true;
		return !previous.equals(getPrediction());
	}

	HealerTtkPrediction getPrediction()
	{
		if (!deterministicTtk.hasPoisonModel()) return HealerTtkPrediction.empty();

		OptionalInt deathTick = healthRatioTtk == null
				? deterministicTtk.calculateDeathTick()
				: healthRatioTtk.calculateDeathTick();
		int observedTick = healthRatioTtk == null
				? deterministicTtk.getLastFoodTick()
				: healthRatioTtk.getObservedTick();

		return deathTick.isPresent()
				? HealerTtkPrediction.known(deathTick.getAsInt(), observedTick, publishable)
				: HealerTtkPrediction.unknown(observedTick, publishable);
	}

	int getConfirmedFoodCount()
	{
		return deterministicTtk.getConfirmedFoodCount();
	}

	int getFirstPoisonTick()
	{
		return deterministicTtk.getFirstPoisonTick();
	}

	Integer getMaxHp()
	{
		return deterministicTtk.getMaxHp();
	}
}
