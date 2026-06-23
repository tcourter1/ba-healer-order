package com.bahealerorder.common;

import java.util.Objects;

public class BaTeamMember
{
	private final String name;
	private final String role;

	public BaTeamMember(String name, String role)
	{
		this.name = name;
		this.role = role;
	}

	public String getName()
	{
		return name;
	}

	public String getRole()
	{
		return role;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof BaTeamMember)) return false;

		BaTeamMember member = (BaTeamMember) other;
		return Objects.equals(name, member.name)
				&& Objects.equals(role, member.role);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, role);
	}

	@Override
	public String toString()
	{
		return name + ":" + role;
	}
}
