package com.bahealerorder.common.strategies;

public interface WaveStrategy
{
	String getId();

	void setId(String id);

	String getName();

	void setName(String name);

	int getWave();

	void setWave(int wave);

	boolean isBuiltIn();

	void setBuiltIn(boolean builtIn);
}
