package com.bahealerorder.tilemarkers;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimedStrategyNotes
{
	private static final Color ACTIVE_NOTE_COLOR = new Color(255, 220, 90);
	private static final Pattern TIME_PREFIX = Pattern.compile("^\\s*((?:\\d+:)?\\d+(?:\\.\\d+)?)(?:\\s*[-:]\\s+|\\s+).*$");

	private TimedStrategyNotes()
	{
	}

	public static List<TimedStrategyNote> parse(String notes)
	{
		List<TimedStrategyNote> parsed = new ArrayList<>();

		if (notes == null || notes.isEmpty())
		{
			return parsed;
		}

		for (String line : notes.split("\\r?\\n", -1))
		{
			Matcher matcher = TIME_PREFIX.matcher(line);
			Integer tick = matcher.matches() ? parseTick(matcher.group(1)) : null;
			parsed.add(new TimedStrategyNote(line, tick));
		}

		return parsed;
	}

	public static int getActiveTimedIndex(List<TimedStrategyNote> notes, int currentWaveTick)
	{
		if (notes == null || notes.isEmpty())
		{
			return -1;
		}

		for (int i = 0; i < notes.size(); i++)
		{
			TimedStrategyNote note = notes.get(i);
			if (note.isTimed() && note.getTick() >= currentWaveTick)
			{
				return i;
			}
		}

		return -1;
	}

	public static Color colorFor(
			TimedStrategyNote note,
			int index,
			int activeIndex,
			int currentWaveTick,
			Color defaultColor,
			Color passedColor)
	{
		if (index == activeIndex)
		{
			return ACTIVE_NOTE_COLOR;
		}

		if (note != null && note.isTimed() && note.getTick() < currentWaveTick)
		{
			return passedColor;
		}

		return defaultColor;
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
