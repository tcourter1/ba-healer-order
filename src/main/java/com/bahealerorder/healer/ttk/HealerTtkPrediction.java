package com.bahealerorder.healer.ttk;

import java.util.Objects;

public final class HealerTtkPrediction
{
	private static final int UNKNOWN_TICK = -1;
	private static final HealerTtkPrediction EMPTY = new HealerTtkPrediction(UNKNOWN_TICK, false);

	private final int deathTick;
	private final boolean unknown;

	private HealerTtkPrediction(int deathTick, boolean unknown)
	{
		this.deathTick = deathTick;
		this.unknown = unknown;
	}

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

	public int getDeathTick()
	{
		return deathTick;
	}

	public boolean isUnknown()
	{
		return unknown;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other) return true;
		if (!(other instanceof HealerTtkPrediction)) return false;

		HealerTtkPrediction that = (HealerTtkPrediction) other;
		return deathTick == that.deathTick
				&& unknown == that.unknown;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(deathTick, unknown);
	}
}
