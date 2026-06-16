package com.bahealerorder.healer;

import com.bahealerorder.BaUtilitiesConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

public class HealerFoodOverlay extends OverlayPanel implements MouseListener
{
    private static final Color PANEL_BACKGROUND = new Color(0, 80, 0, 120);
    private static final Color TITLE_COLOR = new Color(0, 255, 0);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color TTK_COLOR = Color.ORANGE;
    private static final Color DEAD_COLOR = Color.GRAY;
    private static final int MAX_HEALER_COLUMNS = 4;
    private static final int COLUMN_LABEL_WIDTH = 44;
    private static final int COLUMN_CELL_WIDTH = 60;
    private static final int ROW_LABEL_WIDTH = 60;
    private static final int ROW_CELL_WIDTH = 60;
    private static final int MAX_CELL_CHARS = 11;
    private static final int TOGGLE_BUTTON_WIDTH = 24;

    private HealerController controller;
    private boolean codeCollapsed;
    private boolean codeTogglePressed;
    private final Rectangle codeToggleBounds = new Rectangle();
    private LineComponent codeToggleLine;

    @Inject
    private HealerFoodOverlay()
    {
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(320, 0));
        panelComponent.setBackgroundColor(PANEL_BACKGROUND);
    }

    void setController(HealerController controller)
    {
        this.controller = controller;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (controller == null)
        {
            return null;
        }

        if (!controller.shouldShowFoodPanel() || !controller.isWaveActive())
        {
            return null;
        }

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(320, 0));
        codeToggleBounds.setBounds(0, 0, 0, 0);
        codeToggleLine = null;

        List<Integer> healerOrders = controller.getHealerOrdersForCurrentWave();
        BaUtilitiesConfig.FoodPanelStyle panelStyle = getEffectivePanelStyle();

        if (controller.isHealerRole() && panelStyle == BaUtilitiesConfig.FoodPanelStyle.CODE_ONLY)
        {
            String sourceText = controller.getCurrentWaveCodeSource();

            if (sourceText == null || sourceText.isEmpty())
            {
                return null;
            }
        }

        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text(getPanelTitle())
                        .color(TITLE_COLOR)
                        .build()
        );

        if (panelStyle == BaUtilitiesConfig.FoodPanelStyle.CODE_ONLY)
        {
            addCurrentWaveCode(false);
            return renderPanel(graphics);
        }

        if (panelStyle == BaUtilitiesConfig.FoodPanelStyle.SIMPLIFIED)
        {
            addSimplifiedRows(healerOrders);
            addCurrentWaveCodeIfHealer(true);
            return renderPanel(graphics);
        }

        if (panelStyle == BaUtilitiesConfig.FoodPanelStyle.COLUMNS)
        {
            addColumnTables(graphics, healerOrders);
            addCurrentWaveCodeIfHealer(true);
            return renderPanel(graphics);
        }

        if (controller.hasActiveWaveCode())
        {
            addHealerCallRows(graphics, healerOrders);
        }
        else
        {
            addHealerRows(healerOrders);
        }

        addCurrentWaveCodeIfHealer(true);

        return renderPanel(graphics);
    }

    private BaUtilitiesConfig.FoodPanelStyle getEffectivePanelStyle()
    {
        return controller.isHealerRole()
                ? controller.getFoodPanelStyle()
                : BaUtilitiesConfig.FoodPanelStyle.SIMPLIFIED;
    }

    private Dimension renderPanel(Graphics2D graphics)
    {
        Dimension dimension = super.render(graphics);

        if (codeToggleLine != null)
        {
            codeToggleBounds.setBounds(codeToggleLine.getBounds());
            codeToggleBounds.translate(getBounds().x, getBounds().y);
            codeToggleBounds.width = Math.min(TOGGLE_BUTTON_WIDTH, codeToggleBounds.width);
        }

        return dimension;
    }

    private String getPanelTitle()
    {
        String title = "Healers";
        int wave = controller.getCurrentWave();

        if (wave > 0)
        {
            title = "Wave " + wave;
        }

        String codeName = controller.isHealerRole() ? controller.getCurrentWaveCodeName() : null;

        if (codeName != null && !codeName.trim().isEmpty())
        {
            title += " (" + codeName + ")";
        }

        return title;
    }

    private void addSimplifiedRows(List<Integer> healerOrders)
    {
        for (int healerOrder : healerOrders)
        {
            boolean spawned = controller.hasHealerSpawned(healerOrder);
            boolean displayDead = shouldDisplayHealerDead(healerOrder);
            Color healerColor = getHealerColor(spawned, displayDead);
            String deathTime = getDeathTimeText(healerOrder);
            String text = ColorUtil.prependColorTag(controller.getFoodPanelHealerLabel(healerOrder), healerColor)
                    + " "
                    + ColorUtil.prependColorTag("(" + deathTime + ")", getDeathTimeColor(healerOrder, deathTime));

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(text)
                            .build()
            );
        }
    }

    private void addHealerRows(List<Integer> healerOrders)
    {
        for (int healerOrder : healerOrders)
        {
            String countText = controller.getFoodPanelText(healerOrder, -1);
            boolean spawned = controller.hasHealerSpawned(healerOrder);
            boolean displayDead = shouldDisplayHealerDead(healerOrder);
            Color rowColor = getHealerColor(spawned, displayDead);
            Color rightColor = spawned && !displayDead ? controller.getFoodPanelTextColor(healerOrder, -1) : getHealerColor(spawned, displayDead);
            String ttkText = controller.getHealerPanelTtkText(healerOrder);

            if (rightColor == null)
            {
                rightColor = getHealerColor(spawned, displayDead);
            }

            if (ttkText != null && !ttkText.isEmpty())
            {
                countText += " " + ColorUtil.prependColorTag("(" + ttkText + ")", TTK_COLOR);
            }

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(controller.getFoodPanelHealerLabel(healerOrder))
                            .leftColor(rowColor)
                            .right(countText)
                            .rightColor(rightColor)
                            .build()
            );
        }
    }

    private void addColumnTables(Graphics2D graphics, List<Integer> healerOrders)
    {
        for (int start = 0; start < healerOrders.size(); start += MAX_HEALER_COLUMNS)
        {
            if (start > 0)
            {
                panelComponent.getChildren().add(LineComponent.builder().left("").build());
            }

            addColumnTable(graphics, healerOrders.subList(start, Math.min(start + MAX_HEALER_COLUMNS, healerOrders.size())));
        }
    }

    private void addColumnTable(Graphics2D graphics, List<Integer> healerOrders)
    {
        if (healerOrders.isEmpty())
        {
            return;
        }

        List<Integer> callIndexes = getColumnCallIndexes();

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(buildColumnHeader(healerOrders, graphics.getFontMetrics()))
                        .leftColor(TEXT_COLOR)
                        .build()
        );

        for (int callIndex : callIndexes)
        {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(buildColumnRow(healerOrders, callIndex, graphics.getFontMetrics()))
                            .leftColor(TEXT_COLOR)
                            .build()
            );
        }

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(buildDeathTimeRow(healerOrders, graphics.getFontMetrics()))
                        .leftColor(TEXT_COLOR)
                        .build()
        );
    }

    private List<Integer> getColumnCallIndexes()
    {
        if (!controller.hasActiveWaveCode())
        {
            List<Integer> totalOnly = new ArrayList<>();
            totalOnly.add(-1);
            return totalOnly;
        }

        return controller.getFoodPanelCallIndexes();
    }

    private void addHealerCallRows(Graphics2D graphics, List<Integer> healerOrders)
    {
        FontMetrics metrics = graphics.getFontMetrics();

        panelComponent.getChildren().add(
                LineComponent.builder()
                        .left(buildHealerCallHeader(metrics))
                        .leftColor(TEXT_COLOR)
                        .build()
        );

        for (int healerOrder : healerOrders)
        {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(buildHealerCallRow(healerOrder, metrics))
                            .leftColor(TEXT_COLOR)
                            .build()
            );
        }
    }

    private String buildHealerCallHeader(FontMetrics metrics)
    {
        StringBuilder builder = new StringBuilder(padRightPixels("", ROW_LABEL_WIDTH, metrics));

        for (int callIndex : getRowCallIndexes())
        {
            builder.append(ColorUtil.prependColorTag(padCell("C" + (callIndex + 1), ROW_CELL_WIDTH, metrics), TEXT_COLOR));
        }

        builder.append(ColorUtil.prependColorTag(padCell("Dead", ROW_CELL_WIDTH, metrics), TEXT_COLOR));
        return builder.toString();
    }

    private String buildHealerCallRow(int healerOrder, FontMetrics metrics)
    {
        boolean spawned = controller.hasHealerSpawned(healerOrder);
        boolean displayDead = shouldDisplayHealerDead(healerOrder);
        StringBuilder builder = new StringBuilder(ColorUtil.prependColorTag(
                padCell(controller.getFoodPanelHealerLabel(healerOrder), ROW_LABEL_WIDTH, metrics),
                getHealerColor(spawned, displayDead)));

        for (int callIndex : getRowCallIndexes())
        {
            appendFoodCell(builder, healerOrder, callIndex, ROW_CELL_WIDTH, metrics);
        }

        appendDeathTimeCell(builder, healerOrder, ROW_CELL_WIDTH, metrics);
        return builder.toString();
    }

    private List<Integer> getRowCallIndexes()
    {
        List<Integer> callIndexes = new ArrayList<>();
        callIndexes.addAll(controller.getFoodPanelCallIndexes());
        return callIndexes;
    }

    private String buildColumnHeader(List<Integer> healerOrders, FontMetrics metrics)
    {
        StringBuilder builder = new StringBuilder(padRightPixels("", COLUMN_LABEL_WIDTH, metrics));

        for (int healerOrder : healerOrders)
        {
            boolean spawned = controller.hasHealerSpawned(healerOrder);
            boolean displayDead = shouldDisplayHealerDead(healerOrder);
            Color color = getHealerColor(spawned, displayDead);
            String label = controller.getFoodPanelHealerLabel(healerOrder);

            builder.append(ColorUtil.prependColorTag(padCell(label, COLUMN_CELL_WIDTH, metrics), color));
        }

        return builder.toString();
    }

    private String buildColumnRow(List<Integer> healerOrders, int callIndex, FontMetrics metrics)
    {
        String rowLabel = callIndex < 0 ? "Fed" : "C" + (callIndex + 1);
        StringBuilder builder = new StringBuilder(padCell(rowLabel, COLUMN_LABEL_WIDTH, metrics));

        for (int healerOrder : healerOrders)
        {
            appendFoodCell(builder, healerOrder, callIndex, COLUMN_CELL_WIDTH, metrics);
        }

        return builder.toString();
    }

    private String buildDeathTimeRow(List<Integer> healerOrders, FontMetrics metrics)
    {
        StringBuilder builder = new StringBuilder(padCell("Dead", COLUMN_LABEL_WIDTH, metrics));

        for (int healerOrder : healerOrders)
        {
            appendDeathTimeCell(builder, healerOrder, COLUMN_CELL_WIDTH, metrics);
        }

        return builder.toString();
    }

    private void appendFoodCell(StringBuilder builder, int healerOrder, int callIndex, int width, FontMetrics metrics)
    {
        boolean spawned = controller.hasHealerSpawned(healerOrder);
        boolean displayDead = shouldDisplayHealerDead(healerOrder);
        Color color = spawned && !displayDead ? controller.getFoodPanelTextColor(healerOrder, callIndex) : getHealerColor(spawned, displayDead);

        if (color == null)
        {
            color = getHealerColor(spawned, displayDead);
        }

        builder.append(ColorUtil.prependColorTag(padCell(controller.getFoodPanelText(healerOrder, callIndex), width, metrics), color));
    }

    private void appendDeathTimeCell(StringBuilder builder, int healerOrder, int width, FontMetrics metrics)
    {
        String text = getDeathTimeText(healerOrder);
        Color color = getDeathTimeColor(healerOrder, text);

        builder.append(ColorUtil.prependColorTag(padCell(text, width, metrics), color));
    }

    private String getDeathTimeText(int healerOrder)
    {
        String deathTime = controller.getHealerPanelDeathTime(healerOrder);
        return deathTime == null || deathTime.isEmpty() ? "-" : deathTime;
    }

    private Color getDeathTimeColor(int healerOrder, String deathTime)
    {
        boolean spawned = controller.hasHealerSpawned(healerOrder);
        boolean displayDead = shouldDisplayHealerDead(healerOrder);
        return "-".equals(deathTime) || !spawned || displayDead ? getHealerColor(spawned, displayDead) : TTK_COLOR;
    }

    private boolean shouldDisplayHealerDead(int healerOrder)
    {
        return controller.isHealerDead(healerOrder) || controller.isHealerPresumedDead(healerOrder);
    }

    private Color getHealerColor(boolean spawned, boolean dead)
    {
        return !spawned || dead ? DEAD_COLOR : TEXT_COLOR;
    }

    private String padRightPixels(String text, int width, FontMetrics metrics)
    {
        String value = text == null ? "" : text;

        if (metrics == null)
        {
            return value;
        }

        int remaining = width - metrics.stringWidth(value);

        if (remaining <= 0)
        {
            return value;
        }

        int spaceWidth = Math.max(metrics.stringWidth(" "), 1);
        return value + spaces((int) Math.ceil(remaining / (double) spaceWidth));
    }

    private String padCell(String text, int width, FontMetrics metrics)
    {
        return padRightPixels(fixedWidthCellText(text), width, metrics);
    }

    private String fixedWidthCellText(String text)
    {
        String value = truncateCell(text);

        if (value.length() >= MAX_CELL_CHARS)
        {
            return value;
        }

        return value + spaces(MAX_CELL_CHARS - value.length());
    }

    private String truncateCell(String text)
    {
        String value = text == null ? "" : text;

        if (value.length() <= MAX_CELL_CHARS)
        {
            return value;
        }

        return value.substring(0, MAX_CELL_CHARS - 3) + "...";
    }

    private String spaces(int count)
    {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < count; i++)
        {
            builder.append(' ');
        }

        return builder.toString();
    }

    private void addCurrentWaveCode(boolean collapsible)
    {
        String sourceText = controller.getCurrentWaveCodeSource();

        if (sourceText == null || sourceText.isEmpty())
        {
            return;
        }

        if (collapsible)
        {
            codeToggleLine = LineComponent.builder()
                    .left(codeCollapsed ? "Show Code" : "Hide Code")
                    .leftColor(TITLE_COLOR)
                    .build();

            panelComponent.getChildren().add(codeToggleLine);

            if (codeCollapsed)
            {
                return;
            }
        }

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

    private void addCurrentWaveCodeIfHealer(boolean collapsible)
    {
        if (controller.isHealerRole())
        {
            addCurrentWaveCode(collapsible);
        }
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent mouseEvent)
    {
        if (codeTogglePressed || isCodeToggleClick(mouseEvent))
        {
            codeTogglePressed = false;
            mouseEvent.consume();
            return null;
        }

        return mouseEvent;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent mouseEvent)
    {
        if (isCodeToggleClick(mouseEvent))
        {
            codeTogglePressed = true;
            codeCollapsed = !codeCollapsed;
            mouseEvent.consume();
            return null;
        }

        return mouseEvent;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent mouseEvent)
    {
        if (codeTogglePressed || isCodeToggleClick(mouseEvent))
        {
            codeTogglePressed = false;
            mouseEvent.consume();
            return null;
        }

        return mouseEvent;
    }

    private boolean isCodeToggleClick(MouseEvent mouseEvent)
    {
        return mouseEvent.getButton() == MouseEvent.BUTTON1 && codeToggleBounds.contains(mouseEvent.getPoint());
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent mouseEvent)
    {
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent mouseEvent)
    {
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent mouseEvent)
    {
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent mouseEvent)
    {
        return mouseEvent;
    }
}
