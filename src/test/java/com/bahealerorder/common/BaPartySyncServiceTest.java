package com.bahealerorder.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

	private static java.util.function.Predicate<String> inParty(String... names)
	{
		Set<String> partyNames = new HashSet<>(names == null ? Collections.emptyList() : Arrays.asList(names));
		return partyNames::contains;
	}
}
