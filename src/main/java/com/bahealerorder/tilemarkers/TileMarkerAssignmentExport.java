package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TileMarkerAssignmentExport
{
	private String name;
	private Map<Integer, String> waveSelections = new HashMap<>();
	private List<TileMarkerStrategyPreset> strategyPresets = new ArrayList<>();
	private List<TileMarkerSet> markerSets = new ArrayList<>();
}
