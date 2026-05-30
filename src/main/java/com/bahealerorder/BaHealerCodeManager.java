package com.bahealerorder;

import com.bahealerorder.codes.BuiltInStrategyLibrary;
import com.bahealerorder.codes.CallCode;
import com.bahealerorder.codes.CodeDisplayState;
import com.bahealerorder.codes.FeedEvent;
import com.bahealerorder.codes.HealerCodeParser;
import com.bahealerorder.codes.HealerCodeStatus;
import com.bahealerorder.codes.HealerInstruction;
import com.bahealerorder.codes.RunPreset;
import com.bahealerorder.codes.StrategyStore;
import com.bahealerorder.codes.WaveCode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
public class BaHealerCodeManager
{
	private static final String CONFIG_GROUP = "bahealerorder";
	private static final String STRATEGY_STORE_KEY = "strategyStore";

	private final ConfigManager configManager;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private final StrategyStore builtIns = BuiltInStrategyLibrary.create();

	private StrategyStore userStore = new StrategyStore();

	@Inject
	public BaHealerCodeManager(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	public void load()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, STRATEGY_STORE_KEY);

		if (json == null || json.trim().isEmpty())
		{
			userStore = new StrategyStore();
			save();
			return;
		}

		try
		{
			userStore = gson.fromJson(json, StrategyStore.class);
		}
		catch (RuntimeException ex)
		{
			userStore = new StrategyStore();
		}

