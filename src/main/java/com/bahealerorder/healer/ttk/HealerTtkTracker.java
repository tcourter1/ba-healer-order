package com.bahealerorder.healer.ttk;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

public class HealerTtkTracker
{
	private static final int[] HEALER_MAX_HP_BY_WAVE = {0, 27, 32, 37, 43, 49, 55, 60, 67, 76, 60};

	private final Map<Integer, HealerTtkState> statesByNpcIndex = new HashMap<>();
	private int waveStartTick = -1;
	private int wave = -1;
	private int healerMaxHp = -1;

	@Inject
	public HealerTtkTracker()
	{
	}

	public void startWave(int tick, int wave)
	{
		waveStartTick = tick;
		this.wave = wave;
		healerMaxHp = getHealerMaxHp(wave);
		statesByNpcIndex.clear();
	}

	public void reset()
	{
		waveStartTick = -1;
		wave = -1;
		healerMaxHp = -1;
		statesByNpcIndex.clear();
	}

	public void onHealerSpawned(int npcIndex, int healerOrder, int tick)
	{
		statesByNpcIndex.compute(npcIndex, (key, state) ->
		{
			if (state == null)
			{
				return new HealerTtkState(npcIndex, healerOrder, tick, healerMaxHp);
			}

			state.updateSpawn(healerOrder, tick, healerMaxHp);
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

	public Optional<HealerTtkResult> getTtk(int npcIndex, int currentTick)
	{
		HealerTtkState state = statesByNpcIndex.get(npcIndex);
		return state == null ? Optional.empty() : state.getTtk(currentTick);
	}

	public boolean hasPoisonedHealerWithUnknownTtk(int npcIndex)
	{
		HealerTtkState state = statesByNpcIndex.get(npcIndex);
		return state != null && state.hasPoisonedHealerWithUnknownTtk();
	}

	public int getWaveStartTick()
	{
		return waveStartTick;
	}

	public int getWave()
	{
		return wave;
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
