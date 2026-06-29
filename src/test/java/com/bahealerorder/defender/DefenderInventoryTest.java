package com.bahealerorder.defender;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class DefenderInventoryTest
{
	@Test
	public void detectsRegularAndImcandoHammer()
	{
		assertTrue(DefenderInventory.isHammer(ItemID.HAMMER, "Hammer"));
		assertTrue(DefenderInventory.isHammer(12345, "Imcando hammer"));
		assertTrue(DefenderInventory.isImcandoHammer(ItemID.IMCANDO_HAMMER, null));
		assertTrue(DefenderInventory.isImcandoHammer(ItemID.IMCANDO_HAMMER_OFFHAND, null));
		assertFalse(DefenderInventory.isHammer(ItemID.LOGS, "Logs"));
	}
}
