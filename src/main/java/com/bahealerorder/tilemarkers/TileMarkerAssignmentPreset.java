package com.bahealerorder.tilemarkers;

import java.util.HashMap;
import java.util.Map;

public class TileMarkerAssignmentPreset
{
	private String id;
	private String name;
	private String roleContext;
	private boolean builtIn;
	private Map<Integer, String> waveSelections = new HashMap<>();

	public TileMarkerAssignmentPreset()
	{
	}

	public TileMarkerAssignmentPreset(
			String id,
			String name,
			TileMarkerRoleContext roleContext,
			Map<Integer, String> waveSelections)
	{
		this(id, name, roleContext, waveSelections, false);
	}

	public TileMarkerAssignmentPreset(
			String id,
			String name,
			TileMarkerRoleContext roleContext,
			Map<Integer, String> waveSelections,
			boolean builtIn)
	{
		this.id = id;
		this.name = name;
		this.builtIn = builtIn;
		setRoleContext(roleContext);
		setWaveSelections(waveSelections);
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

	public boolean isBuiltIn()
	{
		return builtIn;
	}

	public void setBuiltIn(boolean builtIn)
	{
		this.builtIn = builtIn;
	}

	public TileMarkerRoleContext getRoleContext()
	{
		return TileMarkerRoleContext.fromName(roleContext);
	}

	public void setRoleContext(TileMarkerRoleContext roleContext)
	{
		this.roleContext = (roleContext == null ? TileMarkerRoleContext.DEFENDER : roleContext).name();
	}

	public Map<Integer, String> getWaveSelections()
	{
		if (waveSelections == null)
		{
			waveSelections = new HashMap<>();
		}
		return waveSelections;
	}

	public void setWaveSelections(Map<Integer, String> waveSelections)
	{
		this.waveSelections = copySelections(waveSelections);
	}

	private static Map<Integer, String> copySelections(
			Map<Integer, String> source)
	{
		Map<Integer, String> copy = new HashMap<>();
		if (source == null)
		{
			return copy;
		}

		for (Map.Entry<Integer, String> entry : source.entrySet())
		{
			String strategyId = entry.getValue();
			if (strategyId != null && !strategyId.trim().isEmpty())
			{
				copy.put(entry.getKey(), strategyId);
			}
		}
		return copy;
	}

	@Override
	public String toString()
	{
		return name == null || name.trim().isEmpty() ? "Unnamed preset" : name;
	}
}
