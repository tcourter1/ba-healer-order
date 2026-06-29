package com.bahealerorder.tilemarkers;

import com.bahealerorder.common.TileMarkerOverlayRenderer;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
public class GeneralTileMarkerOverlay extends Overlay
{
	private final Client client;
	private GeneralTileMarkerController controller;

	@Inject
	private GeneralTileMarkerOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	void setController(GeneralTileMarkerController controller)
	{
		this.controller = controller;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (controller == null || !controller.shouldShowTileMarkers())
		{
			return null;
		}

		TileMarkerOverlayRenderer.renderMarkers(client, graphics, controller.getCurrentMarkers());

		return null;
	}
}
