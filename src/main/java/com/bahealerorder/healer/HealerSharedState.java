package com.bahealerorder.healer;

import com.bahealerorder.common.BaHealerSyncMessage;
import com.bahealerorder.healer.codes.FeedEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HealerSharedState
{
	private static final int UNKNOWN_TICK = -1;
	private static final int SYNC_CALL_COUNT = 3;

	private final Map<Integer, State> statesByOrder = new HashMap<>();
	private int wave = -1;
	private int currentCallIndex;

	void reset()
	{
		statesByOrder.clear();
		wave = -1;
		currentCallIndex = 0;
	}

	void startWave(int wave)
	{
		if (this.wave == wave) return;

		reset();
		this.wave = wave;
	}

	void recordLocalSpawn(int healerOrder, int npcIndex)
	{
		State state = state(healerOrder);
		state.spawned = true;
		state.localSpawned = true;
		state.npcIndex = npcIndex;
	}

	void recordLocalCallIndex(int callIndex)
	{
		currentCallIndex = Math.max(currentCallIndex, callIndex);
	}

	void recordLocalFood(int healerOrder, int callIndex, int elapsedSeconds)
	{
		State state = state(healerOrder);
		state.ensureCallCapacity(callIndex + 1);
		state.localFoodFedByCall[callIndex]++;
		state.localLastFoodElapsedByCall[callIndex] = Math.max(state.localLastFoodElapsedByCall[callIndex], elapsedSeconds);
	}

	void recordLocalPrediction(int healerOrder, int predictedDeathTick, boolean unknownTtk, int observedTick)
	{
		State state = state(healerOrder);
		state.localPrediction = true;
		state.predictedDeathTick = predictedDeathTick;
		state.unknownTtk = predictedDeathTick < 0 && unknownTtk;
		state.observedTick = Math.max(state.observedTick, observedTick);
	}

	void recordLocalDeath(int healerOrder, int deathTick)
	{
		State state = state(healerOrder);
		state.localDeath = true;
		state.actualDeathTick = deathTick;
		state.unknownTtk = false;
	}

	void updateFromParty(BaHealerSyncMessage message)
	{
		if (message.getWave() <= 0 || message.getHealerOrder() <= 0) return;

		startWave(message.getWave());
		currentCallIndex = Math.max(currentCallIndex, message.getCurrentCallIndex());

		State state = state(message.getHealerOrder());
		state.spawned = true;

		if (!state.localSpawned && state.npcIndex < 0)
		{
			state.npcIndex = message.getNpcIndex();
		}

		if (!state.localDeath && state.actualDeathTick < 0 && message.getActualDeathTick() >= 0)
		{
			state.actualDeathTick = message.getActualDeathTick();
		}

		boolean newerObservation = message.getObservedTick() >= state.observedTick;
		if (!state.localPrediction && state.actualDeathTick < 0 && newerObservation)
		{
			state.predictedDeathTick = message.getPredictedDeathTick();
			state.unknownTtk = message.getPredictedDeathTick() < 0 && message.isUnknownTtk();
			state.observedTick = Math.max(state.observedTick, message.getObservedTick());
		}
	}

	boolean hasSpawned(int healerOrder)
	{
		return stateOrNull(healerOrder) != null && state(healerOrder).spawned;
	}

	boolean isDead(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state != null && state.actualDeathTick >= 0;
	}

	Integer getActualDeathTick(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state == null || state.actualDeathTick < 0 ? null : state.actualDeathTick;
	}

	Integer getPredictedDeathTick(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state == null || state.predictedDeathTick < 0 ? null : state.predictedDeathTick;
	}

	boolean hasUnknownTtk(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state != null && state.unknownTtk;
	}

	int getCurrentCallIndex()
	{
		return currentCallIndex;
	}

	Map<Integer, Integer> getFoodFedByHealerOrder()
	{
		Map<Integer, Integer> foodFedByHealerOrder = new HashMap<>();

		for (State state : statesByOrder.values())
		{
			int foodFed = state.totalFoodFed();
			if (foodFed > 0)
			{
				foodFedByHealerOrder.put(state.healerOrder, foodFed);
			}
		}

		return foodFedByHealerOrder;
	}

	List<FeedEvent> getFeedEvents()
	{
		List<FeedEvent> feedEvents = new ArrayList<>();

		for (State state : statesByOrder.values())
		{
			for (int callIndex = 0; callIndex < state.callCount(); callIndex++)
			{
				int count = state.foodFed(callIndex);
				int elapsed = Math.max(state.lastFoodElapsed(callIndex), 0);

				for (int i = 0; i < count; i++)
				{
					feedEvents.add(new FeedEvent(state.healerOrder, elapsed, callIndex));
				}
			}
		}

		return feedEvents;
	}

	LocalSnapshot getLocalSnapshot(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		if (state == null || !state.hasLocalData()) return null;

		return new LocalSnapshot(
				state.npcIndex,
				state.localPrediction ? state.predictedDeathTick : UNKNOWN_TICK,
				state.localPrediction && state.predictedDeathTick < 0 && state.unknownTtk,
				state.localDeath ? state.actualDeathTick : UNKNOWN_TICK
		);
	}

	private State state(int healerOrder)
	{
		return statesByOrder.computeIfAbsent(healerOrder, State::new);
	}

	private State stateOrNull(int healerOrder)
	{
		return statesByOrder.get(healerOrder);
	}

	static class LocalSnapshot
	{
		private final int npcIndex;
		private final int predictedDeathTick;
		private final boolean unknownTtk;
		private final int actualDeathTick;

		private LocalSnapshot(
				int npcIndex,
				int predictedDeathTick,
				boolean unknownTtk,
				int actualDeathTick)
		{
			this.npcIndex = npcIndex;
			this.predictedDeathTick = predictedDeathTick;
			this.unknownTtk = unknownTtk;
			this.actualDeathTick = actualDeathTick;
		}

		int getNpcIndex()
		{
			return npcIndex;
		}

		int getPredictedDeathTick()
		{
			return predictedDeathTick;
		}

		boolean isUnknownTtk()
		{
			return unknownTtk;
		}

		int getActualDeathTick()
		{
			return actualDeathTick;
		}
	}

	private static class State
	{
		private final int healerOrder;
		private boolean spawned;
		private boolean localSpawned;
		private int npcIndex = UNKNOWN_TICK;
		private int[] localFoodFedByCall = new int[SYNC_CALL_COUNT];
		private int[] localLastFoodElapsedByCall = filledArray(SYNC_CALL_COUNT, UNKNOWN_TICK);
		private boolean localPrediction;
		private int predictedDeathTick = UNKNOWN_TICK;
		private boolean unknownTtk;
		private boolean localDeath;
		private int actualDeathTick = UNKNOWN_TICK;
		private int observedTick = UNKNOWN_TICK;

		private State(int healerOrder)
		{
			this.healerOrder = healerOrder;
		}

		private boolean hasLocalData()
		{
			return localSpawned || localTotalFoodFed() > 0 || localPrediction || localDeath;
		}

		private int callCount()
		{
			return localFoodFedByCall.length;
		}

		private int foodFed(int callIndex)
		{
			return callIndex < localFoodFedByCall.length ? localFoodFedByCall[callIndex] : 0;
		}

		private int lastFoodElapsed(int callIndex)
		{
			return callIndex < localLastFoodElapsedByCall.length ? localLastFoodElapsedByCall[callIndex] : UNKNOWN_TICK;
		}

		private int totalFoodFed()
		{
			int total = 0;

			for (int callIndex = 0; callIndex < callCount(); callIndex++)
			{
				total += foodFed(callIndex);
			}

			return total;
		}

		private int localTotalFoodFed()
		{
			int total = 0;

			for (int count : localFoodFedByCall)
			{
				total += count;
			}

			return total;
		}

		private void ensureCallCapacity(int size)
		{
			if (size <= localFoodFedByCall.length) return;

			localFoodFedByCall = Arrays.copyOf(localFoodFedByCall, size);
			localLastFoodElapsedByCall = copyAndFill(localLastFoodElapsedByCall, size, UNKNOWN_TICK);
		}

		private static int[] filledArray(int size, int value)
		{
			int[] values = new int[size];
			Arrays.fill(values, value);
			return values;
		}

		private static int[] copyAndFill(int[] source, int size, int fillValue)
		{
			int[] values = Arrays.copyOf(source, size);
			Arrays.fill(values, source.length, values.length, fillValue);
			return values;
		}
	}
}
