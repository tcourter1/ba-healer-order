package com.bahealerorder.common;

public class BaPartySyncMemberStatus
{
	private final String name;
	private final boolean inParty;

	public BaPartySyncMemberStatus(String name, boolean inParty)
	{
		this.name = name;
		this.inParty = inParty;
	}

	public String getName()
	{
		return name;
	}

	public boolean isInParty()
	{
		return inParty;
	}
}
