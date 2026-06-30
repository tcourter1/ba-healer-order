package com.bahealerorder.tilemarkers;

import java.util.HashMap;
import java.util.Map;

public class TileMarkerAssignmentPreset
{
	private String id;
	private String name;
	private String roleContext;
	private boolean builtIn;
	private Map<Integer, TileMarkerWaveSelectionTarget> waveSelections = new HashMap<>();

	public TileMarkerAssignmentPreset()
	{
	}

	public TileMarkerAssignmentPreset(
			String id,
			String name,
			TileMarkerRoleContext roleContext,
			Map<Integer, TileMarkerWaveSelectionTarget> waveSelections)
	{
		this(id, name, roleContext, waveSelections, false);
	}

	public TileMarkerAssignmentPreset(
			String id,
			String name,
			TileMarkerRoleContext roleContext,
			Map<Integer, TileMarkerWaveSelectionTarget> waveSelections,
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

	public Map<Integer, TileMarkerWaveSelectionTarget> getWaveSelections()
	{
		if (waveSelections == null)
		{
			waveSelections = new HashMap<>();
		}
		return waveSelections;
	}

	public void setWaveSelections(Map<Integer, TileMarkerWaveSelectionTarget> waveSelections)
	{
		this.waveSelections = copySelections(waveSelections);
	}

	private static Map<Integer, TileMarkerWaveSelectionTarget> copySelections(
			Map<Integer, TileMarkerWaveSelectionTarget> source)
	{
		Map<Integer, TileMarkerWaveSelectionTarget> copy = new HashMap<>();
		if (source == null)
		{
			return copy;
		}

		for (Map.Entry<Integer, TileMarkerWaveSelectionTarget> entry : source.entrySet())
		{
			TileMarkerWaveSelectionTarget target = entry.getValue();
			if (target != null)
			{
				copy.put(entry.getKey(), new TileMarkerWaveSelectionTarget(target));
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
