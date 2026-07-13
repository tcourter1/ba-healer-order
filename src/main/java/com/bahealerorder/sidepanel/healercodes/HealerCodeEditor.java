package com.bahealerorder.sidepanel.healercodes;

import com.bahealerorder.common.BaOverviewNpcType;
import com.bahealerorder.common.BaWaveInfo;
import com.bahealerorder.common.BaIcons;
import com.bahealerorder.common.BaClipboard;
import com.bahealerorder.healer.HealerCodeManager;
import com.bahealerorder.healer.codes.HealerCodeExportResult;
import com.bahealerorder.healer.codes.CallCode;
import com.bahealerorder.healer.codes.HealerCodeFormatter;
import com.bahealerorder.healer.codes.HealerCodeOverstock;
import com.bahealerorder.healer.codes.HealerCodeParser;
import com.bahealerorder.healer.codes.HealerInstruction;
import com.bahealerorder.healer.codes.WaveCode;
import com.bahealerorder.sidepanel.BaPanelUi;
import com.bahealerorder.sidepanel.BaTransferDialog;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Cursor;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.SwingUtil;

class HealerCodeEditor extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int EDITOR_WIDTH = 960;
	private static final int EDITOR_HEIGHT = 680;
	private static final int ROW_LABEL_WIDTH = 58;
	private static final int HEADER_HEIGHT = 60;
	private static final int CELL_WIDTH = 106;
	private static final int CELL_HEIGHT = 58;
	private static final int FOOD_FIELD_WIDTH = 50;
	private static final int TIME_FIELD_WIDTH = 64;
	private static final int TIME_PAIR_FIELD_WIDTH = 44;
	private static final int ADVANCED_FIELD_WIDTH = 60;
	private static final int RESERVE_SPAWN_FIELD_WIDTH = 42;
	private static final int DELETE_BUTTON_WIDTH = 24;
	private static final int CODE_ACTION_BUTTON_WIDTH = 120;
	private static final int CODE_POPUP_WIDTH = 195;
	private static final int NAME_FIELD_WIDTH = 360;
	private static final int CUSTOM_OVERSTOCK_FIELD_WIDTH = 220;
	private static final int EXPECTED_WAVE_END_FIELD_WIDTH = 54;
	private static final int TEXT_CODE_AREA_HEIGHT = 300;
	private static final int TEXT_HELP_WIDTH = EDITOR_WIDTH - 20;
	private static final int WAVE_TO_CODE_GAP = 54;
	private static final int OPTIONS_TO_TABLE_GAP = 30;
	private static final int TABLE_TO_TEXT_AREAS_GAP = 40;
	private static final String AFTER_PLACEHOLDER = "At or after...";
	private static final String BEFORE_PLACEHOLDER = "Before...";
	private static final String EXACT_PLACEHOLDER = "Exactly...";
	private static final String AFTER_TOOLTIP = "The last food for this code must be used at or after this time";
	private static final String BEFORE_TOOLTIP = "The first food for this code must be used before this time";
	private static final String EXACT_TOOLTIP = "The last food for this code must be used exactly at this time";
	private static final ImageIcon HEALER_ICON = loadHealerIcon();
	private static final String ADVANCED_CARD = "advanced";
	private static final String TEXT_CARD = "text";

	private final HealerCodeManager codeManager;
	private final Runnable codesChanged;
	private final JComboBox<BaPanelUi.ComboOption> codeCombo = BaPanelUi.fixedPopupWidthCombo(CODE_POPUP_WIDTH);
	private final JComboBox<Integer> waveCombo = new JComboBox<>();
	private final JTextField codeName = new JTextField();
	private final JTextArea codeTextSource = new JTextArea();
	private final JComboBox<HealerCodeOverstock> overstockCombo = new JComboBox<>(HealerCodeOverstock.values());
	private final JTextField customOverstockInstructions = new PlaceholderTextField("", "Custom instructions...");
	private final JCheckBox alchHorn = new JCheckBox();
	private final JTextField expectedWaveEnd = new JTextField();
	private final JTextArea restockingInstructions = new JTextArea();
	private final JTextArea additionalNotes = new JTextArea();
	private final JButton codeActionButton = new JButton();
	private final JButton helpButton = BaPanelUi.action("Show Help", this::toggleHelp, 96, CONTROL_HEIGHT);
	private final JButton textViewButton = BaPanelUi.action("Text View", this::toggleTextView, 126, CONTROL_HEIGHT);
	private final CardLayout modeLayout = new CardLayout();
	private final JPanel modePanel = new JPanel(modeLayout);
	private final JPanel helpPanel = BaPanelUi.verticalPanel(ColorScheme.DARK_GRAY_COLOR);
	private final Component helpGap = Box.createVerticalStrut(10);
	private final JPanel codeGridPanel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final Map<CellKey, HealerInstruction> cellInstructions = new HashMap<>();
	private final Map<CellKey, CellTimingOption> cellTimingOptions = new HashMap<>();
	private final Map<Integer, JTextField> expectedTimeFields = new HashMap<>();

	private int selectedWave;
	private String selectedCodeId;
	private int visibleCallCount;
	private CellKey activeCell;
	private JPanel activeEditorPanel;
	private JSpinner activeFoodField;
	private JTextField activeAdvancedField;
	private CellTimingOption activeTimingOption = CellTimingOption.NONE;
	private JTextField activeAfterField;
	private JTextField activeBeforeField;
	private JTextField activeExactField;
	private String textSnapshot;
	private boolean textChangedAfterSnapshot;
	private boolean textMode;
	private boolean helpVisible = true;
	private boolean timingMenuOpen;
	private boolean timingOptionApplied;
	private long ignoreFocusCommitUntilMillis;
	private boolean refreshing;
	private boolean dirty;

	HealerCodeEditor(HealerCodeManager codeManager, Runnable codesChanged, int initialWave, String initialCodeId)
	{
		this.codeManager = codeManager;
		this.codesChanged = codesChanged;
		this.selectedWave = requireWave(initialWave);
		this.visibleCallCount = defaultVisibleCallCountForWave(selectedWave);
		this.helpVisible = codeManager.isCodeEditorHelpVisible();

		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 10));
		setPreferredSize(new Dimension(EDITOR_WIDTH + 20, EDITOR_HEIGHT));
		codeGridPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		add(createHeader(), BorderLayout.NORTH);
		add(createBody(), BorderLayout.CENTER);
		add(createActionRow(), BorderLayout.SOUTH);

		BaPanelUi.addTextChangeListener(codeName, this::markDirty);
		BaPanelUi.addTextChangeListener(codeTextSource, this::markDirty);
		BaPanelUi.addTextChangeListener(customOverstockInstructions, this::markDirty);
		BaPanelUi.addTextChangeListener(expectedWaveEnd, this::markDirty);
		BaPanelUi.addTextChangeListener(restockingInstructions, this::markDirty);
		BaPanelUi.addTextChangeListener(additionalNotes, this::markDirty);
		overstockCombo.addActionListener(event ->
		{
			updateCustomOverstockVisibility();
			markDirty();
		});
		alchHorn.addActionListener(event -> markDirty());

		refreshCodeCombo(initialCodeId);
		if (initialCodeId == null)
		{
			clearDraft();
		}
		else
		{
			loadCode(initialCodeId);
		}
	}

	private JPanel createHeader()
	{
		for (int wave = 1; wave <= 10; wave++)
		{
			waveCombo.addItem(wave);
		}
		BaPanelUi.styleCombo(waveCombo, 84, CONTROL_HEIGHT);
		waveCombo.setSelectedItem(selectedWave);
		waveCombo.addActionListener(event -> changeWave());

		BaPanelUi.styleCombo(codeCombo, 300, CONTROL_HEIGHT);
		codeCombo.setRenderer(BaPanelUi.comboOptionRenderer());
		codeCombo.addActionListener(event ->
		{
			if (refreshing) return;

			if (!confirmDiscard(this))
			{
				selectCodeComboValue(selectedCodeId);
				return;
			}

			loadCode(BaPanelUi.selectedId(codeCombo));
		});

		BaPanelUi.styleActionButton(codeActionButton, CODE_ACTION_BUTTON_WIDTH, CONTROL_HEIGHT);
		codeActionButton.addActionListener(event -> runCodeAction());

		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, EDITOR_WIDTH, CONTROL_HEIGHT);
		row.add(label("Wave", true));
		row.add(Box.createHorizontalStrut(6));
		row.add(waveCombo);
		row.add(Box.createHorizontalStrut(WAVE_TO_CODE_GAP));
		row.add(label("Code", true));
		row.add(Box.createHorizontalStrut(6));
		row.add(codeCombo);
		row.add(Box.createHorizontalStrut(6));
		row.add(codeActionButton);
		row.add(Box.createHorizontalGlue());
		row.add(helpButton);
		row.add(Box.createHorizontalStrut(6));
		row.add(textViewButton);
		return row;
	}

	private JPanel createBody()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		setupHelpPanel();
		panel.add(helpPanel);
		panel.add(helpGap);
		panel.add(createNameRow());
		panel.add(Box.createVerticalStrut(14));
		modePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		modePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		modePanel.add(createAdvancedBody(), ADVANCED_CARD);
		modePanel.add(createTextBody(), TEXT_CARD);
		panel.add(modePanel);
		return panel;
	}

	private JPanel createNameRow()
	{
		BaPanelUi.styleTextInput(codeName, NAME_FIELD_WIDTH, CONTROL_HEIGHT);

		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, EDITOR_WIDTH, CONTROL_HEIGHT);
		row.add(label("Name", true));
		row.add(Box.createHorizontalStrut(8));
		row.add(codeName);
		row.add(Box.createHorizontalGlue());
		return row;
	}

	private JPanel createAdvancedBody()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(createOptionsRow());
		panel.add(Box.createVerticalStrut(OPTIONS_TO_TABLE_GAP));
		panel.add(codeGridPanel);
		panel.add(Box.createVerticalStrut(TABLE_TO_TEXT_AREAS_GAP));
		panel.add(createTextAreas());
		return panel;
	}

	private JPanel createTextBody()
	{
		BaPanelUi.styleTextArea(codeTextSource, 10);

		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Code", true));
		panel.add(Box.createVerticalStrut(6));
		panel.add(BaPanelUi.wrapTextArea(codeTextSource, EDITOR_WIDTH, TEXT_CODE_AREA_HEIGHT));
		return panel;
	}

	private void setupHelpPanel()
	{
		helpPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
		helpPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		helpPanel.setMaximumSize(new Dimension(EDITOR_WIDTH, Integer.MAX_VALUE));
		refreshHelpPanel();
	}

	private void toggleHelp()
	{
		helpVisible = !helpVisible;
		codeManager.setCodeEditorHelpVisible(helpVisible);
		refreshHelpPanel();
	}

	private void refreshHelpPanel()
	{
		helpButton.setText(helpVisible ? "Hide Help" : "Show Help");
		helpPanel.removeAll();
		helpPanel.setVisible(helpVisible);
		helpGap.setVisible(helpVisible);

		if (helpVisible)
		{
			if (textMode)
			{
				addTextViewHelp();
			}
			else
			{
				addStructuredHelp();
			}
		}

		Dimension preferred = helpPanel.getPreferredSize();
		helpPanel.setMaximumSize(new Dimension(EDITOR_WIDTH, helpVisible ? preferred.height : 0));
		helpPanel.revalidate();
		helpPanel.repaint();
	}

	private void addStructuredHelp()
	{
		helpPanel.add(linkedHelpLine(
				"To begin, select the " + helpEmphasis("wave")
						+ " you'd like to create a code for. Simple codes can be created by adding "
						+ helpEmphasis("food counts") + " and " + helpEmphasis("timings")
						+ " to each healer in the table below. To paste in an existing code or type more advanced syntax, switch to the "
						+ helpLink("text-view", "Text View") + "."
		));
	}

	private void addTextViewHelp()
	{
		helpPanel.add(linkedHelpLine(
				"Standard healer code notation is supported, plus some advanced features. Paste a code in the area below to automatically attempt to convert it to a format the plugin supports. Switch back to the "
						+ helpLink("structured-editor", "Structured Editor") + " to ensure your code has been parsed properly."
		));
		helpPanel.add(Box.createVerticalStrut(8));
		helpPanel.add(helpLine(
				helpEmphasis("Numbers") + " separated by hyphens correspond to each healer as spawned, e.g. "
						+ helpCode("1-2-3-4") + "."
		));
		helpPanel.add(Box.createVerticalStrut(6));
		helpPanel.add(helpLine(
				helpEmphasis("Backslashes") + " or " + helpEmphasis("newlines") + " indicate call changes."
		));
		helpPanel.add(Box.createVerticalStrut(6));
		helpPanel.add(helpLine(
				helpEmphasis("(Parentheses)") + " indicate that the last food must be used "
						+ helpEmphasis("at") + " or " + helpEmphasis("after") + " this time, e.g. "
						+ helpCode("6(18)") + "."
		));
		helpPanel.add(Box.createVerticalStrut(6));
		helpPanel.add(helpLine(
				helpEmphasis("[Square brackets]") + " indicate that the first food must be used "
						+ helpEmphasis("before") + " this time, e.g. " + helpCode("1[45]") + "."
		));
		helpPanel.add(Box.createVerticalStrut(6));
		helpPanel.add(helpLine(
				"Food separated by a " + helpEmphasis("slash")
						+ " indicates that either amount of food can be used, e.g. " + helpCode("1/2")
						+ ". This impacts display only - code execution in-game only uses the first number."
		));
		helpPanel.add(Box.createVerticalStrut(6));
		helpPanel.add(helpLine(
				"Food separated by a " + helpEmphasis("comma")
						+ " indicates food that should be used before and after a restock, during the same call, e.g. "
						+ helpCode("6,2") + "."
		));
		helpPanel.add(Box.createVerticalStrut(6));
		helpPanel.add(helpLine(
				"Additional text and text preceded by " + helpEmphasis("// two slashes")
						+ " are treated as additional notes."
		));
	}

	private JLabel helpLine(String text)
	{
		JLabel label = label("<html><div width='" + TEXT_HELP_WIDTH + "'>" + text + "</div></html>", false);
		label.setFont(FontManager.getRunescapeFont());
		label.setMaximumSize(new Dimension(TEXT_HELP_WIDTH, Integer.MAX_VALUE));
		return label;
	}

	private JEditorPane linkedHelpLine(String text)
	{
		JEditorPane pane = new JEditorPane("text/html",
				"<html><body style='margin:0; padding:0; color:" + toHex(ColorScheme.TEXT_COLOR) + ";'>"
						+ "<div width='" + TEXT_HELP_WIDTH + "'>" + text + "</div></body></html>");
		pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		pane.setFont(FontManager.getRunescapeFont());
		pane.setEditable(false);
		pane.setOpaque(false);
		pane.setBorder(BorderFactory.createEmptyBorder());
		pane.setAlignmentX(Component.LEFT_ALIGNMENT);
		pane.addHyperlinkListener(event ->
		{
			if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;

			String action = event.getDescription();
			if ("text-view".equals(action) && !textMode)
			{
				toggleTextView();
			}
			else if ("structured-editor".equals(action) && textMode)
			{
				toggleTextView();
			}
		});
		pane.setSize(TEXT_HELP_WIDTH, 1);
		Dimension preferred = pane.getPreferredSize();
		pane.setPreferredSize(new Dimension(TEXT_HELP_WIDTH, preferred.height));
		pane.setMinimumSize(new Dimension(TEXT_HELP_WIDTH, preferred.height));
		pane.setMaximumSize(new Dimension(TEXT_HELP_WIDTH, preferred.height));
		return pane;
	}

	private String helpEmphasis(String text)
	{
		return "<b><font color='" + toHex(ColorScheme.BRAND_ORANGE) + "'>" + text + "</font></b>";
	}

	private String helpLink(String action, String text)
	{
		return "<a href='" + BaPanelUi.escapeHtml(action) + "'><b><font color='" + toHex(ColorScheme.BRAND_ORANGE) + "'>"
				+ BaPanelUi.escapeHtml(text) + "</font></b></a>";
	}

	private String helpCode(String text)
	{
		return "<font color='" + toHex(BaPanelUi.ACTION_CONTROL_TEXT_COLOR) + "'><b>" + BaPanelUi.escapeHtml(text) + "</b></font>";
	}

	private JPanel createOptionsRow()
	{
		BaPanelUi.styleCombo(overstockCombo, 160, CONTROL_HEIGHT);
		BaPanelUi.styleTextInput(customOverstockInstructions, CUSTOM_OVERSTOCK_FIELD_WIDTH, CONTROL_HEIGHT);
		customOverstockInstructions.setToolTipText("Custom overstock instructions");
		alchHorn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		alchHorn.setForeground(ColorScheme.TEXT_COLOR);
		alchHorn.setBorder(BorderFactory.createLineBorder(BaPanelUi.ACTION_CONTROL_BORDER_COLOR));
		alchHorn.setBorderPainted(true);
		alchHorn.setFocusPainted(false);
		alchHorn.setMargin(new Insets(0, 0, 0, 0));
		BaPanelUi.fixedSize(alchHorn, CONTROL_HEIGHT, CONTROL_HEIGHT);
		BaPanelUi.styleTextInput(expectedWaveEnd, EXPECTED_WAVE_END_FIELD_WIDTH, CONTROL_HEIGHT);
		expectedWaveEnd.setHorizontalAlignment(SwingConstants.CENTER);
		expectedWaveEnd.setToolTipText("Expected wave end time");
		updateCustomOverstockVisibility();

		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, EDITOR_WIDTH, CONTROL_HEIGHT);
		row.add(label("Overstock", true));
		row.add(Box.createHorizontalStrut(8));
		row.add(overstockCombo);
		row.add(Box.createHorizontalStrut(6));
		row.add(customOverstockInstructions);
		row.add(Box.createHorizontalStrut(22));
		row.add(label("Alch Horn", true));
		row.add(Box.createHorizontalStrut(4));
		row.add(alchHorn);
		row.add(Box.createHorizontalStrut(22));
		row.add(label("Expected Wave End", true));
		row.add(Box.createHorizontalStrut(4));
		row.add(expectedWaveEnd);
		row.add(Box.createHorizontalGlue());
		return row;
	}

	private void updateCustomOverstockVisibility()
	{
		boolean custom = overstockCombo.getSelectedItem() == HealerCodeOverstock.CUSTOM;
		customOverstockInstructions.setVisible(custom);
		customOverstockInstructions.setEnabled(custom);
		if (customOverstockInstructions.getParent() instanceof JPanel)
		{
			JPanel parent = (JPanel) customOverstockInstructions.getParent();
			parent.revalidate();
			parent.repaint();
		}
		revalidate();
		repaint();
	}

	private void rebuildCodeGrid()
	{
		Map<Integer, String> expectedTimeTextByOrder = currentExpectedTimeTextByOrder();
		expectedTimeFields.clear();
		activeFoodField = null;
		activeAdvancedField = null;
		activeAfterField = null;
		activeBeforeField = null;
		activeExactField = null;
		codeGridPanel.removeAll();

		int healerCount = getHealerCount();
		int gridWidth = gridWidth(healerCount);
		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(header, gridWidth + DELETE_BUTTON_WIDTH + 6, HEADER_HEIGHT);

		JPanel headerGrid = tableRowPanel(gridWidth, HEADER_HEIGHT);
		headerGrid.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JLabel headerSpacer = new JLabel("");
		BaPanelUi.fixedSize(headerSpacer, ROW_LABEL_WIDTH, HEADER_HEIGHT);
		headerGrid.add(headerSpacer);

		List<String> labels = BaWaveInfo.getLabels(selectedWave, BaOverviewNpcType.HEALER);
		for (int healerOrder = 1; healerOrder <= healerCount; healerOrder++)
		{
			headerGrid.add(Box.createHorizontalStrut(6));
			headerGrid.add(healerHeader(labelForHealer(labels, healerOrder), healerOrder, expectedTimeTextByOrder.get(healerOrder)));
		}
		header.add(headerGrid, BorderLayout.CENTER);
		header.add(Box.createHorizontalStrut(DELETE_BUTTON_WIDTH), BorderLayout.EAST);
		codeGridPanel.add(header);
		codeGridPanel.add(Box.createVerticalStrut(6));

		for (int callIndex = 0; callIndex < visibleCallCount; callIndex++)
		{
			JPanel row = new JPanel(new BorderLayout(6, 0));
			row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			BaPanelUi.fixedSize(row, gridWidth + DELETE_BUTTON_WIDTH + 6, CELL_HEIGHT);

			JPanel codeRow = tableRowPanel(gridWidth, CELL_HEIGHT);
			codeRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			codeRow.add(rowLabel("Call " + (callIndex + 1), CELL_HEIGHT));

			for (int healerOrder = 1; healerOrder <= healerCount; healerOrder++)
			{
				codeRow.add(Box.createHorizontalStrut(6));
				codeRow.add(createFoodCell(callIndex, healerOrder));
			}

			row.add(codeRow, BorderLayout.CENTER);
			row.add(createDeleteCallButton(callIndex), BorderLayout.EAST);
			codeGridPanel.add(row);
			codeGridPanel.add(Box.createVerticalStrut(6));
		}

		if (visibleCallCount < HealerCodeFormatter.CALL_COUNT)
		{
			JButton addCallButton = new JButton(BaIcons.plusIcon());
			addCallButton.setToolTipText("Add call");
			SwingUtil.removeButtonDecorations(addCallButton);
			BaPanelUi.fixedSize(addCallButton, DELETE_BUTTON_WIDTH, CONTROL_HEIGHT);
			addCallButton.addActionListener(event -> addCallRow());

			JPanel addRow = new JPanel(new BorderLayout());
			addRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			BaPanelUi.fixedSize(addRow, EDITOR_WIDTH, CONTROL_HEIGHT);
			addRow.add(addCallButton, BorderLayout.WEST);
			codeGridPanel.add(addRow);
		}
		codeGridPanel.revalidate();
		codeGridPanel.repaint();
	}

	private JPanel tableRowPanel(int width, int height)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(panel, width, height);
		return panel;
	}

	private Map<Integer, String> currentExpectedTimeTextByOrder()
	{
		Map<Integer, String> values = new HashMap<>();
		for (Map.Entry<Integer, JTextField> entry : expectedTimeFields.entrySet())
		{
			JTextField field = entry.getValue();
			if (field != null)
			{
				values.put(entry.getKey(), field.getText());
			}
		}
		return values;
	}

	private void addCallRow()
	{
		if (visibleCallCount >= HealerCodeFormatter.CALL_COUNT) return;

		commitActiveCell();
		visibleCallCount++;
		rebuildCodeGrid();
		markDirty();
	}

	private void deleteCallRow(int callIndex)
	{
		int minimumCallCount = defaultVisibleCallCountForWave(selectedWave);
		if (callIndex < minimumCallCount || visibleCallCount <= minimumCallCount) return;

		commitActiveCell();
		int healerCount = getHealerCount();
		for (int row = callIndex; row < visibleCallCount - 1; row++)
		{
			for (int healerOrder = 1; healerOrder <= healerCount; healerOrder++)
			{
				CellKey target = new CellKey(row, healerOrder);
				CellKey source = new CellKey(row + 1, healerOrder);
				HealerInstruction instruction = cellInstructions.get(source);
				if (instruction == null)
				{
					cellInstructions.remove(target);
				}
				else
				{
					cellInstructions.put(target, instruction.copy());
				}

				CellTimingOption option = cellTimingOptions.get(source);
				if (option == null)
				{
					cellTimingOptions.remove(target);
				}
				else
				{
					cellTimingOptions.put(target, option);
				}
			}
		}

		for (int healerOrder = 1; healerOrder <= healerCount; healerOrder++)
		{
			CellKey key = new CellKey(visibleCallCount - 1, healerOrder);
			cellInstructions.remove(key);
			cellTimingOptions.remove(key);
		}

		visibleCallCount--;
		rebuildCodeGrid();
		markDirty();
	}

	private Component createFoodCell(int callIndex, int healerOrder)
	{
		CellKey key = new CellKey(callIndex, healerOrder);
		JPanel cell = new JPanel(new BorderLayout(3, 0));
		cell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(cell, CELL_WIDTH, CELL_HEIGHT);
		cell.add(key.equals(activeCell) ? createActiveFoodCell(key) : createInactiveFoodCell(key), BorderLayout.CENTER);
		return cell;
	}

	private Component createDeleteCallButton(int callIndex)
	{
		if (callIndex < defaultVisibleCallCountForWave(selectedWave)) return Box.createHorizontalStrut(DELETE_BUTTON_WIDTH);

		JButton button = new JButton(BaIcons.trashIcon());
		button.setToolTipText("Delete call row");
		SwingUtil.removeButtonDecorations(button);
		BaPanelUi.fixedSize(button, DELETE_BUTTON_WIDTH, CONTROL_HEIGHT);
		button.addActionListener(event -> deleteCallRow(callIndex));
		return button;
	}

	private Component createInactiveFoodCell(CellKey key)
	{
		JLabel value = new JLabel(HealerCodeFormatter.formatInstruction(instructionForCell(key)));
		value.setForeground(ColorScheme.TEXT_COLOR);
		value.setHorizontalAlignment(SwingConstants.CENTER);
		value.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		value.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				activateCell(key);
			}
		});
		BaPanelUi.fixedSize(value, CELL_WIDTH - 12, CELL_HEIGHT);
		return value;
	}

	private Component createActiveFoodCell(CellKey key)
	{
		HealerInstruction instruction = instructionForCell(key);
		activeTimingOption = cellTimingOptions.getOrDefault(key, CellTimingOption.fromInstruction(instruction));

		activeFoodField = null;
		activeAdvancedField = null;
		if (activeTimingOption == CellTimingOption.ADVANCED)
		{
			activeAdvancedField = new JTextField(advancedText(instruction));
			BaPanelUi.styleTextInput(activeAdvancedField, ADVANCED_FIELD_WIDTH, CONTROL_HEIGHT);
			activeAdvancedField.setHorizontalAlignment(SwingConstants.CENTER);
			BaPanelUi.addTextChangeListener(activeAdvancedField, this::markDirty);
			addCommitOnFocusLost(activeAdvancedField, key);
		}
		else if (activeTimingOption != CellTimingOption.SPAM)
		{
			activeFoodField = new JSpinner(new SpinnerNumberModel(instruction == null ? 0 : instruction.getTargetFoodCount(), 0, 99, 1));
			BaPanelUi.styleSpinner(activeFoodField, FOOD_FIELD_WIDTH, CONTROL_HEIGHT);
			activeFoodField.addChangeListener(event -> markDirty());
			addCommitOnFocusLost(activeFoodField, key);
			if (activeFoodField.getEditor() instanceof JSpinner.DefaultEditor)
			{
				addCommitOnFocusLost(((JSpinner.DefaultEditor) activeFoodField.getEditor()).getTextField(), key);
			}
		}

		JButton clockButton = new JButton(BaIcons.clockIcon());
		clockButton.setToolTipText("Food timing");
		SwingUtil.removeButtonDecorations(clockButton);
		BaPanelUi.fixedSize(clockButton, 20, CONTROL_HEIGHT);
		clockButton.addActionListener(event ->
		{
			JPopupMenu menu = timingMenu(key);
			timingMenuOpen = true;
			menu.show(clockButton, 0, clockButton.getHeight());
		});

		JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
		topRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		if (activeTimingOption == CellTimingOption.ADVANCED)
		{
			topRow.add(activeAdvancedField);
		}
		else if (activeTimingOption == CellTimingOption.SPAM)
		{
			JLabel spamLabel = new JLabel("X");
			spamLabel.setForeground(ColorScheme.TEXT_COLOR);
			spamLabel.setHorizontalAlignment(SwingConstants.CENTER);
			BaPanelUi.fixedSize(spamLabel, 42, CONTROL_HEIGHT);
			topRow.add(spamLabel);
		}
		else
		{
			topRow.add(activeFoodField);
		}
		topRow.add(clockButton);

		activeEditorPanel = new JPanel();
		activeEditorPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(activeEditorPanel, CELL_WIDTH - 12, CELL_HEIGHT);
		Component timingEditor = createActiveTimingEditor(key, instruction);
		if (timingEditor != null)
		{
			activeEditorPanel.setLayout(new BoxLayout(activeEditorPanel, BoxLayout.Y_AXIS));
			activeEditorPanel.add(topRow);
			activeEditorPanel.add(Box.createVerticalStrut(4));
			activeEditorPanel.add(timingEditor);
			return activeEditorPanel;
		}

		activeEditorPanel.setLayout(new GridBagLayout());
		activeEditorPanel.add(topRow);
		return activeEditorPanel;
	}

	private Component createActiveTimingEditor(CellKey key, HealerInstruction instruction)
	{
		activeAfterField = null;
		activeBeforeField = null;
		activeExactField = null;

		if (activeTimingOption == CellTimingOption.NONE
				|| activeTimingOption == CellTimingOption.ADVANCED
				|| activeTimingOption == CellTimingOption.SPAM)
		{
			return null;
		}

		JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		if (activeTimingOption == CellTimingOption.BEFORE || activeTimingOption == CellTimingOption.WINDOW)
		{
			activeBeforeField = timingField(
					key,
					instruction == null ? null : instruction.getBeforeSeconds(),
					activeTimingOption == CellTimingOption.WINDOW ? TIME_PAIR_FIELD_WIDTH : TIME_FIELD_WIDTH,
					activeTimingOption == CellTimingOption.WINDOW ? "" : BEFORE_PLACEHOLDER,
					BEFORE_TOOLTIP
			);
			row.add(activeBeforeField);
		}
		if (activeTimingOption == CellTimingOption.AT_OR_AFTER || activeTimingOption == CellTimingOption.WINDOW)
		{
			activeAfterField = timingField(
					key,
					instruction == null ? null : instruction.getAfterSeconds(),
					activeTimingOption == CellTimingOption.WINDOW ? TIME_PAIR_FIELD_WIDTH : TIME_FIELD_WIDTH,
					activeTimingOption == CellTimingOption.WINDOW ? "" : AFTER_PLACEHOLDER,
					AFTER_TOOLTIP
			);
			row.add(activeAfterField);
		}
		if (activeTimingOption == CellTimingOption.EXACT)
		{
			activeExactField = timingField(
					key,
					instruction == null ? null : instruction.getExactSeconds(),
					TIME_FIELD_WIDTH,
					EXACT_PLACEHOLDER,
					EXACT_TOOLTIP
			);
			row.add(activeExactField);
		}
		return row;
	}

	private JTextField timingField(CellKey key, Integer seconds, int width, String placeholder, String tooltip)
	{
		JTextField field = new PlaceholderTextField(seconds == null ? "" : String.valueOf(seconds), placeholder);
		BaPanelUi.styleTextInput(field, width, CONTROL_HEIGHT);
		field.setHorizontalAlignment(SwingConstants.CENTER);
		field.setToolTipText(tooltip);
		BaPanelUi.addTextChangeListener(field, this::markDirty);
		addCommitOnFocusLost(field, key);
		return field;
	}

	private JPopupMenu timingMenu(CellKey key)
	{
		JPopupMenu menu = new JPopupMenu();
		CellTimingOption selected = activeCell != null && activeCell.equals(key)
				? activeTimingOption
				: cellTimingOptions.getOrDefault(key, CellTimingOption.fromInstruction(instructionForCell(key)));
		for (CellTimingOption option : CellTimingOption.values())
		{
			JMenuItem item = new JCheckBoxMenuItem(option.label, option == selected);
			item.setToolTipText(option.tooltip);
			if (option == selected)
			{
				item.setForeground(new Color(126, 212, 132));
			}
			item.addActionListener(event -> applyTimingOption(key, option));
			menu.add(item);
		}
		menu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent event)
			{
				timingMenuOpen = true;
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent event)
			{
				timingMenuOpen = false;
				if (timingOptionApplied)
				{
					timingOptionApplied = false;
					return;
				}
				CellKey key = activeCell;
				SwingUtilities.invokeLater(() -> commitActiveCellIfFocusOutside(key));
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent event)
			{
				timingMenuOpen = false;
			}
		});
		return menu;
	}

	private void applyTimingOption(CellKey key, CellTimingOption option)
	{
		HealerInstruction current = activeCell != null && activeCell.equals(key)
				? activeInstructionFromEditor()
				: instructionForCell(key).copy();
		Integer reusableSeconds = current.getTimingSeconds();
		HealerInstruction updated;
		if (option == CellTimingOption.ADVANCED)
		{
			updated = current.copy();
			updated.setAdvanced(true);
			updated.setRaw(advancedText(current));
		}
		else if (option == CellTimingOption.SPAM)
		{
			updated = new HealerInstruction(0, null, null, null, "X");
		}
		else if (option == CellTimingOption.AT_OR_AFTER)
		{
			updated = new HealerInstruction(current.getTargetFoodCount(), reusableSeconds, null, null, null);
		}
		else if (option == CellTimingOption.BEFORE)
		{
			updated = new HealerInstruction(current.getTargetFoodCount(), null, reusableSeconds, null, null);
		}
		else if (option == CellTimingOption.EXACT)
		{
			updated = new HealerInstruction(current.getTargetFoodCount(), null, null, reusableSeconds, null);
		}
		else if (option == CellTimingOption.WINDOW)
		{
			updated = new HealerInstruction(current.getTargetFoodCount(), current.getAfterSeconds(), current.getBeforeSeconds(), null, null);
		}
		else
		{
			updated = new HealerInstruction(current.getTargetFoodCount(), null, null, null, null);
		}

		cellInstructions.put(key, updated);
		cellTimingOptions.put(key, option);
		activeCell = key;
		activeTimingOption = option;
		timingOptionApplied = true;
		ignoreFocusCommitUntilMillis = System.currentTimeMillis() + 300;
		rebuildCodeGrid();
		SwingUtilities.invokeLater(() -> requestActiveEditorFocus());
		markDirty();
	}

	private void addCommitOnFocusLost(Component component, CellKey key)
	{
		component.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				if (timingMenuOpen) return;

				SwingUtilities.invokeLater(() ->
						commitActiveCellIfFocusOutside(key));
			}
		});
	}

	private void commitActiveCellIfFocusOutside(CellKey key)
	{
		if (System.currentTimeMillis() < ignoreFocusCommitUntilMillis)
		{
			SwingUtilities.invokeLater(() -> requestActiveEditorFocus());
			return;
		}

		if (activeCell != null
				&& activeCell.equals(key)
				&& activeEditorPanel != null
				&& !timingMenuOpen
				&& SwingUtilities.findFocusOwner(activeEditorPanel) == null)
		{
			commitActiveCell();
			rebuildCodeGrid();
		}
	}

	private void requestActiveEditorFocus()
	{
		if (activeFoodField != null && activeFoodField.getEditor() instanceof JSpinner.DefaultEditor)
		{
			JTextField textField = ((JSpinner.DefaultEditor) activeFoodField.getEditor()).getTextField();
			SwingUtilities.invokeLater(() ->
			{
				textField.requestFocusInWindow();
				SwingUtilities.invokeLater(textField::selectAll);
			});
			return;
		}

		Component field = activeAdvancedField != null ? activeAdvancedField
				: activeBeforeField != null ? activeBeforeField
				: activeAfterField != null ? activeAfterField : activeExactField;
		if (field != null) field.requestFocusInWindow();
	}

	private void activateCell(CellKey key)
	{
		if (key.equals(activeCell)) return;

		commitActiveCell();
		activeCell = key;
		rebuildCodeGrid();
		requestActiveEditorFocus();
	}

	private void commitActiveCell()
	{
		if (activeCell == null) return;

		cellInstructions.put(activeCell, activeInstructionFromEditor());
		cellTimingOptions.put(activeCell, activeTimingOption);
		activeCell = null;
		activeEditorPanel = null;
		activeFoodField = null;
		activeAdvancedField = null;
		activeAfterField = null;
		activeBeforeField = null;
		activeExactField = null;
	}

	private HealerInstruction activeInstructionFromEditor()
	{
		if (activeTimingOption == CellTimingOption.ADVANCED)
		{
			String text = activeAdvancedField == null ? "" : activeAdvancedField.getText();
			HealerInstruction instruction = HealerCodeParser.parseInstruction(text);
			instruction.setAdvanced(true);
			instruction.setRaw(text.trim());
			return instruction;
		}

		if (activeTimingOption == CellTimingOption.SPAM) return new HealerInstruction(0, null, null, null, "X");

		int food = activeFoodCount();
		Integer after = activeTimingOption == CellTimingOption.AT_OR_AFTER || activeTimingOption == CellTimingOption.WINDOW
				? readOptionalSeconds(activeAfterField)
				: null;
		Integer before = activeTimingOption == CellTimingOption.BEFORE || activeTimingOption == CellTimingOption.WINDOW
				? readOptionalSeconds(activeBeforeField)
				: null;
		Integer exact = activeTimingOption == CellTimingOption.EXACT ? readOptionalSeconds(activeExactField) : null;
		HealerInstruction existing = cellInstructions.get(activeCell);
		String raw = food == 0
				&& after == null
				&& before == null
				&& exact == null
				&& existing != null
				&& existing.getRaw() != null
				&& !existing.getRaw().isBlank()
				? existing.getRaw()
				: null;
		return new HealerInstruction(food, after, before, exact, raw);
	}

	private int activeFoodCount()
	{
		if (activeFoodField == null) return 0;

		try
		{
			activeFoodField.commitEdit();
		}
		catch (ParseException ignored)
		{
			// Keep the last valid spinner value when the editor text is invalid.
		}
		return (Integer) activeFoodField.getValue();
	}

	private HealerInstruction instructionForCell(CellKey key)
	{
		HealerInstruction instruction = cellInstructions.get(key);
		return instruction == null ? new HealerInstruction() : instruction;
	}

	private static String advancedText(HealerInstruction instruction)
	{
		return HealerCodeFormatter.formatInstruction(instruction);
	}

	private JPanel createTextAreas()
	{
		JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(panel, EDITOR_WIDTH, 132);
		panel.add(textAreaBlock("Restocking Instructions", restockingInstructions));
		panel.add(textAreaBlock("Additional Notes", additionalNotes));
		return panel;
	}

	private JPanel textAreaBlock(String title, JTextArea area)
	{
		BaPanelUi.styleTextArea(area, 4);
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label(title, true));
		panel.add(Box.createVerticalStrut(6));
		JScrollPane scrollPane = BaPanelUi.wrapTextArea(area, (EDITOR_WIDTH - 10) / 2, 100);
		panel.add(scrollPane);
		return panel;
	}

	private JPanel createActionRow()
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, EDITOR_WIDTH, CONTROL_HEIGHT);
		JPanel transferButtons = new JPanel();
		transferButtons.setLayout(new BoxLayout(transferButtons, BoxLayout.X_AXIS));
		transferButtons.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		transferButtons.add(BaPanelUi.action("Import", this::importCurrentWaveFromClipboard, 84, CONTROL_HEIGHT));
		transferButtons.add(Box.createHorizontalStrut(6));
		transferButtons.add(BaPanelUi.action("Export", this::exportCurrentWaveToClipboard, 84, CONTROL_HEIGHT));
		JButton save = BaPanelUi.action("Save Wave Code", this::saveCode, 180, CONTROL_HEIGHT);
		row.add(transferButtons, BorderLayout.WEST);
		row.add(save, BorderLayout.EAST);
		return row;
	}

	private void changeWave()
	{
		if (refreshing) return;

		Integer wave = (Integer) waveCombo.getSelectedItem();
		if (wave == null || wave == selectedWave) return;

		if (!confirmDiscard(this))
		{
			selectWaveComboValue();
			return;
		}

		selectedWave = requireWave(wave);
		refreshCodeCombo(null);
		clearDraft();
	}

	private void refreshCodeCombo(String selectedId)
	{
		refreshing = true;
		try
		{
			codeCombo.removeAllItems();
			codeCombo.addItem(new BaPanelUi.ComboOption(null, "-- New --"));
			for (WaveCode code : codeManager.getWaveCodesForWave(selectedWave))
			{
				codeCombo.addItem(new BaPanelUi.ComboOption(code.getId(), HealerCodeManager.waveCodeDisplayName(code)));
			}
			selectCodeComboValue(selectedId);
			selectWaveComboValue();
		}
		finally
		{
			refreshing = false;
		}
	}

	private void loadCode(String id)
	{
		WaveCode code = codeManager.findWaveCode(id);
		if (code == null)
		{
			clearDraft();
			return;
		}

		refreshing = true;
		try
		{
			selectedCodeId = code.getId();
			selectedWave = code.getWave();
			selectWaveComboValue();
			codeTextSource.setText(code.getSourceText());
			textSnapshot = codeTextSource.getText();
			textChangedAfterSnapshot = false;
			loadAdvancedFields(code);
			updateModeView();
			updateControls();
			dirty = false;
		}
		finally
		{
			refreshing = false;
		}
	}

	private void clearDraft()
	{
		refreshing = true;
		try
		{
			selectedCodeId = null;
			codeName.setText("");
			codeTextSource.setText("");
			visibleCallCount = defaultVisibleCallCountForWave(selectedWave);
			activeCell = null;
			activeEditorPanel = null;
			cellInstructions.clear();
			cellTimingOptions.clear();
			textSnapshot = null;
			textChangedAfterSnapshot = false;
			overstockCombo.setSelectedItem(HealerCodeOverstock.REGULAR);
			customOverstockInstructions.setText("");
			updateCustomOverstockVisibility();
			alchHorn.setSelected(false);
			expectedWaveEnd.setText("");
			restockingInstructions.setText("");
			additionalNotes.setText("");
			rebuildCodeGrid();
			selectCodeComboValue(null);
			updateModeView();
			updateControls();
			dirty = false;
		}
		finally
		{
			refreshing = false;
		}
	}

	boolean selectCodeForWave(int wave, String codeId, Component parent)
	{
		if (!confirmDiscard(parent)) return false;

		selectedWave = requireWave(wave);
		refreshCodeCombo(codeId);
		if (codeId == null)
		{
			clearDraft();
		}
		else
		{
			loadCode(codeId);
		}
		return true;
	}

	private void loadAdvancedFields(WaveCode code)
	{
		codeName.setText(code.getName() == null ? "" : code.getName());
		overstockCombo.setSelectedItem(code.getOverstock());
		customOverstockInstructions.setText(code.getCustomOverstockInstructions() == null ? "" : code.getCustomOverstockInstructions());
		updateCustomOverstockVisibility();
		alchHorn.setSelected(code.isAlchHorn());
		expectedWaveEnd.setText(code.getExpectedWaveEndSeconds() == null ? "" : String.valueOf(code.getExpectedWaveEndSeconds()));
		restockingInstructions.setText(code.getRestockingInstructions() == null ? "" : code.getRestockingInstructions());
		additionalNotes.setText(code.getAdditionalNotes() == null ? "" : code.getAdditionalNotes());
		visibleCallCount = visibleCallCountForCode(code);
		activeCell = null;
		activeEditorPanel = null;
		cellInstructions.clear();
		cellTimingOptions.clear();
		loadCalls(code);
		rebuildCodeGrid();
		loadExpectedTimes(code);
	}

	private void toggleTextView()
	{
		refreshing = true;
		try
		{
			if (textMode)
			{
				if (textChangedAfterSnapshot)
				{
					WaveCode parsed = HealerCodeParser.parseWaveCode(
							selectedCodeId,
							codeName.getText().trim(),
							selectedWave,
							false,
							codeTextSource.getText()
					);
					loadAdvancedFields(parsed);
				}
				textMode = false;
			}
			else
			{
				WaveCode code = advancedDraft(false);
				codeTextSource.setText(code.getSourceText());
				textSnapshot = codeTextSource.getText();
				textChangedAfterSnapshot = false;
				textMode = true;
			}
			updateModeView();
		}
		finally
		{
			refreshing = false;
		}
	}

	private void updateModeView()
	{
		modeLayout.show(modePanel, textMode ? TEXT_CARD : ADVANCED_CARD);
		textViewButton.setText(textMode ? "Structured Editor" : "Text View");
		textViewButton.setToolTipText("View editable code text");
		refreshHelpPanel();
		modePanel.revalidate();
		modePanel.repaint();
	}

	private void loadCalls(WaveCode code)
	{
		for (CallCode call : code.getCalls())
		{
			if (call == null || call.getCallIndex() >= HealerCodeFormatter.CALL_COUNT) continue;

			for (int index = 0; index < call.getHealerInstructions().size(); index++)
			{
				setInstruction(call.getCallIndex(), index + 1, call.getHealerInstructions().get(index));
			}
		}
	}

	private void setInstruction(int callIndex, int healerOrder, HealerInstruction instruction)
	{
		CellKey key = new CellKey(callIndex, healerOrder);
		if (instruction == null)
		{
			cellInstructions.remove(key);
			cellTimingOptions.remove(key);
			return;
		}

		HealerInstruction copy = instruction.copy();
		cellInstructions.put(key, copy);
		cellTimingOptions.put(key, CellTimingOption.fromInstruction(copy));
	}

	private void loadExpectedTimes(WaveCode code)
	{
		for (Map.Entry<Integer, JTextField> entry : expectedTimeFields.entrySet())
		{
			Integer seconds = code.getExpectedTimeSeconds(entry.getKey());
			entry.getValue().setText(seconds == null ? "" : String.valueOf(seconds));
		}
	}

	private void saveCode()
	{
		WaveCode draft = draftCode();
		if (draft == null)
		{
			Toolkit.getDefaultToolkit().beep();
			codeName.requestFocusInWindow();
			return;
		}

		WaveCode saved = codeManager.saveWaveCode(selectedCodeId, draft);
		selectedCodeId = saved.getId();
		codeManager.setActiveWaveCodeId(saved.getWave(), saved.getId());
		refreshCodeCombo(selectedCodeId);
		loadCode(selectedCodeId);
		dirty = false;
		codesChanged.run();
		SwingUtilities.getWindowAncestor(this).dispose();
	}

	private void exportCurrentWaveToClipboard()
	{
		String codeId = selectedCodeId == null ? codeManager.getActiveWaveCodeId(selectedWave) : selectedCodeId;
		if (codeId == null)
		{
			JOptionPane.showMessageDialog(this, "No healer code is selected for Wave " + selectedWave + ".", "Export Wave", JOptionPane.ERROR_MESSAGE);
			return;
		}

		HealerCodeExportResult result = codeManager.exportWaveCode(codeId);
		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "Wave " + selectedWave + " could not be exported.", "Export Wave", JOptionPane.ERROR_MESSAGE);
			return;
		}

		BaClipboard.copyText(result.getJson());
		BaTransferDialog.show(this, "Export Wave", "Exported " + result.getTypedName() + ".", "Export", result.getSummaryLines());
	}

	private void importCurrentWaveFromClipboard()
	{
		if (!confirmDiscard(this)) return;

		String json = BaClipboard.readText(this, "Import Wave");
		if (json == null) return;

		HealerCodeExportResult result = codeManager.importHealerCodeJson(json, selectedWave);
		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "Clipboard text could not be imported for Wave " + selectedWave + ".", "Import Wave", JOptionPane.ERROR_MESSAGE);
			return;
		}

		codesChanged.run();
		refreshCodeCombo(result.getId());
		loadCode(result.getId());
		BaTransferDialog.show(this, "Import Wave", "Imported and selected " + result.getTypedName() + ".", "Import", result.getSummaryLines());
	}

	private WaveCode draftCode()
	{
		String name = codeName.getText().trim();
		if (name.isEmpty()) return null;

		if (textMode) return HealerCodeParser.parseWaveCode(selectedCodeId, name, selectedWave, false, codeTextSource.getText());

		return advancedDraft(true);
	}

	private WaveCode advancedDraft(boolean requireName)
	{
		commitActiveCell();
		String name = codeName.getText().trim();
		if (requireName && name.isEmpty()) return null;

		WaveCode code = new WaveCode(selectedCodeId, name, selectedWave, false, collectCalls());
		HealerCodeOverstock overstock = (HealerCodeOverstock) overstockCombo.getSelectedItem();
		code.setOverstock(overstock);
		code.setCustomOverstockInstructions(overstock == HealerCodeOverstock.CUSTOM ? customOverstockInstructions.getText() : null);
		code.setAlchHorn(alchHorn.isSelected());
		code.setExpectedWaveEndSeconds(readOptionalSeconds(expectedWaveEnd));
		code.setRestockingInstructions(restockingInstructions.getText());
		code.setAdditionalNotes(additionalNotes.getText());
		for (Map.Entry<Integer, JTextField> entry : expectedTimeFields.entrySet())
		{
			code.setExpectedTimeSeconds(entry.getKey(), readOptionalSeconds(entry.getValue()));
		}
		return code;
	}

	private List<CallCode> collectCalls()
	{
		List<CallCode> calls = new ArrayList<>();
		for (int callIndex = 0; callIndex < HealerCodeFormatter.CALL_COUNT; callIndex++)
		{
			List<HealerInstruction> instructions = new ArrayList<>();
			if (callIndex < visibleCallCount)
			{
				for (int healerOrder = 1; healerOrder <= getHealerCount(); healerOrder++)
				{
					instructions.add(instructionForCell(new CellKey(callIndex, healerOrder)).copy());
				}
			}
			calls.add(new CallCode(callIndex, instructions, null));
		}
		return calls;
	}

	private Integer readOptionalSeconds(JTextField field)
	{
		if (field == null) return null;

		String value = field.getText().trim();
		if (value.isEmpty()) return null;
		try
		{
			return Math.max(0, Integer.parseInt(value));
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private void runCodeAction()
	{
		if (codeManager.isModifiedBuiltInWaveCode(selectedCodeId))
		{
			resetCode();
			return;
		}

		deleteCode();
	}

	private void deleteCode()
	{
		WaveCode selected = codeManager.findWaveCode(selectedCodeId);
		if (selected == null || selected.isBuiltIn())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this wave code?", "Delete Wave Code", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION) return;

		codeManager.deleteUserWaveCode(selectedCodeId);
		refreshCodeCombo(null);
		clearDraft();
		codesChanged.run();
	}

	private void resetCode()
	{
		if (!codeManager.isModifiedBuiltInWaveCode(selectedCodeId))
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Reset this built-in wave code to its default?", "Reset Wave Code", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION) return;

		String resetCodeId = selectedCodeId;
		codeManager.resetBuiltInWaveCode(resetCodeId);
		refreshCodeCombo(resetCodeId);
		loadCode(resetCodeId);
		codesChanged.run();
	}

	boolean confirmDiscard(Component parent)
	{
		if (!dirty) return true;

		int result = JOptionPane.showConfirmDialog(
				parent,
				"Discard unsaved healer code changes?",
				"Unsaved Changes",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE
		);
		return result == JOptionPane.YES_OPTION;
	}

	private void updateControls()
	{
		WaveCode selected = codeManager.findWaveCode(selectedCodeId);
		if (selected == null)
		{
			codeActionButton.setVisible(false);
		}
		else if (codeManager.isModifiedBuiltInWaveCode(selectedCodeId))
		{
			codeActionButton.setText("Reset to Default");
			codeActionButton.setIcon(BaIcons.resetIcon());
			codeActionButton.setToolTipText("Reset this built-in wave code to its default");
			codeActionButton.setVisible(true);
		}
		else if (!selected.isBuiltIn())
		{
			codeActionButton.setText("Delete");
			codeActionButton.setIcon(BaIcons.trashIcon());
			codeActionButton.setToolTipText("Delete selected wave code");
			codeActionButton.setVisible(true);
		}
		else
		{
			codeActionButton.setVisible(false);
		}

		codeActionButton.revalidate();
		codeActionButton.repaint();
	}

	private int getHealerCount()
	{
		return BaWaveInfo.getExpectedCount(selectedWave, BaOverviewNpcType.HEALER);
	}

	private static int gridWidth(int healerCount)
	{
		return ROW_LABEL_WIDTH + healerCount * CELL_WIDTH + healerCount * 6;
	}

	private static int requireWave(int wave)
	{
		if (!BaWaveInfo.isValidWave(wave))
		{
			throw new IllegalArgumentException("Healer code editor requires wave 1-10.");
		}
		return wave;
	}

	private void selectWaveComboValue()
	{
		refreshing = true;
		try
		{
			waveCombo.setSelectedItem(selectedWave);
		}
		finally
		{
			refreshing = false;
		}
	}

	private void selectCodeComboValue(String id)
	{
		refreshing = true;
		try
		{
			BaPanelUi.selectComboValue(codeCombo, id);
		}
		finally
		{
			refreshing = false;
		}
	}

	private void markDirty()
	{
		if (!refreshing)
		{
			dirty = true;
			if (textMode && textSnapshot != null && !textSnapshot.equals(codeTextSource.getText()))
			{
				textChangedAfterSnapshot = true;
			}
		}
	}

	private JLabel rowLabel(String text, int height)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.TEXT_COLOR);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		BaPanelUi.fixedSize(label, ROW_LABEL_WIDTH, height);
		return label;
	}

	private Component healerHeader(String text, int healerOrder, String expectedTimeText)
	{
		JLabel icon = new JLabel(HEALER_ICON);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		BaPanelUi.fixedSize(icon, CELL_WIDTH, 32);

		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		label.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel labelRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		labelRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(labelRow, CELL_WIDTH, CONTROL_HEIGHT);
		int initialHealerCount = BaWaveInfo.getInitialCount(selectedWave, BaOverviewNpcType.HEALER);
		if (healerOrder > initialHealerCount)
		{
			JTextField spawnField = new JTextField(expectedTimeText == null ? "" : expectedTimeText);
			BaPanelUi.styleTextInput(spawnField, RESERVE_SPAWN_FIELD_WIDTH, CONTROL_HEIGHT);
			spawnField.setHorizontalAlignment(SwingConstants.CENTER);
			spawnField.setToolTipText("Expected reserve spawn time");
			BaPanelUi.addTextChangeListener(spawnField, this::markDirty);
			expectedTimeFields.put(healerOrder, spawnField);
			labelRow.add(spawnField);
		}
		labelRow.add(label);

		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(panel, CELL_WIDTH, HEADER_HEIGHT);
		panel.add(labelRow);
		panel.add(Box.createVerticalStrut(2));
		panel.add(icon);
		return panel;
	}

	private JLabel label(String text, boolean bold)
	{
		JLabel label = BaPanelUi.label(text, bold);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static String toHex(Color color)
	{
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static String labelForHealer(List<String> labels, int healerOrder)
	{
		if (healerOrder <= 0 || healerOrder > labels.size()) return String.valueOf(healerOrder);
		return labels.get(healerOrder - 1).replace("s", "");
	}

	private static int visibleCallCountForCode(WaveCode code)
	{
		return Math.max(defaultVisibleCallCountForWave(code.getWave()), meaningfulCallCount(code));
	}

	private static int meaningfulCallCount(WaveCode code)
	{
		int count = 0;
		for (CallCode call : code.getCalls())
		{
			if (call != null && !call.getHealerInstructions().isEmpty())
			{
				count = Math.max(count, call.getCallIndex() + 1);
			}
		}
		return Math.min(HealerCodeFormatter.CALL_COUNT, count);
	}

	private static int defaultVisibleCallCountForWave(int wave)
	{
		if (wave <= 3) return 1;

		if (wave <= 6) return 2;

		return 3;
	}

	private static ImageIcon loadHealerIcon()
	{
		Image image = new ImageIcon(HealerCodeEditor.class.getResource("/com/bahealerorder/penance_healer.png")).getImage();
		return new ImageIcon(image.getScaledInstance(32, 32, Image.SCALE_SMOOTH));
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@EqualsAndHashCode
	private static class CellKey
	{
		private final int callIndex;
		private final int healerOrder;
	}

	@RequiredArgsConstructor
	private enum CellTimingOption
	{
		NONE("No timing", "No timing rule for this code"),
		AT_OR_AFTER("After", AFTER_TOOLTIP),
		BEFORE("Before", BEFORE_TOOLTIP),
		EXACT("Exactly", EXACT_TOOLTIP),
		WINDOW("First Before / Last After", "The first food for this code must be used before the first time and the last food at or after the second time"),
		SPAM("Spam", "Spam this healer until it dies"),
		ADVANCED("Advanced", "Use custom healer code text for this cell");

		private final String label;
		private final String tooltip;

		private static CellTimingOption fromInstruction(HealerInstruction instruction)
		{
			if (isSpamInstruction(instruction)) return SPAM;
			if (instruction != null && instruction.isAdvanced()) return ADVANCED;
			if (instruction == null || !instruction.hasTiming()) return NONE;
			if (instruction.getAfterSeconds() != null && instruction.getBeforeSeconds() != null) return WINDOW;
			if (instruction.getExactSeconds() != null) return EXACT;
			if (instruction.getAfterSeconds() != null) return AT_OR_AFTER;
			return BEFORE;
		}

		private static boolean isSpamInstruction(HealerInstruction instruction)
		{
			String raw = instruction == null ? null : instruction.getRaw();
			if (instruction == null || instruction.hasTarget() || raw == null) return false;

			String text = raw.trim().toLowerCase();
			return "x".equals(text) || text.contains("spam") || text.contains("yolo");
		}
	}

	private static class PlaceholderTextField extends JTextField
	{
		private final String placeholder;

		private PlaceholderTextField(String text, String placeholder)
		{
			super(text);
			this.placeholder = placeholder;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			if (placeholder == null || placeholder.isEmpty() || !getText().isEmpty()) return;

			Graphics2D graphics2D = (Graphics2D) graphics.create();
			graphics2D.setColor(new Color(140, 140, 140));
			FontMetrics metrics = graphics2D.getFontMetrics();
			Insets insets = getInsets();
			int x = insets.left;
			if (getHorizontalAlignment() == SwingConstants.CENTER)
			{
				x = Math.max(insets.left, (getWidth() - metrics.stringWidth(placeholder)) / 2);
			}
			int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
			graphics2D.drawString(placeholder, x, y);
			graphics2D.dispose();
		}
	}
}
