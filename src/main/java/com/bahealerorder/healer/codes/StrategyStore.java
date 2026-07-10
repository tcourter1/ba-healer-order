package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class StrategyStore
{
	@Getter
	@Setter
	private int version;
	@Getter
	@Setter
	private String activeRunPresetId;
	private Boolean codeEditorHelpVisible = true;
	@Getter
	private Map<Integer, String> activeWaveCodeIds = new HashMap<>();
	@Getter
	@Setter
	private Map<HealerCodeDefaultRole, String> defaultRunPresetIds = new HashMap<>();
	@Getter
	@Setter
	private List<RunPreset> runPresets = new ArrayList<>();
	@Getter
	@Setter
	private List<WaveCode> waveCodes = new ArrayList<>();

	public boolean isCodeEditorHelpVisible()
	{
		return codeEditorHelpVisible == null || codeEditorHelpVisible;
	}

	public void setCodeEditorHelpVisible(boolean codeEditorHelpVisible)
	{
		this.codeEditorHelpVisible = codeEditorHelpVisible;
	}

	public void setActiveWaveCodeIds(Map<Integer, String> activeWaveCodeIds)
	{
		this.activeWaveCodeIds = new HashMap<>(activeWaveCodeIds);
	}

	public String getDefaultRunPresetId(HealerCodeDefaultRole role)
	{
		return defaultRunPresetIds.get(role);
	}

	public void setDefaultRunPresetId(HealerCodeDefaultRole role, String runPresetId)
	{
		if (runPresetId == null || runPresetId.trim().isEmpty())
		{
			defaultRunPresetIds.remove(role);
		}
		else
		{
			defaultRunPresetIds.put(role, runPresetId);
		}
	}
}
