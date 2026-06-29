package com.bahealerorder.common.strategies;

import java.util.List;
import java.util.Map;

public interface WaveStrategyStore<P extends WaveRunPreset, S extends WaveStrategy>
{
	String getActiveRunPresetId();

	void setActiveRunPresetId(String activeRunPresetId);

	Map<Integer, String> getActiveWaveStrategyIds();

	void setActiveWaveStrategyIds(Map<Integer, String> activeWaveStrategyIds);

	List<P> getRunPresets();

	void setRunPresets(List<P> runPresets);

	List<S> getWaveStrategies();

	void setWaveStrategies(List<S> waveStrategies);
}
