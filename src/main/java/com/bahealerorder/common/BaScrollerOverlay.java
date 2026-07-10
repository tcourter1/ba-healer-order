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
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class BaScrollerOverlay extends Overlay
{
	private static final String LADDER_NAME = "ladder";
	private static final int DEBUG_INTERVAL_TICKS = 50;
	private static final int BA_LOBBY_REGION_ID = 10322;
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
			new BaRoom(1, 0, 0),
			new BaRoom(2, WAVE_2_X_OFFSET, 0),
			new BaRoom(3, WAVE_3_X_OFFSET, 0),
			new BaRoom(4, WAVE_4_X_OFFSET, 0),
			new BaRoom(5, 0, -ROOM_ROW_STRIDE),
			new BaRoom(6, WAVE_2_X_OFFSET, -ROOM_ROW_STRIDE),
			new BaRoom(7, WAVE_3_X_OFFSET, -ROOM_ROW_STRIDE),
			new BaRoom(8, WAVE_4_X_OFFSET, -ROOM_ROW_STRIDE),
			new BaRoom(9, 0, -ROOM_ROW_STRIDE * 2),
			new BaRoom(10, WAVE_2_X_OFFSET, -ROOM_ROW_STRIDE * 2)
	};

	private final Client client;

	private BaScrollerController controller;
	private int lastDebugTick = -DEBUG_INTERVAL_TICKS;

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

		LadderScan scan = scanCurrentRoom();

		if (shouldLogDebug(scan))
		{
			lastDebugTick = client.getTickCount();
			log.debug("BA scroller ladder overlay: {}", describeScan(scan));
		}

		if (scan.selectedLadder == null) return null;

		renderLadderHighlight(graphics, scan.selectedLadder);
		return null;
	}

	String getDebugSummary()
	{
		return describeScan(scanCurrentRoom());
	}

	private LadderScan scanCurrentRoom()
	{
		WorldPoint playerLocation = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getWorldLocation();
		BaRoom room = findRoom(playerLocation);

		if (room == null) return new LadderScan(playerLocation, null, null, new ArrayList<>());

		List<TileObject> ladders = findRoomLadders(room);
		TileObject selectedLadder = ladders.isEmpty() ? null : ladders.get(0);

		return new LadderScan(playerLocation, room, selectedLadder, ladders);
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

	private boolean shouldLogDebug(LadderScan scan)
	{
		int tick = client.getTickCount();

		if (tick - lastDebugTick < DEBUG_INTERVAL_TICKS) return false;

		return scan.room != null || scan.isPlayerInBaLobby();
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

	private String describeScan(LadderScan scan)
	{
		return "playerWorld=" + formatWorldPoint(scan.playerLocation)
				+ ", room=" + scan.room
				+ ", selected=" + describeObject(scan.selectedLadder)
				+ ", roomLadders=" + describeObjects(scan.roomLadders);
	}

	private String describeObjects(List<TileObject> objects)
	{
		if (objects.isEmpty()) return "[]";

		StringBuilder builder = new StringBuilder("[");

		for (int i = 0; i < objects.size(); i++)
		{
			if (i > 0)
			{
				builder.append(", ");
			}

			builder.append(describeObject(objects.get(i)));
		}

		return builder.append("]").toString();
	}

	private String describeObject(TileObject object)
	{
		if (object == null) return "null";

		return String.format(
				Locale.ROOT,
				"{id=%d,name='%s',world=%s}",
				object.getId(),
				getObjectName(object),
				formatWorldPoint(object.getWorldLocation())
		);
	}

	private String formatWorldPoint(WorldPoint worldPoint)
	{
		if (worldPoint == null) return "null";

		return "(" + worldPoint.getX() + ", " + worldPoint.getY() + ", " + worldPoint.getPlane() + ")";
	}

	private static final class BaRoom
	{
		private final int wave;
		private final WorldArea area;
		private final int northY;

		private BaRoom(int wave, int northwestXOffset, int northwestYOffset)
		{
			int westX = WAVE_1_NORTHWEST_X + northwestXOffset;
			int northY = WAVE_1_NORTHWEST_Y + northwestYOffset;

			this.wave = wave;
			this.northY = northY;
			this.area = new WorldArea(westX, northY - ROOM_SIZE + 1, ROOM_SIZE, ROOM_SIZE, 0);
		}

		private boolean contains(WorldPoint point)
		{
			return point != null && area.contains(point);
		}

		@Override
		public String toString()
		{
			return "wave=" + wave
					+ ", bounds=(west=" + area.getX()
					+ ", north=" + northY
					+ ", east="
					+ (area.getX() + area.getWidth() - 1) + ", "
					+ "south=" + area.getY() + ")";
		}
	}

	private static final class LadderScan
	{
		private final WorldPoint playerLocation;
		private final BaRoom room;
		private final TileObject selectedLadder;
		private final List<TileObject> roomLadders;

		private LadderScan(
				WorldPoint playerLocation,
				BaRoom room,
				TileObject selectedLadder,
				List<TileObject> roomLadders)
		{
			this.playerLocation = playerLocation;
			this.room = room;
			this.selectedLadder = selectedLadder;
			this.roomLadders = roomLadders;
		}

		private boolean isPlayerInBaLobby()
		{
			return playerLocation != null && playerLocation.getRegionID() == BA_LOBBY_REGION_ID;
		}
	}
}
