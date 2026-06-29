package com.bahealerorder.defender;

import com.bahealerorder.common.BaIcons;
import com.bahealerorder.defender.strategies.DefenderMapLayout;
import com.bahealerorder.defender.strategies.DefenderMarker;
import com.bahealerorder.defender.strategies.DefenderTile;
import com.bahealerorder.tilemarkers.GeneralTileMarkerStrategyManager;
import com.bahealerorder.tilemarkers.TileMarkerSet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
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
import javax.swing.JComponent;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.SwingUtil;

public class TileMarkerSetEditor extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int SIDE_WIDTH = 250;
	private static final int MAP_VIEWPORT_WIDTH = 760;
	private static final int MAP_VIEWPORT_HEIGHT = 660;
	private static final int MAP_MIN_TILE_SIZE = 7;
	private static final int MAP_MAX_TILE_SIZE = 48;
	private static final int MAP_DEFAULT_TILE_SIZE = 30;
	private static final int FULL_ARENA_DEFAULT_ZOOM_STEPS_OUT = 10;
	private static final int SECTION_GAP = 14;
	private static final int MARKER_LABEL_WIDTH = 82;
	private static final int TRASH_BUTTON_WIDTH = 24;
	private static final int MAX_OPACITY_PERCENT = 100;
	private static final double MIN_BORDER_WIDTH = 0.0;
	private static final double MAX_BORDER_WIDTH = 8.0;
	private static final Color BUILT_IN_TEXT_COLOR = new Color(0x77E2FF);

	private final GeneralTileMarkerStrategyManager strategyManager;
	private final ColorPickerManager colorPickerManager;
	private final Runnable setsChanged;
	private final JComboBox<SetOption> setCombo = new JComboBox<>();
	private final JComboBox<MarkerOption> markerCombo = new JComboBox<>();
	private final JTextField setName = new PlaceholderTextField("Name this set of markers...");
	private final JTextField markerName = new JTextField();
	private final JTextField markerLabel = new JTextField();
	private final JButton markerColorButton = new JButton();
	private final JButton deleteMarkerButton = new JButton();
	private final JButton deleteSetButton = new JButton(BaIcons.trashIcon());
	private final JButton copyMarkersButton = new JButton(BaIcons.copyIcon());
	private final JButton pasteMarkersButton = new JButton(BaIcons.pasteIcon());
	private final JLabel selectedCountLabel = new JLabel();
	private final JLabel markerSelectionSummary = new JLabel();
	private final JLabel markerOpacityValue = new JLabel();
	private final JSlider markerOpacity = new JSlider(0, MAX_OPACITY_PERCENT, DefenderMarker.DEFAULT_OPACITY_PERCENT);
	private final JSpinner markerBorderWidth = new JSpinner(new SpinnerNumberModel(
			(double) DefenderMarker.DEFAULT_BORDER_WIDTH,
			MIN_BORDER_WIDTH,
			MAX_BORDER_WIDTH,
			0.5
	));
	private final JSlider mapZoom = new JSlider(MAP_MIN_TILE_SIZE, MAP_MAX_TILE_SIZE, MAP_DEFAULT_TILE_SIZE);
	private final DefenderTileMarkerMapPanel mapPanel;
	private final JScrollPane mapScrollPane;
	private final JPanel markerTextPanel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final ButtonGroup mapModeButtonGroup = new ButtonGroup();
	private final ButtonGroup waveMapButtonGroup = new ButtonGroup();

	private List<DefenderMarker> markers = new ArrayList<>();
	private final Set<String> selectedMarkerIds = new LinkedHashSet<>();
	private TileMarkerMapMode mapMode;
	private TileMarkerWaveMap waveMap;
	private String selectedSetId;
	private Color markerColor = DefenderTileMarkerMapPanel.DEFAULT_MARKER_COLOR;
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

		mapZoom.setValue(defaultTileSize(mapMode));
		mapPanel = new DefenderTileMarkerMapPanel(
				() -> waveMap.getLayout(),
				() -> mapMode,
				() -> markers,
				() -> selectedMarkerIds,
				mapZoom::getValue,
				this::addOrSelectMarkerAt
		);
		mapScrollPane = new JScrollPane(mapPanel);
		markerColor = parseColor(strategyManager.getLastMarkerColor(), DefenderTileMarkerMapPanel.DEFAULT_MARKER_COLOR);
		markerOpacity.setValue(strategyManager.getLastMarkerOpacityPercent());
		markerOpacity.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		markerOpacity.setFocusable(false);
		markerOpacity.setMajorTickSpacing(25);
		markerOpacity.setMinorTickSpacing(5);
		markerBorderWidth.setValue((double) strategyManager.getLastMarkerBorderWidth());
		setCombo.setRenderer(new BuiltInSetRenderer());

		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 10));
		setPreferredSize(new Dimension(1080, 780));

		add(createHeader(), BorderLayout.NORTH);
		add(createBody(), BorderLayout.CENTER);

		addTextChangeListener(setName, this::markDirty);
		addTextChangeListener(markerName, this::updateSelectedMarkerFromFields);
		addTextChangeListener(markerLabel, this::updateSelectedMarkerFromFields);
		ChangeListener markerStyleListener = event -> updateSelectedMarkerStyleFromFields();
		markerOpacity.addChangeListener(markerStyleListener);
		markerBorderWidth.addChangeListener(markerStyleListener);
		refreshSetCombo(null);
		clearDraft();
		resizeMap();
	}

	private JPanel createHeader()
	{
		JLabel label = label("Click any usable tile on the map to add a marker.", true);
		label.setHorizontalAlignment(SwingConstants.LEFT);

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.add(label, BorderLayout.CENTER);
		return row;
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
		JPanel selectionControls = createFloatingSelectionControls();
		JLayeredPane layeredPane = new JLayeredPane()
		{
			@Override
			public void doLayout()
			{
				mapScrollPane.setBounds(0, 0, getWidth(), getHeight());

				Dimension modeSize = mapModeControls.getPreferredSize();
				mapModeControls.setBounds(8, 8, modeSize.width, modeSize.height);

				Dimension selectionSize = selectionControls.getPreferredSize();
				selectionControls.setBounds(
						Math.max(0, getWidth() - selectionSize.width - 18),
						8,
						selectionSize.width,
						selectionSize.height
				);
			}
		};
		fixedSize(layeredPane, MAP_VIEWPORT_WIDTH, MAP_VIEWPORT_HEIGHT);
		layeredPane.add(mapScrollPane, JLayeredPane.DEFAULT_LAYER);
		layeredPane.add(mapModeControls, JLayeredPane.PALETTE_LAYER);
		layeredPane.add(selectionControls, JLayeredPane.PALETTE_LAYER);
		return layeredPane;
	}

	private JPanel createMapModeControls()
	{
		JPanel panel = new JPanel(new GridLayout(2, 2, 4, 4));
		panel.setBorder(new EmptyBorder(4, 4, 4, 4));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(panel, 312, 60);

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

	private JPanel createFloatingSelectionControls()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(4, 6, 4, 6));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		selectedCountLabel.setForeground(ColorScheme.TEXT_COLOR);
		panel.add(selectedCountLabel);
		panel.add(Box.createHorizontalStrut(6));

		copyMarkersButton.setToolTipText("Copy selected markers");
		SwingUtil.removeButtonDecorations(copyMarkersButton);
		fixedSize(copyMarkersButton, CONTROL_HEIGHT, CONTROL_HEIGHT);
		copyMarkersButton.addActionListener(event -> copySelectedMarkersToClipboard());
		panel.add(copyMarkersButton);
		panel.add(Box.createHorizontalStrut(4));

		pasteMarkersButton.setToolTipText("Paste copied markers");
		SwingUtil.removeButtonDecorations(pasteMarkersButton);
		fixedSize(pasteMarkersButton, CONTROL_HEIGHT, CONTROL_HEIGHT);
		pasteMarkersButton.addActionListener(event -> pasteMarkersFromClipboard());
		panel.add(pasteMarkersButton);

		updateFloatingSelectionControls();
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
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setPreferredSize(new Dimension(SIDE_WIDTH, MAP_VIEWPORT_HEIGHT));

		JPanel top = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		top.add(createLegendPanel());
		top.add(Box.createVerticalStrut(SECTION_GAP * 3));
		top.add(createMarkerSelectorPanel());
		top.add(Box.createVerticalStrut(SECTION_GAP + 6));
		top.add(createMarkerDetailPanel());
		panel.add(top, BorderLayout.NORTH);

		JPanel bottom = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		bottom.add(Box.createVerticalStrut(SECTION_GAP * 3));
		bottom.add(createSetPanel());
		panel.add(bottom, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel createLegendPanel()
	{
		return TileMarkerLegendPanel.create(SIDE_WIDTH);
	}

	private JPanel createSetPanel()
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Tile Marker Set", true));
		panel.add(Box.createVerticalStrut(5));

		styleCombo(setCombo, SIDE_WIDTH - TRASH_BUTTON_WIDTH - 6);
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
		fixedSize(deleteSetButton, TRASH_BUTTON_WIDTH, CONTROL_HEIGHT);
		deleteSetButton.addActionListener(event -> deleteSet());

		JPanel setRow = new JPanel(new BorderLayout(6, 0));
		setRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(setRow, SIDE_WIDTH, CONTROL_HEIGHT);
		setRow.add(setCombo, BorderLayout.CENTER);
		setRow.add(deleteSetButton, BorderLayout.EAST);
		panel.add(setRow);
		panel.add(Box.createVerticalStrut(5));

		fixedSize(setName, SIDE_WIDTH, CONTROL_HEIGHT);
		panel.add(setName);
		panel.add(Box.createVerticalStrut(6));

		JButton saveButton = new JButton("Save Marker Set");
		saveButton.addActionListener(event -> saveSet());
		fixedSize(saveButton, SIDE_WIDTH, CONTROL_HEIGHT);
		panel.add(saveButton);
		return panel;
	}

	private JPanel createMarkerSelectorPanel()
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Markers", true));
		panel.add(Box.createVerticalStrut(3));
		styleCombo(markerCombo, SIDE_WIDTH);
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
		fixedSize(deleteMarkerButton, TRASH_BUTTON_WIDTH, CONTROL_HEIGHT);

		JPanel selectionRow = new JPanel(new BorderLayout(6, 0));
		selectionRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(selectionRow, SIDE_WIDTH, CONTROL_HEIGHT);
		selectionRow.add(markerSelectionSummary, BorderLayout.CENTER);
		selectionRow.add(deleteMarkerButton, BorderLayout.EAST);
		panel.add(selectionRow);
		return panel;
	}

	private JPanel createMarkerDetailPanel()
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
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
		fixedSize(rowLabel, MARKER_LABEL_WIDTH, CONTROL_HEIGHT);
		fixedSize(field, SIDE_WIDTH - MARKER_LABEL_WIDTH - 6, CONTROL_HEIGHT);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);
		row.add(rowLabel, BorderLayout.WEST);
		row.add(field, BorderLayout.CENTER);
		return row;
	}

	private JPanel createColorRow()
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);

		JLabel rowLabel = label("Color");
		fixedSize(rowLabel, MARKER_LABEL_WIDTH, CONTROL_HEIGHT);
		markerColorButton.addActionListener(event -> chooseMarkerColor());
		fixedSize(markerColorButton, SIDE_WIDTH - MARKER_LABEL_WIDTH - 6, CONTROL_HEIGHT);
		row.add(rowLabel, BorderLayout.WEST);
		row.add(markerColorButton, BorderLayout.CENTER);
		updateMarkerColorButton();
		return row;
	}

	private JPanel createOpacityRow()
	{
		JLabel rowLabel = label("Opacity");
		fixedSize(rowLabel, MARKER_LABEL_WIDTH, CONTROL_HEIGHT);
		fixedSize(markerOpacity, SIDE_WIDTH - MARKER_LABEL_WIDTH - 44, CONTROL_HEIGHT);

		markerOpacityValue.setForeground(ColorScheme.TEXT_COLOR);
		markerOpacityValue.setHorizontalAlignment(SwingConstants.RIGHT);
		fixedSize(markerOpacityValue, 38, CONTROL_HEIGHT);

		JPanel sliderRow = new JPanel(new BorderLayout(6, 0));
		sliderRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sliderRow.add(markerOpacity, BorderLayout.CENTER);
		sliderRow.add(markerOpacityValue, BorderLayout.EAST);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);
		row.add(rowLabel, BorderLayout.WEST);
		row.add(sliderRow, BorderLayout.CENTER);
		return row;
	}

	private JPanel createSpinnerRow(String text, JSpinner spinner)
	{
		JLabel rowLabel = label(text);
		fixedSize(rowLabel, MARKER_LABEL_WIDTH, CONTROL_HEIGHT);
		fixedSize(spinner, SIDE_WIDTH - MARKER_LABEL_WIDTH - 6, CONTROL_HEIGHT);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);
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
		DefenderMapLayout layout = waveMap.getLayout();
		if (!mapPanel.isSelectableMapTile(layout, mapX, mapY))
		{
			return;
		}

		DefenderMarker existing = findMarkerAt(layout, mapX, mapY);
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

		DefenderMarker marker = new DefenderMarker(
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
			DefenderMapLayout layout = waveMap.getLayout();
			for (DefenderMarker marker : markers)
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
		DefenderMarker marker = getFirstSelectedMarker();
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
				markerColor = parseColor(marker.getColor(), DefenderTileMarkerMapPanel.DEFAULT_MARKER_COLOR);
				markerOpacity.setValue(clampOpacity(marker.getOpacityPercentOrDefault()));
				markerBorderWidth.setValue((double) clampBorderWidth(marker.getBorderWidthOrDefault()));
			}
		}
		finally
		{
			refreshing = wasRefreshing;
		}

		updateMarkerDetailEnabled();
		updateMarkerColorButton();
		updateFloatingSelectionControls();
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
	}

	private void updateSelectedMarkerFromFields()
	{
		if (refreshing)
		{
			return;
		}

		DefenderMarker marker = getSelectedMarker();
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

		List<DefenderMarker> selectedMarkers = getSelectedMarkers();
		if (!selectedMarkers.isEmpty())
		{
			for (DefenderMarker marker : selectedMarkers)
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
		List<DefenderMarker> selectedMarkers = getSelectedMarkers();
		if (!selectedMarkers.isEmpty())
		{
			for (DefenderMarker marker : selectedMarkers)
			{
				markers.remove(marker);
			}
			selectedMarkerIds.clear();
			refreshMarkerControls();
			mapPanel.repaint();
			markDirty();
		}
	}

	private void deleteMarker(DefenderMarker marker)
	{
		markers.remove(marker);
		selectedMarkerIds.remove(marker.getId());
		refreshMarkerControls();
		mapPanel.repaint();
		markDirty();
	}

	private void copySelectedMarkersToClipboard()
	{
		String json = strategyManager.exportMarkers(waveMap, getSelectedMarkers());
		if (json == null)
		{
			JOptionPane.showMessageDialog(this, "Select one or more markers to copy.", "Copy Markers", JOptionPane.ERROR_MESSAGE);
			return;
		}

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
	}

	private void pasteMarkersFromClipboard()
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
			JOptionPane.showMessageDialog(this, "Clipboard does not contain copied markers.", "Paste Markers", JOptionPane.ERROR_MESSAGE);
			return;
		}

		List<DefenderMarker> pastedMarkers = strategyManager.importMarkers(waveMap, json);
		if (pastedMarkers.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "Clipboard does not contain copied markers.", "Paste Markers", JOptionPane.ERROR_MESSAGE);
			return;
		}

		DefenderMapLayout layout = waveMap.getLayout();
		selectedMarkerIds.clear();
		for (DefenderMarker marker : pastedMarkers)
		{
			if (marker == null || !layout.contains(marker.getTile()))
			{
				continue;
			}

			int mapX = layout.toMapX(marker.getTile());
			int mapY = layout.toMapY(marker.getTile());
			if (!mapPanel.isSelectableMapTile(layout, mapX, mapY))
			{
				continue;
			}

			DefenderMarker existing = findMarkerAt(layout, mapX, mapY);
			if (existing != null)
			{
				markers.remove(existing);
			}

			DefenderMarker pasted = new DefenderMarker(
					userMarkerId(mapX, mapY),
					layout.toTile(mapX, mapY),
					marker.getName(),
					marker.getLabel(),
					marker.getColor(),
					marker.getOpacityPercent(),
					marker.getBorderWidth()
			);
			markers.add(pasted);
			selectedMarkerIds.add(pasted.getId());
		}

		if (selectedMarkerIds.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "No copied markers fit on this map.", "Paste Markers", JOptionPane.ERROR_MESSAGE);
			return;
		}

		refreshMarkerControls();
		mapPanel.repaint();
		markDirty();
	}

	private DefenderMarker getSelectedMarker()
	{
		String selectedMarkerId = getSelectedMarkerId();
		if (selectedMarkerId == null)
		{
			return null;
		}

		for (DefenderMarker marker : markers)
		{
			if (selectedMarkerId.equals(marker.getId()))
			{
				return marker;
			}
		}

		return null;
	}

	private DefenderMarker getFirstSelectedMarker()
	{
		for (DefenderMarker marker : markers)
		{
			if (selectedMarkerIds.contains(marker.getId()))
			{
				return marker;
			}
		}

		return null;
	}

	private List<DefenderMarker> getSelectedMarkers()
	{
		List<DefenderMarker> selectedMarkers = new ArrayList<>();
		for (DefenderMarker marker : markers)
		{
			if (selectedMarkerIds.contains(marker.getId()))
			{
				selectedMarkers.add(marker);
			}
		}
		return selectedMarkers;
	}

	private DefenderMarker findMarkerAt(DefenderMapLayout layout, int mapX, int mapY)
	{
		for (DefenderMarker marker : markers)
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

	private String getMarkerDisplayText(DefenderMarker marker)
	{
		String name = marker.getName() == null ? "" : marker.getName().trim();
		if (!name.isEmpty())
		{
			return name;
		}

		String label = marker.getLabel() == null ? "" : marker.getLabel().trim();
		return label.isEmpty() ? "Unnamed marker" : label;
	}

	private void updateMarkerComboLabel(DefenderMarker marker)
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
		markerColorButton.setForeground(getReadableTextColor(markerColor));
	}

	private void updateFloatingSelectionControls()
	{
		int selectedCount = selectedMarkerIds.size();
		selectedCountLabel.setText(selectedCount == 0 ? "" : selectedCount + " selected");
		copyMarkersButton.setEnabled(selectedCount > 0);
		pasteMarkersButton.setEnabled(true);
		markerSelectionSummary.setText(selectionSummaryText());
		markerOpacityValue.setText(getMarkerOpacityPercent() + "%");
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
		return clampOpacity(markerOpacity.getValue());
	}

	private float getMarkerBorderWidth()
	{
		return clampBorderWidth(((Number) markerBorderWidth.getValue()).floatValue());
	}

	private String userMarkerId(int mapX, int mapY)
	{
		return "marker:" + waveMap.name() + ":" + mapMode.name() + ":" + mapX + ":" + mapY + ":" + System.nanoTime();
	}

	private static List<DefenderMarker> copyMarkers(List<DefenderMarker> source)
	{
		List<DefenderMarker> copies = new ArrayList<>();
		if (source == null)
		{
			return copies;
		}

		for (DefenderMarker marker : source)
		{
			if (marker != null)
			{
				copies.add(copyMarker(marker));
			}
		}
		return copies;
	}

	private static DefenderMarker copyMarker(DefenderMarker marker)
	{
		DefenderTile tile = marker.getTile();
		DefenderTile tileCopy = tile == null
				? null
				: new DefenderTile(tile.getRegionId(), tile.getRegionX(), tile.getRegionY(), tile.getZ());
		return new DefenderMarker(
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
		Color value = color == null ? DefenderTileMarkerMapPanel.DEFAULT_MARKER_COLOR : color;
		return String.format("#%02x%02x%02x", value.getRed(), value.getGreen(), value.getBlue());
	}

	private int clampOpacity(int opacityPercent)
	{
		return Math.max(0, Math.min(MAX_OPACITY_PERCENT, opacityPercent));
	}

	private float clampBorderWidth(float borderWidth)
	{
		return Math.max((float) MIN_BORDER_WIDTH, Math.min((float) MAX_BORDER_WIDTH, borderWidth));
	}

	private Color parseColor(String color, Color fallback)
	{
		try
		{
			return color == null || color.trim().isEmpty() ? fallback : Color.decode(color);
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
	}

	private Color getReadableTextColor(Color background)
	{
		return (background.getRed() * 299 + background.getGreen() * 587 + background.getBlue() * 114) / 1000 > 140
				? Color.BLACK
				: Color.WHITE;
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

	private void styleCombo(JComboBox<?> comboBox, int width)
	{
		comboBox.setFocusable(false);
		fixedSize(comboBox, width, CONTROL_HEIGHT);
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
		private final boolean builtIn;

		private SetOption(String id, String label)
		{
			this(id, label, false);
		}

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

	private static class BuiltInSetRenderer extends DefaultListCellRenderer
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
				component.setForeground(BUILT_IN_TEXT_COLOR);
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
