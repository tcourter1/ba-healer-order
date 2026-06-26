package com.bahealerorder.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BaWaveInfo
{
	private static final int MAX_WAVE = 10;

	private static final int[] RUNNER_INITIAL = {0, 2, 2, 2, 3, 4, 4, 5, 5, 5, 5};
	private static final int[] RUNNER_TOTAL = {0, 2, 3, 4, 4, 5, 6, 6, 7, 9, 6};
	private static final int[] HEALER_INITIAL = {0, 2, 3, 2, 3, 4, 4, 4, 5, 6, 4};
	private static final int[] HEALER_TOTAL = {0, 2, 3, 3, 4, 5, 6, 7, 7, 8, 7};
	private static final int[] RANGER_INITIAL = {0, 4, 4, 6, 6, 6, 6, 6, 6, 6, 6};
	private static final int[] RANGER_TOTAL = {0, 4, 4, 6, 6, 6, 7, 7, 8, 8, 7};
	private static final int[] FIGHTER_INITIAL = {0, 4, 5, 5, 6, 3, 6, 5, 6, 6, 5};
	private static final int[] FIGHTER_TOTAL = {0, 4, 5, 5, 6, 6, 6, 7, 7, 8, 7};

	private BaWaveInfo()
	{
	}

	public static boolean isValidWave(int wave)
	{
		return wave >= 1 && wave <= MAX_WAVE;
	}

	public static int getExpectedCount(int wave, BaOverviewNpcType type)
	{
		return getValue(wave, getTotalCounts(type));
	}

	public static List<String> getLabels(int wave, BaOverviewNpcType type)
	{
		if (!isValidWave(wave)) return Collections.emptyList();

		int initial = getValue(wave, getInitialCounts(type));
		int total = getValue(wave, getTotalCounts(type));
		List<String> labels = new ArrayList<>(total);

		for (int i = 1; i <= Math.min(initial, total); i++)
		{
			labels.add((i * 6) + "s");
		}

		for (int i = labels.size() + 1; i <= total; i++)
		{
			int respawnNumber = i - initial;
			int spawnTimeSeconds = i * 6;
			labels.add(spawnTimeSeconds + "s (R" + respawnNumber + ")");
		}

		return Collections.unmodifiableList(labels);
	}

	private static int getValue(int wave, int[] values)
	{
		return isValidWave(wave) && wave < values.length ? values[wave] : 0;
	}

	private static int[] getInitialCounts(BaOverviewNpcType type)
	{
		switch (type)
		{
			case RANGER:
				return RANGER_INITIAL;
			case FIGHTER:
				return FIGHTER_INITIAL;
			case RUNNER:
				return RUNNER_INITIAL;
			case HEALER:
				return HEALER_INITIAL;
			default:
				throw new IllegalArgumentException("Unsupported NPC type " + type);
		}
	}

	private static int[] getTotalCounts(BaOverviewNpcType type)
	{
		switch (type)
		{
			case RANGER:
				return RANGER_TOTAL;
			case FIGHTER:
				return FIGHTER_TOTAL;
			case RUNNER:
				return RUNNER_TOTAL;
			case HEALER:
				return HEALER_TOTAL;
			default:
				throw new IllegalArgumentException("Unsupported NPC type " + type);
		}
	}
}
