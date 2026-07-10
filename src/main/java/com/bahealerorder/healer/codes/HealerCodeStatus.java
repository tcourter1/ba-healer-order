package com.bahealerorder.healer.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class HealerCodeStatus
{
	private final HealerInstruction instruction;
	private final CodeDisplayState state;
	private final int foodFed;
	private final int lastFoodElapsedSeconds;
}
