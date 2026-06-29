package com.bahealerorder.tilemarkers;

import java.util.HashMap;
import java.util.Map;

public class TileMarkerAssignmentPreset
{
	private String id;
	private String name;
	private String roleContext;
	private boolean builtIn;
	private Map<Integer, String> waveStrategyPresetIds = new HashMap<>();

	public TileMarkerAssignmentPreset()
	{
	}

	public TileMarkerAssignmentPreset(String id, String name, TileMarkerRoleContext roleContext, Map<Integer, String> waveStrategyPresetIds)
	{
		this(id, name, roleContext, waveStrategyPresetIds, false);
	}

	public TileMarkerAssignmentPreset(
			String id,
			String name,
			TileMarkerRoleContext roleContext,
			Map<Integer, String> waveStrategyPresetIds,
			boolean builtIn)
	{
		this.id = id;
		this.name = name;
		this.builtIn = builtIn;
		setRoleContext(roleContext);
		setWaveStrategyPresetIds(waveStrategyPresetIds);
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

	public Map<Integer, String> getWaveStrategyPresetIds()
	{
		if (waveStrategyPresetIds == null)
		{
			waveStrategyPresetIds = new HashMap<>();
		}
		return waveStrategyPresetIds;
	}

	public void setWaveStrategyPresetIds(Map<Integer, String> waveStrategyPresetIds)
	{
		this.waveStrategyPresetIds = waveStrategyPresetIds == null ? new HashMap<>() : new HashMap<>(waveStrategyPresetIds);
	}

	@Override
	public String toString()
	{
		return name == null || name.trim().isEmpty() ? "Unnamed preset" : name;
	}
}
