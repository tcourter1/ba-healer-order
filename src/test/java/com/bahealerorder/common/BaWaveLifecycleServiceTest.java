package com.bahealerorder.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import org.junit.Test;

public class BaWaveLifecycleServiceTest
{
	@Test
	public void parsesOnlyNativeWaveMessage()
	{
		assertEquals(Integer.valueOf(1), BaWaveLifecycleService.parseWave("---- Wave: 1 ----"));
		assertNull(BaWaveLifecycleService.parseWave("<colNORMAL>Wave 1 duration: <colHIGHLIGHT>0:00.6"));
		assertNull(BaWaveLifecycleService.parseWave("Wave: 1"));
	}

	@Test
	public void leavesWaveWhenConfirmedArenaDisappears()
	{
		AtomicInteger tick = new AtomicInteger(100);
		AtomicBoolean arenaVisible = new AtomicBoolean();
		BaWaveLifecycleService lifecycle = new BaWaveLifecycleService(
				client(tick),
				arenaVisible::get,
				arenaVisible::get
		);

		BaWaveLifecycleService.WaveStart start = lifecycle.onChatMessage(waveMessage(10, 1));
		assertNotNull(start);
		assertTrue(lifecycle.isWaveActive());
		assertNull(lifecycle.onGameTick());

		arenaVisible.set(true);
		assertNull(lifecycle.onGameTick());
		arenaVisible.set(false);

		assertEquals(Integer.valueOf(1), lifecycle.onGameTick());
		assertFalse(lifecycle.isWaveActive());
	}

	@Test
	public void sameWaveNativeMessageStartsFreshAttempt()
	{
		AtomicInteger tick = new AtomicInteger(100);
		AtomicBoolean arenaVisible = new AtomicBoolean(true);
		BaWaveLifecycleService lifecycle = new BaWaveLifecycleService(
				client(tick),
				arenaVisible::get,
				arenaVisible::get
		);

		lifecycle.onChatMessage(waveMessage(10, 1));
		tick.set(101);
		BaWaveLifecycleService.WaveStart restarted = lifecycle.onChatMessage(waveMessage(11, 1));

		assertNotNull(restarted);
		assertEquals(101, restarted.getTick());
		assertEquals(101, lifecycle.getStartTick());
	}

	@Test
	public void unconfirmedWaveStartExpires()
	{
		AtomicInteger tick = new AtomicInteger(100);
		BaWaveLifecycleService lifecycle = new BaWaveLifecycleService(
				client(tick),
				() -> false,
				() -> false
		);

		lifecycle.onChatMessage(waveMessage(10, 1));
		tick.set(111);

		assertEquals(Integer.valueOf(1), lifecycle.onGameTick());
		assertFalse(lifecycle.isWaveActive());
	}

	private static Client client(AtomicInteger tick)
	{
		return (Client) Proxy.newProxyInstance(
				Client.class.getClassLoader(),
				new Class<?>[]{Client.class},
				(proxy, method, args) -> "getTickCount".equals(method.getName()) ? tick.get() : defaultValue(method.getReturnType())
		);
	}

	private static ChatMessage waveMessage(int id, int wave)
	{
		String message = "---- Wave: " + wave + " ----";
		MessageNode node = (MessageNode) Proxy.newProxyInstance(
				MessageNode.class.getClassLoader(),
				new Class<?>[]{MessageNode.class},
				(proxy, method, args) ->
				{
					switch (method.getName())
					{
						case "getId":
							return id;
						case "getValue":
							return message;
						case "getTimestamp":
							return id;
						case "getType":
							return ChatMessageType.GAMEMESSAGE;
						default:
							return defaultValue(method.getReturnType());
					}
				}
		);
		return new ChatMessage(node, ChatMessageType.GAMEMESSAGE, "", message, "", id);
	}

	private static Object defaultValue(Class<?> type)
	{
		if (!type.isPrimitive()) return null;
		if (type == boolean.class) return false;
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		if (type == float.class) return 0f;
		if (type == double.class) return 0d;
		if (type == byte.class) return (byte) 0;
		if (type == short.class) return (short) 0;
		if (type == char.class) return '\0';
		return null;
	}
}
