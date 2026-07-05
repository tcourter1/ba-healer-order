package com.bahealerorder.common;

import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;

public final class BaNpcIds
{
	private BaNpcIds()
	{
	}

	public static BaOverviewNpcType getOverviewType(NPC npc)
	{
		return npc == null ? null : getOverviewType(npc.getId());
	}

	public static BaOverviewNpcType getOverviewType(int npcId)
	{
		switch (npcId)
		{
			case NpcID.BARBASSAULT_PEN_RANGER_LV1:
			case NpcID.BARBASSAULT_PEN_RANGER_LV2:
			case NpcID.BARBASSAULT_PEN_RANGER_LV3:
			case NpcID.BARBASSAULT_PEN_RANGER_LV4:
			case NpcID.BARBASSAULT_PEN_RANGER_LV5:
			case NpcID.BARBASSAULT_PEN_RANGER_LV6:
			case NpcID.BARBASSAULT_PEN_RANGER_LV7:
			case NpcID.BARBASSAULT_PEN_RANGER_LV8:
			case NpcID.BARBASSAULT_PEN_RANGER_LV9:
				return BaOverviewNpcType.RANGER;
			case NpcID.BARBASSAULT_PEN_FIGHTER_LV1:
			case NpcID.BARBASSAULT_PEN_FIGHTER_LV2:
			case NpcID.BARBASSAULT_PEN_FIGHTER_LV3:
			case NpcID.BARBASSAULT_PEN_FIGHTER_LV4:
			case NpcID.BARBASSAULT_PEN_FIGHTER_LV5:
			case NpcID.BARBASSAULT_PEN_FIGHTER_LV6:
			case NpcID.BARBASSAULT_PEN_FIGHTER_LV7:
			case NpcID.BARBASSAULT_PEN_FIGHTER_LV8:
			case NpcID.BARBASSAULT_PEN_FIGHTER_LV9:
				return BaOverviewNpcType.FIGHTER;
			case NpcID.BARBASSAULT_PEN_RUNNER_LV1:
			case NpcID.BARBASSAULT_PEN_RUNNER_LV2:
			case NpcID.BARBASSAULT_PEN_RUNNER_LV3:
			case NpcID.BARBASSAULT_PEN_RUNNER_LV4:
			case NpcID.BARBASSAULT_PEN_RUNNER_LV5:
			case NpcID.BARBASSAULT_PEN_RUNNER_LV6:
			case NpcID.BARBASSAULT_PEN_RUNNER_LV7:
			case NpcID.BARBASSAULT_PEN_RUNNER_LV8:
			case NpcID.BARBASSAULT_PEN_RUNNER_LV9:
				return BaOverviewNpcType.RUNNER;
			case NpcID.BARBASSAULT_PEN_HEALER_LV1:
			case NpcID.BARBASSAULT_PEN_HEALER_LV2:
			case NpcID.BARBASSAULT_PEN_HEALER_LV3:
			case NpcID.BARBASSAULT_PEN_HEALER_LV4:
			case NpcID.BARBASSAULT_PEN_HEALER_LV5:
			case NpcID.BARBASSAULT_PEN_HEALER_LV6:
			case NpcID.BARBASSAULT_PEN_HEALER_LV7:
			case NpcID.BARBASSAULT_PEN_HEALER_LV8:
			case NpcID.BARBASSAULT_PEN_HEALER_LV9:
				return BaOverviewNpcType.HEALER;
			default:
				return null;
		}
	}

	public static boolean isPenanceHealer(NPC npc)
	{
		return getOverviewType(npc) == BaOverviewNpcType.HEALER;
	}

	public static boolean isPenanceNpc(NPC npc)
	{
		return npc != null && isPenanceNpc(npc.getId());
	}

	private static boolean isPenanceNpc(int npcId)
	{
		return getOverviewType(npcId) != null
				|| npcId == NpcID.BARBASSAULT_PEN_QUEEN_NEW;
	}
}
