package com.bahealerorder.common;

public class BaPartySyncMemberStatus
{
	private final String name;
	private final String role;
	private final boolean inParty;

	public BaPartySyncMemberStatus(String name, boolean inParty)
	{
		this(name, null, inParty);
	}

	public BaPartySyncMemberStatus(String name, String role, boolean inParty)
	{
		this.name = name;
		this.role = role;
		this.inParty = inParty;
	}

	public String getName()
	{
		return name;
	}

	public String getRole()
	{
		return role;
	}

	public boolean isInParty()
	{
		return inParty;
	}
}
