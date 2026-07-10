package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TileMarkerStrategyCollectionExport
{
	private List<TileMarkerStrategyPreset> strategyPresets = new ArrayList<>();
	private List<TileMarkerSet> markerSets = new ArrayList<>();
}
