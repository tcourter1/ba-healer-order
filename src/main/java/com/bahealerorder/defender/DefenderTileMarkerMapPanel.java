package com.bahealerorder.defender;

import com.bahealerorder.defender.strategies.DefenderMapLayout;
import com.bahealerorder.defender.strategies.DefenderMarker;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.swing.JPanel;

class DefenderTileMarkerMapPanel extends JPanel
{
	static final Color DEFAULT_MARKER_COLOR = new Color(80, 170, 255);
	static final Color TRAP_COLOR = new Color(190, 70, 70);
	static final Color LOGS_COLOR = new Color(210, 180, 80);
	static final Color HAMMER_COLOR = new Color(145, 105, 55);
	static final Color RUNNER_CAVE_COLOR = new Color(125, 125, 220);
	static final Color HEALER_CAVE_COLOR = new Color(75, 175, 125);
	static final Color QUEEN_TRAPDOOR_COLOR = new Color(120, 80, 45);
	static final Color DISABLED_TILE_COLOR = Color.BLACK;
	static final Color CANNON_HILL_COLOR = new Color(190, 160, 105);

	private static final MapBounds MAP_BOUNDS = new MapBounds(24, 50, 15, 42);
	private static final int CANNON_HILL_X = 38;
	private static final int CANNON_HILL_Y = 24;
	// Decoded from BaSim BarbarianAssaultMap collision flags for this east-side editor slice.
	private static final String[] MAP_MASK_WAVES_1_TO_9 = {
			"##########################",
			"#####...##################",
			"..............############",
			"....................######",
			".....................#####",
			"......................####",
			"......................####",
			"......................####",
			".......................###",
			"........................##",
			"........................##",
			".......................###",
			".......................###",
			".......................###",
			"..............#...#....###",
			"................##......##",
			".........................#",
			"..............#...#......#",
			".........................#",
			".........................#",
			".........................#",
			".........................#",
			".........................#",
			".........................#",
			".........................#",
			".........................#",
			"........................##",
	};
	private static final String[] MAP_MASK_WAVE_10 = {
			"##########################",
			"#####...##################",
			"..............############",
			"....................######",
			".....................#####",
			"......................####",
			".....................#.###",
			".......................###",
			".....................##.##",
			".....................##..#",
			".......................#.#",
			".....................#....",
			"........................##",
			"........................##",
			"..........................",
			"..........................",
			"..........................",
			".........................#",
			"..........................",
			"..........................",
			"..........................",
			"......................#...",
			".........................#",
			"......................##..",
			"...................#..##..",
			"....................#....#",
			"........................##",
	};

	private final IntSupplier waveSupplier;
	private final Supplier<List<DefenderMarker>> markersSupplier;
	private final Supplier<DefenderMarker> selectedMarkerSupplier;
	private final IntSupplier tileSizeSupplier;
	private final BiConsumer<Integer, Integer> tileClicked;

