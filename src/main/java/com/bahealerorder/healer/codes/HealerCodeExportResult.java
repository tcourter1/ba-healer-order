package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HealerCodeExportResult
{
	private final String json;
	private final String id;
	private final String name;
	private final int wave;
	private final HealerCodeExportType type;
	private final List<String> summaryLines;

	public HealerCodeExportResult(
			String json,
			String id,
			String name,
			int wave,
			HealerCodeExportType type,
			List<String> summaryLines)
	{
		this.json = json;
		this.id = id;
		this.name = name;
		this.wave = wave;
		this.type = type;
		this.summaryLines = summaryLines == null ? Collections.emptyList() : new ArrayList<>(summaryLines);
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

	public int getWave()
	{
		return wave;
	}

	public HealerCodeExportType getType()
	{
		return type;
	}

	public String getTypedName()
	{
		String displayName = isBlank(name) ? unnamedTypeName() : name;
		return type == null ? displayName : type.getDisplayName() + " " + displayName;
	}

	public List<String> getSummaryLines()
	{
		return Collections.unmodifiableList(summaryLines);
	}

	private String unnamedTypeName()
	{
		if (type == HealerCodeExportType.RUN_PRESET)
		{
			return "unnamed run preset";
		}
		if (type == HealerCodeExportType.WAVE_CODE)
		{
			return "unnamed wave code";
		}
		return "unnamed export";
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
