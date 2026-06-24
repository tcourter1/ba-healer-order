package com.bahealerorder.defender.strategies;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DefenderTimedNotes
{
	private static final Pattern TIME_PREFIX = Pattern.compile("^\\s*((?:\\d+:)?\\d+(?:\\.\\d+)?)(?:\\s*[-:]\\s+|\\s+).*$");

	private DefenderTimedNotes()
	{
	}

	public static List<TimedDefenderNote> parse(String notes)
	{
		List<TimedDefenderNote> parsed = new ArrayList<>();

		if (notes == null || notes.isEmpty())
		{
			return parsed;
		}

		for (String line : notes.split("\\r?\\n", -1))
		{
			Matcher matcher = TIME_PREFIX.matcher(line);
			Integer tick = matcher.matches() ? parseTick(matcher.group(1)) : null;

			if (tick == null)
			{
				parsed.add(new TimedDefenderNote(line, null));
				continue;
			}

			parsed.add(new TimedDefenderNote(line, tick));
		}

		return parsed;
	}

	public static int getActiveTimedIndex(List<TimedDefenderNote> notes, int currentWaveTick)
	{
		if (notes == null || notes.isEmpty())
		{
			return -1;
		}

		for (int i = 0; i < notes.size(); i++)
		{
			TimedDefenderNote note = notes.get(i);
			if (!note.isTimed())
			{
				continue;
			}

			if (note.getTick() >= currentWaveTick)
			{
				return i;
			}
		}

		return -1;
	}

	private static Integer parseTick(String timeText)
	{
		try
		{
			double seconds;

			if (timeText.contains(":"))
			{
				String[] parts = timeText.split(":", 2);
				seconds = Integer.parseInt(parts[0]) * 60d + Double.parseDouble(parts[1]);
			}
			else
			{
				seconds = Double.parseDouble(timeText);
			}

			return (int) Math.round(seconds / 0.6d);
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}
}
