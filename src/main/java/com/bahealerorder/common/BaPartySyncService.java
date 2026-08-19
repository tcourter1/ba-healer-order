package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.sidepanel.BaUtilitiesPanel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.messages.PartyMemberMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.party.PartyPlugin;
import net.runelite.client.util.Text;

@Singleton
public class BaPartySyncService
{
	private static final String BA_SYNC_PASSPHRASE_SUFFIX = "BASYNC69";
	private static final int BA_WAVE_DOOR_FIRST_ID = 20199;
	private static final int BA_WAVE_DOOR_LAST_ID = 20208;
	private static final int BA_WAVE_DOOR_EXIT_CONFIRM_TICKS = 10;
	private static final int BA_LOBBY_REGION_ID = 10322;
	private static final int BA_TEAM_GROUP_ID = InterfaceID.BARBASSAULT_OVER_RECRUIT_PLAYER_NAMES;
	private static final int MAX_PARTY_CHAT_MESSAGE_LENGTH = 150;
	private static final String BA_PARTY_CHAT_SENDER = "BA Party Chat";
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
	private final ChatMessageManager chatMessageManager;
	private final BaUtilitiesConfig config;
	private final BaUtilitiesPanel panel;
	private final BaWaveLifecycleService waveLifecycleService;
	private final BaRoleDetector roleDetector;

	private int baPartySyncJoinAttemptTick = -1;
	private int baPartySyncPendingDoorExitTick = -1;
	private String baPartySyncStatus = "Off";
	private String baPartySyncProgenitorName;
	private String baPartySyncPassphrase;
	private String lastDisplayedBaPartySyncStatus;
	private List<BaPartySyncMemberStatus> lastDisplayedBaPartySyncMemberStatuses = new ArrayList<>();
	private List<String> baPartySyncTeamNames = new ArrayList<>();
	private List<BaTeamMember> baPartySyncTeamMembers = new ArrayList<>();
	private final Map<String, BaHealerFoodCounts> healerFoodCountsByPlayerName = new HashMap<>();
	private boolean baSyncManagedParty;

	@Inject
	private BaPartySyncService(
			Client client,
			PartyService partyService,
			PluginManager pluginManager,
			WSClient wsClient,
			ChatMessageManager chatMessageManager,
			BaUtilitiesConfig config,
			BaUtilitiesPanel panel,
			BaWaveLifecycleService waveLifecycleService,
			BaRoleDetector roleDetector)
	{
		this.client = client;
		this.partyService = partyService;
		this.pluginManager = pluginManager;
		this.wsClient = wsClient;
		this.chatMessageManager = chatMessageManager;
		this.config = config;
		this.panel = panel;
		this.waveLifecycleService = waveLifecycleService;
		this.roleDetector = roleDetector;
	}

	public void startUp()
	{
		if (config.enableBaPartySync())
		{
			enablePartyPluginIfNeeded();
		}

		wsClient.registerMessage(BaHealerSyncMessage.class);
		wsClient.registerMessage(BaHealerFoodCountMessage.class);
		wsClient.registerMessage(BaWaveOverviewSyncMessage.class);
		wsClient.registerMessage(BaPartyChatMessage.class);
		updateBaPartySyncPanelStatus();
	}

