package com.bahealerorder.tilemarkers;

import com.bahealerorder.defender.TileMarkerMapMode;
import com.bahealerorder.defender.TileMarkerWaveMap;
import com.bahealerorder.defender.strategies.DefenderMarker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TileMarkerSet
{
	private String id;
	private String name;
	private String mapMode;
	private String waveMap;
	private boolean builtIn;
	private List<DefenderMarker> markers = new ArrayList<>();

	public TileMarkerSet()
	{
	}

	public TileMarkerSet(
			String id,
			String name,
			TileMarkerMapMode mapMode,
			TileMarkerWaveMap waveMap,
			List<DefenderMarker> markers)
	{
		this(id, name, mapMode, waveMap, markers, false);
	}

	public TileMarkerSet(
			String id,
			String name,
			TileMarkerMapMode mapMode,
			TileMarkerWaveMap waveMap,
			List<DefenderMarker> markers,
			boolean builtIn)
	{
		this.id = id;
		this.name = name;
		this.mapMode = (mapMode == null ? TileMarkerMapMode.FULL_MAP : mapMode).name();
		this.waveMap = (waveMap == null ? TileMarkerWaveMap.WAVES_1_TO_9 : waveMap).name();
		this.builtIn = builtIn;
		this.markers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
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

	public boolean isBuiltIn()
	{
		return builtIn;
	}

	public void setBuiltIn(boolean builtIn)
	{
		this.builtIn = builtIn;
	}

	public List<DefenderMarker> getMarkers()
	{
		return markers == null ? Collections.emptyList() : markers;
	}

	public void setMarkers(List<DefenderMarker> markers)
	{
		this.markers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
	}

	@Override
	public String toString()
	{
		return name == null || name.trim().isEmpty() ? "Unnamed set" : name;
	}
}
