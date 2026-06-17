package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesPanel;
import com.bahealerorder.healer.HealerSharedState;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatLineBuffer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
public class BaWaveOverviewService
{
	private static final Pattern WAVE_PATTERN = Pattern.compile(".*----\\s*wave:\\s*(10|[1-9])\\s*----.*", Pattern.CASE_INSENSITIVE);
	private static final Pattern WAVE_START_PATTERN = Pattern.compile(".*\\bwave:\\s*(10|[1-9])\\b.*", Pattern.CASE_INSENSITIVE);
	private static final Pattern WAVE_DURATION_PATTERN = Pattern.compile(".*wave\\s+(10|[1-9])\\s+duration:\\s*([0-9]+:[0-5][0-9](?:\\.\\d+)?).*", Pattern.CASE_INSENSITIVE);
	private static final Pattern ROUND_DURATION_PATTERN = Pattern.compile(".*round\\s+duration:\\s*([0-9]+:[0-5][0-9](?:\\.\\d+)?).*", Pattern.CASE_INSENSITIVE);

	private final Client client;
	private final BaPartySyncService partySyncService;
	private final BaUtilitiesPanel panel;
	private final BaWaveOverviewState state;
	private final HealerSharedState healerState;
	private final BaWaveOverviewStore store;

	private int inGameBit;
	private int waveStartTick = -1;
	private String lastSentSignature;
	private BaWaveOverviewSnapshot pendingCompletionSnapshot;
	private int pendingCompletionWave = -1;
	private int pendingDurationWave = -1;
	private String pendingDuration;

	@Inject
	private BaWaveOverviewService(
			Client client,
			BaPartySyncService partySyncService,
			BaUtilitiesPanel panel,
			BaWaveOverviewState state,
			HealerSharedState healerState,
			BaWaveOverviewStore store)
	{
		this.client = client;
		this.partySyncService = partySyncService;
		this.panel = panel;
		this.state = state;
		this.healerState = healerState;
		this.store = store;
	}

	public void onChatMessage(ChatMessage event)
	{
		String roundDuration = parseRoundDurationFromMessage(event.getMessage());
		if (roundDuration != null)
		{
			log.debug("Parsed BA wave overview round duration: duration={}, type={}, message={}",
					roundDuration,
					event.getType(),
					event.getMessage());
			recordRoundDuration(roundDuration);
			return;
		}

		WaveDuration duration = parseWaveDurationFromMessage(event.getMessage());
		if (duration != null)
		{
			log.debug("Parsed BA wave overview duration: wave={}, duration={}, type={}, message={}",
					duration.wave,
					duration.duration,
					event.getType(),
					event.getMessage());
			recordWaveDuration(duration);
			return;
		}

		logUnparsedDurationLikeMessage(event);

		Integer wave = parseWaveFromMessage(event.getMessage());
		if (wave != null)
		{
			startWave(wave);
		}
	}

	public void onVarbitChanged(VarbitChanged event)
	{
		int currentInGameBit = client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING);

		if (inGameBit == currentInGameBit) return;

		inGameBit = currentInGameBit;

		if (currentInGameBit == 0)
		{
			finishWave();
			return;
		}

