package com.bahealerorder.codes;

public class HealerCodeStatus
{
	private final HealerInstruction instruction;
	private final CodeDisplayState state;
	private final int foodFed;
	private final int lastFoodElapsedSeconds;

	public HealerCodeStatus(HealerInstruction instruction, CodeDisplayState state, int foodFed, int lastFoodElapsedSeconds)
	{
		this.instruction = instruction;
		this.state = state;
		this.foodFed = foodFed;
		this.lastFoodElapsedSeconds = lastFoodElapsedSeconds;
	}

	public HealerInstruction getInstruction()
	{
		return instruction;
	}

	public CodeDisplayState getState()
	{
		return state;
	}

	public int getFoodFed()
	{
		return foodFed;
	}

	public int getLastFoodElapsedSeconds()
	{
		return lastFoodElapsedSeconds;
	}
}
