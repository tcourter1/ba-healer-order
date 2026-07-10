package com.bahealerorder.sidepanel.tilemarkers;

import com.bahealerorder.common.BaClipboard;
import com.bahealerorder.common.BaIcons;
import com.bahealerorder.sidepanel.BaPanelUi;
import com.bahealerorder.sidepanel.BaTransferDialog;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.tilemarkers.GeneralTileMarkerStrategyManager;
import com.bahealerorder.tilemarkers.TileMarkerAssignmentPreset;
import com.bahealerorder.tilemarkers.TileMarkerExportResult;
import com.bahealerorder.tilemarkers.TileMarkerExportType;
import com.bahealerorder.tilemarkers.TileMarkerRoleContext;
import com.bahealerorder.tilemarkers.TileMarkerStrategyPreset;
import com.bahealerorder.tilemarkers.TileMarkerWaveMap;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.SwingUtil;

@Singleton
public class GeneralTileMarkerPanel extends JPanel
{
	private static final String EMPTY_WAVE_SELECTION_LABEL = " ";
	private static final int CONTROL_HEIGHT = 24;
	private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
	private static final int ROLE_ICON_SIZE = 18;
	private static final int ROLE_TAB_GAP = 6;
	private static final int PRESET_TO_WAVES_GAP = 26;
	private static final int STRATEGY_SECTION_BOTTOM_PADDING = 8;
	private static final int WAVE_ROW_GAP = 8;
	private static final int WAVE_ROW_HORIZONTAL_PADDING = 8;
	private static final int WAVE_ROW_CONTROL_WIDTH = CONTENT_WIDTH - WAVE_ROW_HORIZONTAL_PADDING * 2;
	private static final int WAVE_LABEL_WIDTH = 54;
	private static final int WAVE_LABEL_LEFT_PADDING = 2;
	private static final int WAVE_MENU_BUTTON_WIDTH = 24;
	private static final int WAVE_COMBO_WIDTH = WAVE_ROW_CONTROL_WIDTH - WAVE_LABEL_WIDTH - WAVE_MENU_BUTTON_WIDTH - 12;
	private static final int ASSIGNMENT_CONTROL_WIDTH = CONTENT_WIDTH - 16;
	private static final int ASSIGNMENT_PRESET_POPUP_WIDTH = 188;
	private static final int WAVE_STRATEGY_POPUP_WIDTH = 165;
	private static final int ACTION_ROW_GAP = 6;
	private static final int ACTION_BUTTON_WIDTH = (ASSIGNMENT_CONTROL_WIDTH - ACTION_ROW_GAP) / 2;

	private final GeneralTileMarkerStrategyManager strategyManager;
	private final ColorPickerManager colorPickerManager;
	private final ItemManager itemManager;
	private final JPanel contentPanel = new JPanel();
	private final JPanel waveRowsPanel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final JPanel beginnerPromptPanel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final MaterialTabGroup roleTabGroup = new MaterialTabGroup();
	private final Map<TileMarkerRoleContext, MaterialTab> roleTabs = new EnumMap<>(TileMarkerRoleContext.class);
	private final JLabel strategyTitle = new JLabel();
	private final JComboBox<BaPanelUi.ComboOption> assignmentPresetCombo =
			BaPanelUi.fixedPopupWidthCombo(ASSIGNMENT_PRESET_POPUP_WIDTH);

	private TileMarkerRoleContext selectedContext = TileMarkerRoleContext.DEFENDER;
	private JDialog markerEditorDialog;
	private JDialog strategyEditorDialog;
	private JDialog previewDialog;
	private TileMarkerStrategyPresetEditor strategyEditorPanel;
	private boolean refreshing;

