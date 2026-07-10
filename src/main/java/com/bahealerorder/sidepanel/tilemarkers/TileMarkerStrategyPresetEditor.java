package com.bahealerorder.sidepanel.tilemarkers;

import com.bahealerorder.common.BaClipboard;
import com.bahealerorder.common.BaIcons;
import com.bahealerorder.sidepanel.BaPanelUi;
import com.bahealerorder.sidepanel.BaTransferDialog;
import com.bahealerorder.tilemarkers.GeneralTileMarkerStrategyManager;
import com.bahealerorder.tilemarkers.TileMarkerExportResult;
import com.bahealerorder.tilemarkers.TileMarkerExportType;
import com.bahealerorder.tilemarkers.TileMarkerRoleContext;
import com.bahealerorder.tilemarkers.TileMarkerStrategyPreset;
import com.bahealerorder.tilemarkers.TileMarkerWaveMap;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.Collections;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.SwingUtil;

class TileMarkerStrategyPresetEditor extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int EDITOR_WIDTH = 460;
	private static final int EDITOR_HEIGHT = 780;
	private static final int MARKER_SET_LIST_HEIGHT = 260;
	private static final int NOTES_HEIGHT = 150;
	private static final int TRASH_BUTTON_WIDTH = 24;
	private static final int HEADER_ROLE_WIDTH = 126;
	private static final int HEADER_WAVE_WIDTH = 76;
	private static final int STRATEGY_POPUP_WIDTH = 188;
	private static final String NOTES_TOOLTIP = "<html>Notes appear in the overlay panel, if enabled.<br>"
			+ "To use dynamic note highlighting, start a line with a wave time.<br>"
			+ "Example: 12.0 - Delay healer</html>";

	private final GeneralTileMarkerStrategyManager strategyManager;
	private final Runnable strategiesChanged;
	private final TileMarkerSetChecklistPanel markerSetChecklist;
	private final JComboBox<BaPanelUi.ComboOption> strategyCombo = BaPanelUi.fixedPopupWidthCombo(STRATEGY_POPUP_WIDTH);
	private final JTextField strategyName = new JTextField();
	private final JTextArea notes = new JTextArea();
	private final JButton deleteButton = new JButton(BaIcons.trashIcon());
	private final JComboBox<TileMarkerRoleContext> targetRoleCombo = new JComboBox<>();
	private final JComboBox<Integer> targetWaveCombo = new JComboBox<>();

	private TileMarkerWaveMap waveMap = TileMarkerWaveMap.WAVES_1_TO_9;
	private TileMarkerRoleContext targetRoleContext;
	private int targetWave;
	private String selectedStrategyId;
	private JDialog previewDialog;
	private boolean refreshing;
	private boolean dirty;

	TileMarkerStrategyPresetEditor(
			GeneralTileMarkerStrategyManager strategyManager,
			Runnable strategiesChanged,
			Runnable openMarkerEditor,
			String initialStrategyId,
			TileMarkerRoleContext targetRoleContext,
			int targetWave,
			String initialMarkerSetId)
	{
		this.strategyManager = strategyManager;
		this.strategiesChanged = strategiesChanged;
		this.targetRoleContext = targetRoleContext == null ? TileMarkerRoleContext.DEFENDER : targetRoleContext;
		this.targetWave = requireWave(targetWave);
		waveMap = TileMarkerWaveMap.fromWave(this.targetWave);
		TileMarkerStrategyPreset initialPreset = strategyManager.findStrategyPreset(initialStrategyId);

		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 10));
		setPreferredSize(new Dimension(EDITOR_WIDTH + 20, EDITOR_HEIGHT));
		markerSetChecklist = new TileMarkerSetChecklistPanel(
				strategyManager,
				openMarkerEditor,
				this::markDirty,
				EDITOR_WIDTH,
				CONTROL_HEIGHT,
				MARKER_SET_LIST_HEIGHT
		);
		markerSetChecklist.setWaveMap(waveMap);

		add(createHeader(), BorderLayout.NORTH);
		add(createBody(), BorderLayout.CENTER);

		BaPanelUi.addTextChangeListener(strategyName, this::markDirty);
		BaPanelUi.addTextChangeListener(notes, this::markDirty);
		refreshStrategyCombo(initialPreset == null ? null : initialPreset.getId());
		if (initialPreset == null)
		{
			clearDraft(initialMarkerSetId);
		}
		else
		{
			loadStrategy(initialPreset.getId());
		}
	}

	private JPanel createHeader()
	{
		for (TileMarkerRoleContext context : TileMarkerRoleContext.values())
		{
			targetRoleCombo.addItem(context);
		}
		for (int wave = 1; wave <= 10; wave++)
		{
			targetWaveCombo.addItem(wave);
		}
		BaPanelUi.styleCombo(targetRoleCombo, HEADER_ROLE_WIDTH, CONTROL_HEIGHT);
		BaPanelUi.styleCombo(targetWaveCombo, HEADER_WAVE_WIDTH, CONTROL_HEIGHT);
		selectTargetComboValues();

			targetRoleCombo.addActionListener(event ->
		{
			if (refreshing) return;

			targetRoleContext = (TileMarkerRoleContext) targetRoleCombo.getSelectedItem();
		});
		targetWaveCombo.addActionListener(event -> changeTargetWave());

		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, EDITOR_WIDTH, CONTROL_HEIGHT);
		row.add(BaPanelUi.plainLabel("Configuring", true));
		row.add(Box.createHorizontalStrut(6));
		row.add(targetRoleCombo);
		row.add(Box.createHorizontalStrut(6));
		row.add(BaPanelUi.plainLabel("strategy for wave", true));
		row.add(Box.createHorizontalStrut(6));
		row.add(targetWaveCombo);
		row.add(Box.createHorizontalGlue());
		return row;
	}

	private void selectTargetComboValues()
	{
		refreshing = true;
		try
		{
			targetRoleCombo.setSelectedItem(targetRoleContext);
			targetWaveCombo.setSelectedItem(targetWave);
		}
		finally
		{
			refreshing = false;
		}
	}

	private void changeTargetWave()
	{
		if (refreshing) return;

		Integer selectedWave = (Integer) targetWaveCombo.getSelectedItem();
		if (selectedWave == null || selectedWave == targetWave) return;

		TileMarkerWaveMap nextWaveMap = TileMarkerWaveMap.fromWave(selectedWave);
		if (nextWaveMap != waveMap && !confirmDiscard(this))
		{
			selectTargetComboValues();
			return;
		}

		targetWave = selectedWave;
		if (nextWaveMap != waveMap)
		{
			waveMap = nextWaveMap;
			refreshStrategyCombo(null);
			clearDraft();
		}
	}

	private JPanel createBody()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(createStrategySelector());
		panel.add(Box.createVerticalStrut(28));
		panel.add(BaPanelUi.plainLabel("Name", true));
		panel.add(Box.createVerticalStrut(10));
		BaPanelUi.styleTextInput(strategyName, EDITOR_WIDTH, CONTROL_HEIGHT);
		panel.add(strategyName);
		panel.add(Box.createVerticalStrut(30));
		panel.add(markerSetChecklist);
		panel.add(Box.createVerticalStrut(30));
		panel.add(labelWithInfo("Notes"));
		panel.add(Box.createVerticalStrut(10));
		panel.add(createNotesArea());
		panel.add(Box.createVerticalStrut(28));
		panel.add(createActionRow());
		return panel;
	}

	private JPanel createStrategySelector()
	{
		BaPanelUi.styleCombo(strategyCombo, EDITOR_WIDTH - TRASH_BUTTON_WIDTH - 6, CONTROL_HEIGHT);
		strategyCombo.setRenderer(BaPanelUi.comboOptionRenderer(CONTROL_HEIGHT));
		strategyCombo.addActionListener(event ->
		{
			if (refreshing) return;

			if (!confirmDiscard(this))
			{
				refreshing = true;
				try
				{
					selectStrategyComboValue(selectedStrategyId);
				}
				finally
				{
					refreshing = false;
				}
				return;
			}

			BaPanelUi.ComboOption item = (BaPanelUi.ComboOption) strategyCombo.getSelectedItem();
			loadStrategy(item == null ? null : item.getId());
		});

		deleteButton.setToolTipText("Delete selected strategy");
		SwingUtil.removeButtonDecorations(deleteButton);
		BaPanelUi.fixedSize(deleteButton, TRASH_BUTTON_WIDTH, CONTROL_HEIGHT);
		deleteButton.addActionListener(event -> deleteStrategy());

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, EDITOR_WIDTH, CONTROL_HEIGHT);
		row.add(strategyCombo, BorderLayout.CENTER);
		row.add(deleteButton, BorderLayout.EAST);
		return row;
	}

	private JScrollPane createNotesArea()
	{
		notes.setRows(4);
		notes.setLineWrap(true);
		notes.setWrapStyleWord(true);

		JScrollPane scrollPane = new JScrollPane(notes);
		scrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
		BaPanelUi.fixedSize(scrollPane, EDITOR_WIDTH, NOTES_HEIGHT);
		return scrollPane;
	}

	private JPanel createActionRow()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(panel, EDITOR_WIDTH, CONTROL_HEIGHT * 2 + 8);

		int actionButtonWidth = (EDITOR_WIDTH - 16) / 3;
		JPanel row = new JPanel(new GridLayout(1, 3, 8, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, EDITOR_WIDTH, CONTROL_HEIGHT);
		row.add(BaPanelUi.action("Import", this::importStrategyFromClipboard, actionButtonWidth, CONTROL_HEIGHT));
		row.add(BaPanelUi.action("Export", this::exportStrategyToClipboard, actionButtonWidth, CONTROL_HEIGHT));
		row.add(BaPanelUi.action("Preview", this::openStrategyPreview, actionButtonWidth, CONTROL_HEIGHT));

		panel.add(row);
		panel.add(Box.createVerticalStrut(8));
		panel.add(BaPanelUi.action("Save Strategy", this::saveStrategy, EDITOR_WIDTH, CONTROL_HEIGHT));
		return panel;
	}

	private void refreshStrategyCombo(String selectedId)
	{
		refreshing = true;
		try
		{
			strategyCombo.removeAllItems();
			strategyCombo.addItem(new BaPanelUi.ComboOption(null, "-- New --"));
			for (TileMarkerStrategyPreset preset : strategyManager.getStrategyPresets(waveMap))
			{
				strategyCombo.addItem(new BaPanelUi.ComboOption(preset.getId(), preset.toString(), preset.isBuiltIn()));
			}
			selectStrategyComboValue(selectedId);
		}
		finally
		{
			refreshing = false;
		}
	}

	private void loadStrategy(String id)
	{
		TileMarkerStrategyPreset preset = strategyManager.findStrategyPreset(id);
		if (preset == null)
		{
			clearDraft();
			return;
		}

		refreshing = true;
		try
		{
			selectedStrategyId = preset.getId();
			waveMap = TileMarkerWaveMap.fromWave(targetWave);
			strategyName.setText(preset.getName() == null ? "" : preset.getName());
			notes.setText(preset.getNotes() == null ? "" : preset.getNotes());
			markerSetChecklist.setWaveMap(waveMap);
			markerSetChecklist.setSelectedMarkerSetIds(preset.getMarkerSetIds());
			updateControls();
			dirty = false;
		}
		finally
		{
			refreshing = false;
		}
	}

	boolean selectStrategyForWave(
			String id,
			TileMarkerRoleContext roleContext,
			int wave,
			String initialMarkerSetId,
			Component parent)
	{
		if (!confirmDiscard(parent)) return false;

		targetRoleContext = roleContext == null ? TileMarkerRoleContext.DEFENDER : roleContext;
		targetWave = requireWave(wave);
		waveMap = TileMarkerWaveMap.fromWave(targetWave);
		selectTargetComboValues();
		TileMarkerStrategyPreset preset = strategyManager.findStrategyPreset(id);
		if (preset == null)
		{
			refreshStrategyCombo(null);
			clearDraft(initialMarkerSetId);
			return true;
		}

		refreshStrategyCombo(id);
		loadStrategy(id);
		return true;
	}

	private void clearDraft()
	{
		clearDraft(null);
	}

	private void clearDraft(String initialMarkerSetId)
	{
		refreshing = true;
		try
		{
			selectedStrategyId = null;
			strategyName.setText("");
			notes.setText("");
			markerSetChecklist.setWaveMap(waveMap);
			markerSetChecklist.setSelectedMarkerSetIds(isBlank(initialMarkerSetId)
					? Collections.emptyList()
					: Collections.singletonList(initialMarkerSetId));
			selectStrategyComboValue(null);
			updateControls();
			dirty = !isBlank(initialMarkerSetId);
		}
		finally
		{
			refreshing = false;
		}
	}

	void refreshMarkerSets()
	{
		refreshStrategyCombo(selectedStrategyId);
		markerSetChecklist.refresh();
	}

	private void saveStrategy()
	{
		TileMarkerStrategyPreset saved = strategyManager.saveStrategyPreset(
				selectedStrategyId,
				strategyName.getText(),
				notes.getText(),
				waveMap,
				collectSelectedMarkerSetIds()
		);
		if (saved == null)
		{
			Toolkit.getDefaultToolkit().beep();
			strategyName.requestFocusInWindow();
			return;
		}

		selectedStrategyId = saved.getId();
		strategyManager.setWaveSelectionStrategyId(targetRoleContext, targetWave, selectedStrategyId);
		refreshStrategyCombo(selectedStrategyId);
		updateControls();
		dirty = false;
		strategiesChanged.run();
		SwingUtilities.getWindowAncestor(this).dispose();
	}

	private void deleteStrategy()
	{
		if (selectedStrategyId == null)
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		TileMarkerStrategyPreset preset = strategyManager.findStrategyPreset(selectedStrategyId);
		if (preset == null || preset.isBuiltIn())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this wave strategy?", "Delete Strategy", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION) return;

		strategyManager.deleteStrategyPreset(selectedStrategyId);
		refreshStrategyCombo(null);
		clearDraft();
		strategiesChanged.run();
	}

	private void exportStrategyToClipboard()
	{
		TileMarkerExportResult result = strategyManager.exportStrategyPresetJson(draftStrategy());
		if (result == null)
		{
			JOptionPane.showMessageDialog(
					this,
					"Current wave strategy needs a name before it can be exported.",
					"Export Strategy",
					JOptionPane.ERROR_MESSAGE
			);
			return;
		}

		BaClipboard.copyText(result.getJson());
		BaTransferDialog.show(this, "Export Strategy", "Exported " + result.getTypedName() + ".", "Export", result.getSummaryLines());
	}

	private void importStrategyFromClipboard()
	{
		if (!confirmDiscard(this)) return;

		String json = BaClipboard.readText(this, "Import Strategy");
		if (json == null) return;

		TileMarkerExportResult result;
		try
		{
			result = strategyManager.importMarkerExportJson(json, waveMap);
		}
		catch (RuntimeException ex)
		{
			result = null;
		}

		if (result == null)
		{
			JOptionPane.showMessageDialog(
					this,
					"Clipboard text could not be imported as a " + waveMap.getDisplayName() + " tile marker export.",
					"Import Strategy",
					JOptionPane.ERROR_MESSAGE
			);
			return;
		}

		refreshStrategyCombo(result.getId());
		markerSetChecklist.refresh();
		if ((result.getType() == TileMarkerExportType.STRATEGY_PRESET
				|| result.getType() == TileMarkerExportType.STRATEGY_COLLECTION)
				&& result.getId() != null)
		{
			loadStrategy(result.getId());
		}
		strategiesChanged.run();
		BaTransferDialog.show(this, "Import Strategy", "Imported " + result.getTypedName() + ".", "Import", result.getSummaryLines());
	}

	boolean confirmDiscard(Component parent)
	{
		if (!dirty) return true;

		int result = JOptionPane.showConfirmDialog(
				parent,
				"Discard unsaved wave strategy changes?",
				"Unsaved Changes",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE
		);
		return result == JOptionPane.YES_OPTION;
	}

	private List<String> collectSelectedMarkerSetIds()
	{
		return markerSetChecklist.getSelectedMarkerSetIds();
	}

	private void openStrategyPreview()
	{
		if (previewDialog != null && previewDialog.isDisplayable())
		{
			previewDialog.dispose();
		}

		TileMarkerStrategyPreviewPanel previewPanel = new TileMarkerStrategyPreviewPanel(strategyManager, draftStrategy());
		Window owner = SwingUtilities.getWindowAncestor(this);
		previewDialog = new JDialog(owner, "Tile Marker Preview", java.awt.Dialog.ModalityType.MODELESS);
		previewDialog.setContentPane(previewPanel);
		previewDialog.pack();
		previewDialog.setMinimumSize(new Dimension(980, 720));
		previewDialog.setLocationRelativeTo(owner);
		previewDialog.setVisible(true);
	}

	private TileMarkerStrategyPreset draftStrategy()
	{
		return new TileMarkerStrategyPreset(
				selectedStrategyId,
				strategyName.getText().trim(),
				notes.getText(),
				waveMap,
				collectSelectedMarkerSetIds()
		);
	}

	private void selectStrategyComboValue(String id)
	{
		for (int i = 0; i < strategyCombo.getItemCount(); i++)
		{
			BaPanelUi.ComboOption item = strategyCombo.getItemAt(i);
			if ((id == null && item.getId() == null) || (id != null && id.equals(item.getId())))
			{
				strategyCombo.setSelectedIndex(i);
				return;
			}
		}

		if (strategyCombo.getItemCount() > 0)
		{
			strategyCombo.setSelectedIndex(0);
		}
	}

	private void updateControls()
	{
		TileMarkerStrategyPreset preset = strategyManager.findStrategyPreset(selectedStrategyId);
		deleteButton.setEnabled(preset != null && !preset.isBuiltIn());
	}

	private void markDirty()
	{
		if (!refreshing)
		{
			dirty = true;
		}
	}

	private JPanel labelWithInfo(String text)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, EDITOR_WIDTH, CONTROL_HEIGHT);

		JLabel info = new JLabel(BaIcons.infoIcon());
		info.setToolTipText(NOTES_TOOLTIP);
		row.add(BaPanelUi.plainLabel(text, true));
		row.add(Box.createHorizontalStrut(5));
		row.add(info);
		row.add(Box.createHorizontalGlue());
		return row;
	}

	private static int requireWave(int wave)
	{
		if (wave < 1 || wave > 10)
		{
			throw new IllegalArgumentException("Wave strategy editor requires wave 1-10.");
		}
		return wave;
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.isBlank();
	}

}
