package com.bahealerorder.defender.strategies;

import java.util.ArrayList;
import java.util.List;

class DefenderMarkerClipboard
{
	private List<MarkerTemplate> markers = new ArrayList<>();

	public DefenderMarkerClipboard()
	{
	}

	private DefenderMarkerClipboard(List<MarkerTemplate> markers)
	{
		this.markers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
	}

	static DefenderMarkerClipboard fromMarkers(int wave, List<DefenderMarker> markers)
	{
		DefenderMapLayout layout = DefenderMapLayout.forWave(wave);
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

		return new DefenderMarkerClipboard(templates);
	}

	List<DefenderMarker> toMarkers(int wave)
	{
		DefenderMapLayout layout = DefenderMapLayout.forWave(wave);
		List<DefenderMarker> strategyMarkers = new ArrayList<>();
		List<MarkerTemplate> markerTemplates = markers == null ? new ArrayList<>() : markers;

		for (int i = 0; i < markerTemplates.size(); i++)
		{
			MarkerTemplate marker = markerTemplates.get(i);
			if (marker == null)
			{
				continue;
			}

			strategyMarkers.add(new DefenderMarker(
					"clipboard:" + wave + ":" + marker.mapX + ":" + marker.mapY + ":" + i,
					layout.toTile(marker.mapX, marker.mapY),
					marker.name,
					marker.label,
					marker.color,
					marker.opacityPercent,
					marker.borderWidth
			));
		}

		return strategyMarkers;
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
