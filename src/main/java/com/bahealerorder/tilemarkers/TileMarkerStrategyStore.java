package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileMarkerStrategyStore
{
	private String lastMarkerColor = "#50aaff";
	private int lastMarkerOpacityPercent = TileMarker.DEFAULT_OPACITY_PERCENT;
	private float lastMarkerBorderWidth = TileMarker.DEFAULT_BORDER_WIDTH;
	private String lastMapMode = TileMarkerMapMode.FULL_MAP.name();
	private String lastWaveMap = TileMarkerWaveMap.WAVES_1_TO_9.name();
	private String selectedRoleContext = TileMarkerRoleContext.DEFENDER.name();
	private boolean markerEditorLegendVisible = true;
	private boolean beginnerPromptDismissed;
	private List<TileMarkerSet> markerSets = new ArrayList<>();
	private List<TileMarkerWaveSelection> waveSelections = new ArrayList<>();
	private List<TileMarkerStrategyPreset> strategyPresets = new ArrayList<>();
	private List<TileMarkerAssignmentPreset> assignmentPresets = new ArrayList<>();
	private Map<String, String> activeAssignmentPresetIds = new HashMap<>();

	public String getLastMarkerColor()
	{
		return lastMarkerColor;
	}

	public void setLastMarkerColor(String lastMarkerColor)
	{
		this.lastMarkerColor = lastMarkerColor;
	}

	public int getLastMarkerOpacityPercent()
	{
		return lastMarkerOpacityPercent;
	}

	public void setLastMarkerOpacityPercent(int lastMarkerOpacityPercent)
	{
		this.lastMarkerOpacityPercent = lastMarkerOpacityPercent;
	}

	public float getLastMarkerBorderWidth()
	{
		return lastMarkerBorderWidth;
	}

	public void setLastMarkerBorderWidth(float lastMarkerBorderWidth)
	{
		this.lastMarkerBorderWidth = lastMarkerBorderWidth;
	}

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

	public boolean isMarkerEditorLegendVisible()
	{
		return markerEditorLegendVisible;
	}

	public void setMarkerEditorLegendVisible(boolean markerEditorLegendVisible)
	{
		this.markerEditorLegendVisible = markerEditorLegendVisible;
	}

	public boolean isBeginnerPromptDismissed()
	{
		return beginnerPromptDismissed;
	}

	public void setBeginnerPromptDismissed(boolean beginnerPromptDismissed)
	{
		this.beginnerPromptDismissed = beginnerPromptDismissed;
	}

	public List<TileMarkerSet> getMarkerSets()
	{
		if (markerSets == null)
		{
			markerSets = new ArrayList<>();
		}
		return markerSets;
	}

	public void setMarkerSets(List<TileMarkerSet> markerSets)
	{
		this.markerSets = markerSets == null ? new ArrayList<>() : new ArrayList<>(markerSets);
	}

	public List<TileMarkerWaveSelection> getWaveSelections()
	{
		if (waveSelections == null)
		{
			waveSelections = new ArrayList<>();
		}
		return waveSelections;
	}

	public void setWaveSelections(List<TileMarkerWaveSelection> waveSelections)
	{
		this.waveSelections = waveSelections == null ? new ArrayList<>() : new ArrayList<>(waveSelections);
	}

	public List<TileMarkerStrategyPreset> getStrategyPresets()
	{
		if (strategyPresets == null)
		{
			strategyPresets = new ArrayList<>();
		}
		return strategyPresets;
	}

	public void setStrategyPresets(List<TileMarkerStrategyPreset> strategyPresets)
	{
		this.strategyPresets = strategyPresets == null ? new ArrayList<>() : new ArrayList<>(strategyPresets);
	}

	public List<TileMarkerAssignmentPreset> getAssignmentPresets()
	{
		if (assignmentPresets == null)
		{
			assignmentPresets = new ArrayList<>();
		}
		return assignmentPresets;
	}

	public void setAssignmentPresets(List<TileMarkerAssignmentPreset> assignmentPresets)
	{
		this.assignmentPresets = assignmentPresets == null ? new ArrayList<>() : new ArrayList<>(assignmentPresets);
	}

	public Map<String, String> getActiveAssignmentPresetIds()
	{
		if (activeAssignmentPresetIds == null)
		{
			activeAssignmentPresetIds = new HashMap<>();
		}
		return activeAssignmentPresetIds;
	}

	public void setActiveAssignmentPresetId(TileMarkerRoleContext context, String presetId)
	{
		TileMarkerRoleContext resolved = context == null ? TileMarkerRoleContext.DEFENDER : context;
		if (presetId == null || presetId.trim().isEmpty())
		{
			getActiveAssignmentPresetIds().remove(resolved.name());
		}
		else
		{
			getActiveAssignmentPresetIds().put(resolved.name(), presetId);
		}
	}
}
