package com.bahealerorder.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;

@RequiredArgsConstructor
public enum BaRole
{
	ATTACKER(485, InterfaceID.BARBASSAULT_OVER_ATT, "Attacker", "Attacker item machine", ItemID.BARBASSAULT_PLAYERICON_ATTACKER),
	COLLECTOR(486, InterfaceID.BARBASSAULT_OVER_COL, "Collector", "Collector Converter", ItemID.BARBASSAULT_PLAYERICON_COLLECTOR),
	DEFENDER(487, InterfaceID.BARBASSAULT_OVER_DEF, "Defender", "Defender item machine", ItemID.BARBASSAULT_PLAYERICON_DEFENDER),
	HEALER(488, InterfaceID.BARBASSAULT_OVER_HEAL, "Healer", "Healer item machine", ItemID.BARBASSAULT_PLAYERICON_HEALER);

	private final int groupId;
	@Getter(AccessLevel.PACKAGE)
	private final int interfaceGroupId;
	@Getter
	private final String displayName;
	@Getter
	private final String dispenserName;
	@Getter
	private final int playerIconItemId;

	public static BaRole fromDisplayName(String displayName)
	{
		if (displayName == null) return null;

		for (BaRole role : values())
		{
			if (role.displayName.equalsIgnoreCase(displayName)) return role;
		}

		return null;
	}

	static BaRole fromGroupId(int groupId)
	{
		for (BaRole role : values())
		{
			if (groupId == role.groupId || groupId == role.interfaceGroupId) return role;
		}

		return null;
	}
}
