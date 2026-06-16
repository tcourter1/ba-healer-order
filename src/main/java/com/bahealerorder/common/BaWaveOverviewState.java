package com.bahealerorder.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;

@Singleton
public class BaWaveOverviewState
{
	private final Map<BaOverviewNpcType, Set<Integer>> seenNpcIndexesByType = new EnumMap<>(BaOverviewNpcType.class);
	private final Map<BaOverviewNpcType, Map<Integer, Integer>> deathTickByNpcIndexByType = new EnumMap<>(BaOverviewNpcType.class);
	private final Map<BaOverviewNpcType, Map<Integer, Integer>> partyOrderByNpcIndexByType = new EnumMap<>(BaOverviewNpcType.class);
	private final Map<BaOverviewNpcType, Map<Integer, Integer>> partyDeathTickByOrderByType = new EnumMap<>(BaOverviewNpcType.class);
	private final Map<BaOverviewNpcType, Integer> indexBaseByType = new EnumMap<>(BaOverviewNpcType.class);
	private int wave = -1;

	public void reset()
	{
		wave = -1;
		seenNpcIndexesByType.clear();
		deathTickByNpcIndexByType.clear();
		partyOrderByNpcIndexByType.clear();
		partyDeathTickByOrderByType.clear();
		indexBaseByType.clear();
	}

	public boolean startWave(int wave)
	{
		if (!BaWaveInfo.isValidWave(wave)) return false;
		if (this.wave == wave) return false;

		reset();
		this.wave = wave;
		return true;
	}

	public int getWave()
	{
		return wave;
	}

	public boolean isWaveActive()
	{
		return BaWaveInfo.isValidWave(wave);
	}

	public boolean recordSpawn(BaOverviewNpcType type, int npcIndex)
	{
		if (!isTrackable(type) || npcIndex < 0) return false;

		Map<Integer, Integer> before = getOrderByNpcIndex(type);
		indexBaseByType.putIfAbsent(type, npcIndex);
		boolean added = seenNpcIndexes(type).add(npcIndex);
		return added || !before.equals(getOrderByNpcIndex(type));
	}

	public boolean recordDeath(BaOverviewNpcType type, int npcIndex, int deathTick)
	{
		if (!isTrackable(type) || npcIndex < 0 || deathTick < 0) return false;

		boolean changed = recordSpawn(type, npcIndex);
		changed |= putEarlierTick(deathTickByNpcIndex(type), npcIndex, deathTick);
		return changed;
	}

	public int getSpawnedCount(BaOverviewNpcType type)
	{
		return getSpawnedOrders(type).size();
	}

	public int getDeadCount(BaOverviewNpcType type)
	{
		return getDeadOrders(type).size();
	}

	public boolean hasSpawned(BaOverviewNpcType type, int order)
	{
		return getSpawnedOrders(type).contains(order);
	}

	public boolean isDead(BaOverviewNpcType type, int order)
	{
		return getDeathTick(type, order) != null;
	}

	public Integer getDeathTick(BaOverviewNpcType type, int order)
	{
		return getDeathTicksByOrder(type).get(order);
	}

	public boolean updateFromParty(BaWaveOverviewSyncMessage message)
	{
		if (message == null || message.getWave() <= 0) return false;

		boolean changed = startWave(message.getWave());
		changed |= recordPartyMappings(message.getNpcTypes(), message.getNpcIndexes(), message.getNpcOrders());
		changed |= recordPartyDeaths(message.getDeadNpcTypes(), message.getDeadNpcOrders(), message.getDeadNpcDeathTicks());
		return changed;
	}

	public BaWaveOverviewSyncMessage toSyncMessage(int world)
	{
		List<Integer> npcTypes = new ArrayList<>();
		List<Integer> npcIndexes = new ArrayList<>();
		List<Integer> npcOrders = new ArrayList<>();
		List<Integer> deadNpcTypes = new ArrayList<>();
		List<Integer> deadNpcOrders = new ArrayList<>();
		List<Integer> deadNpcDeathTicks = new ArrayList<>();

		for (BaOverviewNpcType type : BaOverviewNpcType.values())
		{
			if (type == BaOverviewNpcType.HEALER) continue;

			for (Map.Entry<Integer, Integer> entry : getOrderByNpcIndex(type).entrySet())
			{
				npcTypes.add(type.ordinal());
				npcIndexes.add(entry.getKey());
				npcOrders.add(entry.getValue());
			}

			for (Map.Entry<Integer, Integer> entry : getDeathTicksByOrder(type).entrySet())
			{
				deadNpcTypes.add(type.ordinal());
				deadNpcOrders.add(entry.getKey());
				deadNpcDeathTicks.add(entry.getValue());
			}
		}

		return new BaWaveOverviewSyncMessage(
				world,
				wave,
				toIntArray(npcTypes),
				toIntArray(npcIndexes),
				toIntArray(npcOrders),
				toIntArray(deadNpcTypes),
				toIntArray(deadNpcOrders),
				toIntArray(deadNpcDeathTicks)
		);
	}

