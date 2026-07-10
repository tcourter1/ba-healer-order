package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class HealerCodeExport
{
	private int version;
	private HealerCodeExportType type;
	private RunPreset preset;
	private WaveCode waveCode;
	private List<WaveCode> waveCodes = new ArrayList<>();

	public HealerCodeExport(int version, HealerCodeExportType type, RunPreset preset, WaveCode waveCode, List<WaveCode> waveCodes)
	{
		this.version = version;
		this.type = type;
		this.preset = preset;
		this.waveCode = waveCode;
		this.waveCodes = waveCodes == null ? new ArrayList<>() : new ArrayList<>(waveCodes);
	}

}