		if (userStore == null)
		{
			userStore = new StrategyStore();
		}

	}

	public void save()
	{
		configManager.setConfiguration(CONFIG_GROUP, STRATEGY_STORE_KEY, gson.toJson(userStore));
	}

	public List<RunPreset> getRunPresets()
	{
		List<RunPreset> presets = new ArrayList<>();
		presets.addAll(userStore.getRunPresets());
		presets.sort(Comparator.comparing(RunPreset::getName, String.CASE_INSENSITIVE_ORDER));
		return presets;
	}

	public List<WaveCode> getWaveCodesForWave(int wave)
	{
		List<WaveCode> waveCodes = new ArrayList<>();

		for (WaveCode code : getWaveCodes())
		{
			if (code.getWave() == wave)
			{
				waveCodes.add(code);
			}
		}

		waveCodes.sort(Comparator.comparing(WaveCode::isBuiltIn).reversed().thenComparing(WaveCode::getName, String.CASE_INSENSITIVE_ORDER));
		return waveCodes;
	}

	public List<WaveCode> getWaveCodes()
	{
		List<WaveCode> waveCodes = new ArrayList<>();

		for (WaveCode builtIn : builtIns.getWaveCodes())
		{
			WaveCode override = findStoredUserWaveCode(builtIn.getId());
			waveCodes.add(override == null ? builtIn : override);
		}

		for (WaveCode userCode : userStore.getWaveCodes())
		{
			if (findBuiltInWaveCode(userCode.getId()) == null)
			{
				waveCodes.add(userCode);
			}
		}

		return waveCodes;
	}

	public String getActiveRunPresetId()
	{
		RunPreset preset = findRunPreset(userStore.getActiveRunPresetId());

		if (preset == null)
		{
			preset = findMatchingRunPreset();
		}

		return preset == null ? null : preset.getId();
	}

	public void setActiveRunPresetId(String activeRunPresetId)
	{
		userStore.setActiveRunPresetId(activeRunPresetId);
		save();
	}

	public RunPreset getActiveRunPreset()
	{
		return findRunPreset(userStore.getActiveRunPresetId());
	}

	public RunPreset findMatchingRunPreset()
	{
		Map<Integer, String> activeWaveCodes = userStore.getActiveWaveCodeIds();

		if (activeWaveCodes.isEmpty())
		{
			return null;
		}

		for (RunPreset preset : userStore.getRunPresets())
		{
			if (preset.getWaveCodeIds().equals(activeWaveCodes))
			{
				return preset;
			}
		}

		return null;
	}

	public RunPreset findRunPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (RunPreset preset : getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	public WaveCode findWaveCode(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (WaveCode code : getWaveCodes())
		{
			if (id.equals(code.getId()))
			{
				return code;
			}
		}

		return null;
	}

	public WaveCode findBuiltInWaveCode(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (WaveCode code : builtIns.getWaveCodes())
		{
			if (id.equals(code.getId()))
			{
				return code;
			}
		}

		return null;
	}

	public WaveCode getActiveWaveCode(int wave)
	{
		WaveCode selectedWaveCode = findWaveCode(getActiveWaveCodeId(wave));

		if (selectedWaveCode != null)
		{
			return selectedWaveCode;
		}

		RunPreset preset = getActiveRunPreset();

		if (preset != null)
		{
			return findWaveCode(preset.getWaveCodeId(wave));
		}

		return null;
	}

	public String getActiveWaveCodeId(int wave)
	{
		return userStore.getActiveWaveCodeIds().get(wave);
	}

	public Map<Integer, String> getActiveWaveCodeIds()
	{
		return new HashMap<>(userStore.getActiveWaveCodeIds());
	}

	public void setActiveWaveCodeId(int wave, String waveCodeId)
	{
		if (waveCodeId == null || waveCodeId.trim().isEmpty())
		{
			userStore.getActiveWaveCodeIds().remove(wave);
		}
		else
		{
			userStore.getActiveWaveCodeIds().put(wave, waveCodeId);
		}

		save();
	}

	public void applyRunPreset(String runPresetId)
	{
		userStore.setActiveRunPresetId(runPresetId);
		RunPreset preset = findRunPreset(runPresetId);

		if (preset != null)
		{
			userStore.setActiveWaveCodeIds(preset.getWaveCodeIds());
		}

		save();
	}

	public void clearActiveSelections()
	{
		userStore.setActiveRunPresetId(null);
		userStore.getActiveWaveCodeIds().clear();
		save();
	}

	public int getExpectedFoodForOrder(int wave, int healerOrder, int currentCallIndex)
	{
		WaveCode waveCode = getActiveWaveCode(wave);

		if (waveCode == null)
		{
			return 0;
		}

		int expected = 0;

		for (CallCode call : waveCode.getCalls())
		{
			if (call.getCallIndex() > currentCallIndex)
			{
				continue;
			}

			HealerInstruction instruction = call.getInstruction(healerOrder);

			if (instruction != null && instruction.hasTarget())
			{
				expected += instruction.getTargetFoodCount();
			}
		}

		return expected;
	}

	public HealerCodeStatus getCurrentStatus(int wave, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		WaveCode waveCode = getActiveWaveCode(wave);

		if (waveCode == null)
		{
			return null;
		}

		CallCode call = waveCode.getCall(currentCallIndex);

		if (call == null)
		{
			return null;
		}

		HealerInstruction instruction = call.getInstruction(healerOrder);

		if (instruction == null || !instruction.hasTarget())
		{
			return null;
		}

		int foodThisCall = countFoodForCall(healerOrder, currentCallIndex, feedEvents);
		int lastFoodElapsed = lastFoodElapsed(healerOrder, currentCallIndex, feedEvents);
		return new HealerCodeStatus(instruction, getState(instruction, foodThisCall, lastFoodElapsed), foodThisCall, lastFoodElapsed);
	}

	public HealerCodeStatus getPreviousStatus(int wave, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		WaveCode waveCode = getActiveWaveCode(wave);

		if (waveCode == null)
		{
			return null;
		}

		int previousCallIndex = currentCallIndex - 1;

		if (previousCallIndex < 0)
		{
			return null;
		}

		CallCode call = waveCode.getCall(previousCallIndex);

		if (call == null)
		{
			return null;
		}

		HealerInstruction instruction = call.getInstruction(healerOrder);

		if (instruction == null || !instruction.hasTarget())
		{
			return null;
		}

		int foodThisCall = countFoodForCall(healerOrder, previousCallIndex, feedEvents);
		int lastFoodElapsed = lastFoodElapsed(healerOrder, previousCallIndex, feedEvents);
		return new HealerCodeStatus(instruction, CodeDisplayState.PREVIOUS, foodThisCall, lastFoodElapsed);
	}

	public HealerCodeStatus getMostRecentPreviousStatus(int wave, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		WaveCode waveCode = getActiveWaveCode(wave);

		if (waveCode == null)
		{
			return null;
		}

		for (int callIndex = currentCallIndex - 1; callIndex >= 0; callIndex--)
		{
			CallCode call = waveCode.getCall(callIndex);

			if (call == null)
			{
				continue;
			}

			HealerInstruction instruction = call.getInstruction(healerOrder);

			if (instruction == null || !instruction.hasTarget())
			{
				continue;
			}

			int foodThisCall = countFoodForCall(healerOrder, callIndex, feedEvents);
			int lastFoodElapsed = lastFoodElapsed(healerOrder, callIndex, feedEvents);
			return new HealerCodeStatus(instruction, CodeDisplayState.PREVIOUS, foodThisCall, lastFoodElapsed);
		}

		return null;
	}

	public HealerCodeStatus getDisplayStatus(int wave, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		WaveCode waveCode = getActiveWaveCode(wave);

		if (waveCode == null)
		{
			return null;
		}

		List<InstructionProgress> progresses = new ArrayList<>();

		for (CallCode call : waveCode.getCalls())
		{
			if (call.getCallIndex() > currentCallIndex)
			{
				continue;
			}

			HealerInstruction instruction = call.getInstruction(healerOrder);

			if (instruction != null && instruction.hasTarget())
			{
				progresses.add(new InstructionProgress(call.getCallIndex(), instruction));
			}
		}

		if (progresses.isEmpty())
		{
			return null;
		}

		List<FeedEvent> events = new ArrayList<>();

		for (FeedEvent event : safeEvents(feedEvents))
		{
			if (event.getHealerOrder() == healerOrder)
			{
				events.add(event);
			}
		}

		events.sort(Comparator.comparingInt(FeedEvent::getElapsedSeconds));

		for (FeedEvent event : events)
		{
			for (InstructionProgress progress : progresses)
			{
				if (event.getCallIndex() >= progress.callIndex
						&& progress.acceptsMoreFood())
				{
					progress.foodFed++;
					progress.lastFoodElapsed = Math.max(progress.lastFoodElapsed, event.getElapsedSeconds());
					break;
				}
			}
		}

		InstructionProgress currentProgress = null;
		InstructionProgress mostRecentProgress = null;

		for (InstructionProgress progress : progresses)
		{
			mostRecentProgress = progress;

			if (progress.callIndex == currentCallIndex)
			{
				currentProgress = progress;
			}

			CodeDisplayState state = getState(progress.instruction, progress.foodFed, progress.lastFoodElapsed);

			if (progress.callIndex < currentCallIndex && state != CodeDisplayState.COMPLETE)
			{
				return progress.status(CodeDisplayState.IN_PROGRESS);
			}
		}

		if (currentProgress != null)
		{
			return currentProgress.status(getState(currentProgress.instruction, currentProgress.foodFed, currentProgress.lastFoodElapsed));
		}

		return mostRecentProgress == null ? null : mostRecentProgress.status(CodeDisplayState.PREVIOUS);
	}

	public RunPreset createUserPreset(String name, Map<Integer, String> waveCodeIds)
	{
		RunPreset preset = new RunPreset(userId("preset", name), name, false, new HashMap<>(waveCodeIds));
		List<RunPreset> presets = new ArrayList<>(userStore.getRunPresets());
		presets.add(preset);
		userStore.setRunPresets(presets);
		userStore.setActiveRunPresetId(preset.getId());
		save();
		return preset;
	}

	public RunPreset createUserPresetFromActive(String name)
	{
		return createUserPreset(name, userStore.getActiveWaveCodeIds());
	}

	public boolean deleteUserPreset(String id)
	{
		if (id == null)
		{
			return false;
		}

		boolean removed = false;
		List<RunPreset> presets = new ArrayList<>();

		for (RunPreset preset : userStore.getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				removed = true;
			}
			else
			{
				presets.add(preset);
			}
		}

		if (!removed)
		{
			return false;
		}

		userStore.setRunPresets(presets);

		if (id.equals(userStore.getActiveRunPresetId()))
		{
			userStore.setActiveRunPresetId(null);
			userStore.getActiveWaveCodeIds().clear();
		}

		save();
		return true;
	}

	public boolean updateUserPreset(String id, String name, Map<Integer, String> waveCodeIds)
	{
		if (id == null || name == null || name.trim().isEmpty())
		{
			return false;
		}

		for (RunPreset preset : userStore.getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				preset.setName(name);
				preset.setWaveCodeIds(new HashMap<>(waveCodeIds));
				save();
				return true;
			}
		}

		return false;
	}

	public WaveCode createUserWaveCode(int wave, String name, String sourceText)
	{
		WaveCode code = HealerCodeParser.parseWaveCode(userId("wave", wave + "-" + name), name, wave, false, sourceText);
		List<WaveCode> waveCodes = new ArrayList<>(userStore.getWaveCodes());
		waveCodes.add(code);
		userStore.setWaveCodes(waveCodes);
		save();
		return code;
	}

	public List<WaveCode> getUserWaveCodesForWave(int wave)
	{
		List<WaveCode> waveCodes = new ArrayList<>();

		for (WaveCode code : userStore.getWaveCodes())
		{
			if (code.getWave() == wave)
			{
				waveCodes.add(code);
			}
		}

		waveCodes.sort(Comparator.comparing(WaveCode::getName, String.CASE_INSENSITIVE_ORDER));
		return waveCodes;
	}

	public boolean updateUserWaveCode(String id, int wave, String name, String sourceText)
	{
		if (id == null || name == null || name.trim().isEmpty())
		{
			return false;
		}

		WaveCode updated = HealerCodeParser.parseWaveCode(id, name, wave, false, sourceText);

		for (int i = 0; i < userStore.getWaveCodes().size(); i++)
		{
			if (id.equals(userStore.getWaveCodes().get(i).getId()))
			{
				List<WaveCode> waveCodes = new ArrayList<>(userStore.getWaveCodes());
				waveCodes.set(i, updated);
				userStore.setWaveCodes(waveCodes);
				save();
				return true;
			}
		}

		return false;
	}

	public boolean updateBuiltInWaveCode(String id, String sourceText)
	{
		WaveCode builtIn = findBuiltInWaveCode(id);

		if (builtIn == null || sourceText == null || sourceText.trim().isEmpty())
		{
			return false;
		}

		WaveCode updated = HealerCodeParser.parseWaveCode(
				builtIn.getId(),
				builtIn.getName(),
				builtIn.getWave(),
				true,
				sourceText
		);
		List<WaveCode> waveCodes = new ArrayList<>(userStore.getWaveCodes());

		for (int i = 0; i < waveCodes.size(); i++)
		{
			if (id.equals(waveCodes.get(i).getId()))
			{
				waveCodes.set(i, updated);
				userStore.setWaveCodes(waveCodes);
				save();
				return true;
			}
		}

		waveCodes.add(updated);
		userStore.setWaveCodes(waveCodes);
		save();
		return true;
	}

	public boolean resetBuiltInWaveCode(String id)
	{
		if (findBuiltInWaveCode(id) == null)
		{
			return false;
		}

		boolean removed = false;
		List<WaveCode> waveCodes = new ArrayList<>();

		for (WaveCode code : userStore.getWaveCodes())
		{
			if (id.equals(code.getId()))
			{
				removed = true;
			}
			else
			{
				waveCodes.add(code);
			}
		}

		if (!removed)
		{
			return false;
		}

		userStore.setWaveCodes(waveCodes);
		save();
		return true;
	}

	public boolean deleteUserWaveCode(String id)
	{
		if (id == null)
		{
			return false;
		}

		boolean removed = false;
		List<WaveCode> waveCodes = new ArrayList<>();

		for (WaveCode code : userStore.getWaveCodes())
		{
			if (id.equals(code.getId()))
			{
				removed = true;
			}
			else
			{
				waveCodes.add(code);
			}
		}

		if (!removed)
		{
			return false;
		}

		userStore.setWaveCodes(waveCodes);
		userStore.getActiveWaveCodeIds().values().removeIf(id::equals);

		for (RunPreset preset : userStore.getRunPresets())
		{
			preset.getWaveCodeIds().values().removeIf(id::equals);
		}

		save();
		return true;
	}

	private static CodeDisplayState getState(HealerInstruction instruction, int foodFed, int lastFoodElapsed)
	{
		if (foodFed <= 0)
		{
			return CodeDisplayState.NOT_STARTED;
		}

		if (foodFed < instruction.getTargetFoodCount())
		{
			return CodeDisplayState.IN_PROGRESS;
		}

		if (instruction.getAfterSeconds() != null && lastFoodElapsed < instruction.getAfterSeconds())
		{
			return CodeDisplayState.IN_PROGRESS;
		}

		if (instruction.getBeforeSeconds() != null && lastFoodElapsed >= instruction.getBeforeSeconds())
		{
			return CodeDisplayState.IN_PROGRESS;
		}

		return CodeDisplayState.COMPLETE;
	}

	private int countFoodForCall(int healerOrder, int callIndex, List<FeedEvent> feedEvents)
	{
		int count = 0;

		for (FeedEvent event : safeEvents(feedEvents))
		{
			if (event.getHealerOrder() == healerOrder
					&& event.getCallIndex() == callIndex)
			{
				count++;
			}
		}

		return count;
	}

	private int lastFoodElapsed(int healerOrder, int callIndex, List<FeedEvent> feedEvents)
	{
		int last = -1;

		for (FeedEvent event : safeEvents(feedEvents))
		{
			if (event.getHealerOrder() == healerOrder
					&& event.getCallIndex() == callIndex)
			{
				last = Math.max(last, event.getElapsedSeconds());
			}
		}

		return last;
	}

	private List<FeedEvent> safeEvents(List<FeedEvent> feedEvents)
	{
		return feedEvents == null ? new ArrayList<>() : feedEvents;
	}

	private static class InstructionProgress
	{
		private final int callIndex;
		private final HealerInstruction instruction;
		private int foodFed;
		private int lastFoodElapsed = -1;

		private InstructionProgress(int callIndex, HealerInstruction instruction)
		{
			this.callIndex = callIndex;
			this.instruction = instruction;
		}

		private HealerCodeStatus status(CodeDisplayState state)
		{
			return new HealerCodeStatus(instruction, state, foodFed, lastFoodElapsed);
		}

		private boolean acceptsMoreFood()
		{
			if (foodFed < instruction.getTargetFoodCount())
			{
				return true;
			}

			return getState(instruction, foodFed, lastFoodElapsed) != CodeDisplayState.COMPLETE;
		}
	}

	private WaveCode findStoredUserWaveCode(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (WaveCode code : userStore.getWaveCodes())
		{
			if (id.equals(code.getId()))
			{
				return code;
			}
		}

		return null;
	}

	private String userId(String type, String name)
	{
		String cleanName = name == null ? "code" : name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return "user:" + type + ":" + cleanName + ":" + System.currentTimeMillis();
	}
}
