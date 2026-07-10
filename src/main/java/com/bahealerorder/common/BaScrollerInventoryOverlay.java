package com.bahealerorder.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ItemID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

public class BaScrollerInventoryOverlay extends WidgetItemOverlay
{
	private static final int ITEM_FILL_OPACITY = 50;
	private static final Set<Integer> OMEGA_DUPE_ITEM_IDS = new HashSet<>(Arrays.asList(
			ItemID.BOOK_OF_EGG,
			ItemID.BOOK_OF_EGG_29435,
			ItemID.SHRINKMEQUICK,
			ItemID.ELEMENTAL_SHIELD,
			ItemID.MIND_SHIELD
	));

	private final ItemManager itemManager;
	private BaScrollerController controller;

	@Inject
	private BaScrollerInventoryOverlay(ItemManager itemManager)
	{
		this.itemManager = itemManager;
		showOnInventory();
	}

	void setController(BaScrollerController controller)
	{
		this.controller = controller;
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (controller == null || !controller.shouldHighlightOmegaDupeItems() || !OMEGA_DUPE_ITEM_IDS.contains(itemId)) return;

		renderItemHighlight(graphics, itemId, widgetItem, controller.getOmegaDupeItemHighlightColor());
	}

	private void renderItemHighlight(Graphics2D graphics, int itemId, WidgetItem widgetItem, Color color)
	{
		Rectangle bounds = widgetItem.getCanvasBounds();

		Image fill = ImageUtil.fillImage(
				itemManager.getImage(itemId, widgetItem.getQuantity(), false),
				ColorUtil.colorWithAlpha(color, ITEM_FILL_OPACITY)
		);
		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), color);

		graphics.drawImage(fill, (int) bounds.getX(), (int) bounds.getY(), null);
		graphics.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);
	}
}
