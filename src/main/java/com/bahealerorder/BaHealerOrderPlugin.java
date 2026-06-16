package com.bahealerorder;

import com.bahealerorder.attacker.AttackerController;
import com.bahealerorder.common.BaDispenserMenuService;
import com.bahealerorder.common.BaHealerSyncMessage;
import com.bahealerorder.common.BaPartySyncService;
import com.bahealerorder.common.BaRoleDetector;
import com.bahealerorder.healer.HealerController;
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
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
		name = "BA Utilities",
		description = "Adds Barbarian Assault helper overlays, menus, and panels.",
		tags = {"barbarian assault", "ba", "healer", "attacker", "defender", "collector", "penance", "overlay"}
)
public class BaHealerOrderPlugin extends Plugin
{

	@Inject
	private AttackerController attackerController;

	@Inject
	private HealerController healerController;

	@Inject
	private BaRoleDetector roleDetector;

	@Inject
	private BaDispenserMenuService dispenserMenuService;

	@Inject
	private BaPartySyncService partySyncService;

	@Override
	protected void startUp()
	{
		partySyncService.startUp();
		attackerController.startUp();
		healerController.startUp();
	}

	@Override
	protected void shutDown()
	{
		healerController.shutDown();
		attackerController.shutDown();
		partySyncService.shutDown();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		attackerController.onNpcSpawned(event);
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
		partySyncService.onMenuOptionClicked(event);
		attackerController.onMenuOptionClicked(event);
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
		partySyncService.onGameTick(event);
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
		partySyncService.onChatMessage(event);
		attackerController.onChatMessage(event);
		healerController.onChatMessage(event);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		roleDetector.onVarbitChanged(event);
		partySyncService.onVarbitChanged(event);
		attackerController.onVarbitChanged(event);
		healerController.onVarbitChanged(event);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		roleDetector.onGameStateChanged(event);
		partySyncService.onGameStateChanged(event);
		attackerController.onGameStateChanged(event);
		healerController.onGameStateChanged(event);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		partySyncService.onConfigChanged(event);
		healerController.onConfigChanged(event);
	}

	@Subscribe
	public void onBaHealerSyncMessage(BaHealerSyncMessage event)
	{
		healerController.onBaHealerSyncMessage(event);
	}

	@Subscribe
	public void onUserJoin(UserJoin event)
	{
		healerController.onPartyUserJoin(event);
	}

	@Provides
	BaUtilitiesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BaUtilitiesConfig.class);
	}
}
