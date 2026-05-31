package com.bahealerorder.codes;

import java.util.ArrayList;
import java.util.List;

public class RunPresetExport
{
	private RunPreset preset;
	private List<WaveCode> waveCodes = new ArrayList<>();

	public RunPresetExport()
	{
	}

	public RunPresetExport(RunPreset preset, List<WaveCode> waveCodes)
	{
		this.preset = preset;
		this.waveCodes = waveCodes == null ? new ArrayList<>() : new ArrayList<>(waveCodes);
	}

	public RunPreset getPreset()
	{
		return preset;
	}

	public void setPreset(RunPreset preset)
	{
		this.preset = preset;
	}

	public List<WaveCode> getWaveCodes()
	{
		if (waveCodes == null)
		{
			waveCodes = new ArrayList<>();
		}

		return waveCodes;
	}

	public void setWaveCodes(List<WaveCode> waveCodes)
	{
		this.waveCodes = waveCodes == null ? new ArrayList<>() : new ArrayList<>(waveCodes);
	}
}
