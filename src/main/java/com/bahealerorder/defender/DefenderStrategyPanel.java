package com.bahealerorder.defender;

import com.bahealerorder.defender.strategies.DefenderMarker;
import com.bahealerorder.defender.strategies.DefenderRunPreset;
import com.bahealerorder.defender.strategies.DefenderStrategyManager;
import com.bahealerorder.defender.strategies.DefenderWaveStrategy;
import com.bahealerorder.common.BaIcons;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

@Singleton
public class DefenderStrategyPanel extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
	private static final int WAVE_LABEL_WIDTH = 48;
	private static final String NOTES_TOOLTIP = "<html>Notes appear in the overlay panel, if enabled.<br>"
			+ "To utilize dynamic note highlighting, start a line with a wave time. E.g.<br>"
			+ "12.0 - Delay healer<br>"
			+ "18.0 - Mainstack delay<br>"
			+ "48.0 - Hendi triangle</html>";

	private final DefenderStrategyManager strategyManager;
	private final ColorPickerManager colorPickerManager;
	private final JComboBox<ComboItem> presetCombo = new JComboBox<>();
	private final Map<Integer, JComboBox<ComboItem>> waveCombos = new HashMap<>();
	private final JComboBox<ComboItem> editWaveCombo = new JComboBox<>();
	private final JComboBox<ComboItem> editStrategyCombo = new JComboBox<>();
	private final JTextField strategyName = new JTextField();
	private final JTextArea notes = new JTextArea();
	private final JPanel contentPanel = new JPanel();

	private List<DefenderMarker> editMarkers = new ArrayList<>();
	private int editNumberOfLogs;
	private JButton deleteStrategyAction;
	private DefenderTileMarkerEditor markerEditor;
	private JDialog markerEditorDialog;
	private boolean editorFieldsEditable = true;
	private boolean refreshing;
	private boolean refreshingEditor;

	@Inject
	public DefenderStrategyPanel(DefenderStrategyManager strategyManager, ColorPickerManager colorPickerManager)
	{
		this.strategyManager = strategyManager;
		this.colorPickerManager = colorPickerManager;

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());
		setAlignmentX(LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(contentPanel, BorderLayout.NORTH);

		contentPanel.add(createPresetSection());
		contentPanel.add(Box.createVerticalStrut(24));
		contentPanel.add(createEditSection());

		refreshAll();
	}

	public void refreshAll()
	{
		refreshing = true;
		refreshPresetCombo();
		refreshWaveCombos();
		refreshing = false;
		refreshEditStrategyCombo(getSelectedEditStrategyId());
	}

	private JPanel createPresetSection()
	{
		JPanel section = section("Defender Strategies");
		styleCombo(presetCombo, CONTENT_WIDTH - 16);
		presetCombo.addActionListener(event ->
		{
			if (refreshing) return;

			ComboItem item = (ComboItem) presetCombo.getSelectedItem();
			if (item == null || item.id == null)
			{
				strategyManager.clearActiveSelections();
			}
			else
			{
				strategyManager.applyRunPreset(item.id);
			}
			refreshAll();
		});
		section.add(presetCombo);
		section.add(Box.createVerticalStrut(6));
		section.add(action("Save Current Selections", this::saveCurrentPreset));
		section.add(Box.createVerticalStrut(5));

		JPanel presetActionRow = horizontalActionRow();
		presetActionRow.add(action("Delete", this::deleteSelectedPreset));
		presetActionRow.add(action("Clear", () ->
		{
			strategyManager.clearActiveSelections();
			refreshAll();
		}));
		section.add(presetActionRow);
		section.add(Box.createVerticalStrut(5));

		JPanel jsonActionRow = horizontalActionRow();
		jsonActionRow.add(action("Import", this::importRunPresetFromClipboard));
		jsonActionRow.add(action("Export", this::exportSelectedRunPresetToClipboard));
		section.add(jsonActionRow);
		section.add(Box.createVerticalStrut(26));
		addWaveSelectors(section);
		return section;
	}

	private void addWaveSelectors(JPanel section)
	{
		for (int wave = 1; wave <= 10; wave++)
		{
			JComboBox<ComboItem> comboBox = new JComboBox<>();
			final int selectedWave = wave;
			styleCombo(comboBox, CONTENT_WIDTH - 16 - WAVE_LABEL_WIDTH - 6);
			comboBox.addActionListener(event ->
			{
				if (refreshing) return;

				ComboItem item = (ComboItem) comboBox.getSelectedItem();
				strategyManager.setActiveWaveStrategyId(selectedWave, item == null ? null : item.id);
				selectEditWaveStrategy(selectedWave, item == null ? null : item.id);
				refreshing = true;
				refreshPresetCombo();
				refreshing = false;
			});
			waveCombos.put(wave, comboBox);
			section.add(comboRow("Wave " + wave, comboBox));
			section.add(Box.createVerticalStrut(6));
		}
	}

	private JPanel createEditSection()
	{
		JPanel section = section("Create Wave Strategy");

		for (int wave = 1; wave <= 10; wave++)
		{
			editWaveCombo.addItem(new ComboItem(String.valueOf(wave), "Wave " + wave));
		}
		styleCombo(editWaveCombo, CONTENT_WIDTH - 16);
		editWaveCombo.addActionListener(event ->
		{
			if (!refreshingEditor)
			{
				refreshEditStrategyCombo(null);
			}
		});
		section.add(label("Wave"));
		section.add(Box.createVerticalStrut(3));
		section.add(editWaveCombo);
		section.add(Box.createVerticalStrut(6));

		styleCombo(editStrategyCombo, CONTENT_WIDTH - 16);
		editStrategyCombo.addActionListener(event ->
		{
			if (!refreshingEditor)
			{
				loadSelectedStrategy();
			}
		});
		section.add(label("Existing Strategy"));
		section.add(Box.createVerticalStrut(3));
		section.add(editStrategyCombo);
		section.add(Box.createVerticalStrut(6));

		section.add(createConfigureMarkersButton());
		section.add(Box.createVerticalStrut(6));

		section.add(labelWithInfo("Notes", CONTENT_WIDTH - 16));
		section.add(Box.createVerticalStrut(3));
		styleTextArea(notes, 7);
		section.add(wrapTextArea(notes, 120));
		section.add(Box.createVerticalStrut(6));
		section.add(action("Save Wave Strategy", this::saveWaveStrategy));
		section.add(Box.createVerticalStrut(5));

		JPanel saveDeleteRow = horizontalActionRow();
		deleteStrategyAction = action("Delete", this::deleteOrResetSelectedStrategy);
		saveDeleteRow.add(deleteStrategyAction);
		saveDeleteRow.add(action("Clear", this::clearEditForm));
		section.add(saveDeleteRow);
		return section;
	}

	private JButton createConfigureMarkersButton()
	{
		JButton button = action("Configure in Popout", () -> openMarkerEditorDialog(false));
		button.setIcon(new PopoutIcon());
		button.setHorizontalTextPosition(SwingConstants.LEADING);
		button.setIconTextGap(8);
		button.setToolTipText("Open tile marker configuration");
		return button;
	}

	private DefenderTileMarkerEditor getMarkerEditor()
	{
		if (markerEditor == null)
		{
			markerEditor = new DefenderTileMarkerEditor(
					this::getEditWave,
					this::selectEditWaveFromMarkerEditor,
					this::getEditStrategyOptions,
					this::getSelectedEditStrategyId,
					this::selectEditStrategyFromMarkerEditor,
					() -> strategyName.getText(),
					this::onMarkerEditorStrategyNameChanged,
					this::getNumberOfLogs,
					this::onMarkerEditorNumberOfLogsChanged,
					() -> notes.getText(),
					this::onMarkerEditorNotesChanged,
					this::onMarkersChanged,
					this::importWaveStrategyTemplateFromClipboard,
					this::exportWaveStrategyTemplateToClipboard,
					this::saveWaveStrategyFromMarkerEditor,
					strategyManager::exportMarkerClipboardJson,
					strategyManager::importMarkerClipboardJson,
					strategyManager.getLastMarkerColor(),
					strategyManager.getLastMarkerOpacityPercent(),
					strategyManager.getLastMarkerBorderWidth(),
					strategyManager::setLastMarkerStyle,
					colorPickerManager
			);
			markerEditor.setEditorEnabled(editorFieldsEditable);
		}

		return markerEditor;
	}

	private void openMarkerEditorDialog()
	{
		openMarkerEditorDialog(false);
	}

	private void openMarkerEditorDialog(boolean focusStrategyName)
	{
		if (markerEditorDialog != null && markerEditorDialog.isDisplayable())
		{
			markerEditorDialog.toFront();
			markerEditorDialog.requestFocus();
			if (focusStrategyName)
			{
				getMarkerEditor().focusStrategyName();
			}
			return;
		}

		DefenderTileMarkerEditor editor = getMarkerEditor();
		editor.setMarkers(editMarkers);
		editor.setEditorEnabled(editorFieldsEditable);
		editor.resetView();

		Window owner = SwingUtilities.getWindowAncestor(this);
		markerEditorDialog = new JDialog(owner, "Configure Tile Markers", Dialog.ModalityType.APPLICATION_MODAL);
		markerEditorDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		markerEditorDialog.setContentPane(editor);
		markerEditorDialog.pack();
		markerEditorDialog.setMinimumSize(new Dimension(980, 720));
		markerEditorDialog.setLocationRelativeTo(null);
		if (focusStrategyName)
		{
			SwingUtilities.invokeLater(editor::focusStrategyName);
		}
		markerEditorDialog.setVisible(true);
		markerEditorDialog = null;
	}

	private void onMarkersChanged(List<DefenderMarker> markers)
	{
		editMarkers = new ArrayList<>(markers);
	}

	private void exportWaveStrategyTemplateToClipboard()
	{
		String json = strategyManager.exportWaveStrategyTemplateJson(buildDraftStrategyFromFields());

		if (json == null)
		{
			JOptionPane.showMessageDialog(markerEditorDialog, "Current wave strategy could not be exported.", "Export Wave Strategy", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
		JOptionPane.showMessageDialog(markerEditorDialog, "Wave strategy copied to clipboard.", "Export Wave Strategy", JOptionPane.INFORMATION_MESSAGE);
	}

	private void importWaveStrategyTemplateFromClipboard()
	{
		if (!editorFieldsEditable)
		{
			JOptionPane.showMessageDialog(markerEditorDialog, "Select an editable strategy before importing.", "Import Wave Strategy", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String json;

		try
		{
			json = (String) Toolkit.getDefaultToolkit()
					.getSystemClipboard()
					.getData(DataFlavor.stringFlavor);
		}
		catch (UnsupportedFlavorException | IOException ex)
		{
			JOptionPane.showMessageDialog(markerEditorDialog, "Clipboard does not contain valid text.", "Import Wave Strategy", JOptionPane.ERROR_MESSAGE);
			return;
		}

		DefenderWaveStrategy selectedStrategy = strategyManager.findWaveStrategy(getSelectedEditStrategyId());
		DefenderWaveStrategy imported = strategyManager.importWaveStrategyTemplateJson(
				json,
				getEditWave(),
				getSelectedEditStrategyId(),
				selectedStrategy != null && selectedStrategy.isBuiltIn()
		);

		if (imported == null)
		{
			JOptionPane.showMessageDialog(markerEditorDialog, "Clipboard text could not be imported as a defender wave strategy.", "Import Wave Strategy", JOptionPane.ERROR_MESSAGE);
			return;
		}

		loadDraftStrategy(imported);
		if (!saveWaveStrategy(markerEditorDialog))
		{
			return;
		}
		JOptionPane.showMessageDialog(markerEditorDialog, "Wave strategy imported.", "Import Wave Strategy", JOptionPane.INFORMATION_MESSAGE);
	}

	private void refreshMarkerEditor()
	{
		if (markerEditor == null)
		{
			return;
		}

		markerEditor.setMarkers(editMarkers);
		markerEditor.setEditorEnabled(editorFieldsEditable);
		markerEditor.refreshMap();
	}

	private void selectEditWaveFromMarkerEditor(int wave)
	{
		selectEditWaveStrategy(wave, getSelectedPresetWaveStrategyId(wave));
	}

	private String getSelectedPresetWaveStrategyId(int wave)
	{
		ComboItem item = (ComboItem) presetCombo.getSelectedItem();
		DefenderRunPreset preset = item == null ? null : strategyManager.findRunPreset(item.id);
		return preset == null ? null : preset.getWaveStrategyId(wave);
	}

	private List<DefenderTileMarkerEditor.StrategyOption> getEditStrategyOptions()
	{
		List<DefenderTileMarkerEditor.StrategyOption> options = new ArrayList<>();
		options.add(new DefenderTileMarkerEditor.StrategyOption(null, "-- New --"));

		for (DefenderWaveStrategy strategy : strategyManager.getWaveStrategiesForWave(getEditWave()))
		{
			options.add(new DefenderTileMarkerEditor.StrategyOption(strategy.getId(), strategy.getName()));
		}

		return options;
	}

	private void selectEditStrategyFromMarkerEditor(String strategyId)
	{
		refreshingEditor = true;
		selectComboValue(editStrategyCombo, strategyId);
		refreshingEditor = false;
		loadSelectedStrategy();
	}

	private void onMarkerEditorNumberOfLogsChanged(int logs)
	{
		if (refreshingEditor)
		{
			return;
		}

		refreshingEditor = true;
		editNumberOfLogs = logs;
		refreshingEditor = false;
	}

	private void onMarkerEditorStrategyNameChanged(String name)
	{
		if (refreshingEditor)
		{
			return;
		}

		refreshingEditor = true;
		strategyName.setText(name == null ? "" : name);
		refreshingEditor = false;
	}

	private void onMarkerEditorNotesChanged(String text)
	{
		if (refreshingEditor)
		{
			return;
		}

		refreshingEditor = true;
		notes.setText(text == null ? "" : text);
		refreshingEditor = false;
	}

	private void refreshPresetCombo()
	{
		String activePresetId = strategyManager.getActiveRunPresetId();
		List<ComboItem> items = new ArrayList<>();
		items.add(new ComboItem(null, ""));

		for (DefenderRunPreset preset : strategyManager.getRunPresets())
		{
			items.add(new ComboItem(preset.getId(), preset.getName()));
		}

		setComboItems(presetCombo, items, activePresetId);
	}

	private void refreshWaveCombos()
	{
		for (Map.Entry<Integer, JComboBox<ComboItem>> entry : waveCombos.entrySet())
		{
			int wave = entry.getKey();
			JComboBox<ComboItem> comboBox = entry.getValue();
			String selectedId = strategyManager.getActiveWaveStrategyId(wave);

			List<ComboItem> items = new ArrayList<>();
			items.add(new ComboItem(null, ""));

			for (DefenderWaveStrategy strategy : strategyManager.getWaveStrategiesForWave(wave))
			{
				items.add(new ComboItem(strategy.getId(), strategy.getName()));
			}

			setComboItems(comboBox, items, selectedId);
		}
	}

	private void selectEditWaveStrategy(int wave, String strategyId)
	{
		refreshingEditor = true;
		selectComboValue(editWaveCombo, String.valueOf(wave));
		refreshingEditor = false;
		refreshEditStrategyCombo(strategyId);
	}

	private void refreshEditStrategyCombo(String selectedStrategyId)
	{
		refreshingEditor = true;
		List<ComboItem> items = new ArrayList<>();
		items.add(new ComboItem(null, "-- New --"));

		for (DefenderWaveStrategy strategy : strategyManager.getWaveStrategiesForWave(getEditWave()))
		{
			items.add(new ComboItem(strategy.getId(), strategy.getName()));
		}

		setComboItems(editStrategyCombo, items, selectedStrategyId);
		refreshingEditor = false;
		loadSelectedStrategy();
	}

	private void loadSelectedStrategy()
	{
		String selectedId = getSelectedEditStrategyId();

		if (selectedId == null)
		{
			clearStrategyFields();
			setEditFieldsEditable(true);
			updateDeleteStrategyAction(null);
			return;
		}

		DefenderWaveStrategy strategy = strategyManager.findWaveStrategy(selectedId);

		if (strategy == null)
		{
			clearStrategyFields();
			setEditFieldsEditable(true);
			updateDeleteStrategyAction(null);
			return;
		}

		refreshingEditor = true;
		try
		{
			strategyName.setText(strategy.getName() == null ? "" : strategy.getName());
			editNumberOfLogs = strategy.getNumberOfLogs();
			notes.setText(strategy.getNotes() == null ? "" : strategy.getNotes());
			editMarkers = new ArrayList<>(strategy.getMarkers());
			setEditFieldsEditable(!strategy.isBuiltIn());
			updateDeleteStrategyAction(strategy);
			refreshMarkerEditor();
		}
		finally
		{
			refreshingEditor = false;
		}
	}

	private void loadDraftStrategy(DefenderWaveStrategy strategy)
	{
		refreshingEditor = true;
		try
		{
			strategyName.setText(strategy.getName() == null ? "" : strategy.getName());
			editNumberOfLogs = strategy.getNumberOfLogs();
			notes.setText(strategy.getNotes() == null ? "" : strategy.getNotes());
			editMarkers = new ArrayList<>(strategy.getMarkers());
			refreshMarkerEditor();
		}
		finally
		{
			refreshingEditor = false;
		}
	}

	private void saveCurrentPreset()
	{
		ComboItem selectedPreset = (ComboItem) presetCombo.getSelectedItem();
		DefenderRunPreset currentPreset = selectedPreset == null ? null : strategyManager.findRunPreset(selectedPreset.id);
		String name = promptName("Preset name", currentPreset == null ? "" : currentPreset.getName());

		if (name == null) return;

		if (currentPreset == null || !name.equalsIgnoreCase(currentPreset.getName()))
		{
			strategyManager.createUserPresetFromActive(name);
		}
		else
		{
			strategyManager.updateUserPreset(currentPreset.getId(), name, strategyManager.getActiveWaveStrategyIds());
		}

		refreshAll();
	}

	private void deleteSelectedPreset()
	{
		ComboItem item = (ComboItem) presetCombo.getSelectedItem();

		if (item == null || item.id == null) return;

		int result = JOptionPane.showConfirmDialog(this, "Delete this defender preset?", "Delete Preset", JOptionPane.OK_CANCEL_OPTION);

		if (result != JOptionPane.OK_OPTION) return;

		strategyManager.deleteUserPreset(item.id);
		refreshAll();
	}

	private void exportSelectedRunPresetToClipboard()
	{
		ComboItem item = (ComboItem) presetCombo.getSelectedItem();

		if (item == null || item.id == null)
		{
			JOptionPane.showMessageDialog(this, "Select a defender preset to export.", "Export Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String json = strategyManager.exportRunPresetJson(item.id);

		if (json == null)
		{
			JOptionPane.showMessageDialog(this, "Selected defender preset could not be exported.", "Export Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int result = JOptionPane.showConfirmDialog(
				this,
				"Copy the selected defender preset to the clipboard?\n\nOnly this preset and its referenced wave strategies will be exported.",
				"Export Preset",
				JOptionPane.OK_CANCEL_OPTION
		);

		if (result != JOptionPane.OK_OPTION) return;

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
		JOptionPane.showMessageDialog(this, "Defender preset copied to clipboard.", "Export Preset", JOptionPane.INFORMATION_MESSAGE);
	}

	private void importRunPresetFromClipboard()
	{
		String json;

		try
		{
			json = (String) Toolkit.getDefaultToolkit()
					.getSystemClipboard()
					.getData(DataFlavor.stringFlavor);
		}
		catch (UnsupportedFlavorException | IOException ex)
		{
			JOptionPane.showMessageDialog(this, "Clipboard does not contain valid text.", "Import Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int result = JOptionPane.showConfirmDialog(
				this,
				"Import one defender preset from the clipboard?\n\nExisting presets with the same name and wave strategies with the same wave/name will be overwritten.",
				"Import Preset",
				JOptionPane.OK_CANCEL_OPTION
		);

		if (result != JOptionPane.OK_OPTION) return;

		if (!strategyManager.importRunPresetJson(json))
		{
			JOptionPane.showMessageDialog(this, "Clipboard text could not be imported as a defender preset.", "Import Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		refreshAll();
		JOptionPane.showMessageDialog(this, "Defender preset imported from clipboard.", "Import Preset", JOptionPane.INFORMATION_MESSAGE);
	}

	private void saveWaveStrategy()
	{
		saveWaveStrategy(this);
	}

	private void saveWaveStrategyFromMarkerEditor()
	{
		if (saveWaveStrategy(markerEditorDialog))
		{
			if (markerEditorDialog != null)
			{
				markerEditorDialog.dispose();
			}
		}
	}

	private boolean saveWaveStrategy(Component parent)
	{
		if (refreshingEditor)
		{
			return false;
		}

		String name = strategyName.getText().trim();

		if (name.isEmpty())
		{
			Toolkit.getDefaultToolkit().beep();
			openMarkerEditorDialog(true);
			return false;
		}

		int wave = getEditWave();
		String selectedId = getSelectedEditStrategyId();
		DefenderWaveStrategy selectedStrategy = strategyManager.findWaveStrategy(selectedId);
		DefenderWaveStrategy strategy = buildStrategyFromFields(wave, name, selectedId, selectedStrategy != null && selectedStrategy.isBuiltIn());
		DefenderWaveStrategy savedStrategy;

		if (selectedStrategy != null && selectedStrategy.isBuiltIn())
		{
			strategyManager.updateBuiltInWaveStrategy(selectedStrategy.getId(), strategy);
			savedStrategy = strategyManager.findWaveStrategy(selectedStrategy.getId());
		}
		else if (selectedId != null && strategyManager.updateUserWaveStrategy(selectedId, strategy))
		{
			savedStrategy = strategyManager.findWaveStrategy(selectedId);
		}
		else
		{
			savedStrategy = strategyManager.createUserWaveStrategy(strategy);
		}

		if (savedStrategy != null)
		{
			strategyManager.setActiveWaveStrategyId(wave, savedStrategy.getId());
		}

		refreshAll();
		selectComboValue(editStrategyCombo, savedStrategy == null ? selectedId : savedStrategy.getId());
		loadSelectedStrategy();
		return true;
	}

	private DefenderWaveStrategy buildStrategyFromFields(int wave, String name, String id, boolean builtIn)
	{
		return new DefenderWaveStrategy(
				id,
				name,
				wave,
				builtIn,
				notes.getText(),
				getNumberOfLogs(),
				editMarkers
		);
	}

	private DefenderWaveStrategy buildDraftStrategyFromFields()
	{
		String selectedId = getSelectedEditStrategyId();
		DefenderWaveStrategy selectedStrategy = strategyManager.findWaveStrategy(selectedId);
		return buildStrategyFromFields(getEditWave(), strategyName.getText().trim(), selectedId, selectedStrategy != null && selectedStrategy.isBuiltIn());
	}

	private void deleteOrResetSelectedStrategy()
	{
		String selectedId = getSelectedEditStrategyId();

		if (selectedId == null) return;

		DefenderWaveStrategy selectedStrategy = strategyManager.findWaveStrategy(selectedId);

		if (selectedStrategy == null) return;

		if (selectedStrategy.isBuiltIn())
		{
			int result = JOptionPane.showConfirmDialog(this, "Reset this built-in defender strategy to its default?", "Reset Wave Strategy", JOptionPane.OK_CANCEL_OPTION);

			if (result != JOptionPane.OK_OPTION) return;

			strategyManager.resetBuiltInWaveStrategy(selectedId);
			refreshAll();
			selectComboValue(editStrategyCombo, selectedId);
			loadSelectedStrategy();
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this custom defender strategy?", "Delete Wave Strategy", JOptionPane.OK_CANCEL_OPTION);

		if (result != JOptionPane.OK_OPTION) return;

		strategyManager.deleteUserWaveStrategy(selectedId);
		clearEditForm();
		refreshAll();
	}

	private void clearEditForm()
	{
		editStrategyCombo.setSelectedIndex(0);
		clearStrategyFields();
		setEditFieldsEditable(true);
		updateDeleteStrategyAction(null);
	}

	private void clearStrategyFields()
	{
		refreshingEditor = true;
		strategyName.setText("");
		editNumberOfLogs = 0;
		notes.setText("");
		editMarkers = new ArrayList<>();
		refreshMarkerEditor();
		refreshingEditor = false;
	}

	private void setEditFieldsEditable(boolean editable)
	{
		editorFieldsEditable = editable;
		strategyName.setEditable(editable);
		notes.setEditable(editable);
		if (markerEditor != null)
		{
			markerEditor.setEditorEnabled(editable);
		}
	}

	private void updateDeleteStrategyAction(DefenderWaveStrategy strategy)
	{
		if (deleteStrategyAction == null) return;

		deleteStrategyAction.setText(strategy != null && strategy.isBuiltIn() ? "Reset" : "Delete");
	}

	private int getEditWave()
	{
		ComboItem waveItem = (ComboItem) editWaveCombo.getSelectedItem();

		if (waveItem == null || waveItem.id == null) return 1;

		return Integer.parseInt(waveItem.id);
	}

	private String getSelectedEditStrategyId()
	{
		ComboItem item = (ComboItem) editStrategyCombo.getSelectedItem();
		return item == null ? null : item.id;
	}

	private int getNumberOfLogs()
	{
		return editNumberOfLogs;
	}

	private String promptName(String title, String defaultValue)
	{
		return promptName(this, title, defaultValue);
	}

	private String promptName(Component parent, String title, String defaultValue)
	{
		JTextField field = new JTextField(defaultValue);
		fixedSize(field, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		int result = JOptionPane.showConfirmDialog(parent, field, title, JOptionPane.OK_CANCEL_OPTION);

		if (result != JOptionPane.OK_OPTION || field.getText().trim().isEmpty()) return null;

		return field.getText().trim();
	}

	private JPanel section(String title)
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(centeredLabelRow(title, true, ColorScheme.DARKER_GRAY_COLOR));
		panel.add(Box.createVerticalStrut(6));
		return panel;
	}

	private JPanel centeredLabelRow(String text, boolean bold, Color background)
	{
		JLabel label = label(text, bold);
		label.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(background);
		row.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.add(label, BorderLayout.CENTER);
		return row;
	}

	private JPanel comboRow(String text, JComboBox<ComboItem> comboBox)
	{
		JLabel rowLabel = label(text);
		rowLabel.setPreferredSize(new Dimension(WAVE_LABEL_WIDTH, CONTROL_HEIGHT));
		rowLabel.setMaximumSize(new Dimension(WAVE_LABEL_WIDTH, CONTROL_HEIGHT));

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.add(rowLabel, BorderLayout.WEST);
		row.add(comboBox, BorderLayout.CENTER);
		return row;
	}

	private JLabel label(String text)
	{
		return label(text, false);
	}

	private JLabel label(String text, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.TEXT_COLOR);
		label.setFont(bold ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private JPanel labelWithInfo(String text, int width)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setPreferredSize(new Dimension(width, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(width, CONTROL_HEIGHT));

		JLabel info = new JLabel(BaIcons.infoIcon());
		info.setToolTipText(NOTES_TOOLTIP);
		row.add(label(text));
		row.add(Box.createHorizontalStrut(5));
		row.add(info);
		row.add(Box.createHorizontalGlue());
		return row;
	}

	private JButton action(String text, Runnable runnable)
	{
		JButton button = new JButton(text);
		button.addActionListener(event -> runnable.run());
		fixedSize(button, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		return button;
	}

	private JPanel horizontalActionRow()
	{
		JPanel panel = new JPanel(new DynamicGridLayout(1, 2, 6, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		return panel;
	}

	private static JPanel verticalPanel(Color background)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(background);
		return panel;
	}

	private JScrollPane wrapTextArea(JTextArea area, int height)
	{
		JScrollPane scrollPane = new JScrollPane(area);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
		fixedSize(scrollPane, CONTENT_WIDTH - 16, height);
		return scrollPane;
	}

	private void styleCombo(JComboBox<?> comboBox, int width)
	{
		comboBox.setFocusable(false);
		fixedSize(comboBox, width, CONTROL_HEIGHT);
	}

	private void styleTextArea(JTextArea area, int rows)
	{
		area.setRows(rows);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
	}

	private static void fixedSize(JComponent component, int width, int height)
	{
		Dimension size = new Dimension(width, height);
		component.setPreferredSize(size);
		component.setMinimumSize(size);
		component.setMaximumSize(size);
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	private void selectComboValue(JComboBox<ComboItem> comboBox, String id)
	{
		for (int i = 0; i < comboBox.getItemCount(); i++)
		{
			ComboItem item = comboBox.getItemAt(i);
			if ((id == null && item.id == null) || (id != null && id.equals(item.id)))
			{
				comboBox.setSelectedIndex(i);
				return;
			}
		}

		if (comboBox.getItemCount() > 0)
		{
			comboBox.setSelectedIndex(0);
		}
	}

	private void setComboItems(JComboBox<ComboItem> comboBox, List<ComboItem> items, String selectedId)
	{
		comboBox.setModel(new DefaultComboBoxModel<>(items.toArray(new ComboItem[0])));
		selectComboValue(comboBox, selectedId);
	}

	private static class ComboItem
	{
		private final String id;
		private String label;

		private ComboItem(String id, String label)
		{
			this.id = id;
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private static class PopoutIcon implements Icon
	{
		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y)
		{
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			graphics2D.setColor(component.isEnabled() ? component.getForeground() : Color.GRAY);
			graphics2D.setStroke(new BasicStroke(1.4f));
			graphics2D.drawRect(x + 1, y + 5, 9, 9);
			graphics2D.drawLine(x + 7, y + 1, x + 14, y + 1);
			graphics2D.drawLine(x + 14, y + 1, x + 14, y + 8);
			graphics2D.drawLine(x + 7, y + 8, x + 14, y + 1);
			graphics2D.dispose();
		}

		@Override
		public int getIconWidth()
		{
			return 16;
		}

		@Override
		public int getIconHeight()
		{
			return 16;
		}
	}
}
