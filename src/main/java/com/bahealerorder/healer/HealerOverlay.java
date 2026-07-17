package com.bahealerorder.healer;

import com.bahealerorder.BaUtilitiesConfig;

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
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class HealerOverlay extends Overlay
{
	private static final Color TEXT_SHADOW_COLOR = Color.BLACK;
	private static final int HEALER_LABEL_Z_OFFSET = 95;
	private static final float HULL_STROKE_WIDTH = 2.0f;
	private static final float TILE_STROKE_WIDTH = 1.0f;
	private static final int TILE_ALPHA = 50;
	private static final int TTK_Y_OFFSET = 33;
	private static final int HEALER_LABEL_TEXT_SIZE = 16;
	private static final int STACKED_LABEL_SPACING = 28;
	private static final int FOOD_COUNT_TEXT_SIZE = 16;
	private static final int FOOD_COUNT_Z_OFFSET = 35;

	private HealerController controller;
	private final BaUtilitiesConfig config;

	@Inject
	private HealerOverlay(BaUtilitiesConfig config)
	{
		this.config = config;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	void setController(HealerController controller)
	{
		this.controller = controller;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (controller == null) return null;

		List<Map.Entry<NPC, Integer>> healers = new ArrayList<>(controller.getTrackedHealers().entrySet());
		Map<NPC, Integer> xOffsets = getStackedLabelXOffsets(healers);
		Map<Integer, Integer> foodFedByHealerOrder = controller.getFoodFedByHealerOrder();

		for (Map.Entry<NPC, Integer> entry : healers)
		{
			NPC npc = entry.getKey();
			Integer order = entry.getValue();

			if (npc == null || order == null) continue;

			if (controller.shouldHideDeadNpc(npc)) continue;

			int xOffset = xOffsets.getOrDefault(npc, 0);

			if (controller.shouldShowHealerHighlights())
			{
				renderHighlight(graphics, npc);
			}

			if (controller.shouldShowLabels()
					&& config.healerLabelStyle() != BaUtilitiesConfig.HealerLabelStyle.NONE)
			{
				renderNumber(graphics, npc, order, xOffset);
			}

			if (controller.shouldShowFoodCountOnNpc())
			{
				int foodFed = foodFedByHealerOrder.getOrDefault(order, 0);
				renderFoodCount(graphics, npc, order, foodFed, xOffset);
			}

			if (controller.shouldShowHealerTtk())
			{
				String ttkText = controller.getHealerTtkText(npc);

				if (ttkText != null)
				{
					renderHealerTtk(graphics, npc, ttkText, xOffset, controller.shouldShowFoodCountOnNpc());
				}
			}
		}

		return null;
	}

	private Map<NPC, Integer> getStackedLabelXOffsets(List<Map.Entry<NPC, Integer>> healers)
	{
		Map<NPC, Integer> offsets = new HashMap<>();

		if (!config.spreadStackedLabels()) return offsets;

		Map<WorldPoint, List<Map.Entry<NPC, Integer>>> healersByLocation = new HashMap<>();

		for (Map.Entry<NPC, Integer> entry : healers)
		{
			NPC npc = entry.getKey();

			if (npc == null || controller.shouldHideDeadNpc(npc)) continue;

			WorldPoint location = npc.getWorldLocation();
			healersByLocation.computeIfAbsent(location, key -> new ArrayList<>()).add(entry);
		}

		for (List<Map.Entry<NPC, Integer>> stack : healersByLocation.values())
		{
			if (stack.size() <= 1) continue;

			stack.sort(Comparator.comparingInt(Map.Entry::getValue));

			int stackSize = stack.size();

			for (int i = 0; i < stackSize; i++)
			{
				int xOffset = (int) Math.round((i - ((stackSize - 1) / 2.0)) * STACKED_LABEL_SPACING);
				offsets.put(stack.get(i).getKey(), xOffset);
			}
		}

		return offsets;
	}

	private void renderHighlight(Graphics2D graphics, NPC npc)
	{
		if (config.highlightStyle() == BaUtilitiesConfig.HighlightStyle.HULL)
		{
			renderHull(graphics, npc);
			return;
		}

		if (config.highlightStyle() == BaUtilitiesConfig.HighlightStyle.TILE)
		{
			renderTile(graphics, npc);
			return;
		}

		if (config.highlightStyle() == BaUtilitiesConfig.HighlightStyle.TRUE_TILE)
		{
			renderTrueTile(graphics, npc);
		}
	}

	private void renderHull(Graphics2D graphics, NPC npc)
	{
		Shape hull = npc.getConvexHull();

		if (hull == null) return;

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
		Polygon tile = Perspective.getCanvasTilePoly(controller.getClient(), npc.getLocalLocation());

		if (tile == null) return;

		renderTilePolygon(graphics, tile);
	}

	private void renderTrueTile(Graphics2D graphics, NPC npc)
	{
		LocalPoint trueTileLocation = LocalPoint.fromWorld(controller.getClient(), npc.getWorldLocation());

		if (trueTileLocation == null) return;

		Polygon tile = Perspective.getCanvasTilePoly(controller.getClient(), trueTileLocation);

		if (tile == null) return;

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
		String text = controller.getHealerLabel(order);

		Point textLocation = npc.getCanvasTextLocation(
				graphics,
				text,
				npc.getLogicalHeight() + HEALER_LABEL_Z_OFFSET
		);

		if (textLocation == null) return;

		Font originalFont = graphics.getFont();

		graphics.setFont(FontManager.getFallbackFont(
				originalFont.getFamily(), Font.BOLD, HEALER_LABEL_TEXT_SIZE));
		renderOutlinedText(graphics, offsetPoint(textLocation, xOffset), text, config.hullColor());
		graphics.setFont(originalFont);
	}

	private void renderFoodCount(Graphics2D graphics, NPC npc, int healerOrder, int foodFed, int xOffset)
	{
		String text = getFoodCountText(healerOrder, foodFed);

		Point textLocation = npc.getCanvasTextLocation(
				graphics,
				text,
				(npc.getLogicalHeight() / 2) + config.foodCountHeight()
		);

		if (textLocation == null) return;

		Font originalFont = graphics.getFont();

		graphics.setFont(FontManager.getFallbackFont(
				originalFont.getFamily(), Font.BOLD, FOOD_COUNT_TEXT_SIZE));
		renderOutlinedText(graphics, offsetPoint(textLocation, xOffset), text, controller.getFoodCountColor(healerOrder, foodFed));
		graphics.setFont(originalFont);
	}

	private void renderHealerTtk(Graphics2D graphics, NPC npc, String text, int xOffset, boolean hasFoodCount)
	{
		int zOffset = (npc.getLogicalHeight() / 2) + FOOD_COUNT_Z_OFFSET;

		if (hasFoodCount)
		{
			zOffset -= FOOD_COUNT_TEXT_SIZE + 4;
		}

		Point textLocation = npc.getCanvasTextLocation(graphics, text, zOffset);

		if (textLocation == null) return;

		Font originalFont = graphics.getFont();

		graphics.setFont(FontManager.getFallbackFont(
				originalFont.getFamily(), Font.BOLD, FOOD_COUNT_TEXT_SIZE));
		renderOutlinedText(graphics, offsetPoint(textLocation, xOffset, TTK_Y_OFFSET), text, controller.getHealerTtkColor());
		graphics.setFont(originalFont);
	}

	private String getFoodCountText(int healerOrder, int foodFed)
	{
		return controller.getFoodCountText(healerOrder, foodFed);
	}

	private Point offsetPoint(Point point, int xOffset)
	{
		return offsetPoint(point, xOffset, 0);
	}

	private Point offsetPoint(Point point, int xOffset, int yOffset)
	{
		if (xOffset == 0 && yOffset == 0) return point;

		return new Point(point.getX() + xOffset, point.getY() + yOffset);
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
