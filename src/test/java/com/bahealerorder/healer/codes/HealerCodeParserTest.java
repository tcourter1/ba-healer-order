package com.bahealerorder.healer.codes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class HealerCodeParserTest
{
	@Test
	public void parsesWindowInstruction()
	{
		HealerInstruction instruction = HealerCodeParser.parseInstruction("1(33)[51]");

		assertEquals(1, instruction.getTargetFoodCount());
		assertEquals(Integer.valueOf(33), instruction.getAfterSeconds());
		assertEquals(Integer.valueOf(51), instruction.getBeforeSeconds());
	}

	@Test
	public void parsesExactInstruction()
	{
		HealerInstruction instruction = HealerCodeParser.parseInstruction("2{27}");

		assertEquals(2, instruction.getTargetFoodCount());
		assertEquals(Integer.valueOf(27), instruction.getExactSeconds());
	}

	@Test
	public void parsesTimeRangeAsFirstTime()
	{
		HealerInstruction instruction = HealerCodeParser.parseInstruction("6(18-27)");

		assertEquals(6, instruction.getTargetFoodCount());
		assertEquals(Integer.valueOf(18), instruction.getAfterSeconds());
		assertEquals("6(18)", HealerCodeFormatter.formatInstruction(instruction));

		List<HealerInstruction> instructions = HealerCodeParser.parseCodeLine("6(18-27)-2[36]");
		assertEquals(2, instructions.size());
		assertEquals(Integer.valueOf(18), instructions.get(0).getAfterSeconds());
		assertEquals(Integer.valueOf(36), instructions.get(1).getBeforeSeconds());
	}

	@Test
	public void preservesVariableFoodAsAdvancedText()
	{
		HealerInstruction instruction = HealerCodeParser.parseInstruction("1/2(33)");

		assertEquals(1, instruction.getTargetFoodCount());
		assertEquals(Integer.valueOf(33), instruction.getAfterSeconds());
		assertTrue(instruction.isAdvanced());
		assertEquals("1/2(33)", HealerCodeFormatter.formatInstruction(instruction));
	}

	@Test
	public void keepsRestockSplitInSingleCell()
	{
		List<HealerInstruction> instructions = HealerCodeParser.parseCodeLine("6,2-1/2");

		assertEquals(2, instructions.size());
		assertEquals(6, instructions.get(0).getTargetFoodCount());
		assertEquals(2, instructions.get(0).getPostRestockFoodCount());
		assertTrue(instructions.get(0).isAdvanced());
		assertEquals("6,2", HealerCodeFormatter.formatInstruction(instructions.get(0)));
		assertEquals("1/2", HealerCodeFormatter.formatInstruction(instructions.get(1)));
	}

	@Test
	public void treatsSpamAsNoTarget()
	{
		HealerInstruction instruction = HealerCodeParser.parseInstruction("X");

		assertFalse(instruction.hasTarget());
	}

	@Test
	public void preservesNoTargetCodeText()
	{
		WaveCode code = HealerCodeParser.parseWaveCode(null, "No Target", 8, false, "spam-X-0");

		assertFalse(code.getCall(0).getInstruction(1).hasTarget());
		assertEquals("spam", HealerCodeFormatter.formatInstruction(code.getCall(0).getInstruction(1)));
		assertEquals("X", HealerCodeFormatter.formatInstruction(code.getCall(0).getInstruction(2)));
		assertEquals("spam-X-0", code.getSourceText());
	}

	@Test
	public void parsesFormattedMetadataBackIntoStructuredFields()
	{
		WaveCode code = HealerCodeParser.parseWaveCode(
				null,
				"Practice",
				9,
				false,
				"Alch horn, 2x OS\n2(18)-4-1-1\nExpected: #1=42s, #7=48s\nRestock: 4x os + 1 reg\nNotes"
		);

		assertEquals(true, code.isAlchHorn());
		assertEquals(HealerCodeOverstock.TWO_X, code.getOverstock());
		assertEquals(Integer.valueOf(42), code.getExpectedTimeSeconds(1));
		assertEquals(Integer.valueOf(48), code.getExpectedTimeSeconds(7));
		assertEquals("4x os + 1 reg", code.getRestockingInstructions());
		assertEquals("Notes", code.getAdditionalNotes());
	}

	@Test
	public void parsesCommentsIntoAdditionalNotes()
	{
		WaveCode code = HealerCodeParser.parseWaveCode(
				null,
				"Practice",
				6,
				false,
				"1-1 // delay if early\n// backup line\nRestock: 1x OS // after restock note"
		);

		assertEquals("1x OS", code.getRestockingInstructions());
		assertEquals("delay if early\nbackup line\nafter restock note", code.getAdditionalNotes());
	}

	@Test
	public void displayTextStripsCodeNameAndExpectedLines()
	{
		WaveCode code = HealerCodeParser.parseWaveCode(
				null,
				"1x OS",
				5,
				false,
				"Wave 5 - 1x OS\n1x OS\n2-2\nExpected: #5=48s"
		);

		assertEquals("2-2", HealerCodeFormatter.formatDisplay(code));
	}

}