		if (currentInGameBit == 1 && !state.isWaveActive())
		{
			Integer wave = getLatestWaveFromChatHistory();

			if (wave != null)
			{
				startWave(wave);
			}
		}
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();

		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			reset();
		}
	}

	public void onBaWaveOverviewSyncMessage(BaWaveOverviewSyncMessage event)
	{
		if (event == null
				|| partySyncService.isLocalPartyMember(event.getMemberId())
				|| event.getWorld() != client.getWorld())
		{
			return;
		}

		if (state.updateFromParty(event))
		{
			saveCurrentSnapshot();
			refreshPanel();
		}
	}

	public void onPartyUserJoin(UserJoin event)
	{
		if (event == null || partySyncService.isLocalPartyMember(event.getMemberId())) return;

		sendSnapshot(true);
	}

	public void startWave(int wave)
	{
		if (shouldFallbackCompletePendingWave(wave))
		{
			log.debug("Saving pending BA wave overview snapshot for wave {} without duration because wave {} started",
					pendingCompletionWave,
					wave);
			completePendingWave(null);
		}
		else if (pendingCompletionWave == wave || pendingCompletionWave > wave)
		{
			clearPendingCompletion();
		}

		boolean stateChanged = state.startWave(wave);
		store.startWave(wave);
		boolean metadataChanged = saveCurrentTeamNames();

		if (stateChanged)
		{
			waveStartTick = client.getTickCount();
			lastSentSignature = null;
			sendSnapshot(true);
		}
		else if (state.getWave() == wave && waveStartTick < 0)
		{
			waveStartTick = client.getTickCount();
		}

		if (saveCurrentSnapshot() || stateChanged || metadataChanged)
		{
			refreshPanel();
		}
	}

	public void reset()
	{
		if (!state.isWaveActive() && pendingCompletionSnapshot == null && pendingDuration == null) return;

		clearPendingCompletion();
		state.reset();
		waveStartTick = -1;
		lastSentSignature = null;
		refreshPanel();
	}

	private void finishWave()
	{
		if (!state.isWaveActive()) return;

		int wave = capturePendingCompletionSnapshot();

		if (pendingDurationWave == wave)
		{
			completePendingWave(pendingDuration);
			return;
		}

		refreshPanel();
	}

	public void recordSpawn(BaOverviewNpcType type, int npcIndex)
	{
		if (state.recordSpawn(type, npcIndex))
		{
			saveCurrentSnapshot();
			refreshPanel();
			sendSnapshot(false);
		}
	}

	public void recordDeath(BaOverviewNpcType type, int npcIndex)
	{
		if (state.recordDeath(type, npcIndex, getCurrentWaveTick()))
		{
			saveCurrentSnapshot();
			refreshPanel();
			sendSnapshot(false);
		}
	}

	public void recordHealerStateChanged()
	{
		if (saveCurrentSnapshot())
		{
			refreshPanel();
		}
	}

	private void sendSnapshot(boolean force)
	{
		if (!partySyncService.isBaPartySyncConnected() || !state.isWaveActive()) return;

		BaWaveOverviewSyncMessage message = state.toSyncMessage(client.getWorld());
		String signature = buildSignature(message);

		if (!force && signature.equals(lastSentSignature)) return;

		lastSentSignature = signature;
		partySyncService.sendWaveOverviewSync(message);
	}

	private void refreshPanel()
	{
		panel.refreshOverviewLater();
	}

	private boolean saveCurrentSnapshot()
	{
		int wave = state.isWaveActive() ? state.getWave() : healerState.getWave();

		if (!BaWaveInfo.isValidWave(wave))
		{
			return false;
		}

		return store.saveSnapshot(BaWaveOverviewSnapshot.fromStates(wave, state, healerState));
	}

	private void recordWaveDuration(WaveDuration duration)
	{
		if (duration == null) return;

		if (pendingCompletionSnapshot != null && pendingCompletionWave == duration.wave)
		{
			completePendingWave(duration.duration);
			return;
		}

		if (state.isWaveActive() && state.getWave() == duration.wave)
		{
			capturePendingCompletionSnapshot();
			completePendingWave(duration.duration);
			return;
		}

		pendingDurationWave = duration.wave;
		pendingDuration = duration.duration;
	}

	private int capturePendingCompletionSnapshot()
	{
		int wave = state.getWave();
		pendingCompletionSnapshot = BaWaveOverviewSnapshot.fromStates(wave, state, healerState);
		pendingCompletionWave = wave;
		saveCurrentTeamNames();
		store.leaveWave(wave);
		state.reset();
		waveStartTick = -1;
		lastSentSignature = null;
		return wave;
	}

	private void completePendingWave(String duration)
	{
		if (pendingCompletionSnapshot == null || !BaWaveInfo.isValidWave(pendingCompletionWave)) return;

		store.completeSnapshot(pendingCompletionSnapshot.withDuration(duration));
		clearPendingCompletion();
		refreshPanel();
	}

	private void recordRoundDuration(String duration)
	{
		if (store.updateLatestCompletedRunRoundDuration(duration))
		{
			refreshPanel();
		}
	}

	private boolean saveCurrentTeamNames()
	{
		return store.updateCurrentRunTeamMembers(partySyncService.getBaPartySyncTeamMembers());
	}

	private void clearPendingCompletion()
	{
		pendingCompletionSnapshot = null;
		pendingCompletionWave = -1;
		pendingDurationWave = -1;
		pendingDuration = null;
	}

	private boolean shouldFallbackCompletePendingWave(int nextWave)
	{
		if (pendingCompletionSnapshot == null || !BaWaveInfo.isValidWave(pendingCompletionWave)) return false;

		return nextWave == pendingCompletionWave + 1
				|| pendingCompletionWave == 10 && nextWave == 1;
	}

	private Integer parseWaveFromMessage(String message)
	{
		String normalized = normalizeMessage(message);
		Matcher matcher = WAVE_PATTERN.matcher(normalized);

		if (!matcher.matches())
		{
			matcher = WAVE_START_PATTERN.matcher(normalized);
		}

		if (!matcher.matches()) return null;

		try
		{
			int wave = Integer.parseInt(matcher.group(1));
			return BaWaveInfo.isValidWave(wave) ? wave : null;
		}
		catch (NumberFormatException ex)
		{
			log.debug("Failed to parse BA wave number from message: {}", message, ex);
			return null;
		}
	}

	private WaveDuration parseWaveDurationFromMessage(String message)
	{
		String normalized = normalizeMessage(message);
		Matcher matcher = WAVE_DURATION_PATTERN.matcher(normalized);

		if (!matcher.matches()) return null;

		try
		{
			int wave = Integer.parseInt(matcher.group(1));
			return BaWaveInfo.isValidWave(wave) ? new WaveDuration(wave, matcher.group(2)) : null;
		}
		catch (NumberFormatException ex)
		{
			log.debug("Failed to parse BA wave duration from message: {}", message, ex);
			return null;
		}
	}

	private String parseRoundDurationFromMessage(String message)
	{
		String normalized = normalizeMessage(message);
		Matcher matcher = ROUND_DURATION_PATTERN.matcher(normalized);
		return matcher.matches() ? matcher.group(1) : null;
	}

	private void logUnparsedDurationLikeMessage(ChatMessage event)
	{
		String normalized = normalizeMessage(event.getMessage());

		if (!normalized.contains("wave") || !normalized.contains("duration")) return;

		log.debug("Saw BA wave overview duration-like message but could not parse it: type={}, message={}, normalized={}",
				event.getType(),
				event.getMessage(),
				normalized);
	}

	private String normalizeMessage(String message)
	{
		return Text.removeTags(message == null ? "" : message).toLowerCase(Locale.ROOT);
	}

	private Integer getLatestWaveFromChatHistory()
	{
		MessageNode latestWaveMessage = null;
		Map<Integer, ChatLineBuffer> chatLineMap = client.getChatLineMap();

		if (chatLineMap == null) return null;

		for (ChatLineBuffer buffer : chatLineMap.values())
		{
			if (buffer == null || buffer.getLines() == null) continue;

			for (MessageNode messageNode : buffer.getLines())
			{
				if (messageNode == null || parseWaveFromMessage(messageNode.getValue()) == null) continue;

				if (latestWaveMessage == null || messageNode.getTimestamp() > latestWaveMessage.getTimestamp())
				{
					latestWaveMessage = messageNode;
				}
			}
		}

		return latestWaveMessage == null ? null : parseWaveFromMessage(latestWaveMessage.getValue());
	}

	private String buildSignature(BaWaveOverviewSyncMessage message)
	{
		return message.getWave()
				+ ":" + java.util.Arrays.toString(message.getNpcTypes())
				+ ":" + java.util.Arrays.toString(message.getNpcIndexes())
				+ ":" + java.util.Arrays.toString(message.getNpcOrders())
				+ ":" + java.util.Arrays.toString(message.getDeadNpcTypes())
				+ ":" + java.util.Arrays.toString(message.getDeadNpcOrders())
				+ ":" + java.util.Arrays.toString(message.getDeadNpcDeathTicks());
	}

	private int getCurrentWaveTick()
	{
		return waveStartTick < 0 ? -1 : Math.max(0, client.getTickCount() - waveStartTick);
	}

	private static class WaveDuration
	{
		private final int wave;
		private final String duration;

		private WaveDuration(int wave, String duration)
		{
			this.wave = wave;
			this.duration = duration;
		}
	}
}
