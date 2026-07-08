package com.bahealerorder.common;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class BaPartyChatMessage extends PartyMemberMessage
{
	int world;
	String message;
}
