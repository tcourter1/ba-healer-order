package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.List;

public final class HealerCodeStoreNormalizer
{
	private HealerCodeStoreNormalizer()
	{
	}

	public static boolean normalize(StrategyStore store, int currentVersion)
	{
		if (store == null)
		{
			return false;
		}

		boolean changed = store.getVersion() != currentVersion;
		List<WaveCode> userCodes = new ArrayList<>();
		for (WaveCode code : store.getWaveCodes())
		{
			if (code == null)
			{
				changed = true;
				continue;
			}

			changed = normalizeCustomCode(code) || changed;
			if (isBuiltInId(code.getId()))
			{
				if (!code.isBuiltIn())
				{
					code.setBuiltIn(true);
					changed = true;
				}
			}
			else if (code.isBuiltIn())
			{
				changed = true;
				continue;
			}

			userCodes.add(code);
		}

		if (userCodes.size() != store.getWaveCodes().size())
		{
			store.setWaveCodes(userCodes);
		}
		store.setVersion(currentVersion);
		return changed;
	}

	private static boolean normalizeCustomCode(WaveCode code)
	{
		if (code == null)
		{
			return false;
		}

		String source = code.getStoredSourceText();
		if (source == null || source.trim().isEmpty())
		{
			return false;
		}

		WaveCode parsed = HealerCodeParser.parseWaveCode(code.getId(), code.getName(), code.getWave(), false, source);
		code.setCalls(parsed.getCalls());
		code.setOverstock(parsed.getOverstock());
		code.setAlchHorn(parsed.isAlchHorn());
		code.setRestockingInstructions(parsed.getRestockingInstructions());
		code.setAdditionalNotes(parsed.getAdditionalNotes());
		code.setExpectedTimesSeconds(parsed.getExpectedTimesSeconds());
		code.clearStoredSourceText();
		return true;
	}

	private static boolean isBuiltInId(String id)
	{
		return id != null && id.startsWith("builtin:");
	}
}
