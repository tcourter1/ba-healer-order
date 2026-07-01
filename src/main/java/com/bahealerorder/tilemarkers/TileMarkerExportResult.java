package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TileMarkerExportResult
{
	private final String json;
	private final String id;
	private final String name;
	private final String wavesText;
	private final TileMarkerExportType type;
	private final List<String> summaryLines;
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
		this(json, null, name, wavesText, null, strategyCount, markerSetCount, markerCount);
	}

	TileMarkerExportResult(
			String json,
			String name,
			String wavesText,
			TileMarkerExportType type,
			List<String> summaryLines,
			int strategyCount,
			int markerSetCount,
			int markerCount)
	{
		this(json, null, name, wavesText, type, summaryLines, strategyCount, markerSetCount, markerCount);
	}

	TileMarkerExportResult(
			String json,
			String name,
			String wavesText,
			TileMarkerExportType type,
			int strategyCount,
			int markerSetCount,
			int markerCount)
	{
		this(json, null, name, wavesText, type, Collections.emptyList(), strategyCount, markerSetCount, markerCount);
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
		this(json, id, name, wavesText, null, Collections.emptyList(), strategyCount, markerSetCount, markerCount);
	}

	TileMarkerExportResult(
			String json,
			String id,
			String name,
			String wavesText,
			TileMarkerExportType type,
			int strategyCount,
			int markerSetCount,
			int markerCount)
	{
		this(json, id, name, wavesText, type, Collections.emptyList(), strategyCount, markerSetCount, markerCount);
	}

	TileMarkerExportResult(
			String json,
			String id,
			String name,
			String wavesText,
			TileMarkerExportType type,
			List<String> summaryLines,
			int strategyCount,
			int markerSetCount,
			int markerCount)
	{
		this.json = json;
		this.id = id;
		this.name = name;
		this.wavesText = wavesText;
		this.type = type;
		this.summaryLines = summaryLines == null ? Collections.emptyList() : new ArrayList<>(summaryLines);
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

	public String getTypedName()
	{
		if (type == null)
		{
			return name;
		}

		if (type == TileMarkerExportType.MARKER_SET_COLLECTION
				|| type == TileMarkerExportType.STRATEGY_COLLECTION)
		{
			return type.getDisplayName();
		}

		return type.getDisplayName() + " " + name;
	}

	public String getWavesText()
	{
		return wavesText;
	}

	public TileMarkerExportType getType()
	{
		return type;
	}

	public List<String> getSummaryLines()
	{
		return Collections.unmodifiableList(summaryLines);
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
