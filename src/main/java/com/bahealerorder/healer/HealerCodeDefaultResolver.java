package com.bahealerorder.healer;

import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.BaTeamMember;
import com.bahealerorder.healer.codes.HealerCodeDefaultRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HealerCodeDefaultResolver
{
	private static final int DUO_TAG_HEALER_INDEX = 1;
	private static final int DUO_SPAM_HEALER_INDEX = 2;

	private HealerCodeDefaultResolver()
	{
	}

	public static HealerCodeDefaultRole resolve(BaRole currentRole, String localPlayerName, List<BaTeamMember> teamMembers)
	{
		if (teamMembers == null) return null;

		List<Integer> healerIndexes = healerIndexes(teamMembers);
		int localPlayerIndex = localPlayerIndex(localPlayerName, teamMembers);
		boolean localPlayerIsHealer = localPlayerIndex >= 0
				&& BaRole.fromDisplayName(teamMembers.get(localPlayerIndex).getRole()) == BaRole.HEALER;

		if (currentRole != BaRole.HEALER && !localPlayerIsHealer) return null;

		if (healerIndexes.size() == 1) return HealerCodeDefaultRole.SOLO_HEALER;

		if (healerIndexes.size() != 2
				|| healerIndexes.get(0) != DUO_TAG_HEALER_INDEX
				|| healerIndexes.get(1) != DUO_SPAM_HEALER_INDEX)
		{
			return null;
		}

		if (localPlayerIndex == DUO_TAG_HEALER_INDEX) return HealerCodeDefaultRole.DH_TAG_SECOND;
		if (localPlayerIndex == DUO_SPAM_HEALER_INDEX) return HealerCodeDefaultRole.DH_SPAM_MAIN;

		return null;
	}

	private static List<Integer> healerIndexes(List<BaTeamMember> teamMembers)
	{
		List<Integer> indexes = new ArrayList<>();
		for (int index = 0; index < teamMembers.size(); index++)
		{
			if (BaRole.fromDisplayName(teamMembers.get(index).getRole()) == BaRole.HEALER)
			{
				indexes.add(index);
			}
		}
		return indexes;
	}

	private static int localPlayerIndex(String localPlayerName, List<BaTeamMember> teamMembers)
	{
		String playerName = normalize(localPlayerName);
		if (playerName.isEmpty()) return -1;

		for (int index = 0; index < teamMembers.size(); index++)
		{
			if (playerName.equals(normalize(teamMembers.get(index).getName()))) return index;
		}
		return -1;
	}

	private static String normalize(String name)
	{
		return name == null ? "" : name.replace('\u00A0', ' ').trim().toLowerCase(Locale.ROOT);
	}
}
