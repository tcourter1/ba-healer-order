package com.bahealerorder;

import com.bahealerorder.attacker.AttackerController;
import com.bahealerorder.common.BaDispenserMenuService;
import com.bahealerorder.common.BaHealerFoodCountMessage;
import com.bahealerorder.common.BaHealerSyncMessage;
import com.bahealerorder.common.BaPartyChatMessage;
import com.bahealerorder.common.BaPartySyncService;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.BaRoleDetector;
import com.bahealerorder.common.BaScrollerController;
import com.bahealerorder.common.BaWaveLifecycleService;
import com.bahealerorder.common.BaWaveLifecycleService.WaveStart;
import com.bahealerorder.common.BaWaveOverviewService;
import com.bahealerorder.common.BaWaveOverviewSyncMessage;
import com.bahealerorder.defender.DefenderController;
import com.bahealerorder.healer.HealerCodeDefaultResolver;
import com.bahealerorder.healer.HealerController;
import com.bahealerorder.healer.HealerCodeManager;
import com.bahealerorder.healer.codes.HealerCodeDefaultRole;
import com.bahealerorder.healer.codes.RunPreset;
import com.bahealerorder.sidepanel.BaUtilitiesPanel;
import com.bahealerorder.sidepanel.onboarding.BaAssistancePresetManager;
import com.bahealerorder.tilemarkers.GeneralTileMarkerController;
import com.google.inject.Provides;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
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
	private static final int BA_SURFACE_REGION = 10039;
	private static final int BA_LOBBY_REGION = 10322;
	private static final int BA_WAVES_1_TO_9_REGION = 7509;
	private static final int BA_WAVE_10_REGION = 7508;

	private boolean onboardingSidePanelOpened;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private AttackerController attackerController;

	@Inject
	private HealerController healerController;

	@Inject
	private HealerCodeManager healerCodeManager;

	@Inject
	private BaUtilitiesPanel panel;

	@Inject
	private BaAssistancePresetManager presetManager;

	@Inject
	private DefenderController defenderController;

	@Inject
	private GeneralTileMarkerController generalTileMarkerController;

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

	@Inject
	private BaScrollerController scrollerController;

	@Override
	protected void startUp()
	{
		partySyncService.startUp();
		scrollerController.startUp();
		attackerController.startUp();
		defenderController.startUp();
		generalTileMarkerController.startUp();
		healerController.startUp();
	}

	@Override
	protected void shutDown()
	{
		healerController.shutDown();
		generalTileMarkerController.shutDown();
		defenderController.shutDown();
		attackerController.shutDown();
		scrollerController.shutDown();
		partySyncService.shutDown();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		scrollerController.onNpcSpawned(event);
		attackerController.onNpcSpawned(event);
		defenderController.onNpcSpawned(event);
		healerController.onNpcSpawned(event);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		scrollerController.onNpcDespawned(event);
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
		defenderController.onActorDeath(event);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		defenderController.onAnimationChanged(event);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		scrollerController.onVarbitChanged(event);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		partySyncService.onMenuOptionClicked(event);
		scrollerController.onMenuOptionClicked(event);
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
	public void onItemSpawned(ItemSpawned event)
	{
		defenderController.onItemSpawned(event);
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		defenderController.onItemDespawned(event);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Integer endedWave = waveLifecycleService.onGameTick();
		if (endedWave != null)
		{
			endWave(endedWave);
		}

		roleDetector.onGameTick(event);
		partySyncService.onGameTick(event);
		scrollerController.onGameTick(event);
		defenderController.onGameTick(event);
		healerController.onGameTick(event);
		waveOverviewService.onGameTick();

		handleOnboardingAutoOpen();
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
	public void onCommandExecuted(CommandExecuted event)
	{
		String command = event.getCommand();

		if ("bawave".equalsIgnoreCase(command))
		{
			handleBaWaveCommand(event.getArguments());
			return;
		}

		if ("bautil".equalsIgnoreCase(command))
		{
			handleBaUtilCommand(event.getArguments());
			return;
		}

		if ("baend".equalsIgnoreCase(command))
		{
			Integer endedWave = waveLifecycleService.endWave();
			endWave(endedWave == null ? -1 : endedWave);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		partySyncService.onChatMessage(event);

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
			onboardingSidePanelOpened = false;
		}

		roleDetector.onGameStateChanged(event);
		waveOverviewService.onGameStateChanged(event);
		partySyncService.onGameStateChanged(event);
		scrollerController.onGameStateChanged(event);
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
		clientThread.invokeLater(() -> healerController.onBaHealerSyncMessage(event));
	}

	@Subscribe
	public void onBaHealerFoodCountMessage(BaHealerFoodCountMessage event)
	{
		clientThread.invokeLater(() -> partySyncService.onBaHealerFoodCountMessage(event));
	}

	@Subscribe
	public void onBaWaveOverviewSyncMessage(BaWaveOverviewSyncMessage event)
	{
		clientThread.invokeLater(() -> waveOverviewService.onBaWaveOverviewSyncMessage(event));
	}

	@Subscribe
	public void onBaPartyChatMessage(BaPartyChatMessage event)
	{
		partySyncService.onBaPartyChatMessage(event);
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
		applyDefaultHealerCodePreset(waveStart);
		waveOverviewService.onWaveStarted(waveStart.getWave());
		attackerController.onWaveStarted(waveStart.getWave());
		defenderController.onWaveStarted();
		healerController.onWaveStarted(waveStart);
	}



	private void applyDefaultHealerCodePreset(WaveStart waveStart)
	{
		if (waveStart.getWave() != 1 || !"wave chat".equals(waveStart.getSource())) return;

		HealerCodeDefaultRole defaultRole = HealerCodeDefaultResolver.resolve(
				roleDetector.getCurrentRole(),
				client.getLocalPlayer() == null ? null : client.getLocalPlayer().getName(),
				partySyncService.getBaPartySyncTeamMembers()
		);

		if (defaultRole != null)
		{
			RunPreset preset = healerCodeManager.applyDefaultRunPreset(defaultRole);
			if (preset != null)
			{
				panel.refreshLater();
				addChatMessage("BA Utilities swapping to "
						+ HealerCodeManager.runPresetDisplayName(preset)
						+ " because it is marked as the default for "
						+ defaultRole.getDisplayName()
						+ ".");
			}
		}
	}

	private void endWave(int wave)
	{
		roleDetector.reset();
		waveOverviewService.onWaveEnded();
		partySyncService.onWaveEnded(wave);
		attackerController.onWaveEnded();
		defenderController.onWaveEnded();
		scrollerController.onWaveEnded();
		healerController.onWaveEnded();
	}

	private void handleBaWaveCommand(String[] arguments)
	{
		if (arguments.length < 1)
		{
			addBaWaveUsage();
			return;
		}

		int wave;
		try
		{
			wave = Integer.parseInt(arguments[0]);
		}
		catch (NumberFormatException ex)
		{
			addBaWaveUsage();
			return;
		}

		if (wave == 0)
		{
			Integer endedWave = waveLifecycleService.endWave();
			endWave(endedWave == null ? -1 : endedWave);
			addChatMessage("BA Utilities dev wave cleared.");
			return;
		}

		BaRole role = null;
		boolean scroller = false;
		boolean queenSpawned = false;
		boolean omegaLoaded = false;

		for (int i = 1; i < arguments.length; i++)
		{
			String option = arguments[i] == null ? "" : arguments[i].trim().toLowerCase(Locale.ROOT);
			if (option.isEmpty()) continue;

			if ("queen".equals(option))
			{
				queenSpawned = true;
				continue;
			}

			if ("omega".equals(option))
			{
				omegaLoaded = true;
				continue;
			}

			if ("scroller".equals(option))
			{
				scroller = true;
				continue;
			}

			BaRole parsedRole = parseBaWaveRole(option);
			if (parsedRole == null)
			{
				addChatMessage("Unknown ::bawave option: " + arguments[i]);
				addBaWaveUsage();
				return;
			}

			role = parsedRole;
		}

		if (role == null)
		{
			role = roleDetector.getCurrentRole();
			if (role == null)
			{
				role = BaRole.HEALER;
			}
		}

		WaveStart waveStart = waveLifecycleService.startDevWave(wave);
		if (waveStart == null)
		{
			addChatMessage("Wave must be between 1 and 10.");
			return;
		}

		roleDetector.setDevRoleOverride(role);
		scrollerController.setDevOverrides(scroller, queenSpawned, omegaLoaded);
		startWave(waveStart);

		addChatMessage("BA Utilities dev wave " + wave + " started as "
				+ (scroller ? "Scroller" : role.getDisplayName()) + ".");
	}

	private void handleBaUtilCommand(String[] arguments)
	{
		if (arguments.length < 1)
		{
			addBaUtilUsage();
			return;
		}

		boolean reset = false;
		for (String argument : arguments)
		{
			String option = argument == null ? "" : argument.trim().toLowerCase(Locale.ROOT);
			if (option.isEmpty()) continue;

			if ("onboard".equals(option))
			{
				presetManager.resetAssistancePresetForDev();
				addChatMessage("BA Utilities assistance onboarding reset.");
				reset = true;
				continue;
			}

			if ("update".equals(option))
			{
				presetManager.resetUpdateNotesForDev();
				addChatMessage("BA Utilities update notes reset.");
				reset = true;
				continue;
			}

			addChatMessage("Unknown ::bautil option: " + argument);
			addBaUtilUsage();
			return;
		}

		if (!reset)
		{
			addBaUtilUsage();
			return;
		}

		onboardingSidePanelOpened = false;
	}

	private void addBaUtilUsage()
	{
		addChatMessage("Usage: ::bautil onboard|update [onboard|update]");
	}

	private void addBaWaveUsage()
	{
		addChatMessage("Usage: ::bawave wave role [queen] [omega]");
		addChatMessage("Roles: attacker, collector, defender, healer, scroller. Use wave 0 to clear.");
	}

	private BaRole parseBaWaveRole(String rawRole)
	{
		if (rawRole == null || rawRole.trim().isEmpty()) return null;

		String role = rawRole.trim().toLowerCase();
		if (role.startsWith("a")) return BaRole.ATTACKER;
		if (role.startsWith("h")) return BaRole.HEALER;
		if (role.startsWith("c")) return BaRole.COLLECTOR;
		if (role.startsWith("d")) return BaRole.DEFENDER;

		return null;
	}

	private void handleOnboardingAutoOpen()
	{
		if (!panel.shouldShowOnboarding())
		{
			onboardingSidePanelOpened = false;
			return;
		}

		if (onboardingSidePanelOpened) return;

		if (!isInOrNearBarbarianAssault()) return;

		panel.showOnboarding();
		healerController.openSidePanel();
		onboardingSidePanelOpened = true;
	}

	private boolean isInOrNearBarbarianAssault()
	{
		if (client.getGameState() != GameState.LOGGED_IN) return false;

		int[] mapRegions = client.getMapRegions();

		if (mapRegions == null) return false;

		for (int region : mapRegions)
		{
			if (region == BA_SURFACE_REGION
					|| region == BA_LOBBY_REGION
					|| region == BA_WAVES_1_TO_9_REGION
					|| region == BA_WAVE_10_REGION)
			{
				return true;
			}
		}

		return false;
	}

	private void addChatMessage(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}
}
