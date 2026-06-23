package com.bahealerorder.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NpcIndexOrderer
{
	public static final int NPC_INDEX_MODULUS = 1 << 16;

	private NpcIndexOrderer()
	{
	}

	public static Map<Integer, Integer> buildOrderByNpcIndex(
			Set<Integer> seenNpcIndexes,
			Map<Integer, Integer> knownOrderByNpcIndex,
			int indexBase,
			int maxOrder)
	{
		Map<Integer, Integer> orderByNpcIndex = new HashMap<>();
		Set<Integer> usedOrders = new HashSet<>();

		if (knownOrderByNpcIndex != null)
		{
			for (Map.Entry<Integer, Integer> entry : knownOrderByNpcIndex.entrySet())
			{
				Integer npcIndex = entry.getKey();
				Integer order = entry.getValue();

				if (npcIndex == null || order == null || npcIndex < 0 || order <= 0 || order > maxOrder) continue;

				orderByNpcIndex.put(npcIndex, order);
				usedOrders.add(order);
			}
		}

		if (seenNpcIndexes == null || seenNpcIndexes.isEmpty()) return orderByNpcIndex;

		List<Integer> sortedIndexes = new ArrayList<>(seenNpcIndexes);
		sortedIndexes.sort((left, right) -> Integer.compare(normalize(left, indexBase), normalize(right, indexBase)));
		int nextOrder = 1;

		for (int npcIndex : sortedIndexes)
		{
			if (orderByNpcIndex.containsKey(npcIndex)) continue;

			while (usedOrders.contains(nextOrder))
			{
				nextOrder++;
			}

			if (nextOrder > maxOrder) break;

			orderByNpcIndex.put(npcIndex, nextOrder);
			usedOrders.add(nextOrder);
			nextOrder++;
		}

		return orderByNpcIndex;
	}

	public static int normalize(int npcIndex, int indexBase)
	{
		if (indexBase < 0)
		{
			return npcIndex;
		}

		return Math.floorMod(npcIndex - indexBase, NPC_INDEX_MODULUS);
	}
}
