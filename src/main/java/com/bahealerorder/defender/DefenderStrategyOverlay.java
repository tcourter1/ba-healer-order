package com.bahealerorder.defender;

import com.bahealerorder.defender.strategies.DefenderMarker;
import com.bahealerorder.defender.strategies.DefenderTile;
import com.bahealerorder.defender.strategies.DefenderWaveStrategy;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemLayer;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class DefenderStrategyOverlay extends Overlay
{
	private static final Color DEFAULT_MARKER_COLOR = new Color(80, 170, 255);
	private static final Color GROUND_ITEM_HIGHLIGHT_COLOR = Color.YELLOW;
	private static final int ITEM_OUTLINE_WIDTH = 2;
	private static final int ITEM_OUTLINE_FEATHER = 2;

	private final Client client;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private DefenderController controller;

	@Inject
	private DefenderStrategyOverlay(Client client, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	void setController(DefenderController controller)
	{
		this.controller = controller;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (controller == null || !controller.shouldShowStrategyTiles())
		{
			return null;
		}

		DefenderWaveStrategy strategy = controller.getCurrentWaveStrategy();
		if (strategy != null)
		{
			for (DefenderMarker marker : strategy.getMarkers())
			{
				if (marker == null || marker.getTile() == null)
				{
					continue;
				}

				renderMarker(graphics, marker);
			}
		}

		for (DefenderGroundItem groundItem : controller.getHighlightedGroundItems())
		{
			renderGroundItem(groundItem);
		}

		return null;
	}

	private void renderMarker(Graphics2D graphics, DefenderMarker marker)
	{
		for (WorldPoint worldPoint : getWorldPoints(marker.getTile()))
		{
			LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
			if (localPoint == null)
			{
				continue;
			}

			Polygon tile = Perspective.getCanvasTilePoly(client, localPoint);
			if (tile == null)
			{
				continue;
			}

			Color color = getMarkerColor(marker);
			renderTile(graphics, tile, color, marker.getOpacityPercentOrDefault(), marker.getBorderWidthOrDefault());
			renderLabel(graphics, localPoint, marker.getLabel(), color);
		}
	}

	private void renderGroundItem(DefenderGroundItem groundItem)
	{
		if (groundItem == null || groundItem.getTile() == null)
		{
			return;
		}

		ItemLayer itemLayer = groundItem.getTile().getItemLayer();

		if (itemLayer == null)
		{
			return;
		}

		modelOutlineRenderer.drawOutline(itemLayer, groundItem.getItem(), ITEM_OUTLINE_WIDTH, GROUND_ITEM_HIGHLIGHT_COLOR, ITEM_OUTLINE_FEATHER);
	}

	private Collection<WorldPoint> getWorldPoints(DefenderTile tile)
	{
		WorldPoint worldPoint = WorldPoint.fromRegion(tile.getRegionId(), tile.getRegionX(), tile.getRegionY(), tile.getZ());
		return client.isInInstancedRegion()
				? WorldPoint.toLocalInstance(client, worldPoint)
				: java.util.Collections.singleton(worldPoint);
	}

	private void renderTile(Graphics2D graphics, Polygon tile, Color color, int opacityPercent, float borderWidth)
	{
		Stroke originalStroke = graphics.getStroke();
		Color originalColor = graphics.getColor();
		Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaFromOpacity(opacityPercent));

		graphics.setColor(fillColor);
		graphics.fill(tile);
		float width = clampBorderWidth(borderWidth);
		if (width > 0)
		{
			graphics.setColor(color);
			graphics.setStroke(new BasicStroke(width));
			graphics.draw(tile);
		}
		graphics.setStroke(originalStroke);
		graphics.setColor(originalColor);
	}

	private void renderLabel(Graphics2D graphics, LocalPoint localPoint, String text, Color color)
	{
		if (text == null || text.trim().isEmpty())
		{
			return;
		}

		Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, text, 0);
		if (textLocation == null)
		{
			return;
		}

		OverlayUtil.renderTextLocation(graphics, textLocation, text, color);
	}

	private Color getMarkerColor(DefenderMarker marker)
	{
		try
		{
			return marker.getColor() == null || marker.getColor().trim().isEmpty()
					? DEFAULT_MARKER_COLOR
					: Color.decode(marker.getColor());
		}
		catch (RuntimeException ex)
		{
			return DEFAULT_MARKER_COLOR;
		}
	}

	private int alphaFromOpacity(int opacityPercent)
	{
		return Math.round(255 * Math.max(0, Math.min(100, opacityPercent)) / 100f);
	}

	private float clampBorderWidth(float borderWidth)
	{
		return Math.max(0f, Math.min(8f, borderWidth));
	}

}
