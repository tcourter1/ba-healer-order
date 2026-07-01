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
import com.bahealerorder.tilemarkers.TileMarkerSet;
import com.bahealerorder.tilemarkers.TileMarkerTile;
import com.bahealerorder.tilemarkers.TileMarkerWaveMap;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLayeredPane;
import javax.swing.JLabel;
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
	private static final int MAP_VIEWPORT_WIDTH = 760;
	private static final int MAP_VIEWPORT_HEIGHT = 660;
	private static final int MAP_MIN_TILE_SIZE = 7;
	private static final int MAP_MAX_TILE_SIZE = 48;
	private static final int MAP_DEFAULT_TILE_SIZE = 30;
	private static final int FULL_ARENA_DEFAULT_ZOOM_STEPS_OUT = 10;
	private static final int SECTION_GAP = 14;
	private static final int MARKER_TO_DETAIL_GAP = (SECTION_GAP + 6) * 2;
	private static final int MARKER_LABEL_WIDTH = 82;
	private static final int TRASH_BUTTON_WIDTH = 24;
	private static final int HELP_BUTTON_WIDTH = 96;
	private static final int HELP_HEIGHT = 120;

	private final GeneralTileMarkerStrategyManager strategyManager;
	private final ColorPickerManager colorPickerManager;
	private final Runnable setsChanged;
	private final JComboBox<SetOption> setCombo = new JComboBox<>();
	private final JComboBox<MarkerOption> markerCombo = new JComboBox<>();
	private final JTextField setName = new PlaceholderTextField("Name this set of markers...");
	private final JTextField markerName = new JTextField();
	private final JTextField markerLabel = new JTextField();
	private final JButton markerColorButton = new JButton();
	private final JButton helpToggleButton = new JButton();
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
	private JPanel helpPanel;
	private final ButtonGroup mapModeButtonGroup = new ButtonGroup();
	private final ButtonGroup waveMapButtonGroup = new ButtonGroup();

	private List<TileMarker> markers = new ArrayList<>();
	private final Set<String> selectedMarkerIds = new LinkedHashSet<>();
	private TileMarkerMapMode mapMode;
	private TileMarkerWaveMap waveMap;
	private String selectedSetId;
	private Color markerColor = TileMarkerStyle.DEFAULT_MARKER_COLOR;
	private boolean helpVisible;
	private boolean refreshing;
	private boolean selectedSetBuiltIn;
	private boolean dirty;

	public TileMarkerSetEditor(
			GeneralTileMarkerStrategyManager strategyManager,
			ColorPickerManager colorPickerManager,
			Runnable setsChanged)
	{
		this.strategyManager = strategyManager;
		this.colorPickerManager = colorPickerManager;
		this.setsChanged = setsChanged;
		this.mapMode = strategyManager.getLastMapMode();
		this.waveMap = strategyManager.getLastWaveMap();
		this.helpVisible = strategyManager.isMarkerEditorHelpVisible();

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
		updatePreferredSize();

		add(createHeader(), BorderLayout.NORTH);
		add(createBody(), BorderLayout.CENTER);

		BaPanelUi.addTextChangeListener(setName, this::markDirty);
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

		helpToggleButton.addActionListener(event -> toggleHelp());
		BaPanelUi.fixedSize(helpToggleButton, HELP_BUTTON_WIDTH, CONTROL_HEIGHT);
		updateHelpButtonText();

		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, MAP_VIEWPORT_WIDTH, CONTROL_HEIGHT);
		row.add(label, BorderLayout.CENTER);
		row.add(helpToggleButton, BorderLayout.EAST);

		JPanel header = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		header.add(row);
		header.add(createHelpPanel());
		return header;
	}

	private JPanel createHelpPanel()
	{
		helpPanel = BaPanelUi.verticalPanel(ColorScheme.DARK_GRAY_COLOR);
		helpPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
		BaPanelUi.fixedSize(helpPanel, MAP_VIEWPORT_WIDTH, HELP_HEIGHT);
		helpPanel.setVisible(helpVisible);
		helpPanel.add(helpLine(
				"Begin by editing the existing " + helpEmphasis("tile markers")
						+ " to your liking, or creating your own.",
				20
		));
		helpPanel.add(Box.createVerticalStrut(5));
		helpPanel.add(helpLine(
				helpEmphasis("Wave strategies")
						+ " combine tile markers with notes that can be displayed on specific waves. "
						+ "Click the " + helpEmphasis("pencil")
						+ " next to a wave to configure them.",
				34
		));
		helpPanel.add(Box.createVerticalStrut(5));
		helpPanel.add(helpLine(
				"Finally, select which strategies should appear on each " + helpEmphasis("wave")
						+ ", and for each " + helpEmphasis("role") + ".",
				20
		));
		return helpPanel;
	}

	private JLabel helpLine(String text, int height)
	{
		JLabel label = label("<html><div width='" + (MAP_VIEWPORT_WIDTH - 20) + "'>" + text + "</div></html>");
		BaPanelUi.fixedSize(label, MAP_VIEWPORT_WIDTH - 20, height);
		return label;
	}

	private String helpEmphasis(String text)
	{
		return "<b><font color='" + TileMarkerStyle.toHex(ColorScheme.BRAND_ORANGE) + "'>" + text + "</font></b>";
	}

	private JPanel createBody()
	{
		JPanel body = new JPanel(new BorderLayout(10, 0));
		body.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		body.add(createMapArea(), BorderLayout.CENTER);
		body.add(createSidePanel(), BorderLayout.EAST);
		return body;
	}

	private JPanel createMapArea()
	{
		JPanel panel = new JPanel(new BorderLayout(0, 8));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

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
		JLayeredPane layeredPane = new JLayeredPane()
		{
			@Override
			public void doLayout()
			{
				mapScrollPane.setBounds(0, 0, getWidth(), getHeight());

				Dimension modeSize = mapModeControls.getPreferredSize();
				mapModeControls.setBounds(8, 8, modeSize.width, modeSize.height);
			}
		};
		BaPanelUi.fixedSize(layeredPane, MAP_VIEWPORT_WIDTH, MAP_VIEWPORT_HEIGHT);
		layeredPane.add(mapScrollPane, JLayeredPane.DEFAULT_LAYER);
		layeredPane.add(mapModeControls, JLayeredPane.PALETTE_LAYER);
		return layeredPane;
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
			button.setFocusable(false);
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
			button.setFocusable(false);
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
		wrapper.setPreferredSize(new Dimension(SIDE_WIDTH, MAP_VIEWPORT_HEIGHT));

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(panel, SIDE_WIDTH, MAP_VIEWPORT_HEIGHT);

		JPanel top = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		top.add(createLegendPanel());
		top.add(Box.createVerticalStrut(SECTION_GAP * 3));
		top.add(createMarkerSelectorPanel());
		top.add(Box.createVerticalStrut(MARKER_TO_DETAIL_GAP));
		top.add(createMarkerDetailPanel());
		panel.add(top, BorderLayout.NORTH);

		JPanel bottom = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		bottom.add(createSetPanel());
		panel.add(bottom, BorderLayout.SOUTH);

		wrapper.add(panel, BorderLayout.NORTH);
		return wrapper;
	}

	private JPanel createLegendPanel()
	{
		return TileMarkerLegendPanel.create(SIDE_WIDTH);
	}

	private JPanel createSetPanel()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Tile Marker Set", true));
		panel.add(Box.createVerticalStrut(5));

		BaPanelUi.styleCombo(setCombo, SIDE_WIDTH - TRASH_BUTTON_WIDTH - 6, CONTROL_HEIGHT);
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

		JPanel setRow = new JPanel(new BorderLayout(6, 0));
		setRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(setRow, SIDE_WIDTH, CONTROL_HEIGHT);
		setRow.add(setCombo, BorderLayout.CENTER);
		setRow.add(deleteSetButton, BorderLayout.EAST);
		panel.add(setRow);
		panel.add(Box.createVerticalStrut(5));

		BaPanelUi.fixedSize(setName, SIDE_WIDTH, CONTROL_HEIGHT);
		panel.add(setName);
		panel.add(Box.createVerticalStrut(6));

		JButton saveButton = new JButton("Save Marker Set");
		saveButton.addActionListener(event -> saveSet());
		BaPanelUi.fixedSize(saveButton, SIDE_WIDTH, CONTROL_HEIGHT);
		panel.add(saveButton);
		panel.add(Box.createVerticalStrut(5));

		JPanel importExportRow = BaPanelUi.horizontalActionRow(SIDE_WIDTH, CONTROL_HEIGHT);
		importExportRow.add(BaPanelUi.action("Import", this::importSetFromClipboard, SIDE_WIDTH, CONTROL_HEIGHT));
		importExportRow.add(BaPanelUi.action("Export", this::exportSetToClipboard, SIDE_WIDTH, CONTROL_HEIGHT));
		panel.add(importExportRow);
		return panel;
	}

	private JPanel createMarkerSelectorPanel()
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
		panel.add(Box.createVerticalStrut(3));
		markerSelectionSummary.setForeground(ColorScheme.TEXT_COLOR);

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
		panel.add(selectionRow);
		return panel;
	}

	private JPanel createMarkerDetailPanel()
	{
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Selected Marker", true));
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
		return panel;
	}

	private JPanel createTextFieldRow(String text, JTextField field)
	{
		JLabel rowLabel = label(text);
		BaPanelUi.fixedSize(rowLabel, MARKER_LABEL_WIDTH, CONTROL_HEIGHT);
		BaPanelUi.fixedSize(field, SIDE_WIDTH - MARKER_LABEL_WIDTH - 6, CONTROL_HEIGHT);

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
		BaPanelUi.fixedSize(spinner, SIDE_WIDTH - MARKER_LABEL_WIDTH - 6, CONTROL_HEIGHT);

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
			setCombo.addItem(new SetOption(null, "-- New --"));
			for (TileMarkerSet set : strategyManager.getMarkerSets(waveMap))
			{
				setCombo.addItem(new SetOption(set.getId(), set.toString()));
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
			setName.setText(set.getName() == null ? "" : set.getName());
			mapMode = set.getMapMode();
			waveMap = set.getWaveMap();
			markers = copyMarkers(set.getMarkers());
			selectedMarkerIds.clear();
			mapZoom.setValue(defaultTileSize(mapMode));
			selectMapModeButton();
			selectWaveMapButton();
			refreshMarkerControls();
			resizeMap();
			updateSetControls();
			dirty = false;
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
			setName.setText("");
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
		String name = setName.getText().trim();
		if (name.isEmpty())
		{
			Toolkit.getDefaultToolkit().beep();
			setName.requestFocusInWindow();
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
		refreshSetCombo(selectedSetId);
		updateSetControls();
		dirty = false;
		if (setsChanged != null)
		{
			setsChanged.run();
		}
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
				setName.getText().trim(),
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
		}
	}

	private void updateSetControls()
	{
		deleteSetButton.setEnabled(selectedSetId != null && !selectedSetBuiltIn);
	}

	private void toggleHelp()
	{
		helpVisible = !helpVisible;
		strategyManager.setMarkerEditorHelpVisible(helpVisible);
		if (helpPanel != null)
		{
			helpPanel.setVisible(helpVisible);
		}
		updateHelpButtonText();
		updatePreferredSize();
		revalidate();
		repaint();
		Window window = SwingUtilities.getWindowAncestor(this);
		if (window != null)
		{
			window.pack();
		}
	}

	private void updateHelpButtonText()
	{
		helpToggleButton.setText(helpVisible ? "Hide Help" : "Show Help");
	}

	private void updatePreferredSize()
	{
		setPreferredSize(new Dimension(EDITOR_WIDTH, EDITOR_BASE_HEIGHT + (helpVisible ? HELP_HEIGHT : 0)));
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
		boolean hasSelection = !selectedMarkerIds.isEmpty();
		boolean singleSelection = getSelectedMarker() != null;
		markerCombo.setEnabled(!markers.isEmpty());
		markerTextPanel.setVisible(singleSelection);
		markerName.setEnabled(singleSelection);
		markerLabel.setEnabled(singleSelection);
		markerColorButton.setEnabled(hasSelection);
		markerOpacity.setEnabled(hasSelection);
		markerBorderWidth.setEnabled(hasSelection);
		deleteMarkerButton.setEnabled(hasSelection);
		markerSelectionSummary.setText(selectionSummaryText());
		markerOpacityValue.setText(getMarkerOpacityPercent() + "%");
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

		return MAP_DEFAULT_TILE_SIZE;
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

	private static class PlaceholderTextField extends JTextField
	{
		private final String placeholder;

		private PlaceholderTextField(String placeholder)
		{
			this.placeholder = placeholder;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			if (!getText().isEmpty() || placeholder == null || placeholder.isEmpty())
			{
				return;
			}

			graphics.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
			graphics.setFont(getFont());
			int y = (getHeight() + graphics.getFontMetrics().getAscent() - graphics.getFontMetrics().getDescent()) / 2;
			graphics.drawString(placeholder, getInsets().left + 2, y);
		}
	}

	private static class SetOption
	{
		private final String id;
		private final String label;

		private SetOption(String id, String label)
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
