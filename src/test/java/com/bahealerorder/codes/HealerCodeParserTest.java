package com.bahealerorder.codes;

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
	public void treatsSpamAsNoTarget()
	{
		HealerInstruction instruction = HealerCodeParser.parseInstruction("X");

		assertFalse(instruction.hasTarget());
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
}
