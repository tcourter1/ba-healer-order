package com.bahealerorder.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BaPartySyncMemberStatus
{
	private final String name;
	private final String role;
	private final boolean inParty;
	private final BaHealerFoodCounts healerFoodCounts;
}
