package com.bahealerorder.common;

import net.runelite.api.gameval.InterfaceID;

public enum BaRole
{
	ATTACKER(485, InterfaceID.BARBASSAULT_OVER_ATT, "Attacker item machine"),
	COLLECTOR(486, InterfaceID.BARBASSAULT_OVER_COL, "Collector Converter"),
	DEFENDER(487, InterfaceID.BARBASSAULT_OVER_DEF, "Defender item machine"),
	HEALER(488, InterfaceID.BARBASSAULT_OVER_HEAL, "Healer item machine");

	private final int groupId;
	private final int interfaceGroupId;
	private final String dispenserName;

	BaRole(int groupId, int interfaceGroupId, String dispenserName)
	{
		this.groupId = groupId;
		this.interfaceGroupId = interfaceGroupId;
		this.dispenserName = dispenserName;
	}

	public String getDispenserName()
	{
		return dispenserName;
	}

	int getInterfaceGroupId()
	{
		return interfaceGroupId;
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
