package com.bahealerorder.healer.ttk;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.Getter;

public class HealerTtkTracker
{
	private static final int[] HEALER_MAX_HP_BY_WAVE = {0, 27, 32, 37, 43, 49, 55, 60, 67, 76, 60};

	private final Map<Integer, HealerTtkState> statesByNpcIndex = new HashMap<>();
	@Getter
	private int waveStartTick = -1;
	private int healerMaxHp = -1;

	@Inject
	public HealerTtkTracker()
	{
	}

	public void startWave(int tick, int wave)
	{
		waveStartTick = tick;
		healerMaxHp = getHealerMaxHp(wave);
		statesByNpcIndex.clear();
	}

	public void reset()
	{
		waveStartTick = -1;
		healerMaxHp = -1;
		statesByNpcIndex.clear();
	}

	public void onHealerSpawned(int npcIndex, int tick)
	{
		statesByNpcIndex.compute(npcIndex, (key, state) ->
		{
			if (state == null) return new HealerTtkState(tick, healerMaxHp);

			state.updateSpawn(tick, healerMaxHp);
			return state;
		});
	}

	public void onFoodConsumedForHealer(int npcIndex, int tick)
	{
		HealerTtkState state = statesByNpcIndex.get(npcIndex);

		if (state != null)
		{
			state.recordFoodConsumed(tick, waveStartTick);
		}
	}

	public boolean observeHp(int npcIndex, int tick, int healthRatio, int healthScale)
	{
		HealerTtkState state = statesByNpcIndex.computeIfAbsent(
				npcIndex,
				key -> new HealerTtkState(tick, healerMaxHp)
		);

		return state.observeHp(tick, healthRatio, healthScale);
	}

	public boolean switchToHealthRatioTtk(int npcIndex, int tick, int healthRatio, int healthScale)
	{
		HealerTtkState state = statesByNpcIndex.computeIfAbsent(
				npcIndex,
				key -> new HealerTtkState(tick, healerMaxHp)
		);

		return state.switchToHealthRatioTtk(tick, healthRatio, healthScale);
	}

	public HealerTtkPrediction getPrediction(int npcIndex)
	{
		HealerTtkState state = statesByNpcIndex.get(npcIndex);
		return state == null ? HealerTtkPrediction.empty() : state.getPrediction();
	}

	public boolean isHealthRatioMode(int npcIndex)
	{
		HealerTtkState state = statesByNpcIndex.get(npcIndex);
		return state != null && state.isHealthRatioMode();
	}

	HealerTtkState getState(int npcIndex)
	{
		return statesByNpcIndex.get(npcIndex);
	}

	static int getHealerMaxHp(int wave)
	{
		return wave >= 1 && wave < HEALER_MAX_HP_BY_WAVE.length ? HEALER_MAX_HP_BY_WAVE[wave] : -1;
	}
}
