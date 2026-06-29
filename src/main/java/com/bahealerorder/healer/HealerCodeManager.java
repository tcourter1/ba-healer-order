package com.bahealerorder.healer;

import com.bahealerorder.common.strategies.AbstractWaveStrategyManager;
import com.bahealerorder.healer.codes.BuiltInStrategyLibrary;
import com.bahealerorder.healer.codes.CallCode;
import com.bahealerorder.healer.codes.CodeDisplayState;
import com.bahealerorder.healer.codes.FeedEvent;
import com.bahealerorder.healer.codes.HealerCodeParser;
import com.bahealerorder.healer.codes.HealerCodeStatus;
import com.bahealerorder.healer.codes.HealerInstruction;
import com.bahealerorder.healer.codes.RunPreset;
import com.bahealerorder.healer.codes.RunPresetExport;
import com.bahealerorder.healer.codes.StrategyStore;
import com.bahealerorder.healer.codes.WaveCode;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
public class HealerCodeManager extends AbstractWaveStrategyManager<RunPreset, WaveCode, StrategyStore, RunPresetExport>
{
	private static final String CONFIG_GROUP = "bahealerorder";
	private static final String STRATEGY_STORE_KEY = "strategyStore";

	@Inject
	public HealerCodeManager(ConfigManager configManager, Gson gson)
	{
		super(configManager, gson, CONFIG_GROUP, STRATEGY_STORE_KEY, "", BuiltInStrategyLibrary.create());
	}

	HealerCodeManager(StrategyStore userStore, Gson gson)
	{
		super(null, gson, CONFIG_GROUP, STRATEGY_STORE_KEY, "", BuiltInStrategyLibrary.create());
		setUserStore(userStore);
	}

	@Override
	protected StrategyStore newStore()
	{
		return new StrategyStore();
	}

	@Override
	protected WaveCode copyStrategy(WaveCode strategy, String id, boolean builtIn)
	{
		return HealerCodeParser.parseWaveCode(id, strategy.getName(), strategy.getWave(), builtIn, strategy.getSourceText());
	}

	@Override
	protected RunPreset createRunPreset(String id, String name, boolean builtIn, Map<Integer, String> waveStrategyIds)
	{
		return new RunPreset(id, name, builtIn, waveStrategyIds);
	}

	@Override
	protected RunPresetExport createExport(RunPreset preset, List<WaveCode> strategies)
	{
		return new RunPresetExport(preset, strategies);
	}

	@Override
	protected Class<RunPresetExport> exportType()
	{
		return RunPresetExport.class;
	}

	@Override
	protected RunPreset getExportPreset(RunPresetExport export)
	{
		return export.getPreset();
	}

	@Override
	protected List<WaveCode> getExportStrategies(RunPresetExport export)
	{
		return export.getWaveCodes();
	}

	public List<WaveCode> getWaveCodesForWave(int wave)
	{
		return getWaveStrategiesForWave(wave);
	}

	public List<WaveCode> getWaveCodes()
	{
		return getWaveStrategies();
	}

	public RunPreset getActiveRunPreset()
	{
		return findRunPreset(getActiveRunPresetId());
	}

	public WaveCode findWaveCode(String id)
	{
		return findWaveStrategy(id);
	}

	public WaveCode findBuiltInWaveCode(String id)
	{
		return findBuiltInWaveStrategy(id);
	}

	public WaveCode getActiveWaveCode(int wave)
	{
		return getActiveWaveStrategy(wave);
	}

	public String getActiveWaveCodeId(int wave)
	{
		return getActiveWaveStrategyId(wave);
	}

	public Map<Integer, String> getActiveWaveCodeIds()
	{
		return getActiveWaveStrategyIds();
	}

	public void setActiveWaveCodeId(int wave, String waveCodeId)
	{
		setActiveWaveStrategyId(wave, waveCodeId);
	}

	public WaveCode createUserWaveCode(int wave, String name, String sourceText)
	{
		return createUserWaveStrategy(HealerCodeParser.parseWaveCode(null, name, wave, false, sourceText));
	}

	public boolean updateUserWaveCode(String id, int wave, String name, String sourceText)
	{
		return updateUserWaveStrategy(id, HealerCodeParser.parseWaveCode(id, name, wave, false, sourceText));
	}

