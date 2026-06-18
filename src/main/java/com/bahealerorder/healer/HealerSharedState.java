package com.bahealerorder.healer;

import com.bahealerorder.common.BaHealerSyncMessage;
import com.bahealerorder.healer.codes.FeedEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HealerSharedState
{
	private static final int UNKNOWN_TICK = -1;
	private static final int SYNC_CALL_COUNT = 3;

	private final Map<Integer, State> statesByOrder = new HashMap<>();
	private int wave = -1;
	private int currentCallIndex;

	@Inject
	public HealerSharedState()
	{
	}

	void reset()
	{
		statesByOrder.clear();
		wave = -1;
		currentCallIndex = 0;
	}

	boolean startWave(int wave)
	{
		if (this.wave == wave) return false;

		reset();
		this.wave = wave;
		return true;
	}

	boolean recordLocalSpawn(int healerOrder, int npcIndex, int spawnTick)
	{
		return recordSpawn(healerOrder, npcIndex, spawnTick);
	}

	private boolean recordSpawn(int healerOrder, int npcIndex, int spawnTick)
	{
		State state = state(healerOrder);
		boolean changed = !state.spawned;
		state.spawned = true;
		if (spawnTick >= 0)
		{
			int previousSpawnTick = state.spawnTick;
			state.spawnTick = state.spawnTick < 0 ? spawnTick : Math.min(state.spawnTick, spawnTick);
			changed |= previousSpawnTick != state.spawnTick;
		}

		if (npcIndex >= 0)
		{
			int previousNpcIndex = state.npcIndex;
			clearNpcIndexFromOtherStates(healerOrder, npcIndex);
			state.npcIndex = npcIndex;
			changed |= previousNpcIndex != state.npcIndex;
		}

		return changed;
	}

	void recordLocalCallIndex(int callIndex)
	{
		currentCallIndex = Math.max(currentCallIndex, callIndex);
	}

	void recordLocalFood(int healerOrder, int callIndex, int elapsedSeconds, int foodTick)
	{
		State state = state(healerOrder);
		state.ensureCallCapacity(callIndex + 1);
		state.localFoodFedByCall[callIndex]++;
		state.localLastFoodElapsedByCall[callIndex] = Math.max(state.localLastFoodElapsedByCall[callIndex], elapsedSeconds);
		if (foodTick >= 0)
		{
			state.localFoodTicks.add(foodTick);
			Collections.sort(state.localFoodTicks);
		}
	}

	boolean recordPrediction(int healerOrder, int predictedDeathTick, boolean unknownTtk)
	{
		State state = state(healerOrder);

		if (state.actualDeathTick >= 0) return false;

		if (predictedDeathTick >= 0)
		{
			boolean changed = state.predictedDeathTick != predictedDeathTick || state.unknownTtk;
			state.predictedDeathTick = predictedDeathTick;
			state.unknownTtk = false;
			return changed;
		}

		boolean changed = state.unknownTtk != unknownTtk || state.predictedDeathTick != UNKNOWN_TICK;
		state.predictedDeathTick = UNKNOWN_TICK;
		state.unknownTtk = unknownTtk;
		return changed;
	}

	boolean recordHealthRatioMode(int healerOrder)
	{
		State state = state(healerOrder);
		if (state.healthRatioMode) return false;

		state.healthRatioMode = true;
		return true;
	}

	boolean recordDeath(int healerOrder, int deathTick)
	{
		State state = state(healerOrder);
		boolean wasUnknown = state.unknownTtk;
		boolean changed = recordDeath(state, deathTick, true);
		state.unknownTtk = false;
		return changed || wasUnknown;
	}

	boolean recordPresumedDeath(int healerOrder, int deathTick)
	{
		State state = state(healerOrder);
		boolean wasUnknown = state.unknownTtk;
		boolean changed = recordDeath(state, deathTick, false);
		state.unknownTtk = false;
		return changed || wasUnknown;
	}

	boolean clearPresumedDeath(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		if (state == null || state.actualDeathTick < 0 || state.observedDeath) return false;

		state.actualDeathTick = UNKNOWN_TICK;
		return true;
	}

	boolean updateFromParty(BaHealerSyncMessage message, boolean acceptPrediction)
	{
		if (message.getWave() <= 0 || message.getHealerOrder() <= 0) return false;

		boolean changed = startWave(message.getWave());
		currentCallIndex = Math.max(currentCallIndex, message.getCurrentCallIndex());

		changed |= recordSpawn(message.getHealerOrder(), message.getNpcIndex(), message.getSpawnTick());
		State state = state(message.getHealerOrder());

		if (message.getActualDeathTick() >= 0)
		{
			changed |= recordDeath(state, message.getActualDeathTick(), message.isObservedDeath());
		}

		changed |= message.isHealthRatioMode() && recordHealthRatioMode(message.getHealerOrder());

		boolean hasPrediction = message.getPredictedDeathTick() >= 0 || message.isUnknownTtk();
		if (acceptPrediction && hasPrediction && state.actualDeathTick < 0)
		{
			changed |= state.predictedDeathTick != message.getPredictedDeathTick()
					|| state.unknownTtk != (message.getPredictedDeathTick() < 0 && message.isUnknownTtk());
			state.predictedDeathTick = message.getPredictedDeathTick();
			state.unknownTtk = message.getPredictedDeathTick() < 0 && message.isUnknownTtk();
		}

		return changed;
	}

	public boolean hasSpawned(int healerOrder)
	{
		return stateOrNull(healerOrder) != null && state(healerOrder).spawned;
	}

	Integer getHealerOrderForNpcIndex(int npcIndex)
	{
		return getHealerOrdersByNpcIndex().get(npcIndex);
	}

	Map<Integer, Integer> getHealerOrdersByNpcIndex()
	{
		Map<Integer, Integer> orderByNpcIndex = new HashMap<>();

		for (State state : statesByOrder.values())
		{
			if (state.npcIndex >= 0)
			{
				orderByNpcIndex.put(state.npcIndex, state.healerOrder);
			}
		}

		return orderByNpcIndex;
	}

	int getNpcIndex(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state == null ? UNKNOWN_TICK : state.npcIndex;
	}

	int getSpawnTick(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state == null ? UNKNOWN_TICK : state.spawnTick;
	}

	boolean isDead(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state != null && state.actualDeathTick >= 0;
	}

	public Integer getActualDeathTick(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state == null || state.actualDeathTick < 0 ? null : state.actualDeathTick;
	}

	boolean isObservedDeath(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state != null && state.observedDeath;
	}

	public Integer getPredictedDeathTick(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state == null || state.predictedDeathTick < 0 ? null : state.predictedDeathTick;
	}

	public boolean hasUnknownTtk(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state != null && state.unknownTtk;
	}

	int getCurrentCallIndex()
	{
		return currentCallIndex;
	}

	public int getWave()
	{
		return wave;
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

	int[] getLocalFoodTicks(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		if (state == null || state.localFoodTicks.isEmpty()) return new int[0];

		int[] ticks = new int[state.localFoodTicks.size()];
		for (int i = 0; i < state.localFoodTicks.size(); i++)
		{
			ticks[i] = state.localFoodTicks.get(i);
		}

		return ticks;
	}

	boolean isHealthRatioMode(int healerOrder)
	{
		State state = stateOrNull(healerOrder);
		return state != null && state.healthRatioMode;
	}

	List<Integer> recordPartyFoodTicks(long memberId, int healerOrder, int[] foodTicks)
	{
		List<Integer> newFoodTicks = new ArrayList<>();
		if (foodTicks == null || foodTicks.length == 0) return newFoodTicks;

		State state = state(healerOrder);
		Set<Integer> memberTicks = state.partyFoodTicksByMember.computeIfAbsent(memberId, ignored -> new HashSet<>());

		for (int foodTick : foodTicks)
		{
			if (foodTick >= 0 && memberTicks.add(foodTick))
			{
				newFoodTicks.add(foodTick);
			}
		}

		Collections.sort(newFoodTicks);
		return newFoodTicks;
	}

	private State state(int healerOrder)
	{
		return statesByOrder.computeIfAbsent(healerOrder, State::new);
	}

	private State stateOrNull(int healerOrder)
	{
		return statesByOrder.get(healerOrder);
	}

	private void clearNpcIndexFromOtherStates(int healerOrder, int npcIndex)
	{
		for (State state : statesByOrder.values())
		{
			if (state.healerOrder != healerOrder && state.npcIndex == npcIndex)
			{
				state.npcIndex = UNKNOWN_TICK;
			}
		}
	}

	private boolean recordDeath(State state, int deathTick, boolean observed)
	{
		if (deathTick < 0) return false;

		int previousDeathTick = state.actualDeathTick;
		boolean previousObservedDeath = state.observedDeath;

		if (state.actualDeathTick < 0 || observed && !state.observedDeath)
		{
			state.actualDeathTick = deathTick;
			state.observedDeath = observed;
		}
		else if (!observed && !state.observedDeath)
		{
			state.actualDeathTick = Math.min(state.actualDeathTick, deathTick);
		}

		return previousDeathTick != state.actualDeathTick || previousObservedDeath != state.observedDeath;
	}

	private static class State
	{
		private final int healerOrder;
		private boolean spawned;
		private int npcIndex = UNKNOWN_TICK;
		private int spawnTick = UNKNOWN_TICK;
		private int[] localFoodFedByCall = new int[SYNC_CALL_COUNT];
		private int[] localLastFoodElapsedByCall = filledArray(SYNC_CALL_COUNT, UNKNOWN_TICK);
		private final List<Integer> localFoodTicks = new ArrayList<>();
		private final Map<Long, Set<Integer>> partyFoodTicksByMember = new HashMap<>();
		private int predictedDeathTick = UNKNOWN_TICK;
		private boolean unknownTtk;
		private int actualDeathTick = UNKNOWN_TICK;
		private boolean observedDeath;
		private boolean healthRatioMode;

		private State(int healerOrder)
		{
			this.healerOrder = healerOrder;
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
