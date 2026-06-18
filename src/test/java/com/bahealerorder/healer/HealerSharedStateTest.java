package com.bahealerorder.healer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HealerSharedStateTest
{
	@Test
	public void observedDeathOverwritesEarlierPresumedDeath()
	{
		HealerSharedState state = new HealerSharedState();

		assertTrue(state.recordPresumedDeath(1, 204));
		assertEquals(204, state.getActualDeathTick(1).intValue());
		assertFalse(state.isObservedDeath(1));

		assertTrue(state.recordDeath(1, 207));
		assertEquals(207, state.getActualDeathTick(1).intValue());
		assertTrue(state.isObservedDeath(1));

		assertFalse(state.recordDeath(1, 208));
		assertEquals(207, state.getActualDeathTick(1).intValue());
	}

	@Test
	public void visibleAliveHealerClearsPresumedDeath()
	{
		HealerSharedState state = new HealerSharedState();

		assertTrue(state.recordPresumedDeath(1, 204));
		assertTrue(state.clearPresumedDeath(1));
		assertNull(state.getActualDeathTick(1));

		assertTrue(state.recordDeath(1, 207));
		assertEquals(207, state.getActualDeathTick(1).intValue());
	}
}