	public boolean updateBuiltInWaveCode(String id, String sourceText)
	{
		WaveCode builtIn = findBuiltInWaveCode(id);

		if (builtIn == null || isBlank(sourceText))
		{
			return false;
		}

		return updateBuiltInWaveStrategy(id, HealerCodeParser.parseWaveCode(
				builtIn.getId(),
				builtIn.getName(),
				builtIn.getWave(),
				true,
				sourceText
		));
	}

	public boolean resetBuiltInWaveCode(String id)
	{
		return resetBuiltInWaveStrategy(id);
	}

	public boolean deleteUserWaveCode(String id)
	{
		return deleteUserWaveStrategy(id);
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

	public HealerCodeStatus getDisplayStatus(int wave, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		List<InstructionProgress> progresses = getInstructionProgresses(wave, healerOrder, currentCallIndex, feedEvents);

		InstructionProgress currentProgress = null;
		InstructionProgress mostRecentProgress = null;

		for (InstructionProgress progress : progresses)
		{
			if (!progress.hasTarget())
			{
				continue;
			}

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

	public HealerCodeStatus getPanelStatusForCall(int wave, int healerOrder, int currentCallIndex, int panelCallIndex, List<FeedEvent> feedEvents)
	{
		if (panelCallIndex > currentCallIndex)
		{
			WaveCode waveCode = getActiveWaveCode(wave);
			CallCode call = waveCode == null ? null : waveCode.getCall(panelCallIndex);
			HealerInstruction instruction = call == null ? null : call.getInstruction(healerOrder);
			return instruction != null && instruction.hasTarget()
					? new HealerCodeStatus(instruction, CodeDisplayState.NOT_STARTED, 0, -1)
					: null;
		}

		for (InstructionProgress progress : getInstructionProgresses(wave, healerOrder, currentCallIndex, feedEvents))
		{
			if (progress.callIndex == panelCallIndex)
			{
				return progress.hasTarget() ? progress.status(getState(progress.instruction, progress.foodFed, progress.lastFoodElapsed)) : null;
			}
		}

		return null;
	}

	public int getPanelFoodCountForCall(int wave, int healerOrder, int currentCallIndex, int panelCallIndex, List<FeedEvent> feedEvents)
	{
		for (InstructionProgress progress : getInstructionProgresses(wave, healerOrder, currentCallIndex, feedEvents))
		{
			if (progress.callIndex == panelCallIndex)
			{
				return progress.foodFed;
			}
		}

		return 0;
	}

	private List<InstructionProgress> getInstructionProgresses(int wave, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		WaveCode waveCode = getActiveWaveCode(wave);

		if (waveCode == null)
		{
			return new ArrayList<>();
		}

		List<InstructionProgress> progresses = new ArrayList<>();

		for (int callIndex = 0; callIndex <= currentCallIndex; callIndex++)
		{
			CallCode call = waveCode.getCall(callIndex);
			HealerInstruction instruction = call == null ? null : call.getInstruction(healerOrder);
			progresses.add(new InstructionProgress(callIndex, instruction));
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
			InstructionProgress fallbackProgress = null;

			for (InstructionProgress progress : progresses)
			{
				if (progress.callIndex == event.getCallIndex())
				{
					fallbackProgress = progress;
				}

				if (event.getCallIndex() >= progress.callIndex
						&& progress.acceptsMoreFood())
				{
					progress.addFood(event.getElapsedSeconds());
					fallbackProgress = null;
					break;
				}
			}

			if (fallbackProgress != null)
			{
				fallbackProgress.addFood(event.getElapsedSeconds());
			}
		}

		return progresses;
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

		private void addFood(int elapsedSeconds)
		{
			foodFed++;
			lastFoodElapsed = Math.max(lastFoodElapsed, elapsedSeconds);
		}

		private boolean hasTarget()
		{
			return instruction != null && instruction.hasTarget();
		}

		private boolean acceptsMoreFood()
		{
			if (!hasTarget())
			{
				return false;
			}

			if (foodFed < instruction.getTargetFoodCount())
			{
				return true;
			}

			return getState(instruction, foodFed, lastFoodElapsed) != CodeDisplayState.COMPLETE;
		}
	}
}
