package com.bahealerorder.common;

import com.bahealerorder.sidepanel.BaUtilitiesPanel;
import com.bahealerorder.healer.HealerSharedState;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AllArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.util.Text;

@Singleton
public class BaWaveOverviewService
{
	private static final Pattern WAVE_DURATION_PATTERN = Pattern.compile(".*wave\\s+(10|[1-9])\\s+duration:\\s*([0-9]+:[0-5][0-9](?:\\.\\d+)?).*", Pattern.CASE_INSENSITIVE);
	private static final Pattern ROUND_DURATION_PATTERN = Pattern.compile(".*round\\s+duration:\\s*([0-9]+:[0-5][0-9](?:\\.\\d+)?).*", Pattern.CASE_INSENSITIVE);

	private final Client client;
	private final BaPartySyncService partySyncService;
	private final BaRoleDetector roleDetector;
	private final BaWaveLifecycleService waveLifecycleService;
	private final BaUtilitiesPanel panel;
	private final BaWaveOverviewState state;
	private final HealerSharedState healerState;
	private final BaWaveOverviewStore store;

	private int waveStartTick = -1;
	private String lastSentSignature;
	private BaWaveOverviewSnapshot pendingCompletionSnapshot;
	private int pendingCompletionWave = -1;
	private int pendingDurationWave = -1;
	private String pendingDuration;
	private boolean updatePending;
	private boolean syncPending;
	private boolean forceSyncPending;

	@Inject
	private BaWaveOverviewService(
			Client client,
			BaPartySyncService partySyncService,
			BaRoleDetector roleDetector,
			BaWaveLifecycleService waveLifecycleService,
			BaUtilitiesPanel panel,
			BaWaveOverviewState state,
			HealerSharedState healerState,
			BaWaveOverviewStore store)
	{
		this.client = client;
		this.partySyncService = partySyncService;
		this.roleDetector = roleDetector;
		this.waveLifecycleService = waveLifecycleService;
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
			recordRoundDuration(roundDuration);
			return;
		}

		WaveDuration duration = parseWaveDurationFromMessage(event.getMessage());
		if (duration != null)
		{
			recordWaveDuration(duration);
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
		if (partySyncService.isLocalPartyMember(event.getMemberId())
				|| event.getWorld() != client.getWorld()
				|| !waveLifecycleService.isWaveActive()
				|| event.getWave() != waveLifecycleService.getWave())
		{
			return;
		}

		if (state.updateFromParty(event))
		{
			updatePending = true;
		}
	}

	public void onPartyUserJoin(UserJoin event)
	{
		if (partySyncService.isLocalPartyMember(event.getMemberId())) return;

		sendSnapshot(true);
	}

	public void onWaveStarted(int wave)
	{
		if (shouldFallbackCompletePendingWave(wave))
		{
			completePendingWave(null);
		}
		else if (pendingCompletionWave == wave || pendingCompletionWave > wave)
		{
			clearPendingCompletion();
		}

		boolean sameWaveRestart = state.isWaveActive() && state.getWave() == wave;
		if (sameWaveRestart)
		{
			state.reset();
		}
		boolean stateChanged = state.startWave(wave);
		store.startWave(wave);
		boolean metadataChanged = saveCurrentRunMetadata();

		if (stateChanged)
		{
			waveStartTick = client.getTickCount();
			lastSentSignature = null;
			syncPending = true;
			forceSyncPending = true;
		}
		else if (state.getWave() == wave && waveStartTick < 0)
		{
			waveStartTick = client.getTickCount();
		}

		updatePending |= stateChanged || metadataChanged;
	}

	public void onGameTick()
	{
		boolean activeWaveTick = waveLifecycleService.isWaveActive();
		boolean metadataChanged = activeWaveTick && saveCurrentRunMetadata();
		if (!updatePending && !syncPending && !metadataChanged && !activeWaveTick) return;

		boolean changed = updatePending && saveCurrentSnapshot();
		if (changed || metadataChanged || activeWaveTick)
		{
			refreshPanel();
		}

		if (syncPending)
		{
			sendSnapshot(forceSyncPending);
		}

		updatePending = false;
		syncPending = false;
		forceSyncPending = false;
	}

