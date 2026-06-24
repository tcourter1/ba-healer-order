package com.bahealerorder.defender.strategies;

public class TimedDefenderNote
{
	private final String text;
	private final Integer tick;

	TimedDefenderNote(String text, Integer tick)
	{
		this.text = text;
		this.tick = tick;
	}

	public String getText()
	{
		return text;
	}

	public Integer getTick()
	{
		return tick;
	}

	public boolean isTimed()
	{
		return tick != null;
	}
}
