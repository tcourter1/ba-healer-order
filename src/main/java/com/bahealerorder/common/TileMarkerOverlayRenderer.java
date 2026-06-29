package com.bahealerorder.common;

import com.bahealerorder.defender.strategies.DefenderMarker;
import com.bahealerorder.defender.strategies.DefenderTile;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.Collection;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;

public final class TileMarkerOverlayRenderer
{
	private static final Color DEFAULT_MARKER_COLOR = new Color(80, 170, 255);

	private TileMarkerOverlayRenderer()
	{
	}

	public static void renderMarkers(Client client, Graphics2D graphics, Iterable<DefenderMarker> markers)
	{
		if (markers == null)
		{
			return;
		}

		for (DefenderMarker marker : markers)
		{
			if (marker == null || marker.getTile() == null)
			{
				continue;
			}

			renderMarker(client, graphics, marker);
		}
	}

	private static void renderMarker(Client client, Graphics2D graphics, DefenderMarker marker)
	{
		for (WorldPoint worldPoint : getWorldPoints(client, marker.getTile()))
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
			renderLabel(client, graphics, localPoint, marker.getLabel(), color);
		}
	}

	private static Collection<WorldPoint> getWorldPoints(Client client, DefenderTile tile)
	{
		WorldPoint worldPoint = WorldPoint.fromRegion(tile.getRegionId(), tile.getRegionX(), tile.getRegionY(), tile.getZ());
		return client.isInInstancedRegion()
				? WorldPoint.toLocalInstance(client, worldPoint)
				: java.util.Collections.singleton(worldPoint);
	}

	private static void renderTile(Graphics2D graphics, Polygon tile, Color color, int opacityPercent, float borderWidth)
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

	private static void renderLabel(Client client, Graphics2D graphics, LocalPoint localPoint, String text, Color color)
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

	private static Color getMarkerColor(DefenderMarker marker)
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

	private static int alphaFromOpacity(int opacityPercent)
	{
		return Math.round(255 * Math.max(0, Math.min(100, opacityPercent)) / 100f);
	}

	private static float clampBorderWidth(float borderWidth)
	{
		return Math.max(0f, Math.min(8f, borderWidth));
	}
}
