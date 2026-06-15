package com.bahealerorder.attacker;

import com.bahealerorder.BaUtilitiesConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
public class AttackerCaveOverlay extends Overlay
{
    private static final Color TEXT_SHADOW_COLOR = Color.BLACK;
    private static final int OVERLAY_DEBUG_INTERVAL_TICKS = 25;

    /*
     * Normal waves 1-9.
     * Captured from Block -> Penance cave:
     * Ranger cave:  param0=33, param1=79
     * Fighter cave: param0=39, param1=80
     *
     * These defaults are shifted slightly toward the cave mouths compared to the
     * clicked tile, so the text starts centered above the cave instead of over
     * the walkway/player position.
     */
    private static final int NORMAL_RANGER_CAVE_SCENE_X = 34;
    private static final int NORMAL_RANGER_CAVE_SCENE_Y = 81;
    private static final int NORMAL_FIGHTER_CAVE_SCENE_X = 40;
    private static final int NORMAL_FIGHTER_CAVE_SCENE_Y = 82;

    /*
     * Wave 10 is in a different chunk/instance.
     * Temporary tuned placeholders until we capture the real Wave 10 cave scene coords.
     */
    private static final int WAVE_10_RANGER_CAVE_SCENE_X = NORMAL_RANGER_CAVE_SCENE_X + 6;
    private static final int WAVE_10_RANGER_CAVE_SCENE_Y = NORMAL_RANGER_CAVE_SCENE_Y;
    private static final int WAVE_10_FIGHTER_CAVE_SCENE_X = NORMAL_FIGHTER_CAVE_SCENE_X + 6;
    private static final int WAVE_10_FIGHTER_CAVE_SCENE_Y = NORMAL_FIGHTER_CAVE_SCENE_Y;

    private final Client client;
    private final BaUtilitiesConfig config;

    private AttackerController controller;
    private int lastOverlayDebugTick = -OVERLAY_DEBUG_INTERVAL_TICKS;

    @Inject
    private AttackerCaveOverlay(Client client, BaUtilitiesConfig config)
    {
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    void setController(AttackerController controller)
    {
        this.controller = controller;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (controller == null)
        {
            return null;
        }

        boolean shouldRender = controller.shouldRenderSpawnCountOverlay();
        int tick = client.getTickCount();

        if (tick - lastOverlayDebugTick >= OVERLAY_DEBUG_INTERVAL_TICKS)
        {
            lastOverlayDebugTick = tick;

            log.debug(
                    "Attacker cave overlay render: shouldRender={}, currentWave={}, rangerScene=({}, {}), fighterScene=({}, {}), textSize={}, textColor={}, heightOffset={}, horizontalOffset={}",
                    shouldRender,
                    controller.getCurrentWave(),
                    getRangerSceneX(),
                    getRangerSceneY(),
                    getFighterSceneX(),
                    getFighterSceneY(),
                    config.attackerSpawnCountTextSize(),
                    config.attackerSpawnCountTextColor(),
                    config.attackerSpawnCountHeightOffset(),
                    config.attackerSpawnCountHorizontalOffset()
            );
        }

        if (!shouldRender)
        {
            return null;
        }

        renderText(
                graphics,
                getRangerSceneX(),
                getRangerSceneY(),
                controller.getRangersSpawned() + " / " + controller.getRangerTotal()
        );

        renderText(
                graphics,
                getFighterSceneX(),
                getFighterSceneY(),
                controller.getFightersSpawned() + " / " + controller.getFighterTotal()
        );

        return null;
    }

    private int getRangerSceneX()
    {
        return (controller.getCurrentWave() == 10 ? WAVE_10_RANGER_CAVE_SCENE_X : NORMAL_RANGER_CAVE_SCENE_X)
                + config.attackerSpawnCountHorizontalOffset();
    }

    private int getRangerSceneY()
    {
        return controller.getCurrentWave() == 10 ? WAVE_10_RANGER_CAVE_SCENE_Y : NORMAL_RANGER_CAVE_SCENE_Y;
    }

    private int getFighterSceneX()
    {
        return (controller.getCurrentWave() == 10 ? WAVE_10_FIGHTER_CAVE_SCENE_X : NORMAL_FIGHTER_CAVE_SCENE_X)
                + config.attackerSpawnCountHorizontalOffset();
    }

    private int getFighterSceneY()
    {
        return controller.getCurrentWave() == 10 ? WAVE_10_FIGHTER_CAVE_SCENE_Y : NORMAL_FIGHTER_CAVE_SCENE_Y;
    }

    private void renderText(Graphics2D graphics, int sceneX, int sceneY, String text)
    {
        if (text == null)
        {
            return;
        }

        LocalPoint localPoint = LocalPoint.fromScene(sceneX, sceneY, client.getTopLevelWorldView());

        if (localPoint == null)
        {
            log.debug(
                    "Attacker cave overlay local point was null for scene=({}, {}), text='{}'",
                    sceneX,
                    sceneY,
                    text
            );
            return;
        }

        Point textLocation = Perspective.getCanvasTextLocation(
                client,
                graphics,
                localPoint,
                text,
                config.attackerSpawnCountHeightOffset()
        );

        if (textLocation == null)
        {
            log.debug(
                    "Attacker cave overlay canvas text location was null for scene=({}, {}), localPoint={}, text='{}'",
                    sceneX,
                    sceneY,
                    localPoint,
                    text
            );
            return;
        }

        Font originalFont = graphics.getFont();

        graphics.setFont(originalFont.deriveFont(Font.BOLD, config.attackerSpawnCountTextSize()));
        renderOutlinedText(graphics, textLocation, text, config.attackerSpawnCountTextColor());
        graphics.setFont(originalFont);

        log.debug(
                "Attacker cave overlay rendered text='{}' at scene=({}, {}), localPoint={}, canvas={}",
                text,
                sceneX,
                sceneY,
                localPoint,
                textLocation
        );
    }

    private void renderOutlinedText(Graphics2D graphics, Point textLocation, String text, Color textColor)
    {
        int x = textLocation.getX();
        int y = textLocation.getY();

        OverlayUtil.renderTextLocation(graphics, new Point(x - 1, y), text, TEXT_SHADOW_COLOR);
        OverlayUtil.renderTextLocation(graphics, new Point(x + 1, y), text, TEXT_SHADOW_COLOR);
        OverlayUtil.renderTextLocation(graphics, new Point(x, y - 1), text, TEXT_SHADOW_COLOR);
        OverlayUtil.renderTextLocation(graphics, new Point(x, y + 1), text, TEXT_SHADOW_COLOR);

        OverlayUtil.renderTextLocation(graphics, textLocation, text, textColor);
    }
}