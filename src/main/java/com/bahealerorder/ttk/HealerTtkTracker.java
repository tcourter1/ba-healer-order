package com.bahealerorder.ttk;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.NPC;

public class HealerTtkTracker
{
	private final HealerPoisonModel poisonModel;
	private final HealerHealthEstimator healthEstimator;
	private final Map<Integer, HealerTtkState> statesByNpcIndex = new HashMap<>();
	private int waveStartTick = -1;

	@Inject
	public HealerTtkTracker(HealerPoisonModel poisonModel, HealerHealthEstimator healthEstimator)
	{
		this.poisonModel = poisonModel;
		this.healthEstimator = healthEstimator;
	}

	HealerTtkTracker(HealerPoisonModel poisonModel)
	{
		this.poisonModel = poisonModel;
		this.healthEstimator = null;
	}

	public void startWave(int tick)
	{
		waveStartTick = tick;
		statesByNpcIndex.clear();
	}

	public void reset()
	{
		waveStartTick = -1;
		statesByNpcIndex.clear();
	}

	public void onHealerSpawned(int npcIndex, int healerOrder, int tick)
	{
		statesByNpcIndex.compute(npcIndex, (key, state) ->
		{
			if (state == null)
			{
				return new HealerTtkState(npcIndex, healerOrder, tick, poisonModel);
			}

			state.updateSpawn(healerOrder, tick);
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

	public void observeVisibleHealers(Collection<NPC> healers, int tick)
	{
		if (healthEstimator == null)
		{
			return;
		}

		for (NPC npc : healers)
		{
			healthEstimator.estimate(npc).ifPresent(hp -> observeHp(npc.getIndex(), tick, hp));
		}
	}

	public void observeHp(int npcIndex, int tick, ObservedHealerHp hp)
	{
		HealerTtkState state = statesByNpcIndex.get(npcIndex);

		if (state != null)
		{
			state.observeHp(tick, hp);
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

	HealerTtkState getState(int npcIndex)
	{
		return statesByNpcIndex.get(npcIndex);
	}
}
