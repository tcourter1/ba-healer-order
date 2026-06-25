package com.bahealerorder.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import org.junit.Test;

public class BaPartySyncServiceTest
{
	private static final List<BaTeamMember> DUO_HEALER_TEAM = Arrays.asList(
			new BaTeamMember("Attacker", "Attacker"),
			new BaTeamMember("Healer One", "Healer"),
			new BaTeamMember("Defender", "Defender"),
			new BaTeamMember("Collector", "Collector"),
			new BaTeamMember("Healer Two", "Healer")
	);

	@Test
	public void completeDuoHealerPartyUsesDeterministicTtk()
	{
		assertFalse(BaPartySyncService.hasIncompleteDuoHealerParty(
				DUO_HEALER_TEAM,
				inParty("Healer One", "Healer Two")
		));
	}

	@Test
	public void missingDuoHealerUsesHealthRatioTtk()
	{
		assertTrue(BaPartySyncService.hasIncompleteDuoHealerParty(
				DUO_HEALER_TEAM,
				inParty("Healer One")
		));
	}

	@Test
	public void duoHealerWithoutPartyUsesHealthRatioTtk()
	{
		assertTrue(BaPartySyncService.hasIncompleteDuoHealerParty(
				DUO_HEALER_TEAM,
				inParty()
		));
	}

	@Test
	public void singleHealerTeamUsesDeterministicTtk()
	{
		List<BaTeamMember> team = Arrays.asList(
				new BaTeamMember("Attacker One", "Attacker"),
				new BaTeamMember("Attacker Two", "Attacker"),
				new BaTeamMember("Defender", "Defender"),
				new BaTeamMember("Collector", "Collector"),
				new BaTeamMember("Healer One", "Healer")
		);

		assertFalse(BaPartySyncService.hasIncompleteDuoHealerParty(team, inParty()));
	}

	@Test
	public void rosterPlayerNameCanBeAllNumbers()
	{
		assertEquals("123456", BaPartySyncService.getBaTeamPlayerName(0, "Leader: 123456"));
		assertEquals("123456", BaPartySyncService.getBaTeamPlayerName(0, "123456"));
		assertEquals("000 111", BaPartySyncService.getBaTeamPlayerName(2, "Player 2: 000 111"));
	}

	@Test
	public void rosterPlayerNameIgnoresEmptySlots()
	{
		assertNull(BaPartySyncService.getBaTeamPlayerName(1, "Player 1: -----"));
		assertNull(BaPartySyncService.getBaTeamPlayerName(3, "Player 3: "));
	}

	@Test
	public void devWaveIsNotRealPartySyncWave()
	{
		BaWaveLifecycleService lifecycle = new BaWaveLifecycleService(
				client(new AtomicInteger(100)),
				() -> true
		);

		assertTrue(lifecycle.startDevWave(1) != null);
		assertTrue(lifecycle.isWaveActive());
		assertTrue(lifecycle.isDevWaveActive());
		assertFalse(BaPartySyncService.isRealWaveActive(lifecycle));
	}

	private static java.util.function.Predicate<String> inParty(String... names)
	{
		Set<String> partyNames = new HashSet<>(names == null ? Collections.emptyList() : Arrays.asList(names));
		return partyNames::contains;
	}

	private static Client client(AtomicInteger tick)
	{
		return (Client) Proxy.newProxyInstance(
				Client.class.getClassLoader(),
				new Class<?>[]{Client.class},
				(proxy, method, args) -> "getTickCount".equals(method.getName()) ? tick.get() : defaultValue(method.getReturnType())
		);
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
