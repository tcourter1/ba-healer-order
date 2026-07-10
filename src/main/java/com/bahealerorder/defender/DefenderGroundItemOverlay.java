package com.bahealerorder.defender;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemLayer;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class DefenderGroundItemOverlay extends Overlay
{
	private static final Color GROUND_ITEM_HIGHLIGHT_COLOR = Color.YELLOW;
	private static final int ITEM_OUTLINE_WIDTH = 2;
	private static final int ITEM_OUTLINE_FEATHER = 2;

	private final Client client;
	private final ModelOutlineRenderer modelOutlineRenderer;
	private DefenderController controller;

	@Inject
	private DefenderGroundItemOverlay(Client client, ModelOutlineRenderer modelOutlineRenderer)
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
		if (controller == null) return null;

		for (DefenderGroundItem groundItem : controller.getHighlightedGroundItems())
		{
			renderGroundItem(groundItem);
		}

		return null;
	}

	private void renderGroundItem(DefenderGroundItem groundItem)
	{
		if (groundItem == null || groundItem.getTile() == null) return;

		ItemLayer itemLayer = groundItem.getTile().getItemLayer();

		if (itemLayer == null) return;

		modelOutlineRenderer.drawOutline(itemLayer, groundItem.getItem(), ITEM_OUTLINE_WIDTH, GROUND_ITEM_HIGHLIGHT_COLOR, ITEM_OUTLINE_FEATHER);
	}

}
