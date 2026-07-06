package com.bahealerorder.healer.codes;

import java.util.Objects;

public class HealerInstruction
{
	private int targetFoodCount;
	private Integer afterSeconds;
	private Integer beforeSeconds;
	private Integer exactSeconds;
	private String raw;

	public HealerInstruction()
	{
	}

	public HealerInstruction(int targetFoodCount, Integer afterSeconds, Integer beforeSeconds, String raw)
	{
		this(targetFoodCount, afterSeconds, beforeSeconds, null, raw);
	}

	public HealerInstruction(int targetFoodCount, Integer afterSeconds, Integer beforeSeconds, Integer exactSeconds, String raw)
	{
		this.targetFoodCount = Math.max(0, targetFoodCount);
		this.afterSeconds = afterSeconds;
		this.beforeSeconds = beforeSeconds;
		this.exactSeconds = exactSeconds;
		this.raw = raw;
	}

	public int getTargetFoodCount()
	{
		return targetFoodCount;
	}

	public void setTargetFoodCount(int targetFoodCount)
	{
		this.targetFoodCount = Math.max(0, targetFoodCount);
	}

	public Integer getAfterSeconds()
	{
		return afterSeconds;
	}

	public void setAfterSeconds(Integer afterSeconds)
	{
		this.afterSeconds = afterSeconds;
	}

	public Integer getBeforeSeconds()
	{
		return beforeSeconds;
	}

	public void setBeforeSeconds(Integer beforeSeconds)
	{
		this.beforeSeconds = beforeSeconds;
	}

	public Integer getExactSeconds()
	{
		return exactSeconds;
	}

	public void setExactSeconds(Integer exactSeconds)
	{
		this.exactSeconds = exactSeconds;
	}

	public String getRaw()
	{
		return raw;
	}

	public void setRaw(String raw)
	{
		this.raw = raw;
	}

	public boolean hasTarget()
	{
		return targetFoodCount > 0;
	}

	public boolean hasTiming()
	{
		return afterSeconds != null || beforeSeconds != null || exactSeconds != null;
	}

	public HealerTimingMode getTimingMode()
	{
		if (exactSeconds != null)
		{
			return HealerTimingMode.EXACT;
		}

		if (afterSeconds != null)
		{
			return HealerTimingMode.AT_OR_AFTER;
		}

		if (beforeSeconds != null)
		{
			return HealerTimingMode.BEFORE;
		}

		return HealerTimingMode.NONE;
	}

	public Integer getTimingSeconds()
	{
		if (exactSeconds != null)
		{
			return exactSeconds;
		}

		if (afterSeconds != null)
		{
			return afterSeconds;
		}

		return beforeSeconds;
	}

	public String formatTarget()
	{
		if (!hasTarget())
		{
			return "";
		}

		StringBuilder builder = new StringBuilder();
		builder.append(targetFoodCount);

		if (afterSeconds != null)
		{
			builder.append('(').append(afterSeconds).append(')');
		}

		if (beforeSeconds != null)
		{
			builder.append('[').append(beforeSeconds).append(']');
		}

		if (exactSeconds != null)
		{
			builder.append('{').append(exactSeconds).append('}');
		}

		return builder.toString();
	}

	public HealerInstruction copy()
	{
		return new HealerInstruction(targetFoodCount, afterSeconds, beforeSeconds, exactSeconds, raw);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof HealerInstruction))
		{
			return false;
		}
		HealerInstruction that = (HealerInstruction) o;
		return targetFoodCount == that.targetFoodCount
				&& Objects.equals(afterSeconds, that.afterSeconds)
				&& Objects.equals(beforeSeconds, that.beforeSeconds)
				&& Objects.equals(exactSeconds, that.exactSeconds)
				&& Objects.equals(raw, that.raw);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(targetFoodCount, afterSeconds, beforeSeconds, exactSeconds, raw);
	}
}
