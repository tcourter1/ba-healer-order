package com.bahealerorder.common;

import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@EqualsAndHashCode(callSuper = true)
public class BaHealerFoodCountMessage extends PartyMemberMessage
{
	String playerName;
	int world;
	int tofu;
	int worms;
	int meat;
	int calledFood;
}
