package com.bahealerorder;

import com.bahealerorder.healer.HealerController;
import com.bahealerorder.common.BaDispenserMenuService;
import com.bahealerorder.common.BaRoleDetector;
import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
		name = "BA Healer Utilities",
		description = "Adds Barbarian Assault helper overlays, menus, and panels.",
		tags = {"barbarian assault", "ba", "healer", "attacker", "defender", "collector", "penance", "overlay"}
)
public class BaHealerOrderPlugin extends Plugin
{
	@Inject
	private HealerController healerController;

	@Inject
	private BaRoleDetector roleDetector;

	@Inject
	private BaDispenserMenuService dispenserMenuService;

	@Override
	protected void startUp()
	{
		healerController.startUp();
	}

	@Override
	protected void shutDown()
	{
		healerController.shutDown();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		healerController.onNpcSpawned(event);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		healerController.onNpcDespawned(event);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		healerController.onMenuOptionClicked(event);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		healerController.onMenuEntryAdded(event);
	}

	@Subscribe(priority = -2)
	public void onPostMenuSort(PostMenuSort event)
	{
		dispenserMenuService.apply();
		healerController.onPostMenuSort(event);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		healerController.onMenuOpened(event);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		healerController.onItemContainerChanged(event);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		roleDetector.onGameTick(event);
		healerController.onGameTick(event);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		roleDetector.onWidgetLoaded(event);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		healerController.onChatMessage(event);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		roleDetector.onVarbitChanged(event);
		healerController.onVarbitChanged(event);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		roleDetector.onGameStateChanged(event);
		healerController.onGameStateChanged(event);
	}

	@Provides
	BaUtilitiesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BaUtilitiesConfig.class);
	}
}
