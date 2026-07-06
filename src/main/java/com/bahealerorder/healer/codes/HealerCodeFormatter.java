package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class HealerCodeFormatter
{
	public static final int CALL_COUNT = 3;

	private HealerCodeFormatter()
	{
	}

	public static String format(WaveCode code)
	{
		return format(code, true);
	}

	public static String formatDisplay(WaveCode code)
	{
		if (code == null)
		{
			return "";
		}

		if (code.isLegacyMode())
		{
			return stripDisplayOnlyLines(code.getSourceText(), code.getName(), code.getWave());
		}

		return stripDisplayOnlyLines(format(code, false), code.getName(), code.getWave());
	}

	private static String format(WaveCode code, boolean includeExpectedTimes)
	{
		if (code == null)
		{
			return "";
		}

		List<String> lines = new ArrayList<>();
		List<String> headerParts = new ArrayList<>();

		if (code.isAlchHorn())
		{
			headerParts.add("Alch horn");
		}

		HealerCodeOverstock overstock = HealerCodeOverstock.valueOrRegular(code.getOverstock());
		if (overstock != HealerCodeOverstock.REGULAR)
		{
			headerParts.add(overstock + " OS");
		}

		if (!headerParts.isEmpty())
		{
			lines.add(String.join(", ", headerParts));
		}

		for (int callIndex = 0; callIndex < CALL_COUNT; callIndex++)
		{
			CallCode call = code.getCall(callIndex);
			if (hasCallText(call))
			{
				lines.add(formatCall(call));
			}
		}

		String times = includeExpectedTimes ? formatExpectedTimes(code) : "";
		if (!times.isEmpty())
		{
			lines.add("Expected: " + times);
		}

		if (!isBlank(code.getRestockingInstructions()))
		{
			lines.add("Restock: " + code.getRestockingInstructions().trim());
		}

		if (!isBlank(code.getAdditionalNotes()))
		{
			for (String line : code.getAdditionalNotes().trim().split("\\r?\\n", -1))
			{
				lines.add(line);
			}
		}

		return String.join("\n", lines);
	}

	private static boolean hasCallText(CallCode call)
	{
		return call != null && !call.getHealerInstructions().isEmpty();
	}

	public static String formatCall(CallCode call)
	{
		if (call == null || call.getHealerInstructions().isEmpty())
		{
			return "0";
		}

		List<String> parts = new ArrayList<>();
		for (HealerInstruction instruction : call.getHealerInstructions())
		{
			parts.add(formatInstruction(instruction));
		}
		return String.join("-", parts);
	}

	public static String formatInstruction(HealerInstruction instruction)
	{
		if (instruction == null || !instruction.hasTarget())
		{
			if (instruction != null && !isBlank(instruction.getRaw()))
			{
				return instruction.getRaw().trim();
			}
			return "0";
		}
		return instruction.formatTarget();
	}

	private static String formatExpectedTimes(WaveCode code)
	{
		Map<Integer, Integer> times = new TreeMap<>(code.getExpectedTimesSeconds());
		if (times.isEmpty())
		{
			return "";
		}

		List<String> parts = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : times.entrySet())
		{
			if (entry.getKey() != null && entry.getValue() != null)
			{
				parts.add("#" + entry.getKey() + "=" + entry.getValue() + "s");
			}
		}
		return String.join(", ", parts);
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}

	private static String stripDisplayOnlyLines(String sourceText, String codeName, int wave)
	{
		if (isBlank(sourceText))
		{
			return "";
		}

		List<String> lines = new ArrayList<>();
		for (String rawLine : sourceText.split("\\r?\\n", -1))
		{
			String line = rawLine.trim();
			if (line.toLowerCase().startsWith("expected:"))
			{
				continue;
			}
			if (isCodeNameLine(line, codeName, wave))
			{
				continue;
			}
			lines.add(rawLine);
		}
		return String.join("\n", lines).trim();
	}

	private static boolean isCodeNameLine(String line, String codeName, int wave)
	{
		if (isBlank(line) || isBlank(codeName))
		{
			return false;
		}

		String text = trimTrailingColon(line.trim());
		String name = trimTrailingColon(codeName.trim());
		if (text.equalsIgnoreCase(name))
		{
			return true;
		}

		if (text.toLowerCase().startsWith("code:"))
		{
			return trimTrailingColon(text.substring(5).trim()).equalsIgnoreCase(name);
		}

		String wavePrefix = "wave " + wave;
		String lower = text.toLowerCase();
		if (!lower.startsWith(wavePrefix))
		{
			return false;
		}

		String remainder = trimLeadingSeparators(text.substring(wavePrefix.length()).trim());
		return remainder.equalsIgnoreCase(name);
	}

	private static String trimTrailingColon(String value)
	{
		return value.endsWith(":") ? value.substring(0, value.length() - 1).trim() : value;
	}

	private static String trimLeadingSeparators(String value)
	{
		while (value.startsWith("-") || value.startsWith(":"))
		{
			value = value.substring(1).trim();
		}
		return value;
	}
}
