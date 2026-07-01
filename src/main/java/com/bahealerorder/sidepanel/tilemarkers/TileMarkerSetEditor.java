package com.bahealerorder.sidepanel.tilemarkers;

import com.bahealerorder.common.BaClipboard;
import com.bahealerorder.common.BaIcons;
import com.bahealerorder.sidepanel.BaPanelUi;
import com.bahealerorder.common.TileMarkerStyle;
import com.bahealerorder.tilemarkers.GeneralTileMarkerStrategyManager;
import com.bahealerorder.tilemarkers.TileMarker;
import com.bahealerorder.tilemarkers.TileMarkerExportResult;
import com.bahealerorder.tilemarkers.TileMarkerExportType;
import com.bahealerorder.tilemarkers.TileMarkerMapLayout;
import com.bahealerorder.tilemarkers.TileMarkerMapMode;
import com.bahealerorder.tilemarkers.TileMarkerRoleContext;
import com.bahealerorder.tilemarkers.TileMarkerSet;
import com.bahealerorder.tilemarkers.TileMarkerTile;
import com.bahealerorder.tilemarkers.TileMarkerWaveMap;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLayeredPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.SwingUtil;

public class TileMarkerSetEditor extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int EDITOR_WIDTH = 1080;
	private static final int EDITOR_BASE_HEIGHT = 780;
	private static final int SIDE_WIDTH = 250;
	private static final int APPLY_INSTRUCTION_HEIGHT = 40;
	private static final int MAP_VIEWPORT_WIDTH = 800;
	private static final int MAP_VIEWPORT_HEIGHT = 660;
	private static final int BODY_GAP = 10;
	private static final int FLOATING_CONTROL_RIGHT_INSET = 18;
	private static final int MAP_MIN_TILE_SIZE = 7;
	private static final int MAP_MAX_TILE_SIZE = 48;
	private static final int MAP_DEFAULT_TILE_SIZE = 30;
	private static final int FULL_ARENA_DEFAULT_ZOOM_STEPS_OUT = 10;
	private static final int EAST_SIDE_DEFAULT_ZOOM_STEPS_IN = 1;
	private static final int SECTION_GAP = 14;
	private static final int APPLY_SECTION_GAP = 102;
	private static final int MARKER_LABEL_WIDTH = 82;
	private static final int TRASH_BUTTON_WIDTH = 24;
	private static final int IMPORT_EXPORT_BUTTON_WIDTH = 140;
	private static final Color BUILT_IN_SET_COLOR = new Color(120, 120, 120);

	private final GeneralTileMarkerStrategyManager strategyManager;
	private final ColorPickerManager colorPickerManager;
	private final Runnable setsChanged;
	private final StrategyEditorOpener strategyEditorOpener;
	private final JComboBox<SetOption> setCombo = new JComboBox<>();
	private final JComboBox<MarkerOption> markerCombo = new JComboBox<>();
	private final JComboBox<WaveOption> applyWaveCombo = new JComboBox<>();
	private final JComboBox<TileMarkerRoleContext> applyRoleCombo = new JComboBox<>();
	private final JTextField markerName = new JTextField();
	private final JTextField markerLabel = new JTextField();
	private final JButton markerColorButton = new JButton();
	private final JLabel legendToggleLink = new JLabel();
	private final JLabel applyInstructionLabel = new JLabel();
	private final JButton exportMarkersButton;
	private final JButton saveMarkersButton = new JButton("Save Markers");
	private final JButton applyMarkersButton;
	private final JButton deleteMarkerButton = new JButton();
	private final JButton deleteSetButton = new JButton(BaIcons.trashIcon());
	private final JLabel markerSelectionSummary = new JLabel();
	private final JLabel markerOpacityValue = new JLabel();
	private final JSlider markerOpacity = new JSlider(
			TileMarkerStyle.MIN_OPACITY_PERCENT,
			TileMarkerStyle.MAX_OPACITY_PERCENT,
			TileMarker.DEFAULT_OPACITY_PERCENT
	);
	private final JSpinner markerBorderWidth = new JSpinner(new SpinnerNumberModel(
			(double) TileMarker.DEFAULT_BORDER_WIDTH,
			(double) TileMarkerStyle.MIN_BORDER_WIDTH,
			(double) TileMarkerStyle.MAX_BORDER_WIDTH,
			0.5
	));
	private final JSlider mapZoom = new JSlider(MAP_MIN_TILE_SIZE, MAP_MAX_TILE_SIZE, MAP_DEFAULT_TILE_SIZE);
	private final TileMarkerMapPanel mapPanel;
	private final JScrollPane mapScrollPane;
	private final JPanel markerTextPanel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final JPanel markerControlsPanel;
	private JPanel helpPanel;
	private JPanel legendPanel;
	private Component legendMarkerGap;
	private final ButtonGroup mapModeButtonGroup = new ButtonGroup();
	private final ButtonGroup waveMapButtonGroup = new ButtonGroup();

	private List<TileMarker> markers = new ArrayList<>();
	private final Set<String> selectedMarkerIds = new LinkedHashSet<>();
	private TileMarkerMapMode mapMode;
	private TileMarkerWaveMap waveMap;
	private String selectedSetId;
	private String currentSetName = "";
	private Color markerColor = TileMarkerStyle.DEFAULT_MARKER_COLOR;
	private boolean legendVisible;
	private boolean refreshing;
	private boolean selectedSetBuiltIn;
	private boolean dirty;

	public TileMarkerSetEditor(
			GeneralTileMarkerStrategyManager strategyManager,
			ColorPickerManager colorPickerManager,
			Runnable setsChanged,
			StrategyEditorOpener strategyEditorOpener)
	{
		this.strategyManager = strategyManager;
		this.colorPickerManager = colorPickerManager;
		this.setsChanged = setsChanged;
		this.strategyEditorOpener = strategyEditorOpener;
		this.exportMarkersButton = BaPanelUi.action("Export Markers", this::exportSetToClipboard, IMPORT_EXPORT_BUTTON_WIDTH, CONTROL_HEIGHT);
		this.applyMarkersButton = BaPanelUi.action("Apply", this::applyMarkersToStrategy, SIDE_WIDTH, CONTROL_HEIGHT);
		this.mapMode = strategyManager.getLastMapMode();
		this.waveMap = strategyManager.getLastWaveMap();
		this.legendVisible = strategyManager.isMarkerEditorLegendVisible();

		mapZoom.setValue(defaultTileSize(mapMode));
		mapPanel = new TileMarkerMapPanel(
				() -> waveMap.getLayout(),
				() -> mapMode,
				() -> markers,
				() -> selectedMarkerIds,
				mapZoom::getValue,
				this::addOrSelectMarkerAt
		);
		mapScrollPane = new JScrollPane(mapPanel);
		markerColor = TileMarkerStyle.parseColor(strategyManager.getLastMarkerColor(), TileMarkerStyle.DEFAULT_MARKER_COLOR);
		markerOpacity.setValue(strategyManager.getLastMarkerOpacityPercent());
		markerOpacity.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		markerOpacity.setFocusable(false);
		markerOpacity.setMajorTickSpacing(25);
		markerOpacity.setMinorTickSpacing(5);
		markerBorderWidth.setValue((double) strategyManager.getLastMarkerBorderWidth());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 10));
		markerControlsPanel = createMarkerControlsPanel();
		setPreferredSize(new Dimension(EDITOR_WIDTH, EDITOR_BASE_HEIGHT));

		add(createHeader(), BorderLayout.NORTH);
		add(createBody(), BorderLayout.CENTER);

		BaPanelUi.addTextChangeListener(markerName, this::updateSelectedMarkerFromFields);
		BaPanelUi.addTextChangeListener(markerLabel, this::updateSelectedMarkerFromFields);
		ChangeListener markerStyleListener = event -> updateSelectedMarkerStyleFromFields();
		markerOpacity.addChangeListener(markerStyleListener);
		markerBorderWidth.addChangeListener(markerStyleListener);
		refreshSetCombo(null);
		clearDraft();
		resizeMap();
	}

	private JPanel createHeader()
	{
		JLabel label = label("Click any tile on the map to add a marker.");
		label.setHorizontalAlignment(SwingConstants.LEFT);

		JPanel actionRow = new JPanel(new BorderLayout(6, 0));
		actionRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(actionRow, IMPORT_EXPORT_BUTTON_WIDTH * 2 + 6, CONTROL_HEIGHT);
		actionRow.add(BaPanelUi.action("Import Markers", this::importSetFromClipboard, IMPORT_EXPORT_BUTTON_WIDTH, CONTROL_HEIGHT), BorderLayout.WEST);
		actionRow.add(exportMarkersButton, BorderLayout.EAST);

		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, MAP_VIEWPORT_WIDTH, CONTROL_HEIGHT);
		row.add(label, BorderLayout.CENTER);
		row.add(actionRow, BorderLayout.EAST);

		JPanel header = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		header.add(row);
		return header;
	}

	private JPanel createHelpPanel()
	{
		helpPanel = BaPanelUi.verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		helpPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
		helpPanel.setMaximumSize(new Dimension(SIDE_WIDTH, Integer.MAX_VALUE));
		helpPanel.add(helpLine(
				"Begin by editing existing " + helpEmphasis("tile markers")
						+ " or creating your own."
		));
		helpPanel.add(Box.createVerticalStrut(8));
		helpPanel.add(helpLine(
				helpEmphasis("Apply")
						+ " those markers to a chosen wave and role, along with optional notes, to create a "
						+ helpEmphasis("strategy") + "."
		));
		helpPanel.add(Box.createVerticalStrut(8));
		helpPanel.add(helpLine(
				"If desired, save your chosen strategies as a "
						+ helpEmphasis("preset")
						+ " to easily swap between them."
		));
		helpPanel.add(Box.createVerticalStrut(8));
		helpPanel.add(helpLine(
				helpEmphasis("Export")
						+ " tile markers, strategies, or presets to share them with others."
		));
		return helpPanel;
	}

	private JLabel helpLine(String text)
	{
		JLabel label = label("<html><div width='" + (SIDE_WIDTH - 20) + "'>" + text + "</div></html>");
		label.setMaximumSize(new Dimension(SIDE_WIDTH - 20, Integer.MAX_VALUE));
		return label;
	}

	private String helpEmphasis(String text)
	{
		return "<b><font color='" + TileMarkerStyle.toHex(ColorScheme.BRAND_ORANGE) + "'>" + text + "</font></b>";
	}

	private JPanel createBody()
	{
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.X_AXIS));
		body.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		body.add(createMapArea());
		body.add(Box.createHorizontalStrut(BODY_GAP));
		body.add(createSidePanel());
		body.add(Box.createHorizontalGlue());
		return body;
	}

	private JPanel createMapArea()
	{
		JPanel panel = new JPanel(new BorderLayout(0, 8));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(panel, MAP_VIEWPORT_WIDTH, MAP_VIEWPORT_HEIGHT + CONTROL_HEIGHT + 8);

		mapScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		mapScrollPane.setPreferredSize(new Dimension(MAP_VIEWPORT_WIDTH, MAP_VIEWPORT_HEIGHT));
		mapScrollPane.setWheelScrollingEnabled(false);
		mapScrollPane.addMouseWheelListener(this::zoomFromMouseWheel);
		mapPanel.addMouseWheelListener(this::zoomFromMouseWheel);
		panel.add(createLayeredMap(), BorderLayout.CENTER);
		panel.add(createZoomControls(), BorderLayout.SOUTH);
		return panel;
	}

	private JLayeredPane createLayeredMap()
	{
		JPanel mapModeControls = createMapModeControls();
		JPanel markerSetControls = createFloatingSetControls();
		JLayeredPane layeredPane = new JLayeredPane()
		{
			@Override
			public void doLayout()
			{
				mapScrollPane.setBounds(0, 0, getWidth(), getHeight());

				Dimension modeSize = mapModeControls.getPreferredSize();
				mapModeControls.setBounds(8, 8, modeSize.width, modeSize.height);

				Dimension setSize = markerSetControls.getPreferredSize();
				markerSetControls.setBounds(getWidth() - setSize.width - FLOATING_CONTROL_RIGHT_INSET, 8, setSize.width, setSize.height);
			}
		};
		BaPanelUi.fixedSize(layeredPane, MAP_VIEWPORT_WIDTH, MAP_VIEWPORT_HEIGHT);
		layeredPane.add(mapScrollPane, JLayeredPane.DEFAULT_LAYER);
		layeredPane.add(mapModeControls, JLayeredPane.PALETTE_LAYER);
		layeredPane.add(markerSetControls, JLayeredPane.PALETTE_LAYER);
		return layeredPane;
	}

	private JPanel createFloatingSetControls()
	{
		BaPanelUi.styleCombo(setCombo, 230, CONTROL_HEIGHT);
		setCombo.setRenderer(new MarkerSetRenderer());
		setCombo.addActionListener(event ->
		{
			if (refreshing)
			{
				return;
			}

			SetOption item = (SetOption) setCombo.getSelectedItem();
			if (!confirmDiscard(this))
			{
				refreshing = true;
				try
				{
					selectSetComboValue(selectedSetId);
				}
				finally
				{
					refreshing = false;
				}
				return;
			}
			loadSet(item == null ? null : item.id);
		});

		deleteSetButton.setToolTipText("Delete selected marker set");
		SwingUtil.removeButtonDecorations(deleteSetButton);
		BaPanelUi.fixedSize(deleteSetButton, TRASH_BUTTON_WIDTH, CONTROL_HEIGHT);
		deleteSetButton.addActionListener(event -> deleteSet());

		legendToggleLink.setForeground(ColorScheme.BRAND_ORANGE);
		legendToggleLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		legendToggleLink.setHorizontalAlignment(SwingConstants.RIGHT);
		legendToggleLink.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				toggleLegend();
			}
		});
		updateLegendButtonText();

		JPanel selectorRow = new JPanel(new BorderLayout(6, 0));
		selectorRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(selectorRow, 264, CONTROL_HEIGHT);
		selectorRow.add(setCombo, BorderLayout.CENTER);
		selectorRow.add(deleteSetButton, BorderLayout.EAST);

		JButton newButton = new JButton("New");
		newButton.addActionListener(event -> beginNewSet());
		BaPanelUi.styleActionButton(newButton, 72, 20);

		JPanel linkRow = new JPanel(new BorderLayout(6, 0));
		linkRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(linkRow, 264, 20);
		linkRow.add(newButton, BorderLayout.WEST);
		linkRow.add(legendToggleLink, BorderLayout.EAST);

		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(4, 4, 4, 4));
		BaPanelUi.fixedSize(panel, 272, CONTROL_HEIGHT + 31);
		panel.add(selectorRow);
		panel.add(Box.createVerticalStrut(3));
		panel.add(linkRow);
		return panel;
	}

	private JPanel createMapModeControls()
	{
		JPanel panel = new JPanel(new GridLayout(2, 2, 4, 4));
		panel.setBorder(new EmptyBorder(4, 4, 4, 4));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(panel, 312, 60);

		for (TileMarkerWaveMap map : TileMarkerWaveMap.values())
		{
			JToggleButton button = new JToggleButton(map.getDisplayName());
			BaPanelUi.styleActionButton(button);
			button.setToolTipText(map.getDisplayName());
			button.addActionListener(event -> setWaveMap(map));
			waveMapButtonGroup.add(button);
			panel.add(button);
			if (map == waveMap)
			{
				button.setSelected(true);
			}
		}

		for (TileMarkerMapMode mode : TileMarkerMapMode.values())
		{
			JToggleButton button = new JToggleButton(mode.getDisplayName());
			BaPanelUi.styleActionButton(button);
			button.setToolTipText(mode.getDisplayName());
			button.addActionListener(event -> setMapMode(mode));
			mapModeButtonGroup.add(button);
			panel.add(button);
			if (mode == mapMode)
			{
				button.setSelected(true);
			}
		}

		return panel;
	}

	private JPanel createZoomControls()
	{
		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Drag Slider or Scroll to Zoom"), BorderLayout.WEST);

		mapZoom.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		mapZoom.setFocusable(false);
		mapZoom.setMajorTickSpacing(10);
		mapZoom.setMinorTickSpacing(1);
		mapZoom.addChangeListener(event -> resizeMap());
		panel.add(mapZoom, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createSidePanel()
	{
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(wrapper, SIDE_WIDTH, MAP_VIEWPORT_HEIGHT);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(panel, SIDE_WIDTH, MAP_VIEWPORT_HEIGHT);

		JPanel top = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		legendPanel = createLegendPanel();
		legendPanel.setVisible(legendVisible);
		top.add(legendPanel);
		legendMarkerGap = Box.createVerticalStrut(SECTION_GAP * 3);
		top.add(legendMarkerGap);
		top.add(markerControlsPanel);
		top.add(createHelpPanel());
		panel.add(top, BorderLayout.NORTH);

		wrapper.add(panel, BorderLayout.NORTH);
		return wrapper;
	}

	private JPanel createLegendPanel()
	{
		return TileMarkerLegendPanel.create(SIDE_WIDTH);
	}

	private JPanel createMarkerControlsPanel()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Markers", true));
		panel.add(Box.createVerticalStrut(3));
		BaPanelUi.styleCombo(markerCombo, SIDE_WIDTH, CONTROL_HEIGHT);
		markerCombo.addActionListener(event ->
		{
			if (refreshing)
			{
				return;
			}

			MarkerOption item = (MarkerOption) markerCombo.getSelectedItem();
			selectedMarkerIds.clear();
			if (item != null && item.id != null)
			{
				selectedMarkerIds.add(item.id);
			}
			loadSelectedMarker();
		});
		panel.add(markerCombo);
		panel.add(Box.createVerticalStrut(5));
		panel.add(createSelectionSummaryRow());
		panel.add(Box.createVerticalStrut(5));
		markerTextPanel.add(createTextFieldRow("Marker Name", markerName));
		markerTextPanel.add(Box.createVerticalStrut(5));
		markerTextPanel.add(createTextFieldRow("Tile Label", markerLabel));
		panel.add(markerTextPanel);
		panel.add(Box.createVerticalStrut(5));
		panel.add(createColorRow());
		panel.add(Box.createVerticalStrut(5));
		panel.add(createOpacityRow());
		panel.add(Box.createVerticalStrut(5));
		panel.add(createSpinnerRow("Border", markerBorderWidth));
		panel.add(Box.createVerticalStrut(8));
		saveMarkersButton.addActionListener(event -> saveSet());
		BaPanelUi.styleActionButton(saveMarkersButton, SIDE_WIDTH, CONTROL_HEIGHT);
		panel.add(saveMarkersButton);
		panel.add(Box.createVerticalStrut(APPLY_SECTION_GAP));
		panel.add(createApplyPanel());
		return panel;
	}

	private JPanel createApplyPanel()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(panel, SIDE_WIDTH, APPLY_INSTRUCTION_HEIGHT + CONTROL_HEIGHT * 2 + 18);

		applyInstructionLabel.setForeground(ColorScheme.TEXT_COLOR);
		applyInstructionLabel.setHorizontalAlignment(SwingConstants.LEFT);
		BaPanelUi.fixedSize(applyInstructionLabel, SIDE_WIDTH, APPLY_INSTRUCTION_HEIGHT);
		panel.add(applyInstructionLabel);
		panel.add(Box.createVerticalStrut(6));

		for (int wave = 1; wave <= 10; wave++)
		{
			applyWaveCombo.addItem(new WaveOption(wave));
		}

		for (TileMarkerRoleContext context : TileMarkerRoleContext.values())
		{
			applyRoleCombo.addItem(context);
		}
		int comboWidth = (SIDE_WIDTH - 6) / 2;
		BaPanelUi.styleCombo(applyWaveCombo, comboWidth, CONTROL_HEIGHT);
		BaPanelUi.styleCombo(applyRoleCombo, comboWidth, CONTROL_HEIGHT);

		JPanel selectorRow = new JPanel(new GridLayout(1, 2, 6, 0));
		selectorRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(selectorRow, SIDE_WIDTH, CONTROL_HEIGHT);
		selectorRow.add(applyWaveCombo);
		selectorRow.add(applyRoleCombo);
		panel.add(selectorRow);
		panel.add(Box.createVerticalStrut(6));

		panel.add(applyMarkersButton);
		return panel;
	}

	private JPanel createSelectionSummaryRow()
	{
		markerSelectionSummary.setForeground(ColorScheme.TEXT_COLOR);
		markerSelectionSummary.setFont(markerSelectionSummary.getFont().deriveFont(Font.BOLD));
		deleteMarkerButton.setIcon(BaIcons.trashIcon());
		deleteMarkerButton.setToolTipText("Delete selected marker");
		SwingUtil.removeButtonDecorations(deleteMarkerButton);
		deleteMarkerButton.addActionListener(event -> deleteSelectedMarker());
		BaPanelUi.fixedSize(deleteMarkerButton, TRASH_BUTTON_WIDTH, CONTROL_HEIGHT);

		JPanel selectionRow = new JPanel(new BorderLayout(6, 0));
		selectionRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(selectionRow, SIDE_WIDTH, CONTROL_HEIGHT);
		selectionRow.add(markerSelectionSummary, BorderLayout.CENTER);
		selectionRow.add(deleteMarkerButton, BorderLayout.EAST);
		return selectionRow;
	}

	private JPanel createTextFieldRow(String text, JTextField field)
	{
		JLabel rowLabel = label(text);
		BaPanelUi.fixedSize(rowLabel, MARKER_LABEL_WIDTH, CONTROL_HEIGHT);
		BaPanelUi.styleTextInput(field, SIDE_WIDTH - MARKER_LABEL_WIDTH - 6, CONTROL_HEIGHT);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);
		row.add(rowLabel, BorderLayout.WEST);
		row.add(field, BorderLayout.CENTER);
		return row;
	}

	private JPanel createColorRow()
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);

		JLabel rowLabel = label("Color");
		BaPanelUi.fixedSize(rowLabel, MARKER_LABEL_WIDTH, CONTROL_HEIGHT);
		markerColorButton.addActionListener(event -> chooseMarkerColor());
		BaPanelUi.fixedSize(markerColorButton, SIDE_WIDTH - MARKER_LABEL_WIDTH - 6, CONTROL_HEIGHT);
		row.add(rowLabel, BorderLayout.WEST);
		row.add(markerColorButton, BorderLayout.CENTER);
		updateMarkerColorButton();
		return row;
	}

	private JPanel createOpacityRow()
	{
		JLabel rowLabel = label("Opacity");
		BaPanelUi.fixedSize(rowLabel, MARKER_LABEL_WIDTH, CONTROL_HEIGHT);
		BaPanelUi.fixedSize(markerOpacity, SIDE_WIDTH - MARKER_LABEL_WIDTH - 44, CONTROL_HEIGHT);

		markerOpacityValue.setForeground(ColorScheme.TEXT_COLOR);
		markerOpacityValue.setHorizontalAlignment(SwingConstants.RIGHT);
		BaPanelUi.fixedSize(markerOpacityValue, 38, CONTROL_HEIGHT);

		JPanel sliderRow = new JPanel(new BorderLayout(6, 0));
		sliderRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sliderRow.add(markerOpacity, BorderLayout.CENTER);
		sliderRow.add(markerOpacityValue, BorderLayout.EAST);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);
		row.add(rowLabel, BorderLayout.WEST);
		row.add(sliderRow, BorderLayout.CENTER);
		return row;
	}

	private JPanel createSpinnerRow(String text, JSpinner spinner)
	{
		JLabel rowLabel = label(text);
		BaPanelUi.fixedSize(rowLabel, MARKER_LABEL_WIDTH, CONTROL_HEIGHT);
		BaPanelUi.styleSpinner(spinner, SIDE_WIDTH - MARKER_LABEL_WIDTH - 6, CONTROL_HEIGHT);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);
		row.add(rowLabel, BorderLayout.WEST);
		row.add(spinner, BorderLayout.CENTER);
		return row;
	}

	private void refreshSetCombo(String selectedId)
	{
		refreshing = true;
		try
		{
			setCombo.removeAllItems();
			setCombo.addItem(new SetOption(null, "Select a tile marker set...", false));
			for (TileMarkerSet set : strategyManager.getMarkerSets(waveMap))
			{
				setCombo.addItem(new SetOption(set.getId(), set.toString(), set.isBuiltIn()));
			}
			selectSetComboValue(selectedId);
		}
		finally
		{
			refreshing = false;
		}
	}

	private void loadSet(String id)
	{
		TileMarkerSet set = strategyManager.findMarkerSet(id);
		if (set == null)
		{
			clearDraft();
			return;
		}

		refreshing = true;
		try
		{
			selectedSetId = set.getId();
			selectedSetBuiltIn = set.isBuiltIn();
			currentSetName = set.getName() == null ? "" : set.getName();
			selectSetComboValue(selectedSetId);
			mapMode = set.getMapMode();
			waveMap = set.getWaveMap();
			markers = copyMarkers(set.getMarkers());
			selectedMarkerIds.clear();
			mapZoom.setValue(defaultTileSize(mapMode));
			selectMapModeButton();
			selectWaveMapButton();
			refreshMarkerControls();
			resizeMap();
			dirty = false;
			updateSetControls();
			SwingUtilities.invokeLater(mapPanel::scrollToTrap);
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
			selectedSetId = null;
			selectedSetBuiltIn = false;
			currentSetName = "";
			mapMode = strategyManager.getLastMapMode();
			waveMap = strategyManager.getLastWaveMap();
			markers = new ArrayList<>();
			selectedMarkerIds.clear();
			selectSetComboValue(null);
			selectMapModeButton();
			selectWaveMapButton();
			refreshMarkerControls();
			mapZoom.setValue(defaultTileSize(mapMode));
			resizeMap();
			updateSetControls();
			dirty = false;
		}
		finally
		{
			refreshing = false;
		}
	}

	private void saveSet()
	{
		String name = promptMarkerSetName();
		if (name == null)
		{
			return;
		}

		TileMarkerSet saved;
		if (selectedSetId != null
				&& !selectedSetBuiltIn
				&& strategyManager.updateMarkerSet(selectedSetId, name, mapMode, waveMap, copyMarkers(markers)))
		{
			saved = strategyManager.findMarkerSet(selectedSetId);
		}
		else
		{
			saved = strategyManager.createMarkerSet(name, mapMode, waveMap, copyMarkers(markers));
		}

		selectedSetId = saved == null ? selectedSetId : saved.getId();
		selectedSetBuiltIn = false;
		currentSetName = saved == null ? name : saved.getName();
		refreshSetCombo(selectedSetId);
		dirty = false;
		updateSetControls();
		if (setsChanged != null)
		{
			setsChanged.run();
		}
	}

	private String promptMarkerSetName()
	{
		JTextField nameField = new JTextField(currentSetName == null ? "" : currentSetName);
		BaPanelUi.fixedSize(nameField, 260, CONTROL_HEIGHT);

		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Marker Set Name"));
		panel.add(Box.createVerticalStrut(5));
		panel.add(nameField);

		int result = JOptionPane.showConfirmDialog(this, panel, "Save Markers", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION)
		{
			return null;
		}

		String name = nameField.getText() == null ? "" : nameField.getText().trim();
		if (name.isEmpty())
		{
			Toolkit.getDefaultToolkit().beep();
			return null;
		}
		return name;
	}

	private void deleteSet()
	{
		if (selectedSetId == null)
		{
			return;
		}

		if (selectedSetBuiltIn)
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		int result = JOptionPane.showConfirmDialog(this, "Delete this tile marker set?", "Delete Marker Set", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		strategyManager.deleteMarkerSet(selectedSetId);
		refreshSetCombo(null);
		clearDraft();
		if (setsChanged != null)
		{
			setsChanged.run();
		}
	}

	private void exportSetToClipboard()
	{
		if (markers.isEmpty())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		TileMarkerExportResult result = strategyManager.exportMarkerSetJson(currentDraftSet());
		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "Current tile marker set could not be exported.", "Export Marker Set", JOptionPane.ERROR_MESSAGE);
			return;
		}

		BaClipboard.copyText(result.getJson());
		TileMarkerTransferDialog.show(
				this,
				"Export Marker Set",
				"Exported tile set " + result.getName() + " with " + result.getMarkerCount() + " tile markers.",
				"Export",
				result.getSummaryLines()
		);
	}

	private void importSetFromClipboard()
	{
		String json = BaClipboard.readText(this, "Import Marker Set");
		if (json == null)
		{
			return;
		}

		TileMarkerExportResult result;
		try
		{
			result = strategyManager.importMarkerExportJson(json, null);
		}
		catch (RuntimeException ex)
		{
			result = null;
		}

		if (result == null)
		{
			JOptionPane.showMessageDialog(this, "Clipboard text could not be imported as a tile marker export.", "Import Marker Set", JOptionPane.ERROR_MESSAGE);
			return;
		}

		refreshSetCombo(null);
		if ((result.getType() == TileMarkerExportType.MARKER_SET
				|| result.getType() == TileMarkerExportType.MARKER_SET_COLLECTION)
				&& result.getId() != null)
		{
			loadSet(result.getId());
		}
		if (setsChanged != null)
		{
			setsChanged.run();
		}
		TileMarkerTransferDialog.show(this, "Import Marker Set", markerSetImportMessage(result), "Import", result.getSummaryLines());
	}

	private String markerSetImportMessage(TileMarkerExportResult result)
	{
		if (result.getType() == TileMarkerExportType.MARKER_SET)
		{
			return "Imported tile set " + result.getName() + " with " + result.getMarkerCount() + " tile markers.";
		}
		return "Imported " + result.getTypedName() + ".";
	}

	private TileMarkerSet currentDraftSet()
	{
		return new TileMarkerSet(
				selectedSetBuiltIn ? null : selectedSetId,
				currentSetName == null ? "" : currentSetName.trim(),
				mapMode,
				waveMap,
				copyMarkers(markers),
				false
		);
	}

	public boolean hasUnsavedChanges()
	{
		return dirty;
	}

	public boolean confirmDiscard(Component parent)
	{
		if (!hasUnsavedChanges())
		{
			return true;
		}

		int result = JOptionPane.showConfirmDialog(
				parent,
				"Discard unsaved tile marker set changes?",
				"Unsaved Changes",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE
		);
		return result == JOptionPane.YES_OPTION;
	}

	private void markDirty()
	{
		if (!refreshing)
		{
			dirty = true;
			updateSetControls();
		}
	}

	private void updateSetControls()
	{
		deleteSetButton.setEnabled(selectedSetId != null && !selectedSetBuiltIn);
		exportMarkersButton.setEnabled(!markers.isEmpty());
		saveMarkersButton.setEnabled(!markers.isEmpty());
		boolean canApply = savedMarkerSetReady();
		applyInstructionLabel.setText(applyInstructionText(canApply
				? "Select a wave and role to apply your markers to."
				: "Save your markers to apply them to a wave."));
		applyWaveCombo.setEnabled(canApply);
		applyRoleCombo.setEnabled(canApply);
		applyMarkersButton.setEnabled(canApply);
	}

	private String applyInstructionText(String text)
	{
		return "<html><div width='" + SIDE_WIDTH + "'>" + text + "</div></html>";
	}

	private void beginNewSet()
	{
		if (confirmDiscard(this))
		{
			clearDraft();
		}
	}

	private void toggleLegend()
	{
		legendVisible = !legendVisible;
		strategyManager.setMarkerEditorLegendVisible(legendVisible);
		updateLegendButtonText();
		updateRightPanelLayout();
	}

	private void applyMarkersToStrategy()
	{
		if (!savedMarkerSetReady())
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		WaveOption wave = (WaveOption) applyWaveCombo.getSelectedItem();
		TileMarkerRoleContext role = (TileMarkerRoleContext) applyRoleCombo.getSelectedItem();
		if (wave == null || role == null)
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		TileMarkerSet set = strategyManager.findMarkerSet(selectedSetId);
		if (set == null || set.getWaveMap() != TileMarkerWaveMap.fromWave(wave.wave))
		{
			JOptionPane.showMessageDialog(
					this,
					"This marker set does not match the selected wave map.",
					"Apply Markers",
					JOptionPane.ERROR_MESSAGE
			);
			return;
		}

		if (strategyEditorOpener != null)
		{
			strategyEditorOpener.open(wave.wave, role, selectedSetId);
		}
	}

	private boolean savedMarkerSetReady()
	{
		return selectedSetId != null && !dirty;
	}

	private void updateLegendButtonText()
	{
		legendToggleLink.setText(legendVisible ? "Hide Legend" : "Show Legend");
	}

	private void updateRightPanelLayout()
	{
		if (legendPanel != null)
		{
			legendPanel.setVisible(legendVisible);
		}
		boolean hasMarkers = !markers.isEmpty();
		if (helpPanel != null)
		{
			helpPanel.setVisible(!hasMarkers);
		}
		if (legendMarkerGap != null)
		{
			legendMarkerGap.setVisible(legendVisible);
		}

		revalidate();
		repaint();
	}

	private void setMapMode(TileMarkerMapMode mode)
	{
		if (refreshing || mode == null || mode == mapMode)
		{
			return;
		}

		mapMode = mode;
		strategyManager.setLastMapMode(mode);
		mapZoom.setValue(defaultTileSize(mode));
		resizeMap();
		refreshMarkerControls();
		markDirty();
		SwingUtilities.invokeLater(mapPanel::scrollToTrap);
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
		strategyManager.setLastWaveMap(map);
		refreshSetCombo(null);
		clearDraft();
		SwingUtilities.invokeLater(mapPanel::scrollToTrap);
	}

	private void addOrSelectMarkerAt(int mapX, int mapY)
	{
		TileMarkerMapLayout layout = waveMap.getLayout();
		if (!mapPanel.isSelectableMapTile(layout, mapX, mapY))
		{
			return;
		}

		TileMarker existing = findMarkerAt(layout, mapX, mapY);
		if (existing != null)
		{
			if (selectedMarkerIds.contains(existing.getId()) && isBlank(existing.getName()))
			{
				deleteMarker(existing);
				return;
			}

			if (selectedMarkerIds.contains(existing.getId()))
			{
				selectedMarkerIds.remove(existing.getId());
			}
			else
			{
				selectedMarkerIds.add(existing.getId());
			}
			refreshMarkerControls();
			return;
		}

		TileMarker marker = new TileMarker(
				userMarkerId(mapX, mapY),
				layout.toTile(mapX, mapY),
				"",
				"",
				toHex(markerColor),
				getMarkerOpacityPercent(),
				getMarkerBorderWidth()
		);
		markers.add(marker);
		persistCurrentMarkerStyle();
		selectedMarkerIds.clear();
		selectedMarkerIds.add(marker.getId());
		refreshMarkerControls();
		mapPanel.repaint();
		markDirty();
	}

	private void refreshMarkerControls()
	{
		refreshing = true;
		try
		{
			markerCombo.removeAllItems();
			markerCombo.addItem(new MarkerOption(null, "Select a marker..."));
			TileMarkerMapLayout layout = waveMap.getLayout();
			for (TileMarker marker : markers)
			{
				if (layout.contains(marker.getTile()))
				{
					markerCombo.addItem(new MarkerOption(marker.getId(), getMarkerDisplayText(marker)));
				}
			}
			selectMarkerComboValue(getSelectedMarkerId());
		}
		finally
		{
			refreshing = false;
		}

		loadSelectedMarker();
		revalidate();
		repaint();
	}

	private void loadSelectedMarker()
	{
		TileMarker marker = getFirstSelectedMarker();
		boolean singleSelection = getSelectedMarker() != null;
		boolean wasRefreshing = refreshing;
		refreshing = true;

		try
		{
			if (marker == null)
			{
				markerName.setText("");
				markerLabel.setText("");
			}
			else
			{
				markerName.setText(singleSelection && marker.getName() != null ? marker.getName() : "");
				markerLabel.setText(singleSelection && marker.getLabel() != null ? marker.getLabel() : "");
				markerColor = TileMarkerStyle.parseColor(marker.getColor(), TileMarkerStyle.DEFAULT_MARKER_COLOR);
				markerOpacity.setValue(TileMarkerStyle.clampOpacityPercent(marker.getOpacityPercentOrDefault()));
				markerBorderWidth.setValue((double) TileMarkerStyle.clampBorderWidth(marker.getBorderWidthOrDefault()));
			}
		}
		finally
		{
			refreshing = wasRefreshing;
		}

		updateMarkerDetailEnabled();
		updateMarkerColorButton();
		mapPanel.repaint();
	}

	private void updateMarkerDetailEnabled()
	{
		boolean hasMarkers = !markers.isEmpty();
		boolean hasSelection = !selectedMarkerIds.isEmpty();
		boolean singleSelection = getSelectedMarker() != null;
		markerControlsPanel.setVisible(hasMarkers);
		markerCombo.setEnabled(hasMarkers);
		markerTextPanel.setVisible(true);
		markerName.setEnabled(singleSelection);
		markerLabel.setEnabled(singleSelection);
		markerColorButton.setEnabled(hasSelection);
		markerOpacity.setEnabled(hasSelection);
		markerBorderWidth.setEnabled(hasSelection);
		deleteMarkerButton.setEnabled(hasSelection);
		markerSelectionSummary.setText(selectionSummaryText());
		markerOpacityValue.setText(getMarkerOpacityPercent() + "%");
		updateSetControls();
		updateRightPanelLayout();
	}

	private void updateSelectedMarkerFromFields()
	{
		if (refreshing)
		{
			return;
		}

		TileMarker marker = getSelectedMarker();
		if (marker == null)
		{
			return;
		}

		marker.setName(markerName.getText().trim());
		marker.setLabel(markerLabel.getText().trim());
		updateMarkerComboLabel(marker);
		mapPanel.repaint();
		markDirty();
	}

	private void updateSelectedMarkerStyleFromFields()
	{
		markerOpacityValue.setText(getMarkerOpacityPercent() + "%");
		if (refreshing)
		{
			return;
		}

		List<TileMarker> selectedMarkers = getSelectedMarkers();
		if (!selectedMarkers.isEmpty())
		{
			for (TileMarker marker : selectedMarkers)
			{
				marker.setColor(toHex(markerColor));
				marker.setOpacityPercent(getMarkerOpacityPercent());
				marker.setBorderWidth(getMarkerBorderWidth());
			}
			mapPanel.repaint();
			markDirty();
		}

		persistCurrentMarkerStyle();
	}

	private void chooseMarkerColor()
	{
		RuneliteColorPicker colorPicker = colorPickerManager.create(this, markerColor, "Marker Color", true);
		colorPicker.setLocationRelativeTo(this);
		colorPicker.setOnColorChange(this::setMarkerColor);
		colorPicker.setOnClose(this::setMarkerColor);
		colorPicker.setVisible(true);
	}

	private void setMarkerColor(Color color)
	{
		if (color == null)
		{
			return;
		}

		markerColor = color;
		updateMarkerColorButton();
		updateSelectedMarkerStyleFromFields();
	}

	private void deleteSelectedMarker()
	{
		List<TileMarker> selectedMarkers = getSelectedMarkers();
		if (!selectedMarkers.isEmpty())
		{
			for (TileMarker marker : selectedMarkers)
			{
				markers.remove(marker);
			}
			selectedMarkerIds.clear();
			refreshMarkerControls();
			mapPanel.repaint();
			markDirty();
		}
	}

	private void deleteMarker(TileMarker marker)
	{
		markers.remove(marker);
		selectedMarkerIds.remove(marker.getId());
		refreshMarkerControls();
		mapPanel.repaint();
		markDirty();
	}

	private TileMarker getSelectedMarker()
	{
		String selectedMarkerId = getSelectedMarkerId();
		if (selectedMarkerId == null)
		{
			return null;
		}

		for (TileMarker marker : markers)
		{
			if (selectedMarkerId.equals(marker.getId()))
			{
				return marker;
			}
		}

		return null;
	}

	private TileMarker getFirstSelectedMarker()
	{
		for (TileMarker marker : markers)
		{
			if (selectedMarkerIds.contains(marker.getId()))
			{
				return marker;
			}
		}

		return null;
	}

	private List<TileMarker> getSelectedMarkers()
	{
		List<TileMarker> selectedMarkers = new ArrayList<>();
		for (TileMarker marker : markers)
		{
			if (selectedMarkerIds.contains(marker.getId()))
			{
				selectedMarkers.add(marker);
			}
		}
		return selectedMarkers;
	}

	private TileMarker findMarkerAt(TileMarkerMapLayout layout, int mapX, int mapY)
	{
		for (TileMarker marker : markers)
		{
			if (layout.contains(marker.getTile())
					&& layout.toMapX(marker.getTile()) == mapX
					&& layout.toMapY(marker.getTile()) == mapY)
			{
				return marker;
			}
		}

		return null;
	}

	private String getSelectedMarkerId()
	{
		return selectedMarkerIds.size() == 1 ? selectedMarkerIds.iterator().next() : null;
	}

	private String selectionSummaryText()
	{
		int count = selectedMarkerIds.size();
		if (count == 0)
		{
			return "No markers selected";
		}
		return count == 1 ? "1 marker selected" : count + " markers selected";
	}

	private String getMarkerDisplayText(TileMarker marker)
	{
		String name = marker.getName() == null ? "" : marker.getName().trim();
		if (!name.isEmpty())
		{
			return name;
		}

		String label = marker.getLabel() == null ? "" : marker.getLabel().trim();
		return label.isEmpty() ? "Unnamed marker" : label;
	}

	private void updateMarkerComboLabel(TileMarker marker)
	{
		for (int i = 0; i < markerCombo.getItemCount(); i++)
		{
			MarkerOption item = markerCombo.getItemAt(i);
			if (marker.getId().equals(item.id))
			{
				item.label = getMarkerDisplayText(marker);
				markerCombo.repaint();
				return;
			}
		}
	}

	private void updateMarkerColorButton()
	{
		markerColorButton.setText(toHex(markerColor));
		markerColorButton.setBackground(markerColor);
		markerColorButton.setForeground(TileMarkerStyle.readableTextColor(markerColor));
	}

	private void persistCurrentMarkerStyle()
	{
		strategyManager.setLastMarkerStyle(toHex(markerColor), getMarkerOpacityPercent(), getMarkerBorderWidth());
	}

	private void zoomFromMouseWheel(MouseWheelEvent event)
	{
		int rotation = event.getWheelRotation();
		if (rotation != 0)
		{
			mapZoom.setValue(mapZoom.getValue() - rotation);
		}
		event.consume();
	}

	private void resizeMap()
	{
		Dimension mapSize = new Dimension(mapPanel.getMapWidthPixels(), mapPanel.getMapHeightPixels());
		mapPanel.setPreferredSize(mapSize);
		mapPanel.setMinimumSize(mapSize);
		mapScrollPane.getHorizontalScrollBar().setUnitIncrement(mapPanel.getTileSize());
		mapScrollPane.getVerticalScrollBar().setUnitIncrement(mapPanel.getTileSize());
		mapPanel.revalidate();
		mapPanel.repaint();
	}

	private void selectSetComboValue(String id)
	{
		for (int i = 0; i < setCombo.getItemCount(); i++)
		{
			SetOption item = setCombo.getItemAt(i);
			if ((id == null && item.id == null) || (id != null && id.equals(item.id)))
			{
				setCombo.setSelectedIndex(i);
				return;
			}
		}

		if (setCombo.getItemCount() > 0)
		{
			setCombo.setSelectedIndex(0);
		}
	}

	private void selectMarkerComboValue(String id)
	{
		for (int i = 0; i < markerCombo.getItemCount(); i++)
		{
			MarkerOption item = markerCombo.getItemAt(i);
			if ((id == null && item.id == null) || (id != null && id.equals(item.id)))
			{
				markerCombo.setSelectedIndex(i);
				return;
			}
		}

		if (markerCombo.getItemCount() > 0)
		{
			markerCombo.setSelectedIndex(0);
		}
	}

	private void selectMapModeButton()
	{
		for (java.util.Enumeration<javax.swing.AbstractButton> buttons = mapModeButtonGroup.getElements(); buttons.hasMoreElements(); )
		{
			javax.swing.AbstractButton button = buttons.nextElement();
			if (mapMode.getDisplayName().equals(button.getText()))
			{
				button.setSelected(true);
				return;
			}
		}
	}

	private void selectWaveMapButton()
	{
		for (java.util.Enumeration<javax.swing.AbstractButton> buttons = waveMapButtonGroup.getElements(); buttons.hasMoreElements(); )
		{
			javax.swing.AbstractButton button = buttons.nextElement();
			if (waveMap.getDisplayName().equals(button.getText()))
			{
				button.setSelected(true);
				return;
			}
		}
	}

	private int getMarkerOpacityPercent()
	{
		return TileMarkerStyle.clampOpacityPercent(markerOpacity.getValue());
	}

	private float getMarkerBorderWidth()
	{
		return TileMarkerStyle.clampBorderWidth(((Number) markerBorderWidth.getValue()).floatValue());
	}

	private String userMarkerId(int mapX, int mapY)
	{
		return "marker:" + waveMap.name() + ":" + mapMode.name() + ":" + mapX + ":" + mapY + ":" + System.nanoTime();
	}

	private static List<TileMarker> copyMarkers(List<TileMarker> source)
	{
		List<TileMarker> copies = new ArrayList<>();
		if (source == null)
		{
			return copies;
		}

		for (TileMarker marker : source)
		{
			if (marker != null)
			{
				copies.add(copyMarker(marker));
			}
		}
		return copies;
	}

	private static TileMarker copyMarker(TileMarker marker)
	{
		TileMarkerTile tile = marker.getTile();
		TileMarkerTile tileCopy = tile == null
				? null
				: new TileMarkerTile(tile.getRegionId(), tile.getRegionX(), tile.getRegionY(), tile.getZ());
		return new TileMarker(
				marker.getId(),
				tileCopy,
				marker.getName(),
				marker.getLabel(),
				marker.getColor(),
				marker.getOpacityPercent(),
				marker.getBorderWidth()
		);
	}

	private static int defaultTileSize(TileMarkerMapMode mapMode)
	{
		if (mapMode != null && mapMode.isFullArena())
		{
			return Math.max(MAP_MIN_TILE_SIZE, MAP_DEFAULT_TILE_SIZE - FULL_ARENA_DEFAULT_ZOOM_STEPS_OUT);
		}

		return Math.min(MAP_MAX_TILE_SIZE, MAP_DEFAULT_TILE_SIZE + EAST_SIDE_DEFAULT_ZOOM_STEPS_IN);
	}

	private String toHex(Color color)
	{
		return TileMarkerStyle.toHex(color);
	}

	private JLabel label(String text)
	{
		return label(text, false);
	}

	private JLabel label(String text, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.TEXT_COLOR);
		if (bold)
		{
			label.setFont(label.getFont().deriveFont(Font.BOLD));
		}
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}

	interface StrategyEditorOpener
	{
		void open(int wave, TileMarkerRoleContext context, String markerSetId);
	}

	private static class WaveOption
	{
		private final int wave;

		private WaveOption(int wave)
		{
			this.wave = wave;
		}

		@Override
		public String toString()
		{
			return "Wave " + wave;
		}
	}

	private static class SetOption
	{
		private final String id;
		private final String label;
		private final boolean builtIn;

		private SetOption(String id, String label, boolean builtIn)
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

	private static class MarkerSetRenderer extends DefaultListCellRenderer
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
			if (value instanceof SetOption && ((SetOption) value).builtIn)
			{
				component.setForeground(BUILT_IN_SET_COLOR);
			}
			return component;
		}
	}

	private static class MarkerOption
	{
		private final String id;
		private String label;

		private MarkerOption(String id, String label)
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
