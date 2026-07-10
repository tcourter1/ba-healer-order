package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class TileMarkerStrategyStore
{
	@Getter
	@Setter
	private String lastMarkerColor = "#50aaff";
	@Getter
	@Setter
	private int lastMarkerOpacityPercent = TileMarker.DEFAULT_OPACITY_PERCENT;
	@Getter
	@Setter
	private float lastMarkerBorderWidth = TileMarker.DEFAULT_BORDER_WIDTH;
	private String lastMapMode = TileMarkerMapMode.FULL_MAP.name();
	private String lastWaveMap = TileMarkerWaveMap.WAVES_1_TO_9.name();
	private String selectedRoleContext = TileMarkerRoleContext.DEFENDER.name();
	@Getter
	@Setter
	private boolean markerEditorLegendVisible = true;
	@Getter
	@Setter
	private boolean beginnerPromptDismissed;
	@Getter
	@Setter
	private List<TileMarkerSet> markerSets = new ArrayList<>();
	@Getter
	@Setter
	private List<TileMarkerWaveSelection> waveSelections = new ArrayList<>();
	@Getter
	@Setter
	private List<TileMarkerStrategyPreset> strategyPresets = new ArrayList<>();
	@Getter
	private List<TileMarkerAssignmentPreset> assignmentPresets = new ArrayList<>();
	@Getter
	@Setter
	private Map<String, String> activeAssignmentPresetIds = new HashMap<>();

	public TileMarkerMapMode getLastMapMode()
	{
		return TileMarkerMapMode.fromName(lastMapMode);
	}

	public void setLastMapMode(TileMarkerMapMode lastMapMode)
	{
		this.lastMapMode = (lastMapMode == null ? TileMarkerMapMode.FULL_MAP : lastMapMode).name();
	}

	public TileMarkerWaveMap getLastWaveMap()
	{
		return TileMarkerWaveMap.fromName(lastWaveMap);
	}

	public void setLastWaveMap(TileMarkerWaveMap lastWaveMap)
	{
		this.lastWaveMap = (lastWaveMap == null ? TileMarkerWaveMap.WAVES_1_TO_9 : lastWaveMap).name();
	}

	public TileMarkerRoleContext getSelectedRoleContext()
	{
		return TileMarkerRoleContext.fromName(selectedRoleContext);
	}

	public void setSelectedRoleContext(TileMarkerRoleContext selectedRoleContext)
	{
		this.selectedRoleContext = (selectedRoleContext == null ? TileMarkerRoleContext.DEFENDER : selectedRoleContext).name();
	}

	public void setActiveAssignmentPresetId(TileMarkerRoleContext context, String presetId)
	{
		TileMarkerRoleContext resolved = context == null ? TileMarkerRoleContext.DEFENDER : context;
		if (presetId == null || presetId.isBlank())
		{
			getActiveAssignmentPresetIds().remove(resolved.name());
		}
		else
		{
			getActiveAssignmentPresetIds().put(resolved.name(), presetId);
		}
	}
}
