package com.bahealerorder.healer.ttk;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Getter
public final class HealerTtkPrediction
{
	private static final int UNKNOWN_TICK = -1;
	private static final HealerTtkPrediction EMPTY = new HealerTtkPrediction(UNKNOWN_TICK, false);

	private final int deathTick;
	private final boolean unknown;

	public static HealerTtkPrediction empty()
	{
		return EMPTY;
	}

	static HealerTtkPrediction known(int deathTick)
	{
		return new HealerTtkPrediction(deathTick, false);
	}

	static HealerTtkPrediction unknown()
	{
		return new HealerTtkPrediction(UNKNOWN_TICK, true);
	}

	public boolean hasValue()
	{
		return deathTick >= 0 || unknown;
	}

	public boolean hasDeathTick()
	{
		return deathTick >= 0;
	}

}
