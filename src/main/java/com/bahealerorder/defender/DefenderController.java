package com.bahealerorder.defender;

import com.bahealerorder.common.BaOverviewNpcType;
import com.bahealerorder.common.BaWaveLifecycleService;
import com.bahealerorder.common.BaWaveOverviewService;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.util.Text;

@Singleton
public class DefenderController
{
	private static final String PENANCE_RUNNER_NAME = "Penance Runner";

	private final BaWaveOverviewService waveOverviewService;
	private final BaWaveLifecycleService waveLifecycleService;
	private final Map<NPC, BaOverviewNpcType> visibleRunnerNpcs = new HashMap<>();

	@Inject
	private DefenderController(
			BaWaveOverviewService waveOverviewService,
			BaWaveLifecycleService waveLifecycleService)
	{
		this.waveOverviewService = waveOverviewService;
		this.waveLifecycleService = waveLifecycleService;
	}

	public void startUp()
	{
		resetState();
	}

	public void shutDown()
	{
		resetState();
	}

	public void onNpcSpawned(NpcSpawned event)
	{
		if (!waveLifecycleService.isWaveActive()) return;

		NPC npc = event.getNpc();
		if (npc == null || npc.getName() == null) return;

		String npcName = Text.removeTags(npc.getName()).toLowerCase(Locale.ROOT);
		if (!PENANCE_RUNNER_NAME.toLowerCase(Locale.ROOT).equals(npcName)) return;

		visibleRunnerNpcs.put(npc, BaOverviewNpcType.RUNNER);
		waveOverviewService.recordSpawn(BaOverviewNpcType.RUNNER, npc.getIndex());
	}

	public void onNpcDespawned(NpcDespawned event)
	{
		visibleRunnerNpcs.remove(event.getNpc());
	}

	public void onActorDeath(ActorDeath event)
	{
		recordRunnerDeath(event.getActor());
	}

	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();

		if (actor != null && actor.getAnimation() == AnimationID.BARBASSAULT_PENANCE_RUNNER_DEATH)
		{
			recordRunnerDeath(actor);
		}
	}

	public void onGameTick(GameTick event)
	{
		if (!waveLifecycleService.isWaveActive()) return;

		for (Map.Entry<NPC, BaOverviewNpcType> entry : visibleRunnerNpcs.entrySet())
		{
			NPC npc = entry.getKey();
			if (npc != null && npc.getHealthRatio() == 0)
			{
				waveOverviewService.recordDeath(entry.getValue(), npc.getIndex());
			}
		}
	}

	public void onWaveEnded()
	{
		resetWaveState();
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();

		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			resetState();
		}
	}

	private void recordRunnerDeath(Actor actor)
	{
		if (!waveLifecycleService.isWaveActive() || !(actor instanceof NPC)) return;

		NPC npc = (NPC) actor;
		if (visibleRunnerNpcs.containsKey(npc))
		{
			waveOverviewService.recordDeath(BaOverviewNpcType.RUNNER, npc.getIndex());
		}
	}

	private void resetWaveState()
	{
		visibleRunnerNpcs.clear();
	}

	private void resetState()
	{
		resetWaveState();
	}
}
