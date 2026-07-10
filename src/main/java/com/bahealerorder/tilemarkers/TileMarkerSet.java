package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
public class TileMarkerSet
{
	@Setter
	private String id;
	@Setter
	private String name;
	private String mapMode;
	private String waveMap;
	@Setter
	private boolean builtIn;
	@Setter
	private List<TileMarker> markers = new ArrayList<>();

	public TileMarkerSet(
			String id,
			String name,
			TileMarkerMapMode mapMode,
			TileMarkerWaveMap waveMap,
			List<TileMarker> markers)
	{
		this(id, name, mapMode, waveMap, markers, false);
	}

	public TileMarkerSet(
			String id,
			String name,
			TileMarkerMapMode mapMode,
			TileMarkerWaveMap waveMap,
			List<TileMarker> markers,
			boolean builtIn)
	{
		this.id = id;
		this.name = name;
		this.mapMode = (mapMode == null ? TileMarkerMapMode.FULL_MAP : mapMode).name();
		this.waveMap = (waveMap == null ? TileMarkerWaveMap.WAVES_1_TO_9 : waveMap).name();
		this.builtIn = builtIn;
		this.markers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
	}

	public TileMarkerMapMode getMapMode()
	{
		return TileMarkerMapMode.fromName(mapMode);
	}

	public void setMapMode(TileMarkerMapMode mapMode)
	{
		this.mapMode = (mapMode == null ? TileMarkerMapMode.FULL_MAP : mapMode).name();
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
		return name == null || name.isBlank() ? "Unnamed set" : name;
	}
}
