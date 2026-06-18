package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.BaUtilitiesPanel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.PartyMember;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.party.PartyPlugin;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
public class BaPartySyncService
{
	private static final String BA_SYNC_PASSPHRASE_SUFFIX = "BASYNC69";
	private static final int BA_WAVE_DOOR_FIRST_ID = 20199;
	private static final int BA_WAVE_DOOR_LAST_ID = 20208;
	private static final int BA_WAVE_DOOR_EXIT_CONFIRM_TICKS = 10;
	private static final int BA_LOBBY_REGION_ID = 10322;
	private static final int BA_TEAM_GROUP_ID = InterfaceID.BARBASSAULT_OVER_RECRUIT_PLAYER_NAMES;
	private static final int BA_TEAM_PLAYER1_NAME_CHILD_ID =
			InterfaceID.BarbassaultOverRecruitPlayerNames.BARBASSAULT_LEADER_NAME & 0xFFFF;
	private static final int BA_TEAM_PLAYER1_ROLE_CHILD_ID =
			InterfaceID.BarbassaultOverRecruitPlayerNames.BARBASSAULT_LEADER_ICON & 0xFFFF;
	private static final int BA_ATTACKER_ROLE_MODEL_ID = 20561;
	private static final int BA_COLLECTOR_ROLE_MODEL_ID = 20563;
	private static final int BA_DEFENDER_ROLE_MODEL_ID = 20566;
	private static final int BA_HEALER_ROLE_MODEL_ID = 20569;

	private static final int[][] BA_WAVE_DOOR_EXIT_TILES = {
			{2579, 5299, 0},
			{2587, 5299, 0},
			{2599, 5299, 0},
			{2607, 5299, 0},
			{2579, 5289, 0},
			{2587, 5289, 0},
			{2599, 5289, 0},
			{2607, 5289, 0},
			{2579, 5279, 0},
			{2587, 5279, 0}
	};

	private final Client client;
	private final PartyService partyService;
	private final PluginManager pluginManager;
	private final WSClient wsClient;
	private final BaUtilitiesConfig config;
	private final BaUtilitiesPanel panel;
	private final BaWaveLifecycleService waveLifecycleService;

	private int baPartySyncJoinAttemptTick = -1;
	private int baPartySyncPendingDoorExitTick = -1;
	private int baPartySyncPendingDoorExitObjectId = -1;
	private String baPartySyncStatus = "Off";
	private String baPartySyncProgenitorName;
	private String baPartySyncPassphrase;
	private String lastDisplayedBaPartySyncStatus;
	private List<BaPartySyncMemberStatus> lastDisplayedBaPartySyncMemberStatuses = new ArrayList<>();
	private List<String> baPartySyncTeamNames = new ArrayList<>();
	private List<BaTeamMember> baPartySyncTeamMembers = new ArrayList<>();
	private boolean baSyncManagedParty;

	@Inject
	private BaPartySyncService(
			Client client,
			PartyService partyService,
			PluginManager pluginManager,
			WSClient wsClient,
			BaUtilitiesConfig config,
			BaUtilitiesPanel panel,
			BaWaveLifecycleService waveLifecycleService)
	{
		this.client = client;
		this.partyService = partyService;
		this.pluginManager = pluginManager;
		this.wsClient = wsClient;
		this.config = config;
		this.panel = panel;
		this.waveLifecycleService = waveLifecycleService;
	}

	public void startUp()
	{
		if (config.enableBaPartySync())
		{
			enablePartyPluginIfNeeded();
		}

		wsClient.registerMessage(BaHealerSyncMessage.class);
		wsClient.registerMessage(BaWaveOverviewSyncMessage.class);
		updateBaPartySyncPanelStatus();
	}

	public void shutDown()
	{
		leaveBaSyncParty("plugin shutdown");
		wsClient.unregisterMessage(BaHealerSyncMessage.class);
		wsClient.unregisterMessage(BaWaveOverviewSyncMessage.class);
	}

	public boolean isBaPartySyncConnected()
	{
		return config.enableBaPartySync() && baSyncManagedParty && partyService.isInParty();
	}

	public boolean isLocalPartyMember(long memberId)
	{
		PartyMember localMember = partyService.getLocalMember();
		return localMember != null && localMember.getMemberId() == memberId;
	}

	public List<String> getBaPartySyncTeamNames()
	{
		return new ArrayList<>(baPartySyncTeamNames);
	}

	public List<BaTeamMember> getBaPartySyncTeamMembers()
	{
		return new ArrayList<>(baPartySyncTeamMembers);
	}

