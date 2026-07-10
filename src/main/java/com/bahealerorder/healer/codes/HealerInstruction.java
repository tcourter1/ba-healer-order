package com.bahealerorder.healer.codes;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Setter
public class HealerInstruction
{
	private int targetFoodCount;
	private int postRestockFoodCount;
	private Integer afterSeconds;
	private Integer beforeSeconds;
	private Integer exactSeconds;
	private boolean advanced;
	private String raw;

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

	public void setTargetFoodCount(int targetFoodCount)
	{
		this.targetFoodCount = Math.max(0, targetFoodCount);
	}

	public void setPostRestockFoodCount(int postRestockFoodCount)
	{
		this.postRestockFoodCount = Math.max(0, postRestockFoodCount);
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

	public Integer getTimingSeconds()
	{
		if (exactSeconds != null) return exactSeconds;

		if (afterSeconds != null) return afterSeconds;

		return beforeSeconds;
	}

	public String formatTarget()
	{
		if (advanced && raw != null && !raw.trim().isEmpty()) return raw.trim();

		if (!hasTarget()) return "";

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

}
