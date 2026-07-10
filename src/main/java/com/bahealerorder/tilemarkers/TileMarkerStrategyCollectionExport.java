package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class TileMarkerStrategyCollectionExport
{
	private List<TileMarkerStrategyPreset> strategyPresets = new ArrayList<>();
	private List<TileMarkerSet> markerSets = new ArrayList<>();
}
