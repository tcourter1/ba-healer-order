package com.bahealerorder.codes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HealerCodeParser
{
	private static final Pattern COUNT_PATTERN = Pattern.compile("^\\s*(\\d+)");
	private static final Pattern AFTER_PATTERN = Pattern.compile("\\((\\d+)\\)");
	private static final Pattern BEFORE_PATTERN = Pattern.compile("\\[(\\d+)]");

	private HealerCodeParser()
	{
	}

	public static WaveCode parseWaveCode(String id, String name, int wave, boolean builtIn, String sourceText)
	{
		List<CallCode> calls = new ArrayList<>();

		if (sourceText != null)
		{
			int callIndex = 0;

			for (String rawLine : sourceText.split("\\\\|\\r?\\n"))
			{
				String line = stripComment(rawLine).trim();

				// Descriptive lines and commented alternates stay in the source text for
				// display, but only code-looking lines become sequential BA call entries.
				if (!looksLikeCodeLine(line))
				{
					continue;
				}

				List<HealerInstruction> instructions = parseCodeLine(line);

				if (!instructions.isEmpty())
				{
					calls.add(new CallCode(callIndex++, instructions, null));
				}
			}
		}

		return new WaveCode(id, name, wave, builtIn, sourceText, calls);
	}

	public static List<HealerInstruction> parseCodeLine(String line)
	{
		List<HealerInstruction> instructions = new ArrayList<>();

		if (line == null || line.trim().isEmpty())
		{
			return instructions;
		}

		for (String rawPart : line.split("\\s*[-,]\\s*"))
		{
			String part = rawPart.trim();

			if (part.isEmpty())
			{
				continue;
			}

			instructions.add(parseInstruction(part));
		}

		return instructions;
	}

	public static HealerInstruction parseInstruction(String rawPart)
	{
		String part = rawPart == null ? "" : rawPart.trim();

		if (part.isEmpty() || "x".equalsIgnoreCase(part) || part.toLowerCase().contains("yolo"))
		{
			// "X"/spam/YOLO instructions deliberately have no target count; the overlay
			// should fall back to showing raw food used instead of progress to a goal.
			return new HealerInstruction(0, null, null, part);
		}

		Matcher countMatcher = COUNT_PATTERN.matcher(part);
		int count = 0;

		if (countMatcher.find())
		{
			try
			{
				count = Integer.parseInt(countMatcher.group(1));
			}
			catch (NumberFormatException ignored)
			{
				count = 0;
			}
		}

		return new HealerInstruction(count, firstInt(AFTER_PATTERN, part), firstInt(BEFORE_PATTERN, part), part);
	}

	private static String stripComment(String rawLine)
	{
		if (rawLine == null)
		{
			return "";
		}

		int commentIndex = rawLine.indexOf("//");
		return commentIndex >= 0 ? rawLine.substring(0, commentIndex) : rawLine;
	}

	private static boolean looksLikeCodeLine(String line)
	{
		if (line == null || line.isEmpty())
		{
			return false;
		}

		String firstToken = line.split("\\s*[-,]\\s*", 2)[0].trim();
		return firstToken.equalsIgnoreCase("x") || COUNT_PATTERN.matcher(firstToken).find();
	}

	private static Integer firstInt(Pattern pattern, String value)
	{
		Matcher matcher = pattern.matcher(value);

		if (!matcher.find())
		{
			return null;
		}

		try
		{
			return Integer.parseInt(matcher.group(1));
		}
		catch (NumberFormatException ignored)
		{
			return null;
		}
	}
}
