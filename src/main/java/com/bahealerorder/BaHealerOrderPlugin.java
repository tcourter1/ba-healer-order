package com.bahealerorder;

import com.bahealerorder.attacker.AttackerController;
import com.bahealerorder.common.BaDispenserMenuService;
import com.bahealerorder.common.BaHealerSyncMessage;
import com.bahealerorder.common.BaPartySyncService;
import com.bahealerorder.common.BaRoleDetector;
import com.bahealerorder.common.BaWaveLifecycleService;
import com.bahealerorder.common.BaWaveLifecycleService.WaveStart;
import com.bahealerorder.common.BaWaveOverviewService;
import com.bahealerorder.common.BaWaveOverviewSyncMessage;
import com.bahealerorder.defender.DefenderController;
import com.bahealerorder.healer.HealerController;
import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.GameState;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
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
	private DefenderController defenderController;

	@Inject
	private BaRoleDetector roleDetector;

	@Inject
	private BaDispenserMenuService dispenserMenuService;

	@Inject
	private BaPartySyncService partySyncService;

	@Inject
	private BaWaveOverviewService waveOverviewService;

	@Inject
	private BaWaveLifecycleService waveLifecycleService;

	@Override
	protected void startUp()
	{
		partySyncService.startUp();
		attackerController.startUp();
		defenderController.startUp();
		healerController.startUp();
	}

	@Override
	protected void shutDown()
	{
		healerController.shutDown();
		defenderController.shutDown();
		attackerController.shutDown();
		partySyncService.shutDown();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		attackerController.onNpcSpawned(event);
		defenderController.onNpcSpawned(event);
		healerController.onNpcSpawned(event);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		attackerController.onNpcDespawned(event);
		defenderController.onNpcDespawned(event);
		healerController.onNpcDespawned(event);
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		healerController.onHitsplatApplied(event);
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		attackerController.onActorDeath(event);
		defenderController.onActorDeath(event);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		defenderController.onAnimationChanged(event);
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
		WaveStart recoveredWave = waveLifecycleService.recoverIfNeeded();
		if (recoveredWave != null)
		{
			startWave(recoveredWave);
		}

		Integer endedWave = waveLifecycleService.onGameTick();
		if (endedWave != null)
		{
			endWave(endedWave);
		}

		roleDetector.onGameTick(event);
		partySyncService.onGameTick(event);
		attackerController.onGameTick(event);
		defenderController.onGameTick(event);
		healerController.onGameTick(event);
		waveOverviewService.onGameTick();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BARBASSAULT_WAVECOMPLETE)
		{
			Integer endedWave = waveLifecycleService.endWave();
			if (endedWave != null)
			{
				endWave(endedWave);
			}
		}

		roleDetector.onWidgetLoaded(event);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		WaveStart waveStart = waveLifecycleService.onChatMessage(event);
		if (waveStart != null)
		{
			startWave(waveStart);
		}

		waveOverviewService.onChatMessage(event);
		healerController.onChatMessage(event);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN
				|| event.getGameState() == GameState.HOPPING)
		{
			waveLifecycleService.reset();
		}

		roleDetector.onGameStateChanged(event);
		waveOverviewService.onGameStateChanged(event);
		partySyncService.onGameStateChanged(event);
		attackerController.onGameStateChanged(event);
		defenderController.onGameStateChanged(event);
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
	public void onBaWaveOverviewSyncMessage(BaWaveOverviewSyncMessage event)
	{
		waveOverviewService.onBaWaveOverviewSyncMessage(event);
	}

	@Subscribe
	public void onUserJoin(UserJoin event)
	{
		waveOverviewService.onPartyUserJoin(event);
		healerController.onPartyUserJoin(event);
	}

	@Provides
	BaUtilitiesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BaUtilitiesConfig.class);
	}

	private void startWave(WaveStart waveStart)
	{
		waveOverviewService.onWaveStarted(waveStart.getWave());
		attackerController.onWaveStarted(waveStart.getWave());
		healerController.onWaveStarted(waveStart);
	}

	private void endWave(int wave)
	{
		roleDetector.reset();
		waveOverviewService.onWaveEnded();
		partySyncService.onWaveEnded(wave);
		attackerController.onWaveEnded();
		defenderController.onWaveEnded();
		healerController.onWaveEnded();
	}
}
