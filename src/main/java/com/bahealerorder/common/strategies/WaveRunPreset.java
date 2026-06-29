package com.bahealerorder.common.strategies;

import java.util.Map;

public interface WaveRunPreset
{
	String getId();

	void setId(String id);

	String getName();

	void setName(String name);

	boolean isBuiltIn();

	void setBuiltIn(boolean builtIn);

	Map<Integer, String> getWaveStrategyIds();

	void setWaveStrategyIds(Map<Integer, String> waveStrategyIds);

	default String getWaveStrategyId(int wave)
	{
		return getWaveStrategyIds().get(wave);
	}
}
