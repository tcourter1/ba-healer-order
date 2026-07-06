package com.bahealerorder.sidepanel.healercodes;

import com.bahealerorder.common.BaClipboard;
import com.bahealerorder.common.BaIcons;
import com.bahealerorder.healer.HealerCodeManager;
import com.bahealerorder.healer.codes.HealerCodeExportResult;
import com.bahealerorder.healer.codes.RunPreset;
import com.bahealerorder.healer.codes.WaveCode;
import com.bahealerorder.sidepanel.BaPanelUi;
import com.bahealerorder.sidepanel.BaTransferDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

@Singleton
public class HealerCodePanel extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
	private static final int WAVE_ROW_GAP = 8;
	private static final int WAVE_ROW_HORIZONTAL_PADDING = 8;
	private static final int WAVE_ROW_CONTROL_WIDTH = CONTENT_WIDTH - WAVE_ROW_HORIZONTAL_PADDING * 2;
	private static final int WAVE_LABEL_WIDTH = 54;
	private static final int WAVE_MENU_BUTTON_WIDTH = 24;
	private static final int WAVE_COMBO_WIDTH = WAVE_ROW_CONTROL_WIDTH - WAVE_LABEL_WIDTH - WAVE_MENU_BUTTON_WIDTH - 12;
	private static final int PRESET_CONTROL_WIDTH = CONTENT_WIDTH - 16;
	private static final int ACTION_ROW_GAP = 6;
	private static final int ACTION_BUTTON_WIDTH = (PRESET_CONTROL_WIDTH - ACTION_ROW_GAP) / 2;

	private final HealerCodeManager codeManager;
	private final JPanel contentPanel = new JPanel();
	private final JPanel waveRowsPanel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final JComboBox<PresetOption> presetCombo = new JComboBox<>();

	private JDialog editorDialog;
	private HealerCodeEditor editorPanel;
	private boolean refreshing;

	@Inject
	public HealerCodePanel(HealerCodeManager codeManager)
	{
		this.codeManager = codeManager;

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());
		setAlignmentX(LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 0, 5));
		add(contentPanel, BorderLayout.NORTH);

		contentPanel.add(createEditorButton());
		contentPanel.add(Box.createVerticalStrut(12));
		contentPanel.add(createPresetSection());

		refreshAll();
	}

	public void refreshAll()
	{
		refreshing = true;
		try
		{
			refreshPresetCombo();
			refreshWaveRows();
		}
		finally
		{
			refreshing = false;
		}
	}

	public void refreshLater()
	{
		SwingUtilities.invokeLater(this::refreshAll);
	}

	private JButton createEditorButton()
	{
		JButton button = BaPanelUi.action("Open Healer Code Editor", this::openDefaultEditorDialog, CONTENT_WIDTH - 10, CONTROL_HEIGHT);
		button.setIcon(BaIcons.popoutIcon());
		button.setHorizontalTextPosition(SwingConstants.LEADING);
		button.setIconTextGap(8);
		button.setToolTipText("Open Healer Code Editor");
		return button;
	}

	private JPanel createPresetSection()
	{
		JPanel section = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(new EmptyBorder(8, 8, 8, 8));
		section.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		section.setAlignmentX(LEFT_ALIGNMENT);

		JLabel title = BaPanelUi.label("Healer Codes", true);
		title.setHorizontalAlignment(SwingConstants.CENTER);
		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(titleRow, PRESET_CONTROL_WIDTH, CONTROL_HEIGHT);
		titleRow.add(title, BorderLayout.CENTER);
		section.add(titleRow);
		section.add(Box.createVerticalStrut(6));

		BaPanelUi.styleCombo(presetCombo, PRESET_CONTROL_WIDTH, CONTROL_HEIGHT);
		presetCombo.addActionListener(event ->
		{
			if (refreshing)
			{
				return;
			}

			PresetOption item = (PresetOption) presetCombo.getSelectedItem();
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
		section.add(BaPanelUi.action("Save Current Selections", this::saveCurrentPreset, PRESET_CONTROL_WIDTH, CONTROL_HEIGHT));
		section.add(Box.createVerticalStrut(5));

		JPanel presetActionRow = BaPanelUi.horizontalActionRow(PRESET_CONTROL_WIDTH, CONTROL_HEIGHT);
		presetActionRow.add(BaPanelUi.action("Delete", this::deleteSelectedPreset, ACTION_BUTTON_WIDTH, CONTROL_HEIGHT));
		presetActionRow.add(BaPanelUi.action("Clear", this::clearSelections, ACTION_BUTTON_WIDTH, CONTROL_HEIGHT));
		section.add(presetActionRow);
		section.add(Box.createVerticalStrut(5));

		JPanel transferRow = BaPanelUi.horizontalActionRow(PRESET_CONTROL_WIDTH, CONTROL_HEIGHT);
		transferRow.add(BaPanelUi.action("Import", this::importFromClipboard, ACTION_BUTTON_WIDTH, CONTROL_HEIGHT));
		transferRow.add(BaPanelUi.action("Export", this::exportSelectedPresetToClipboard, ACTION_BUTTON_WIDTH, CONTROL_HEIGHT));
		section.add(transferRow);
		section.add(Box.createVerticalStrut(26));

		waveRowsPanel.setAlignmentX(LEFT_ALIGNMENT);
		section.add(waveRowsPanel);
		return section;
	}

	private void refreshPresetCombo()
	{
		boolean wasRefreshing = refreshing;
		refreshing = true;
		try
		{
			String activePresetId = codeManager.getActiveRunPresetId();
			presetCombo.removeAllItems();
			presetCombo.addItem(new PresetOption(null, "Select a preset..."));

			for (RunPreset preset : codeManager.getRunPresets())
			{
				presetCombo.addItem(new PresetOption(preset.getId(), HealerCodeManager.runPresetDisplayName(preset)));
			}

			selectPresetComboValue(activePresetId);
		}
		finally
		{
			refreshing = wasRefreshing;
		}
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
		BaPanelUi.fixedSize(waveLabel, WAVE_LABEL_WIDTH, CONTROL_HEIGHT);

		JComboBox<WaveCodeOption> comboBox = new JComboBox<>();
		comboBox.setRenderer(new WaveCodeOptionRenderer());
		BaPanelUi.styleCombo(comboBox, WAVE_COMBO_WIDTH, CONTROL_HEIGHT);
		populateWaveCombo(comboBox, wave, codeManager.getActiveWaveCodeId(wave));
		comboBox.addActionListener(event ->
		{
			if (refreshing)
			{
				return;
			}

			WaveCodeOption option = (WaveCodeOption) comboBox.getSelectedItem();
			codeManager.setActiveWaveCodeId(wave, option == null ? null : option.id);
			refreshPresetCombo();
		});
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(0, WAVE_ROW_HORIZONTAL_PADDING, 0, WAVE_ROW_HORIZONTAL_PADDING));
		BaPanelUi.fixedSize(row, CONTENT_WIDTH, CONTROL_HEIGHT);
		row.add(waveLabel, BorderLayout.WEST);
		row.add(comboBox, BorderLayout.CENTER);
		row.add(createWaveMenuButton(wave, comboBox), BorderLayout.EAST);
		return row;
	}

	private JButton createWaveMenuButton(int wave, JComboBox<WaveCodeOption> comboBox)
	{
		JButton button = new JButton(BaIcons.verticalEllipsisIcon());
		button.addActionListener(event -> createWaveActionMenu(wave, comboBox).show(button, 0, button.getHeight()));
		SwingUtil.removeButtonDecorations(button);
		BaPanelUi.fixedSize(button, WAVE_MENU_BUTTON_WIDTH, CONTROL_HEIGHT);
		return button;
	}

	private JPopupMenu createWaveActionMenu(int wave, JComboBox<WaveCodeOption> comboBox)
	{
		JPopupMenu menu = new JPopupMenu();
		menu.add(menuItem("Add", BaIcons.plusIcon(), () -> openEditorDialog(wave, null)));
		menu.add(menuItem("Edit", BaIcons.pencilIcon(), () ->
		{
			String codeId = selectedWaveCodeId(comboBox);
			if (codeId == null)
			{
				Toolkit.getDefaultToolkit().beep();
				return;
			}
			openEditorDialog(wave, codeId);
		}));
		String selectedCodeId = selectedWaveCodeId(comboBox);
		WaveCode selectedCode = codeManager.findWaveCode(selectedCodeId);
		if (codeManager.isModifiedBuiltInWaveCode(selectedCodeId))
		{
			menu.add(menuItem("Reset", BaIcons.resetIcon(), () -> resetSelectedBuiltInWaveCode(selectedCodeId)));
		}
		else if (selectedCode != null && !selectedCode.isBuiltIn())
		{
			menu.add(menuItem("Delete", BaIcons.trashIcon(), () -> deleteSelectedWaveCode(selectedCodeId)));
		}
		menu.add(menuItem("Import", BaIcons.importIcon(), () -> importWaveCodeFromClipboard(wave)));
		menu.add(menuItem("Export", BaIcons.exportIcon(), () -> exportWaveCodeToClipboard(wave)));
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

	private void populateWaveCombo(JComboBox<WaveCodeOption> comboBox, int wave, String selectedCodeId)
	{
		comboBox.removeAllItems();
		comboBox.addItem(new WaveCodeOption(null, "", false));
		for (WaveCode code : codeManager.getWaveCodesForWave(wave))
		{
			comboBox.addItem(new WaveCodeOption(code.getId(), code.getName(), code.isBuiltIn()));
		}
		selectWaveComboValue(comboBox, selectedCodeId);
	}

	private void openDefaultEditorDialog()
	{
		openEditorDialog(5, null);
	}

	private void openEditorDialog(int wave, String codeId)
	{
		if (editorDialog != null && editorDialog.isDisplayable())
		{
			if (editorPanel != null && !editorPanel.selectCodeForWave(wave, codeId, editorDialog))
			{
				return;
			}
			editorDialog.toFront();
			editorDialog.requestFocus();
			return;
		}

		HealerCodeEditor editor = new HealerCodeEditor(codeManager, this::refreshAll, wave, codeId);
		editorPanel = editor;
		Window owner = SwingUtilities.getWindowAncestor(this);
		editorDialog = new JDialog(owner, "Healer Code Editor", java.awt.Dialog.ModalityType.MODELESS);
		editorDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		editorDialog.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				if (editor.confirmDiscard(editorDialog))
				{
					editorDialog.dispose();
				}
			}
		});
		editorDialog.setContentPane(editor);
		editorDialog.pack();
		editorDialog.setMinimumSize(new Dimension(980, 760));
		editorDialog.setLocationRelativeTo(null);
		editorDialog.setVisible(true);
	}

	private void saveCurrentPreset()
	{
		PresetOption selectedPreset = (PresetOption) presetCombo.getSelectedItem();
		RunPreset currentPreset = selectedPreset == null ? null : codeManager.findRunPreset(selectedPreset.id);
		String name = promptName("Preset name", currentPreset == null ? "" : currentPreset.getName());
		if (name == null)
		{
			return;
		}

		if (currentPreset == null || !name.equalsIgnoreCase(currentPreset.getName()))
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
		PresetOption item = (PresetOption) presetCombo.getSelectedItem();
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

	private void deleteSelectedWaveCode(String codeId)
	{
		WaveCode code = codeManager.findWaveCode(codeId);
		if (code == null || code.isBuiltIn())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this wave code?", "Delete Wave Code", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		codeManager.deleteUserWaveCode(codeId);
		refreshAll();
	}

	private void resetSelectedBuiltInWaveCode(String codeId)
	{
		if (!codeManager.isModifiedBuiltInWaveCode(codeId))
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Reset this built-in wave code to its default?", "Reset Wave Code", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		codeManager.resetBuiltInWaveCode(codeId);
		refreshAll();
	}

	private void clearSelections()
	{
		codeManager.clearActiveSelections();
		refreshAll();
	}

	private void exportSelectedPresetToClipboard()
	{
		RunPreset activePreset = codeManager.findRunPreset(codeManager.getActiveRunPresetId());
		HealerCodeExportResult result = codeManager.exportCurrentRunPreset(activePreset == null ? null : activePreset.getName());

		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "No healer codes are currently selected.", "Export Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		BaClipboard.copyText(result.getJson());
		BaTransferDialog.show(this, "Export Preset", "Exported " + result.getTypedName() + ".", "Export", result.getSummaryLines());
	}

	private void exportWaveCodeToClipboard(int wave)
	{
		String codeId = codeManager.getActiveWaveCodeId(wave);
		if (codeId == null)
		{
			JOptionPane.showMessageDialog(this, "No healer code is selected for Wave " + wave + ".", "Export Wave", JOptionPane.ERROR_MESSAGE);
			return;
		}

		HealerCodeExportResult result = codeManager.exportWaveCode(codeId);
		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "Wave " + wave + " could not be exported.", "Export Wave", JOptionPane.ERROR_MESSAGE);
			return;
		}

		BaClipboard.copyText(result.getJson());
		BaTransferDialog.show(this, "Export Wave", "Exported " + result.getTypedName() + ".", "Export", result.getSummaryLines());
	}

	private void importFromClipboard()
	{
		String json = BaClipboard.readText(this, "Import Healer Codes");
		if (json == null)
		{
			return;
		}

		HealerCodeExportResult result = codeManager.importHealerCodeJson(json);
		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "Clipboard text could not be imported as a healer code export.", "Import Healer Codes", JOptionPane.ERROR_MESSAGE);
			return;
		}

		refreshAll();
		BaTransferDialog.show(this, "Import Healer Codes", "Imported " + result.getTypedName() + ".", "Import", result.getSummaryLines());
	}

	private void importWaveCodeFromClipboard(int wave)
	{
		String json = BaClipboard.readText(this, "Import Wave");
		if (json == null)
		{
			return;
		}

		HealerCodeExportResult result = codeManager.importHealerCodeJson(json, wave);
		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "Clipboard text could not be imported for Wave " + wave + ".", "Import Wave", JOptionPane.ERROR_MESSAGE);
			return;
		}

		refreshAll();
		BaTransferDialog.show(this, "Import Wave", "Imported and selected " + result.getTypedName() + ".", "Import", result.getSummaryLines());
	}

	private String selectedWaveCodeId(JComboBox<WaveCodeOption> comboBox)
	{
		WaveCodeOption option = (WaveCodeOption) comboBox.getSelectedItem();
		return option == null ? null : option.id;
	}

	private String promptName(String title, String defaultValue)
	{
		JTextField field = new JTextField(defaultValue);
		BaPanelUi.fixedSize(field, PRESET_CONTROL_WIDTH, CONTROL_HEIGHT);
		int result = JOptionPane.showConfirmDialog(this, field, title, JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION || field.getText().trim().isEmpty())
		{
			return null;
		}
		return field.getText().trim();
	}

	private void selectPresetComboValue(String id)
	{
		for (int i = 0; i < presetCombo.getItemCount(); i++)
		{
			PresetOption item = presetCombo.getItemAt(i);
			if ((id == null && item.id == null) || (id != null && id.equals(item.id)))
			{
				presetCombo.setSelectedIndex(i);
				return;
			}
		}

		if (presetCombo.getItemCount() > 0)
		{
			presetCombo.setSelectedIndex(0);
		}
	}

	private void selectWaveComboValue(JComboBox<WaveCodeOption> comboBox, String id)
	{
		for (int i = 0; i < comboBox.getItemCount(); i++)
		{
			WaveCodeOption item = comboBox.getItemAt(i);
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

	private static class PresetOption
	{
		private final String id;
		private final String label;

		private PresetOption(String id, String label)
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

	private static class WaveCodeOption
	{
		private final String id;
		private final String label;
		private final boolean builtIn;

		private WaveCodeOption(String id, String label, boolean builtIn)
		{
			this.id = id;
			this.label = label;
			this.builtIn = builtIn;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private static class WaveCodeOptionRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(
				JList<?> list,
				Object value,
				int index,
				boolean isSelected,
			boolean cellHasFocus)
		{
			Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (!isSelected)
			{
				component.setForeground(value instanceof WaveCodeOption && ((WaveCodeOption) value).builtIn
						? new Color(120, 120, 120)
						: BaPanelUi.ACTION_CONTROL_TEXT_COLOR);
			}
			return component;
		}
	}
}
