package com.bahealerorder.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class BaScrollerOverlay extends Overlay
{
	private static final String LADDER_NAME = "ladder";
	private static final int ROOM_SIZE = 8;
	private static final int WAVE_1_NORTHWEST_X = 2576;
	private static final int WAVE_1_NORTHWEST_Y = 5298;
	private static final int MIDDLE_ROOM_GAP_SIZE = 3;
	private static final int ROW_GAP_SIZE = 2;
	private static final int ROOM_ROW_STRIDE = ROOM_SIZE + ROW_GAP_SIZE;
	private static final int WAVE_2_X_OFFSET = ROOM_SIZE;
	private static final int WAVE_3_X_OFFSET = ROOM_SIZE * 2 + MIDDLE_ROOM_GAP_SIZE;
	private static final int WAVE_4_X_OFFSET = ROOM_SIZE * 3 + MIDDLE_ROOM_GAP_SIZE;
	private static final Color LADDER_HIGHLIGHT_COLOR = new Color(255, 225, 0);
	private static final Stroke LADDER_HIGHLIGHT_STROKE = new BasicStroke(2.0f);

	private static final BaRoom[] BA_ROOMS = {
			new BaRoom(0, 0),
			new BaRoom(WAVE_2_X_OFFSET, 0),
			new BaRoom(WAVE_3_X_OFFSET, 0),
			new BaRoom(WAVE_4_X_OFFSET, 0),
			new BaRoom(0, -ROOM_ROW_STRIDE),
			new BaRoom(WAVE_2_X_OFFSET, -ROOM_ROW_STRIDE),
			new BaRoom(WAVE_3_X_OFFSET, -ROOM_ROW_STRIDE),
			new BaRoom(WAVE_4_X_OFFSET, -ROOM_ROW_STRIDE),
			new BaRoom(0, -ROOM_ROW_STRIDE * 2),
			new BaRoom(WAVE_2_X_OFFSET, -ROOM_ROW_STRIDE * 2)
	};

	private final Client client;

	private BaScrollerController controller;

	@Inject
	private BaScrollerOverlay(Client client)
	{
		this.client = client;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	void setController(BaScrollerController controller)
	{
		this.controller = controller;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (controller == null || !controller.shouldHighlightCurrentRoomLadder()) return null;

		TileObject selectedLadder = findCurrentRoomLadder();

		if (selectedLadder == null) return null;

		renderLadderHighlight(graphics, selectedLadder);
		return null;
	}

	private TileObject findCurrentRoomLadder()
	{
		WorldPoint playerLocation = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getWorldLocation();
		BaRoom room = findRoom(playerLocation);

		if (room == null) return null;

		List<TileObject> ladders = findRoomLadders(room);
		return ladders.isEmpty() ? null : ladders.get(0);
	}

	private BaRoom findRoom(WorldPoint playerLocation)
	{
		if (playerLocation == null) return null;

		for (BaRoom room : BA_ROOMS)
		{
			if (room.contains(playerLocation)) return room;
		}

		return null;
	}

	private List<TileObject> findRoomLadders(BaRoom room)
	{
		WorldView worldView = client.getTopLevelWorldView();

		if (worldView == null) return new ArrayList<>();

		Scene scene = worldView.getScene();

		if (scene == null) return new ArrayList<>();

		Tile[][][] tiles = scene.getTiles();
		int plane = worldView.getPlane();

		if (tiles == null || plane < 0 || plane >= tiles.length || tiles[plane] == null) return new ArrayList<>();

		List<TileObject> ladders = new ArrayList<>();

		for (Tile[] column : tiles[plane])
		{
			if (column == null) continue;

			for (Tile tile : column)
			{
				if (tile == null) continue;

				addRoomLadder(tile.getWallObject(), room, ladders);
				addRoomLadder(tile.getDecorativeObject(), room, ladders);
				addRoomLadder(tile.getGroundObject(), room, ladders);

				for (TileObject object : tile.getGameObjects())
				{
					addRoomLadder(object, room, ladders);
				}
			}
		}

		ladders.sort(Comparator
				.comparingInt((TileObject ladder) -> ladder.getWorldLocation().getY())
				.thenComparingInt(ladder -> ladder.getWorldLocation().getX())
				.thenComparingInt(TileObject::getId));

		return ladders;
	}

	private void addRoomLadder(TileObject object, BaRoom room, List<TileObject> ladders)
	{
		if (object == null || !room.contains(object.getWorldLocation()) || !isNamedLadder(object)) return;

		ladders.add(object);
	}

	private boolean isNamedLadder(TileObject object)
	{
		String name = getObjectName(object);
		return name != null && LADDER_NAME.equals(name.toLowerCase(Locale.ROOT));
	}

	private String getObjectName(TileObject object)
	{
		ObjectComposition composition = client.getObjectDefinition(object.getId());
		return composition == null ? null : composition.getName();
	}

	private void renderLadderHighlight(Graphics2D graphics, TileObject ladder)
	{
		Shape clickbox = ladder.getClickbox();

		if (clickbox != null)
		{
			renderShapeOutline(graphics, clickbox);
			return;
		}

		Polygon tile = ladder.getCanvasTilePoly();

		if (tile != null)
		{
			renderShapeOutline(graphics, tile);
		}
	}

	private void renderShapeOutline(Graphics2D graphics, Shape shape)
	{
		Color originalColor = graphics.getColor();
		Stroke originalStroke = graphics.getStroke();

		graphics.setColor(LADDER_HIGHLIGHT_COLOR);
		graphics.setStroke(LADDER_HIGHLIGHT_STROKE);
		graphics.draw(shape);

		graphics.setColor(originalColor);
		graphics.setStroke(originalStroke);
	}

	private static final class BaRoom
	{
		private final WorldArea area;

		private BaRoom(int northwestXOffset, int northwestYOffset)
		{
			int westX = WAVE_1_NORTHWEST_X + northwestXOffset;
			int northY = WAVE_1_NORTHWEST_Y + northwestYOffset;

			this.area = new WorldArea(westX, northY - ROOM_SIZE + 1, ROOM_SIZE, ROOM_SIZE, 0);
		}

		private boolean contains(WorldPoint point)
		{
			return point != null && area.contains(point);
		}

	}
}
