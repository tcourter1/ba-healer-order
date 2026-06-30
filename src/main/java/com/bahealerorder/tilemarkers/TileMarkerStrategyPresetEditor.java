package com.bahealerorder.tilemarkers;

import com.bahealerorder.common.BaIcons;
import com.bahealerorder.common.BaPanelUi;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.SwingUtil;

class TileMarkerStrategyPresetEditor extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int EDITOR_WIDTH = 460;
	private static final int EDITOR_HEIGHT = 740;
	private static final int MARKER_SET_LIST_HEIGHT = 260;
	private static final int NOTES_HEIGHT = 150;
	private static final int TRASH_BUTTON_WIDTH = 24;
	private static final int NO_TARGET_WAVE = 0;
	private static final String NOTES_TOOLTIP = "<html>Notes appear in the overlay panel, if enabled.<br>"
			+ "To use dynamic note highlighting, start a line with a wave time.<br>"
			+ "Example: 12.0 - Delay healer</html>";

	private final GeneralTileMarkerStrategyManager strategyManager;
	private final Runnable strategiesChanged;
	private final TileMarkerSetChecklistPanel markerSetChecklist;
	private final JComboBox<StrategyOption> strategyCombo = new JComboBox<>();
	private final JTextField strategyName = new JTextField();
	private final JTextArea notes = new JTextArea();
	private final JButton deleteButton = new JButton(BaIcons.trashIcon());

	private TileMarkerWaveMap waveMap = TileMarkerWaveMap.WAVES_1_TO_9;
	private TileMarkerRoleContext targetRoleContext;
	private int targetWave = NO_TARGET_WAVE;
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
			int targetWave)
	{
		this.strategyManager = strategyManager;
		this.strategiesChanged = strategiesChanged;
		this.targetRoleContext = targetRoleContext;
		this.targetWave = validWave(targetWave) ? targetWave : NO_TARGET_WAVE;
		TileMarkerStrategyPreset initialPreset = strategyManager.findStrategyPreset(initialStrategyId);
		if (this.targetWave != NO_TARGET_WAVE)
		{
			waveMap = TileMarkerWaveMap.fromWave(this.targetWave);
		}
		else if (initialPreset != null)
		{
			waveMap = initialPreset.getWaveMap();
		}

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
			clearDraft();
		}
		else
		{
			loadStrategy(initialPreset.getId());
		}
	}

	private JPanel createHeader()
	{
		JLabel label = label("Configure saved wave strategies.", true);
		label.setHorizontalAlignment(SwingConstants.LEFT);

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.add(label, BorderLayout.CENTER);
		return row;
	}

	private JPanel createBody()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(createStrategySelector());
		panel.add(Box.createVerticalStrut(28));
		panel.add(label("Name", true));
		panel.add(Box.createVerticalStrut(10));
		BaPanelUi.fixedSize(strategyName, EDITOR_WIDTH, CONTROL_HEIGHT);
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
		BaPanelUi.fixedSize(strategyCombo, EDITOR_WIDTH - TRASH_BUTTON_WIDTH - 6, CONTROL_HEIGHT);
		strategyCombo.addActionListener(event ->
		{
			if (refreshing)
			{
				return;
			}

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

			StrategyOption item = (StrategyOption) strategyCombo.getSelectedItem();
			loadStrategy(item == null ? null : item.id);
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

	private JButton createSaveButton()
	{
		JButton button = new JButton("Save Strategy");
		button.addActionListener(event -> saveStrategy());
		BaPanelUi.fixedSize(button, (EDITOR_WIDTH - 8) / 2, CONTROL_HEIGHT);
		return button;
	}

	private JButton createPreviewButton()
	{
		JButton button = new JButton("Preview Strategy");
		button.addActionListener(event -> openStrategyPreview());
		BaPanelUi.fixedSize(button, (EDITOR_WIDTH - 8) / 2, CONTROL_HEIGHT);
		return button;
	}

	private JPanel createActionRow()
	{
		JPanel row = new JPanel(new GridLayout(1, 2, 8, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, EDITOR_WIDTH, CONTROL_HEIGHT);
		row.add(createPreviewButton());
		row.add(createSaveButton());
		return row;
	}

	private void refreshStrategyCombo(String selectedId)
	{
		refreshing = true;
		try
		{
			strategyCombo.removeAllItems();
			strategyCombo.addItem(new StrategyOption(null, "-- New --"));
			for (TileMarkerStrategyPreset preset : strategyManager.getStrategyPresets(waveMap))
			{
				strategyCombo.addItem(new StrategyOption(preset.getId(), preset.toString()));
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
			waveMap = targetWave == NO_TARGET_WAVE ? preset.getWaveMap() : TileMarkerWaveMap.fromWave(targetWave);
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

	boolean selectStrategyForWave(String id, TileMarkerRoleContext roleContext, int wave, Component parent)
	{
		if (!confirmDiscard(parent))
		{
			return false;
		}

		targetRoleContext = roleContext;
		targetWave = validWave(wave) ? wave : NO_TARGET_WAVE;
		TileMarkerStrategyPreset preset = strategyManager.findStrategyPreset(id);
		if (preset == null)
		{
			waveMap = targetWave == NO_TARGET_WAVE ? waveMap : TileMarkerWaveMap.fromWave(targetWave);
			clearDraft();
			return true;
		}

		waveMap = targetWave == NO_TARGET_WAVE ? preset.getWaveMap() : TileMarkerWaveMap.fromWave(targetWave);
		refreshStrategyCombo(id);
		loadStrategy(id);
		return true;
	}

	private void clearDraft()
	{
		refreshing = true;
		try
		{
			selectedStrategyId = null;
			strategyName.setText("");
			notes.setText("");
			markerSetChecklist.setWaveMap(waveMap);
			markerSetChecklist.setSelectedMarkerSetIds(java.util.Collections.emptyList());
			selectStrategyComboValue(null);
			updateControls();
			dirty = false;
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
		if (targetWave != NO_TARGET_WAVE)
		{
			strategyManager.setWaveSelectionTarget(
					targetRoleContext,
					targetWave,
					TileMarkerWaveSelectionTarget.strategyPreset(selectedStrategyId)
			);
		}
		refreshStrategyCombo(selectedStrategyId);
		updateControls();
		dirty = false;
		if (strategiesChanged != null)
		{
			strategiesChanged.run();
		}
		Window window = SwingUtilities.getWindowAncestor(this);
		if (window != null)
		{
			window.dispose();
		}
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
		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		strategyManager.deleteStrategyPreset(selectedStrategyId);
		refreshStrategyCombo(null);
		clearDraft();
		if (strategiesChanged != null)
		{
			strategiesChanged.run();
		}
	}

	boolean confirmDiscard(Component parent)
	{
		if (!dirty)
		{
			return true;
		}

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
				strategyName.getText() == null ? "" : strategyName.getText().trim(),
				notes.getText(),
				waveMap,
				collectSelectedMarkerSetIds()
		);
	}

	private void selectStrategyComboValue(String id)
	{
		for (int i = 0; i < strategyCombo.getItemCount(); i++)
		{
			StrategyOption item = strategyCombo.getItemAt(i);
			if ((id == null && item.id == null) || (id != null && id.equals(item.id)))
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
		row.add(label(text, true));
		row.add(Box.createHorizontalStrut(5));
		row.add(info);
		row.add(Box.createHorizontalGlue());
		return row;
	}

	private JLabel label(String text, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.TEXT_COLOR);
		if (bold)
		{
			label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
		}
		return label;
	}

	private static boolean validWave(int wave)
	{
		return wave >= 1 && wave <= 10;
	}

	private static class StrategyOption
	{
		private final String id;
		private final String label;

		private StrategyOption(String id, String label)
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