	private boolean recordPartyMappings(int[] npcTypes, int[] npcIndexes, int[] npcOrders)
	{
		boolean changed = false;
		int count = Math.min(Math.min(length(npcTypes), length(npcIndexes)), length(npcOrders));

		for (int i = 0; i < count; i++)
		{
			BaOverviewNpcType type = typeFromOrdinal(npcTypes[i]);
			int npcIndex = npcIndexes[i];
			int order = npcOrders[i];

			if (!isTrackable(type) || npcIndex < 0 || !isValidOrder(type, order)) continue;

			Map<Integer, Integer> partyOrderByNpcIndex = partyOrderByNpcIndex(type);
			changed |= removeOrderFromOtherIndexes(partyOrderByNpcIndex, npcIndex, order);
			Integer previous = partyOrderByNpcIndex.put(npcIndex, order);
			changed |= previous == null || previous != order;
		}

		return changed;
	}

	private boolean recordPartyDeaths(int[] deadNpcTypes, int[] deadNpcOrders, int[] deadNpcDeathTicks)
	{
		boolean changed = false;
		int count = Math.min(Math.min(length(deadNpcTypes), length(deadNpcOrders)), length(deadNpcDeathTicks));

		for (int i = 0; i < count; i++)
		{
			BaOverviewNpcType type = typeFromOrdinal(deadNpcTypes[i]);
			int order = deadNpcOrders[i];
			int deathTick = deadNpcDeathTicks[i];

			if (!isTrackable(type) || !isValidOrder(type, order) || deathTick < 0) continue;

			changed |= putEarlierTick(partyDeathTickByOrder(type), order, deathTick);
		}

		return changed;
	}

	private Map<Integer, Integer> getOrderByNpcIndex(BaOverviewNpcType type)
	{
		if (!isTrackable(type)) return Collections.emptyMap();

		return NpcIndexOrderer.buildOrderByNpcIndex(
				seenNpcIndexes(type),
				partyOrderByNpcIndex(type),
				indexBaseByType.getOrDefault(type, -1),
				BaWaveInfo.getExpectedCount(wave, type)
		);
	}

	private Set<Integer> getSpawnedOrders(BaOverviewNpcType type)
	{
		return new HashSet<>(getOrderByNpcIndex(type).values());
	}

	private Set<Integer> getDeadOrders(BaOverviewNpcType type)
	{
		return getDeathTicksByOrder(type).keySet();
	}

	private Map<Integer, Integer> getDeathTicksByOrder(BaOverviewNpcType type)
	{
		Map<Integer, Integer> deathTicksByOrder = new HashMap<>(partyDeathTickByOrder(type));
		Map<Integer, Integer> orderByNpcIndex = getOrderByNpcIndex(type);

		for (Map.Entry<Integer, Integer> entry : deathTickByNpcIndex(type).entrySet())
		{
			Integer order = orderByNpcIndex.get(entry.getKey());
			if (order != null)
			{
				putEarlierTick(deathTicksByOrder, order, entry.getValue());
			}
		}

		deathTicksByOrder.keySet().removeIf(order -> !isValidOrder(type, order));
		return deathTicksByOrder;
	}

	private Set<Integer> seenNpcIndexes(BaOverviewNpcType type)
	{
		return seenNpcIndexesByType.computeIfAbsent(type, ignored -> new HashSet<>());
	}

	private Map<Integer, Integer> deathTickByNpcIndex(BaOverviewNpcType type)
	{
		return deathTickByNpcIndexByType.computeIfAbsent(type, ignored -> new HashMap<>());
	}

	private Map<Integer, Integer> partyOrderByNpcIndex(BaOverviewNpcType type)
	{
		return partyOrderByNpcIndexByType.computeIfAbsent(type, ignored -> new HashMap<>());
	}

	private Map<Integer, Integer> partyDeathTickByOrder(BaOverviewNpcType type)
	{
		return partyDeathTickByOrderByType.computeIfAbsent(type, ignored -> new HashMap<>());
	}

	private boolean putEarlierTick(Map<Integer, Integer> deathTickByKey, int key, int deathTick)
	{
		Integer previousDeathTick = deathTickByKey.get(key);

		if (previousDeathTick != null && previousDeathTick <= deathTick) return false;

		deathTickByKey.put(key, deathTick);
		return true;
	}

	private boolean removeOrderFromOtherIndexes(Map<Integer, Integer> orderByNpcIndex, int npcIndex, int order)
	{
		boolean changed = false;

		for (Map.Entry<Integer, Integer> entry : new ArrayList<>(orderByNpcIndex.entrySet()))
		{
			if (entry.getKey() != npcIndex && entry.getValue() == order)
			{
				orderByNpcIndex.remove(entry.getKey());
				changed = true;
			}
		}

		return changed;
	}

	private boolean isTrackable(BaOverviewNpcType type)
	{
		return type != null && type != BaOverviewNpcType.HEALER && isWaveActive();
	}

	private boolean isValidOrder(BaOverviewNpcType type, int order)
	{
		return order > 0 && order <= BaWaveInfo.getExpectedCount(wave, type);
	}

	private BaOverviewNpcType typeFromOrdinal(int ordinal)
	{
		BaOverviewNpcType[] values = BaOverviewNpcType.values();
		return ordinal < 0 || ordinal >= values.length ? null : values[ordinal];
	}

	private int length(int[] values)
	{
		return values == null ? 0 : values.length;
	}

	private int[] toIntArray(List<Integer> values)
	{
		int[] array = new int[values.size()];
		for (int i = 0; i < values.size(); i++)
		{
			array[i] = values.get(i);
		}
		return array;
	}
}
