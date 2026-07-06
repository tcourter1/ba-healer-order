package com.bahealerorder.tilemarkers;

import com.bahealerorder.common.BaRole;

public enum TileMarkerRoleContext
{
	DEFENDER("Defender", BaRole.DEFENDER),
	COLLECTOR("Collector", BaRole.COLLECTOR),
	HEALER("Healer", BaRole.HEALER),
	ATTACKER("Attacker", BaRole.ATTACKER),
	GLOBAL("Global", null);

	private final String displayName;
	private final BaRole role;

	TileMarkerRoleContext(String displayName, BaRole role)
	{
		this.displayName = displayName;
		this.role = role;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}

	public BaRole getRole()
	{
		return role;
	}

	public static TileMarkerRoleContext fromRole(BaRole role)
	{
		if (role != null)
		{
			for (TileMarkerRoleContext context : values())
			{
				if (context.role == role)
				{
					return context;
				}
			}
		}

		return GLOBAL;
	}

	public static TileMarkerRoleContext fromName(String name)
	{
		if (name != null)
		{
			for (TileMarkerRoleContext context : values())
			{
				if (context.name().equals(name))
				{
					return context;
				}
			}
		}

		return DEFENDER;
	}
}
