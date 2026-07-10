package com.bahealerorder.defender;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter(AccessLevel.PACKAGE)
class DefenderGroundItem
{
	enum Type
	{
		HAMMER
	}

	private final Tile tile;
	private final TileItem item;
	private final Type type;
}
