package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.defender.DefenderController;
import com.bahealerorder.defender.strategies.DefenderTimedNotes;
import com.bahealerorder.defender.strategies.DefenderWaveStrategy;
import com.bahealerorder.defender.strategies.TimedDefenderNote;
import com.bahealerorder.healer.HealerController;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;

@Singleton
public class BaRolePanelOverlay extends OverlayPanel implements MouseListener
{
    private static final Color DEFAULT_PANEL_BACKGROUND = ComponentConstants.STANDARD_BACKGROUND_COLOR;
    private static final Color TITLE_COLOR = new Color(0, 255, 0);
    private static final Color DEFENDER_TITLE_COLOR = new Color(80, 170, 255);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color TTK_COLOR = Color.ORANGE;
    private static final Color DEAD_COLOR = Color.GRAY;
    private static final Color ACTIVE_DEFENDER_NOTE_COLOR = new Color(255, 220, 90);

    private static final int BASE_OVERLAY_TEXT_SIZE = 16;
    private static final int BASE_PANEL_WIDTH = 240;
    private static final int BASE_MAX_HEALER_COLUMNS = 4;
    private static final int BASE_COLUMN_LABEL_WIDTH = 44;
    private static final int BASE_COLUMN_CELL_WIDTH = 60;
    private static final int BASE_ROW_LABEL_WIDTH = 60;
    private static final int BASE_ROW_CELL_WIDTH = 60;
    private static final int BASE_MAX_CELL_CHARS = 11;
    private static final int BASE_TOGGLE_BUTTON_WIDTH = 24;

    @Inject
    private BaUtilitiesConfig config;

    @Inject
    private ConfigManager configManager;

    private HealerController healerController;
    private DefenderController defenderController;
    private boolean codeTogglePressed;
    private final Rectangle codeToggleBounds = new Rectangle();
    private LineComponent codeToggleLine;

