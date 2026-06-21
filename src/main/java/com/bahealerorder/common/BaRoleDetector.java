package com.bahealerorder.common;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;

@Singleton
public class BaRoleDetector
{
	private final Client client;
	private BaRole currentRole;

	@Inject
	private BaRoleDetector(Client client)
	{
		this.client = client;
	}

	public BaRole getCurrentRole()
	{
		return currentRole;
	}

	public boolean isRole(BaRole role)
	{
		return currentRole == role;
	}

	public boolean isRoleInterfaceLoaded()
	{
		for (BaRole role : BaRole.values())
		{
			if (client.getWidget(role.getInterfaceGroupId(), 0) != null)
			{
				return true;
			}
		}

		return false;
	}

	public void onGameTick(GameTick event)
	{
		if (currentRole == null)
		{
			detectRoleFromLoadedWidgets();
		}
	}

	public void onWidgetLoaded(WidgetLoaded event)
	{
		setRole(BaRole.fromGroupId(event.getGroupId()));
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();

		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			reset();
		}
	}

	public void reset()
	{
		currentRole = null;
	}

	private void detectRoleFromLoadedWidgets()
	{
		for (BaRole role : BaRole.values())
		{
			if (client.getWidget(role.getInterfaceGroupId(), 0) != null)
			{
				setRole(role);
				return;
			}
		}
	}

	private void setRole(BaRole role)
	{
		if (role != null)
		{
			currentRole = role;
		}
	}
}
