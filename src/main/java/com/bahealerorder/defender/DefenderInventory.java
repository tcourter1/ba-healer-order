package com.bahealerorder.defender;

import java.util.Locale;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Text;

final class DefenderInventory
{
	private static final String IMCANDO_HAMMER_NAME = "imcando hammer";
	private static final String LOGS_NAME = "logs";

	private DefenderInventory()
	{
	}

	static boolean isHammer(int itemId, String itemName)
	{
		if (itemId == ItemID.HAMMER)
		{
			return true;
		}

		return isImcandoHammer(itemId, itemName);
	}

	static boolean isImcandoHammer(int itemId, String itemName)
	{
		if (itemId == ItemID.IMCANDO_HAMMER || itemId == ItemID.IMCANDO_HAMMER_OFFHAND)
		{
			return true;
		}

		return itemName != null
				&& Text.removeTags(itemName).toLowerCase(Locale.ROOT).contains(IMCANDO_HAMMER_NAME);
	}

	static boolean isLogs(int itemId, String itemName)
	{
		if (itemId == ItemID.LOGS)
		{
			return true;
		}

		return itemName != null
				&& LOGS_NAME.equals(Text.removeTags(itemName).toLowerCase(Locale.ROOT).trim());
	}
}