	DefenderTileMarkerMapPanel(
			IntSupplier waveSupplier,
			Supplier<List<DefenderMarker>> markersSupplier,
			Supplier<DefenderMarker> selectedMarkerSupplier,
			IntSupplier tileSizeSupplier,
			BiConsumer<Integer, Integer> tileClicked)
	{
		this.waveSupplier = waveSupplier;
		this.markersSupplier = markersSupplier;
		this.selectedMarkerSupplier = selectedMarkerSupplier;
		this.tileSizeSupplier = tileSizeSupplier;
		this.tileClicked = tileClicked;

		setBackground(new Color(31, 31, 31));
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				int tileSize = getTileSize();
				int x = MAP_BOUNDS.minX + event.getX() / tileSize;
				int y = MAP_BOUNDS.maxY - 1 - event.getY() / tileSize;

				if (MAP_BOUNDS.contains(x, y))
				{
					tileClicked.accept(x, y);
				}
			}
		});
	}

	int getMapWidthPixels()
	{
		return MAP_BOUNDS.width() * getTileSize();
	}

	int getMapHeightPixels()
	{
		return MAP_BOUNDS.height() * getTileSize();
	}

	int getTileSize()
	{
		return tileSizeSupplier.getAsInt();
	}

	void scrollToTrap()
	{
		int tileSize = getTileSize();
		int x = Math.max(0, toScreenX(45) - (tileSize * 4));
		int y = Math.max(0, toScreenY(26) - (tileSize * 4));
		scrollRectToVisible(new Rectangle(x, y, tileSize * 9, tileSize * 9));
	}

	boolean isSelectableMapTile(DefenderMapLayout layout, int mapX, int mapY)
	{
		if (!MAP_BOUNDS.contains(mapX, mapY))
		{
			return false;
		}

		if (isCannonHillTile(mapX, mapY))
		{
			return true;
		}

		String[] rows = layout == DefenderMapLayout.WAVE_10 ? MAP_MASK_WAVE_10 : MAP_MASK_WAVES_1_TO_9;
		int row = MAP_BOUNDS.maxY - 1 - mapY;
		int column = mapX - MAP_BOUNDS.minX;
		return row >= 0
				&& row < rows.length
				&& column >= 0
				&& column < rows[row].length()
				&& rows[row].charAt(column) == '.';
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		DefenderMapLayout layout = DefenderMapLayout.forWave(waveSupplier.getAsInt());
		int tileSize = getTileSize();
		int width = getMapWidthPixels();
		int height = getMapHeightPixels();

		graphics.setColor(new Color(43, 43, 43));
		graphics.fillRect(0, 0, width, height);
		drawGrid(graphics, tileSize, width, height);
		drawDisabledTiles(graphics, layout);
		drawCannonHills(graphics, layout);
		drawLandmarks(graphics, layout);
		drawMarkers(graphics, layout);
	}

	private void drawGrid(Graphics graphics, int tileSize, int width, int height)
	{
		for (int x = MAP_BOUNDS.minX; x <= MAP_BOUNDS.maxX; x++)
		{
			graphics.setColor(x % 5 == 0 ? new Color(82, 82, 82) : new Color(58, 58, 58));
			int screenX = (x - MAP_BOUNDS.minX) * tileSize;
			graphics.drawLine(screenX, 0, screenX, height);
		}

		for (int y = MAP_BOUNDS.minY; y <= MAP_BOUNDS.maxY; y++)
		{
			graphics.setColor(y % 5 == 0 ? new Color(82, 82, 82) : new Color(58, 58, 58));
			int screenY = (MAP_BOUNDS.maxY - y) * tileSize;
			graphics.drawLine(0, screenY, width, screenY);
		}
	}

	private void drawMarkers(Graphics graphics, DefenderMapLayout layout)
	{
		DefenderMarker selected = selectedMarkerSupplier.get();
		for (DefenderMarker marker : markersSupplier.get())
		{
			if (!layout.contains(marker.getTile()) || !MAP_BOUNDS.contains(layout.toMapX(marker.getTile()), layout.toMapY(marker.getTile())))
			{
				continue;
			}

			int x = toScreenX(layout.toMapX(marker.getTile()));
			int y = toScreenY(layout.toMapY(marker.getTile()));
			Color color = parseColor(marker.getColor(), DEFAULT_MARKER_COLOR);
			graphics.setColor(withOpacity(color, marker.getOpacityPercentOrDefault()));
			graphics.fillRect(x, y, getTileSize(), getTileSize());
			drawMarkerBorder(graphics, x, y, color, marker.getBorderWidthOrDefault());

			if (selected != null && selected.getId().equals(marker.getId()))
			{
				graphics.setColor(Color.WHITE);
				graphics.drawRect(x, y, getTileSize() - 1, getTileSize() - 1);
			}

			drawMarkerText(graphics, marker, x, y);
		}
	}

	private void drawMarkerBorder(Graphics graphics, int x, int y, Color color, float borderWidth)
	{
		float width = clampBorderWidth(borderWidth);
		if (width <= 0)
		{
			return;
		}

		Graphics2D graphics2D = (Graphics2D) graphics.create();
		Stroke originalStroke = graphics2D.getStroke();
		graphics2D.setColor(color);
		graphics2D.setStroke(new BasicStroke(width));
		graphics2D.drawRect(x, y, getTileSize() - 1, getTileSize() - 1);
		graphics2D.setStroke(originalStroke);
		graphics2D.dispose();
	}

	private void drawLandmarks(Graphics graphics, DefenderMapLayout layout)
	{
		if (layout == DefenderMapLayout.WAVE_10)
		{
			drawQueenTrapdoor(graphics);
			drawLandmark(graphics, 45, 26, TRAP_COLOR);
			drawLandmark(graphics, 29, 39, LOGS_COLOR);
			drawLandmark(graphics, 30, 38, LOGS_COLOR);
			drawLandmark(graphics, 32, 34, HAMMER_COLOR);
			drawLandmark(graphics, 42, 38, RUNNER_CAVE_COLOR);
			drawLandmark(graphics, 36, 39, HEALER_CAVE_COLOR);
			return;
		}

		drawLandmark(graphics, 45, 26, TRAP_COLOR);
		drawLandmark(graphics, 28, 39, LOGS_COLOR);
		drawLandmark(graphics, 29, 38, LOGS_COLOR);
		drawLandmark(graphics, 32, 34, HAMMER_COLOR);
		drawLandmark(graphics, 36, 39, RUNNER_CAVE_COLOR);
		drawLandmark(graphics, 42, 37, HEALER_CAVE_COLOR);
	}

	private void drawQueenTrapdoor(Graphics graphics)
	{
		drawTileGroupOutline(graphics, QUEEN_TRAPDOOR_COLOR, this::isQueenTrapdoorTile, 27, 35, 20, 28);
	}

	private void drawDisabledTiles(Graphics graphics, DefenderMapLayout layout)
	{
		int tileSize = getTileSize();

		for (int x = MAP_BOUNDS.minX; x < MAP_BOUNDS.maxX; x++)
		{
			for (int y = MAP_BOUNDS.minY; y < MAP_BOUNDS.maxY; y++)
			{
				if (isSelectableMapTile(layout, x, y))
				{
					continue;
				}

				graphics.setColor(DISABLED_TILE_COLOR);
				graphics.fillRect(toScreenX(x), toScreenY(y), tileSize, tileSize);
			}
		}
	}

	private void drawCannonHills(Graphics graphics, DefenderMapLayout layout)
	{
		if (layout == DefenderMapLayout.WAVE_10)
		{
			return;
		}

		drawCannonHill(graphics);
	}

	private void drawCannonHill(Graphics graphics)
	{
		drawTileGroupOutline(
				graphics,
				CANNON_HILL_COLOR,
				this::isCannonHillShapeTile,
				CANNON_HILL_X,
				CANNON_HILL_X + 5,
				CANNON_HILL_Y - 2,
				CANNON_HILL_Y + 4
		);
	}

	private void drawTileGroupOutline(Graphics graphics, Color color, TileGroup group, int minX, int maxX, int minY, int maxY)
	{
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		graphics2D.setColor(color);
		graphics2D.setStroke(new BasicStroke(2f));

		for (int x = minX; x < maxX; x++)
		{
			for (int y = minY; y < maxY; y++)
			{
				if (!group.contains(x, y))
				{
					continue;
				}

				drawPerimeterEdges(graphics2D, group, x, y);
			}
		}

		graphics2D.dispose();
	}

	private void drawPerimeterEdges(Graphics graphics, TileGroup group, int mapX, int mapY)
	{
		int tileSize = getTileSize();
		int x = toScreenX(mapX);
		int y = toScreenY(mapY);

		if (!group.contains(mapX, mapY + 1))
		{
			graphics.drawLine(x, y, x + tileSize, y);
		}

		if (!group.contains(mapX, mapY - 1))
		{
			graphics.drawLine(x, y + tileSize, x + tileSize, y + tileSize);
		}

		if (!group.contains(mapX - 1, mapY))
		{
			graphics.drawLine(x, y, x, y + tileSize);
		}

		if (!group.contains(mapX + 1, mapY))
		{
			graphics.drawLine(x + tileSize, y, x + tileSize, y + tileSize);
		}
	}

	private void drawLandmark(Graphics graphics, int mapX, int mapY, Color color)
	{
		if (!drawFilledTile(graphics, mapX, mapY, color))
		{
			return;
		}

		graphics.setColor(Color.WHITE);
		graphics.drawRect(toScreenX(mapX), toScreenY(mapY), getTileSize() - 1, getTileSize() - 1);
	}

	private boolean drawFilledTile(Graphics graphics, int mapX, int mapY, Color color)
	{
		if (!MAP_BOUNDS.contains(mapX, mapY))
		{
			return false;
		}

		graphics.setColor(color);
		graphics.fillRect(toScreenX(mapX), toScreenY(mapY), getTileSize(), getTileSize());
		return true;
	}

	private void drawMarkerText(Graphics graphics, DefenderMarker marker, int x, int y)
	{
		String text = marker.getLabel();
		if (text == null || text.trim().isEmpty())
		{
			return;
		}

		drawCenteredText(graphics, text.trim(), x, y, getReadableTextColor(parseColor(marker.getColor(), DEFAULT_MARKER_COLOR)));
	}

	private void drawCenteredText(Graphics graphics, String text, int x, int y, Color color)
	{
		int tileSize = getTileSize();
		graphics.setColor(color);
		int textWidth = graphics.getFontMetrics().stringWidth(text);
		int textX = x + Math.max(2, (tileSize - textWidth) / 2);
		int textY = y + ((tileSize - graphics.getFontMetrics().getHeight()) / 2) + graphics.getFontMetrics().getAscent();
		graphics.drawString(text, textX, textY);
	}

	private int toScreenX(int mapX)
	{
		return (mapX - MAP_BOUNDS.minX) * getTileSize();
	}

	private int toScreenY(int mapY)
	{
		return (MAP_BOUNDS.maxY - mapY - 1) * getTileSize();
	}

	private boolean isCannonHillTile(int mapX, int mapY)
	{
		return DefenderMapLayout.forWave(waveSupplier.getAsInt()) != DefenderMapLayout.WAVE_10
				&& isCannonHillShapeTile(mapX, mapY);
	}

	private boolean isCannonHillShapeTile(int mapX, int mapY)
	{
		return mapX >= CANNON_HILL_X && mapX < CANNON_HILL_X + 5 && mapY >= CANNON_HILL_Y && mapY < CANNON_HILL_Y + 4
				|| mapX >= CANNON_HILL_X + 1 && mapX < CANNON_HILL_X + 4 && mapY >= CANNON_HILL_Y - 2 && mapY < CANNON_HILL_Y;
	}

	private boolean isQueenTrapdoorTile(int mapX, int mapY)
	{
		return mapX >= 27 && mapX < 35 && mapY >= 20 && mapY < 28;
	}

	private static Color parseColor(String color, Color fallback)
	{
		try
		{
			return color == null || color.trim().isEmpty() ? fallback : Color.decode(color);
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
	}

	private static Color withOpacity(Color color, int opacityPercent)
	{
		int alpha = Math.round(255 * Math.max(0, Math.min(100, opacityPercent)) / 100f);
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private static float clampBorderWidth(float borderWidth)
	{
		return Math.max(0f, Math.min(8f, borderWidth));
	}

	private static Color getReadableTextColor(Color background)
	{
		return (background.getRed() * 299 + background.getGreen() * 587 + background.getBlue() * 114) / 1000 > 140
				? Color.BLACK
				: Color.WHITE;
	}

	private static class MapBounds
	{
		private final int minX;
		private final int maxX;
		private final int minY;
		private final int maxY;

		private MapBounds(int minX, int maxX, int minY, int maxY)
		{
			this.minX = minX;
			this.maxX = maxX;
			this.minY = minY;
			this.maxY = maxY;
		}

		private int width()
		{
			return maxX - minX;
		}

		private int height()
		{
			return maxY - minY;
		}

		private boolean contains(int x, int y)
		{
			return x >= minX && x < maxX && y >= minY && y < maxY;
		}
	}

	private interface TileGroup
	{
		boolean contains(int mapX, int mapY);
	}
}
