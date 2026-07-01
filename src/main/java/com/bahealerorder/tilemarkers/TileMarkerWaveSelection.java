package com.bahealerorder.tilemarkers;

public class TileMarkerWaveSelection
{
	private String id;
	private String roleContext;
	private int wave;
	private String strategyId;

	public TileMarkerWaveSelection()
	{
	}

	public TileMarkerWaveSelection(String id, TileMarkerRoleContext roleContext, int wave)
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

	public String getStrategyId()
	{
		return strategyId;
	}

	public void setStrategyId(String strategyId)
	{
		this.strategyId = strategyId;
	}
}
