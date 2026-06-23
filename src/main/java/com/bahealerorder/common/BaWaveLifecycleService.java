package com.bahealerorder.common;

import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.util.Text;

@Singleton
public class BaWaveLifecycleService
{
	private static final Pattern WAVE_PATTERN =
			Pattern.compile(".*----\\s*wave:\\s*(10|[1-9])\\s*----.*", Pattern.CASE_INSENSITIVE);
	private static final int ARENA_CONFIRM_TIMEOUT_TICKS = 10;

	private final Client client;
	private final BooleanSupplier arenaVisible;

	private int wave = -1;
	private int startTick = -1;
	private long startTimeMs = -1;
	private int messageNodeId = -1;
	private boolean arenaConfirmed;

	@Inject
	BaWaveLifecycleService(Client client, BaRoleDetector roleDetector)
	{
		this(
				client,
				() -> roleDetector.isRoleInterfaceLoaded() || client.isInInstancedRegion()
		);
	}

	BaWaveLifecycleService(Client client, BooleanSupplier arenaVisible)
	{
		this.client = client;
		this.arenaVisible = arenaVisible;
	}

	public WaveStart onChatMessage(ChatMessage event)
	{
		if (event == null || event.getType() != ChatMessageType.GAMEMESSAGE) return null;
		return start(event.getMessageNode(), event.getMessage(), "wave chat");
	}

	public Integer onGameTick()
	{
		if (!isWaveActive()) return null;

		if (isArenaVisible())
		{
			arenaConfirmed = true;
			return null;
		}

		if (!arenaConfirmed && client.getTickCount() - startTick <= ARENA_CONFIRM_TIMEOUT_TICKS) return null;

		int endedWave = wave;
		clearWave();
		return endedWave;
	}

	public Integer endWave()
	{
		if (!isWaveActive()) return null;

		int endedWave = wave;
		clearWave();
		return endedWave;
	}

	public void reset()
	{
		clearWave();
	}

	private void clearWave()
	{
		wave = -1;
		startTick = -1;
		startTimeMs = -1;
		messageNodeId = -1;
		arenaConfirmed = false;
	}

	public boolean isWaveActive()
	{
		return BaWaveInfo.isValidWave(wave) && startTick >= 0;
	}

	public int getWave()
	{
		return wave;
	}

	public int getStartTick()
	{
		return startTick;
	}

	public long getElapsedTimeMs()
	{
		return startTimeMs < 0 ? 0 : System.currentTimeMillis() - startTimeMs;
	}

	private WaveStart start(MessageNode messageNode, String message, String source)
	{
		Integer parsedWave = parseWave(message);
		if (parsedWave == null) return null;

		int nodeId = messageNode == null ? -1 : messageNode.getId();
		if (nodeId >= 0 && nodeId == messageNodeId) return null;

		wave = parsedWave;
		startTick = client.getTickCount();
		startTimeMs = System.currentTimeMillis();
		messageNodeId = nodeId;
		arenaConfirmed = false;
		return new WaveStart(wave, startTick, nodeId, source);
	}

	private boolean isArenaVisible()
	{
		return arenaVisible.getAsBoolean();
	}

	static Integer parseWave(String rawMessage)
	{
		Matcher matcher = WAVE_PATTERN.matcher(Text.removeTags(rawMessage == null ? "" : rawMessage));
		return matcher.matches() ? Integer.valueOf(matcher.group(1)) : null;
	}

	public static final class WaveStart
	{
		private final int wave;
		private final int tick;
		private final int messageNodeId;
		private final String source;

		private WaveStart(int wave, int tick, int messageNodeId, String source)
		{
			this.wave = wave;
			this.tick = tick;
			this.messageNodeId = messageNodeId;
			this.source = source;
		}

		public int getWave()
		{
			return wave;
		}

		public int getTick()
		{
			return tick;
		}

		public int getMessageNodeId()
		{
			return messageNodeId;
		}

		public String getSource()
		{
			return source;
		}
	}
}
