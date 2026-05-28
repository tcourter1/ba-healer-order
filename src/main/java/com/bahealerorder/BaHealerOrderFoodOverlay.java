package com.bahealerorder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class BaHealerOrderFoodOverlay extends OverlayPanel
{
    private static final Color PANEL_BACKGROUND = new Color(0, 80, 0, 120);
    private static final Color TITLE_COLOR = new Color(0, 255, 0);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color COUNT_COLOR = new Color(0, 255, 0);

    private final BaHealerOrderPlugin plugin;
    private final BaHealerOrderConfig config;

    @Inject
    private BaHealerOrderFoodOverlay(BaHealerOrderPlugin plugin, BaHealerOrderConfig config)
    {
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(140, 0));
        panelComponent.setBackgroundColor(PANEL_BACKGROUND);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showFoodPanel())
        {
            return null;
        }

        // Ensure the config display fields reflect the currently selected list
        plugin.syncDisplayWithSelected();

        panelComponent.getChildren().clear();

        List<Integer> healerOrders = plugin.getHealerOrderByNpcIndex()
                .values()
                .stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        if (healerOrders.isEmpty())
        {
            return null;
        }

        String title = "BA Healer Order";
        int wave = plugin.getCurrentWave();
        if (wave > 0)
        {
            title += " - Wave " + wave;
        }

        panelComponent.getChildren().add(
            TitleComponent.builder()
                    .text(title)
                    .color(TITLE_COLOR)
                    .build()
        );

        for (int healerOrder : healerOrders)
        {
                int foodFed = plugin.getFoodFedByHealerOrder().getOrDefault(healerOrder, 0);
                int expected = plugin.getExpectedFoodForOrder(healerOrder);

                String rightText = expected > 0 ? (foodFed + "/" + expected + " fed") : (foodFed + " fed");

                panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("#" + healerOrder)
                            .leftColor(TEXT_COLOR)
                            .right(rightText)
                            .rightColor(COUNT_COLOR)
                            .build()
                );
        }

        return super.render(graphics);
    }
}