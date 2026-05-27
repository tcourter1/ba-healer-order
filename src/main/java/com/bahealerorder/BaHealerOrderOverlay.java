package com.bahealerorder;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;


public class BaHealerOrderOverlay extends Overlay
{
	private static final Color TEXT_SHADOW_COLOR = Color.BLACK;
	private static final int TEXT_Z_OFFSET = 60;
	private static final float HULL_STROKE_WIDTH = 2.0f;
	private static final int FOOD_COUNT_Z_OFFSET = 30;

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
		/*
		 * Copy entries before rendering so the overlay is safe if the map changes
		 * during an event.
		 */
		for (Map.Entry<NPC, Integer> entry : new ArrayList<>(plugin.getTrackedHealers().entrySet()))
		{
			NPC npc = entry.getKey();
			Integer order = entry.getValue();

			if (npc == null || order == null)
			{
				continue;
			}

			renderHull(graphics, npc);
			renderNumber(graphics, npc, order);

			// Optionally render food count on NPC if enabled in config
			if (config.showFoodCountOnNpc())
			{
				int foodFed = plugin.getFoodFedByHealerOrder().getOrDefault(order, 0);
				renderFoodCount(graphics, npc, foodFed);
			}
		}

		return null;
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

	private void renderNumber(Graphics2D graphics, NPC npc, int order)
	{
		String text = String.valueOf(order);

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

		renderOutlinedText(graphics, textLocation, text, config.textColor());

		graphics.setFont(originalFont);
	}

	private void renderFoodCount(Graphics2D graphics, NPC npc, int foodFed)
	{
		String text = foodFed + " fed";

		Point textLocation = npc.getCanvasTextLocation(
				graphics,
				text,
				npc.getLogicalHeight() + FOOD_COUNT_Z_OFFSET
		);

		if (textLocation == null)
		{
			return;
		}

		Font originalFont = graphics.getFont();

		graphics.setFont(originalFont.deriveFont(Font.BOLD, (float) config.textSize()));

		renderOutlinedText(graphics, textLocation, text, config.foodCountColor());

		graphics.setFont(originalFont);
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