package com.bahealerorder;

import com.bahealerorder.codes.HealerCodeStatus;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class BaHealerOrderOverlay extends Overlay
{
	private static final Color TEXT_SHADOW_COLOR = Color.BLACK;
	private static final int TEXT_Z_OFFSET = 60;
	private static final float HULL_STROKE_WIDTH = 2.0f;
	private static final float TILE_STROKE_WIDTH = 1.0f;
	private static final int TILE_ALPHA = 50;

	private final BaHealerOrderPlugin plugin;
	private final BaHealerOrderConfig config;

	@Inject
	private BaHealerOrderOverlay(BaHealerOrderPlugin plugin, BaHealerOrderConfig config)
	{
		this.plugin = plugin;
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<Map.Entry<NPC, Integer>> healers = new ArrayList<>(plugin.getTrackedHealers().entrySet());
		Map<NPC, Integer> xOffsets = getStackedLabelXOffsets(healers);

		for (Map.Entry<NPC, Integer> entry : healers)
		{
			NPC npc = entry.getKey();
			Integer order = entry.getValue();

			if (npc == null || order == null)
			{
				continue;
			}

			int xOffset = xOffsets.getOrDefault(npc, 0);

			renderHighlight(graphics, npc);

			if (config.healerLabelStyle() != BaHealerOrderConfig.HealerLabelStyle.NONE)
			{
				renderNumber(graphics, npc, order, xOffset);
			}

			if (config.showFoodCountOnNpc())
			{
				int foodFed = plugin.getFoodFedByHealerOrder().getOrDefault(order, 0);
				renderFoodCount(graphics, npc, order, foodFed, xOffset);
			}
		}

		return null;
	}

	private Map<NPC, Integer> getStackedLabelXOffsets(List<Map.Entry<NPC, Integer>> healers)
	{
		Map<NPC, Integer> offsets = new HashMap<>();

		if (!config.spreadStackedLabels())
		{
			return offsets;
		}

		Map<WorldPoint, List<Map.Entry<NPC, Integer>>> healersByLocation = new HashMap<>();

		for (Map.Entry<NPC, Integer> entry : healers)
		{
			NPC npc = entry.getKey();

			if (npc == null)
			{
				continue;
			}

			WorldPoint location = npc.getWorldLocation();
			healersByLocation.computeIfAbsent(location, key -> new ArrayList<>()).add(entry);
		}

		for (List<Map.Entry<NPC, Integer>> stack : healersByLocation.values())
		{
			if (stack.size() <= 1)
			{
				continue;
			}

			stack.sort(Comparator.comparingInt(Map.Entry::getValue));

			int stackSize = stack.size();

			for (int i = 0; i < stackSize; i++)
			{
				int xOffset = getStackedLabelXOffset(i, stackSize);
				offsets.put(stack.get(i).getKey(), xOffset);
			}
		}

		return offsets;
	}

	private int getStackedLabelXOffset(int stackIndex, int stackSize)
	{
		return (int) Math.round((stackIndex - ((stackSize - 1) / 2.0)) * config.stackedLabelSpacing());
	}

	private void renderHighlight(Graphics2D graphics, NPC npc)
	{
		if (config.highlightStyle() == BaHealerOrderConfig.HighlightStyle.HULL)
		{
			renderHull(graphics, npc);
			return;
		}

		if (config.highlightStyle() == BaHealerOrderConfig.HighlightStyle.TILE)
		{
			renderTile(graphics, npc);
			return;
		}

		if (config.highlightStyle() == BaHealerOrderConfig.HighlightStyle.TRUE_TILE)
		{
			renderTrueTile(graphics, npc);
		}
	}

	private void renderHull(Graphics2D graphics, NPC npc)
	{
		Shape hull = npc.getConvexHull();

		if (hull == null)
		{
			return;
		}

		Stroke originalStroke = graphics.getStroke();
		Color originalColor = graphics.getColor();

		graphics.setColor(config.hullColor());
		graphics.setStroke(new BasicStroke(HULL_STROKE_WIDTH));
		graphics.draw(hull);

		graphics.setStroke(originalStroke);
		graphics.setColor(originalColor);
	}

	private void renderTile(Graphics2D graphics, NPC npc)
	{
		Polygon tile = Perspective.getCanvasTilePoly(plugin.getClient(), npc.getLocalLocation());

		if (tile == null)
		{
			return;
		}

		renderTilePolygon(graphics, tile);
	}

	private void renderTrueTile(Graphics2D graphics, NPC npc)
	{
		LocalPoint trueTileLocation = LocalPoint.fromWorld(plugin.getClient(), npc.getWorldLocation());

		if (trueTileLocation == null)
		{
			return;
		}

		Polygon tile = Perspective.getCanvasTilePoly(plugin.getClient(), trueTileLocation);

		if (tile == null)
		{
			return;
		}

		renderTilePolygon(graphics, tile);
	}

	private void renderTilePolygon(Graphics2D graphics, Polygon tile)
	{
		Stroke originalStroke = graphics.getStroke();
		Color originalColor = graphics.getColor();

		Color tileColor = config.hullColor();
		Color fillColor = new Color(
				tileColor.getRed(),
				tileColor.getGreen(),
				tileColor.getBlue(),
				TILE_ALPHA
		);

		graphics.setColor(fillColor);
		graphics.fill(tile);

		graphics.setColor(tileColor);
		graphics.setStroke(new BasicStroke(TILE_STROKE_WIDTH));
		graphics.draw(tile);

		graphics.setStroke(originalStroke);
		graphics.setColor(originalColor);
	}

	private void renderNumber(Graphics2D graphics, NPC npc, int order, int xOffset)
	{
		String text = plugin.getHealerLabel(order);

		Point textLocation = npc.getCanvasTextLocation(
				graphics,
				text,
				npc.getLogicalHeight() + TEXT_Z_OFFSET
		);

		if (textLocation == null)
		{
			return;
		}

		Font originalFont = graphics.getFont();

		graphics.setFont(originalFont.deriveFont(Font.BOLD, (float) config.textSize()));
		renderOutlinedText(graphics, offsetPoint(textLocation, xOffset), text, config.textColor());
		graphics.setFont(originalFont);
	}

	private void renderFoodCount(Graphics2D graphics, NPC npc, int healerOrder, int foodFed, int xOffset)
	{
		String text = getFoodCountText(healerOrder, foodFed);

		Point textLocation = npc.getCanvasTextLocation(
				graphics,
				text,
				(npc.getLogicalHeight() / 2) + config.foodCountZOffset()
		);

		if (textLocation == null)
		{
			return;
		}

		Font originalFont = graphics.getFont();

		graphics.setFont(originalFont.deriveFont(Font.BOLD, (float) config.foodCountTextSize()));
		renderOutlinedText(graphics, offsetPoint(textLocation, xOffset), text, getFoodCountColor(healerOrder));
		graphics.setFont(originalFont);
	}

	private String getFoodCountText(int healerOrder, int foodFed)
	{
		HealerCodeStatus status = plugin.getDisplayCodeStatus(healerOrder);
		String codeText = plugin.formatCodeStatus(status);

		if (codeText != null)
		{
			return codeText;
		}

		BaHealerOrderConfig.FoodCountType type = config.foodCountType();

		int expected = plugin.getExpectedFoodForOrder(healerOrder);

		if (expected <= 0)
		{
			return foodFed + "f";
		}

		if (type == BaHealerOrderConfig.FoodCountType.COUNT_UP)
		{
			return foodFed + "/" + expected;
		}

		return String.valueOf(Math.max(expected - foodFed, 0));
	}

	private Color getFoodCountColor(int healerOrder)
	{
		HealerCodeStatus status = plugin.getDisplayCodeStatus(healerOrder);

		if (status != null)
		{
			return plugin.getCodeStatusColor(status.getState());
		}

		return config.foodCountColor();
	}

	private Point offsetPoint(Point point, int xOffset)
	{
		if (xOffset == 0)
		{
			return point;
		}

		return new Point(point.getX() + xOffset, point.getY());
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
