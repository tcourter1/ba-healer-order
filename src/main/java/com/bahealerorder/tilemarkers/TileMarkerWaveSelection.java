package com.bahealerorder.tilemarkers;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
public class TileMarkerWaveSelection
{
	@Setter
	private String id;
	private String roleContext;
	private int wave;
	@Setter
	private String strategyId;

	public TileMarkerWaveSelection(String id, TileMarkerRoleContext roleContext, int wave)
	{
		this.id = id;
		this.roleContext = (roleContext == null ? TileMarkerRoleContext.GLOBAL : roleContext).name();
		this.wave = wave;
	}

	public TileMarkerRoleContext getRoleContext()
	{
		return TileMarkerRoleContext.fromName(roleContext);
	}

	public void setRoleContext(TileMarkerRoleContext roleContext)
	{
		this.roleContext = (roleContext == null ? TileMarkerRoleContext.GLOBAL : roleContext).name();
	}

}
