package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
public class TileMarkerStrategyPreset
{
	@Setter
	private String id;
	@Setter
	private String name;
	@Setter
	private String notes;
	private String waveMap;
	@Setter
	private boolean builtIn;
	@Setter
	private List<String> markerSetIds = new ArrayList<>();

	public TileMarkerStrategyPreset(
			String id,
			String name,
			String notes,
			TileMarkerWaveMap waveMap,
			List<String> markerSetIds)
	{
		this(id, name, notes, waveMap, markerSetIds, false);
	}

	public TileMarkerStrategyPreset(
			String id,
			String name,
			String notes,
			TileMarkerWaveMap waveMap,
			List<String> markerSetIds,
			boolean builtIn)
	{
		this.id = id;
		this.name = name;
		this.notes = notes;
		this.waveMap = (waveMap == null ? TileMarkerWaveMap.WAVES_1_TO_9 : waveMap).name();
		this.builtIn = builtIn;
		setMarkerSetIds(markerSetIds);
	}

	public TileMarkerWaveMap getWaveMap()
	{
		return TileMarkerWaveMap.fromName(waveMap);
	}

	public void setWaveMap(TileMarkerWaveMap waveMap)
	{
		this.waveMap = (waveMap == null ? TileMarkerWaveMap.WAVES_1_TO_9 : waveMap).name();
	}

	@Override
	public String toString()
	{
		return name == null || name.trim().isEmpty() ? "Unnamed strategy" : name;
	}
}
