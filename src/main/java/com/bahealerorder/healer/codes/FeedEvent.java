package com.bahealerorder.healer.codes;

public class FeedEvent
{
	private final int healerOrder;
	private final int elapsedSeconds;
	private final int callIndex;

	public FeedEvent(int healerOrder, int elapsedSeconds, int callIndex)
	{
		this.healerOrder = healerOrder;
		this.elapsedSeconds = elapsedSeconds;
		this.callIndex = callIndex;
	}

	public int getHealerOrder()
	{
		return healerOrder;
	}

	public int getElapsedSeconds()
	{
		return elapsedSeconds;
	}

	public int getCallIndex()
	{
		return callIndex;
	}
}
