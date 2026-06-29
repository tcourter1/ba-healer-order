package com.bahealerorder.healer;

import com.bahealerorder.common.BaPanelUi;
import com.bahealerorder.common.BaPanelUi.ComboOption;
import com.bahealerorder.common.WavePresetSection;
import com.bahealerorder.healer.codes.RunPreset;
import com.bahealerorder.healer.codes.WaveCode;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

@Singleton
public class HealerCodePanel extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
	private static final int CREATE_CODE_GAP = 24;
	private static final int WAVE_LABEL_WIDTH = 48;

	private final HealerCodeManager codeManager;
	private final JComboBox<ComboOption> importWaveCombo = new JComboBox<>();
	private final JComboBox<ComboOption> userWaveCodeCombo = new JComboBox<>();
	private final JTextField importName = new JTextField();
	private final JTextArea importCode = new JTextArea();
	private final JPanel contentPanel = new JPanel();
	private WavePresetSection presetSection;
	private JButton deleteWaveCodeAction;

	private boolean refreshingImport;

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
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(contentPanel, BorderLayout.NORTH);

		presetSection = createPresetSection();
		contentPanel.add(presetSection);
		contentPanel.add(Box.createVerticalStrut(CREATE_CODE_GAP));
		contentPanel.add(createImportCodeSection());

		refreshAll();
	}

	public void refreshAll()
	{
		presetSection.refreshAll();
		refreshUserWaveCodeCombo();
	}

	public void refreshLater()
	{
		SwingUtilities.invokeLater(this::refreshAll);
	}

	private WavePresetSection createPresetSection()
	{
		return new WavePresetSection(
				"Healer Codes",
				CONTENT_WIDTH,
				CONTROL_HEIGHT,
				WAVE_LABEL_WIDTH,
				WavePresetSection.managerAdapter(
						codeManager,
						this::refreshAll,
						(wave, strategyId) ->
						{
							selectImportWaveCode(wave, strategyId);
							presetSection.refreshPresetCombo();
						},
						this::saveCurrentPreset,
						this::deleteSelectedPreset,
						this::importRunPresetFromClipboard,
						this::exportSelectedRunPresetToClipboard
				)
		);
	}

	private JPanel createImportCodeSection()
	{
		JPanel section = BaPanelUi.section("Create Wave Code", CONTENT_WIDTH, CONTROL_HEIGHT);

		for (int wave = 1; wave <= 10; wave++)
		{
			importWaveCombo.addItem(new ComboOption(String.valueOf(wave), "Wave " + wave));
		}
		BaPanelUi.styleCombo(importWaveCombo, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		importWaveCombo.addActionListener(event ->
		{
			if (!refreshingImport)
			{
				refreshUserWaveCodeCombo();
			}
		});
		section.add(BaPanelUi.label("Wave"));
		section.add(Box.createVerticalStrut(3));
		section.add(importWaveCombo);
		section.add(Box.createVerticalStrut(6));

		BaPanelUi.styleCombo(userWaveCodeCombo, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		userWaveCodeCombo.addActionListener(event ->
		{
			if (!refreshingImport)
			{
				loadSelectedUserWaveCode();
			}
		});
		section.add(BaPanelUi.label("Existing Code"));
		section.add(Box.createVerticalStrut(3));
		section.add(userWaveCodeCombo);
		section.add(Box.createVerticalStrut(6));

		importName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter new code name...");
		BaPanelUi.fixedSize(importName, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		section.add(BaPanelUi.label("Name"));
		section.add(Box.createVerticalStrut(3));
		section.add(importName);
		section.add(Box.createVerticalStrut(6));

		BaPanelUi.styleTextArea(importCode, 7);
		section.add(BaPanelUi.label("Code"));
		section.add(Box.createVerticalStrut(3));
		section.add(BaPanelUi.wrapTextArea(importCode, CONTENT_WIDTH - 16, 120));
		section.add(Box.createVerticalStrut(6));
		section.add(BaPanelUi.action("Save Wave Code", this::saveImportedWaveCode, CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		section.add(Box.createVerticalStrut(5));

		JPanel saveDeleteRow = BaPanelUi.horizontalActionRow(CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		deleteWaveCodeAction = BaPanelUi.action("Delete", this::deleteOrResetSelectedWaveCode, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		saveDeleteRow.add(deleteWaveCodeAction);
		saveDeleteRow.add(BaPanelUi.action("Clear", this::clearImportForm, CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		section.add(saveDeleteRow);
		return section;
	}

	private void refreshUserWaveCodeCombo()
	{
		refreshUserWaveCodeCombo(getSelectedUserWaveCodeId());
	}

	private void selectImportWaveCode(int wave, String waveCodeId)
	{
		refreshingImport = true;
		BaPanelUi.selectComboValue(importWaveCombo, String.valueOf(wave));
		refreshingImport = false;
		refreshUserWaveCodeCombo(waveCodeId);
	}

	private void refreshUserWaveCodeCombo(String selectedWaveCodeId)
	{
		refreshingImport = true;
		List<ComboOption> items = new ArrayList<>();
		items.add(new ComboOption(null, "-- New --"));

		for (WaveCode code : codeManager.getWaveCodesForWave(getImportWave()))
		{
			items.add(new ComboOption(code.getId(), code.getName()));
		}

		BaPanelUi.setComboItems(userWaveCodeCombo, items, selectedWaveCodeId);
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
		RunPreset currentPreset = codeManager.findRunPreset(codeManager.getActiveRunPresetId());
		String name = promptName("Preset name", currentPreset == null ? "" : currentPreset.getName());

		if (name == null) return;

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

	private void deleteSelectedPreset(String presetId)
	{
		if (presetId == null) return;

		int result = JOptionPane.showConfirmDialog(this, "Delete this run preset?", "Delete Preset", JOptionPane.OK_CANCEL_OPTION);

		if (result != JOptionPane.OK_OPTION) return;

		codeManager.deleteUserPreset(presetId);
		refreshAll();
	}

	private void exportSelectedRunPresetToClipboard(String presetId)
	{
		if (presetId == null)
		{
			JOptionPane.showMessageDialog(this, "Select a run preset to export.", "Export Preset", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String json = codeManager.exportRunPresetJson(presetId);

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

		if (result != JOptionPane.OK_OPTION) return;

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

		if (result != JOptionPane.OK_OPTION) return;

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

		if (name.isEmpty() || code.isEmpty()) return;

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
		BaPanelUi.selectComboValue(userWaveCodeCombo, waveCode == null ? selectedId : waveCode.getId());
		loadSelectedUserWaveCode();
	}

	private void deleteOrResetSelectedWaveCode()
	{
		String selectedId = getSelectedUserWaveCodeId();

		if (selectedId == null) return;

		WaveCode selectedCode = codeManager.findWaveCode(selectedId);

		if (selectedCode == null) return;

		if (selectedCode.isBuiltIn())
		{
			resetSelectedBuiltInWaveCode(selectedCode);
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this custom wave code?", "Delete Wave Code", JOptionPane.OK_CANCEL_OPTION);

		if (result != JOptionPane.OK_OPTION) return;

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

		if (result != JOptionPane.OK_OPTION) return;

		codeManager.resetBuiltInWaveCode(selectedCode.getId());
		refreshAll();
		BaPanelUi.selectComboValue(userWaveCodeCombo, selectedCode.getId());
		loadSelectedUserWaveCode();
	}

	private void setImportFieldsEditable(boolean nameEditable, boolean codeEditable)
	{
		importName.setEditable(nameEditable);
		importCode.setEditable(codeEditable);
	}

	private void updateDeleteWaveCodeAction(WaveCode code)
	{
		if (deleteWaveCodeAction == null) return;

		deleteWaveCodeAction.setText(code != null && code.isBuiltIn() ? "Reset" : "Delete");
	}

	private String promptName(String title, String defaultValue)
	{
		JTextField field = new JTextField(defaultValue);
		BaPanelUi.fixedSize(field, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		int result = JOptionPane.showConfirmDialog(this, field, title, JOptionPane.OK_CANCEL_OPTION);

		if (result != JOptionPane.OK_OPTION || field.getText().trim().isEmpty()) return null;

		return field.getText().trim();
	}

	private int getImportWave()
	{
		String waveId = BaPanelUi.selectedId(importWaveCombo);
		return waveId == null ? 1 : Integer.parseInt(waveId);
	}

	private String getSelectedUserWaveCodeId()
	{
		return BaPanelUi.selectedId(userWaveCodeCombo);
	}
}
