package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public class TileMarkerExportResult
{
	private final String json;
	private final String id;
	private final String name;
	private final TileMarkerExportType type;
	private final List<String> summaryLines;
	private final int markerCount;

	TileMarkerExportResult(
			String json,
			String name,
			TileMarkerExportType type,
			int markerCount)
	{
		this(json, null, name, type, Collections.emptyList(), markerCount);
	}

	TileMarkerExportResult(
			String json,
			String name,
			TileMarkerExportType type,
			List<String> summaryLines,
			int markerCount)
	{
		this(json, null, name, type, summaryLines, markerCount);
	}

	TileMarkerExportResult(
			String json,
			String id,
			String name,
			TileMarkerExportType type,
			int markerCount)
	{
		this(json, id, name, type, Collections.emptyList(), markerCount);
	}

	TileMarkerExportResult(
			String json,
			String id,
			String name,
			TileMarkerExportType type,
			List<String> summaryLines,
			int markerCount)
	{
		this.json = json;
		this.id = id;
		this.name = name;
		this.type = type;
		this.summaryLines = summaryLines == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(summaryLines));
		this.markerCount = markerCount;
	}

	public String getTypedName()
	{
		if (type == null) return name;

		if (type == TileMarkerExportType.MARKER_SET_COLLECTION
				|| type == TileMarkerExportType.STRATEGY_COLLECTION)
		{
			return type.getDisplayName();
		}

		return type.getDisplayName() + " " + name;
	}
}
