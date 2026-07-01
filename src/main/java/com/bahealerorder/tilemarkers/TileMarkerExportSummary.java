package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class TileMarkerExportSummary
{
	private TileMarkerExportSummary()
	{
	}

	static List<String> assignment(
			Map<Integer, String> waveSelections,
			Function<String, String> strategyName)
	{
		List<String> lines = new ArrayList<>();
		if (waveSelections == null || waveSelections.isEmpty())
		{
			return lines;
		}

		List<Integer> waves = new ArrayList<>(waveSelections.keySet());
		Collections.sort(waves);
		for (Integer wave : waves)
		{
			if (wave != null)
			{
				lines.add("Wave " + wave + ": " + strategyName.apply(waveSelections.get(wave)));
			}
		}
		return lines;
	}

	static List<String> markerSets(List<TileMarkerSet> markerSets)
	{
		List<String> lines = new ArrayList<>();
		if (markerSets == null)
		{
			return lines;
		}

		for (TileMarkerSet set : markerSets)
		{
			if (set != null)
			{
				lines.add(markerSetDisplayName(set));
			}
		}
		return lines;
	}

	static List<String> strategies(List<TileMarkerStrategyPreset> presets)
	{
		List<String> lines = new ArrayList<>();
		if (presets == null)
		{
			return lines;
		}

		for (TileMarkerStrategyPreset preset : presets)
		{
			if (preset != null)
			{
				lines.add(strategyPresetDisplayName(preset));
			}
		}
		return lines;
	}

	static List<String> strategyPreset(TileMarkerStrategyPreset preset, List<TileMarkerSet> markerSets)
	{
		List<String> lines = markerSets(markerSets);
		if (preset != null && !isBlank(preset.getNotes()))
		{
			lines.add("Notes: " + preset.getNotes().trim());
		}
		return lines;
	}

	private static String markerSetDisplayName(TileMarkerSet set)
	{
		return set == null || isBlank(set.getName()) ? "unnamed tile marker set" : set.getName().trim();
	}

	private static String strategyPresetDisplayName(TileMarkerStrategyPreset preset)
	{
		return preset == null || isBlank(preset.getName()) ? "unnamed wave strategy" : preset.getName().trim();
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
