package com.bahealerorder.healer.ttk;

import java.util.OptionalInt;

final class HealerHealthEstimator
{
	private HealerHealthEstimator()
	{
	}

	static OptionalInt estimate(int healthRatio, int healthScale, int maxHp)
	{
		if (maxHp <= 0 || healthRatio < 0 || healthScale <= 0)
		{
			return OptionalInt.empty();
		}

		if (healthRatio <= 0)
		{
			return OptionalInt.of(0);
		}

		int minHp = 1;
		int maxEstimatedHp;

		if (healthScale > 1)
		{
			if (healthRatio > 1)
			{
				minHp = (maxHp * (healthRatio - 1) + healthScale - 2) / (healthScale - 1);
			}

			maxEstimatedHp = Math.min((maxHp * healthRatio - 1) / (healthScale - 1), maxHp);
		}
		else
		{
			maxEstimatedHp = maxHp;
		}

		return OptionalInt.of((minHp + maxEstimatedHp + 1) / 2);
	}
}
