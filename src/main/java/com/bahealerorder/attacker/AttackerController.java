package com.bahealerorder.attacker;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.common.BaOverviewNpcType;
import com.bahealerorder.common.BaNpcIds;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.BaRoleDetector;
import com.bahealerorder.common.BaWaveLifecycleService;
import com.bahealerorder.common.BaWaveInfo;
import com.bahealerorder.common.BaWaveOverviewService;
import com.bahealerorder.common.BaWaveOverviewState;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.ui.overlay.OverlayManager;

@Singleton
public class AttackerController
{
    private final BaUtilitiesConfig config;
    private final BaRoleDetector roleDetector;
    private final BaWaveLifecycleService waveLifecycleService;
    private final OverlayManager overlayManager;
    private final AttackerCaveOverlay caveOverlay;
    private final BaWaveOverviewService waveOverviewService;
    private final BaWaveOverviewState waveOverviewState;

    private final Map<NPC, BaOverviewNpcType> visibleAttackableNpcs = new HashMap<>();

    @Inject
    private AttackerController(
            BaUtilitiesConfig config,
            BaRoleDetector roleDetector,
            BaWaveLifecycleService waveLifecycleService,
            OverlayManager overlayManager,
            AttackerCaveOverlay caveOverlay,
            BaWaveOverviewService waveOverviewService,
            BaWaveOverviewState waveOverviewState)
    {
        this.config = config;
        this.roleDetector = roleDetector;
        this.waveLifecycleService = waveLifecycleService;
        this.overlayManager = overlayManager;
        this.caveOverlay = caveOverlay;
        this.waveOverviewService = waveOverviewService;
        this.waveOverviewState = waveOverviewState;
    }

    public void startUp()
    {
        caveOverlay.setController(this);
        overlayManager.add(caveOverlay);
        resetState();
    }

    public void shutDown()
    {
        overlayManager.remove(caveOverlay);
        resetState();
    }

    public void onNpcSpawned(NpcSpawned event)
    {
        if (!isWaveActive()) return;

        NPC npc = event.getNpc();
        BaOverviewNpcType type = BaNpcIds.getOverviewType(npc);

        if (type == BaOverviewNpcType.RANGER || type == BaOverviewNpcType.FIGHTER)
        {
            visibleAttackableNpcs.put(npc, type);
            waveOverviewService.recordSpawn(type, npc.getIndex());
        }
    }

    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();
        BaOverviewNpcType type = visibleAttackableNpcs.remove(npc);

        if (isWaveActive() && type != null)
        {
            waveOverviewService.recordDeath(type, npc.getIndex());
        }
    }

    public void onGameStateChanged(GameStateChanged event)
    {
        GameState gameState = event.getGameState();

        if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
        {
            resetState();
        }
    }

    public boolean shouldRenderSpawnCountOverlay()
    {
        return config.showAttackerSpawnCountOverlay()
                && isAttackerRole()
                && isWaveActive()
                && getRangerTotal() > 0
                && getFighterTotal() > 0;
    }

    public boolean isAttackerRole()
    {
        return roleDetector.isRole(BaRole.ATTACKER);
    }

    public boolean isWaveActive()
    {
        return waveLifecycleService.isWaveActive();
    }

    public int getCurrentWave()
    {
        return waveLifecycleService.getWave();
    }

    public int getRangerTotal()
    {
        return BaWaveInfo.getExpectedCount(getCurrentWave(), BaOverviewNpcType.RANGER);
    }

    public int getFighterTotal()
    {
        return BaWaveInfo.getExpectedCount(getCurrentWave(), BaOverviewNpcType.FIGHTER);
    }

    public int getRangersSpawned()
    {
        return waveOverviewState.getSpawnedCount(BaOverviewNpcType.RANGER);
    }

    public int getFightersSpawned()
    {
        return waveOverviewState.getSpawnedCount(BaOverviewNpcType.FIGHTER);
    }

    public void onWaveStarted()
    {
        visibleAttackableNpcs.clear();
    }

    public void onWaveEnded()
    {
        resetState();
    }

    private void resetState()
    {
        visibleAttackableNpcs.clear();
    }

}
