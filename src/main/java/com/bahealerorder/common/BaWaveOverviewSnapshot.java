package com.bahealerorder.common;

import com.bahealerorder.healer.HealerSharedState;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BaWaveOverviewSnapshot
{
	private final int wave;
	private final Map<BaOverviewNpcType, Set<Integer>> spawnedOrdersByType;
	private final Map<BaOverviewNpcType, Map<Integer, Integer>> deathTicksByType;
	private final Map<BaOverviewNpcType, Map<Integer, Integer>> predictedDeathTicksByType;
	private final Map<BaOverviewNpcType, Set<Integer>> unknownTtkOrdersByType;
	private final String duration;

	private BaWaveOverviewSnapshot(
			int wave,
			Map<BaOverviewNpcType, Set<Integer>> spawnedOrdersByType,
			Map<BaOverviewNpcType, Map<Integer, Integer>> deathTicksByType,
			Map<BaOverviewNpcType, Map<Integer, Integer>> predictedDeathTicksByType,
			Map<BaOverviewNpcType, Set<Integer>> unknownTtkOrdersByType,
			String duration)
	{
		this.wave = wave;
		this.spawnedOrdersByType = copySets(spawnedOrdersByType);
		this.deathTicksByType = copyMaps(deathTicksByType);
		this.predictedDeathTicksByType = copyMaps(predictedDeathTicksByType);
		this.unknownTtkOrdersByType = copySets(unknownTtkOrdersByType);
		this.duration = duration;
	}

	public static BaWaveOverviewSnapshot blank(int wave)
	{
		return new BaWaveOverviewSnapshot(wave, emptySets(), emptyMaps(), emptyMaps(), emptySets(), null);
	}

	public static BaWaveOverviewSnapshot fromStates(int wave, BaWaveOverviewState waveState, HealerSharedState healerState)
	{
		Map<BaOverviewNpcType, Set<Integer>> spawnedOrdersByType = emptySets();
		Map<BaOverviewNpcType, Map<Integer, Integer>> deathTicksByType = emptyMaps();
		Map<BaOverviewNpcType, Map<Integer, Integer>> predictedDeathTicksByType = emptyMaps();
		Map<BaOverviewNpcType, Set<Integer>> unknownTtkOrdersByType = emptySets();

		for (BaOverviewNpcType type : BaOverviewNpcType.values())
		{
			int expectedCount = BaWaveInfo.getExpectedCount(wave, type);

			for (int order = 1; order <= expectedCount; order++)
			{
				if (type == BaOverviewNpcType.HEALER)
				{
					addHealerState(wave, healerState, spawnedOrdersByType, deathTicksByType, predictedDeathTicksByType, unknownTtkOrdersByType, order);
					continue;
				}

				if (waveState.hasSpawned(type, order))
				{
					spawnedOrdersByType.get(type).add(order);
				}

				Integer deathTick = waveState.getDeathTick(type, order);
				if (deathTick != null)
				{
					spawnedOrdersByType.get(type).add(order);
					deathTicksByType.get(type).put(order, deathTick);
				}
			}
		}

		return new BaWaveOverviewSnapshot(wave, spawnedOrdersByType, deathTicksByType, predictedDeathTicksByType, unknownTtkOrdersByType, null);
	}

	private static void addHealerState(
			int wave,
			HealerSharedState healerState,
			Map<BaOverviewNpcType, Set<Integer>> spawnedOrdersByType,
			Map<BaOverviewNpcType, Map<Integer, Integer>> deathTicksByType,
			Map<BaOverviewNpcType, Map<Integer, Integer>> predictedDeathTicksByType,
			Map<BaOverviewNpcType, Set<Integer>> unknownTtkOrdersByType,
			int healerOrder)
	{
		if (healerState.getWave() != wave) return;

		if (healerState.hasSpawned(healerOrder))
		{
			spawnedOrdersByType.get(BaOverviewNpcType.HEALER).add(healerOrder);
		}

		Integer actualDeathTick = healerState.getActualDeathTick(healerOrder);
		if (actualDeathTick != null)
		{
			spawnedOrdersByType.get(BaOverviewNpcType.HEALER).add(healerOrder);
			deathTicksByType.get(BaOverviewNpcType.HEALER).put(healerOrder, actualDeathTick);
			return;
		}

		Integer predictedDeathTick = healerState.getPredictedDeathTick(healerOrder);
		if (predictedDeathTick != null)
		{
			spawnedOrdersByType.get(BaOverviewNpcType.HEALER).add(healerOrder);
			predictedDeathTicksByType.get(BaOverviewNpcType.HEALER).put(healerOrder, predictedDeathTick);
		}
		else if (healerState.hasUnknownTtk(healerOrder))
		{
			spawnedOrdersByType.get(BaOverviewNpcType.HEALER).add(healerOrder);
			unknownTtkOrdersByType.get(BaOverviewNpcType.HEALER).add(healerOrder);
		}
	}

	public int getWave()
	{
		return wave;
	}

	public boolean hasSpawned(BaOverviewNpcType type, int order)
	{
		return spawnedOrdersByType.getOrDefault(type, Collections.emptySet()).contains(order);
	}

	public Integer getDeathTick(BaOverviewNpcType type, int order)
	{
		return deathTicksByType.getOrDefault(type, Collections.emptyMap()).get(order);
	}

	public Integer getPredictedDeathTick(BaOverviewNpcType type, int order)
	{
		return predictedDeathTicksByType.getOrDefault(type, Collections.emptyMap()).get(order);
	}

	public boolean hasUnknownTtk(BaOverviewNpcType type, int order)
	{
		return unknownTtkOrdersByType.getOrDefault(type, Collections.emptySet()).contains(order);
	}

	public String getDuration()
	{
		return duration;
	}

	public BaWaveOverviewSnapshot withDuration(String duration)
	{
		return new BaWaveOverviewSnapshot(wave, spawnedOrdersByType, deathTicksByType, predictedDeathTicksByType, unknownTtkOrdersByType, duration);
	}

	public String signature()
	{
		return wave
				+ ":" + spawnedOrdersByType
				+ ":" + deathTicksByType
				+ ":" + predictedDeathTicksByType
				+ ":" + unknownTtkOrdersByType
				+ ":" + duration;
	}

	private static Map<BaOverviewNpcType, Set<Integer>> emptySets()
	{
		Map<BaOverviewNpcType, Set<Integer>> values = new EnumMap<>(BaOverviewNpcType.class);

		for (BaOverviewNpcType type : BaOverviewNpcType.values())
		{
			values.put(type, new HashSet<>());
		}

		return values;
	}

	private static Map<BaOverviewNpcType, Map<Integer, Integer>> emptyMaps()
	{
		Map<BaOverviewNpcType, Map<Integer, Integer>> values = new EnumMap<>(BaOverviewNpcType.class);

		for (BaOverviewNpcType type : BaOverviewNpcType.values())
		{
			values.put(type, new HashMap<>());
		}

		return values;
	}

	private static Map<BaOverviewNpcType, Set<Integer>> copySets(Map<BaOverviewNpcType, Set<Integer>> source)
	{
		Map<BaOverviewNpcType, Set<Integer>> copy = new EnumMap<>(BaOverviewNpcType.class);

		for (Map.Entry<BaOverviewNpcType, Set<Integer>> entry : source.entrySet())
		{
			copy.put(entry.getKey(), Collections.unmodifiableSet(new HashSet<>(entry.getValue())));
		}

		return Collections.unmodifiableMap(copy);
	}

	private static Map<BaOverviewNpcType, Map<Integer, Integer>> copyMaps(Map<BaOverviewNpcType, Map<Integer, Integer>> source)
	{
		Map<BaOverviewNpcType, Map<Integer, Integer>> copy = new EnumMap<>(BaOverviewNpcType.class);

		for (Map.Entry<BaOverviewNpcType, Map<Integer, Integer>> entry : source.entrySet())
		{
			copy.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
		}

		return Collections.unmodifiableMap(copy);
	}
}
