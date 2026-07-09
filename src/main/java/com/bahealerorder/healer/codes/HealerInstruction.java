package com.bahealerorder.healer.codes;

import java.util.Objects;

public class HealerInstruction
{
	private int targetFoodCount;
	private int postRestockFoodCount;
	private Integer afterSeconds;
	private Integer beforeSeconds;
	private Integer exactSeconds;
	private boolean advanced;
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

	public int getPostRestockFoodCount()
	{
		return postRestockFoodCount;
	}

	public void setPostRestockFoodCount(int postRestockFoodCount)
	{
		this.postRestockFoodCount = Math.max(0, postRestockFoodCount);
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

	public boolean isAdvanced()
	{
		return advanced;
	}

	public void setAdvanced(boolean advanced)
	{
		this.advanced = advanced;
	}

	public boolean hasTarget()
	{
		return targetFoodCount > 0;
	}

	public boolean hasPostRestockFoodCount()
	{
		return postRestockFoodCount > 0;
	}

	public int getTotalTargetFoodCount()
	{
		return targetFoodCount + postRestockFoodCount;
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
		if (advanced && raw != null && !raw.trim().isEmpty())
		{
			return raw.trim();
		}

		if (!hasTarget())
		{
			return "";
		}

		StringBuilder builder = new StringBuilder();
		builder.append(targetFoodCount);
		if (postRestockFoodCount > 0)
		{
			builder.append(',').append(postRestockFoodCount);
		}

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
		HealerInstruction copy = new HealerInstruction(targetFoodCount, afterSeconds, beforeSeconds, exactSeconds, raw);
		copy.setPostRestockFoodCount(postRestockFoodCount);
		copy.setAdvanced(advanced);
		return copy;
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
				&& postRestockFoodCount == that.postRestockFoodCount
				&& advanced == that.advanced
				&& Objects.equals(afterSeconds, that.afterSeconds)
				&& Objects.equals(beforeSeconds, that.beforeSeconds)
				&& Objects.equals(exactSeconds, that.exactSeconds)
				&& Objects.equals(raw, that.raw);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(targetFoodCount, postRestockFoodCount, afterSeconds, beforeSeconds, exactSeconds, advanced, raw);
	}
}
