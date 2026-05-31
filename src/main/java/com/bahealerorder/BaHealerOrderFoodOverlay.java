package com.bahealerorder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        panelComponent.setPreferredSize(new Dimension(220, 0));
        panelComponent.setBackgroundColor(PANEL_BACKGROUND);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showFoodPanel() || !plugin.isWaveActive())
        {
            return null;
        }

        panelComponent.getChildren().clear();

        List<Integer> healerOrders = plugin.getHealerOrderByNpcIndex()
                .values()
                .stream()
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        Map<Integer, Integer> foodFedByHealerOrder = plugin.getFoodFedByHealerOrder();
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
            int foodFed = foodFedByHealerOrder.getOrDefault(healerOrder, 0);
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

        addCurrentWaveCode();

        return super.render(graphics);
    }

    private void addCurrentWaveCode()
    {
        String sourceText = plugin.getCurrentWaveCodeSource();

        if (sourceText == null || sourceText.isEmpty())
        {
            return;
        }

        panelComponent.getChildren().add(
            LineComponent.builder()
                    .left("")
                    .build()
        );

        panelComponent.getChildren().add(
            TitleComponent.builder()
                    .text(getCurrentCodeTitle())
                    .color(TITLE_COLOR)
                    .build()
        );

        for (String line : sourceText.split("\\r?\\n", -1))
        {
            panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(line.isEmpty() ? " " : line)
                        .leftColor(TEXT_COLOR)
                        .build()
            );
        }
    }

    private String getCurrentCodeTitle()
    {
        String codeName = plugin.getCurrentWaveCodeName();

        if (codeName == null || codeName.trim().isEmpty())
        {
            return "Current Code";
        }

        return "Current Code (" + codeName + ")";
    }

}
