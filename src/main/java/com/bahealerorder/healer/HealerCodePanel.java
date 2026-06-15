package com.bahealerorder.healer;

import com.bahealerorder.common.BaPartySyncMemberStatus;
import com.bahealerorder.healer.codes.RunPreset;
import com.bahealerorder.healer.codes.WaveCode;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.inject.Inject;
import javax.inject.Singleton;
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
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.AsyncBufferedImage;

@Singleton
public class HealerCodePanel extends PluginPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
	private static final int CREATE_CODE_GAP = 24;
	private static final int PRESET_BUTTON_WAVE_GAP = 26;
	private static final int WAVE_LABEL_WIDTH = 48;
	private static final int TAB_ICON_SIZE = 24;
	private static final int OVERVIEW_ICON_SIZE = 28;
	private static final int OVERVIEW_SKULL_ICON_SIZE = 14;
	private static final int OVERVIEW_CELL_HEIGHT = 50;
	private static final int OVERVIEW_CELL_GAP = 7;
	private static final int OVERVIEW_HEADER_GAP = 10;
	private static final int OVERVIEW_COLUMN_GAP = 4;
	private static final String OVERVIEW_ICON_RESOURCE_PATH = "/com/bahealerorder/overview/";
	private static final Font TITLE_FONT = FontManager.getRunescapeBoldFont();
	private static final Font LABEL_FONT = FontManager.getRunescapeSmallFont();
	private static final WaveOverviewColumn[] WAVE_9_OVERVIEW_COLUMNS = {
			new WaveOverviewColumn("Ranger", "penance_ranger.png", new WaveOverviewEntry[] {
					WaveOverviewEntry.dead("6s", "18.0"),
					WaveOverviewEntry.dead("12s", "24.6"),
					WaveOverviewEntry.spawned("18s"),
					WaveOverviewEntry.spawned("24s"),
					WaveOverviewEntry.spawned("30s"),
					WaveOverviewEntry.pending("36s"),
					WaveOverviewEntry.pending("42s"),
					WaveOverviewEntry.pending("R1")
			}),
			new WaveOverviewColumn("Fighter", "penance_fighter.png", new WaveOverviewEntry[] {
					WaveOverviewEntry.dead("6s", "20.4"),
					WaveOverviewEntry.spawned("12s"),
					WaveOverviewEntry.spawned("18s"),
					WaveOverviewEntry.spawned("24s"),
					WaveOverviewEntry.pending("30s"),
					WaveOverviewEntry.pending("36s"),
					WaveOverviewEntry.pending("R1"),
					WaveOverviewEntry.pending("R2")
			}),
			new WaveOverviewColumn("Runner", "penance_runner.png", new WaveOverviewEntry[] {
					WaveOverviewEntry.dead("6s", "15.0"),
					WaveOverviewEntry.dead("12s", "18.0"),
					WaveOverviewEntry.spawned("18s"),
					WaveOverviewEntry.spawned("24s"),
					WaveOverviewEntry.spawned("30s"),
					WaveOverviewEntry.pending("R1"),
					WaveOverviewEntry.pending("R2"),
					WaveOverviewEntry.pending("R3"),
					WaveOverviewEntry.pending("R4")
			}),
			new WaveOverviewColumn("Healer", "penance_healer.png", new WaveOverviewEntry[] {
					WaveOverviewEntry.dead("6s", "29.4"),
					WaveOverviewEntry.predicted("12s", "45.0"),
					WaveOverviewEntry.predicted("18s", "?"),
					WaveOverviewEntry.spawned("24s"),
					WaveOverviewEntry.pending("30s"),
					WaveOverviewEntry.pending("36s"),
					WaveOverviewEntry.pending("R1"),
					WaveOverviewEntry.pending("R2")
			})
	};

	private final HealerCodeManager codeManager;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	private final JComboBox<ComboItem> presetCombo = new JComboBox<>();
	private final Map<Integer, JComboBox<ComboItem>> waveCombos = new HashMap<>();
	private final JComboBox<ComboItem> importWaveCombo = new JComboBox<>();
	private final JComboBox<ComboItem> userWaveCodeCombo = new JComboBox<>();
	private final JTextField importName = new JTextField();
	private final JTextArea importCode = new JTextArea();
	private final JPanel contentPanel = new JPanel();
	private final JPanel tabDisplayPanel = new JPanel(new BorderLayout());
	private final MaterialTabGroup tabGroup = new MaterialTabGroup(tabDisplayPanel);
	private final JLabel partySyncStatus = label("Party Sync Off", true);
	private final JPanel partySyncMembersPanel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private JButton deleteWaveCodeAction;

	private boolean refreshing;
	private boolean refreshingImport;

	@Inject
	public HealerCodePanel(HealerCodeManager codeManager, ItemManager itemManager, SpriteManager spriteManager)
	{
		this.codeManager = codeManager;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;

		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(contentPanel, BorderLayout.NORTH);

		contentPanel.add(header("BA Utilities"));
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(createPartySyncSection());
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(createTabSection());

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

	public void updatePartySyncStatus(String status, List<BaPartySyncMemberStatus> memberStatuses)
	{
		SwingUtilities.invokeLater(() ->
		{
			partySyncStatus.setText("Party Sync " + (status == null || status.isEmpty() ? "Unknown" : status));
			partySyncStatus.setForeground(getPartySyncStatusColor(status));
			partySyncMembersPanel.removeAll();
			partySyncMembersPanel.setVisible(shouldShowPartySyncMembers(status) || "Already in Party".equals(status));

			if (shouldShowPartySyncMembers(status))
			{
				for (BaPartySyncMemberStatus memberStatus : memberStatuses)
				{
					String statusText = memberStatus.isInParty() ? "In Party" : "Not In Party";
					partySyncMembersPanel.add(partySyncMemberRow(
							memberStatus.getName(),
							statusText,
							memberStatus.isInParty() ? Color.GREEN : Color.RED
					));
					partySyncMembersPanel.add(Box.createVerticalStrut(3));
				}
			}
			else if ("Already in Party".equals(status))
			{
				partySyncMembersPanel.add(partySyncMessage("You must leave your current party to join a BA party."));
			}

			contentPanel.revalidate();
			contentPanel.repaint();
		});
	}

	private boolean shouldShowPartySyncMembers(String status)
	{
		return "Connected".equals(status) || "In Wave".equals(status);
	}

	private JPanel createPartySyncSection()
	{
		JPanel section = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(new EmptyBorder(8, 8, 8, 8));
		section.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		section.setAlignmentX(LEFT_ALIGNMENT);
		section.add(partySyncHeaderRow());
		section.add(Box.createVerticalStrut(6));
		partySyncMembersPanel.setAlignmentX(LEFT_ALIGNMENT);
		partySyncMembersPanel.setVisible(false);
		section.add(partySyncMembersPanel);
		return section;
	}

	private JPanel partySyncHeaderRow()
	{
		partySyncStatus.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.add(partySyncStatus, BorderLayout.CENTER);
		return row;
	}

	private JPanel partySyncMemberRow(String name, String status, Color statusColor)
	{
		JLabel nameLabel = label(name);
		JLabel statusLabel = label(status);
		statusLabel.setForeground(statusColor);
		statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.add(nameLabel, BorderLayout.CENTER);
		row.add(statusLabel, BorderLayout.EAST);
		return row;
	}

	private JTextArea partySyncMessage(String text)
	{
		JTextArea message = message(text, ColorScheme.DARKER_GRAY_COLOR, CONTROL_HEIGHT * 2);
		message.setOpaque(false);
		return message;
	}

	private JPanel createTabSection()
	{
		JPanel overviewPanel = createWaveOverviewTab();
		JPanel healerPanel = createHealerTab();
		JPanel defenderPanel = createPlaceholderTab("Defender", "Defender strategy content will go here.");

		tabGroup.setLayout(new DynamicGridLayout(1, 3, 6, 0));
		tabGroup.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabGroup.setPreferredSize(new Dimension(CONTENT_WIDTH, 34));
		tabGroup.setMaximumSize(new Dimension(CONTENT_WIDTH, 34));
		tabGroup.setAlignmentX(LEFT_ALIGNMENT);

		MaterialTab overviewTab = itemTab(ItemID.MIRROR, "Wave Overview", overviewPanel);
		MaterialTab healerTab = itemTab(ItemID.BARBASSAULT_PENANCE_HEALER_HAT, "Healer Codes", healerPanel);
		MaterialTab defenderTab = itemTab(ItemID.BARBASSAULT_PENANCE_RUNNER_HAT, "Defender Strategies", defenderPanel);

		tabGroup.addTab(overviewTab);
		tabGroup.addTab(healerTab);
		tabGroup.addTab(defenderTab);
		tabGroup.select(healerTab);

		tabDisplayPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabDisplayPanel.setAlignmentX(LEFT_ALIGNMENT);
		tabDisplayPanel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

		JPanel panel = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		panel.add(tabGroup);
		panel.add(Box.createVerticalStrut(10));
		panel.add(tabDisplayPanel);
		return panel;
	}

	private JPanel createWaveOverviewTab()
	{
		JPanel panel = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

		JPanel section = section("Wave 9 Overview");
		section.add(message("Static proof of concept for expected wave 9 spawns and status display.", ColorScheme.DARKER_GRAY_COLOR, CONTROL_HEIGHT * 2));
		section.add(Box.createVerticalStrut(8));

		JPanel table = new JPanel(new DynamicGridLayout(1, 4, OVERVIEW_COLUMN_GAP, 0));
		table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		table.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, getWaveOverviewTableHeight()));
		table.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, getWaveOverviewTableHeight()));
		table.setAlignmentX(LEFT_ALIGNMENT);

		for (WaveOverviewColumn column : WAVE_9_OVERVIEW_COLUMNS)
		{
			table.add(createWaveOverviewColumn(column));
		}

		section.add(table);
		panel.add(section);
		return panel;
	}

	private JPanel createWaveOverviewColumn(WaveOverviewColumn column)
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR.darker());
		panel.setBorder(new EmptyBorder(4, 2, 4, 2));

		JLabel icon = new JLabel(loadOverviewIcon(column.iconResource));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setAlignmentX(LEFT_ALIGNMENT);
		icon.setPreferredSize(new Dimension(getWaveOverviewColumnWidth(), OVERVIEW_ICON_SIZE));
		icon.setMaximumSize(new Dimension(getWaveOverviewColumnWidth(), OVERVIEW_ICON_SIZE));
		panel.add(icon);
		panel.add(Box.createVerticalStrut(OVERVIEW_HEADER_GAP));

		for (WaveOverviewEntry entry : column.entries)
		{
			panel.add(createWaveOverviewEntry(entry));
			panel.add(Box.createVerticalStrut(OVERVIEW_CELL_GAP));
		}

		return panel;
	}

	private JPanel createWaveOverviewEntry(WaveOverviewEntry entry)
	{
		int width = getWaveOverviewColumnWidth();
		Color background = entry.status == WaveOverviewStatus.PENDING
				? ColorScheme.DARKER_GRAY_COLOR.darker().darker()
				: ColorScheme.DARKER_GRAY_COLOR.darker();
		Color labelColor = entry.status == WaveOverviewStatus.PENDING ? new Color(90, 90, 90) : ColorScheme.TEXT_COLOR;

		JPanel content = verticalPanel(background);
		content.setBorder(new EmptyBorder(4, 0, 2, 0));
		content.setPreferredSize(new Dimension(width, OVERVIEW_CELL_HEIGHT));
		content.setMaximumSize(new Dimension(width, OVERVIEW_CELL_HEIGHT));

		if (entry.status == WaveOverviewStatus.DEAD)
		{
			JLabel skull = centeredOverviewLabel("", entry.textColor, false, CONTROL_HEIGHT - 6);
			loadSkullIcon(skull);
			content.add(skull);
			content.add(Box.createVerticalStrut(4));
			content.add(centeredOverviewLabel(entry.displayText, entry.textColor, false, CONTROL_HEIGHT - 8));
		}
		else
		{
			content.add(centeredOverviewLabel(entry.label, labelColor, true, CONTROL_HEIGHT - 6));

			if (!entry.displayText.isEmpty())
			{
				content.add(centeredOverviewLabel(entry.displayText, entry.textColor, false, CONTROL_HEIGHT - 8));
			}
		}

		return content;
	}

	private JLabel centeredOverviewLabel(String text, Color color, boolean bold, int height)
	{
		JLabel label = label(text, bold);
		label.setForeground(color);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setPreferredSize(new Dimension(getWaveOverviewColumnWidth(), height));
		label.setMinimumSize(new Dimension(getWaveOverviewColumnWidth(), height));
		label.setMaximumSize(new Dimension(getWaveOverviewColumnWidth(), height));
		return label;
	}

	private ImageIcon loadOverviewIcon(String resource)
	{
		return new ImageIcon(new ImageIcon(getClass().getResource(OVERVIEW_ICON_RESOURCE_PATH + resource))
				.getImage()
				.getScaledInstance(OVERVIEW_ICON_SIZE, OVERVIEW_ICON_SIZE, Image.SCALE_SMOOTH));
	}

	private void loadSkullIcon(JLabel label)
	{
		spriteManager.getSpriteAsync(SpriteID.HEADICONS_PK, 0, image ->
				SwingUtilities.invokeLater(() -> label.setIcon(transparentIcon(image, OVERVIEW_SKULL_ICON_SIZE, 0.45f)))
		);
	}

	private ImageIcon transparentIcon(BufferedImage image, int size, float alpha)
	{
		BufferedImage transparent = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = transparent.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		graphics.setComposite(AlphaComposite.SrcOver.derive(alpha));
		graphics.drawImage(image, 0, 0, size, size, null);
		graphics.dispose();
		return new ImageIcon(transparent);
	}

	private int getWaveOverviewColumnWidth()
	{
		return (CONTENT_WIDTH - 16 - OVERVIEW_COLUMN_GAP * 3) / 4;
	}

	private int getWaveOverviewTableHeight()
	{
		return OVERVIEW_ICON_SIZE + OVERVIEW_HEADER_GAP + (OVERVIEW_CELL_HEIGHT + OVERVIEW_CELL_GAP) * 9 + 8;
	}

	private JPanel createHealerTab()
	{
		JPanel panel = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		panel.add(createPresetSection());
		panel.add(Box.createVerticalStrut(CREATE_CODE_GAP));
		panel.add(createImportCodeSection());
		return panel;
	}

	private JPanel createPlaceholderTab(String title, String text)
	{
		JPanel panel = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

		JPanel section = section(title);
		section.add(message(text, ColorScheme.DARKER_GRAY_COLOR, CONTROL_HEIGHT));
		panel.add(section);
		return panel;
	}

	private MaterialTab itemTab(int itemId, String tooltip, JPanel content)
	{
		MaterialTab tab = iconTab(tooltip, content);
		AsyncBufferedImage icon = itemManager.getImage(itemId);

		if (icon != null)
		{
			icon.onLoaded(() -> SwingUtilities.invokeLater(() -> tab.setIcon(scaledIcon(icon))));
		}

		return tab;
	}

	private MaterialTab iconTab(String tooltip, JPanel content)
	{
		MaterialTab tab = new MaterialTab(new ImageIcon(), tabGroup, content);
		tab.setToolTipText(tooltip);
		tab.setPreferredSize(new Dimension((CONTENT_WIDTH - 12) / 3, 32));
		tab.setMaximumSize(new Dimension((CONTENT_WIDTH - 12) / 3, 32));
		return tab;
	}

	private JTextArea message(String text, Color background, int height)
	{
		JTextArea message = new JTextArea(text);
		message.setBackground(background);
		message.setForeground(ColorScheme.TEXT_COLOR);
		message.setFont(LABEL_FONT);
		message.setEditable(false);
		message.setFocusable(false);
		message.setLineWrap(true);
		message.setWrapStyleWord(true);
		message.setOpaque(true);
		message.setBorder(null);
		message.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, height));
		message.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, height));
		message.setAlignmentX(LEFT_ALIGNMENT);
		return message;
	}

	private ImageIcon scaledIcon(BufferedImage image)
	{
		return new ImageIcon(image.getScaledInstance(TAB_ICON_SIZE, TAB_ICON_SIZE, Image.SCALE_SMOOTH));
	}

	private Color getPartySyncStatusColor(String status)
	{
		if ("Off".equals(status))
		{
			return Color.RED;
		}

		if ("Waiting for Team".equals(status))
		{
			return Color.WHITE;
		}

		if ("Joining".equals(status) || "Connecting".equals(status))
		{
			return Color.YELLOW;
		}

		if ("Connected".equals(status) || "In Wave".equals(status))
		{
			return Color.GREEN;
		}

		if ("Already in Party".equals(status))
		{
			return Color.ORANGE;
		}

		return ColorScheme.TEXT_COLOR;
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
		importName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter new code name...");
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
		userWaveCodeCombo.addItem(new ComboItem(null, "-- New --"));

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

	private static class WaveOverviewColumn
	{
		private final String name;
		private final String iconResource;
		private final WaveOverviewEntry[] entries;

		private WaveOverviewColumn(String name, String iconResource, WaveOverviewEntry[] entries)
		{
			this.name = name;
			this.iconResource = iconResource;
			this.entries = entries;
		}
	}

	private static class WaveOverviewEntry
	{
		private final String label;
		private final String displayText;
		private final WaveOverviewStatus status;
		private final Color textColor;

		private static WaveOverviewEntry spawned(String label)
		{
			return new WaveOverviewEntry(label, "", WaveOverviewStatus.SPAWNED, Color.WHITE);
		}

		private static WaveOverviewEntry pending(String label)
		{
			return new WaveOverviewEntry(label, "", WaveOverviewStatus.PENDING, new Color(90, 90, 90));
		}

		private static WaveOverviewEntry dead(String label, String deathTime)
		{
			return new WaveOverviewEntry(label, deathTime, WaveOverviewStatus.DEAD, new Color(150, 150, 150));
		}

		private static WaveOverviewEntry predicted(String label, String waveTime)
		{
			return new WaveOverviewEntry(label, waveTime, WaveOverviewStatus.PREDICTED, Color.ORANGE);
		}

		private WaveOverviewEntry(String label, String displayText, WaveOverviewStatus status, Color textColor)
		{
			this.label = label;
			this.displayText = displayText;
			this.status = status;
			this.textColor = textColor;
		}
	}

	private enum WaveOverviewStatus
	{
		SPAWNED,
		PENDING,
		DEAD,
		PREDICTED
	}
}
