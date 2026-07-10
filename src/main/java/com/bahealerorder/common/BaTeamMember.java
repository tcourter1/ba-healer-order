package com.bahealerorder.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class BaTeamMember
{
	private final String name;
	private final String role;

	@Override
	public String toString()
	{
		return name + ":" + role;
	}
}
