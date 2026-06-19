package com.bahealerorder.attacker;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.common.BaOverviewNpcType;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.BaRoleDetector;
import com.bahealerorder.common.BaWaveLifecycleService;
import com.bahealerorder.common.BaWaveInfo;
import com.bahealerorder.common.BaWaveOverviewService;
import com.bahealerorder.common.BaWaveOverviewState;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
public class AttackerController
{
    private static final String PENANCE_RANGER_NAME = "Penance Ranger";
    private static final String PENANCE_FIGHTER_NAME = "Penance Fighter";

    /*
     * Normal waves 1-9. Captured from Block -> Penance cave debug.
     */
    private static final WorldPoint NORMAL_RANGER_CAVE_LABEL_TILE = new WorldPoint(12810, 4518, 0);
    private static final WorldPoint NORMAL_FIGHTER_CAVE_LABEL_TILE = new WorldPoint(12816, 4519, 0);

    /*
     * Wave 10 is in a different chunk/instance. These are temporary placeholders
     * until the Wave 10 cave coordinates are captured.
     */
    private static final WorldPoint WAVE_10_RANGER_CAVE_LABEL_TILE = NORMAL_RANGER_CAVE_LABEL_TILE;
    private static final WorldPoint WAVE_10_FIGHTER_CAVE_LABEL_TILE = NORMAL_FIGHTER_CAVE_LABEL_TILE;

    private final Client client;
    private final BaUtilitiesConfig config;
    private final BaRoleDetector roleDetector;
    private final BaWaveLifecycleService waveLifecycleService;
    private final OverlayManager overlayManager;
    private final AttackerCaveOverlay caveOverlay;
    private final BaWaveOverviewService waveOverviewService;
    private final BaWaveOverviewState waveOverviewState;

    private final Map<NPC, BaOverviewNpcType> visibleAttackableNpcs = new HashMap<>();

    private int lastSpawnOverlayDebugTick = -100;

    @Inject
    private AttackerController(
            Client client,
            BaUtilitiesConfig config,
            BaRoleDetector roleDetector,
            BaWaveLifecycleService waveLifecycleService,
            OverlayManager overlayManager,
            AttackerCaveOverlay caveOverlay,
            BaWaveOverviewService waveOverviewService,
            BaWaveOverviewState waveOverviewState)
    {
        this.client = client;
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
        resetAllState();
    }

    public void shutDown()
    {
        overlayManager.remove(caveOverlay);
        resetAllState();
    }

    public void onNpcSpawned(NpcSpawned event)
    {
        if (!isWaveActive())
        {
            return;
        }

        NPC npc = event.getNpc();

        if (npc == null || npc.getName() == null)
        {
            return;
        }

        String npcName = Text.removeTags(npc.getName());

        if (PENANCE_RANGER_NAME.equals(npcName))
        {
            visibleAttackableNpcs.put(npc, BaOverviewNpcType.RANGER);
            waveOverviewService.recordSpawn(BaOverviewNpcType.RANGER, npc.getIndex());
            log.debug("Attacker spawn counter registered Ranger {}/{} for wave {}", getRangersSpawned(), getRangerTotal(), getCurrentWave());
        }
        else if (PENANCE_FIGHTER_NAME.equals(npcName))
        {
            visibleAttackableNpcs.put(npc, BaOverviewNpcType.FIGHTER);
            waveOverviewService.recordSpawn(BaOverviewNpcType.FIGHTER, npc.getIndex());
            log.debug("Attacker spawn counter registered Fighter {}/{} for wave {}", getFightersSpawned(), getFighterTotal(), getCurrentWave());
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
            resetAllState();
        }
    }

    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        debugAttackerCaveClick(event);
    }

    public boolean shouldRenderSpawnCountOverlay()
    {
        boolean enabled = config.showAttackerSpawnCountOverlay();
        boolean attackerRole = isAttackerRole();
        boolean waveActive = isWaveActive();
        int rangerTotal = getRangerTotal();
        int fighterTotal = getFighterTotal();

        boolean shouldRender = enabled
                && attackerRole
                && waveActive
                && rangerTotal > 0
                && fighterTotal > 0;

        int tick = client.getTickCount();

        if (tick - lastSpawnOverlayDebugTick >= 25)
        {
            lastSpawnOverlayDebugTick = tick;

            log.debug(
                    "Attacker spawn overlay render check: shouldRender={}, enabled={}, attackerRole={}, currentRole={}, waveActive={}, currentWave={}, rangers={}/{}, fighters={}/{}",
                    shouldRender,
                    enabled,
                    attackerRole,
                    roleDetector.getCurrentRole(),
                    waveActive,
                    getCurrentWave(),
                    getRangersSpawned(),
                    rangerTotal,
                    getFightersSpawned(),
                    fighterTotal
            );
        }

        return shouldRender;
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

    public WorldPoint getRangerCaveLabelTile()
    {
        return getCurrentWave() == 10 ? WAVE_10_RANGER_CAVE_LABEL_TILE : NORMAL_RANGER_CAVE_LABEL_TILE;
    }

    public WorldPoint getFighterCaveLabelTile()
    {
        return getCurrentWave() == 10 ? WAVE_10_FIGHTER_CAVE_LABEL_TILE : NORMAL_FIGHTER_CAVE_LABEL_TILE;
    }

    public void onWaveStarted(int wave)
    {
        if (wave < 1 || wave > 10)
        {
            return;
        }

        visibleAttackableNpcs.clear();

        log.debug(
                "Starting attacker spawn counter for wave {}. Rangers total={}, Fighters total={}",
                getCurrentWave(),
                getRangerTotal(),
                getFighterTotal()
        );
    }

    public void onWaveEnded()
    {
        resetWaveState();
    }

    private void resetWaveState()
    {
        visibleAttackableNpcs.clear();
    }

    private void resetAllState()
    {
        resetWaveState();
    }

    private void debugAttackerCaveClick(MenuOptionClicked event)
    {
        if (client.getLocalPlayer() == null)
        {
            return;
        }

        String option = Text.removeTags(event.getMenuOption() == null ? "" : event.getMenuOption()).toLowerCase(Locale.ROOT);
        String target = Text.removeTags(event.getMenuTarget() == null ? "" : event.getMenuTarget()).toLowerCase(Locale.ROOT);

        if (!"block".equals(option) || !target.contains("cave"))
        {
            return;
        }

        WorldPoint worldPoint = client.getLocalPlayer().getWorldLocation();
        int areaExitPending = client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING);

        log.debug(
                "BA attacker cave debug: option='{}', target='{}', id={}, menuAction={}, param0={}, param1={}, world=({}, {}, {}), regionId={}, regionLocal=({}, {}), areaExitPending={}, currentRole={}",
                event.getMenuOption(),
                event.getMenuTarget(),
                event.getId(),
                event.getMenuAction(),
                event.getParam0(),
                event.getParam1(),
                worldPoint.getX(),
                worldPoint.getY(),
                worldPoint.getPlane(),
                worldPoint.getRegionID(),
                worldPoint.getRegionX(),
                worldPoint.getRegionY(),
                areaExitPending,
                roleDetector.getCurrentRole()
        );
    }
}
