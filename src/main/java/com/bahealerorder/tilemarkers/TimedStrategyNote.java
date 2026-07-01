package com.bahealerorder.tilemarkers;

public class TimedStrategyNote
{
	private final String text;
	private final Integer tick;

	TimedStrategyNote(String text, Integer tick)
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