	public boolean hasIncompleteDuoHealerParty()
	{
		return hasIncompleteDuoHealerParty(
				baPartySyncTeamMembers,
				name -> partyService.getMemberByDisplayName(name) != null
		);
	}

	static boolean hasIncompleteDuoHealerParty(List<BaTeamMember> teamMembers, Predicate<String> isInParty)
	{
		int healerCount = 0;
		int healersInParty = 0;

		for (BaTeamMember member : teamMembers)
		{
			if (!BaRole.HEALER.getDisplayName().equals(member.getRole())) continue;

			healerCount++;
			if (isInParty.test(member.getName()))
			{
				healersInParty++;
			}
		}

		return healerCount >= 2 && healersInParty < healerCount;
	}

	public void sendHealerSync(BaHealerSyncMessage message)
	{
		if (!isBaPartySyncConnected() || message == null) return;

		try
		{
			partyService.send(message);
		}
		catch (RuntimeException ex)
		{
			log.debug("Failed to send BA healer sync message", ex);
		}
	}

	public void sendWaveOverviewSync(BaWaveOverviewSyncMessage message)
	{
		if (!isBaPartySyncConnected() || message == null) return;

		try
		{
			partyService.send(message);
		}
		catch (RuntimeException ex)
		{
			log.debug("Failed to send BA wave overview sync message", ex);
		}
	}

