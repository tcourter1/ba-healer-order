package com.bahealerorder.tilemarkers;

import com.bahealerorder.common.BaIcons;
import com.bahealerorder.common.BaPanelUi;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.defender.TileMarkerStrategyPreviewPanel;
import com.bahealerorder.defender.TileMarkerWaveMap;
import com.bahealerorder.defender.TileMarkerSetEditor;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
	private static final int CONTROL_HEIGHT = 24;
	private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
	private static final int ROLE_ICON_SIZE = 18;
	private static final int ROLE_TAB_GAP = 6;
	private static final TileMarkerRoleContext[] ROLE_CONTEXT_ORDER = {
			TileMarkerRoleContext.DEFENDER,
			TileMarkerRoleContext.COLLECTOR,
			TileMarkerRoleContext.HEALER,
			TileMarkerRoleContext.ATTACKER,
			TileMarkerRoleContext.GLOBAL
	};

	private final GeneralTileMarkerStrategyManager strategyManager;
	private final ColorPickerManager colorPickerManager;
	private final ItemManager itemManager;
	private final JPanel contentPanel = new JPanel();
	private final JPanel waveRowsPanel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final JPanel beginnerPromptPanel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final MaterialTabGroup roleTabGroup = new MaterialTabGroup();
	private final Map<TileMarkerRoleContext, MaterialTab> roleTabs = new EnumMap<>(TileMarkerRoleContext.class);
	private final JLabel strategyTitle = new JLabel();
	private final JComboBox<AssignmentPresetOption> assignmentPresetCombo = new JComboBox<>();

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
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
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
		JButton button = BaPanelUi.action("Configure Markers", this::openMarkerEditorDialog, CONTENT_WIDTH - 10, CONTROL_HEIGHT);
		button.setIcon(BaIcons.popoutIcon());
		button.setHorizontalTextPosition(SwingConstants.LEADING);
		button.setIconTextGap(8);
		button.setToolTipText("Configure Markers");
		return button;
	}

	private JPanel createStrategySection()
	{
		JPanel section = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(new EmptyBorder(8, 8, 8, 8));
		section.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		section.setAlignmentX(LEFT_ALIGNMENT);

		strategyTitle.setForeground(ColorScheme.TEXT_COLOR);
		strategyTitle.setHorizontalAlignment(SwingConstants.CENTER);
		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(titleRow, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		titleRow.add(strategyTitle, BorderLayout.CENTER);
		section.add(titleRow);
		section.add(Box.createVerticalStrut(6));

		section.add(createRoleContextRow());
		section.add(Box.createVerticalStrut(8));
		section.add(createAssignmentPresetControls());
		section.add(Box.createVerticalStrut(10));
		waveRowsPanel.setAlignmentX(LEFT_ALIGNMENT);
		section.add(waveRowsPanel);
		return section;
	}

	private JPanel createRoleContextRow()
	{
		roleTabGroup.setLayout(new DynamicGridLayout(1, ROLE_CONTEXT_ORDER.length, ROLE_TAB_GAP, 0));
		roleTabGroup.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(roleTabGroup, CONTENT_WIDTH - 16, CONTROL_HEIGHT + 8);

		for (TileMarkerRoleContext context : ROLE_CONTEXT_ORDER)
		{
			MaterialTab tab = new MaterialTab(new ImageIcon(), roleTabGroup, new JPanel());
			tab.setToolTipText(context.getDisplayName());
			BaPanelUi.fixedSize(
					tab,
					(CONTENT_WIDTH - 16 - ROLE_TAB_GAP * (ROLE_CONTEXT_ORDER.length - 1)) / ROLE_CONTEXT_ORDER.length,
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
			waveRowsPanel.add(Box.createVerticalStrut(6));
		}
		waveRowsPanel.revalidate();
		waveRowsPanel.repaint();
	}

	private JPanel createWaveRow(int wave)
	{
		JLabel waveLabel = new JLabel("Wave " + wave);
		waveLabel.setForeground(ColorScheme.TEXT_COLOR);

		JPanel headerRow = new JPanel(new BorderLayout(6, 0));
		headerRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(headerRow, CONTENT_WIDTH - 16, CONTROL_HEIGHT);

		JComboBox<StrategyOption> comboBox = new JComboBox<>();
		BaPanelUi.fixedSize(comboBox, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		populateStrategyCombo(comboBox, wave, strategyManager.getWaveStrategyPresetId(selectedContext, wave));
		comboBox.addActionListener(event ->
		{
			if (refreshing)
			{
				return;
			}

			StrategyOption item = (StrategyOption) comboBox.getSelectedItem();
			strategyManager.setWaveStrategyPresetId(selectedContext, wave, item == null ? null : item.id);
			refreshAssignmentPresetCombo();
		});

		headerRow.add(waveLabel, BorderLayout.CENTER);
		headerRow.add(createWaveActionRow(wave, comboBox), BorderLayout.EAST);

		JPanel row = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, CONTENT_WIDTH - 16, CONTROL_HEIGHT * 2 + 3);
		row.add(headerRow);
		row.add(Box.createVerticalStrut(3));
		row.add(comboBox);
		return row;
	}

	private JPanel createWaveActionRow(int wave, JComboBox<StrategyOption> comboBox)
	{
		JPanel row = new JPanel(new DynamicGridLayout(1, 2, 4, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, CONTROL_HEIGHT * 2 + 4, CONTROL_HEIGHT);
		row.add(iconButton(BaIcons.pencilIcon(), BaIcons.pencilHoverIcon(), "Edit", () ->
		{
			StrategyOption item = (StrategyOption) comboBox.getSelectedItem();
			openStrategyEditorDialog(wave, item == null ? null : item.id);
		}));
		row.add(iconButton(BaIcons.eyeIcon(), BaIcons.eyeHoverIcon(), "Preview", () -> openPreviewDialog(wave)));
		return row;
	}

	private JButton iconButton(ImageIcon icon, ImageIcon hoverIcon, String tooltip, Runnable action)
	{
		JButton button = new JButton(icon);
		button.setRolloverIcon(hoverIcon);
		button.setToolTipText(tooltip);
		button.addActionListener(event -> action.run());
		SwingUtil.removeButtonDecorations(button);
		BaPanelUi.fixedSize(button, CONTROL_HEIGHT, CONTROL_HEIGHT);
		return button;
	}

	private JPanel createAssignmentPresetControls()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(createBeginnerPromptPanel());
		BaPanelUi.fixedSize(assignmentPresetCombo, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		assignmentPresetCombo.addActionListener(event ->
		{
			if (refreshing)
			{
				return;
			}

			AssignmentPresetOption item = (AssignmentPresetOption) assignmentPresetCombo.getSelectedItem();
			strategyManager.applyAssignmentPreset(selectedContext, item == null ? null : item.id);
			refreshAll();
		});
		panel.add(assignmentPresetCombo);
		panel.add(Box.createVerticalStrut(6));
		panel.add(BaPanelUi.action("Save Current Selections", this::saveCurrentAssignmentPreset, CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		panel.add(Box.createVerticalStrut(5));

		JPanel actionRow = BaPanelUi.horizontalActionRow(CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		actionRow.add(BaPanelUi.action("Delete", this::deleteSelectedAssignmentPreset, CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		actionRow.add(BaPanelUi.action("Clear", this::clearAssignmentSelections, CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		panel.add(actionRow);
		return panel;
	}

	private JPanel createBeginnerPromptPanel()
	{
		JButton button = BaPanelUi.action(
				"<html><center>No strategies are currently selected.<br>Click here to apply beginner tiles.</center></html>",
				this::applyBeginnerAssignmentPreset,
				CONTENT_WIDTH - 16,
				42
		);
		button.setToolTipText("Apply the Beginner preset to the current role.");
		beginnerPromptPanel.add(button);
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
			assignmentPresetCombo.addItem(new AssignmentPresetOption(null, ""));
			for (TileMarkerAssignmentPreset preset : strategyManager.getAssignmentPresets(selectedContext))
			{
				assignmentPresetCombo.addItem(new AssignmentPresetOption(preset.getId(), preset.toString()));
			}
			selectAssignmentPresetComboValue(strategyManager.getActiveAssignmentPresetId(selectedContext));
			beginnerPromptPanel.setVisible(
					selectedContext == TileMarkerRoleContext.DEFENDER
							&& strategyManager.getWaveStrategyPresetIds(selectedContext).isEmpty()
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
			if (preset != null && "Beginner".equalsIgnoreCase(preset.getName()))
			{
				return preset.getId();
			}
		}
		return null;
	}

	private void selectAssignmentPresetComboValue(String id)
	{
		for (int i = 0; i < assignmentPresetCombo.getItemCount(); i++)
		{
			AssignmentPresetOption item = assignmentPresetCombo.getItemAt(i);
			if ((id == null && item.id == null) || (id != null && id.equals(item.id)))
			{
				assignmentPresetCombo.setSelectedIndex(i);
				return;
			}
		}

		if (assignmentPresetCombo.getItemCount() > 0)
		{
			assignmentPresetCombo.setSelectedIndex(0);
		}
	}

	private void saveCurrentAssignmentPreset()
	{
		String activePresetId = strategyManager.getActiveAssignmentPresetId(selectedContext);
		TileMarkerAssignmentPreset activePreset = strategyManager.findAssignmentPreset(activePresetId);
		String name = promptName("Preset name", activePreset == null ? "" : activePreset.getName());
		if (name == null)
		{
			return;
		}

		String id = activePreset != null && name.equalsIgnoreCase(activePreset.getName()) ? activePreset.getId() : null;
		strategyManager.saveAssignmentPreset(selectedContext, id, name, strategyManager.getWaveStrategyPresetIds(selectedContext));
		refreshAll();
	}

	private void deleteSelectedAssignmentPreset()
	{
		AssignmentPresetOption item = (AssignmentPresetOption) assignmentPresetCombo.getSelectedItem();
		if (item == null || item.id == null)
		{
			return;
		}

		TileMarkerAssignmentPreset preset = strategyManager.findAssignmentPreset(item.id);
		if (preset == null || preset.isBuiltIn())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this wave strategy preset?", "Delete Preset", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		strategyManager.deleteAssignmentPreset(item.id);
		refreshAll();
	}

	private void clearAssignmentSelections()
	{
		strategyManager.clearWaveStrategyPresetIds(selectedContext);
		refreshAll();
	}

	private void populateStrategyCombo(JComboBox<StrategyOption> comboBox, int wave, String selectedId)
	{
		comboBox.removeAllItems();
		comboBox.addItem(new StrategyOption(null, ""));
		for (TileMarkerStrategyPreset preset : strategyManager.getStrategyPresets(TileMarkerWaveMap.fromWave(wave)))
		{
			comboBox.addItem(new StrategyOption(preset.getId(), preset.toString()));
		}
		selectStrategyComboValue(comboBox, selectedId);
	}

	private void selectStrategyComboValue(JComboBox<StrategyOption> comboBox, String selectedId)
	{
		for (int i = 0; i < comboBox.getItemCount(); i++)
		{
			StrategyOption item = comboBox.getItemAt(i);
			if ((selectedId == null && item.id == null) || (selectedId != null && selectedId.equals(item.id)))
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

	private String promptName(String title, String defaultValue)
	{
		JTextField field = new JTextField(defaultValue);
		BaPanelUi.fixedSize(field, CONTENT_WIDTH - 16, CONTROL_HEIGHT);
		int result = JOptionPane.showConfirmDialog(this, field, title, JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION || field.getText().trim().isEmpty())
		{
			return null;
		}
		return field.getText().trim();
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
		});
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
		if (strategyEditorDialog != null && strategyEditorDialog.isDisplayable())
		{
			if (strategyEditorPanel != null
					&& !strategyEditorPanel.selectStrategyForWave(strategyId, selectedContext, wave, strategyEditorDialog))
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
				selectedContext,
				wave
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
		strategyEditorDialog.setMinimumSize(new Dimension(520, 760));
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

	private ImageIcon scaledRoleIcon(BufferedImage image)
	{
		return new ImageIcon(image.getScaledInstance(ROLE_ICON_SIZE, ROLE_ICON_SIZE, Image.SCALE_SMOOTH));
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

	private static class AssignmentPresetOption
	{
		private final String id;
		private final String label;

		private AssignmentPresetOption(String id, String label)
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
