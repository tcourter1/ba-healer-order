package com.bahealerorder.sidepanel.tilemarkers;

import com.bahealerorder.sidepanel.BaPanelUi;
import com.bahealerorder.tilemarkers.GeneralTileMarkerStrategyManager;
import com.bahealerorder.tilemarkers.TileMarker;
import com.bahealerorder.tilemarkers.TileMarkerMapMode;
import com.bahealerorder.tilemarkers.TileMarkerRoleContext;
import com.bahealerorder.tilemarkers.TileMarkerStrategyPreset;
import com.bahealerorder.tilemarkers.TileMarkerWaveMap;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseWheelEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

public class TileMarkerStrategyPreviewPanel extends JPanel
{
	private static final int CONTROL_HEIGHT = 24;
	private static final int SIDE_WIDTH = 250;
	private static final int MAP_VIEWPORT_WIDTH = 760;
	private static final int MAP_VIEWPORT_HEIGHT = 660;
	private static final int MAP_MIN_TILE_SIZE = 7;
	private static final int MAP_MAX_TILE_SIZE = 48;
	private static final int MAP_DEFAULT_TILE_SIZE = 20;

	private final TileMarkerWaveMap waveMap;
	private final List<TileMarker> markers;
	private final JSlider mapZoom = new JSlider(MAP_MIN_TILE_SIZE, MAP_MAX_TILE_SIZE, MAP_DEFAULT_TILE_SIZE);
	private final TileMarkerMapPanel mapPanel;
	private final JScrollPane mapScrollPane;

	public TileMarkerStrategyPreviewPanel(
			GeneralTileMarkerStrategyManager strategyManager,
			TileMarkerRoleContext roleContext,
			int wave)
	{
		this(
				TileMarkerWaveMap.fromWave(wave),
				strategyManager.getActiveMarkers(wave, roleContext),
				strategyManager.getActiveNotes(wave, roleContext),
				waveHeader(roleContext, wave)
		);
	}

	public TileMarkerStrategyPreviewPanel(
			GeneralTileMarkerStrategyManager strategyManager,
			TileMarkerStrategyPreset strategy)
	{
		this(
				strategy == null ? TileMarkerWaveMap.WAVES_1_TO_9 : strategy.getWaveMap(),
				strategyManager.getMarkersForStrategyPreset(strategy),
				strategy == null ? "" : strategy.getNotes(),
				strategyHeader(strategy)
		);
	}

	private TileMarkerStrategyPreviewPanel(
			TileMarkerWaveMap waveMap,
			List<TileMarker> markers,
			String notes,
			String headerText)
	{
		this.waveMap = waveMap == null ? TileMarkerWaveMap.WAVES_1_TO_9 : waveMap;
		this.markers = markers == null ? Collections.emptyList() : markers;
		this.mapPanel = new TileMarkerMapPanel(
				() -> this.waveMap.getLayout(),
				() -> TileMarkerMapMode.FULL_MAP,
				() -> this.markers,
				Collections::emptySet,
				mapZoom::getValue,
				(x, y) -> { }
		);
		this.mapScrollPane = new JScrollPane(mapPanel);

		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 10));
		setPreferredSize(new Dimension(1080, 780));

		add(createHeader(headerText), BorderLayout.NORTH);
		add(createBody(notes), BorderLayout.CENTER);
		resizeMap();
		javax.swing.SwingUtilities.invokeLater(mapPanel::scrollToTrap);
	}

	private static String waveHeader(TileMarkerRoleContext roleContext, int wave)
	{
		String role = roleContext == null ? TileMarkerRoleContext.DEFENDER.getDisplayName() : roleContext.getDisplayName();
		return "This is a preview of the tile markers and notes that will be visible on Wave "
				+ wave
				+ " while playing the "
				+ role
				+ " role.";
	}

	private static String strategyHeader(TileMarkerStrategyPreset strategy)
	{
		if (strategy == null || strategy.getName() == null || strategy.getName().trim().isEmpty())
		{
			return "This is a preview of the tile markers and notes that will be visible when using the current strategy.";
		}

		return "This is a preview of the tile markers and notes that will be visible when using the "
				+ escapeHtml(strategy.getName().trim())
				+ " strategy.";
	}

	private static String escapeHtml(String text)
	{
		return text
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

	private JPanel createHeader(String headerText)
	{
		JLabel label = new JLabel("<html>" + headerText + "</html>");
		label.setForeground(ColorScheme.TEXT_COLOR);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createBody(String notes)
	{
		JPanel body = new JPanel(new BorderLayout(10, 0));
		body.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		body.add(createMapArea(), BorderLayout.CENTER);
		body.add(createSidePanel(notes), BorderLayout.EAST);
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
		panel.add(mapScrollPane, BorderLayout.CENTER);
		panel.add(createZoomControls(), BorderLayout.SOUTH);
		return panel;
	}

	private JPanel createZoomControls()
	{
		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(label("Drag Slider or Scroll to Zoom", false), BorderLayout.WEST);

		mapZoom.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		mapZoom.setFocusable(false);
		mapZoom.setMajorTickSpacing(10);
		mapZoom.setMinorTickSpacing(1);
		mapZoom.addChangeListener(event -> resizeMap());
		panel.add(mapZoom, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createSidePanel(String notes)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setPreferredSize(new Dimension(SIDE_WIDTH, MAP_VIEWPORT_HEIGHT));

		JPanel top = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		top.add(TileMarkerLegendPanel.create(SIDE_WIDTH));
		top.add(Box.createVerticalStrut(42));
		top.add(label("Notes", true));
		top.add(Box.createVerticalStrut(5));
		top.add(createNotesPreview(notes));
		panel.add(top, BorderLayout.NORTH);
		return panel;
	}

	private JScrollPane createNotesPreview(String notes)
	{
		JTextArea textArea = new JTextArea(notes == null || notes.trim().isEmpty() ? "No notes configured." : notes);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setBackground(ColorScheme.DARK_GRAY_COLOR);
		textArea.setForeground(ColorScheme.TEXT_COLOR);

		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
		BaPanelUi.fixedSize(scrollPane, SIDE_WIDTH, 240);
		return scrollPane;
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

}