    @Inject
    private BaRolePanelOverlay()
    {
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(BASE_PANEL_WIDTH, 0));
        panelComponent.setBackgroundColor(DEFAULT_PANEL_BACKGROUND);
    }

    public void setHealerController(HealerController controller)
    {
        this.healerController = controller;
    }

    public void setDefenderController(DefenderController controller)
    {
        this.defenderController = controller;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlayPanel() || (healerController == null && defenderController == null))
        {
            return hidePanel();
        }

        if (defenderController != null && defenderController.isDefenderRole())
        {
            return shouldRenderDefenderPanel() ? renderDefenderPanel(graphics) : hidePanel();
        }

        if (!shouldRenderHealerPanel())
        {
            return hidePanel();
        }

        Font originalFont = preparePanel(graphics);

        try
        {
            List<Integer> healerOrders = healerController.getHealerOrdersForCurrentWave();
            BaUtilitiesConfig.FoodPanelStyle panelStyle = getEffectivePanelStyle();

            if (panelStyle == BaUtilitiesConfig.FoodPanelStyle.CODE_ONLY)
            {
                String sourceText = healerController.getCurrentWaveCodeSource();

                if (sourceText == null || sourceText.isEmpty())
                {
                    return hidePanel();
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

            if (healerController.hasActiveWaveCode())
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
        finally
        {
            graphics.setFont(originalFont);
        }
    }

    private BaUtilitiesConfig.FoodPanelStyle getEffectivePanelStyle()
    {
        return healerController.getFoodPanelStyle();
    }

    private boolean shouldRenderHealerPanel()
    {
        return healerController != null
                && healerController.isHealerRole()
                && healerController.getFoodPanelStyle() != BaUtilitiesConfig.FoodPanelStyle.NONE
                && healerController.isWaveActive();
    }

    private boolean shouldRenderDefenderPanel()
    {
        return defenderController != null
                && defenderController.shouldShowStrategyPanel()
                && hasDefenderNotes(defenderController.getCurrentWaveStrategy());
    }

    private Dimension renderDefenderPanel(Graphics2D graphics)
    {
        DefenderWaveStrategy strategy = defenderController.getCurrentWaveStrategy();
        if (!hasDefenderNotes(strategy))
        {
            return hidePanel();
        }

        Font originalFont = preparePanel(graphics);

        try
        {
            panelComponent.getChildren().add(
                    TitleComponent.builder()
                            .text(getDefenderPanelTitle(strategy))
                            .color(DEFENDER_TITLE_COLOR)
                            .build()
            );

            addDefenderNotes(strategy);
            return renderPanel(graphics);
        }
        finally
        {
            graphics.setFont(originalFont);
        }
    }

    private boolean hasDefenderNotes(DefenderWaveStrategy strategy)
    {
        return strategy != null && strategy.getNotes() != null && !strategy.getNotes().trim().isEmpty();
    }

    private String getDefenderPanelTitle(DefenderWaveStrategy strategy)
    {
        return "Wave " + defenderController.getCurrentWave() + " (" + strategy.getName() + ")";
    }

    private void addDefenderNotes(DefenderWaveStrategy strategy)
    {
        List<TimedDefenderNote> notes = DefenderTimedNotes.parse(strategy.getNotes());
        int currentWaveTick = defenderController.getCurrentWaveTick();
        int activeIndex = DefenderTimedNotes.getActiveTimedIndex(notes, currentWaveTick);

        for (int i = 0; i < notes.size(); i++)
        {
            TimedDefenderNote note = notes.get(i);
            String text = note.getText() == null || note.getText().isEmpty() ? " " : note.getText();
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(text)
                            .leftColor(getDefenderNoteColor(note, i, activeIndex, currentWaveTick))
                            .build()
            );
        }
    }

    private Color getDefenderNoteColor(TimedDefenderNote note, int index, int activeIndex, int currentWaveTick)
    {
        if (index == activeIndex)
        {
            return ACTIVE_DEFENDER_NOTE_COLOR;
        }

        if (note.isTimed() && note.getTick() < currentWaveTick)
        {
            return DEAD_COLOR;
        }

        return TEXT_COLOR;
    }

    private Font preparePanel(Graphics2D graphics)
    {
        Font originalFont = graphics.getFont();
        applyConfiguredFont(graphics, originalFont);
        clearPanelState();
        panelComponent.setBackgroundColor(getPanelBackgroundColor());
        panelComponent.setPreferredSize(new Dimension(scale(BASE_PANEL_WIDTH), 0));
        return originalFont;
    }

    private void applyConfiguredFont(Graphics2D graphics, Font defaultFont)
    {
        BaUtilitiesConfig.OverlayFont overlayFont = config.overlayFont();
        int textSize = getOverlayTextSize();

        if (overlayFont == BaUtilitiesConfig.OverlayFont.ARIAL)
        {
            graphics.setFont(new Font("Arial", Font.PLAIN, textSize));
            return;
        }

        if (overlayFont == BaUtilitiesConfig.OverlayFont.ARIAL_BOLD)
        {
            graphics.setFont(new Font("Arial", Font.BOLD, textSize));
            return;
        }

        if (textSize != BASE_OVERLAY_TEXT_SIZE)
        {
            graphics.setFont(defaultFont.deriveFont((float) textSize));
        }
    }

    private Dimension hidePanel()
    {
        codeTogglePressed = false;
        clearPanelState();
        return null;
    }

    private void clearPanelState()
    {
        panelComponent.getChildren().clear();
        codeToggleBounds.setBounds(0, 0, 0, 0);
        codeToggleLine = null;
    }

    private Dimension renderPanel(Graphics2D graphics)
    {
        Dimension dimension = super.render(graphics);

        if (codeToggleLine != null)
        {
            codeToggleBounds.setBounds(codeToggleLine.getBounds());
            codeToggleBounds.translate(getBounds().x, getBounds().y);
            codeToggleBounds.width = Math.min(scale(BASE_TOGGLE_BUTTON_WIDTH), codeToggleBounds.width);
        }

        return dimension;
    }

    private int getOverlayTextSize()
    {
        return Math.max(8, config.foodPanelOverlayTextSize());
    }

    private double getOverlayScale()
    {
        return getOverlayTextSize() / (double) BASE_OVERLAY_TEXT_SIZE;
    }

    private int scale(int value)
    {
        return Math.max(1, (int) Math.round(value * getOverlayScale()));
    }

    private int getMaxCellChars()
    {
        return Math.max(6, (int) Math.round(BASE_MAX_CELL_CHARS * getOverlayScale()));
    }

    private Color getPanelBackgroundColor()
    {
        Color color = config.foodPanelOverlayBackgroundColor();
        return color == null ? DEFAULT_PANEL_BACKGROUND : color;
    }

    private String getPanelTitle()
    {
        String title = "Healers";
        int wave = healerController.getCurrentWave();

        if (wave > 0)
        {
            title = "Wave " + wave;
        }

        String codeName = healerController.isHealerRole() ? healerController.getCurrentWaveCodeName() : null;

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
            boolean spawned = healerController.hasHealerSpawned(healerOrder);
            boolean displayDead = shouldDisplayHealerDead(healerOrder);
            Color healerColor = getHealerColor(spawned, displayDead);
            String deathTime = getDeathTimeText(healerOrder);
            String text = ColorUtil.prependColorTag(healerController.getFoodPanelHealerLabel(healerOrder), healerColor)
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
            String countText = healerController.getFoodPanelText(healerOrder, -1);
            boolean spawned = healerController.hasHealerSpawned(healerOrder);
            boolean displayDead = shouldDisplayHealerDead(healerOrder);
            Color rowColor = getHealerColor(spawned, displayDead);
            Color rightColor = spawned && !displayDead ? healerController.getFoodPanelTextColor(healerOrder, -1) : getHealerColor(spawned, displayDead);
            String ttkText = healerController.getHealerPanelTtkText(healerOrder);

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
                            .left(healerController.getFoodPanelHealerLabel(healerOrder))
                            .leftColor(rowColor)
                            .right(countText)
                            .rightColor(rightColor)
                            .build()
            );
        }
    }

    private void addColumnTables(Graphics2D graphics, List<Integer> healerOrders)
    {
        for (int start = 0; start < healerOrders.size(); start += BASE_MAX_HEALER_COLUMNS)
        {
            if (start > 0)
            {
                panelComponent.getChildren().add(LineComponent.builder().left("").build());
            }

            addColumnTable(graphics, healerOrders.subList(start, Math.min(start + BASE_MAX_HEALER_COLUMNS, healerOrders.size())));
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
        if (!healerController.hasActiveWaveCode())
        {
            List<Integer> totalOnly = new ArrayList<>();
            totalOnly.add(-1);
            return totalOnly;
        }

        return healerController.getFoodPanelCallIndexes();
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
        StringBuilder builder = new StringBuilder(padRightPixels("", scale(BASE_ROW_LABEL_WIDTH), metrics));

        for (int callIndex : getRowCallIndexes())
        {
            builder.append(ColorUtil.prependColorTag(padCell("C" + (callIndex + 1), scale(BASE_ROW_CELL_WIDTH), metrics), TEXT_COLOR));
        }

        builder.append(ColorUtil.prependColorTag(padCell("Dead", scale(BASE_ROW_CELL_WIDTH), metrics), TEXT_COLOR));
        return builder.toString();
    }

    private String buildHealerCallRow(int healerOrder, FontMetrics metrics)
    {
        boolean spawned = healerController.hasHealerSpawned(healerOrder);
        boolean displayDead = shouldDisplayHealerDead(healerOrder);
        StringBuilder builder = new StringBuilder(ColorUtil.prependColorTag(
                padCell(healerController.getFoodPanelHealerLabel(healerOrder), scale(BASE_ROW_LABEL_WIDTH), metrics),
                getHealerColor(spawned, displayDead)));

        for (int callIndex : getRowCallIndexes())
        {
            appendFoodCell(builder, healerOrder, callIndex, scale(BASE_ROW_CELL_WIDTH), metrics);
        }

        appendDeathTimeCell(builder, healerOrder, scale(BASE_ROW_CELL_WIDTH), metrics);
        return builder.toString();
    }

    private List<Integer> getRowCallIndexes()
    {
        List<Integer> callIndexes = new ArrayList<>();
        callIndexes.addAll(healerController.getFoodPanelCallIndexes());
        return callIndexes;
    }

    private String buildColumnHeader(List<Integer> healerOrders, FontMetrics metrics)
    {
        StringBuilder builder = new StringBuilder(padRightPixels("", scale(BASE_COLUMN_LABEL_WIDTH), metrics));

        for (int healerOrder : healerOrders)
        {
            boolean spawned = healerController.hasHealerSpawned(healerOrder);
            boolean displayDead = shouldDisplayHealerDead(healerOrder);
            Color color = getHealerColor(spawned, displayDead);
            String label = healerController.getFoodPanelHealerLabel(healerOrder);

            builder.append(ColorUtil.prependColorTag(padCell(label, scale(BASE_COLUMN_CELL_WIDTH), metrics), color));
        }

        return builder.toString();
    }

    private String buildColumnRow(List<Integer> healerOrders, int callIndex, FontMetrics metrics)
    {
        String rowLabel = callIndex < 0 ? "Fed" : "C" + (callIndex + 1);
        StringBuilder builder = new StringBuilder(padCell(rowLabel, scale(BASE_COLUMN_LABEL_WIDTH), metrics));

        for (int healerOrder : healerOrders)
        {
            appendFoodCell(builder, healerOrder, callIndex, scale(BASE_COLUMN_CELL_WIDTH), metrics);
        }

        return builder.toString();
    }

    private String buildDeathTimeRow(List<Integer> healerOrders, FontMetrics metrics)
    {
        StringBuilder builder = new StringBuilder(padCell("Dead", scale(BASE_COLUMN_LABEL_WIDTH), metrics));

        for (int healerOrder : healerOrders)
        {
            appendDeathTimeCell(builder, healerOrder, scale(BASE_COLUMN_CELL_WIDTH), metrics);
        }

        return builder.toString();
    }

    private void appendFoodCell(StringBuilder builder, int healerOrder, int callIndex, int width, FontMetrics metrics)
    {
        boolean spawned = healerController.hasHealerSpawned(healerOrder);
        boolean displayDead = shouldDisplayHealerDead(healerOrder);
        Color color = spawned && !displayDead ? healerController.getFoodPanelTextColor(healerOrder, callIndex) : getHealerColor(spawned, displayDead);

        if (color == null)
        {
            color = getHealerColor(spawned, displayDead);
        }

        builder.append(ColorUtil.prependColorTag(padCell(healerController.getFoodPanelText(healerOrder, callIndex), width, metrics), color));
    }

    private void appendDeathTimeCell(StringBuilder builder, int healerOrder, int width, FontMetrics metrics)
    {
        String text = getDeathTimeText(healerOrder);
        Color color = getDeathTimeColor(healerOrder, text);

        builder.append(ColorUtil.prependColorTag(padCell(text, width, metrics), color));
    }

    private String getDeathTimeText(int healerOrder)
    {
        String deathTime = healerController.getHealerPanelDeathTime(healerOrder);
        return deathTime == null || deathTime.isEmpty() ? "-" : deathTime;
    }

    private Color getDeathTimeColor(int healerOrder, String deathTime)
    {
        boolean spawned = healerController.hasHealerSpawned(healerOrder);
        boolean displayDead = shouldDisplayHealerDead(healerOrder);
        return "-".equals(deathTime) || !spawned || displayDead ? getHealerColor(spawned, displayDead) : TTK_COLOR;
    }

    private boolean shouldDisplayHealerDead(int healerOrder)
    {
        return healerController.isHealerDead(healerOrder);
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
        int maxCellChars = getMaxCellChars();

        if (value.length() >= maxCellChars)
        {
            return value;
        }

        return value + spaces(maxCellChars - value.length());
    }

    private String truncateCell(String text)
    {
        String value = text == null ? "" : text;
        int maxCellChars = getMaxCellChars();

        if (value.length() <= maxCellChars)
        {
            return value;
        }

        return value.substring(0, maxCellChars - 3) + "...";
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
        String sourceText = healerController.getCurrentWaveCodeSource();

        if (sourceText == null || sourceText.isEmpty())
        {
            return;
        }

        if (collapsible)
        {
            codeToggleLine = LineComponent.builder()
                    .left(isCodeCollapsed() ? "Show Code" : "Hide Code")
                    .leftColor(TITLE_COLOR)
                    .build();

            panelComponent.getChildren().add(codeToggleLine);

            if (isCodeCollapsed())
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
        if (healerController.isHealerRole())
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
            configManager.setConfiguration(
                    BaUtilitiesConfig.GROUP_NAME,
                    BaUtilitiesConfig.FOOD_PANEL_CODE_COLLAPSED_KEY,
                    !isCodeCollapsed()
            );
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

    private boolean isCodeCollapsed()
    {
        return config.foodPanelCodeCollapsed();
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
