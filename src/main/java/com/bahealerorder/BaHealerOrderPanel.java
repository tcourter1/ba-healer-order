package com.bahealerorder;

import com.bahealerorder.codes.RunPreset;
import com.bahealerorder.codes.WaveCode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public class BaHealerOrderPanel extends PluginPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
	private static final int CREATE_CODE_GAP = 24;
	private static final int PRESET_BUTTON_WAVE_GAP = 26;
	private static final int WAVE_LABEL_WIDTH = 48;
	private static final Font TITLE_FONT = FontManager.getRunescapeBoldFont();
	private static final Font LABEL_FONT = FontManager.getRunescapeSmallFont();

	private final BaHealerCodeManager codeManager;
	private final JComboBox<ComboItem> presetCombo = new JComboBox<>();
	private final Map<Integer, JComboBox<ComboItem>> waveCombos = new HashMap<>();
	private final JComboBox<ComboItem> importWaveCombo = new JComboBox<>();
	private final JComboBox<ComboItem> userWaveCodeCombo = new JComboBox<>();
	private final JTextField importName = new JTextField();
	private final JTextArea importCode = new JTextArea();
	private final JPanel contentPanel = new JPanel();
	private JButton deleteWaveCodeAction;

	private boolean refreshing;
	private boolean refreshingImport;

	@Inject
	public BaHealerOrderPanel(BaHealerCodeManager codeManager)
	{
		this.codeManager = codeManager;

		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(contentPanel, BorderLayout.NORTH);

		contentPanel.add(header("BA Healer Utilities"));
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(createPresetSection());
		contentPanel.add(Box.createVerticalStrut(CREATE_CODE_GAP));
		contentPanel.add(createImportCodeSection());

		refreshAll();
	}

	public void refreshAll()
	{
		refreshing = true;
		refreshPresetCombo();
		refreshWaveCombos();
		refreshUserWaveCodeCombo();
		refreshing = false;
	}

	private JPanel createPresetSection()
	{
		JPanel section = section("Run Preset");
		styleCombo(presetCombo);
		presetCombo.addActionListener(event ->
		{
			if (refreshing)
			{
				return;
			}

			ComboItem item = (ComboItem) presetCombo.getSelectedItem();
			if (item == null || item.id == null)
			{
				codeManager.clearActiveSelections();
			}
			else
			{
				codeManager.applyRunPreset(item.id);
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
			codeManager.clearActiveSelections();
			refreshAll();
		}));
		section.add(presetActionRow);
		section.add(Box.createVerticalStrut(5));
		JPanel jsonActionRow = horizontalActionRow();
		jsonActionRow.add(action("Import", this::importRunPresetFromClipboard));
		jsonActionRow.add(action("Export", this::exportSelectedRunPresetToClipboard));
		section.add(jsonActionRow);
		section.add(Box.createVerticalStrut(PRESET_BUTTON_WAVE_GAP));
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
				if (refreshing)
				{
					return;
				}

				ComboItem item = (ComboItem) comboBox.getSelectedItem();
				codeManager.setActiveWaveCodeId(selectedWave, item == null ? null : item.id);
				selectImportWaveCode(selectedWave, item == null ? null : item.id);
				refreshing = true;
				refreshPresetCombo();
				refreshing = false;
			});
			waveCombos.put(wave, comboBox);
			section.add(comboRow("Wave " + wave, comboBox));
			section.add(Box.createVerticalStrut(6));
		}
	}

	private JPanel createImportCodeSection()
	{
		JPanel section = section("Create Wave Code");

		for (int wave = 1; wave <= 10; wave++)
		{
			importWaveCombo.addItem(new ComboItem(String.valueOf(wave), "Wave " + wave));
		}
		styleCombo(importWaveCombo);
		importWaveCombo.addActionListener(event ->
		{
			if (!refreshingImport)
			{
				refreshUserWaveCodeCombo();
			}
		});
		section.add(label("Wave"));
		section.add(Box.createVerticalStrut(3));
		section.add(importWaveCombo);
		section.add(Box.createVerticalStrut(6));

		styleCombo(userWaveCodeCombo);
		userWaveCodeCombo.addActionListener(event ->
		{
			if (!refreshingImport)
			{
				loadSelectedUserWaveCode();
			}
		});
		section.add(label("Existing Code"));
		section.add(Box.createVerticalStrut(3));
		section.add(userWaveCodeCombo);
		section.add(Box.createVerticalStrut(6));

		styleTextField(importName);
		section.add(label("Name"));
		section.add(Box.createVerticalStrut(3));
		section.add(importName);
		section.add(Box.createVerticalStrut(6));

		styleTextArea(importCode, 7);
		section.add(label("Code"));
		section.add(Box.createVerticalStrut(3));
		section.add(wrapTextArea(importCode, 120));
		section.add(Box.createVerticalStrut(6));
		section.add(action("Save Wave Code", this::saveImportedWaveCode));
		section.add(Box.createVerticalStrut(5));
		JPanel saveDeleteRow = horizontalActionRow();
		deleteWaveCodeAction = action("Delete", this::deleteOrResetSelectedWaveCode);
		saveDeleteRow.add(deleteWaveCodeAction);
		saveDeleteRow.add(action("Clear", this::clearImportForm));
		section.add(saveDeleteRow);
		return section;
	}

	private void refreshPresetCombo()
	{
		String activePresetId = codeManager.getActiveRunPresetId();
		presetCombo.removeAllItems();
		presetCombo.addItem(new ComboItem(null, ""));

		for (RunPreset preset : codeManager.getRunPresets())
		{
			presetCombo.addItem(new ComboItem(preset.getId(), preset.getName()));
		}

		selectComboValue(presetCombo, activePresetId);
	}

	private void refreshWaveCombos()
	{
		for (Map.Entry<Integer, JComboBox<ComboItem>> entry : waveCombos.entrySet())
		{
			int wave = entry.getKey();
			JComboBox<ComboItem> comboBox = entry.getValue();
			String selectedCodeId = codeManager.getActiveWaveCodeId(wave);

			comboBox.removeAllItems();
			comboBox.addItem(new ComboItem(null, ""));

			for (WaveCode code : codeManager.getWaveCodesForWave(wave))
			{
				comboBox.addItem(new ComboItem(code.getId(), code.getName()));
			}

			selectComboValue(comboBox, selectedCodeId);
		}
	}

	private void refreshUserWaveCodeCombo()
	{
		refreshUserWaveCodeCombo(getSelectedUserWaveCodeId());
	}

	private void selectImportWaveCode(int wave, String waveCodeId)
	{
		refreshingImport = true;
		selectComboValue(importWaveCombo, String.valueOf(wave));
		refreshingImport = false;
		refreshUserWaveCodeCombo(waveCodeId);
	}

	private void refreshUserWaveCodeCombo(String selectedWaveCodeId)
	{
		refreshingImport = true;
		userWaveCodeCombo.removeAllItems();
		userWaveCodeCombo.addItem(new ComboItem(null, ""));

		for (WaveCode code : codeManager.getWaveCodesForWave(getImportWave()))
		{
			userWaveCodeCombo.addItem(new ComboItem(code.getId(), code.getName()));
		}

		selectComboValue(userWaveCodeCombo, selectedWaveCodeId);
		refreshingImport = false;
		loadSelectedUserWaveCode();
	}

	private void loadSelectedUserWaveCode()
	{
		String selectedId = getSelectedUserWaveCodeId();

		if (selectedId == null)
		{
			importName.setText("");
			importCode.setText("");
			setImportFieldsEditable(true, true);
			updateDeleteWaveCodeAction(null);
			return;
		}

		WaveCode code = codeManager.findWaveCode(selectedId);

		if (code == null)
		{
			setImportFieldsEditable(true, true);
			updateDeleteWaveCodeAction(null);
			return;
		}

		importName.setText(code.getName() == null ? "" : code.getName());
		importCode.setText(code.getSourceText() == null ? "" : code.getSourceText());
		setImportFieldsEditable(!code.isBuiltIn(), true);
		updateDeleteWaveCodeAction(code);
	}

	private void saveCurrentPreset()
	{
		ComboItem selectedPreset = (ComboItem) presetCombo.getSelectedItem();
		RunPreset currentPreset = selectedPreset == null ? null : codeManager.findRunPreset(selectedPreset.id);
		String name = promptName("Preset name", currentPreset == null ? "" : currentPreset.getName());

		if (name == null)
		{
			return;
		}

		if (currentPreset == null)
		{
			codeManager.createUserPresetFromActive(name);
		}
		else if (!name.equalsIgnoreCase(currentPreset.getName()))
		{
			codeManager.createUserPresetFromActive(name);
		}
		else
		{
			codeManager.updateUserPreset(currentPreset.getId(), name, codeManager.getActiveWaveCodeIds());
		}

		refreshAll();
	}

	private void deleteSelectedPreset()
	{
		ComboItem item = (ComboItem) presetCombo.getSelectedItem();

		if (item == null || item.id == null)
		{
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this run preset?", "Delete Preset", JOptionPane.OK_CANCEL_OPTION);

		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		codeManager.deleteUserPreset(item.id);
		refreshAll();
	}

	private void exportSelectedRunPresetToClipboard()
	{
		ComboItem item = (ComboItem) presetCombo.getSelectedItem();

		if (item == null || item.id == null)
		{
			JOptionPane.showMessageDialog(this, "Select a run preset to export.", "Export Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String json = codeManager.exportRunPresetJson(item.id);

		if (json == null)
		{
			JOptionPane.showMessageDialog(this, "Selected run preset could not be exported.", "Export Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int result = JOptionPane.showConfirmDialog(
				this,
				"Copy the selected run preset to the clipboard?\n\nOnly this preset and its referenced wave codes will be exported.",
				"Export Preset",
				JOptionPane.OK_CANCEL_OPTION
		);

		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		StringSelection contents = new StringSelection(json);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(contents, null);
		JOptionPane.showMessageDialog(this, "Run preset copied to clipboard.", "Export Preset", JOptionPane.INFORMATION_MESSAGE);
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
				"Import one run preset from the clipboard?\n\nExisting presets with the same name and wave codes with the same wave/name will be overwritten.",
				"Import Preset",
				JOptionPane.OK_CANCEL_OPTION
		);

		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		if (!codeManager.importRunPresetJson(json))
		{
			JOptionPane.showMessageDialog(this, "Clipboard text could not be imported as a run preset.", "Import Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		refreshAll();
		JOptionPane.showMessageDialog(this, "Run preset imported from clipboard.", "Import Preset", JOptionPane.INFORMATION_MESSAGE);
	}

	private void saveImportedWaveCode()
	{
		String name = importName.getText().trim();
		String code = importCode.getText().trim();

		if (name.isEmpty() || code.isEmpty())
		{
			return;
		}

		int wave = getImportWave();
		String selectedId = getSelectedUserWaveCodeId();
		WaveCode selectedCode = codeManager.findWaveCode(selectedId);

		WaveCode waveCode;

		if (selectedCode != null && selectedCode.isBuiltIn())
		{
			codeManager.updateBuiltInWaveCode(selectedCode.getId(), code);
			waveCode = codeManager.findWaveCode(selectedCode.getId());
		}
		else if (selectedId != null && codeManager.updateUserWaveCode(selectedId, wave, name, code))
		{
			waveCode = codeManager.findWaveCode(selectedId);
		}
		else
		{
			waveCode = codeManager.createUserWaveCode(wave, name, code);
		}

		if (waveCode != null)
		{
			codeManager.setActiveWaveCodeId(wave, waveCode.getId());
		}

		refreshAll();
		selectComboValue(userWaveCodeCombo, waveCode == null ? selectedId : waveCode.getId());
		loadSelectedUserWaveCode();
	}

	private void deleteOrResetSelectedWaveCode()
	{
		String selectedId = getSelectedUserWaveCodeId();

		if (selectedId == null)
		{
			return;
		}

		WaveCode selectedCode = codeManager.findWaveCode(selectedId);

		if (selectedCode == null)
		{
			return;
		}

		if (selectedCode.isBuiltIn())
		{
			resetSelectedBuiltInWaveCode(selectedCode);
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this custom wave code?", "Delete Wave Code", JOptionPane.OK_CANCEL_OPTION);

		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		codeManager.deleteUserWaveCode(selectedId);
		clearImportForm();
		refreshAll();
	}

	private void clearImportForm()
	{
		userWaveCodeCombo.setSelectedIndex(0);
		importName.setText("");
		importCode.setText("");
		setImportFieldsEditable(true, true);
		updateDeleteWaveCodeAction(null);
	}

	private void resetSelectedBuiltInWaveCode(WaveCode selectedCode)
	{
		int result = JOptionPane.showConfirmDialog(this, "Reset this built-in wave code to its default?", "Reset Wave Code", JOptionPane.OK_CANCEL_OPTION);

		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		codeManager.resetBuiltInWaveCode(selectedCode.getId());
		refreshAll();
		selectComboValue(userWaveCodeCombo, selectedCode.getId());
		loadSelectedUserWaveCode();
	}

	private void setImportFieldsEditable(boolean nameEditable, boolean codeEditable)
	{
		importName.setEditable(nameEditable);
		importCode.setEditable(codeEditable);
	}

	private void updateDeleteWaveCodeAction(WaveCode code)
	{
		if (deleteWaveCodeAction == null)
		{
			return;
		}

		deleteWaveCodeAction.setText(code != null && code.isBuiltIn() ? "Reset" : "Delete");
	}

	private String promptName(String title, String defaultValue)
	{
		JTextField field = new JTextField(defaultValue);
		styleTextField(field);
		int result = JOptionPane.showConfirmDialog(this, field, title, JOptionPane.OK_CANCEL_OPTION);

		if (result != JOptionPane.OK_OPTION || field.getText().trim().isEmpty())
		{
			return null;
		}

		return field.getText().trim();
	}

	private int getImportWave()
	{
		ComboItem waveItem = (ComboItem) importWaveCombo.getSelectedItem();

		if (waveItem == null || waveItem.id == null)
		{
			return 1;
		}

		return Integer.parseInt(waveItem.id);
	}

	private String getSelectedUserWaveCodeId()
	{
		ComboItem item = (ComboItem) userWaveCodeCombo.getSelectedItem();
		return item == null ? null : item.id;
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

	private JPanel header(String text)
	{
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		separator.setForeground(ColorScheme.BORDER_COLOR);
		separator.setBackground(ColorScheme.DARK_GRAY_COLOR);
		separator.setPreferredSize(new Dimension(CONTENT_WIDTH, 1));
		separator.setMaximumSize(new Dimension(CONTENT_WIDTH, 1));
		separator.setAlignmentX(LEFT_ALIGNMENT);

		JPanel panel = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH, 34));
		panel.add(centeredLabelRow(text, true, ColorScheme.DARK_GRAY_COLOR));
		panel.add(Box.createVerticalStrut(5));
		panel.add(separator);
		return panel;
	}

	private JPanel centeredLabelRow(String text, boolean bold, java.awt.Color background)
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
		label.setFont(bold ? TITLE_FONT : LABEL_FONT);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private JButton action(String text, Runnable runnable)
	{
		JButton button = new JButton(text);
		button.addActionListener(event -> runnable.run());
		button.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		button.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		button.setAlignmentX(LEFT_ALIGNMENT);
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

	private static JPanel verticalPanel(java.awt.Color background)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(background);
		return panel;
	}

	private void styleCombo(JComboBox<ComboItem> comboBox)
	{
		styleCombo(comboBox, CONTENT_WIDTH - 16);
	}

	private void styleCombo(JComboBox<ComboItem> comboBox, int width)
	{
		Dimension size = new Dimension(width, CONTROL_HEIGHT);
		comboBox.setFocusable(false);
		comboBox.setPreferredSize(size);
		comboBox.setMinimumSize(size);
		comboBox.setMaximumSize(size);
		comboBox.setAlignmentX(LEFT_ALIGNMENT);
	}

	private void styleTextField(JTextField field)
	{
		Dimension size = new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		field.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		field.setForeground(ColorScheme.TEXT_COLOR);
		field.setCaretColor(ColorScheme.TEXT_COLOR);
		field.setBorder(new EmptyBorder(5, 5, 5, 5));
		field.setPreferredSize(size);
		field.setMaximumSize(size);
		field.setAlignmentX(LEFT_ALIGNMENT);
	}

	private void styleTextArea(JTextArea area, int rows)
	{
		area.setRows(rows);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		area.setForeground(ColorScheme.TEXT_COLOR);
		area.setCaretColor(ColorScheme.TEXT_COLOR);
		area.setBorder(new EmptyBorder(5, 5, 5, 5));
	}

	private JScrollPane wrapTextArea(JTextArea area, int height)
	{
		JScrollPane scrollPane = new JScrollPane(area);
		scrollPane.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		scrollPane.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, height));
		scrollPane.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, height));
		scrollPane.setAlignmentX(LEFT_ALIGNMENT);
		return scrollPane;
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

	public void refreshLater()
	{
		SwingUtilities.invokeLater(this::refreshAll);
	}

	private static class ComboItem
	{
		private final String id;
		private final String label;

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
}
