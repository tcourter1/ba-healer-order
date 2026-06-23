package com.bahealerorder;

import com.bahealerorder.common.BaPartySyncMemberStatus;
import com.bahealerorder.common.BaHealerFoodCounts;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.WaveOverviewPanel;
import com.bahealerorder.healer.HealerCodePanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.AsyncBufferedImage;

@Singleton
public class BaUtilitiesPanel extends PluginPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
	private static final int TAB_ICON_SIZE = 24;
	private static final int ROLE_ICON_SIZE = 18;
	private static final Font TITLE_FONT = FontManager.getRunescapeBoldFont();
	private static final Font LABEL_FONT = FontManager.getRunescapeSmallFont();
	private static final String CALLED_FOOD_HTML_COLOR = "#00dc00";
	private static final String OVERVIEW_TAB = "overview";
	private static final String HEALER_TAB = "healer";
	private static final String DEFENDER_TAB = "defender";

	private final ItemManager itemManager;
	private final BaUtilitiesConfig config;
	private final ConfigManager configManager;
	private final WaveOverviewPanel waveOverviewPanel;
	private final HealerCodePanel healerCodePanel;
	private final JPanel contentPanel = new JPanel();
	private final JPanel tabDisplayPanel = new JPanel(new BorderLayout());
	private final MaterialTabGroup tabGroup = new MaterialTabGroup(tabDisplayPanel);
	private final Map<String, MaterialTab> tabsById = new LinkedHashMap<>();
	private final Map<String, JLabel> partySyncMemberStatusLabels = new LinkedHashMap<>();
	private final JLabel partySyncStatus = label("Party Sync Off", true);
	private final JPanel partySyncMembersPanel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private String lastPartySyncMemberStructure;

	@Inject
	public BaUtilitiesPanel(
			ItemManager itemManager,
			BaUtilitiesConfig config,
			ConfigManager configManager,
			WaveOverviewPanel waveOverviewPanel,
			HealerCodePanel healerCodePanel)
	{
		this.itemManager = itemManager;
		this.config = config;
		this.configManager = configManager;
		this.waveOverviewPanel = waveOverviewPanel;
		this.healerCodePanel = healerCodePanel;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

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
		waveOverviewPanel.refreshAll();
		healerCodePanel.refreshAll();
	}

	public void refreshLater()
	{
		SwingUtilities.invokeLater(this::refreshAll);
	}

	public void refreshOverviewLater()
	{
		SwingUtilities.invokeLater(waveOverviewPanel::refreshAll);
	}

	public void updatePartySyncStatus(String status, List<BaPartySyncMemberStatus> memberStatuses)
	{
		SwingUtilities.invokeLater(() ->
		{
			partySyncStatus.setText("Party Sync " + (status == null || status.isEmpty() ? "Unknown" : status));
			partySyncStatus.setForeground(getPartySyncStatusColor(status));
			String memberStructure = buildPartySyncMemberStructure(status, memberStatuses);

			if (memberStructure.equals(lastPartySyncMemberStructure))
			{
				updatePartySyncMemberStatusLabels(memberStatuses);
				return;
			}

			lastPartySyncMemberStructure = memberStructure;
			partySyncMemberStatusLabels.clear();
			partySyncMembersPanel.removeAll();
			partySyncMembersPanel.setVisible(!memberStatuses.isEmpty() || "Already in Party".equals(status));

			if (!memberStatuses.isEmpty())
			{
				for (BaPartySyncMemberStatus memberStatus : memberStatuses)
				{
					partySyncMembersPanel.add(partySyncMemberRow(memberStatus));
					partySyncMembersPanel.add(Box.createVerticalStrut(3));
				}
			}
			else if ("Already in Party".equals(status))
			{
				partySyncMembersPanel.add(message("You must leave your current party to join a BA party.", ColorScheme.DARKER_GRAY_COLOR, CONTROL_HEIGHT * 2, false));
			}

			contentPanel.revalidate();
			contentPanel.repaint();
		});
	}

	private String buildPartySyncMemberStructure(String status, List<BaPartySyncMemberStatus> memberStatuses)
	{
		if (memberStatuses.isEmpty())
		{
			return "Already in Party".equals(status) ? "message:already-in-party" : "empty";
		}

		StringBuilder builder = new StringBuilder();
		for (BaPartySyncMemberStatus memberStatus : memberStatuses)
		{
			builder.append(memberStatus.getName())
					.append(':')
					.append(memberStatus.getRole())
					.append('|');
		}
		return builder.toString();
	}

	private void updatePartySyncMemberStatusLabels(List<BaPartySyncMemberStatus> memberStatuses)
	{
		for (BaPartySyncMemberStatus memberStatus : memberStatuses)
		{
			JLabel statusLabel = partySyncMemberStatusLabels.get(memberStatus.getName());
			if (statusLabel == null) continue;

			String statusText = getPartySyncMemberStatusText(memberStatus);
			Color statusColor = getPartySyncMemberStatusColor(memberStatus);
			if (!statusText.equals(statusLabel.getText()))
			{
				statusLabel.setText(statusText);
			}
			if (!statusColor.equals(statusLabel.getForeground()))
			{
				statusLabel.setForeground(statusColor);
			}
		}
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

	private JPanel partySyncMemberRow(BaPartySyncMemberStatus memberStatus)
	{
		JLabel nameLabel = label(memberStatus.getName());
		JLabel statusLabel = label(getPartySyncMemberStatusText(memberStatus));
		statusLabel.setForeground(getPartySyncMemberStatusColor(memberStatus));
		statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		partySyncMemberStatusLabels.put(memberStatus.getName(), statusLabel);

		JPanel namePanel = new JPanel();
		namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
		namePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		namePanel.add(roleIconLabel(memberStatus.getRole()));
		namePanel.add(Box.createHorizontalStrut(5));
		namePanel.add(nameLabel);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.add(namePanel, BorderLayout.CENTER);
		row.add(statusLabel, BorderLayout.EAST);
		return row;
	}

	private String getPartySyncMemberStatusText(BaPartySyncMemberStatus memberStatus)
	{
		BaHealerFoodCounts counts = memberStatus.getHealerFoodCounts();
		if (counts != null)
		{
			return formatHealerFoodCounts(counts);
		}

		return memberStatus.isInParty() ? "In Party" : "Not In Party";
	}

	private Color getPartySyncMemberStatusColor(BaPartySyncMemberStatus memberStatus)
	{
		return memberStatus.getHealerFoodCounts() != null
				? ColorScheme.TEXT_COLOR
				: memberStatus.isInParty() ? Color.GREEN : Color.RED;
	}

	private String formatHealerFoodCounts(BaHealerFoodCounts counts)
	{
		return "<html>"
				+ formatFoodCount(counts.getTofu(), "T", counts.getCalledFood() == BaHealerFoodCounts.FOOD_TOFU)
				+ " "
				+ formatFoodCount(counts.getWorms(), "W", counts.getCalledFood() == BaHealerFoodCounts.FOOD_WORMS)
				+ " "
				+ formatFoodCount(counts.getMeat(), "M", counts.getCalledFood() == BaHealerFoodCounts.FOOD_MEAT)
				+ "</html>";
	}

	private String formatFoodCount(int count, String label, boolean called)
	{
		String text = count + label;
		return called ? "<font color=\"" + CALLED_FOOD_HTML_COLOR + "\">" + text + "</font>" : text;
	}

	private JLabel roleIconLabel(String roleName)
	{
		JLabel iconLabel = new JLabel();
		iconLabel.setPreferredSize(new Dimension(ROLE_ICON_SIZE, CONTROL_HEIGHT));
		iconLabel.setMaximumSize(new Dimension(ROLE_ICON_SIZE, CONTROL_HEIGHT));
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

		BaRole role = BaRole.fromDisplayName(roleName);

		if (role != null)
		{
			AsyncBufferedImage icon = itemManager.getImage(role.getPlayerIconItemId());
			icon.onLoaded(() -> SwingUtilities.invokeLater(() -> iconLabel.setIcon(scaledRoleIcon(icon))));
		}

		return iconLabel;
	}

	private JPanel createTabSection()
	{
		JPanel defenderPanel = placeholderTab("Defender", "Defender strategy content will go here.");
		List<SidePanelTab> tabs = new ArrayList<>();
		tabs.add(new SidePanelTab(OVERVIEW_TAB, ItemID.MIRROR, "Wave Overview", waveOverviewPanel));
		tabs.add(new SidePanelTab(HEALER_TAB, ItemID.BARBASSAULT_PENANCE_HEALER_HAT, "Healer Codes", healerCodePanel));
		tabs.add(new SidePanelTab(DEFENDER_TAB, ItemID.BARBASSAULT_PENANCE_RUNNER_HAT, "Defender Strategies", defenderPanel));

		tabGroup.setLayout(new DynamicGridLayout(1, tabs.size(), 6, 0));
		tabGroup.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabGroup.setPreferredSize(new Dimension(CONTENT_WIDTH, 34));
		tabGroup.setMaximumSize(new Dimension(CONTENT_WIDTH, 34));
		tabGroup.setAlignmentX(LEFT_ALIGNMENT);

		for (SidePanelTab tab : tabs)
		{
			addTab(tab, tabs.size());
		}

		selectSavedTab();

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

	private void addTab(SidePanelTab sidePanelTab, int tabCount)
	{
		MaterialTab tab = itemTab(sidePanelTab, tabCount);
		tab.setOnSelectEvent(() ->
		{
			configManager.setConfiguration(BaUtilitiesConfig.GROUP_NAME, BaUtilitiesConfig.SIDE_PANEL_TAB_KEY, sidePanelTab.id);
			SwingUtilities.invokeLater(() ->
			{
				SwingUtilities.updateComponentTreeUI(tabDisplayPanel);
				tabDisplayPanel.revalidate();
				tabDisplayPanel.repaint();
			});
			return true;
		});

		tabsById.put(sidePanelTab.id, tab);
		tabGroup.addTab(tab);
	}

	private void selectSavedTab()
	{
		MaterialTab tab = tabsById.get(config.sidePanelTab());

		if (tab == null)
		{
			tab = tabsById.get(HEALER_TAB);
		}

		if (tab != null)
		{
			tabGroup.select(tab);
		}
	}

	private JPanel placeholderTab(String title, String text)
	{
		JPanel panel = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

		JPanel section = section(title);
		section.add(message(text, ColorScheme.DARKER_GRAY_COLOR, CONTROL_HEIGHT * 2, true));
		panel.add(section);
		return panel;
	}

	private MaterialTab itemTab(SidePanelTab sidePanelTab, int tabCount)
	{
		MaterialTab tab = new MaterialTab(new ImageIcon(), tabGroup, wrapTabContent(sidePanelTab.content));
		tab.setToolTipText(sidePanelTab.tooltip);
		tab.setPreferredSize(new Dimension((CONTENT_WIDTH - 6 * (tabCount - 1)) / tabCount, 32));
		tab.setMaximumSize(new Dimension((CONTENT_WIDTH - 6 * (tabCount - 1)) / tabCount, 32));

		AsyncBufferedImage icon = itemManager.getImage(sidePanelTab.itemId);
		if (icon != null)
		{
			icon.onLoaded(() -> SwingUtilities.invokeLater(() -> tab.setIcon(scaledIcon(icon))));
		}

		return tab;
	}

	private JPanel wrapTabContent(JComponent content)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		panel.add(content, BorderLayout.NORTH);
		return panel;
	}

	private static class SidePanelTab
	{
		private final String id;
		private final int itemId;
		private final String tooltip;
		private final JComponent content;

		private SidePanelTab(String id, int itemId, String tooltip, JComponent content)
		{
			this.id = id;
			this.itemId = itemId;
			this.tooltip = tooltip;
			this.content = content;
		}
	}

	private ImageIcon scaledIcon(BufferedImage image)
	{
		return new ImageIcon(image.getScaledInstance(TAB_ICON_SIZE, TAB_ICON_SIZE, Image.SCALE_SMOOTH));
	}

	private ImageIcon scaledRoleIcon(BufferedImage image)
	{
		return new ImageIcon(image.getScaledInstance(ROLE_ICON_SIZE, ROLE_ICON_SIZE, Image.SCALE_SMOOTH));
	}

	private Color getPartySyncStatusColor(String status)
	{
		if ("Off".equals(status)) return Color.RED;
		if ("Waiting for Team".equals(status)) return Color.WHITE;
		if ("Joining".equals(status) || "Connecting".equals(status)) return Color.YELLOW;
		if ("Connected".equals(status) || "In Wave".equals(status)) return Color.GREEN;
		if ("Already in Party".equals(status)) return Color.ORANGE;
		return ColorScheme.TEXT_COLOR;
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

	private JTextArea message(String text, Color background, int height, boolean opaque)
	{
		JTextArea message = new JTextArea(text);
		message.setBackground(background);
		message.setForeground(ColorScheme.TEXT_COLOR);
		message.setFont(LABEL_FONT);
		message.setEditable(false);
		message.setFocusable(false);
		message.setLineWrap(true);
		message.setWrapStyleWord(true);
		message.setOpaque(opaque);
		message.setBorder(null);
		message.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, height));
		message.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, height));
		message.setAlignmentX(LEFT_ALIGNMENT);
		return message;
	}

	private static JLabel label(String text)
	{
		return label(text, false);
	}

	private static JLabel label(String text, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.TEXT_COLOR);
		label.setFont(bold ? TITLE_FONT : LABEL_FONT);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JPanel verticalPanel(Color background)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(background);
		return panel;
	}
}
