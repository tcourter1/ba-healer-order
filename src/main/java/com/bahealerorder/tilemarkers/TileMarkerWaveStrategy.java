package com.bahealerorder.tilemarkers;

public class TileMarkerWaveStrategy
{
	private String id;
	private String roleContext;
	private int wave;
	private String strategyPresetId;

	public TileMarkerWaveStrategy()
	{
	}

	public TileMarkerWaveStrategy(String id, TileMarkerRoleContext roleContext, int wave)
	{
		this.id = id;
		this.roleContext = (roleContext == null ? TileMarkerRoleContext.GLOBAL : roleContext).name();
		this.wave = wave;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public TileMarkerRoleContext getRoleContext()
	{
		return TileMarkerRoleContext.fromName(roleContext);
	}

	public void setRoleContext(TileMarkerRoleContext roleContext)
	{
		this.roleContext = (roleContext == null ? TileMarkerRoleContext.GLOBAL : roleContext).name();
	}

	public int getWave()
	{
		return wave;
	}

	public void setWave(int wave)
	{
		this.wave = wave;
	}

	public String getStrategyPresetId()
	{
		return strategyPresetId;
	}

	public void setStrategyPresetId(String strategyPresetId)
	{
		this.strategyPresetId = strategyPresetId;
	}
}
