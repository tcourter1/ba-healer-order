package com.bahealerorder.defender.strategies;

public class DefenderTile
{
	private int regionId;
	private int regionX;
	private int regionY;
	private int z;

	public DefenderTile()
	{
	}

	public DefenderTile(int regionId, int regionX, int regionY, int z)
	{
		this.regionId = regionId;
		this.regionX = regionX;
		this.regionY = regionY;
		this.z = z;
	}

	public int getRegionId()
	{
		return regionId;
	}

	public void setRegionId(int regionId)
	{
		this.regionId = regionId;
	}

	public int getRegionX()
	{
		return regionX;
	}

	public void setRegionX(int regionX)
	{
		this.regionX = regionX;
	}

	public int getRegionY()
	{
		return regionY;
	}

	public void setRegionY(int regionY)
	{
		this.regionY = regionY;
	}

	public int getZ()
	{
		return z;
	}

	public void setZ(int z)
	{
		this.z = z;
	}
}
