package com.bahealerorder.defender;

import com.bahealerorder.defender.strategies.DefenderMapLayout;
import com.bahealerorder.defender.strategies.DefenderMarker;
import com.bahealerorder.common.BaIcons;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JSeparator;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.util.SwingUtil;

class DefenderTileMarkerEditor extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int SIDE_WIDTH = 250;
	private static final int MAP_VIEWPORT_WIDTH = 760;
	private static final int MAP_VIEWPORT_HEIGHT = 660;
	private static final int MAP_MIN_TILE_SIZE = 7;
	private static final int MAP_MAX_TILE_SIZE = 48;
	private static final int MAP_DEFAULT_TILE_SIZE = 30;
	private static final int WAVE_COMBO_WIDTH = 92;
	private static final int STRATEGY_COMBO_WIDTH = 260;
	private static final int NOTES_HEIGHT = 120;
	private static final int SECTION_GAP = 14;
	private static final int TRASH_BUTTON_WIDTH = 24;
	private static final int MARKER_LABEL_WIDTH = 82;
	private static final int MAX_OPACITY_PERCENT = 100;
	private static final double MIN_BORDER_WIDTH = 0.0;
	private static final double MAX_BORDER_WIDTH = 8.0;
	private static final String NOTES_TOOLTIP = "<html>Notes appear in the overlay panel, if enabled.<br>"
			+ "To utilize dynamic note highlighting, start a line with a wave time. E.g.<br>"
			+ "12.0 - Delay healer<br>"
			+ "18.0 - Mainstack delay<br>"
			+ "48.0 - Hendi triangle</html>";

	private final IntSupplier waveSupplier;
	private final Consumer<Integer> waveChanged;
	private final Supplier<List<StrategyOption>> strategyOptionsSupplier;
	private final Supplier<String> selectedStrategyIdSupplier;
	private final Consumer<String> strategyChanged;
	private final Supplier<String> strategyNameSupplier;
	private final Consumer<String> strategyNameChanged;
	private final IntSupplier numberOfLogsSupplier;
	private final Consumer<Integer> numberOfLogsChanged;
	private final Supplier<String> notesSupplier;
	private final Consumer<String> notesChanged;
	private final Consumer<List<DefenderMarker>> markersChanged;
	private final Runnable importRequested;
	private final Runnable exportRequested;
	private final Runnable saveRequested;
	private final MarkerClipboardExporter markerClipboardExporter;
	private final MarkerClipboardImporter markerClipboardImporter;
	private final MarkerStyleChanged markerStyleChanged;
	private final ColorPickerManager colorPickerManager;
	private final JComboBox<ComboItem> waveCombo = new JComboBox<>();
	private final JComboBox<ComboItem> strategyCombo = new JComboBox<>();
	private final JComboBox<ComboItem> markerCombo = new JComboBox<>();
	private final JTextField markerName = new JTextField();
	private final JTextField markerLabel = new JTextField();
	private final JTextField strategyName = new JTextField();
	private final JButton markerColorButton = new JButton();
	private final JSpinner markerOpacity = new JSpinner(new SpinnerNumberModel(
			DefenderMarker.DEFAULT_OPACITY_PERCENT,
			0,
			MAX_OPACITY_PERCENT,
			5
	));
	private final JSpinner markerBorderWidth = new JSpinner(new SpinnerNumberModel(
			(double) DefenderMarker.DEFAULT_BORDER_WIDTH,
			MIN_BORDER_WIDTH,
			MAX_BORDER_WIDTH,
			0.5
	));
	private final JButton deleteMarkerButton = new JButton();
	private final JButton importButton = new JButton("Import");
	private final JButton exportButton = new JButton("Export");
	private final JButton saveButton = new JButton("Save Wave Strategy");
	private final JLabel selectedCountLabel = new JLabel();
	private final JLabel markerSelectionSummary = new JLabel();
	private final JButton copyMarkersButton = new JButton();
	private final JButton pasteMarkersButton = new JButton();
	private final JComboBox<Integer> numberOfLogs = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4});
	private final JTextArea notes = new JTextArea();
	private final JSlider mapZoom = new JSlider(MAP_MIN_TILE_SIZE, MAP_MAX_TILE_SIZE, MAP_DEFAULT_TILE_SIZE);
	private final DefenderTileMarkerMapPanel mapPanel;
	private final JScrollPane mapScrollPane;
	private final JPanel markerSelectorPanel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final JPanel markerDetailPanel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final JPanel markerTextPanel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);

	private List<DefenderMarker> markers = new ArrayList<>();
	private final Set<String> selectedMarkerIds = new LinkedHashSet<>();
	private Color markerColor = DefenderTileMarkerMapPanel.DEFAULT_MARKER_COLOR;
	private boolean refreshing;
	private boolean strategyNameEditable = true;

	DefenderTileMarkerEditor(
			IntSupplier waveSupplier,
			Consumer<Integer> waveChanged,
			Supplier<List<StrategyOption>> strategyOptionsSupplier,
			Supplier<String> selectedStrategyIdSupplier,
			Consumer<String> strategyChanged,
			Supplier<String> strategyNameSupplier,
			Consumer<String> strategyNameChanged,
			IntSupplier numberOfLogsSupplier,
			Consumer<Integer> numberOfLogsChanged,
			Supplier<String> notesSupplier,
			Consumer<String> notesChanged,
			Consumer<List<DefenderMarker>> markersChanged,
			Runnable importRequested,
			Runnable exportRequested,
			Runnable saveRequested,
			MarkerClipboardExporter markerClipboardExporter,
			MarkerClipboardImporter markerClipboardImporter,
			String lastMarkerColor,
			int lastMarkerOpacityPercent,
			float lastMarkerBorderWidth,
			MarkerStyleChanged markerStyleChanged,
			ColorPickerManager colorPickerManager)
	{
		this.waveSupplier = waveSupplier;
		this.waveChanged = waveChanged;
		this.strategyOptionsSupplier = strategyOptionsSupplier;
		this.selectedStrategyIdSupplier = selectedStrategyIdSupplier;
		this.strategyChanged = strategyChanged;
		this.strategyNameSupplier = strategyNameSupplier;
		this.strategyNameChanged = strategyNameChanged;
		this.numberOfLogsSupplier = numberOfLogsSupplier;
		this.numberOfLogsChanged = numberOfLogsChanged;
		this.notesSupplier = notesSupplier;
		this.notesChanged = notesChanged;
		this.markersChanged = markersChanged;
		this.importRequested = importRequested;
		this.exportRequested = exportRequested;
		this.saveRequested = saveRequested;
		this.markerClipboardExporter = markerClipboardExporter;
		this.markerClipboardImporter = markerClipboardImporter;
		this.markerStyleChanged = markerStyleChanged;
		this.colorPickerManager = colorPickerManager;
		mapPanel = new DefenderTileMarkerMapPanel(
				waveSupplier,
				() -> markers,
				() -> selectedMarkerIds,
				mapZoom::getValue,
				this::addOrSelectMarkerAt
		);
		mapScrollPane = new JScrollPane(mapPanel);
		markerColor = parseColor(lastMarkerColor, DefenderTileMarkerMapPanel.DEFAULT_MARKER_COLOR);
		markerOpacity.setValue(clampOpacity(lastMarkerOpacityPercent));
		markerBorderWidth.setValue((double) clampBorderWidth(lastMarkerBorderWidth));

		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 10));
		setPreferredSize(new Dimension(1080, 780));

		add(createHeader(), BorderLayout.NORTH);
		add(createBody(), BorderLayout.CENTER);
		addTextChangeListener(strategyName, this::updateStrategyNameFromField);
		addTextChangeListener(markerName, this::updateSelectedMarkerFromFields);
		addTextChangeListener(markerLabel, this::updateSelectedMarkerFromFields);
		addTextChangeListener(notes, this::updateNotesFromField);
		ChangeListener markerStyleListener = event -> updateSelectedMarkerStyleFromFields();
		markerOpacity.addChangeListener(markerStyleListener);
		markerBorderWidth.addChangeListener(markerStyleListener);
		refreshStrategyControls();
		refreshMarkerControls();
		resizeMap();
	}

	void setMarkers(List<DefenderMarker> markers)
	{
		this.markers = markers == null ? new ArrayList<>() : new ArrayList<>(markers);
		pruneSelectedMarkers();
		refreshMarkerControls();
		refreshMap();
	}

	void setStrategyNameEditable(boolean strategyNameEditable)
	{
		this.strategyNameEditable = strategyNameEditable;
		strategyName.setEditable(strategyNameEditable);
		updateMarkerDetailEnabled();
	}

	void resetView()
	{
		refreshStrategyControls();
		if (mapZoom.getValue() == MAP_DEFAULT_TILE_SIZE)
		{
			resizeMap();
		}
		else
		{
			mapZoom.setValue(MAP_DEFAULT_TILE_SIZE);
		}
		SwingUtilities.invokeLater(mapPanel::scrollToTrap);
	}

	void refreshMap()
	{
		refreshStrategyControls();
		mapPanel.repaint();
		SwingUtilities.invokeLater(mapPanel::scrollToTrap);
	}

	void focusStrategyName()
	{
		if (strategyNameEditable)
		{
			strategyName.requestFocusInWindow();
		}
	}

	private JPanel createInstructionRow()
	{
		JLabel label = label("Click any tile on the map to add a marker.", true);
		label.setHorizontalAlignment(SwingConstants.LEFT);

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.add(label, BorderLayout.CENTER);
		return row;
	}

	private JPanel createHeader()
	{
		JPanel header = new JPanel(new BorderLayout(10, 0));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.add(createStrategySelectorPanel(), BorderLayout.WEST);
		header.add(createInstructionRow(), BorderLayout.CENTER);
		return header;
	}

	private JPanel createStrategySelectorPanel()
	{
		JPanel panel = new JPanel(new BorderLayout(6, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setPreferredSize(new Dimension(WAVE_COMBO_WIDTH + STRATEGY_COMBO_WIDTH + 6, CONTROL_HEIGHT));

		for (int wave = 1; wave <= 10; wave++)
		{
			waveCombo.addItem(new ComboItem(String.valueOf(wave), "Wave " + wave));
		}
		styleCombo(waveCombo, WAVE_COMBO_WIDTH);
		waveCombo.addActionListener(event ->
		{
			if (refreshing) return;

			ComboItem item = (ComboItem) waveCombo.getSelectedItem();
			if (item != null && item.id != null)
			{
				waveChanged.accept(Integer.parseInt(item.id));
			}
		});

		styleCombo(strategyCombo, STRATEGY_COMBO_WIDTH);
		strategyCombo.addActionListener(event ->
		{
			if (refreshing) return;

			ComboItem item = (ComboItem) strategyCombo.getSelectedItem();
			strategyChanged.accept(item == null ? null : item.id);
		});

		panel.add(waveCombo, BorderLayout.WEST);
		panel.add(strategyCombo, BorderLayout.CENTER);
		return panel;
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
		mapScrollPane.getHorizontalScrollBar().setUnitIncrement(MAP_DEFAULT_TILE_SIZE);
		mapScrollPane.getVerticalScrollBar().setUnitIncrement(MAP_DEFAULT_TILE_SIZE);
		mapScrollPane.addMouseWheelListener(this::zoomFromMouseWheel);
		mapPanel.addMouseWheelListener(this::zoomFromMouseWheel);
		panel.add(createLayeredMap(), BorderLayout.CENTER);
		panel.add(createZoomControls(), BorderLayout.SOUTH);
		return panel;
	}

	private JLayeredPane createLayeredMap()
	{
		JPanel floatingControls = createFloatingMapControls();
		JLayeredPane layeredPane = new JLayeredPane()
		{
			@Override
			public void doLayout()
			{
				mapScrollPane.setBounds(0, 0, getWidth(), getHeight());
				Dimension controlsSize = floatingControls.getPreferredSize();
				floatingControls.setBounds(
						Math.max(0, getWidth() - controlsSize.width - 18),
						8,
						controlsSize.width,
						controlsSize.height
				);
			}
		};
		fixedSize(layeredPane, MAP_VIEWPORT_WIDTH, MAP_VIEWPORT_HEIGHT);
		layeredPane.add(mapScrollPane, JLayeredPane.DEFAULT_LAYER);
		layeredPane.add(floatingControls, JLayeredPane.PALETTE_LAYER);
		return layeredPane;
	}

	private JPanel createFloatingMapControls()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setBorder(new EmptyBorder(4, 6, 4, 6));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		selectedCountLabel.setForeground(ColorScheme.TEXT_COLOR);
		selectedCountLabel.setFont(FontManager.getRunescapeSmallFont());
		panel.add(selectedCountLabel);
		panel.add(Box.createHorizontalStrut(6));

		copyMarkersButton.setIcon(BaIcons.copyIcon());
		copyMarkersButton.setToolTipText("Copy selected markers");
		SwingUtil.removeButtonDecorations(copyMarkersButton);
		fixedSize(copyMarkersButton, CONTROL_HEIGHT, CONTROL_HEIGHT);
		copyMarkersButton.addActionListener(event -> copySelectedMarkersToClipboard());
		panel.add(copyMarkersButton);
		panel.add(Box.createHorizontalStrut(4));

		pasteMarkersButton.setIcon(BaIcons.pasteIcon());
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
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(0, 0, 0, 0));
		panel.setPreferredSize(new Dimension(SIDE_WIDTH, MAP_VIEWPORT_HEIGHT));
		panel.add(createLegendPanel());
		panel.add(Box.createVerticalStrut(SECTION_GAP * 2));
		panel.add(createMarkerSelectorPanel());
		panel.add(Box.createVerticalStrut(SECTION_GAP));
		panel.add(createMarkerDetailPanel());
		panel.add(Box.createVerticalStrut(14));
		panel.add(separator());
		panel.add(Box.createVerticalStrut(10));
		panel.add(createStrategyPanel());
		panel.add(Box.createVerticalStrut(8));
		panel.add(createNumberOfLogsPanel());
		panel.add(Box.createVerticalStrut(8));
		panel.add(createNotesPanel());
		return panel;
	}

	private JPanel createLegendPanel()
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Legend", true));
		panel.add(Box.createVerticalStrut(5));

		JPanel grid = new JPanel(new GridLayout(4, 2, 8, 2));
		grid.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		grid.setAlignmentX(LEFT_ALIGNMENT);
		fixedSize(grid, SIDE_WIDTH, 78);
		grid.add(legendRow(DefenderTileMarkerMapPanel.TRAP_COLOR, "Trap"));
		grid.add(legendRow(DefenderTileMarkerMapPanel.LOGS_COLOR, "Logs"));
		grid.add(legendRow(DefenderTileMarkerMapPanel.HAMMER_COLOR, "Hammer"));
		grid.add(legendRow(DefenderTileMarkerMapPanel.RUNNER_CAVE_COLOR, "Runner cave"));
		grid.add(legendRow(DefenderTileMarkerMapPanel.HEALER_CAVE_COLOR, "Healer cave"));
		grid.add(legendRow(DefenderTileMarkerMapPanel.QUEEN_TRAPDOOR_COLOR, "Queen door"));
		grid.add(legendRow(DefenderTileMarkerMapPanel.CANNON_HILL_COLOR, "Cannon hill"));
		grid.add(legendRow(DefenderTileMarkerMapPanel.DISABLED_TILE_COLOR, "Unavailable"));
		panel.add(grid);
		return panel;
	}

	private JPanel legendRow(Color color, String text)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(1, 0, 1, 0));
		row.setPreferredSize(new Dimension((SIDE_WIDTH - 8) / 2, 18));
		row.setMaximumSize(new Dimension((SIDE_WIDTH - 8) / 2, 18));
		row.setAlignmentX(LEFT_ALIGNMENT);

		JPanel swatch = new JPanel();
		swatch.setBackground(color);
		swatch.setPreferredSize(new Dimension(12, 12));
		swatch.setMinimumSize(new Dimension(12, 12));
		swatch.setMaximumSize(new Dimension(12, 12));
		row.add(swatch);
		row.add(Box.createHorizontalStrut(6));
		row.add(label(text));
		return row;
	}

	private JPanel createMarkerSelectorPanel()
	{
		markerSelectorPanel.add(createMarkerSelectorHeader());
		markerSelectorPanel.add(Box.createVerticalStrut(3));
		styleCombo(markerCombo, SIDE_WIDTH);
		markerCombo.addActionListener(event ->
		{
			if (refreshing) return;

			ComboItem item = (ComboItem) markerCombo.getSelectedItem();
			selectedMarkerIds.clear();
			if (item != null && item.id != null)
			{
				selectedMarkerIds.add(item.id);
			}
			loadSelectedMarker();
		});
		markerSelectorPanel.add(markerCombo);
		markerSelectionSummary.setForeground(ColorScheme.TEXT_COLOR);
		markerSelectionSummary.setFont(FontManager.getRunescapeSmallFont());
		markerSelectionSummary.setHorizontalAlignment(SwingConstants.CENTER);
		fixedSize(markerSelectionSummary, SIDE_WIDTH, CONTROL_HEIGHT);
		markerSelectorPanel.add(markerSelectionSummary);
		return markerSelectorPanel;
	}

	private JPanel createMarkerDetailPanel()
	{
		markerTextPanel.add(createTextFieldRow("Marker Name", markerName));
		markerTextPanel.add(Box.createVerticalStrut(6));

		markerTextPanel.add(createTextFieldRow("Tile Label", markerLabel));
		markerTextPanel.add(Box.createVerticalStrut(6));
		markerDetailPanel.add(markerTextPanel);

		markerColorButton.addActionListener(event -> chooseMarkerColor());
		updateMarkerColorButton();
		fixedSize(markerColorButton, SIDE_WIDTH, CONTROL_HEIGHT);
		markerDetailPanel.add(label("Color"));
		markerDetailPanel.add(Box.createVerticalStrut(3));
		markerDetailPanel.add(markerColorButton);
		markerDetailPanel.add(Box.createVerticalStrut(6));

		markerDetailPanel.add(createMarkerStyleRow());
		markerDetailPanel.setPreferredSize(new Dimension(SIDE_WIDTH, CONTROL_HEIGHT * 7 + 18));
		markerDetailPanel.setMaximumSize(new Dimension(SIDE_WIDTH, CONTROL_HEIGHT * 7 + 18));
		return markerDetailPanel;
	}

	private JPanel createMarkerSelectorHeader()
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);
		row.add(label("Markers", true), BorderLayout.CENTER);

		deleteMarkerButton.setIcon(BaIcons.trashIcon());
		deleteMarkerButton.setToolTipText("Delete selected markers");
		SwingUtil.removeButtonDecorations(deleteMarkerButton);
		fixedSize(deleteMarkerButton, TRASH_BUTTON_WIDTH, CONTROL_HEIGHT);
		deleteMarkerButton.addActionListener(event -> deleteSelectedMarkers());
		row.add(deleteMarkerButton, BorderLayout.EAST);
		return row;
	}

	private JPanel createTextFieldRow(String text, JTextField field)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);

		JLabel rowLabel = label(text);
		fixedSize(rowLabel, MARKER_LABEL_WIDTH, CONTROL_HEIGHT);
		row.add(rowLabel, BorderLayout.WEST);
		fixedSize(field, SIDE_WIDTH - MARKER_LABEL_WIDTH - 6, CONTROL_HEIGHT);
		row.add(field, BorderLayout.CENTER);
		return row;
	}

	private JPanel createMarkerStyleRow()
	{
		JPanel row = new JPanel(new GridLayout(1, 2, 6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setAlignmentX(LEFT_ALIGNMENT);
		fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT * 2 + 3);

		row.add(labeledMarkerControl("Opacity %", markerOpacity));
		row.add(labeledMarkerControl("Border Width", markerBorderWidth));
		return row;
	}

	private JPanel labeledMarkerControl(String text, JComponent component)
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label(text));
		panel.add(Box.createVerticalStrut(3));
		fixedSize(component, (SIDE_WIDTH - 6) / 2, CONTROL_HEIGHT);
		panel.add(component);
		return panel;
	}

	private JPanel createStrategyPanel()
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Strategy", true));
		panel.add(Box.createVerticalStrut(3));
		strategyName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Enter strategy name...");
		fixedSize(strategyName, SIDE_WIDTH, CONTROL_HEIGHT);
		panel.add(strategyName);
		return panel;
	}

	private JPanel createNumberOfLogsPanel()
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Number of Logs"));
		panel.add(Box.createVerticalStrut(3));
		styleCombo(numberOfLogs, SIDE_WIDTH);
		numberOfLogs.addActionListener(event ->
		{
			if (refreshing) return;
			numberOfLogsChanged.accept(getNumberOfLogs());
		});
		panel.add(numberOfLogs);
		return panel;
	}

	private JPanel createNotesPanel()
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(labelWithInfo("Notes"));
		panel.add(Box.createVerticalStrut(3));
		styleTextArea(notes);
		panel.add(wrapTextArea(notes, NOTES_HEIGHT));
		panel.add(Box.createVerticalStrut(8));
		panel.add(createImportExportRow());
		panel.add(Box.createVerticalStrut(5));
		saveButton.addActionListener(event -> saveRequested.run());
		fixedSize(saveButton, SIDE_WIDTH, CONTROL_HEIGHT);
		panel.add(saveButton);
		return panel;
	}

	private JPanel createImportExportRow()
	{
		JPanel row = new JPanel(new GridLayout(1, 2, 6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setAlignmentX(LEFT_ALIGNMENT);
		fixedSize(row, SIDE_WIDTH, CONTROL_HEIGHT);

		importButton.addActionListener(event -> importRequested.run());
		exportButton.addActionListener(event -> exportRequested.run());
		row.add(importButton);
		row.add(exportButton);
		return row;
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

	private void refreshMarkerControls()
	{
		refreshing = true;
		try
		{
			markerCombo.removeAllItems();
			markerCombo.addItem(new ComboItem(null, "Select a marker..."));
			for (DefenderMarker marker : markers)
			{
				markerCombo.addItem(new ComboItem(marker.getId(), getMarkerDisplayText(marker)));
			}
			selectMarkerComboValue(getSingleSelectedMarkerId());
		}
		finally
		{
			refreshing = false;
		}

		loadSelectedMarker();
		revalidate();
		repaint();
	}

	void refreshStrategyControls()
	{
		refreshing = true;
		try
		{
			selectComboValue(waveCombo, String.valueOf(waveSupplier.getAsInt()));
			refreshStrategyCombo();
			String name = strategyNameSupplier.get();
			strategyName.setText(name == null ? "" : name);
			numberOfLogs.setSelectedItem(numberOfLogsSupplier.getAsInt());
			String strategyNotes = notesSupplier.get();
			notes.setText(strategyNotes == null ? "" : strategyNotes);
		}
		finally
		{
			refreshing = false;
		}
	}

	private void refreshStrategyCombo()
	{
		strategyCombo.removeAllItems();
		for (StrategyOption option : strategyOptionsSupplier.get())
		{
			strategyCombo.addItem(new ComboItem(option.id, option.label));
		}
		selectComboValue(strategyCombo, selectedStrategyIdSupplier.get());
	}

	private void selectMarkerComboValue(String id)
	{
		if (id != null)
		{
			for (int i = 0; i < markerCombo.getItemCount(); i++)
			{
				ComboItem item = markerCombo.getItemAt(i);
				if (id.equals(item.id))
				{
					markerCombo.setSelectedIndex(i);
					return;
				}
			}
		}

		if (markerCombo.getItemCount() > 0)
		{
			markerCombo.setSelectedIndex(0);
		}
	}

	private void loadSelectedMarker()
	{
		DefenderMarker marker = getSelectedMarker();
		DefenderMarker styleMarker = marker == null ? getFirstSelectedMarker() : marker;
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
				markerName.setText(marker.getName() == null ? "" : marker.getName());
				markerLabel.setText(marker.getLabel() == null ? "" : marker.getLabel());
			}

			if (styleMarker != null)
			{
				markerColor = parseColor(styleMarker.getColor(), DefenderTileMarkerMapPanel.DEFAULT_MARKER_COLOR);
				markerOpacity.setValue(clampOpacity(styleMarker.getOpacityPercentOrDefault()));
				markerBorderWidth.setValue((double) clampBorderWidth(styleMarker.getBorderWidthOrDefault()));
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
		int selectedCount = selectedMarkerIds.size();
		boolean hasSelection = selectedCount > 0;
		boolean hasSingleSelection = selectedCount == 1;
		boolean hasMultipleSelections = selectedCount > 1;

		markerCombo.setVisible(!hasMultipleSelections);
		markerCombo.setEnabled(!markers.isEmpty() && !hasMultipleSelections);
		markerSelectionSummary.setText(hasMultipleSelections ? selectedCount + " markers selected" : "");
		markerSelectionSummary.setVisible(hasMultipleSelections);
		markerTextPanel.setVisible(!hasMultipleSelections);
		markerName.setEditable(hasSingleSelection);
		markerName.setEnabled(hasSingleSelection);
		markerLabel.setEditable(hasSingleSelection);
		markerLabel.setEnabled(hasSingleSelection);
		markerColorButton.setEnabled(hasSelection);
		markerOpacity.setEnabled(hasSelection);
		markerBorderWidth.setEnabled(hasSelection);
		deleteMarkerButton.setEnabled(hasSelection);
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
		notifyMarkersChanged();
	}

	private void updateSelectedMarkerStyleFromFields()
	{
		if (refreshing)
		{
			return;
		}

		List<DefenderMarker> selectedMarkers = getSelectedMarkers();
		if (!selectedMarkers.isEmpty())
		{
			String color = toHex(markerColor);
			int opacity = getMarkerOpacityPercent();
			float borderWidth = getMarkerBorderWidth();
			for (DefenderMarker marker : selectedMarkers)
			{
				marker.setColor(color);
				marker.setOpacityPercent(opacity);
				marker.setBorderWidth(borderWidth);
			}
			mapPanel.repaint();
			notifyMarkersChanged();
		}

		persistCurrentMarkerStyle();
	}

	private void updateNotesFromField()
	{
		if (!refreshing)
		{
			notesChanged.accept(notes.getText());
		}
	}

	private void updateStrategyNameFromField()
	{
		if (!refreshing && strategyNameEditable)
		{
			strategyNameChanged.accept(strategyName.getText());
		}
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

	private void updateMarkerColorButton()
	{
		markerColorButton.setText(toHex(markerColor));
		markerColorButton.setBackground(markerColor);
		markerColorButton.setForeground(getReadableTextColor(markerColor));
	}

	private DefenderMarker getSelectedMarker()
	{
		String selectedMarkerId = getSingleSelectedMarkerId();
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

	private String getSingleSelectedMarkerId()
	{
		return selectedMarkerIds.size() == 1 ? selectedMarkerIds.iterator().next() : null;
	}

	private int getNumberOfLogs()
	{
		Integer selected = (Integer) numberOfLogs.getSelectedItem();
		return selected == null ? 0 : selected;
	}

	private int getMarkerOpacityPercent()
	{
		return clampOpacity(((Number) markerOpacity.getValue()).intValue());
	}

	private float getMarkerBorderWidth()
	{
		return clampBorderWidth(((Number) markerBorderWidth.getValue()).floatValue());
	}

	private void addOrSelectMarkerAt(int mapX, int mapY)
	{
		DefenderMapLayout layout = DefenderMapLayout.forWave(waveSupplier.getAsInt());

		if (!mapPanel.isSelectableMapTile(layout, mapX, mapY))
		{
			return;
		}

		for (DefenderMarker marker : markers)
		{
			if (layout.contains(marker.getTile())
					&& layout.toMapX(marker.getTile()) == mapX
					&& layout.toMapY(marker.getTile()) == mapY)
			{
				if (selectedMarkerIds.contains(marker.getId()))
				{
					if (canLeftClickDelete(marker))
					{
						deleteMarker(marker);
					}
					else
					{
						selectedMarkerIds.remove(marker.getId());
						refreshMarkerControls();
					}
				}
				else
				{
					selectedMarkerIds.add(marker.getId());
					refreshMarkerControls();
				}
				return;
			}
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
		notifyMarkersChanged();
	}

	private void deleteMarker(DefenderMarker marker)
	{
		markers.remove(marker);
		selectedMarkerIds.remove(marker.getId());
		refreshMarkerControls();
		mapPanel.repaint();
		notifyMarkersChanged();
	}

	private void deleteSelectedMarkers()
	{
		if (selectedMarkerIds.isEmpty())
		{
			return;
		}

		markers.removeIf(marker -> selectedMarkerIds.contains(marker.getId()));
		selectedMarkerIds.clear();
		refreshMarkerControls();
		mapPanel.repaint();
		notifyMarkersChanged();
	}

	private void pruneSelectedMarkers()
	{
		selectedMarkerIds.removeIf(id -> markers.stream().noneMatch(marker -> id.equals(marker.getId())));
	}

	private void copySelectedMarkersToClipboard()
	{
		List<DefenderMarker> selectedMarkers = getSelectedMarkers();
		String json = markerClipboardExporter.exportMarkers(waveSupplier.getAsInt(), selectedMarkers);

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

		List<DefenderMarker> pastedMarkers = markerClipboardImporter.importMarkers(waveSupplier.getAsInt(), json);

		if (pastedMarkers == null || pastedMarkers.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "Clipboard does not contain copied markers.", "Paste Markers", JOptionPane.ERROR_MESSAGE);
			return;
		}

		DefenderMapLayout layout = DefenderMapLayout.forWave(waveSupplier.getAsInt());
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
		notifyMarkersChanged();
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

	private void updateFloatingSelectionControls()
	{
		int selectedCount = selectedMarkerIds.size();
		selectedCountLabel.setText(selectedCount == 0 ? "" : selectedCount + " selected");
		copyMarkersButton.setEnabled(selectedCount > 0);
		pasteMarkersButton.setEnabled(true);
	}

	private boolean canLeftClickDelete(DefenderMarker marker)
	{
		return marker != null && isBlank(marker.getName());
	}

	private boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
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
			ComboItem item = markerCombo.getItemAt(i);

			if (marker.getId().equals(item.id))
			{
				item.label = getMarkerDisplayText(marker);
				markerCombo.repaint();
				return;
			}
		}
	}

	private void notifyMarkersChanged()
	{
		markersChanged.accept(new ArrayList<>(markers));
	}

	private void persistCurrentMarkerStyle()
	{
		markerStyleChanged.accept(toHex(markerColor), getMarkerOpacityPercent(), getMarkerBorderWidth());
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

	private String userMarkerId(int mapX, int mapY)
	{
		return "marker:" + waveSupplier.getAsInt() + ":" + mapX + ":" + mapY + ":" + System.nanoTime();
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
		label.setFont(bold ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private JPanel labelWithInfo(String text)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setPreferredSize(new Dimension(SIDE_WIDTH, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(SIDE_WIDTH, CONTROL_HEIGHT));

		JLabel info = new JLabel(BaIcons.infoIcon());
		info.setToolTipText(NOTES_TOOLTIP);
		row.add(label(text));
		row.add(Box.createHorizontalStrut(5));
		row.add(info);
		row.add(Box.createHorizontalGlue());
		return row;
	}

	private void styleCombo(JComboBox<?> comboBox, int width)
	{
		comboBox.setFocusable(false);
		fixedSize(comboBox, width, CONTROL_HEIGHT);
	}

	private void styleTextArea(JTextArea area)
	{
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
	}

	private JScrollPane wrapTextArea(JTextArea area, int height)
	{
		JScrollPane scrollPane = new JScrollPane(area);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
		fixedSize(scrollPane, SIDE_WIDTH, height);
		return scrollPane;
	}

	private JSeparator separator()
	{
		JSeparator separator = new JSeparator();
		separator.setForeground(ColorScheme.DARK_GRAY_COLOR);
		separator.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(separator, SIDE_WIDTH, 1);
		return separator;
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

	private static class ComboItem
	{
		private final String id;
		private String label;

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

	static class StrategyOption
	{
		private final String id;
		private final String label;

		StrategyOption(String id, String label)
		{
			this.id = id;
			this.label = label;
		}
	}

	interface MarkerStyleChanged
	{
		void accept(String color, int opacityPercent, float borderWidth);
	}

	interface MarkerClipboardExporter
	{
		String exportMarkers(int wave, List<DefenderMarker> markers);
	}

	interface MarkerClipboardImporter
	{
		List<DefenderMarker> importMarkers(int wave, String json);
	}

}
