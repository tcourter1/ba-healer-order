package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HealerCodeParser
{
	private static final Pattern COUNT_PATTERN = Pattern.compile("^\\s*(\\d+)");
	private static final Pattern AFTER_PATTERN = Pattern.compile("\\((\\d+)\\)");
	private static final Pattern BEFORE_PATTERN = Pattern.compile("\\[(\\d+)]");
	private static final Pattern EXACT_PATTERN = Pattern.compile("\\{(\\d+)}");
	private static final Pattern EXPECTED_TIME_PATTERN = Pattern.compile("#(\\d+)\\s*=\\s*(\\d+)s?", Pattern.CASE_INSENSITIVE);

	private HealerCodeParser()
	{
	}

	public static WaveCode parseWaveCode(String id, String name, int wave, boolean builtIn, String sourceText)
	{
		List<CallCode> calls = new ArrayList<>();

		if (sourceText != null)
		{
			int callIndex = 0;
			List<String> notes = new ArrayList<>();
			String restockingInstructions = null;
			boolean alchHorn = false;
			HealerCodeOverstock overstock = overstockFromText(name);
			Map<Integer, Integer> expectedTimes = new HashMap<>();

			for (String rawLine : sourceText.split("\\\\|\\r?\\n"))
			{
				String line = stripComment(rawLine).trim();
				String lower = line.toLowerCase();
				HealerCodeOverstock lineOverstock = overstockFromHeader(line);

				// Descriptive lines and commented alternates stay in the source text for
				// display, but only code-looking lines become sequential BA call entries.
				if (isMetadataLine(lower, lineOverstock) || !looksLikeCodeLine(line))
				{
					if (!line.isEmpty())
					{
						alchHorn = alchHorn || lower.contains("alch horn");
						if (lineOverstock != HealerCodeOverstock.REGULAR)
						{
							overstock = lineOverstock;
						}
						if (lower.startsWith("restock"))
						{
							restockingInstructions = line.replaceFirst("(?i)^restock\\s*:?", "").trim();
						}
						else if (lower.startsWith("expected"))
						{
							applyExpectedTimes(line, expectedTimes);
						}
						else if (!lower.contains("alch horn") && lineOverstock == HealerCodeOverstock.REGULAR)
						{
							notes.add(line);
						}
					}
					continue;
				}

				List<HealerInstruction> instructions = parseCodeLine(line);

				if (!instructions.isEmpty())
				{
					calls.add(new CallCode(callIndex++, instructions, null));
				}
			}

			while (calls.size() < HealerCodeFormatter.CALL_COUNT)
			{
				calls.add(new CallCode(calls.size(), new ArrayList<>(), null));
			}

			WaveCode code = new WaveCode(id, name, wave, builtIn, calls);
			code.setAlchHorn(alchHorn);
			code.setOverstock(overstock);
			code.setRestockingInstructions(restockingInstructions);
			code.setAdditionalNotes(String.join("\n", notes));
			code.setExpectedTimesSeconds(expectedTimes);
			return code;
		}

		while (calls.size() < HealerCodeFormatter.CALL_COUNT)
		{
			calls.add(new CallCode(calls.size(), new ArrayList<>(), null));
		}

		return new WaveCode(id, name, wave, builtIn, calls);
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

	private static boolean isMetadataLine(String lower, HealerCodeOverstock overstock)
	{
		return lower.contains("alch horn")
				|| lower.startsWith("restock")
				|| lower.startsWith("expected")
				|| overstock != HealerCodeOverstock.REGULAR;
	}

	public static HealerInstruction parseInstruction(String rawPart)
	{
		String part = rawPart == null ? "" : rawPart.trim();

		String lower = part.toLowerCase();
		if (part.isEmpty() || "x".equalsIgnoreCase(part) || lower.contains("spam") || lower.contains("yolo"))
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

		return new HealerInstruction(
				count,
				firstInt(AFTER_PATTERN, part),
				firstInt(BEFORE_PATTERN, part),
				firstInt(EXACT_PATTERN, part),
				part
		);
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
		return firstToken.equalsIgnoreCase("x")
				|| firstToken.toLowerCase().contains("spam")
				|| COUNT_PATTERN.matcher(firstToken).find();
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

	private static HealerCodeOverstock overstockFromText(String value)
	{
		String text = value == null ? "" : value.toLowerCase();
		if (text.contains("5x"))
		{
			return HealerCodeOverstock.FIVE_X;
		}
		if (text.contains("4x"))
		{
			return HealerCodeOverstock.FOUR_X;
		}
		if (text.contains("3x"))
		{
			return HealerCodeOverstock.THREE_X;
		}
		if (text.contains("2x"))
		{
			return HealerCodeOverstock.TWO_X;
		}
		if (text.contains("1x"))
		{
			return HealerCodeOverstock.ONE_X;
		}
		return HealerCodeOverstock.REGULAR;
	}

	private static HealerCodeOverstock overstockFromHeader(String value)
	{
		String text = value == null ? "" : value.trim().toLowerCase();
		if (text.startsWith("restock") || !text.contains("os"))
		{
			return HealerCodeOverstock.REGULAR;
		}
		return overstockFromText(text);
	}

	private static void applyExpectedTimes(String line, Map<Integer, Integer> expectedTimes)
	{
		Matcher matcher = EXPECTED_TIME_PATTERN.matcher(line == null ? "" : line);
		while (matcher.find())
		{
			try
			{
				expectedTimes.put(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
			}
			catch (NumberFormatException ignored)
			{
				// Ignore invalid fragments in an otherwise parseable text representation.
			}
		}
	}
}
