package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.BaUtilitiesPanel;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
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
	private static final int BA_TEAM_WIDGET_DEBUG_INTERVAL_TICKS = 10;
	private static final int CHATBOX_GROUP_ID = 162;
	private static final int BA_TEAM_GROUP_ID = 256;

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

	private int currentWave = -1;
	private long waveStartTimeMs = -1;
	private int inGameBit;
	private int lastBaTeamWidgetDebugTick = -BA_TEAM_WIDGET_DEBUG_INTERVAL_TICKS;
	private int baPartySyncJoinAttemptTick = -1;
	private int baPartySyncPendingDoorExitTick = -1;
	private int baPartySyncPendingDoorExitObjectId = -1;
	private String baPartySyncStatus = "Off";
	private String baPartySyncProgenitorName;
	private String baPartySyncPassphrase;
	private String lastDisplayedBaPartySyncStatus;
	private List<BaPartySyncMemberStatus> lastDisplayedBaPartySyncMemberStatuses = new ArrayList<>();
	private List<String> baPartySyncTeamNames = new ArrayList<>();
	private boolean baSyncManagedParty;

	@Inject
	private BaPartySyncService(Client client, PartyService partyService, PluginManager pluginManager, WSClient wsClient, BaUtilitiesConfig config, BaUtilitiesPanel panel)
	{
		this.client = client;
		this.partyService = partyService;
		this.pluginManager = pluginManager;
		this.wsClient = wsClient;
		this.config = config;
		this.panel = panel;
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

	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE) return;

		String message = Text.removeTags(event.getMessage()).toLowerCase(Locale.ROOT);

		if (handleWaveStartMessage(message)) return;

		if (message.matches(".*---- wave: (10|[1-9]) ----.*"))
		{
			startNewWave(Integer.parseInt(message.replaceAll(".*---- wave: (10|[1-9]) ----.*", "$1")));
		}
	}

	public void onVarbitChanged(VarbitChanged event)
	{
		int currentInGameBit = client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING);

		if (inGameBit == currentInGameBit) return;

		inGameBit = currentInGameBit;

		if (currentInGameBit == 0)
		{
			if (currentWave == 10)
			{
				leaveBaSyncParty("BA wave 10 ended");
			}

			resetWaveState();
		}
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();

		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			leaveBaSyncParty("game state changed to " + gameState);
			resetAllState();
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

		if (isWaveActive())
		{
			setBaPartySyncStatus(partyService.isInParty() ? "In Wave" : "In Wave - Not Connected", baPartySyncProgenitorName);
			return;
		}

		Optional<BaTeamRoster> teamRoster = getBaTeamRosterFromWidgetScanner();

		if (partyService.isInParty())
		{
			String currentPassphrase = partyService.getPartyPassphrase();
			teamRoster.ifPresent(roster -> baPartySyncTeamNames = roster.names);

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
		baPartySyncTeamNames = teamRoster.get().names;
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
					teamRoster.get().names
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
				currentWave,
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
			statuses.add(new BaPartySyncMemberStatus(name, partyService.getMemberByDisplayName(name) != null));
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
					|| leftStatus.isInParty() != rightStatus.isInParty())
			{
				return false;
			}
		}

		return true;
	}

	private Optional<BaTeamRoster> getBaTeamRosterFromWidgetScanner()
	{
		Widget[] roots = client.getWidgetRoots();

		if (roots == null) return Optional.empty();

		List<WidgetTextCandidate> candidates = new ArrayList<>();
		List<String> contextTexts = new ArrayList<>();

		for (Widget root : roots)
		{
			collectVisibleWidgetTextCandidates(root, candidates, contextTexts);
		}

		Optional<BaTeamRoster> directBaTeamRoster = getBaTeamRosterFromKnownWidgetGroup(candidates, contextTexts);

		if (directBaTeamRoster.isPresent())
		{
			debugBaTeamWidgetScan(
					"Detected BA team roster from known widget group",
					candidates,
					contextTexts,
					directBaTeamRoster.get()
			);

			return directBaTeamRoster;
		}

		boolean hasCurrentTeamText = hasCurrentTeamContextText(contextTexts);
		debugBaTeamWidgetScan(
				hasCurrentTeamText
						? "Current team widget detected, but no visible BA team names found"
						: "No known BA team widget detected",
				candidates,
				contextTexts,
				null
		);

		if (hasCurrentTeamText && baSyncManagedParty)
		{
			leaveBaSyncParty("BA team cleared");
		}

		return Optional.empty();
	}

	private boolean hasCurrentTeamContextText(List<String> contextTexts)
	{
		for (String text : contextTexts)
		{
			if ("current team".equalsIgnoreCase(text))
			{
				return true;
			}
		}

		return false;
	}

	private Optional<BaTeamRoster> getBaTeamRosterFromKnownWidgetGroup(List<WidgetTextCandidate> candidates, List<String> contextTexts)
	{
		boolean hasCurrentTeamText = false;

		for (String text : contextTexts)
		{
			if ("current team".equalsIgnoreCase(text))
			{
				hasCurrentTeamText = true;
				break;
			}
		}

		if (!hasCurrentTeamText) return Optional.empty();

		List<WidgetTextCandidate> teamCandidates = new ArrayList<>();
		LinkedHashSet<String> names = new LinkedHashSet<>();

		for (WidgetTextCandidate candidate : candidates)
		{
			if ((candidate.widgetId >>> 16) != BA_TEAM_GROUP_ID || !isLikelyBaTeamPlayerName(candidate.text)) continue;

			if (names.add(candidate.text))
			{
				teamCandidates.add(candidate);
			}
		}

		if (teamCandidates.isEmpty())
		{
			log.debug("BA party sync found Current team widget, but no group {} team member names were visible yet", BA_TEAM_GROUP_ID);
			return Optional.empty();
		}

		teamCandidates.sort(Comparator.comparingInt(candidate -> candidate.bounds.y));
		return Optional.of(new BaTeamRoster(new ArrayList<>(names), teamCandidates));
	}

	private boolean isIgnoredWidgetForBaTeamScan(Widget widget)
	{
		int groupId = getWidgetGroupId(widget);

		if (groupId == CHATBOX_GROUP_ID) return true;

		Rectangle bounds = widget.getBounds();

		if (bounds == null) return false;

		return bounds.y >= 480;
	}

	private int getWidgetGroupId(Widget widget)
	{
		return widget.getId() >>> 16;
	}

	private void collectVisibleWidgetTextCandidates(Widget widget, List<WidgetTextCandidate> candidates, List<String> contextTexts)
	{
		if (widget == null || widget.isHidden() || isIgnoredWidgetForBaTeamScan(widget)) return;

		collectWidgetTextCandidate(widget, widget.getText(), candidates, contextTexts);
		collectWidgetTextCandidate(widget, widget.getName(), candidates, contextTexts);

		Widget[] dynamicChildren = widget.getDynamicChildren();
		if (dynamicChildren != null)
		{
			for (Widget child : dynamicChildren)
			{
				collectVisibleWidgetTextCandidates(child, candidates, contextTexts);
			}
		}

		Widget[] staticChildren = widget.getStaticChildren();
		if (staticChildren != null)
		{
			for (Widget child : staticChildren)
			{
				collectVisibleWidgetTextCandidates(child, candidates, contextTexts);
			}
		}

		Widget[] nestedChildren = widget.getNestedChildren();
		if (nestedChildren != null)
		{
			for (Widget child : nestedChildren)
			{
				collectVisibleWidgetTextCandidates(child, candidates, contextTexts);
			}
		}
	}

	private void collectWidgetTextCandidate(Widget widget, String rawText, List<WidgetTextCandidate> candidates, List<String> contextTexts)
	{
		String text = cleanWidgetText(rawText);

		if (text.isEmpty()) return;

		contextTexts.add(text);

		if (!isLikelyBaTeamPlayerName(text)) return;

		Rectangle bounds = widget.getBounds();

		if (bounds == null || bounds.width <= 0 || bounds.height <= 0) return;

		candidates.add(new WidgetTextCandidate(text, bounds, widget.getId()));
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

	private String cleanWidgetText(String text)
	{
		if (text == null) return "";

		return Text.removeTags(text)
				.replace('\u00A0', ' ')
				.replaceAll("\\s+", " ")
				.trim();
	}

	private void debugBaTeamWidgetScan(String message, List<WidgetTextCandidate> candidates, List<String> contextTexts, BaTeamRoster roster)
	{
		if (!config.enableBaPartySync()) return;

		int tick = client.getTickCount();

		if (tick - lastBaTeamWidgetDebugTick < BA_TEAM_WIDGET_DEBUG_INTERVAL_TICKS) return;

		lastBaTeamWidgetDebugTick = tick;

		List<String> candidateTexts = new ArrayList<>();

		for (WidgetTextCandidate candidate : candidates)
		{
			candidateTexts.add(candidate.text + "@" + candidate.bounds.x + "," + candidate.bounds.y + "#" + candidate.widgetId + "/g" + (candidate.widgetId >>> 16));
		}

		List<String> interestingContextTexts = new ArrayList<>();

		for (String text : contextTexts)
		{
			String lower = text.toLowerCase(Locale.ROOT);

			if (lower.contains("current team")
					|| lower.contains("leader:")
					|| lower.contains("player 1:")
					|| lower.contains("player 2:")
					|| lower.contains("player 3:")
					|| lower.contains("player 4:")
					|| lower.contains("barbarian")
					|| lower.contains("wave")
					|| lower.contains("attacker")
					|| lower.contains("collector")
					|| lower.contains("defender")
					|| lower.contains("healer")
					|| lower.contains("penance"))
			{
				interestingContextTexts.add(text);
			}
		}

		log.debug(
				"BA party sync widget scan: {}. Roster: {}. Candidates: {}. Context: {}",
				message,
				roster == null ? null : roster.names,
				candidateTexts,
				interestingContextTexts
		);
	}

	private boolean handleWaveStartMessage(String message)
	{
		if (!message.matches(".*\\bwave:\\s*(10|[1-9])\\b.*")) return false;

		try
		{
			startNewWave(Integer.parseInt(message.replaceAll(".*\\bwave:\\s*(10|[1-9])\\b.*", "$1")));
			return true;
		}
		catch (NumberFormatException ex)
		{
			log.debug("Failed to parse BA wave start message: {}", message, ex);
			return false;
		}
	}

	private void startNewWave(int wave)
	{
		currentWave = wave;
		waveStartTimeMs = System.currentTimeMillis();
	}

	private boolean isWaveActive()
	{
		return waveStartTimeMs > 0 && currentWave > 0;
	}

	private void resetWaveState()
	{
		waveStartTimeMs = -1;
	}

	private void resetAllState()
	{
		resetWaveState();
		currentWave = -1;
		inGameBit = 0;
	}

	private static class BaTeamRoster
	{
		private final List<String> names;
		private final List<WidgetTextCandidate> candidates;

		private BaTeamRoster(List<String> names, List<WidgetTextCandidate> candidates)
		{
			this.names = names;
			this.candidates = candidates;
		}

		private String getProgenitorName()
		{
			if (candidates == null || candidates.isEmpty())
			{
				return names.isEmpty() ? null : names.get(0);
			}

			WidgetTextCandidate topCandidate = candidates.get(0);

			for (WidgetTextCandidate candidate : candidates)
			{
				if (candidate.bounds.y < topCandidate.bounds.y)
				{
					topCandidate = candidate;
				}
			}

			return topCandidate.text;
		}
	}

	private static class WidgetTextCandidate
	{
		private final String text;
		private final Rectangle bounds;
		private final int widgetId;

		private WidgetTextCandidate(String text, Rectangle bounds, int widgetId)
		{
			this.text = text;
			this.bounds = bounds;
			this.widgetId = widgetId;
		}
	}
}
