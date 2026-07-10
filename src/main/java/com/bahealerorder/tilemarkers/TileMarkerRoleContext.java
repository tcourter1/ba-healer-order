package com.bahealerorder.tilemarkers;

import com.bahealerorder.common.BaRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TileMarkerRoleContext
{
	DEFENDER("Defender", BaRole.DEFENDER),
	COLLECTOR("Collector", BaRole.COLLECTOR),
	HEALER("Healer", BaRole.HEALER),
	ATTACKER("Attacker", BaRole.ATTACKER),
	GLOBAL("Global", null);

	private final String displayName;
	private final BaRole role;

	@Override
	public String toString()
	{
		return displayName;
	}

	public static TileMarkerRoleContext fromRole(BaRole role)
	{
		if (role != null)
		{
			for (TileMarkerRoleContext context : values())
			{
				if (context.role == role) return context;
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
				if (context.name().equals(name)) return context;
			}
		}

		return DEFENDER;
	}
}
