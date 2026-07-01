package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.healer.HealerController;
import com.bahealerorder.tilemarkers.GeneralTileMarkerStrategyManager;
import com.bahealerorder.tilemarkers.TimedStrategyNote;
import com.bahealerorder.tilemarkers.TimedStrategyNotes;
import com.bahealerorder.tilemarkers.TileMarkerRoleContext;
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
	private static final Color TEXT_COLOR = Color.WHITE;
	private static final Color TTK_COLOR = Color.ORANGE;
	private static final Color DEAD_COLOR = Color.GRAY;

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

    @Inject
    private BaRoleDetector roleDetector;

    @Inject
    private GeneralTileMarkerStrategyManager strategyManager;

    private HealerController healerController;
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

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlayPanel() || healerController == null)
        {
            return hidePanel();
        }

        if (!healerController.isWaveActive())
        {
            return hidePanel();
        }

        BaUtilitiesConfig.FoodPanelStyle panelStyle = getEffectivePanelStyle();
        String sourceText = healerController.getCurrentWaveCodeSource();
        String notes = getCurrentWaveNotes();
        boolean healerContent = shouldRenderHealerContent(panelStyle, sourceText);
        boolean notesContent = !isBlank(notes);

        if (!healerContent && !notesContent)
        {
            return hidePanel();
        }

        Font originalFont = preparePanel(graphics);

        try
        {
            panelComponent.getChildren().add(
                    TitleComponent.builder()
                            .text(getPanelTitle())
                            .color(getPanelTitleColor())
                            .build()
            );

            if (!healerContent)
            {
                addCurrentWaveNotes(false);
                return renderPanel(graphics);
            }

            List<Integer> healerOrders = healerController.getHealerOrdersForCurrentWave();

            if (panelStyle == BaUtilitiesConfig.FoodPanelStyle.CODE_ONLY)
            {
                addCurrentWaveCodeAndNotes(false);
                return renderPanel(graphics);
            }

            if (panelStyle == BaUtilitiesConfig.FoodPanelStyle.SIMPLIFIED)
            {
                addSimplifiedRows(healerOrders);
                addCurrentWaveCodeAndNotesIfHealer(true);
                return renderPanel(graphics);
            }

            if (panelStyle == BaUtilitiesConfig.FoodPanelStyle.COLUMNS)
            {
                addColumnTables(graphics, healerOrders);
                addCurrentWaveCodeAndNotesIfHealer(true);
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

            addCurrentWaveCodeAndNotesIfHealer(true);

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

    private boolean shouldRenderHealerContent(BaUtilitiesConfig.FoodPanelStyle panelStyle, String sourceText)
    {
        return healerController != null
                && healerController.isHealerRole()
                && panelStyle != BaUtilitiesConfig.FoodPanelStyle.NONE
                && (panelStyle != BaUtilitiesConfig.FoodPanelStyle.CODE_ONLY || !isBlank(sourceText));
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
        String title = "Wave";
        int wave = healerController.getCurrentWave();

        if (wave > 0)
        {
            title = "Wave " + wave;
        }

        String codeName = healerController.isHealerRole() && healerController.hasActiveWaveCode()
				? healerController.getCurrentWaveCodeName()
				: null;
        String detail = !isBlank(codeName) ? codeName : getCurrentStrategyName();

        if (!isBlank(detail))
        {
            title += " (" + detail + ")";
        }

        return title;
	}

	private Color getPanelTitleColor()
	{
		Color color = BaRoleColors.color(getDisplayRole());
		return color == null ? TITLE_COLOR : color;
	}

	private BaRole getDisplayRole()
	{
		BaRole role = roleDetector.getCurrentRole();
		return role == null ? BaRole.HEALER : role;
	}

	private String getCurrentStrategyName()
	{
		if (healerController == null || !healerController.isWaveActive())
		{
			return "";
		}

		return strategyManager.getActiveStrategyName(
				healerController.getCurrentWave(),
				getCurrentRoleContext()
		);
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

    private void addCurrentWaveCodeAndNotes(boolean collapsible)
    {
        addCurrentWaveNotes(addCurrentWaveCode(collapsible));
    }

    private boolean addCurrentWaveCode(boolean collapsible)
    {
        String sourceText = healerController.getCurrentWaveCodeSource();

        if (isBlank(sourceText))
        {
            return false;
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
                return true;
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

        return true;
    }

    private void addCurrentWaveCodeAndNotesIfHealer(boolean collapsible)
    {
        if (healerController.isHealerRole())
        {
            addCurrentWaveCodeAndNotes(collapsible);
        }
    }

    private void addCurrentWaveNotes(boolean separateFromCode)
    {
        String notes = getCurrentWaveNotes();

        if (isBlank(notes))
        {
            return;
        }

        if (separateFromCode)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("").build());
        }

        List<TimedStrategyNote> timedNotes = TimedStrategyNotes.parse(notes);
        int currentWaveTick = getCurrentWaveTick();
        int activeIndex = TimedStrategyNotes.getActiveTimedIndex(timedNotes, currentWaveTick);

        for (int i = 0; i < timedNotes.size(); i++)
        {
            TimedStrategyNote note = timedNotes.get(i);
            String line = note.getText();
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(line.isEmpty() ? " " : line)
                            .leftColor(TimedStrategyNotes.colorFor(note, i, activeIndex, currentWaveTick, TEXT_COLOR, DEAD_COLOR))
                            .build()
            );
        }
    }

    private String getCurrentWaveNotes()
    {
        if (healerController == null || !healerController.isWaveActive())
        {
            return "";
        }

		String notes = strategyManager.getActiveNotes(
				healerController.getCurrentWave(),
                getCurrentRoleContext()
        );
		return notes == null ? "" : notes.trim();
	}

	private TileMarkerRoleContext getCurrentRoleContext()
	{
		return TileMarkerRoleContext.fromRole(getDisplayRole());
	}

    private int getCurrentWaveTick()
    {
        return Math.max(0, Math.round(healerController.getCurrentWaveElapsedSeconds() / 0.6f));
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

    private static boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
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
