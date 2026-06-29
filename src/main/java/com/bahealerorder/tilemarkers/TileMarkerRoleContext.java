package com.bahealerorder.tilemarkers;

import com.bahealerorder.common.BaRole;

public enum TileMarkerRoleContext
{
	GLOBAL("Global", null),
	ATTACKER("Attacker", BaRole.ATTACKER),
	HEALER("Healer", BaRole.HEALER),
	COLLECTOR("Collector", BaRole.COLLECTOR),
	DEFENDER("Defender", BaRole.DEFENDER);

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
