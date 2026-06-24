package com.bahealerorder.defender.strategies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class DefenderTimedNotesTest
{
	@Test
	public void parsesSupportedTimedLinesAndPlainLines()
	{
		List<TimedDefenderNote> notes = DefenderTimedNotes.parse(
				"Wave 9 (B9)\n"
						+ "28.2 - if trap broken, drop 1 food\n"
						+ "44.4 - go to 4N1E and drop 2\n"
						+ "1:12 - bring lures to horn\n"
						+ "1:12.6 - final note\n"
						+ "\n"
						+ "repair trap"
		);

		assertEquals(7, notes.size());
		assertFalse(notes.get(0).isTimed());
		assertTrue(notes.get(1).isTimed());
		assertEquals(Integer.valueOf(47), notes.get(1).getTick());
		assertEquals("28.2 - if trap broken, drop 1 food", notes.get(1).getText());
		assertEquals(Integer.valueOf(74), notes.get(2).getTick());
		assertEquals(Integer.valueOf(120), notes.get(3).getTick());
		assertEquals(Integer.valueOf(121), notes.get(4).getTick());
		assertFalse(notes.get(5).isTimed());
		assertFalse(notes.get(6).isTimed());
	}

	@Test
	public void selectsNextUpTimedNoteAndFallsBackToLastTimedNote()
	{
		List<TimedDefenderNote> notes = DefenderTimedNotes.parse(
				"header\n"
						+ "28.2 - first\n"
						+ "44.4 - second\n"
						+ "repair trap"
		);

		assertEquals(1, DefenderTimedNotes.getActiveTimedIndex(notes, 0));
		assertEquals(1, DefenderTimedNotes.getActiveTimedIndex(notes, 47));
		assertEquals(2, DefenderTimedNotes.getActiveTimedIndex(notes, 48));
		assertEquals(-1, DefenderTimedNotes.getActiveTimedIndex(notes, 999));
	}
}
