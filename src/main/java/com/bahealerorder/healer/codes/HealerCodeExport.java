package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.List;

public class HealerCodeExport
{
	private int version;
	private HealerCodeExportType type;
	private RunPreset preset;
	private WaveCode waveCode;
	private List<WaveCode> waveCodes = new ArrayList<>();

	public HealerCodeExport()
	{
	}

	public HealerCodeExport(int version, HealerCodeExportType type, RunPreset preset, WaveCode waveCode, List<WaveCode> waveCodes)
	{
		this.version = version;
		this.type = type;
		this.preset = preset;
		this.waveCode = waveCode;
		this.waveCodes = waveCodes == null ? new ArrayList<>() : new ArrayList<>(waveCodes);
	}

	public int getVersion()
	{
		return version;
	}

	public void setVersion(int version)
	{
		this.version = version;
	}

	public HealerCodeExportType getType()
	{
		return type;
	}

	public void setType(HealerCodeExportType type)
	{
		this.type = type;
	}

	public RunPreset getPreset()
	{
		return preset;
	}

	public void setPreset(RunPreset preset)
	{
		this.preset = preset;
	}

	public WaveCode getWaveCode()
	{
		return waveCode;
	}

	public void setWaveCode(WaveCode waveCode)
	{
		this.waveCode = waveCode;
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
