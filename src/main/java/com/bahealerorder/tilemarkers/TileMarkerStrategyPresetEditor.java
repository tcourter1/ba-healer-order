package com.bahealerorder.tilemarkers;

import com.bahealerorder.common.BaIcons;
import com.bahealerorder.common.BaPanelUi;
import com.bahealerorder.defender.TileMarkerStrategyPreviewPanel;
import com.bahealerorder.defender.TileMarkerWaveMap;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.SwingUtil;

class TileMarkerStrategyPresetEditor extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int EDITOR_WIDTH = 460;
	private static final int MARKER_SET_LIST_HEIGHT = 210;
	private static final int NOTES_HEIGHT = 150;
	private static final int TRASH_BUTTON_WIDTH = 24;
	private static final int NO_TARGET_WAVE = 0;
	private static final Color BUILT_IN_TEXT_COLOR = new Color(0x77E2FF);
	private static final ImageIcon CHECKBOX_ICON = checkboxIcon(false);
	private static final ImageIcon CHECKBOX_SELECTED_ICON = checkboxIcon(true);
	private static final String NOTES_TOOLTIP = "<html>Notes appear in the overlay panel, if enabled.<br>"
			+ "To use dynamic note highlighting, start a line with a wave time.<br>"
			+ "Example: 12.0 - Delay healer</html>";

	private final GeneralTileMarkerStrategyManager strategyManager;
	private final Runnable strategiesChanged;
	private final Runnable openMarkerEditor;
	private final JComboBox<StrategyOption> strategyCombo = new JComboBox<>();
	private final JTextField strategyName = new JTextField();
	private final JPanel markerSetPanel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final JTextArea notes = new JTextArea();
	private final JButton deleteButton = new JButton(BaIcons.trashIcon());
	private final ButtonGroup waveMapButtonGroup = new ButtonGroup();
	private final JCheckBox showCustomMarkerSets = filterCheckBox("Custom");
	private final JCheckBox showBuiltInMarkerSets = filterCheckBox("Built-In");
	private final Set<String> selectedMarkerSetIds = new LinkedHashSet<>();

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
		this.openMarkerEditor = openMarkerEditor;
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
		setPreferredSize(new Dimension(EDITOR_WIDTH + 20, 720));

		add(createHeader(), BorderLayout.NORTH);
		add(createBody(), BorderLayout.CENTER);

		addTextChangeListener(strategyName, this::markDirty);
		addTextChangeListener(notes, this::markDirty);
		showCustomMarkerSets.addItemListener(event -> refreshMarkerSetChecks());
		showBuiltInMarkerSets.addItemListener(event -> refreshMarkerSetChecks());
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
		row.add(createWaveMapToggle(), BorderLayout.EAST);
		return row;
	}

	private JPanel createWaveMapToggle()
	{
		JPanel row = new JPanel(new GridLayout(1, 2, 4, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, 184, CONTROL_HEIGHT);
		for (TileMarkerWaveMap map : TileMarkerWaveMap.values())
		{
			JToggleButton button = new JToggleButton(map.getDisplayName());
			button.setFocusable(false);
			button.setToolTipText(map.getDisplayName());
			button.setEnabled(targetWave == NO_TARGET_WAVE || map == TileMarkerWaveMap.fromWave(targetWave));
			button.addActionListener(event -> setWaveMap(map));
			waveMapButtonGroup.add(button);
			row.add(button);
			if (map == waveMap)
			{
				button.setSelected(true);
			}
		}
		return row;
	}

	private JPanel createBody()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(createStrategySelector());
		panel.add(Box.createVerticalStrut(10));
		panel.add(label("Name", true));
		panel.add(Box.createVerticalStrut(3));
		BaPanelUi.fixedSize(strategyName, EDITOR_WIDTH, CONTROL_HEIGHT);
		panel.add(strategyName);
		panel.add(Box.createVerticalStrut(12));
		panel.add(createMarkerSetHeader());
		panel.add(Box.createVerticalStrut(3));
		panel.add(createMarkerSetList());
		panel.add(Box.createVerticalStrut(12));
		panel.add(labelWithInfo("Notes"));
		panel.add(Box.createVerticalStrut(3));
		panel.add(createNotesArea());
		panel.add(Box.createVerticalStrut(12));
		panel.add(createPreviewButton());
		panel.add(Box.createVerticalStrut(6));
		panel.add(createSaveButton());
		return panel;
	}

	private JPanel createMarkerSetHeader()
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, EDITOR_WIDTH, CONTROL_HEIGHT);

		JPanel labelRow = new JPanel();
		labelRow.setLayout(new BoxLayout(labelRow, BoxLayout.X_AXIS));
		labelRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		labelRow.add(label("Tile Marker Sets", true));
		labelRow.add(Box.createHorizontalStrut(5));
		labelRow.add(iconButton(BaIcons.popoutIcon(), "Configure Marker Sets", openMarkerEditor));
		row.add(labelRow, BorderLayout.WEST);

		JPanel filters = new JPanel(new GridLayout(1, 2, 4, 0));
		filters.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(filters, 156, CONTROL_HEIGHT);
		filters.add(showCustomMarkerSets);
		filters.add(showBuiltInMarkerSets);
		row.add(filters, BorderLayout.EAST);
		return row;
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

	private JScrollPane createMarkerSetList()
	{
		refreshMarkerSetChecks();

		JScrollPane scrollPane = new JScrollPane(markerSetPanel);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
		scrollPane.getVerticalScrollBar().setUnitIncrement(CONTROL_HEIGHT);
		BaPanelUi.fixedSize(scrollPane, EDITOR_WIDTH, MARKER_SET_LIST_HEIGHT);
		return scrollPane;
	}

	private JScrollPane createNotesArea()
	{
		notes.setRows(4);
		notes.setLineWrap(true);
		notes.setWrapStyleWord(true);

		JScrollPane scrollPane = new JScrollPane(notes);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
		BaPanelUi.fixedSize(scrollPane, EDITOR_WIDTH, NOTES_HEIGHT);
		return scrollPane;
	}

	private JButton createSaveButton()
	{
		JButton button = new JButton("Save Strategy");
		button.addActionListener(event -> saveStrategy());
		BaPanelUi.fixedSize(button, EDITOR_WIDTH, CONTROL_HEIGHT);
		return button;
	}

	private JButton createPreviewButton()
	{
		JButton button = new JButton("Preview Strategy");
		button.addActionListener(event -> openStrategyPreview());
		BaPanelUi.fixedSize(button, EDITOR_WIDTH, CONTROL_HEIGHT);
		return button;
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
			waveMap = preset.getWaveMap();
			selectWaveMapButton();
			strategyName.setText(preset.getName() == null ? "" : preset.getName());
			notes.setText(preset.getNotes() == null ? "" : preset.getNotes());
			setSelectedMarkerSetIds(preset.getMarkerSetIds());
			refreshMarkerSetChecks();
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
			selectWaveMapButton();
			clearDraft();
			return true;
		}

		waveMap = targetWave == NO_TARGET_WAVE ? preset.getWaveMap() : TileMarkerWaveMap.fromWave(targetWave);
		selectWaveMapButton();
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
			setSelectedMarkerSetIds(java.util.Collections.emptyList());
			refreshMarkerSetChecks();
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
		refreshMarkerSetChecks();
	}

	private void setSelectedMarkerSetIds(List<String> selectedIds)
	{
		selectedMarkerSetIds.clear();
		if (selectedIds != null)
		{
			selectedMarkerSetIds.addAll(selectedIds);
		}
	}

	private void refreshMarkerSetChecks()
	{
		markerSetPanel.removeAll();
		for (TileMarkerSet set : strategyManager.getMarkerSets(waveMap))
		{
			if (set.isBuiltIn() && !showBuiltInMarkerSets.isSelected())
			{
				continue;
			}

			if (!set.isBuiltIn() && !showCustomMarkerSets.isSelected())
			{
				continue;
			}

			JCheckBox checkBox = new JCheckBox(set.toString(), selectedMarkerSetIds.contains(set.getId()));
			checkBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			checkBox.setForeground(set.isBuiltIn() ? BUILT_IN_TEXT_COLOR : ColorScheme.TEXT_COLOR);
			checkBox.setFocusable(false);
			checkBox.setIcon(CHECKBOX_ICON);
			checkBox.setSelectedIcon(CHECKBOX_SELECTED_ICON);
			checkBox.putClientProperty("markerSetId", set.getId());
			checkBox.addItemListener(event ->
			{
				if (checkBox.isSelected())
				{
					selectedMarkerSetIds.add(set.getId());
				}
				else
				{
					selectedMarkerSetIds.remove(set.getId());
				}
				markDirty();
			});
			markerSetPanel.add(checkBox);
		}
		if (markerSetPanel.getComponentCount() == 0)
		{
			JLabel empty = label("No marker sets match the current filters.", false);
			empty.setBorder(new EmptyBorder(6, 6, 0, 0));
			markerSetPanel.add(empty);
		}
		markerSetPanel.revalidate();
		markerSetPanel.repaint();
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
			strategyManager.setWaveStrategyPresetId(targetRoleContext, targetWave, selectedStrategyId);
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
		return new ArrayList<>(selectedMarkerSetIds);
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

	private void setWaveMap(TileMarkerWaveMap map)
	{
		if (refreshing || map == null || map == waveMap)
		{
			return;
		}

		if (!confirmDiscard(this))
		{
			selectWaveMapButton();
			return;
		}

		waveMap = map;
		refreshStrategyCombo(null);
		clearDraft();
	}

	private void selectWaveMapButton()
	{
		for (java.util.Enumeration<javax.swing.AbstractButton> buttons = waveMapButtonGroup.getElements(); buttons.hasMoreElements(); )
		{
			javax.swing.AbstractButton button = buttons.nextElement();
			button.setEnabled(targetWave == NO_TARGET_WAVE || waveMap.getDisplayName().equals(button.getText()));
			if (waveMap.getDisplayName().equals(button.getText()))
			{
				button.setSelected(true);
			}
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

	private JButton iconButton(ImageIcon icon, String tooltip, Runnable action)
	{
		JButton button = new JButton(icon);
		button.setToolTipText(tooltip);
		button.addActionListener(event ->
		{
			if (action != null)
			{
				action.run();
			}
		});
		SwingUtil.removeButtonDecorations(button);
		BaPanelUi.fixedSize(button, CONTROL_HEIGHT, CONTROL_HEIGHT);
		return button;
	}

	private JLabel label(String text, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.TEXT_COLOR);
		return label;
	}

	private static JCheckBox filterCheckBox(String text)
	{
		JCheckBox checkBox = new JCheckBox(text, true);
		checkBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		checkBox.setForeground(ColorScheme.TEXT_COLOR);
		checkBox.setFocusable(false);
		checkBox.setIcon(CHECKBOX_ICON);
		checkBox.setSelectedIcon(CHECKBOX_SELECTED_ICON);
		return checkBox;
	}

	private static ImageIcon checkboxIcon(boolean selected)
	{
		int size = 14;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(Color.WHITE);
		graphics.drawRoundRect(2, 2, 10, 10, 2, 2);
		if (selected)
		{
			graphics.setColor(ColorScheme.PROGRESS_COMPLETE_COLOR);
			graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.drawLine(4, 7, 6, 10);
			graphics.drawLine(6, 10, 11, 4);
		}
		graphics.dispose();
		return new ImageIcon(image);
	}

	private static boolean validWave(int wave)
	{
		return wave >= 1 && wave <= 10;
	}

	private void addTextChangeListener(JTextComponent component, Runnable runnable)
	{
		component.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				runnable.run();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				runnable.run();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				runnable.run();
			}
		});
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
