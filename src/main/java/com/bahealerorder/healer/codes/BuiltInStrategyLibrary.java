package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BuiltInStrategyLibrary
{
	private BuiltInStrategyLibrary()
	{
	}

	public static StrategyStore create()
	{
		StrategyStore store = new StrategyStore();
		List<WaveCode> waveCodes = new ArrayList<>();
		List<RunPreset> runPresets = new ArrayList<>();

		add(waveCodes, 4, "regular", "Reg", 42, "2-5-1(24) //\n0-0-1-7\nExpected: #4=30s");
		add(waveCodes, 4, "1x-os", "1x", 42, "2-5-4 //\n0-0-0-7\nExpected: #4=30s");
		add(waveCodes, 5, "regular", "Reg", 57, "1-3(18)-2-2(27) //\n0-0-0-0-8\nExpected: #5=42s");
		add(waveCodes, 5, "1x-os", "1x", 54, "2-5-2(21)-3(30) //\n0-0-0-0-7\nExpected: #5=36s");
		add(waveCodes, 5, "c5", "C5 6x Alch", 45, "Alch horn, 6x os\n3(22)[43]-7-4(21)-6-8\nExpected: #5=30s\n// If 12s not on Coll:\n// 8-2(21)-4(21)-6-8");
		add(waveCodes, 6, "regular", "Reg", 66, "2/3(18)-4(24)-1-1 //\nRestock 3x os + 1x reg\n0-0-1-1-X-X\nExpected: #5=42s, #6=48s");
		add(waveCodes, 6, "1x-os", "1x", 60, "3(18)-5-2(21)-2 //\nRestock 3x + 1x reg\n0-0-0-1-9-11\nExpected: #5=42s, #6=48s");
		add(waveCodes, 6, "2x-os", "2x", 60, "3(18)-5-3-4 //\nRestock 3x os + 1x reg\n0-0-0-0-10-12\nExpected: #5=42s, #6=48s");
		add(waveCodes, 7, "regular", "Reg", 75, "2(18)-4/5(27)-1-1\n1(33)-1/0-1(33)-1-4/5(48)-1\n0-0-0-0-1/0-5-X\nExpected: #5=42s, #6=48s, #7=60s\nRestock: 13/14 or 1x os");
		add(waveCodes, 7, "2x-os", "2x", 66, "2(27)-6-6-1-0-0-0\n0-0-0-1(39)-1/3-9-11\nExpected: #5=42s, #6=48s, #7=54s\nRestock: 4x os\nIf tofu, and/or if worms with alch, 3(21) on 6 works. 2(27) impossible with late poison on 6, the healer will need 3 food to die by 54.");
		add(waveCodes, 8, "regular", "Reg", 78, "2-4(21)-1/2(27)-1 //\n1-1[42]-1/0-1-3(42)-1/2 //\nRestock\n0-0-0-0-6/5-X\nExpected: #6=48s, #7=60s");
		add(waveCodes, 8, "1x-os", "1x", 78, "1-9-1-1 //\n2-0-1-1(36)-2-1\nRestock 13/13 or 1-2x os //\n0-0-0-0-0-3-10\nExpected: #6=36s, #7=66s");
		add(waveCodes, 8, "2x-os", "2x", 69, "4(24)-9-1-1 //\n0-0-1-2(39)-1-1\nRestock 4x os + 1x reg\n0-0-0-0-3(51)-5(51)[57]-X\nExpected: #6=36s, #7=54s");
		add(waveCodes, 9, "regular", "Reg", 90, "2(18)-4/5(18)-1-1 //\n1[54]-2/3-1(33)-1(42)-1-1-1 //\nRestock\n0-0-0-0-1[69]-2[69]-5[75]-X\nExpected: #7=48s, #8=60s");
		add(waveCodes, 9, "1x-os", "1x", 87, "2-8(21)-1-1 //\n1-0-1(33)-1(45)-2(45)-1-1 //\nRestock 4x os\n0-0-0-0-0-3-5-X\nExpected: #7=48s, #8=66s");
		add(waveCodes, 9, "b9", "B9 1x", 78, "9(27)-1-1-1 //\n0-1-1(42)-2[45](45)-1-1-1\nRestock 4x\n0-0-0-0-5-6-7[42]\nExpected: #7=42s");
		add(waveCodes, 10, "regular", "Reg", 78, "2(21)-4/5(27)-1-1\n1(33)-1/0-1(33)-1-3(51)-2/1\n0-0-0-0-1-3-7\nExpected: #5=42s, #6=48s, #7=60s\nRestock: 10/10 then 1x os after call");
		add(waveCodes, 10, "2x-os", "2x", 69, "3(25)-5(27)-6-1 //\n0-0-0-2-8-9-10\nRestock: 5x os\nExpected: #5=42s, #6=48s, #7=54s");
		add(waveCodes, 6, "dh-spam", "DH Spam", 54, "1x OS\n8-0-4-0\nRestock: 1x OS\n0-0-0-0-0-10\nExpected: #5=30s, #6=42s");
		add(waveCodes, 7, "dh-spam", "DH Spam", 66, "1x OS\n9-0-3-0\nRestock: 1x OS\n0-0-0-0-0-7(51)-5\n0-0-0-0-0-0-4\nExpected: #5=30s, #6=42s, #7=54s");
		add(waveCodes, 8, "dh-spam", "DH Spam", 66, "2x OS\n8-0-4-0\nRestock: 2x OS\n0-0-0-0-0-4-11\n0-0-0-0-0-5-0\nExpected: #6=42s, #7=48s");
		add(waveCodes, 9, "dh-spam", "DH Spam", 78, "1x OS\n10-2(21)-0-0\nRestock: 1x OS\n0-0-0-0-0-0-8(51)-4\n0-0-0-0-0-0-0-7\nExpected: #7=42s, #8=54s");
		add(waveCodes, 10, "dh-spam", "DH Spam", 66, "1x OS\n9-0-3-0\nRestock: 1x OS\n0-0-0-0-0-7(51)-5\n0-0-0-0-0-0-4\nExpected: #5=30s, #6=42s, #7=54s");
		add(waveCodes, 6, "dh-tag", "DH Tag", 54, "0-5-0-3\n0-0-0-2-6(36)-0\nExpected: #5=30s, #6=42s");
		add(waveCodes, 7, "dh-tag", "DH Tag", 66, "0-6-0-2\n0-0-1-1(39)-4(39)-0-2\nExpected: #5=30s, #6=42s, #7=54s");
		add(waveCodes, 8, "dh-tag", "DH Tag", 66, "0-6(18)-0-2\n0-0-0-2(36)-6(36)-0-0\nExpected: #6=42s, #7=48s");
		add(waveCodes, 9, "dh-tag", "DH Tag", 78, "0-5-1-2\n0-0-2(33)-2(39)-3-1-0-0\nRestock: Regular\n0-0-0-0-3[72]-6[72]-0-0\nExpected: #7=42s, #8=54s");
		add(waveCodes, 10, "dh-tag", "DH Tag", 66, "0-6-0-2\n0-0-1-1(39)-4(39)-0-2\nExpected: #5=30s, #6=42s, #7=54s");

		addPreset(runPresets, "beginner", "Beginner", regularPreset());
		addPreset(runPresets, "intermediate", "Intermediate", intermediatePreset());
		addPreset(runPresets, "dh-spam", "DH Spam", dhPreset("dh-spam"));
		addPreset(runPresets, "dh-tag", "DH Tag", dhPreset("dh-tag"));

		store.setWaveCodes(waveCodes);
		store.setRunPresets(runPresets);
		return store;
	}

	private static void add(List<WaveCode> waveCodes, int wave, String key, String name, int expectedWaveEndSeconds, String source)
	{
		WaveCode code = HealerCodeParser.parseWaveCode(waveId(wave, key), name, wave, true, source);
		code.setExpectedWaveEndSeconds(expectedWaveEndSeconds);
		waveCodes.add(code);
	}

	public static String waveId(int wave, String key)
	{
		return "builtin:w" + wave + ":" + key;
	}

	private static void addPreset(List<RunPreset> runPresets, String key, String name, Map<Integer, String> waveCodeIds)
	{
		runPresets.add(new RunPreset(presetId(key), name, true, waveCodeIds));
	}

	private static Map<Integer, String> regularPreset()
	{
		Map<Integer, String> waveCodeIds = new HashMap<>();
		for (int wave = 4; wave <= 10; wave++)
		{
			waveCodeIds.put(wave, waveId(wave, "regular"));
		}
		return waveCodeIds;
	}

	private static Map<Integer, String> intermediatePreset()
	{
		Map<Integer, String> waveCodeIds = regularPreset();
		waveCodeIds.put(5, waveId(5, "1x-os"));
		waveCodeIds.put(6, waveId(6, "1x-os"));
		waveCodeIds.put(8, waveId(8, "1x-os"));
		waveCodeIds.put(9, waveId(9, "1x-os"));
		return waveCodeIds;
	}

	private static Map<Integer, String> dhPreset(String key)
	{
		Map<Integer, String> waveCodeIds = new HashMap<>();
		for (int wave = 6; wave <= 10; wave++)
		{
			waveCodeIds.put(wave, waveId(wave, key));
		}
		return waveCodeIds;
	}

	private static String presetId(String key)
	{
		return "builtin:preset:" + key;
	}
}
