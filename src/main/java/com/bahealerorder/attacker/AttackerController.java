package com.bahealerorder.attacker;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.BaRoleDetector;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
public class AttackerController
{
    private static final String PENANCE_RANGER_NAME = "Penance Ranger";
    private static final String PENANCE_FIGHTER_NAME = "Penance Fighter";

    private static final Pattern WAVE_START_PATTERN = Pattern.compile(".*\\bwave:\\s*(\\d+)\\b.*");
    private static final Pattern WAVE_PATTERN = Pattern.compile(".*---- Wave: (10|[1-9]) ----.*");

    /*
     * Index 0 is unused so wave numbers can be used directly.
     */
    private static final int[] RANGER_TOTALS_BY_WAVE = {
            0,
            4, 4, 6, 6, 6,
            7, 7, 8, 8, 7
    };

    private static final int[] FIGHTER_TOTALS_BY_WAVE = {
            0,
            4, 5, 5, 6, 6,
            6, 7, 7, 8, 7
    };

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
    private final OverlayManager overlayManager;
    private final AttackerCaveOverlay caveOverlay;

    @Getter
    private int currentWave = -1;

    @Getter
    private int rangersSpawned;

    @Getter
    private int fightersSpawned;

    private int inGameBit;

    private int lastSpawnOverlayDebugTick = -100;

    @Inject
    private AttackerController(
            Client client,
            BaUtilitiesConfig config,
            BaRoleDetector roleDetector,
            OverlayManager overlayManager,
            AttackerCaveOverlay caveOverlay)
    {
        this.client = client;
        this.config = config;
        this.roleDetector = roleDetector;
        this.overlayManager = overlayManager;
        this.caveOverlay = caveOverlay;
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
        if (!isAttackerRole() || !isWaveActive())
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
            rangersSpawned++;
            log.debug("Attacker spawn counter registered Ranger {}/{} for wave {}", rangersSpawned, getRangerTotal(), currentWave);
        }
        else if (PENANCE_FIGHTER_NAME.equals(npcName))
        {
            fightersSpawned++;
            log.debug("Attacker spawn counter registered Fighter {}/{} for wave {}", fightersSpawned, getFighterTotal(), currentWave);
        }
    }

    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

        Matcher waveMatcher = WAVE_PATTERN.matcher(event.getMessage());

        if (waveMatcher.matches())
        {
            try
            {
                startNewWave(Integer.parseInt(waveMatcher.group(1)));
            }
            catch (NumberFormatException ex)
            {
                log.debug("Failed to parse attacker wave number from message: {}", event.getMessage());
            }

            return;
        }

        String message = Text.removeTags(event.getMessage()).toLowerCase(Locale.ROOT);
        Matcher waveStartMatcher = WAVE_START_PATTERN.matcher(message);

        if (!waveStartMatcher.matches())
        {
            return;
        }

        try
        {
            startNewWave(Integer.parseInt(waveStartMatcher.group(1)));
        }
        catch (NumberFormatException ex)
        {
            log.debug("Failed to parse attacker wave start message: {}", message, ex);
        }
    }

    public void onVarbitChanged(VarbitChanged event)
    {
        int currentInGameBit = client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING);

        if (inGameBit == currentInGameBit)
        {
            return;
        }

        inGameBit = currentInGameBit;

        if (currentInGameBit == 0)
        {
            resetWaveState();
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
                    currentWave,
                    rangersSpawned,
                    rangerTotal,
                    fightersSpawned,
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
        return currentWave >= 1 && currentWave <= 10;
    }

    public int getRangerTotal()
    {
        if (currentWave < 1 || currentWave >= RANGER_TOTALS_BY_WAVE.length)
        {
            return 0;
        }

        return RANGER_TOTALS_BY_WAVE[currentWave];
    }

    public int getFighterTotal()
    {
        if (currentWave < 1 || currentWave >= FIGHTER_TOTALS_BY_WAVE.length)
        {
            return 0;
        }

        return FIGHTER_TOTALS_BY_WAVE[currentWave];
    }

    public WorldPoint getRangerCaveLabelTile()
    {
        return currentWave == 10 ? WAVE_10_RANGER_CAVE_LABEL_TILE : NORMAL_RANGER_CAVE_LABEL_TILE;
    }

    public WorldPoint getFighterCaveLabelTile()
    {
        return currentWave == 10 ? WAVE_10_FIGHTER_CAVE_LABEL_TILE : NORMAL_FIGHTER_CAVE_LABEL_TILE;
    }

    private void startNewWave(int wave)
    {
        if (wave < 1 || wave > 10)
        {
            return;
        }

        currentWave = wave;
        rangersSpawned = 0;
        fightersSpawned = 0;

        log.debug(
                "Starting attacker spawn counter for wave {}. Rangers total={}, Fighters total={}",
                currentWave,
                getRangerTotal(),
                getFighterTotal()
        );
    }

    private void resetWaveState()
    {
        currentWave = -1;
        rangersSpawned = 0;
        fightersSpawned = 0;
    }

    private void resetAllState()
    {
        resetWaveState();
        inGameBit = 0;
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