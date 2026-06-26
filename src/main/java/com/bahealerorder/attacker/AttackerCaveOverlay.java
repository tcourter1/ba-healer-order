package com.bahealerorder.attacker;

import com.bahealerorder.BaUtilitiesConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
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
    private static final String PENANCE_CAVE_NAME = "penance cave";
    private static final int CAVE_LABEL_SCENE_Y_OFFSET = 2;

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

        List<TileObject> caves = findPenanceCaves();

        renderText(
                graphics,
                caves.size() > 0 ? toLabelLocalPoint(caves.get(0)) : null,
                getRangerSceneX(),
                getRangerSceneY(),
                controller.getRangersSpawned() + " / " + controller.getRangerTotal()
        );

        renderText(
                graphics,
                caves.size() > 1 ? toLabelLocalPoint(caves.get(1)) : null,
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

    private List<TileObject> findPenanceCaves()
    {
        Scene scene = client.getTopLevelWorldView().getScene();
        Tile[][][] tiles = scene.getTiles();
        Tile[][] planeTiles = tiles[client.getTopLevelWorldView().getPlane()];
        List<TileObject> caves = new ArrayList<>();
        Set<Long> seenCaves = new HashSet<>();

        for (Tile[] column : planeTiles)
        {
            for (Tile tile : column)
            {
                if (tile == null)
                {
                    continue;
                }

                TileObject cave = getPenanceCave(tile);

                if (cave == null)
                {
                    continue;
                }

                if (!seenCaves.add(cave.getHash()))
                {
                    continue;
                }

                caves.add(cave);
            }
        }

        caves.sort(Comparator
                .comparingInt((TileObject cave) -> cave.getLocalLocation().getSceneX())
                .thenComparingInt(cave -> cave.getLocalLocation().getSceneY()));
        return caves;
    }

    private TileObject getPenanceCave(Tile tile)
    {
        for (TileObject object : new TileObject[]{tile.getWallObject(), tile.getDecorativeObject(), tile.getGroundObject()})
        {
            TileObject cave = getPenanceCave(object);

            if (cave != null)
            {
                return cave;
            }
        }

        for (TileObject object : tile.getGameObjects())
        {
            TileObject cave = getPenanceCave(object);

            if (cave != null)
            {
                return cave;
            }
        }

        return null;
    }

    private TileObject getPenanceCave(TileObject object)
    {
        if (object == null)
        {
            return null;
        }

        ObjectComposition objectComposition = client.getObjectDefinition(object.getId());

        if (objectComposition == null || objectComposition.getName() == null
                || !PENANCE_CAVE_NAME.equals(objectComposition.getName().toLowerCase(Locale.ROOT)))
        {
            return null;
        }

        return object;
    }

    private LocalPoint toLabelLocalPoint(TileObject cave)
    {
        return cave.getLocalLocation().plus(
                config.attackerSpawnCountHorizontalOffset() << Perspective.LOCAL_COORD_BITS,
                CAVE_LABEL_SCENE_Y_OFFSET << Perspective.LOCAL_COORD_BITS
        );
    }

    private void renderText(Graphics2D graphics, LocalPoint localPoint, int fallbackSceneX, int fallbackSceneY, String text)
    {
        if (text == null)
        {
            return;
        }

        if (localPoint == null)
        {
            localPoint = LocalPoint.fromScene(fallbackSceneX, fallbackSceneY, client.getTopLevelWorldView());
        }

        if (localPoint == null)
        {
            log.debug(
                    "Attacker cave overlay local point was null for scene=({}, {}), text='{}'",
                    fallbackSceneX,
                    fallbackSceneY,
                    text
            );
            return;
        }

        Font originalFont = graphics.getFont();
        graphics.setFont(originalFont.deriveFont(Font.BOLD, config.attackerSpawnCountTextSize()));

        try
        {
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
                        fallbackSceneX,
                        fallbackSceneY,
                        localPoint,
                        text
                );
                return;
            }

            renderOutlinedText(graphics, textLocation, text, config.attackerSpawnCountTextColor());

            log.debug(
                    "Attacker cave overlay rendered text='{}' at scene=({}, {}), localPoint={}, canvas={}",
                    text,
                    fallbackSceneX,
                    fallbackSceneY,
                    localPoint,
                    textLocation
            );
        }
        finally
        {
            graphics.setFont(originalFont);
        }
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
