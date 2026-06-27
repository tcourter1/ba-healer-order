package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesConfig;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.SwingUtil;

@Singleton
public class WaveOverviewPanel extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
	private static final int OVERVIEW_ICON_SIZE = 28;
	private static final int OVERVIEW_SKULL_ICON_SIZE = 14;
	private static final int OVERVIEW_MAX_SKULL_ICON_SIZE = 24;
	private static final int ROLE_ICON_SIZE = 18;
	private static final int OVERVIEW_CELL_HEIGHT = 50;
	private static final int OVERVIEW_CELL_GAP = 7;
	private static final int OVERVIEW_HEADER_GAP = 10;
	private static final int OVERVIEW_COLUMN_GAP = 4;
	private static final String OVERVIEW_ICON_RESOURCE_PATH = "/com/bahealerorder/overview/";
	private static final Font TITLE_FONT = FontManager.getRunescapeBoldFont();
	private static final Font LABEL_FONT = FontManager.getRunescapeSmallFont();
	private static final BaOverviewNpcType[] COLUMNS = {
			BaOverviewNpcType.RANGER,
			BaOverviewNpcType.FIGHTER,
			BaOverviewNpcType.RUNNER,
			BaOverviewNpcType.HEALER
	};

	private final SpriteManager spriteManager;
	private final ItemManager itemManager;
	private final BaUtilitiesConfig config;
	private final ConfigManager configManager;
	private final BaWaveOverviewStore store;
	private final WaveOverviewSelectorPanel selectorPanel;
	private final Map<BaOverviewNpcType, ImageIcon> overviewIcons = new EnumMap<>(BaOverviewNpcType.class);
	private final JLabel titleLabel = label("Wave Overview", true);
	private final JPanel contentPanel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);

	private boolean loadingSkullIcon;
	private ImageIcon skullIcon;
	private String lastRenderSignature;

	@Inject
	public WaveOverviewPanel(
			SpriteManager spriteManager,
			ItemManager itemManager,
			BaUtilitiesConfig config,
			ConfigManager configManager,
			BaWaveOverviewStore store)
	{
		this.spriteManager = spriteManager;
		this.itemManager = itemManager;
		this.config = config;
		this.configManager = configManager;
		this.store = store;
		this.selectorPanel = new WaveOverviewSelectorPanel(store, this::createColumnMenuButton, () ->
		{
			lastRenderSignature = null;
			refreshAll();
		});

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentX(LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		add(createWaveOverviewSection());
		refreshAll();
	}

	public void refreshAll()
	{
		selectorPanel.refreshSelectors();
		BaWaveOverviewSnapshot snapshot = store.getSelectedSnapshot();
		String signature = buildRenderSignature(snapshot);

		if (signature.equals(lastRenderSignature))
		{
			return;
		}

		lastRenderSignature = signature;
		titleLabel.setText(getTitleText(snapshot));

		contentPanel.removeAll();
		populateWaveOverviewContent(contentPanel, snapshot);
		revalidate();
		repaint();
	}

	private JPanel createWaveOverviewSection()
	{
		JPanel section = section();
		section.add(selectorPanel);
		section.add(Box.createVerticalStrut(6));
		section.add(contentPanel);
		return section;
	}

	private void populateWaveOverviewContent(JPanel section, BaWaveOverviewSnapshot snapshot)
	{
		List<BaOverviewNpcType> columns = getVisibleColumns();

		if (snapshot == null)
		{
			section.add(store.getSelectedRunId() == null
					? centeredMessage("Recent runs will appear here")
					: createRunMetadataPanel(store.getSelectedRun(), true));
			return;
		}

		if (columns.isEmpty())
		{
			section.add(centeredMessage("Use the menu above to choose which NPCs appear here."));
			return;
		}

		JPanel table = new JPanel(new DynamicGridLayout(1, columns.size(), OVERVIEW_COLUMN_GAP, 0));
		int tableHeight = getWaveOverviewTableHeight(snapshot.getWave(), columns);
		table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		table.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, tableHeight));
		table.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, tableHeight));
		table.setAlignmentX(LEFT_ALIGNMENT);

		for (BaOverviewNpcType type : columns)
		{
			table.add(createWaveOverviewColumn(snapshot, type, columns.size()));
		}

		section.add(table);

		if (snapshot.getDuration() != null)
		{
			section.add(Box.createVerticalStrut(6));
			section.add(centeredLabelRow("Duration: " + snapshot.getDuration(), false, ColorScheme.DARKER_GRAY_COLOR));
		}

		BaWaveOverviewRun selectedRun = store.getSelectedRun();
		if (selectedRun != null && selectedRun.getRoundDuration() != null)
		{
			section.add(Box.createVerticalStrut(12));
			section.add(createRunMetadataPanel(selectedRun, false));
		}
	}

	private JPanel createWaveOverviewColumn(BaWaveOverviewSnapshot snapshot, BaOverviewNpcType type, int columnCount)
	{
		int columnWidth = getWaveOverviewColumnWidth(columnCount);
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR.darker());
		panel.setBorder(new EmptyBorder(4, 2, 4, 2));

		JLabel icon = new JLabel(loadOverviewIcon(type));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setAlignmentX(LEFT_ALIGNMENT);
		icon.setPreferredSize(new Dimension(columnWidth, OVERVIEW_ICON_SIZE));
		icon.setMaximumSize(new Dimension(columnWidth, OVERVIEW_ICON_SIZE));
		panel.add(icon);
		panel.add(Box.createVerticalStrut(OVERVIEW_HEADER_GAP));

		List<String> labels = BaWaveInfo.getLabels(snapshot.getWave(), type);
		for (int i = 0; i < labels.size(); i++)
		{
			panel.add(createWaveOverviewEntry(snapshot, type, i + 1, formatWaveOverviewLabel(labels.get(i)), columnCount));
			panel.add(Box.createVerticalStrut(OVERVIEW_CELL_GAP));
		}

		return panel;
	}

	private String formatWaveOverviewLabel(String label)
	{
		if (label == null)
		{
			return "";
		}

		if (label.matches("^\\(R\\d+\\)$"))
		{
			return label.substring(1, label.length() - 1);
		}

		return label.replaceFirst("\\s*\\(R\\d+\\)$", "");
	}

	private JPanel createWaveOverviewEntry(BaWaveOverviewSnapshot snapshot, BaOverviewNpcType type, int order, String label, int columnCount)
	{
		EntryState entry = getEntryState(snapshot, type, order);
		int width = getWaveOverviewColumnWidth(columnCount);
		Color background = entry.spawned || entry.dead
				? ColorScheme.DARKER_GRAY_COLOR.darker()
				: ColorScheme.DARKER_GRAY_COLOR.darker().darker();

		JPanel content = verticalPanel(background);
		content.setBorder(new EmptyBorder(4, 0, 2, 0));
		content.setPreferredSize(new Dimension(width, OVERVIEW_CELL_HEIGHT));
		content.setMaximumSize(new Dimension(width, OVERVIEW_CELL_HEIGHT));

		if (entry.dead)
		{
			int skullSize = getOverviewSkullIconSize(width);
			JLabel skull = centeredOverviewLabel("", entry.color, false, Math.max(CONTROL_HEIGHT - 6, skullSize), width);
			loadSkullIcon(skull, skullSize);
			content.add(skull);
			content.add(Box.createVerticalStrut(4));
		}
		else
		{
			content.add(centeredOverviewLabel(label, entry.labelColor, true, CONTROL_HEIGHT - 6, width));
		}

		if (entry.text != null && !entry.text.isEmpty())
		{
			JLabel text = centeredOverviewLabel(entry.text, entry.color, false, CONTROL_HEIGHT - 8, width);
			if (entry.dead)
			{
				text.setFont(getOverviewDeathFont(width));
			}
			content.add(text);
		}

		return content;
	}

	private EntryState getEntryState(BaWaveOverviewSnapshot snapshot, BaOverviewNpcType type, int order)
	{
		Integer deathTick = snapshot.getDeathTick(type, order);
		if (deathTick != null)
		{
			return EntryState.dead(formatWaveTick(deathTick));
		}

		Integer predictedDeathTick = snapshot.getPredictedDeathTick(type, order);
		if (predictedDeathTick != null)
		{
			return EntryState.predicted(formatWaveTick(predictedDeathTick));
		}

		if (snapshot.hasUnknownTtk(type, order))
		{
			return EntryState.predicted("?");
		}

		return snapshot.hasSpawned(type, order) ? EntryState.spawned() : EntryState.pending();
	}

	private String formatWaveTick(int waveTick)
	{
		return String.format(java.util.Locale.ROOT, "%.1f", Math.max(0, waveTick) * 0.6d);
	}

	private JLabel centeredOverviewLabel(String text, Color color, boolean bold, int height, int width)
	{
		JLabel label = label(text, bold);
		label.setForeground(color);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setPreferredSize(new Dimension(width, height));
		label.setMinimumSize(new Dimension(width, height));
		label.setMaximumSize(new Dimension(width, height));
		return label;
	}

	private JButton createColumnMenuButton()
	{
		JButton menuButton = new JButton(createHamburgerIcon());
		menuButton.setToolTipText("Choose NPC columns");
		SwingUtil.removeButtonDecorations(menuButton);
		fixedSize(menuButton, CONTROL_HEIGHT + 4, CONTROL_HEIGHT);
		menuButton.addActionListener(event -> createColumnMenu().show(menuButton, 0, menuButton.getHeight()));
		return menuButton;
	}

	private ImageIcon createHamburgerIcon()
	{
		int size = 14;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(ColorScheme.TEXT_COLOR);
		graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(2, 3, 12, 3);
		graphics.drawLine(2, 7, 12, 7);
		graphics.drawLine(2, 11, 12, 11);
		graphics.dispose();
		return new ImageIcon(image);
	}

	private JPopupMenu createColumnMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		menu.add(columnMenuItem("Show Rangers", BaUtilitiesConfig.SHOW_OVERVIEW_RANGERS_KEY, config.showOverviewRangers()));
		menu.add(columnMenuItem("Show Fighters", BaUtilitiesConfig.SHOW_OVERVIEW_FIGHTERS_KEY, config.showOverviewFighters()));
		menu.add(columnMenuItem("Show Runners", BaUtilitiesConfig.SHOW_OVERVIEW_RUNNERS_KEY, config.showOverviewRunners()));
		menu.add(columnMenuItem("Show Healers", BaUtilitiesConfig.SHOW_OVERVIEW_HEALERS_KEY, config.showOverviewHealers()));
		return menu;
	}

	private JCheckBoxMenuItem columnMenuItem(String label, String key, boolean selected)
	{
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, selected);
		item.addActionListener(event ->
		{
			configManager.setConfiguration(BaUtilitiesConfig.GROUP_NAME, key, item.isSelected());
			refreshAll();
		});
		return item;
	}

	private ImageIcon loadOverviewIcon(BaOverviewNpcType type)
	{
		ImageIcon icon = overviewIcons.get(type);

		if (icon != null)
		{
			return icon;
		}

		icon = new ImageIcon(new ImageIcon(getClass().getResource(OVERVIEW_ICON_RESOURCE_PATH + getIconResource(type)))
				.getImage()
				.getScaledInstance(OVERVIEW_ICON_SIZE, OVERVIEW_ICON_SIZE, Image.SCALE_SMOOTH));
		overviewIcons.put(type, icon);
		return icon;
	}

	private String getIconResource(BaOverviewNpcType type)
	{
		switch (type)
		{
			case RANGER:
				return "penance_ranger.png";
			case FIGHTER:
				return "penance_fighter.png";
			case RUNNER:
				return "penance_runner.png";
			case HEALER:
				return "penance_healer.png";
			default:
				throw new IllegalArgumentException("Unsupported NPC type " + type);
		}
	}

	private int getOverviewSkullIconSize(int columnWidth)
	{
		return Math.max(OVERVIEW_SKULL_ICON_SIZE, Math.min(OVERVIEW_MAX_SKULL_ICON_SIZE, columnWidth / 4));
	}

	private Font getOverviewDeathFont(int columnWidth)
	{
		float extraSize = Math.max(0f, Math.min(4f, (columnWidth - getWaveOverviewColumnWidth(COLUMNS.length)) / 18f));
		return LABEL_FONT.deriveFont(LABEL_FONT.getSize2D() + extraSize);
	}

	private void loadSkullIcon(JLabel label, int size)
	{
		if (skullIcon != null)
		{
			label.setIcon(getScaledSkullIcon(size));
			return;
		}

		if (loadingSkullIcon)
		{
			return;
		}

		loadingSkullIcon = true;
		spriteManager.getSpriteAsync(SpriteID.HEADICONS_PK, 0, image ->
				SwingUtilities.invokeLater(() ->
				{
					skullIcon = transparentIcon(image, OVERVIEW_MAX_SKULL_ICON_SIZE, 0.45f);
					lastRenderSignature = null;
					refreshAll();
				})
		);
	}

	private ImageIcon getScaledSkullIcon(int size)
	{
		if (skullIcon == null || size == OVERVIEW_MAX_SKULL_ICON_SIZE)
		{
			return skullIcon;
		}

		return new ImageIcon(skullIcon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
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

	private int getWaveOverviewColumnWidth(int columnCount)
	{
		return (CONTENT_WIDTH - 16 - OVERVIEW_COLUMN_GAP * (columnCount - 1)) / columnCount;
	}

	private int getWaveOverviewTableHeight(int wave, List<BaOverviewNpcType> columns)
	{
		int maxRows = 0;

		for (BaOverviewNpcType type : columns)
		{
			maxRows = Math.max(maxRows, BaWaveInfo.getExpectedCount(wave, type));
		}

		return OVERVIEW_ICON_SIZE + OVERVIEW_HEADER_GAP + (OVERVIEW_CELL_HEIGHT + OVERVIEW_CELL_GAP) * maxRows + 8;
	}

	private List<BaOverviewNpcType> getVisibleColumns()
	{
		List<BaOverviewNpcType> columns = new ArrayList<>();

		for (BaOverviewNpcType type : COLUMNS)
		{
			if (isColumnVisible(type))
			{
				columns.add(type);
			}
		}

		return columns;
	}

	private boolean isColumnVisible(BaOverviewNpcType type)
	{
		switch (type)
		{
			case RANGER:
				return config.showOverviewRangers();
			case FIGHTER:
				return config.showOverviewFighters();
			case RUNNER:
				return config.showOverviewRunners();
			case HEALER:
				return config.showOverviewHealers();
			default:
				return false;
		}
	}

	private String buildRenderSignature(BaWaveOverviewSnapshot snapshot)
	{
		BaWaveOverviewRun selectedRun = store.getSelectedRun();

		return store.getSelectedRunId()
				+ ":" + store.getSelectedWave()
				+ ":" + store.isSelectedWaveInProgress()
				+ ":" + getVisibleColumns()
				+ ":" + (skullIcon != null)
				+ ":" + (selectedRun == null ? "none" : selectedRun.metadataSignature())
				+ ":" + (snapshot == null ? "none" : snapshot.signature());
	}

	private String getTitleText(BaWaveOverviewSnapshot snapshot)
	{
		if (!BaWaveInfo.isValidWave(snapshot == null ? -1 : snapshot.getWave()))
		{
			return "Recent Runs";
		}

		return store.isSelectedWaveInProgress()
				? "Wave " + snapshot.getWave() + " In Progress..."
				: "Wave " + snapshot.getWave() + " Overview";
	}

	private JPanel section()
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(centeredLabelRow(titleLabel, ColorScheme.DARKER_GRAY_COLOR));
		panel.add(Box.createVerticalStrut(6));
		return panel;
	}

	private JPanel centeredLabelRow(String text, boolean bold, Color background)
	{
		JLabel label = label(text, bold);
		return centeredLabelRow(label, background);
	}

	private JPanel centeredLabelRow(JLabel label, Color background)
	{
		label.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(background);
		row.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.add(label, BorderLayout.CENTER);
		return row;
	}

	private JPanel centeredMessage(String text)
	{
		JTextPane message = new JTextPane();
		message.setText(text);
		message.setForeground(ColorScheme.TEXT_COLOR);
		message.setFont(LABEL_FONT);
		message.setEditable(false);
		message.setFocusable(false);
		message.setOpaque(false);

		StyledDocument document = message.getStyledDocument();
		SimpleAttributeSet center = new SimpleAttributeSet();
		StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
		document.setParagraphAttributes(0, document.getLength(), center, false);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		int height = getMessageHeight(text);
		panel.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, height));
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, height));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(message, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createRunMetadataPanel(BaWaveOverviewRun run, boolean showMissingDuration)
	{
		if (run == null)
		{
			return centeredMessage("Select a wave to view.");
		}

		if (!showMissingDuration && run.getRoundDuration() == null)
		{
			return verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		}

		List<BaTeamMember> members = run.getTeamMembers();
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		int height = Math.max(CONTROL_HEIGHT * 2, CONTROL_HEIGHT * (members.size() + 1) + (members.isEmpty() ? 0 : 10));
		panel.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, height));
		panel.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, height));
		panel.setAlignmentX(LEFT_ALIGNMENT);

		for (BaTeamMember member : members)
		{
			panel.add(metadataMemberRow(member));
		}

		if (!members.isEmpty())
		{
			panel.add(Box.createVerticalStrut(10));
		}

		panel.add(roundDurationRow(run.getRoundDuration() == null ? "-" : run.getRoundDuration()));
		return panel;
	}

	private int getMessageHeight(String text)
	{
		int lines = text == null || text.isEmpty() ? 1 : text.split("\\R", -1).length;
		return Math.max(CONTROL_HEIGHT * 2, CONTROL_HEIGHT * lines);
	}

	private JPanel metadataMemberRow(BaTeamMember member)
	{
		JLabel nameLabel = label(member.getName(), false);
		nameLabel.setHorizontalAlignment(SwingConstants.LEFT);

		JPanel namePanel = new JPanel();
		namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
		namePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		namePanel.add(roleIconLabel(member.getRole()));
		namePanel.add(Box.createHorizontalStrut(5));
		namePanel.add(nameLabel);

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.add(namePanel, BorderLayout.CENTER);

		return row;
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

	private ImageIcon scaledRoleIcon(BufferedImage image)
	{
		return new ImageIcon(image.getScaledInstance(ROLE_ICON_SIZE, ROLE_ICON_SIZE, Image.SCALE_SMOOTH));
	}

	private JPanel roundDurationRow(String duration)
	{
		JLabel label = label("Round duration: ", true);
		JLabel value = label(duration, true);
		value.setForeground(Color.ORANGE);

		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setPreferredSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(CONTENT_WIDTH - 16, CONTROL_HEIGHT));
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.add(label);
		row.add(value);
		return row;
	}

	private JLabel label(String text, boolean bold)
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

	private static void fixedSize(JComponent component, int width, int height)
	{
		Dimension size = new Dimension(width, height);
		component.setPreferredSize(size);
		component.setMinimumSize(size);
		component.setMaximumSize(size);
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	private static class EntryState
	{
		private final boolean spawned;
		private final boolean dead;
		private final Color labelColor;
		private final Color color;
		private final String text;

		private static EntryState pending()
		{
			return new EntryState(false, false, new Color(90, 90, 90), new Color(90, 90, 90), "");
		}

		private static EntryState spawned()
		{
			return new EntryState(true, false, ColorScheme.TEXT_COLOR, ColorScheme.TEXT_COLOR, "");
		}

		private static EntryState dead(String text)
		{
			return new EntryState(true, true, new Color(150, 150, 150), new Color(150, 150, 150), text);
		}

		private static EntryState predicted(String text)
		{
			return new EntryState(true, false, ColorScheme.TEXT_COLOR, Color.ORANGE, text);
		}

		private EntryState(boolean spawned, boolean dead, Color labelColor, Color color, String text)
		{
			this.spawned = spawned;
			this.dead = dead;
			this.labelColor = labelColor;
			this.color = color;
			this.text = text;
		}
	}
}