	public void shutDown()
	{
		leaveBaSyncParty();
		wsClient.unregisterMessage(BaHealerSyncMessage.class);
		wsClient.unregisterMessage(BaHealerFoodCountMessage.class);
		wsClient.unregisterMessage(BaWaveOverviewSyncMessage.class);
		wsClient.unregisterMessage(BaPartyChatMessage.class);
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

	public List<BaTeamMember> getBaPartySyncTeamMembers()
	{
		return new ArrayList<>(baPartySyncTeamMembers);
	}

	public boolean isLocalPlayerBaTeamLeader()
	{
		return !baPartySyncTeamMembers.isEmpty()
				&& isLocalPlayer(baPartySyncTeamMembers.get(0).getName());
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
		sendPartyMessage(message);
	}

	public void sendWaveOverviewSync(BaWaveOverviewSyncMessage message)
	{
		sendPartyMessage(message);
	}

	private void sendPartyMessage(PartyMemberMessage message)
	{
		if (!isBaPartySyncConnected() || message == null) return;

		try
		{
			partyService.send(message);
		}
		catch (RuntimeException ignored)
		{
		}
	}

	public void updateLocalHealerFoodCounts(String playerName, BaHealerFoodCounts counts, boolean forceSend)
	{
		if (playerName == null || playerName.isEmpty() || counts == null) return;

		String normalizedName = normalizePlayerName(playerName);
		BaHealerFoodCounts previous = healerFoodCountsByPlayerName.put(normalizedName, counts);
		boolean changed = !counts.equals(previous);

		if (changed)
		{
			updateBaPartySyncPanelStatus();
		}

		if (!changed && !forceSend) return;

		if (!isBaPartySyncConnected() || !isRealWaveActive()) return;

		BaHealerFoodCountMessage message = new BaHealerFoodCountMessage(
				playerName,
				client.getWorld(),
				counts.getTofu(),
				counts.getWorms(),
				counts.getMeat(),
				counts.getCalledFood()
		);

		sendPartyMessage(message);
	}

	public void onBaHealerFoodCountMessage(BaHealerFoodCountMessage event)
	{
		String playerName = event.getPlayerName();
		if (playerName == null || playerName.isEmpty()) return;

		if (isLocalPartyMember(event.getMemberId())) return;

		if (event.getWorld() != client.getWorld()) return;

		BaHealerFoodCounts counts = new BaHealerFoodCounts(
				event.getTofu(),
				event.getWorms(),
				event.getMeat(),
				event.getCalledFood()
		);
		String normalizedName = normalizePlayerName(playerName);
		BaHealerFoodCounts previous = healerFoodCountsByPlayerName.put(normalizedName, counts);

		if (!counts.equals(previous))
		{
			updateBaPartySyncPanelStatus();
		}
	}

	public void onChatMessage(ChatMessage event)
	{
		if (!isPartyChatEnabled() || !isRealWaveActive()) return;
		if (!isPublicChatType(event.getType())) return;
		if (BaPartyChatExclusions.contains(event.getMessage())) return;

		String playerName = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getName();
		if (playerName == null || playerName.isEmpty() || !samePlayerName(event.getName(), playerName)) return;

		String message = cleanPartyChatMessage(event.getMessage());
		if (message.isEmpty()) return;

		sendPartyMessage(new BaPartyChatMessage(client.getWorld(), message));
	}

	public void onBaPartyChatMessage(BaPartyChatMessage event)
	{
		if (!isPartyChatEnabled() || !isRealWaveActive()) return;
		if (isLocalPartyMember(event.getMemberId())) return;
		if (event.getWorld() != client.getWorld()) return;

		PartyMember member = partyService.getMemberById(event.getMemberId());
		if (member == null || !member.isLoggedIn()) return;

		String senderName = member.getDisplayName();
		if (senderName == null || senderName.isEmpty() || !isBaTeamMember(senderName)) return;

		String message = cleanPartyChatMessage(event.getMessage());
		if (message.isEmpty()) return;

		if (hasNativePublicChatMessage(client.getMessages(), senderName, message)) return;

		chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.PUBLICCHAT)
				.name(senderName)
				.runeLiteFormattedMessage(new ChatMessageBuilder().append(message).build())
				.sender(BA_PARTY_CHAT_SENDER)
				.build());
	}

	public void onGameTick()
	{
		updateBaPartySyncDoorExit();
		updateBaPartySync();
	}

	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		armBaPartySyncDoorExit(event);
	}

	public void onWaveEnded(int wave)
	{
		healerFoodCountsByPlayerName.clear();
		updateBaPartySyncPanelStatus();

		if (wave == 10)
		{
			leaveBaSyncParty();
		}
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();

		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			leaveBaSyncParty();
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
			catch (PluginInstantiationException ignored)
			{
			}
		});
	}

	private void updateBaPartySync()
	{
		Optional<BaTeamRoster> teamRoster = isRealWaveActive()
				|| client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING) == 1
				? Optional.empty()
				: getBaTeamRosterFromWidgets();
		teamRoster.ifPresent(this::setBaPartySyncTeam);

		if (!config.enableBaPartySync())
		{
			if (baSyncManagedParty)
			{
				leaveBaSyncParty();
			}

			setBaPartySyncStatus("Off", null);
			return;
		}

		if (isRealWaveActive())
		{
			setBaPartySyncStatus(partyService.isInParty() ? "In Wave" : "In Wave - Not Connected", baPartySyncProgenitorName);
			return;
		}

		if (partyService.isInParty())
		{
			String currentPassphrase = partyService.getPartyPassphrase();

			if (baSyncManagedParty && baPartySyncPassphrase != null && !baPartySyncPassphrase.equals(currentPassphrase))
			{
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
			if (baPartySyncJoinAttemptTick >= 0 && client.getTickCount() - baPartySyncJoinAttemptTick <= 10)
			{
				setBaPartySyncStatus("Connecting", baPartySyncProgenitorName);
				return;
			}

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
		String passphrase = progenitorName + BA_SYNC_PASSPHRASE_SUFFIX;

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
		}
		catch (RuntimeException ignored)
		{
			baSyncManagedParty = false;
			baPartySyncPassphrase = null;
			baPartySyncJoinAttemptTick = -1;
			setBaPartySyncStatus("Join failed", progenitorName);
		}
	}

	private void armBaPartySyncDoorExit(MenuOptionClicked event)
	{
		if (baSyncManagedParty && partyService.isInParty() && isBaWaveDoorClick(event))
		{
			baPartySyncPendingDoorExitTick = client.getTickCount();
		}
	}

	private boolean isBaWaveDoorClick(MenuOptionClicked event)
	{
		String optionText = Text.removeTags(event.getMenuOption() == null ? "" : event.getMenuOption()).toLowerCase(Locale.ROOT);
		String targetText = Text.removeTags(event.getMenuTarget() == null ? "" : event.getMenuTarget()).toLowerCase(Locale.ROOT);
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
			baPartySyncPendingDoorExitTick = -1;
			return;
		}

		int ticksSinceDoorClick = client.getTickCount() - baPartySyncPendingDoorExitTick;

		if (ticksSinceDoorClick > BA_WAVE_DOOR_EXIT_CONFIRM_TICKS)
		{
			baPartySyncPendingDoorExitTick = -1;
			return;
		}

		if (client.getLocalPlayer() == null) return;

		WorldPoint worldPoint = client.getLocalPlayer().getWorldLocation();

		if (!isBaWaveDoorExitLandingTile(worldPoint)) return;

		baPartySyncPendingDoorExitTick = -1;
		leaveBaSyncParty();
	}

	private boolean isBaWaveDoorExitLandingTile(WorldPoint worldPoint)
	{
		if (worldPoint.getRegionID() != BA_LOBBY_REGION_ID) return false;

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

	private void leaveBaSyncParty()
	{
		if (!baSyncManagedParty)
		{
			clearBaSyncState();
			updateBaPartySyncPanelStatus();
			return;
		}

		try
		{
			if (partyService.isInParty())
			{
				partyService.changeParty(null);
			}
		}
		catch (RuntimeException ignored)
		{
		}
		finally
		{
			clearBaSyncState();
			setBaPartySyncStatus(config.enableBaPartySync() ? "Waiting for Team" : "Off", null);
		}
	}

	private void clearBaSyncState()
	{
		baSyncManagedParty = false;
		baPartySyncPassphrase = null;
		baPartySyncProgenitorName = null;
		baPartySyncTeamNames = new ArrayList<>();
		baPartySyncTeamMembers = new ArrayList<>();
		healerFoodCountsByPlayerName.clear();
		baPartySyncJoinAttemptTick = -1;
		baPartySyncPendingDoorExitTick = -1;
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
				&& memberStatuses.equals(lastDisplayedBaPartySyncMemberStatuses))
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

		for (String name : baPartySyncTeamNames)
		{
			BaTeamMember member = getBaPartySyncTeamMember(name);
			String role = member == null ? null : member.getRole();
			boolean inParty = partyService.getMemberByDisplayName(name) != null;
			statuses.add(new BaPartySyncMemberStatus(
					name,
					role,
					inParty,
					getDisplayableHealerFoodCounts(name, role, inParty)));
		}

		return statuses;
	}

	private BaHealerFoodCounts getDisplayableHealerFoodCounts(String playerName, String role, boolean inParty)
	{
		if (!isRealWaveActive() || BaRole.fromDisplayName(role) != BaRole.HEALER) return null;

		if (!isLocalPlayer(playerName) && !inParty) return null;

		return healerFoodCountsByPlayerName.get(normalizePlayerName(playerName));
	}

	private boolean isLocalPlayer(String playerName)
	{
		return client.getLocalPlayer() != null
				&& normalizePlayerName(client.getLocalPlayer().getName()).equals(normalizePlayerName(playerName));
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
			String name = getBaTeamPlayerName(playerIndex, nameWidget.getText());

			if (name != null && names.add(name))
			{
				members.add(new BaTeamMember(name, getBaTeamRole(playerIndex)));
			}
		}

		if (!teamWidgetLoaded) return Optional.empty();

		if (members.isEmpty())
		{
			if (baSyncManagedParty)
			{
				leaveBaSyncParty();
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

	static String getBaTeamPlayerName(int playerIndex, String text)
	{
		String rosterText = cleanWidgetText(text);
		String expectedPrefix = playerIndex == 0 ? "Leader" : "Player " + playerIndex;

		if (rosterText.isEmpty()) return null;

		String name = rosterText.toLowerCase(Locale.ROOT).startsWith(expectedPrefix.toLowerCase(Locale.ROOT) + ":")
				? rosterText.substring(rosterText.indexOf(':') + 1).trim()
				: rosterText;
		return name.isEmpty() || "-----".equals(name) ? null : name;
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

	private static String cleanWidgetText(String text)
	{
		if (text == null) return "";

		return Text.removeTags(text)
				.replace('\u00A0', ' ')
				.replaceAll("\\s+", " ")
				.trim();
	}

	private static String normalizePlayerName(String playerName)
	{
		return Text.removeTags(Text.toJagexName(playerName == null ? "" : playerName)).toLowerCase(Locale.ROOT);
	}

	private void setBaPartySyncTeam(BaTeamRoster roster)
	{
		baPartySyncTeamMembers = new ArrayList<>(roster.members);
		baPartySyncTeamNames = new ArrayList<>(roster.names);
		roleDetector.setCurrentRole(findBaTeamRole(roster.members,
				client.getLocalPlayer() == null ? null : client.getLocalPlayer().getName()));
	}

	static BaRole findBaTeamRole(List<BaTeamMember> members, String playerName)
	{
		String name = normalizePlayerName(playerName);
		for (BaTeamMember member : members)
		{
			if (normalizePlayerName(member.getName()).equals(name)) return BaRole.fromDisplayName(member.getRole());
		}
		return null;
	}

	private boolean isPartyChatEnabled()
	{
		return config.enablePartyChat() && isBaPartySyncConnected();
	}

	private boolean isBaTeamMember(String playerName)
	{
		String normalizedPlayerName = normalizePlayerName(playerName);

		for (String teamName : baPartySyncTeamNames)
		{
			if (normalizePlayerName(teamName).equals(normalizedPlayerName)) return true;
		}

		return false;
	}

	private BaTeamMember getBaPartySyncTeamMember(String name)
	{
		for (BaTeamMember member : baPartySyncTeamMembers)
		{
			if (member.getName().equals(name)) return member;
		}

		return null;
	}

	private boolean isRealWaveActive()
	{
		return isRealWaveActive(waveLifecycleService);
	}

	static boolean isRealWaveActive(BaWaveLifecycleService waveLifecycleService)
	{
		return waveLifecycleService.isWaveActive() && !waveLifecycleService.isDevWaveActive();
	}

	static boolean hasNativePublicChatMessage(Iterable<MessageNode> messages, String senderName, String message)
	{
		if (messages == null) return false;

		for (MessageNode messageNode : messages)
		{
			if (isMatchingNativePublicChatMessage(messageNode, senderName, message)) return true;
		}

		return false;
	}

	static boolean isMatchingNativePublicChatMessage(MessageNode messageNode, String senderName, String message)
	{
		if (messageNode == null || !isPublicChatType(messageNode.getType())) return false;
		if (BA_PARTY_CHAT_SENDER.equals(messageNode.getSender())) return false;
		return samePlayerName(messageNode.getName(), senderName)
				&& normalizeChatMessage(messageNode.getValue()).equalsIgnoreCase(normalizeChatMessage(message));
	}

	static String cleanPartyChatMessage(String message)
	{
		String cleanMessage = Text.JAGEX_PRINTABLE_CHAR_MATCHER.retainFrom(message == null ? "" : message)
				.replaceAll("<img=[^>]*>", "")
				.trim();
		return cleanMessage.length() <= MAX_PARTY_CHAT_MESSAGE_LENGTH
				? cleanMessage
				: cleanMessage.substring(0, MAX_PARTY_CHAT_MESSAGE_LENGTH);
	}

	private static boolean isPublicChatType(ChatMessageType type)
	{
		return type == ChatMessageType.PUBLICCHAT || type == ChatMessageType.MODCHAT;
	}

	private static boolean samePlayerName(String left, String right)
	{
		return normalizePlayerName(left).equals(normalizePlayerName(right));
	}

	private static String normalizeChatMessage(String message)
	{
		return Text.removeFormattingTags(message == null ? "" : message)
				.replace('\u00A0', ' ')
				.trim();
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