	public void reset()
	{
		if (!state.isWaveActive() && pendingCompletionSnapshot == null && pendingDuration == null) return;

		clearPendingCompletion();
		state.reset();
		waveStartTick = -1;
		lastSentSignature = null;
		updatePending = false;
		syncPending = false;
		forceSyncPending = false;
		refreshPanel();
	}

	public void onWaveEnded()
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
			updatePending = true;
			syncPending = true;
		}
	}

	public void recordDeath(BaOverviewNpcType type, int npcIndex)
	{
		if (state.recordDeath(type, npcIndex, getCurrentWaveTick()))
		{
			updatePending = true;
			syncPending = true;
		}
	}

	public void recordHealerStateChanged()
	{
		updatePending = true;
	}

	private void sendSnapshot(boolean force)
	{
		if (!partySyncService.isBaPartySyncConnected() || !waveLifecycleService.isWaveActive()) return;

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
		int wave = waveLifecycleService.getWave();

		if (!BaWaveInfo.isValidWave(wave)) return false;

		return store.saveSnapshot(BaWaveOverviewSnapshot.fromStates(wave, state, healerState));
	}

	private void recordWaveDuration(WaveDuration duration)
	{
		if (pendingCompletionSnapshot != null && pendingCompletionWave == duration.wave)
		{
			completePendingWave(duration.duration);
			return;
		}

		if (waveLifecycleService.isWaveActive() && waveLifecycleService.getWave() == duration.wave)
		{
			capturePendingCompletionSnapshot();
			completePendingWave(duration.duration);
			return;
		}

		if (store.saveCurrentRunWaveDuration(duration.wave, duration.duration))
		{
			refreshPanel();
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
		saveCurrentRunMetadata();
		store.leaveWave(wave);
		state.reset();
		waveStartTick = -1;
		lastSentSignature = null;
		updatePending = false;
		syncPending = false;
		forceSyncPending = false;
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

	private boolean saveCurrentRunMetadata()
	{
		List<BaTeamMember> teamMembers = partySyncService.getBaPartySyncTeamMembers();
		boolean changed = store.updateCurrentRunPlayerRole(roleDetector.getCurrentRole());
		changed |= store.updateCurrentRunTeamMembers(teamMembers);
		return changed;
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

	private WaveDuration parseWaveDurationFromMessage(String message)
	{
		String normalized = normalizeMessage(message);
		Matcher matcher = WAVE_DURATION_PATTERN.matcher(normalized);

		if (!matcher.matches()) return null;

		return new WaveDuration(Integer.parseInt(matcher.group(1)), matcher.group(2));
	}

	private String parseRoundDurationFromMessage(String message)
	{
		String normalized = normalizeMessage(message);
		Matcher matcher = ROUND_DURATION_PATTERN.matcher(normalized);
		return matcher.matches() ? matcher.group(1) : null;
	}

	private String normalizeMessage(String message)
	{
		return Text.removeTags(message == null ? "" : message);
	}

	private String buildSignature(BaWaveOverviewSyncMessage message)
	{
		return message.getWave()
				+ ":" + Arrays.toString(message.getNpcTypes())
				+ ":" + Arrays.toString(message.getNpcIndexes())
				+ ":" + Arrays.toString(message.getNpcOrders())
				+ ":" + Arrays.toString(message.getDeadNpcTypes())
				+ ":" + Arrays.toString(message.getDeadNpcOrders())
				+ ":" + Arrays.toString(message.getDeadNpcDeathTicks());
	}

	private int getCurrentWaveTick()
	{
		return waveStartTick < 0 ? -1 : Math.max(0, client.getTickCount() - waveStartTick);
	}

	@AllArgsConstructor
	private static class WaveDuration
	{
		private final int wave;
		private final String duration;
	}
}
