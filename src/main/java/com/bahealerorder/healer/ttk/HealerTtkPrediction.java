package com.bahealerorder.healer.ttk;

import java.util.Objects;

public final class HealerTtkPrediction
{
	private static final int UNKNOWN_TICK = -1;
	private static final HealerTtkPrediction EMPTY = new HealerTtkPrediction(UNKNOWN_TICK, false, UNKNOWN_TICK, false);

	private final int deathTick;
	private final boolean unknown;
	private final int observedTick;
	private final boolean publishable;

	private HealerTtkPrediction(int deathTick, boolean unknown, int observedTick, boolean publishable)
	{
		this.deathTick = deathTick;
		this.unknown = unknown;
		this.observedTick = observedTick;
		this.publishable = publishable;
	}

	public static HealerTtkPrediction empty()
	{
		return EMPTY;
	}

	static HealerTtkPrediction known(int deathTick, int observedTick, boolean publishable)
	{
		return new HealerTtkPrediction(deathTick, false, observedTick, publishable);
	}

	static HealerTtkPrediction unknown(int observedTick, boolean publishable)
	{
		return new HealerTtkPrediction(UNKNOWN_TICK, true, observedTick, publishable);
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

	public int getObservedTick()
	{
		return observedTick;
	}

	public boolean isPublishable()
	{
		return publishable;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other) return true;
		if (!(other instanceof HealerTtkPrediction)) return false;

		HealerTtkPrediction that = (HealerTtkPrediction) other;
		return deathTick == that.deathTick
				&& unknown == that.unknown
				&& observedTick == that.observedTick
				&& publishable == that.publishable;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(deathTick, unknown, observedTick, publishable);
	}
}
