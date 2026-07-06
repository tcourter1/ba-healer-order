package com.bahealerorder.healer.codes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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
	public void loadsBuiltInWaveNineOneTimesOverstock()
	{
		StrategyStore store = BuiltInStrategyLibrary.create();
		WaveCode waveCode = null;

		for (WaveCode code : store.getWaveCodes())
		{
			if ("builtin:w9:1x-os".equals(code.getId()))
			{
				waveCode = code;
				break;
			}
		}

		assertNotNull(waveCode);
		List<HealerInstruction> firstCall = waveCode.getCall(0).getHealerInstructions();
		assertEquals(2, firstCall.get(0).getTargetFoodCount());
		assertEquals(8, firstCall.get(1).getTargetFoodCount());
		assertEquals(Integer.valueOf(21), firstCall.get(1).getAfterSeconds());
	}

	@Test
	public void convertsC5MetadataIntoStructuredFields()
	{
		StrategyStore store = BuiltInStrategyLibrary.create();
		WaveCode waveCode = null;

		for (WaveCode code : store.getWaveCodes())
		{
			if ("builtin:w5:c5".equals(code.getId()))
			{
				waveCode = code;
				break;
			}
		}

		assertNotNull(waveCode);
		assertNotNull(waveCode.getCall(0));
		assertEquals(3, waveCode.getCalls().size());
		assertEquals(true, waveCode.isAlchHorn());
		assertEquals("Alch horn\n3(22)[43]-7-4(21)-6-8", waveCode.getSourceText());
	}

	@Test
	public void usesNameForOverstockInsteadOfRestockText()
	{
		StrategyStore store = BuiltInStrategyLibrary.create();
		WaveCode regular = findCode(store, "builtin:w6:regular");
		WaveCode oneTimes = findCode(store, "builtin:w6:1x-os");
		WaveCode twoTimes = findCode(store, "builtin:w6:2x-os");

		assertNotNull(regular);
		assertNotNull(oneTimes);
		assertNotNull(twoTimes);
		assertEquals(HealerCodeOverstock.REGULAR, regular.getOverstock());
		assertEquals(HealerCodeOverstock.ONE_X, oneTimes.getOverstock());
		assertEquals(HealerCodeOverstock.TWO_X, twoTimes.getOverstock());
		assertEquals("3x os + 1x reg", regular.getRestockingInstructions());
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

	private static WaveCode findCode(StrategyStore store, String id)
	{
		for (WaveCode code : store.getWaveCodes())
		{
			if (id.equals(code.getId()))
			{
				return code;
			}
		}

		return null;
	}
}
