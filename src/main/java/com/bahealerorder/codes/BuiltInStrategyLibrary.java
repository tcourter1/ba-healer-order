package com.bahealerorder.codes;

import java.util.ArrayList;
import java.util.List;

public final class BuiltInStrategyLibrary
{
	private BuiltInStrategyLibrary()
	{
	}

	public static StrategyStore create()
	{
		StrategyStore store = new StrategyStore();
		List<WaveCode> waveCodes = new ArrayList<>();

		add(waveCodes, 4, "regular", "Regular", "2-4-2(27) //\n0-0-0-8");
		add(waveCodes, 4, "1x-os", "1x OS", "2-5-4 //\n0-0-0-7");
		add(waveCodes, 5, "regular", "Regular", "1-3(18)-2-2(27) //\n0-0-0-0-8");
		add(waveCodes, 5, "1x-os", "1x OS", "2-5-2(21)-3(30) //\n0-0-0-0-7");
		add(waveCodes, 5, "c5", "C5", "Alch horn, 6x os\n3(22)[43]-7-4(21)-6-8\n// If 12s not on Coll:\n// 8-2(21)-4(21)-6-8");
		add(waveCodes, 6, "regular", "Regular", "2/3(18)-4(24)-1-1 //\nRestock 3x os + 1x reg\n0-0-1-1-X-X");
		add(waveCodes, 6, "1x-os", "1x OS", "3(18)-5-2(21)-2 //\nRestock 3x + 1x reg\n0-0-0-1-9-11");
		add(waveCodes, 6, "2x-os", "2x OS", "3(18)-5-3-4 //\nRestock 3x os + 1x reg\n0-0-10-12");
		add(waveCodes, 7, "regular", "Regular", "2-5(27)-1-1 //\n1(33)-0-1(33)-1-4-1\nRestock 13/13 or 1x os //\n1-3-7");
		add(waveCodes, 7, "2x-os", "2x OS", "2(27)-6-6-1 //\nRestock 4x os\n0-0-0-1(39)-2-9-11");
		add(waveCodes, 8, "regular", "Regular", "2-4(21)-2(27)-1 //\n1-1[42]-1/0-1-3(42)-1/2 //\nRestock\n0-0-0-0-6/5-X");
		add(waveCodes, 8, "1x-os", "1x OS", "1-9-1-1 //\n2-0-1-1(36)-2-1\nRestock 13/13 or 1-2x os //\n0-0-3-10");
		add(waveCodes, 8, "2x-os", "2x OS", "4(24)-9-1-1 //\n0-0-1-2(39)-1-1\nRestock 4x os + 1x reg\n0-0-3(51)-5(51)[57]-X");
		add(waveCodes, 9, "regular", "Regular", "2(18)-4/5(18)-1-1 //\n1[54]-2/3-1(33)-1(42)-1-1-1 //\nRestock\n0-0-0-0-1[69]-2[69]-5[75]-X");
		add(waveCodes, 9, "1x-os", "1x OS", "2-8(21)-1-1 //\n1-0-1(33)-1(45)-2(45)-1-1 //\nRestock 4x os\n0-0-3-5-X");
		add(waveCodes, 9, "b9", "B9 (1x OS)", "9(27)-1-1-1 //\n0-1-1(42)-2[45](45)-1-1-1\nRestock 4x\n0-0-5-6-7[42]");
		add(waveCodes, 10, "regular", "Regular", "2-4/5(27)-1-1 //\n1(33)-1/0-1(33)-1-4(51)-1\nRestock 13/13 or 2x os //\n1-3-7");
		add(waveCodes, 10, "2x-os", "2x OS", "3(25)-5(27)-6-1 //\n0-0-0-2 \nRestock 5x os\n0-8-8-10");

		store.setWaveCodes(waveCodes);
		return store;
	}

	private static void add(List<WaveCode> waveCodes, int wave, String key, String name, String source)
	{
		waveCodes.add(HealerCodeParser.parseWaveCode(waveId(wave, key), name, wave, true, source));
	}

	public static String waveId(int wave, String key)
	{
		return "builtin:w" + wave + ":" + key;
	}
}
