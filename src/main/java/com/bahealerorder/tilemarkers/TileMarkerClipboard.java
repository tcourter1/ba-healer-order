package com.bahealerorder.tilemarkers;

import com.bahealerorder.defender.TileMarkerWaveMap;
import com.bahealerorder.defender.strategies.DefenderMapLayout;
import com.bahealerorder.defender.strategies.DefenderMarker;
import java.util.ArrayList;
import java.util.List;

class TileMarkerClipboard
{
	private List<MarkerTemplate> markers = new ArrayList<>();

	public TileMarkerClipboard()
	{
	}

	private TileMarkerClipboard(List<MarkerTemplate> markers)
	{
		this.markers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
	}

	static TileMarkerClipboard fromMarkers(TileMarkerWaveMap waveMap, List<DefenderMarker> markers)
	{
		DefenderMapLayout layout = waveMap.getLayout();
		List<MarkerTemplate> templates = new ArrayList<>();

		if (markers != null)
		{
			for (DefenderMarker marker : markers)
			{
				if (marker == null || !layout.contains(marker.getTile()))
				{
					continue;
				}

				templates.add(new MarkerTemplate(
						layout.toMapX(marker.getTile()),
						layout.toMapY(marker.getTile()),
						marker.getName(),
						marker.getLabel(),
						marker.getColor(),
						marker.getOpacityPercent(),
						marker.getBorderWidth()
				));
			}
		}

		return new TileMarkerClipboard(templates);
	}

	List<DefenderMarker> toMarkers(TileMarkerWaveMap waveMap)
	{
		DefenderMapLayout layout = waveMap.getLayout();
		List<DefenderMarker> copiedMarkers = new ArrayList<>();
		List<MarkerTemplate> templates = markers == null ? new ArrayList<>() : markers;

		for (int i = 0; i < templates.size(); i++)
		{
			MarkerTemplate marker = templates.get(i);
			if (marker == null)
			{
				continue;
			}

			copiedMarkers.add(new DefenderMarker(
					"clipboard:" + waveMap.name() + ":" + marker.mapX + ":" + marker.mapY + ":" + i,
					layout.toTile(marker.mapX, marker.mapY),
					marker.name,
					marker.label,
					marker.color,
					marker.opacityPercent,
					marker.borderWidth
			));
		}

		return copiedMarkers;
	}

	private static class MarkerTemplate
	{
		private int mapX;
		private int mapY;
		private String name;
		private String label;
		private String color;
		private Integer opacityPercent;
		private Float borderWidth;

		public MarkerTemplate()
		{
		}

		private MarkerTemplate(int mapX, int mapY, String name, String label, String color, Integer opacityPercent, Float borderWidth)
		{
			this.mapX = mapX;
			this.mapY = mapY;
			this.name = name;
			this.label = label;
			this.color = color;
			this.opacityPercent = opacityPercent;
			this.borderWidth = borderWidth;
		}
	}
}