	public void onGameTick(GameTick event)
	{
		updateBaPartySyncDoorExit();
		updateBaPartySync();
	}

	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		debugBaDoorClick(event, event.getMenuOption(), event.getMenuTarget());
	}

	public void onWaveEnded(int wave)
	{
		if (wave == 10)
		{
			leaveBaSyncParty("BA wave 10 ended");
		}
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();

		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			leaveBaSyncParty("game state changed to " + gameState);
		}
	}

	public void onConfigChanged(ConfigChanged event)
	{
		if (!"bahealerorder".equals(event.getGroup())
				|| !"enableBaPartySync".equals(event.getKey())
				|| !config.enableBaPartySync())
		{
			return;
		}

		enablePartyPluginIfNeeded();
	}

	private void enablePartyPluginIfNeeded()
	{
		SwingUtilities.invokeLater(() ->
		{
			Optional<Plugin> partyPlugin = pluginManager.getPlugins().stream()
					.filter(PartyPlugin.class::isInstance)
					.findFirst();

			if (!partyPlugin.isPresent()) return;

			Plugin plugin = partyPlugin.get();

			try
			{
				if (!pluginManager.isPluginEnabled(plugin))
				{
					pluginManager.setPluginEnabled(plugin, true);
				}

				if (!pluginManager.isPluginActive(plugin))
				{
					pluginManager.startPlugin(plugin);
				}
			}
			catch (PluginInstantiationException ex)
			{
				log.debug("Failed to enable Party plugin for BA Party Sync", ex);
			}
		});
	}

	private void updateBaPartySync()
	{
		if (!config.enableBaPartySync())
		{
			if (baSyncManagedParty)
			{
				leaveBaSyncParty("BA Party Sync disabled");
			}

			setBaPartySyncStatus("Off", null);
			return;
		}

		Optional<BaTeamRoster> teamRoster = isWaveActive()
				|| client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING) == 1
				? Optional.empty()
				: getBaTeamRosterFromWidgets();
		teamRoster.ifPresent(this::setBaPartySyncTeam);

		if (isWaveActive())
		{
			setBaPartySyncStatus(partyService.isInParty() ? "In Wave" : "In Wave - Not Connected", baPartySyncProgenitorName);
			return;
		}

		if (partyService.isInParty())
		{
			String currentPassphrase = partyService.getPartyPassphrase();

			if (baSyncManagedParty && baPartySyncPassphrase != null && !baPartySyncPassphrase.equals(currentPassphrase))
			{
				log.debug(
						"Current Party passphrase no longer matches managed BA sync party. Managed={}, current={}",
						baPartySyncPassphrase,
						currentPassphrase
				);

				baSyncManagedParty = false;
				baPartySyncPassphrase = null;
				baPartySyncJoinAttemptTick = -1;
				setBaPartySyncStatus("Already in Party", null);
				return;
			}

			baPartySyncJoinAttemptTick = -1;
			setBaPartySyncStatus(baSyncManagedParty ? "Connected" : "Already in Party", baPartySyncProgenitorName);
			return;
		}

		if (baSyncManagedParty && baPartySyncPassphrase != null)
		{
			int ticksSinceJoinAttempt = client.getTickCount() - baPartySyncJoinAttemptTick;

			if (baPartySyncJoinAttemptTick >= 0 && ticksSinceJoinAttempt <= 10)
			{
				setBaPartySyncStatus("Connecting", baPartySyncProgenitorName);
				return;
			}

			log.debug("Managed BA sync party did not connect after {} ticks. Allowing a fresh join attempt.", ticksSinceJoinAttempt);
			baSyncManagedParty = false;
			baPartySyncPassphrase = null;
			baPartySyncJoinAttemptTick = -1;
		}

		if (!teamRoster.isPresent())
		{
			baPartySyncProgenitorName = null;
			setBaPartySyncStatus("Waiting for Team", null);
			return;
		}

		String progenitorName = teamRoster.get().getProgenitorName();

		if (progenitorName == null || progenitorName.isEmpty())
		{
			baPartySyncProgenitorName = null;
			setBaPartySyncStatus("Waiting for Team", null);
			return;
		}

		baPartySyncProgenitorName = progenitorName;
		setBaPartySyncTeam(teamRoster.get());
		String passphrase = buildBaPartySyncPassphrase(progenitorName);

		if (baSyncManagedParty && passphrase.equals(baPartySyncPassphrase))
		{
			setBaPartySyncStatus("Connecting", progenitorName);
			return;
		}

		setBaPartySyncStatus("Joining", progenitorName);

		try
		{
			baSyncManagedParty = true;
			baPartySyncPassphrase = passphrase;
			baPartySyncJoinAttemptTick = client.getTickCount();

			partyService.changeParty(passphrase);

			setBaPartySyncStatus("Connecting", progenitorName);

			log.debug(
					"Joined or created BA sync party using progenitor {} and passphrase {}. Team roster: {}",
					progenitorName,
					passphrase,
					teamRoster.get().members
			);
		}
		catch (RuntimeException ex)
		{
			baSyncManagedParty = false;
			baPartySyncPassphrase = null;
			baPartySyncJoinAttemptTick = -1;
			setBaPartySyncStatus("Join failed", progenitorName);
			log.debug("Failed to join BA sync party using passphrase {}", passphrase, ex);
		}
	}

	private void debugBaDoorClick(MenuOptionClicked event, String option, String target)
	{
		if (client.getLocalPlayer() == null) return;

		String optionText = Text.removeTags(option == null ? "" : option).toLowerCase(Locale.ROOT);
		String targetText = Text.removeTags(target == null ? "" : target).toLowerCase(Locale.ROOT);

		if (!isBaWaveDoorClick(event, optionText, targetText)) return;

		WorldPoint worldPoint = client.getLocalPlayer().getWorldLocation();

		log.debug(
				"BA door debug: option='{}', target='{}', id={}, menuAction={}, param0={}, param1={}, world=({}, {}, {}), regionId={}, regionLocal=({}, {}), areaExitPending={}, wave={}, managedParty={}, inParty={}",
				option,
				target,
				event.getId(),
				event.getMenuAction(),
				event.getParam0(),
				event.getParam1(),
				worldPoint.getX(),
				worldPoint.getY(),
				worldPoint.getPlane(),
				worldPoint.getRegionID(),
				worldPoint.getRegionX(),
				worldPoint.getRegionY(),
				client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING),
				waveLifecycleService.getWave(),
				baSyncManagedParty,
				partyService.isInParty()
		);

		if (baSyncManagedParty && partyService.isInParty())
		{
			baPartySyncPendingDoorExitTick = client.getTickCount();
			baPartySyncPendingDoorExitObjectId = event.getId();

			log.debug(
					"Armed BA sync door-exit check from door id {} at world=({}, {}, {})",
					event.getId(),
					worldPoint.getX(),
					worldPoint.getY(),
					worldPoint.getPlane()
			);
		}
	}

	private boolean isBaWaveDoorClick(MenuOptionClicked event, String optionText, String targetText)
	{
		return event.getMenuAction() == MenuAction.GAME_OBJECT_FIRST_OPTION
				&& "pass".equals(optionText)
				&& "door".equals(targetText)
				&& event.getId() >= BA_WAVE_DOOR_FIRST_ID
				&& event.getId() <= BA_WAVE_DOOR_LAST_ID;
	}

	private void updateBaPartySyncDoorExit()
	{
		if (baPartySyncPendingDoorExitTick < 0) return;

		if (!baSyncManagedParty || !partyService.isInParty())
		{
			clearBaPartySyncPendingDoorExit();
			return;
		}

		int ticksSinceDoorClick = client.getTickCount() - baPartySyncPendingDoorExitTick;

		if (ticksSinceDoorClick > BA_WAVE_DOOR_EXIT_CONFIRM_TICKS)
		{
			log.debug("BA sync door-exit check expired after {} ticks", ticksSinceDoorClick);
			clearBaPartySyncPendingDoorExit();
			return;
		}

		if (client.getLocalPlayer() == null) return;

		WorldPoint worldPoint = client.getLocalPlayer().getWorldLocation();

		if (!isBaWaveDoorExitLandingTile(worldPoint)) return;

		log.debug(
				"Confirmed BA sync door exit from door id {} at world=({}, {}, {}). Leaving managed party.",
				baPartySyncPendingDoorExitObjectId,
				worldPoint.getX(),
				worldPoint.getY(),
				worldPoint.getPlane()
		);

		clearBaPartySyncPendingDoorExit();
		leaveBaSyncParty("left BA room through wave door");
	}

	private boolean isBaWaveDoorExitLandingTile(WorldPoint worldPoint)
	{
		if (worldPoint == null || worldPoint.getRegionID() != BA_LOBBY_REGION_ID) return false;

		for (int[] tile : BA_WAVE_DOOR_EXIT_TILES)
		{
			if (worldPoint.getX() == tile[0]
					&& worldPoint.getY() == tile[1]
					&& worldPoint.getPlane() == tile[2])
			{
				return true;
			}
		}

		return false;
	}

	private void clearBaPartySyncPendingDoorExit()
	{
		baPartySyncPendingDoorExitTick = -1;
		baPartySyncPendingDoorExitObjectId = -1;
	}

	private String buildBaPartySyncPassphrase(String progenitorName)
	{
		return progenitorName + BA_SYNC_PASSPHRASE_SUFFIX;
	}

	private void leaveBaSyncParty(String reason)
	{
		if (!baSyncManagedParty)
		{
			baPartySyncPassphrase = null;
			baPartySyncProgenitorName = null;
			baPartySyncTeamNames = new ArrayList<>();
			baPartySyncTeamMembers = new ArrayList<>();
			baPartySyncJoinAttemptTick = -1;
			clearBaPartySyncPendingDoorExit();
			updateBaPartySyncPanelStatus();
			return;
		}

		try
		{
			if (partyService.isInParty())
			{
				log.debug("Leaving managed BA sync party. Reason: {}", reason);
				partyService.changeParty(null);
			}
		}
		catch (RuntimeException ex)
		{
			log.debug("Failed to leave managed BA sync party. Reason: {}", reason, ex);
		}
		finally
		{
			baSyncManagedParty = false;
			baPartySyncPassphrase = null;
			baPartySyncProgenitorName = null;
			baPartySyncTeamNames = new ArrayList<>();
			baPartySyncTeamMembers = new ArrayList<>();
			baPartySyncJoinAttemptTick = -1;
			clearBaPartySyncPendingDoorExit();
			setBaPartySyncStatus(config.enableBaPartySync() ? "Waiting for Team" : "Off", null);
		}
	}

	private void setBaPartySyncStatus(String status, String progenitorName)
	{
		baPartySyncStatus = status;
		baPartySyncProgenitorName = progenitorName;
		updateBaPartySyncPanelStatus();
	}

	private void updateBaPartySyncPanelStatus()
	{
		List<BaPartySyncMemberStatus> memberStatuses = getBaPartySyncMemberStatuses();

		if (baPartySyncStatus != null
				&& baPartySyncStatus.equals(lastDisplayedBaPartySyncStatus)
				&& memberStatusesEqual(memberStatuses, lastDisplayedBaPartySyncMemberStatuses))
		{
			return;
		}

		lastDisplayedBaPartySyncStatus = baPartySyncStatus;
		lastDisplayedBaPartySyncMemberStatuses = memberStatuses;
		panel.updatePartySyncStatus(baPartySyncStatus, memberStatuses);
	}

	private List<BaPartySyncMemberStatus> getBaPartySyncMemberStatuses()
	{
		List<BaPartySyncMemberStatus> statuses = new ArrayList<>();

		if (!"Connected".equals(baPartySyncStatus) && !"In Wave".equals(baPartySyncStatus))
		{
			return statuses;
		}

		for (String name : baPartySyncTeamNames)
		{
			BaTeamMember member = getBaPartySyncTeamMember(name);
			statuses.add(new BaPartySyncMemberStatus(
					name,
					member == null ? null : member.getRole(),
					partyService.getMemberByDisplayName(name) != null));
		}

		return statuses;
	}

	private boolean memberStatusesEqual(List<BaPartySyncMemberStatus> left, List<BaPartySyncMemberStatus> right)
	{
		if (left.size() != right.size())
		{
			return false;
		}

		for (int i = 0; i < left.size(); i++)
		{
			BaPartySyncMemberStatus leftStatus = left.get(i);
			BaPartySyncMemberStatus rightStatus = right.get(i);

			if (!leftStatus.getName().equals(rightStatus.getName())
					|| !Objects.equals(leftStatus.getRole(), rightStatus.getRole())
					|| leftStatus.isInParty() != rightStatus.isInParty())
			{
				return false;
			}
		}

		return true;
	}

	private Optional<BaTeamRoster> getBaTeamRosterFromWidgets()
	{
		List<BaTeamMember> members = new ArrayList<>();
		LinkedHashSet<String> names = new LinkedHashSet<>();
		boolean teamWidgetLoaded = false;

		for (int playerIndex = 0; playerIndex < 5; playerIndex++)
		{
			Widget nameWidget = client.getWidget(BA_TEAM_GROUP_ID, BA_TEAM_PLAYER1_NAME_CHILD_ID + playerIndex);
			if (nameWidget == null) continue;

			teamWidgetLoaded = true;
			String name = cleanWidgetText(nameWidget.getText());

			if (isLikelyBaTeamPlayerName(name) && names.add(name))
			{
				members.add(new BaTeamMember(name, getBaTeamRole(playerIndex)));
			}
		}

		if (!teamWidgetLoaded) return Optional.empty();

		if (members.isEmpty())
		{
			if (baSyncManagedParty)
			{
				leaveBaSyncParty("BA team cleared");
			}

			return Optional.empty();
		}

		return Optional.of(new BaTeamRoster(members));
	}

	private String getBaTeamRole(int playerIndex)
	{
		Widget roleWidget = client.getWidget(BA_TEAM_GROUP_ID, BA_TEAM_PLAYER1_ROLE_CHILD_ID + playerIndex);

		return roleWidget == null ? null : getBaTeamRoleForModelId(roleWidget.getModelId());
	}

	private boolean isLikelyBaTeamPlayerName(String text)
	{
		if (text == null || text.length() < 1 || text.length() > 12) return false;

		String lower = text.toLowerCase(Locale.ROOT);

		if (lower.contains("current team")
				|| lower.startsWith("leader")
				|| lower.startsWith("player ")
				|| lower.contains("-----")
				|| lower.contains("wave")
				|| lower.contains("attacker")
				|| lower.contains("collector")
				|| lower.contains("defender")
				|| lower.contains("healer")
				|| lower.contains("penance")
				|| lower.contains("points")
				|| lower.contains("role")
				|| lower.contains("level"))
		{
			return false;
		}

		if (!text.matches("[A-Za-z0-9 _\\-]+") || !text.matches(".*[A-Za-z].*"))
		{
			return false;
		}

		return !text.matches("\\d+");
	}

	private String getBaTeamRoleForModelId(int modelId)
	{
		switch (modelId)
		{
			case BA_ATTACKER_ROLE_MODEL_ID:
				return BaRole.ATTACKER.getDisplayName();
			case BA_COLLECTOR_ROLE_MODEL_ID:
				return BaRole.COLLECTOR.getDisplayName();
			case BA_DEFENDER_ROLE_MODEL_ID:
				return BaRole.DEFENDER.getDisplayName();
			case BA_HEALER_ROLE_MODEL_ID:
				return BaRole.HEALER.getDisplayName();
			default:
				return null;
		}
	}

	private String cleanWidgetText(String text)
	{
		if (text == null) return "";

		return Text.removeTags(text)
				.replace('\u00A0', ' ')
				.replaceAll("\\s+", " ")
				.trim();
	}

	private void setBaPartySyncTeam(BaTeamRoster roster)
	{
		baPartySyncTeamMembers = new ArrayList<>(roster.members);
		baPartySyncTeamNames = new ArrayList<>(roster.names);
	}

	private BaTeamMember getBaPartySyncTeamMember(String name)
	{
		for (BaTeamMember member : baPartySyncTeamMembers)
		{
			if (member.getName().equals(name))
			{
				return member;
			}
		}

		return null;
	}

	private boolean isWaveActive()
	{
		return waveLifecycleService.isWaveActive();
	}

	private static class BaTeamRoster
	{
		private final List<BaTeamMember> members;
		private final List<String> names;

		private BaTeamRoster(List<BaTeamMember> members)
		{
			this.members = members;
			this.names = new ArrayList<>();
			for (BaTeamMember member : members)
			{
				this.names.add(member.getName());
			}
		}

		private String getProgenitorName()
		{
			return names.isEmpty() ? null : names.get(0);
		}
	}
}
