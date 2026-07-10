package com.bahealerorder.common;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class BaPartySyncMemberStatus
{
	private final String name;
	private final String role;
	private final boolean inParty;
	private final BaHealerFoodCounts healerFoodCounts;
}
