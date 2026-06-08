package com.bahealerorder.healer.ttk;

import java.util.OptionalInt;

public class HealerPoisonModel
{
	static final int TICKS_PER_POISON_HIT = 5;
	static final int HITS_PER_POISON_DAMAGE = 5;
	private static final int INITIAL_POISON_DAMAGE = 4;
	private static final int TOTAL_POISON_HITS = INITIAL_POISON_DAMAGE * HITS_PER_POISON_DAMAGE;

	public OptionalInt calculateDeathTick(int currentHp, int firstPoisonTick, int lastFoodTick, int currentTick)
	{
		if (currentHp <= 0)
		{
			return OptionalInt.of(currentTick);
		}

		if (firstPoisonTick < 0 || lastFoodTick < 0)
		{
			return OptionalInt.empty();
		}

		int hp = currentHp;

		for (int poisonTick = getNextPoisonTick(firstPoisonTick, currentTick); ; poisonTick += TICKS_PER_POISON_HIT)
		{
			int damage = getDamageAtPoisonTick(lastFoodTick, poisonTick);

			if (damage <= 0)
			{
				break;
			}

			hp -= damage;

			if (hp <= 0)
			{
				return OptionalInt.of(poisonTick);
			}
		}

		return OptionalInt.empty();
	}

	static int getDamageForHit(int poisonHitNumber)
	{
		if (poisonHitNumber < 1 || poisonHitNumber > TOTAL_POISON_HITS)
		{
			return 0;
		}

		return INITIAL_POISON_DAMAGE - (poisonHitNumber - 1) / HITS_PER_POISON_DAMAGE;
	}

	static int getTickForHit(int lastFoodTick, int poisonHitNumber)
	{
		return lastFoodTick + poisonHitNumber * TICKS_PER_POISON_HIT;
	}

	private static int getDamageAtPoisonTick(int lastFoodTick, int poisonTick)
	{
		int elapsedTicks = poisonTick - lastFoodTick;

		if (elapsedTicks <= 0)
		{
			return 0;
		}

		int poisonHitNumber = (elapsedTicks + TICKS_PER_POISON_HIT - 1) / TICKS_PER_POISON_HIT;
		return getDamageForHit(poisonHitNumber);
	}

	private static int getNextPoisonTick(int firstPoisonTick, int afterTick)
	{
		if (afterTick < firstPoisonTick)
		{
			return firstPoisonTick;
		}

		int ticksSinceStart = afterTick - firstPoisonTick;
		return firstPoisonTick + (ticksSinceStart / TICKS_PER_POISON_HIT + 1) * TICKS_PER_POISON_HIT;
	}
}
