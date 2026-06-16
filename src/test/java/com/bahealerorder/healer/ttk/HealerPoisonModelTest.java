package com.bahealerorder.healer.ttk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.OptionalInt;
import org.junit.Test;

public class HealerPoisonModelTest
{
	private final HealerPoisonModel model = new HealerPoisonModel();

	@Test
	public void poisonCannotKillMoreThanFiftyHp()
	{
		OptionalInt deathTick = model.calculateDeathTick(51, 100, 100, 100);

		assertFalse(deathTick.isPresent());
	}

	@Test
	public void poisonKillsFiveHpOnSecondPoisonHit()
	{
		OptionalInt deathTick = model.calculateDeathTick(5, 100, 100, 100);

		assertEquals(110, deathTick.getAsInt());
	}

	@Test
	public void poisonKillsFiftyHpOnLastPoisonHit()
	{
		OptionalInt deathTick = model.calculateDeathTick(50, 0, 0, 0);

		assertEquals(100, deathTick.getAsInt());
	}

	@Test
	public void laterFoodRefreshesDamageWithoutResettingCadence()
	{
		OptionalInt deathTick = model.calculateDeathTick(4, 100, 119, 119);

		assertEquals(120, deathTick.getAsInt());
	}

	@Test
	public void elapsedCurrentHpStartsFromNextFutureCadenceTick()
	{
		OptionalInt deathTick = model.calculateDeathTick(4, 100, 100, 106);

		assertEquals(110, deathTick.getAsInt());
	}
}
