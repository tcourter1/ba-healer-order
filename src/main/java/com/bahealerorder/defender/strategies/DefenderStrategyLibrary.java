package com.bahealerorder.defender.strategies;

public final class DefenderStrategyLibrary
{
	private DefenderStrategyLibrary()
	{
	}

	public static DefenderStrategyStore create()
	{
		return new DefenderStrategyStore();
	}
}
