package com.bahealerorder.healer.ttk;

public class ObservedHealerHp
{
	private final int currentHp;
	private final int maxHp;

	public ObservedHealerHp(int currentHp, int maxHp)
	{
		this.currentHp = currentHp;
		this.maxHp = maxHp;
	}

	public int getCurrentHp()
	{
		return currentHp;
	}

	public int getMaxHp()
	{
		return maxHp;
	}
}
