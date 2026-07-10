package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

class TileMarkerStrategyResolver
{
	private final Function<String, TileMarkerStrategyPreset> presetFinder;
	private final Function<String, TileMarkerSet> markerSetFinder;

	TileMarkerStrategyResolver(
			Function<String, TileMarkerStrategyPreset> presetFinder,
			Function<String, TileMarkerSet> markerSetFinder)
	{
		this.presetFinder = presetFinder;
		this.markerSetFinder = markerSetFinder;
	}

	List<TileMarker> activeMarkers(TileMarkerWaveSelection globalSelection, TileMarkerWaveSelection roleSelection)
	{
		List<TileMarker> markers = new ArrayList<>();
		Set<String> seenSetIds = new LinkedHashSet<>();
		addSelectionMarkers(markers, seenSetIds, globalSelection);
		addSelectionMarkers(markers, seenSetIds, roleSelection);
		return markers;
	}

	List<TileMarker> markersForPreset(TileMarkerStrategyPreset preset)
	{
		List<TileMarker> markers = new ArrayList<>();
		addPresetMarkers(markers, new LinkedHashSet<>(), preset);
		return markers;
	}

	String activeNotes(TileMarkerWaveSelection globalSelection, TileMarkerWaveSelection roleSelection)
	{
		List<String> notes = new ArrayList<>();
		addSelectionNotes(notes, globalSelection);
		addSelectionNotes(notes, roleSelection);
		return String.join("\n\n", notes);
	}

	private void addSelectionMarkers(List<TileMarker> markers, Set<String> seenSetIds, TileMarkerWaveSelection selection)
	{
		String strategyId = selection == null ? null : selection.getStrategyId();
		if (isBlank(strategyId)) return;

		addPresetMarkers(markers, seenSetIds, presetForSelection(selection));
	}

	private void addPresetMarkers(List<TileMarker> markers, Set<String> seenSetIds, TileMarkerStrategyPreset preset)
	{
		if (preset == null) return;

		for (String setId : preset.getMarkerSetIds())
		{
			addMarkerSetMarkers(markers, seenSetIds, setId);
		}
	}

	private void addMarkerSetMarkers(List<TileMarker> markers, Set<String> seenSetIds, String setId)
	{
		if (setId == null || !seenSetIds.add(setId)) return;

		TileMarkerSet set = markerSetFinder.apply(setId);
		if (set != null)
		{
			markers.addAll(set.getMarkers());
		}
	}

	private void addSelectionNotes(List<String> notes, TileMarkerWaveSelection selection)
	{
		TileMarkerStrategyPreset preset = presetForSelection(selection);
		if (preset == null) return;

		String text = preset.getNotes();
		if (text != null && !text.trim().isEmpty())
		{
			notes.add(text.trim());
		}
	}

	private TileMarkerStrategyPreset presetForSelection(TileMarkerWaveSelection selection)
	{
		String strategyId = selection == null ? null : selection.getStrategyId();
		if (isBlank(strategyId)) return null;

		TileMarkerStrategyPreset preset = presetFinder.apply(strategyId);
		return preset != null && preset.getWaveMap() == TileMarkerWaveMap.fromWave(selection.getWave()) ? preset : null;
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
