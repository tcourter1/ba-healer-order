package com.bahealerorder.defender;

import net.runelite.api.Tile;
import net.runelite.api.TileItem;

class DefenderGroundItem
{
	enum Type
	{
		HAMMER,
		LOGS
	}

	private final Tile tile;
	private final TileItem item;
	private final Type type;

	DefenderGroundItem(Tile tile, TileItem item, Type type)
	{
		this.tile = tile;
		this.item = item;
		this.type = type;
	}

	Tile getTile()
	{
		return tile;
	}

	TileItem getItem()
	{
		return item;
	}

	Type getType()
	{
		return type;
	}
}
