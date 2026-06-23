package com.bahealerorder.common;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class BaHealerSyncMessage extends PartyMemberMessage
{
	int world;
	int wave;
	int npcIndex;
	int healerOrder;
	int spawnTick;
	int currentCallIndex;
	int predictedDeathTick;
	boolean unknownTtk;
	int actualDeathTick;
	boolean observedDeath;
	boolean healthRatioMode;
	int[] foodTicks;
}
