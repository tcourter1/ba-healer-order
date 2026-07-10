package com.bahealerorder.healer.codes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HealerCodeProgress
{
	private HealerCodeProgress()
	{
	}

	public static int getExpectedFoodForOrder(WaveCode waveCode, int healerOrder, int currentCallIndex)
	{
		if (waveCode == null) return 0;

		int expected = 0;

		for (CallCode call : waveCode.getCalls())
		{
			if (call.getCallIndex() > currentCallIndex) continue;

			HealerInstruction instruction = call.getInstruction(healerOrder);
			if (instruction != null && instruction.hasTarget())
			{
				expected += instruction.getTotalTargetFoodCount();
			}
		}

		return expected;
	}

	public static HealerCodeStatus getCurrentStatus(WaveCode waveCode, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		if (waveCode == null) return null;

		CallCode call = waveCode.getCall(currentCallIndex);
		if (call == null) return null;

		HealerInstruction instruction = call.getInstruction(healerOrder);
		if (instruction == null || !instruction.hasTarget()) return null;

		int foodThisCall = countFoodForCall(healerOrder, currentCallIndex, feedEvents);
		int lastFoodElapsed = lastFoodElapsed(healerOrder, currentCallIndex, feedEvents);
		return displayStatus(instruction, getOverallState(instruction, foodThisCall, lastFoodElapsed), foodThisCall, lastFoodElapsed);
	}

	public static HealerCodeStatus getPreviousStatus(WaveCode waveCode, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		if (waveCode == null) return null;

		int previousCallIndex = currentCallIndex - 1;
		if (previousCallIndex < 0) return null;

		CallCode call = waveCode.getCall(previousCallIndex);
		if (call == null) return null;

		HealerInstruction instruction = call.getInstruction(healerOrder);
		if (instruction == null || !instruction.hasTarget()) return null;

		int foodThisCall = countFoodForCall(healerOrder, previousCallIndex, feedEvents);
		int lastFoodElapsed = lastFoodElapsed(healerOrder, previousCallIndex, feedEvents);
		return displayStatus(instruction, CodeDisplayState.PREVIOUS, foodThisCall, lastFoodElapsed);
	}

	public static HealerCodeStatus getDisplayStatus(WaveCode waveCode, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		List<InstructionProgress> progresses = getInstructionProgresses(waveCode, healerOrder, currentCallIndex, feedEvents);

		InstructionProgress currentProgress = null;
		InstructionProgress mostRecentProgress = null;

		for (InstructionProgress progress : progresses)
		{
			if (!progress.hasTarget()) continue;

			mostRecentProgress = progress;

			if (progress.callIndex == currentCallIndex)
			{
				currentProgress = progress;
			}

			CodeDisplayState state = getOverallState(progress.instruction, progress.foodFed, progress.lastFoodElapsed);
			if (progress.callIndex < currentCallIndex && state != CodeDisplayState.COMPLETE) return progress.status(CodeDisplayState.IN_PROGRESS);
		}

		if (currentProgress != null) return currentProgress.status(getOverallState(currentProgress.instruction, currentProgress.foodFed, currentProgress.lastFoodElapsed));

		return mostRecentProgress == null ? null : mostRecentProgress.status(CodeDisplayState.PREVIOUS);
	}

	public static HealerCodeStatus getPanelStatusForCall(
			WaveCode waveCode,
			int healerOrder,
			int currentCallIndex,
			int panelCallIndex,
			List<FeedEvent> feedEvents)
	{
		if (panelCallIndex > currentCallIndex)
		{
			CallCode call = waveCode == null ? null : waveCode.getCall(panelCallIndex);
			HealerInstruction instruction = call == null ? null : call.getInstruction(healerOrder);
			return instruction != null && instruction.hasTarget()
					? displayStatus(instruction, CodeDisplayState.NOT_STARTED, 0, -1)
					: null;
		}

		for (InstructionProgress progress : getInstructionProgresses(waveCode, healerOrder, currentCallIndex, feedEvents))
		{
			if (progress.callIndex == panelCallIndex) return progress.hasTarget() ? progress.status(getOverallState(progress.instruction, progress.foodFed, progress.lastFoodElapsed)) : null;
		}

		return null;
	}

	public static int getPanelFoodCountForCall(
			WaveCode waveCode,
			int healerOrder,
			int currentCallIndex,
			int panelCallIndex,
			List<FeedEvent> feedEvents)
	{
		for (InstructionProgress progress : getInstructionProgresses(waveCode, healerOrder, currentCallIndex, feedEvents))
		{
			if (progress.callIndex == panelCallIndex) return progress.foodFed;
		}

		return 0;
	}

	private static List<InstructionProgress> getInstructionProgresses(
			WaveCode waveCode,
			int healerOrder,
			int currentCallIndex,
			List<FeedEvent> feedEvents)
	{
		if (waveCode == null) return new ArrayList<>();

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

				if (event.getCallIndex() >= progress.callIndex && progress.acceptsMoreFood())
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

	private static CodeDisplayState getOverallState(HealerInstruction instruction, int foodFed, int lastFoodElapsed)
	{
		if (foodFed <= 0) return CodeDisplayState.NOT_STARTED;

		if (foodFed < instruction.getTotalTargetFoodCount()) return CodeDisplayState.IN_PROGRESS;

		if (instruction.getAfterSeconds() != null && lastFoodElapsed < instruction.getAfterSeconds()) return CodeDisplayState.IN_PROGRESS;

		if (instruction.getBeforeSeconds() != null && lastFoodElapsed >= instruction.getBeforeSeconds()) return CodeDisplayState.IN_PROGRESS;

		if (instruction.getExactSeconds() != null && lastFoodElapsed != instruction.getExactSeconds()) return CodeDisplayState.IN_PROGRESS;

		return CodeDisplayState.COMPLETE;
	}

	private static HealerCodeStatus displayStatus(HealerInstruction instruction, CodeDisplayState state, int totalFoodFed, int lastFoodElapsed)
	{
		DisplayPhase phase = displayPhase(instruction, totalFoodFed);
		return new HealerCodeStatus(phase.instruction, state, phase.foodFed, lastFoodElapsed);
	}

	private static DisplayPhase displayPhase(HealerInstruction instruction, int totalFoodFed)
	{
		if (instruction == null || !instruction.hasPostRestockFoodCount()) return new DisplayPhase(instruction, totalFoodFed);

		int firstTarget = instruction.getTargetFoodCount();
		if (totalFoodFed < firstTarget)
		{
			HealerInstruction firstPhase = instruction.copy();
			firstPhase.setPostRestockFoodCount(0);
			firstPhase.setAdvanced(false);
			firstPhase.setRaw(null);
			return new DisplayPhase(firstPhase, totalFoodFed);
		}

		HealerInstruction secondPhase = new HealerInstruction(
				instruction.getPostRestockFoodCount(),
				instruction.getAfterSeconds(),
				instruction.getBeforeSeconds(),
				instruction.getExactSeconds(),
				null
		);
		return new DisplayPhase(secondPhase, totalFoodFed - firstTarget);
	}

	private static int countFoodForCall(int healerOrder, int callIndex, List<FeedEvent> feedEvents)
	{
		int count = 0;

		for (FeedEvent event : safeEvents(feedEvents))
		{
			if (event.getHealerOrder() == healerOrder && event.getCallIndex() == callIndex)
			{
				count++;
			}
		}

		return count;
	}

	private static int lastFoodElapsed(int healerOrder, int callIndex, List<FeedEvent> feedEvents)
	{
		int last = -1;

		for (FeedEvent event : safeEvents(feedEvents))
		{
			if (event.getHealerOrder() == healerOrder && event.getCallIndex() == callIndex)
			{
				last = Math.max(last, event.getElapsedSeconds());
			}
		}

		return last;
	}

	private static List<FeedEvent> safeEvents(List<FeedEvent> feedEvents)
	{
		return feedEvents == null ? new ArrayList<>() : feedEvents;
	}

	private static class DisplayPhase
	{
		private final HealerInstruction instruction;
		private final int foodFed;

		private DisplayPhase(HealerInstruction instruction, int foodFed)
		{
			this.instruction = instruction;
			this.foodFed = Math.max(0, foodFed);
		}
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
			return displayStatus(instruction, state, foodFed, lastFoodElapsed);
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
			if (!hasTarget()) return false;

			if (foodFed < instruction.getTotalTargetFoodCount()) return true;

			return getOverallState(instruction, foodFed, lastFoodElapsed) != CodeDisplayState.COMPLETE;
		}
	}
}
