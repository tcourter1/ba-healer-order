package com.bahealerorder.defender.strategies;

import java.util.ArrayList;
import java.util.List;

class DefenderWaveStrategyTemplate
{
	private String name;
	private String notes;
	private int numberOfLogs;
	private List<MarkerTemplate> markers = new ArrayList<>();

	public DefenderWaveStrategyTemplate()
	{
	}

	private DefenderWaveStrategyTemplate(String name, String notes, int numberOfLogs, List<MarkerTemplate> markers)
	{
		this.name = name;
		this.notes = notes;
		this.numberOfLogs = numberOfLogs;
		this.markers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
	}

	static DefenderWaveStrategyTemplate fromStrategy(DefenderWaveStrategy strategy)
	{
		DefenderMapLayout layout = DefenderMapLayout.forWave(strategy.getWave());
		List<MarkerTemplate> templates = new ArrayList<>();

		for (DefenderMarker marker : strategy.getMarkers())
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

		return new DefenderWaveStrategyTemplate(strategy.getName(), strategy.getNotes(), strategy.getNumberOfLogs(), templates);
	}

	DefenderWaveStrategy toStrategy(int wave, String id, boolean builtIn)
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
					"marker:" + wave + ":" + marker.mapX + ":" + marker.mapY + ":" + i,
					layout.toTile(marker.mapX, marker.mapY),
					marker.name,
					marker.label,
					marker.color,
					marker.opacityPercent,
					marker.borderWidth
			));
		}

		return new DefenderWaveStrategy(id, name, wave, builtIn, notes, numberOfLogs, strategyMarkers);
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
