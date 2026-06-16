package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesPanel;
import com.bahealerorder.healer.HealerSharedState;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
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
	private static final Pattern WAVE_DURATION_PATTERN = Pattern.compile(".*wave\\s+(10|[1-9])\\s+duration:\\s*([0-9]+:[0-5][0-9](?:\\.\\d+)?).*", Pattern.CASE_INSENSITIVE);

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
		if (event.getType() != ChatMessageType.GAMEMESSAGE) return;

		WaveDuration duration = parseWaveDurationFromMessage(event.getMessage());
		if (duration != null)
		{
			recordWaveDuration(duration);
			return;
		}

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
		clearPendingCompletion();
		boolean stateChanged = state.startWave(wave);
		store.startWave(wave);

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

		if (saveCurrentSnapshot() || stateChanged)
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

		int wave = state.getWave();
		pendingCompletionSnapshot = BaWaveOverviewSnapshot.fromStates(wave, state, healerState);
		pendingCompletionWave = wave;
		store.leaveWave(wave);
		state.reset();
		waveStartTick = -1;
		lastSentSignature = null;

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

		pendingDurationWave = duration.wave;
		pendingDuration = duration.duration;
	}

	private void completePendingWave(String duration)
	{
		if (pendingCompletionSnapshot == null || !BaWaveInfo.isValidWave(pendingCompletionWave)) return;

		store.completeSnapshot(pendingCompletionSnapshot.withDuration(duration));
		clearPendingCompletion();
		refreshPanel();
	}

	private void clearPendingCompletion()
	{
		pendingCompletionSnapshot = null;
		pendingCompletionWave = -1;
		pendingDurationWave = -1;
		pendingDuration = null;
	}

	private Integer parseWaveFromMessage(String message)
	{
		String normalized = Text.removeTags(message == null ? "" : message).toLowerCase(Locale.ROOT);
		Matcher matcher = WAVE_PATTERN.matcher(normalized);

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
		String normalized = Text.removeTags(message == null ? "" : message).toLowerCase(Locale.ROOT);
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
