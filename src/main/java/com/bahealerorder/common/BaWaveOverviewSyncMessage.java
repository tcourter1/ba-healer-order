package com.bahealerorder.common;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class BaWaveOverviewSyncMessage extends PartyMemberMessage
{
	int world;
	int wave;
	int[] npcTypes;
	int[] npcIndexes;
	int[] npcOrders;
	int[] deadNpcTypes;
	int[] deadNpcOrders;
	int[] deadNpcDeathTicks;
}
