package com.bahealerorder.tilemarkers;

public class TileMarkerExportResult
{
	private final String json;
	private final String id;
	private final String name;
	private final String wavesText;
	private final int strategyCount;
	private final int markerSetCount;
	private final int markerCount;

	TileMarkerExportResult(
			String json,
			String name,
			String wavesText,
			int strategyCount,
			int markerSetCount,
			int markerCount)
	{
		this(json, null, name, wavesText, strategyCount, markerSetCount, markerCount);
	}

	TileMarkerExportResult(
			String json,
			String id,
			String name,
			String wavesText,
			int strategyCount,
			int markerSetCount,
			int markerCount)
	{
		this.json = json;
		this.id = id;
		this.name = name;
		this.wavesText = wavesText;
		this.strategyCount = strategyCount;
		this.markerSetCount = markerSetCount;
		this.markerCount = markerCount;
	}

	public String getJson()
	{
		return json;
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getWavesText()
	{
		return wavesText;
	}

	public int getStrategyCount()
	{
		return strategyCount;
	}

	public int getMarkerSetCount()
	{
		return markerSetCount;
	}

	public int getMarkerCount()
	{
		return markerCount;
	}
}