	@Inject
	public GeneralTileMarkerPanel(
			GeneralTileMarkerStrategyManager strategyManager,
			ColorPickerManager colorPickerManager,
			ItemManager itemManager)
	{
		this.strategyManager = strategyManager;
		this.colorPickerManager = colorPickerManager;
		this.itemManager = itemManager;

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());
		setAlignmentX(LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 0, 5));
		add(contentPanel, BorderLayout.NORTH);

		contentPanel.add(createConfigureMarkersButton());
		contentPanel.add(Box.createVerticalStrut(12));
		contentPanel.add(createStrategySection());
		strategyManager.load();
		refreshAll();
	}

	public void refreshAll()
	{
		refreshing = true;
		try
		{
			selectedContext = strategyManager.getSelectedRoleContext();
			refreshRoleTabs();
			refreshWaveRows();
			refreshAssignmentPresetCombo();
		}
		finally
		{
			refreshing = false;
		}
	}

	private JButton createConfigureMarkersButton()
	{
		JButton button = BaPanelUi.action("Open Tile Marker Editor", this::openMarkerEditorDialog, CONTENT_WIDTH - 10, CONTROL_HEIGHT);
		button.setIcon(BaIcons.popoutIcon());
		button.setHorizontalTextPosition(SwingConstants.LEADING);
		button.setIconTextGap(8);
		button.setToolTipText("Open Tile Marker Editor");
		return button;
	}

	private JPanel createStrategySection()
	{
		JPanel section = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(new EmptyBorder(0, 0, STRATEGY_SECTION_BOTTOM_PADDING, 0));
		section.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		section.setAlignmentX(LEFT_ALIGNMENT);

		JPanel controls = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		controls.setBorder(new EmptyBorder(8, 8, 0, 8));
		controls.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		controls.setAlignmentX(LEFT_ALIGNMENT);

		strategyTitle.setForeground(ColorScheme.TEXT_COLOR);
		strategyTitle.setHorizontalAlignment(SwingConstants.CENTER);
		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(titleRow, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		titleRow.add(strategyTitle, BorderLayout.CENTER);
		controls.add(titleRow);
		controls.add(Box.createVerticalStrut(6));

		controls.add(createRoleContextRow());
		controls.add(Box.createVerticalStrut(8));
		controls.add(createAssignmentPresetControls());
		section.add(controls);
		section.add(Box.createVerticalStrut(PRESET_TO_WAVES_GAP));
		waveRowsPanel.setAlignmentX(LEFT_ALIGNMENT);
		section.add(waveRowsPanel);
		return section;
	}

	private JPanel createRoleContextRow()
	{
		int roleCount = TileMarkerRoleContext.values().length;
		roleTabGroup.setLayout(new DynamicGridLayout(1, roleCount, ROLE_TAB_GAP, 0));
		roleTabGroup.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(roleTabGroup, CONTENT_WIDTH - 16, CONTROL_HEIGHT + 8);

		for (TileMarkerRoleContext context : TileMarkerRoleContext.values())
		{
			MaterialTab tab = new MaterialTab(new ImageIcon(), roleTabGroup, new JPanel());
			tab.setToolTipText(context.getDisplayName());
			BaPanelUi.fixedSize(
					tab,
					(CONTENT_WIDTH - 16 - ROLE_TAB_GAP * (roleCount - 1)) / roleCount,
					CONTROL_HEIGHT + 8
			);
			setRoleTabIcon(tab, context);
			tab.setOnSelectEvent(() ->
			{
				if (!refreshing && context != selectedContext)
				{
					selectedContext = context;
					strategyManager.setSelectedRoleContext(context);
					strategyTitle.setText(selectedContext.getDisplayName() + " Wave Strategies");
					refreshWaveRows();
					refreshAssignmentPresetCombo();
				}
				return true;
			});
			roleTabs.put(context, tab);
			roleTabGroup.addTab(tab);
		}

		refreshRoleTabs();
		return roleTabGroup;
	}

	private void setRoleTabIcon(MaterialTab tab, TileMarkerRoleContext context)
	{
		BaRole role = context.getRole();
		if (role == null)
		{
			tab.setIcon(BaIcons.globeIcon(ROLE_ICON_SIZE));
			return;
		}

		AsyncBufferedImage icon = itemManager.getImage(role.getPlayerIconItemId());
		if (icon != null)
		{
			icon.onLoaded(() -> SwingUtilities.invokeLater(() -> tab.setIcon(scaledRoleIcon(icon))));
		}
	}

	private void refreshRoleTabs()
	{
		MaterialTab selectedTab = roleTabs.get(selectedContext);
		if (selectedTab != null)
		{
			roleTabGroup.select(selectedTab);
		}
		strategyTitle.setText(selectedContext.getDisplayName() + " Wave Strategies");
	}

	private void refreshWaveRows()
	{
		waveRowsPanel.removeAll();
		for (int wave = 1; wave <= 10; wave++)
		{
			waveRowsPanel.add(createWaveRow(wave));
			if (wave < 10)
			{
				waveRowsPanel.add(Box.createVerticalStrut(WAVE_ROW_GAP));
			}
		}
		waveRowsPanel.revalidate();
		waveRowsPanel.repaint();
	}

	private JPanel createWaveRow(int wave)
	{
		JLabel waveLabel = new JLabel("Wave " + wave);
		waveLabel.setForeground(ColorScheme.TEXT_COLOR);
		waveLabel.setBorder(new EmptyBorder(0, WAVE_LABEL_LEFT_PADDING, 0, 0));

		JComboBox<BaPanelUi.ComboOption> comboBox = BaPanelUi.fixedPopupWidthCombo(WAVE_STRATEGY_POPUP_WIDTH);
		comboBox.setRenderer(BaPanelUi.comboOptionRenderer(CONTROL_HEIGHT));
		BaPanelUi.styleCombo(comboBox, WAVE_COMBO_WIDTH, CONTROL_HEIGHT);
		populateWaveSelectionCombo(comboBox, wave, strategyManager.getWaveSelectionStrategyId(selectedContext, wave));
		comboBox.addActionListener(event ->
		{
			if (refreshing) return;

			strategyManager.setWaveSelectionStrategyId(selectedContext, wave, BaPanelUi.selectedId(comboBox));
			refreshAssignmentPresetCombo();
		});

		BaPanelUi.fixedSize(waveLabel, WAVE_LABEL_WIDTH, CONTROL_HEIGHT);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(0, WAVE_ROW_HORIZONTAL_PADDING, 0, WAVE_ROW_HORIZONTAL_PADDING));
		BaPanelUi.fixedSize(row, CONTENT_WIDTH, CONTROL_HEIGHT);
		row.add(waveLabel, BorderLayout.WEST);
		row.add(comboBox, BorderLayout.CENTER);
		row.add(createWaveMenuButton(wave, comboBox), BorderLayout.EAST);
		return row;
	}

	private JButton createWaveMenuButton(int wave, JComboBox<BaPanelUi.ComboOption> comboBox)
	{
		JButton button = new JButton(BaIcons.verticalEllipsisIcon());
		button.addActionListener(event -> createWaveActionMenu(wave, comboBox).show(button, 0, button.getHeight()));
		SwingUtil.removeButtonDecorations(button);
		BaPanelUi.fixedSize(button, WAVE_MENU_BUTTON_WIDTH, CONTROL_HEIGHT);
		return button;
	}

	private JPopupMenu createWaveActionMenu(int wave, JComboBox<BaPanelUi.ComboOption> comboBox)
	{
		JPopupMenu menu = new JPopupMenu();
		menu.add(menuItem("Add", BaIcons.plusIcon(), () -> openStrategyEditorDialog(wave, null)));
		menu.add(menuItem("Edit", BaIcons.pencilIcon(), () ->
		{
			String strategyId = selectedWaveStrategyId(comboBox);
			openStrategyEditorDialog(wave, strategyId);
		}));
		menu.add(menuItem("Delete", BaIcons.trashIcon(), () -> deleteSelectedWaveStrategy(comboBox)));
		menu.add(menuItem("Import", BaIcons.importIcon(), () -> importWaveSelectionFromClipboard(wave)));
		menu.add(menuItem("Export", BaIcons.exportIcon(), () -> exportWaveSelectionToClipboard(wave)));
		menu.add(menuItem("Preview", BaIcons.eyeIcon(), () -> openPreviewDialog(wave)));
		return menu;
	}

	private JMenuItem menuItem(String text, ImageIcon icon, Runnable action)
	{
		JMenuItem item = new JMenuItem(text);
		item.setIcon(icon);
		item.setIconTextGap(8);
		item.addActionListener(event -> action.run());
		return item;
	}

	private JMenuItem menuItem(String text, Runnable action)
	{
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(event -> action.run());
		return item;
	}

	private JPanel createAssignmentPresetControls()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(createBeginnerPromptPanel());
		BaPanelUi.styleCombo(assignmentPresetCombo, ASSIGNMENT_CONTROL_WIDTH, CONTROL_HEIGHT);
		assignmentPresetCombo.setRenderer(BaPanelUi.comboOptionRenderer(CONTROL_HEIGHT));
		assignmentPresetCombo.addActionListener(event ->
		{
			if (refreshing) return;

			strategyManager.applyAssignmentPreset(selectedContext, BaPanelUi.selectedId(assignmentPresetCombo));
			refreshAll();
		});
		panel.add(assignmentPresetCombo);
		panel.add(Box.createVerticalStrut(6));
		panel.add(BaPanelUi.action("Save Current Selections", this::saveCurrentAssignmentPreset, ASSIGNMENT_CONTROL_WIDTH, CONTROL_HEIGHT));
		panel.add(Box.createVerticalStrut(5));

		JPanel actionRow = BaPanelUi.horizontalActionRow(ASSIGNMENT_CONTROL_WIDTH, CONTROL_HEIGHT);
		actionRow.add(BaPanelUi.action("Delete", this::deleteSelectedAssignmentPreset, ACTION_BUTTON_WIDTH, CONTROL_HEIGHT));
		actionRow.add(BaPanelUi.action("Clear", this::clearAssignmentSelections, ACTION_BUTTON_WIDTH, CONTROL_HEIGHT));
		panel.add(actionRow);
		panel.add(Box.createVerticalStrut(5));

		JPanel importExportRow = BaPanelUi.horizontalActionRow(ASSIGNMENT_CONTROL_WIDTH, CONTROL_HEIGHT);
		importExportRow.add(BaPanelUi.action("Import", this::importAssignmentPresetFromClipboard, ACTION_BUTTON_WIDTH, CONTROL_HEIGHT));
		importExportRow.add(createAssignmentExportButton());
		panel.add(importExportRow);
		return panel;
	}

	private JButton createAssignmentExportButton()
	{
		JButton button = new JButton("Export");
		button.addActionListener(event -> createAssignmentExportMenu().show(button, 0, button.getHeight()));
		BaPanelUi.styleActionButton(button, ACTION_BUTTON_WIDTH, CONTROL_HEIGHT);
		return button;
	}

	private JPopupMenu createAssignmentExportMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		menu.add(menuItem("Current Preset", this::exportAssignmentPresetToClipboard));
		menu.add(menuItem("All Tile Markers", this::exportAllMarkerSetsToClipboard));
		menu.add(menuItem("All Strategies", this::exportAllStrategiesToClipboard));
		return menu;
	}

	private JPanel createBeginnerPromptPanel()
	{
		int buttonWidth = CONTENT_WIDTH - 16;
		int buttonHeight = 96;
		JButton button = BaPanelUi.action(
				"<html><center>No strategies are<br>currently selected.<br>Click here to apply<br>beginner defender tiles.</center></html>",
				this::applyBeginnerAssignmentPreset,
				buttonWidth,
				buttonHeight
		);
		button.setBackground(new Color(0x14029E));
		button.setForeground(Color.WHITE);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setToolTipText("Apply the Beginner preset to the current role.");

		JButton closeButton = new JButton(BaIcons.closeIcon());
		closeButton.setToolTipText("Dismiss");
		closeButton.addActionListener(event -> dismissBeginnerPrompt());
		SwingUtil.removeButtonDecorations(closeButton);
		BaPanelUi.fixedSize(closeButton, 18, 18);

		JLayeredPane layeredPane = new JLayeredPane()
		{
			@Override
			public void doLayout()
			{
				button.setBounds(0, 0, buttonWidth, buttonHeight);
				closeButton.setBounds(buttonWidth - 30, 4, 18, 18);
			}
		};
		BaPanelUi.fixedSize(layeredPane, buttonWidth, buttonHeight);
		layeredPane.add(button, JLayeredPane.DEFAULT_LAYER);
		layeredPane.add(closeButton, JLayeredPane.PALETTE_LAYER);

		beginnerPromptPanel.add(layeredPane);
		beginnerPromptPanel.add(Box.createVerticalStrut(6));
		return beginnerPromptPanel;
	}

	private void refreshAssignmentPresetCombo()
	{
		boolean previousRefreshing = refreshing;
		refreshing = true;
		try
		{
			assignmentPresetCombo.removeAllItems();
			assignmentPresetCombo.addItem(new BaPanelUi.ComboOption(null, "Select a preset..."));
			for (TileMarkerAssignmentPreset preset : strategyManager.getAssignmentPresets(selectedContext))
			{
				assignmentPresetCombo.addItem(new BaPanelUi.ComboOption(preset.getId(), preset.toString(), preset.isBuiltIn()));
			}
			BaPanelUi.selectComboValue(assignmentPresetCombo, strategyManager.getActiveAssignmentPresetId(selectedContext));
			beginnerPromptPanel.setVisible(
					selectedContext == TileMarkerRoleContext.DEFENDER
							&& !strategyManager.isBeginnerPromptDismissed()
							&& strategyManager.getWaveSelections(selectedContext).isEmpty()
							&& beginnerAssignmentPresetId() != null
			);
		}
		finally
		{
			refreshing = previousRefreshing;
		}
	}

	private void applyBeginnerAssignmentPreset()
	{
		String beginnerPresetId = beginnerAssignmentPresetId();
		if (beginnerPresetId == null)
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		strategyManager.applyAssignmentPreset(selectedContext, beginnerPresetId);
		refreshAll();
	}

	private String beginnerAssignmentPresetId()
	{
		for (TileMarkerAssignmentPreset preset : strategyManager.getAssignmentPresets(selectedContext))
		{
			if (preset != null && "Beginner".equalsIgnoreCase(preset.getName())) return preset.getId();
		}
		return null;
	}

	private void dismissBeginnerPrompt()
	{
		strategyManager.dismissBeginnerPrompt();
		beginnerPromptPanel.setVisible(false);
	}

	private void saveCurrentAssignmentPreset()
	{
		String activePresetId = strategyManager.getActiveAssignmentPresetId(selectedContext);
		TileMarkerAssignmentPreset activePreset = strategyManager.findAssignmentPreset(activePresetId);
		String name = promptName("Preset name", activePreset == null ? "" : activePreset.getName());
		if (name == null) return;

		String id = activePreset != null && !activePreset.isBuiltIn() && name.equalsIgnoreCase(activePreset.getName())
				? activePreset.getId()
				: null;
		strategyManager.saveAssignmentPreset(selectedContext, id, name, strategyManager.getWaveSelections(selectedContext));
		refreshAll();
	}

	private void deleteSelectedAssignmentPreset()
	{
		String id = BaPanelUi.selectedId(assignmentPresetCombo);
		if (id == null) return;

		TileMarkerAssignmentPreset preset = strategyManager.findAssignmentPreset(id);
		if (preset == null || preset.isBuiltIn())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this wave strategy preset?", "Delete Preset", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION) return;

		strategyManager.deleteAssignmentPreset(id);
		refreshAll();
	}

	private void deleteSelectedWaveStrategy(JComboBox<BaPanelUi.ComboOption> comboBox)
	{
		String strategyId = selectedWaveStrategyId(comboBox);
		if (isBlank(strategyId))
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		TileMarkerStrategyPreset preset = strategyManager.findStrategyPreset(strategyId);
		if (preset == null || preset.isBuiltIn())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this wave strategy?", "Delete Strategy", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION) return;

		strategyManager.deleteStrategyPreset(strategyId);
		refreshAll();
	}

	private void clearAssignmentSelections()
	{
		strategyManager.clearWaveSelections(selectedContext);
		refreshAll();
	}

	private void exportWaveSelectionToClipboard(int wave)
	{
		String strategyId = strategyManager.getWaveSelectionStrategyId(selectedContext, wave);
		if (isBlank(strategyId))
		{
			JOptionPane.showMessageDialog(this, "No strategy is selected for Wave " + wave + ".", "Export Wave", JOptionPane.ERROR_MESSAGE);
			return;
		}

		TileMarkerExportResult result = strategyManager.exportStrategyPresetJson(strategyManager.findStrategyPreset(strategyId));

		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "Wave " + wave + " could not be exported.", "Export Wave", JOptionPane.ERROR_MESSAGE);
			return;
		}

		BaClipboard.copyText(result.getJson());
		showTransferDialog("Export Wave", exportMessage(result), "Export", result);
	}

	private void importWaveSelectionFromClipboard(int wave)
	{
		String json = BaClipboard.readText(this, "Import Wave");
		if (json == null) return;

		TileMarkerExportResult result;
		try
		{
			result = strategyManager.importMarkerExportForWave(selectedContext, wave, json);
		}
		catch (RuntimeException ex)
		{
			result = null;
		}

		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "Clipboard text could not be imported for Wave " + wave + ".", "Import Wave", JOptionPane.ERROR_MESSAGE);
			return;
		}

		refreshAll();
		showTransferDialog("Import Wave", importWaveMessage(result, wave), "Import", result);
	}

	private void exportAssignmentPresetToClipboard()
	{
		TileMarkerAssignmentPreset activePreset = strategyManager.findAssignmentPreset(
				strategyManager.getActiveAssignmentPresetId(selectedContext)
		);
		TileMarkerExportResult result = strategyManager.exportAssignmentPresetJson(
				selectedContext,
				activePreset == null ? null : activePreset.getName()
		);
		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "No wave strategies are currently selected.", "Export Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		BaClipboard.copyText(result.getJson());
		showTransferDialog("Export Preset", "Exported preset " + result.getName() + ".", "Export", result);
	}

	private void exportAllMarkerSetsToClipboard()
	{
		TileMarkerExportResult result = strategyManager.exportAllMarkerSetsJson();
		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "There are no custom tile marker sets to export.", "Export Tile Markers", JOptionPane.ERROR_MESSAGE);
			return;
		}

		BaClipboard.copyText(result.getJson());
		showTransferDialog("Export Tile Markers", "Exported all custom tile marker sets.", "Export", result);
	}

	private void exportAllStrategiesToClipboard()
	{
		TileMarkerExportResult result = strategyManager.exportAllStrategyPresetsJson();
		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "There are no saved wave strategies to export.", "Export Strategies", JOptionPane.ERROR_MESSAGE);
			return;
		}

		BaClipboard.copyText(result.getJson());
		showTransferDialog("Export Strategies", "Exported all saved wave strategies.", "Export", result);
	}

	private void importAssignmentPresetFromClipboard()
	{
		String json = BaClipboard.readText(this, "Import Preset");
		if (json == null) return;

		TileMarkerExportResult result;
		try
		{
			result = strategyManager.importMarkerExportJson(selectedContext, json);
		}
		catch (RuntimeException ex)
		{
			result = null;
		}

		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "Clipboard text could not be imported as a tile marker export.", "Import Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		refreshAll();
		showTransferDialog("Import Preset", "Imported " + result.getTypedName() + ".", "Import", result);
	}

	private void populateWaveSelectionCombo(
			JComboBox<BaPanelUi.ComboOption> comboBox,
			int wave,
			String selectedStrategyId)
	{
		comboBox.removeAllItems();
		comboBox.addItem(new BaPanelUi.ComboOption(null, EMPTY_WAVE_SELECTION_LABEL));
		TileMarkerWaveMap waveMap = TileMarkerWaveMap.fromWave(wave);

		for (TileMarkerStrategyPreset preset : strategyManager.getUserStrategyPresets(waveMap))
		{
			comboBox.addItem(new BaPanelUi.ComboOption(preset.getId(), preset.toString(), preset.isBuiltIn()));
		}

		for (TileMarkerStrategyPreset preset : strategyManager.getBuiltInStrategyPresets(waveMap))
		{
			comboBox.addItem(new BaPanelUi.ComboOption(preset.getId(), preset.toString(), preset.isBuiltIn()));
		}
		BaPanelUi.selectComboValue(comboBox, selectedStrategyId);
	}

	private String promptName(String title, String defaultValue)
	{
		JTextField field = new JTextField(defaultValue);
		BaPanelUi.fixedSize(field, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		int result = JOptionPane.showConfirmDialog(this, field, title, JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION || field.getText().trim().isEmpty()) return null;
		return field.getText().trim();
	}

	private void showTransferDialog(String title, String message, String action, TileMarkerExportResult result)
	{
		BaTransferDialog.show(this, title, message, action, result.getSummaryLines());
	}

	private String exportMessage(TileMarkerExportResult result)
	{
		return "Exported " + result.getTypedName() + ".";
	}

	private String importWaveMessage(TileMarkerExportResult result, int wave)
	{
		if (result.getType() == TileMarkerExportType.STRATEGY_PRESET) return "Imported and selected wave strategy " + result.getName() + " for Wave " + wave + ".";
		return "Imported " + result.getTypedName() + ".";
	}

	private void openMarkerEditorDialog()
	{
		if (markerEditorDialog != null && markerEditorDialog.isDisplayable())
		{
			markerEditorDialog.toFront();
			markerEditorDialog.requestFocus();
			return;
		}

		TileMarkerSetEditor editor = new TileMarkerSetEditor(strategyManager, colorPickerManager, () ->
		{
			refreshAll();
			if (strategyEditorPanel != null)
			{
				strategyEditorPanel.refreshMarkerSets();
			}
		}, (wave, context, markerSetId) -> openStrategyEditorDialog(wave, context, null, markerSetId));
		Window owner = SwingUtilities.getWindowAncestor(this);
		markerEditorDialog = new JDialog(owner, "Tile Marker Sets", java.awt.Dialog.ModalityType.MODELESS);
		markerEditorDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		markerEditorDialog.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				if (editor.confirmDiscard(markerEditorDialog))
				{
					markerEditorDialog.dispose();
				}
			}
		});
		markerEditorDialog.setContentPane(editor);
		markerEditorDialog.pack();
		markerEditorDialog.setMinimumSize(new Dimension(980, 720));
		markerEditorDialog.setLocationRelativeTo(null);
		markerEditorDialog.setVisible(true);
	}

	private void openStrategyEditorDialog(int wave, String strategyId)
	{
		openStrategyEditorDialog(wave, selectedContext, strategyId, null);
	}

	private void openStrategyEditorDialog(int wave, TileMarkerRoleContext context, String strategyId, String initialMarkerSetId)
	{
		if (strategyEditorDialog != null && strategyEditorDialog.isDisplayable())
		{
			if (strategyEditorPanel != null
					&& !strategyEditorPanel.selectStrategyForWave(strategyId, context, wave, initialMarkerSetId, strategyEditorDialog))
			{
				return;
			}
			strategyEditorDialog.toFront();
			strategyEditorDialog.requestFocus();
			return;
		}

		TileMarkerStrategyPresetEditor editor = new TileMarkerStrategyPresetEditor(
				strategyManager,
				this::refreshAll,
				this::openMarkerEditorDialog,
				strategyId,
				context,
				wave,
				initialMarkerSetId
		);
		strategyEditorPanel = editor;
		Window owner = SwingUtilities.getWindowAncestor(this);
		strategyEditorDialog = new JDialog(owner, "Wave Strategies", java.awt.Dialog.ModalityType.MODELESS);
		strategyEditorDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		strategyEditorDialog.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				if (editor.confirmDiscard(strategyEditorDialog))
				{
					strategyEditorDialog.dispose();
				}
			}
		});
		strategyEditorDialog.setContentPane(editor);
		strategyEditorDialog.pack();
		strategyEditorDialog.setMinimumSize(new Dimension(520, 740));
		strategyEditorDialog.setLocationRelativeTo(null);
		strategyEditorDialog.setVisible(true);
	}

	private void openPreviewDialog(int wave)
	{
		if (previewDialog != null && previewDialog.isDisplayable())
		{
			previewDialog.dispose();
		}

		TileMarkerStrategyPreviewPanel previewPanel = new TileMarkerStrategyPreviewPanel(strategyManager, selectedContext, wave);
		Window owner = SwingUtilities.getWindowAncestor(this);
		previewDialog = new JDialog(owner, "Tile Marker Preview", java.awt.Dialog.ModalityType.MODELESS);
		previewDialog.setContentPane(previewPanel);
		previewDialog.pack();
		previewDialog.setMinimumSize(new Dimension(980, 720));
		previewDialog.setLocationRelativeTo(null);
		previewDialog.setVisible(true);
	}

	private String selectedWaveStrategyId(JComboBox<BaPanelUi.ComboOption> comboBox)
	{
		return BaPanelUi.selectedId(comboBox);
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}

	private ImageIcon scaledRoleIcon(BufferedImage image)
	{
		return new ImageIcon(image.getScaledInstance(ROLE_ICON_SIZE, ROLE_ICON_SIZE, Image.SCALE_SMOOTH));
	}

}
