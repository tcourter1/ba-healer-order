package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TileMarkerStrategyPresetExport
{
	private TileMarkerStrategyPreset strategyPreset;
	private List<TileMarkerSet> markerSets = new ArrayList<>();
}
