package com.bahealerorder.healer.codes;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class RunPreset
{
	private String id;
	private String name;
	private boolean builtIn;
	private Map<Integer, String> waveCodes = new HashMap<>();

	public RunPreset(String id, String name, boolean builtIn, Map<Integer, String> waveCodeIds)
	{
		this.id = id;
		this.name = name;
		this.builtIn = builtIn;
		this.waveCodes = waveCodeIds == null ? new HashMap<>() : new HashMap<>(waveCodeIds);
	}

	public Map<Integer, String> getWaveCodeIds()
	{
		return waveCodes;
	}

	public void setWaveCodeIds(Map<Integer, String> waveCodeIds)
	{
		this.waveCodes = new HashMap<>(waveCodeIds);
	}

	public String getWaveCodeId(int wave)
	{
		return getWaveCodeIds().get(wave);
	}

	public void setWaveCodeId(int wave, String waveCodeId)
	{
		if (waveCodeId == null || waveCodeId.trim().isEmpty())
		{
			getWaveCodeIds().remove(wave);
			return;
		}

		getWaveCodeIds().put(wave, waveCodeId);
	}

	@Override
	public String toString()
	{
		return name == null ? id : name;
	}
}
