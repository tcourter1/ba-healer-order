package com.bahealerorder.tilemarkers;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
public class TileMarkerAssignmentPreset
{
	@Setter
	private String id;
	@Setter
	private String name;
	private String roleContext;
	@Setter
	private boolean builtIn;
	@Setter
	private Map<Integer, String> waveSelections = new HashMap<>();

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

	public TileMarkerRoleContext getRoleContext()
	{
		return TileMarkerRoleContext.fromName(roleContext);
	}

	public void setRoleContext(TileMarkerRoleContext roleContext)
	{
		this.roleContext = (roleContext == null ? TileMarkerRoleContext.DEFENDER : roleContext).name();
	}

	@Override
	public String toString()
	{
		return name == null || name.trim().isEmpty() ? "Unnamed preset" : name;
	}
}
