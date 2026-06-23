package com.bahealerorder.common;

import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;

public enum BaRole
{
	ATTACKER(485, InterfaceID.BARBASSAULT_OVER_ATT, "Attacker", "Attacker item machine", ItemID.BARBASSAULT_PLAYERICON_ATTACKER),
	COLLECTOR(486, InterfaceID.BARBASSAULT_OVER_COL, "Collector", "Collector Converter", ItemID.BARBASSAULT_PLAYERICON_COLLECTOR),
	DEFENDER(487, InterfaceID.BARBASSAULT_OVER_DEF, "Defender", "Defender item machine", ItemID.BARBASSAULT_PLAYERICON_DEFENDER),
	HEALER(488, InterfaceID.BARBASSAULT_OVER_HEAL, "Healer", "Healer item machine", ItemID.BARBASSAULT_PLAYERICON_HEALER);

	private final int groupId;
	private final int interfaceGroupId;
	private final String displayName;
	private final String dispenserName;
	private final int playerIconItemId;

	BaRole(int groupId, int interfaceGroupId, String displayName, String dispenserName, int playerIconItemId)
	{
		this.groupId = groupId;
		this.interfaceGroupId = interfaceGroupId;
		this.displayName = displayName;
		this.dispenserName = dispenserName;
		this.playerIconItemId = playerIconItemId;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getDispenserName()
	{
		return dispenserName;
	}

	public int getPlayerIconItemId()
	{
		return playerIconItemId;
	}

	int getInterfaceGroupId()
	{
		return interfaceGroupId;
	}

	public static BaRole fromDisplayName(String displayName)
	{
		if (displayName == null) return null;

		for (BaRole role : values())
		{
			if (role.displayName.equalsIgnoreCase(displayName))
			{
				return role;
			}
		}

		return null;
	}

	static BaRole fromGroupId(int groupId)
	{
		for (BaRole role : values())
		{
			if (groupId == role.groupId || groupId == role.interfaceGroupId)
			{
				return role;
			}
		}

		return null;
	}
}
