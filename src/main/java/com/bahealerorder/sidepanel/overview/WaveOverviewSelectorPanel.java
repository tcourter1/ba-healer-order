package com.bahealerorder.sidepanel.overview;

import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.BaIcons;
import com.bahealerorder.common.BaWaveInfo;
import com.bahealerorder.common.BaWaveOverviewRun;
import com.bahealerorder.common.BaWaveOverviewSnapshot;
import com.bahealerorder.common.BaWaveOverviewStore;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

class WaveOverviewSelectorPanel extends JPanel
{
	private static final Object OPEN_RUN_FOLDER_ACTION = new Object();
	private static final int CONTROL_HEIGHT = 24;
	private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
	private static final int SELECTOR_WIDTH = CONTENT_WIDTH - 16;
	private static final int ACTION_BUTTON_WIDTH = CONTROL_HEIGHT + 4;
	private static final int RUN_DROPDOWN_LIMIT = 10;
	private static final int RUN_STATUS_HTML_WIDTH = 64;
	private static final DateTimeFormatter RUN_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final BaWaveOverviewStore store;
	private final Runnable onSelectionChanged;
	private final FixedPopupWidthComboBox<SelectorItem> runCombo = new FixedPopupWidthComboBox<>(SELECTOR_WIDTH);
	private final JComboBox<SelectorItem> waveCombo = new JComboBox<>();
	private final JButton deleteRunButton = new JButton();

	private boolean refreshingControls;

	WaveOverviewSelectorPanel(
			BaWaveOverviewStore store,
			Supplier<JButton> columnMenuButtonSupplier,
			Runnable onSelectionChanged)
	{
		this.store = store;
		this.onSelectionChanged = onSelectionChanged;

		SelectorItemRenderer renderer = new SelectorItemRenderer();
		runCombo.setRenderer(renderer);
		waveCombo.setRenderer(renderer);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setPreferredSize(new Dimension(SELECTOR_WIDTH, CONTROL_HEIGHT * 2 + 6));
		setMaximumSize(new Dimension(SELECTOR_WIDTH, CONTROL_HEIGHT * 2 + 6));
		setAlignmentX(LEFT_ALIGNMENT);

		runCombo.addActionListener(event ->
		{
			if (refreshingControls) return;

			SelectorItem item = (SelectorItem) runCombo.getSelectedItem();
			if (item != null && item.value == OPEN_RUN_FOLDER_ACTION)
			{
				openSavedRunFolder();
				refreshSelectors();
				return;
			}

			String selectedRunId = item == null ? null : (String) item.value;
			if (!java.util.Objects.equals(selectedRunId, store.getSelectedRunId()))
			{
				store.setSelectedRunId(selectedRunId);
				store.setSelectedWave(-1);
			}
			notifySelectionChanged();
		});

		waveCombo.addActionListener(event ->
		{
			if (refreshingControls) return;

			SelectorItem item = (SelectorItem) waveCombo.getSelectedItem();
			store.setSelectedWave(item == null ? -1 : (Integer) item.value);
			notifySelectionChanged();
		});

		add(createRunSelectorRow());
		add(Box.createVerticalStrut(6));
		add(createWaveSelectorRow(columnMenuButtonSupplier.get()));
	}

	void refreshSelectors()
	{
		refreshingControls = true;

		DefaultComboBoxModel<SelectorItem> runModel = new DefaultComboBoxModel<>();
		List<BaWaveOverviewRun> runs = store.getRuns();
		for (int i = 0; i < Math.min(RUN_DROPDOWN_LIMIT, runs.size()); i++)
		{
			BaWaveOverviewRun run = runs.get(i);
			runModel.addElement(new SelectorItem(run.getId(), formatRunDropdownLabel(run), formatSelectedRunLabel(run)));
		}
		if (runModel.getSize() == 0)
		{
			runModel.addElement(new SelectorItem(null, " ", ""));
		}
		runModel.addElement(new SelectorItem(OPEN_RUN_FOLDER_ACTION, "Open saved run folder..."));

		runCombo.setModel(runModel);
		SelectorItem selectedRunItem = getSelectedRunItem(runModel, store.getSelectedRunId());
		if (selectedRunItem.value == null && store.getSelectedRunId() != null)
		{
			store.setSelectedRunId(null);
		}
		else if (selectedRunItem.value instanceof String && !selectedRunItem.value.equals(store.getSelectedRunId()))
		{
			store.setSelectedRunId((String) selectedRunItem.value);
		}
		runCombo.setSelectedItem(selectedRunItem);
		deleteRunButton.setEnabled(selectedRunItem.value != null);
		styleCombo(runCombo, SELECTOR_WIDTH - ACTION_BUTTON_WIDTH - 6);

		DefaultComboBoxModel<SelectorItem> waveModel = new DefaultComboBoxModel<>();
		waveModel.addElement(waveItem(-1, null));
		BaWaveOverviewRun selectedRun = store.getSelectedRun();
		for (int wave = 1; wave <= 10; wave++)
		{
			waveModel.addElement(waveItem(wave, getWaveDropdownDuration(selectedRun, wave)));
		}

		waveCombo.setModel(waveModel);
		waveCombo.setSelectedItem(waveItem(store.getSelectedWave(), null));
		styleCombo(waveCombo, SELECTOR_WIDTH - ACTION_BUTTON_WIDTH - 6);

		refreshingControls = false;
	}

	private JPanel createRunSelectorRow()
	{
		JPanel row = selectorRow();
		row.add(runCombo, BorderLayout.CENTER);
		row.add(createDeleteRunButton(), BorderLayout.EAST);
		return row;
	}

	private JPanel createWaveSelectorRow(JButton columnMenuButton)
	{
		JPanel row = selectorRow();
		row.add(waveCombo, BorderLayout.CENTER);
		row.add(columnMenuButton, BorderLayout.EAST);
		return row;
	}

	private JPanel selectorRow()
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setPreferredSize(new Dimension(SELECTOR_WIDTH, CONTROL_HEIGHT));
		row.setMaximumSize(new Dimension(SELECTOR_WIDTH, CONTROL_HEIGHT));
		row.setAlignmentX(LEFT_ALIGNMENT);
		return row;
	}

	private JButton createDeleteRunButton()
	{
		deleteRunButton.setIcon(BaIcons.trashIcon());
		deleteRunButton.setToolTipText("Delete selected run");
		SwingUtil.removeButtonDecorations(deleteRunButton);
		fixedSize(deleteRunButton, ACTION_BUTTON_WIDTH, CONTROL_HEIGHT);
		deleteRunButton.addActionListener(event -> deleteSelectedRun());
		return deleteRunButton;
	}

	private void deleteSelectedRun()
	{
		BaWaveOverviewRun run = store.getSelectedRun();
		if (run == null) return;

		if (!confirmDeleteRun()) return;

		if (store.deleteRun(run.getId()))
		{
			notifySelectionChanged();
		}
	}

	private boolean confirmDeleteRun()
	{
		JOptionPane pane = new JOptionPane(
				"Delete this wave overview run?",
				JOptionPane.WARNING_MESSAGE,
				JOptionPane.YES_NO_OPTION);
		JDialog dialog = pane.createDialog(this, "Delete run");
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);

		Object value = pane.getValue();
		return value instanceof Integer && (Integer) value == JOptionPane.YES_OPTION;
	}

	private void openSavedRunFolder()
	{
		File directory = store.getRunsDirectory();
		if (directory == null)
		{
			showOpenFolderError("Saved run folder is not available.");
			return;
		}

		try
		{
			if (!directory.exists() && !directory.mkdirs())
			{
				showOpenFolderError("Could not create saved run folder.");
				return;
			}

			if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
			{
				showOpenFolderError("Opening folders is not supported on this system.");
				return;
			}

			Desktop.getDesktop().open(directory);
		}
		catch (IOException | RuntimeException ex)
		{
			showOpenFolderError("Could not open saved run folder.");
		}
	}

	private void showOpenFolderError(String message)
	{
		JOptionPane.showMessageDialog(this, message, "Open saved run folder", JOptionPane.ERROR_MESSAGE);
	}

	private void notifySelectionChanged()
	{
		onSelectionChanged.run();
	}

	private SelectorItem getSelectedRunItem(DefaultComboBoxModel<SelectorItem> model, String selectedRunId)
	{
		SelectorItem fallback = model.getElementAt(0);

		for (int i = 0; i < model.getSize(); i++)
		{
			SelectorItem item = model.getElementAt(i);
			if (selectedRunId == null ? item.value == null : selectedRunId.equals(item.value))
			{
				if (selectedRunId != null || item.value instanceof String)
				{
					return item;
				}
			}
		}

		for (int i = 0; i < model.getSize(); i++)
		{
			SelectorItem item = model.getElementAt(i);
			if (item.value instanceof String)
			{
				return item;
			}
		}

		return fallback;
	}

	private SelectorItem waveItem(int wave, String duration)
	{
		if (!BaWaveInfo.isValidWave(wave)) return new SelectorItem(-1, "Select a wave...");

		String label = duration == null || duration.isEmpty()
				? "Wave " + wave
				: "Wave " + wave + " (" + duration + ")";
		return new SelectorItem(wave, label, "Wave " + wave);
	}

	private String getWaveDropdownDuration(BaWaveOverviewRun run, int wave)
	{
		if (run == null) return null;

		BaWaveOverviewSnapshot snapshot = run.getSnapshot(wave);
		if (snapshot == null || snapshot.getDuration() == null || snapshot.getDuration().isEmpty()) return null;

		return formatWaveDropdownDuration(snapshot.getDuration());
	}

	private String formatWaveDropdownDuration(String duration)
	{
		return duration.startsWith("0:") ? duration.substring(2) : duration;
	}

	private String formatRunDropdownLabel(BaWaveOverviewRun run)
	{
		if (run == null) return "";

		String role = getRunRole(run);
		String status = run.isComplete() ? run.getRoundDuration() : "Incomplete";
		String age = formatRunDropdownAge(run);
		return "<html><table width=\"" + SELECTOR_WIDTH + "\" cellpadding=\"0\" cellspacing=\"0\"><tr>"
				+ "<td>" + formatRoleHtml(role) + formatRunAgeHtml(age) + "</td>"
				+ "<td width=\"" + RUN_STATUS_HTML_WIDTH + "\" align=\"right\">" + escapeHtml(status) + "</td>"
				+ "</tr></table></html>";
	}

	private String formatSelectedRunLabel(BaWaveOverviewRun run)
	{
		if (run == null) return "";

		String age = formatRunDropdownAge(run);
		return "<html>" + formatRoleHtml(getRunRole(run)) + formatRunAgeHtml(age) + "</html>";
	}

	private String formatRunAgeHtml(String age)
	{
		return age == null || age.isEmpty() ? "" : " <i>" + escapeHtml(age) + "</i>";
	}

	private String formatRunDropdownAge(BaWaveOverviewRun run)
	{
		if (run.isCurrent())
		{
			return run.isComplete() ? "" : "in progress";
		}

		String age = formatRunAge(run.getName());
		return age;
	}

	private String getRunRole(BaWaveOverviewRun run)
	{
		return run.getPlayerRole() == null || run.getPlayerRole().isEmpty() ? "Unknown" : run.getPlayerRole();
	}

	private String formatRoleHtml(String roleName)
	{
		BaRole role = BaRole.fromDisplayName(roleName);
		String escapedRoleName = escapeHtml(roleName == null || roleName.isEmpty() ? "Unknown" : roleName);
		String color = getRoleHtmlColor(role);
		return color == null ? escapedRoleName : "<font color=\"" + color + "\">" + escapedRoleName + "</font>";
	}

	private String getRoleHtmlColor(BaRole role)
	{
		if (role == null) return null;

		switch (role)
		{
			case ATTACKER:
				return "#ff5a5a";
			case HEALER:
				return "#55d96a";
			case DEFENDER:
				return "#65a7ff";
			case COLLECTOR:
				return "#ffd84d";
			default:
				return null;
		}
	}

	private String formatRunAge(String runName)
	{
		if (runName == null || runName.isEmpty()) return "";

		try
		{
			Duration age = Duration.between(LocalDateTime.parse(runName, RUN_TIME_FORMAT), LocalDateTime.now());

			if (age.isNegative()) return "";

			long minutes = age.toMinutes();
			if (minutes < 60) return minutes + "m";

			long hours = age.toHours();
			if (hours < 24) return hours + "h";

			long days = age.toDays();
			if (days < 14) return days + "d";

			long weeks = days / 7;
			if (weeks < 8) return weeks + "w";

			long months = days / 30;
			if (months < 10) return Math.max(1, months) + "mo";

			long years = days / 365;
			return Math.max(1, years) + "y";
		}
		catch (DateTimeParseException ex)
		{
			return runName;
		}
	}

	private String escapeHtml(String text)
	{
		return text
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}

	private static void styleCombo(JComboBox<?> comboBox, int width)
	{
		comboBox.setFocusable(false);
		fixedSize(comboBox, width, CONTROL_HEIGHT);
	}

	private static void fixedSize(JComponent component, int width, int height)
	{
		Dimension size = new Dimension(width, height);
		component.setPreferredSize(size);
		component.setMinimumSize(size);
		component.setMaximumSize(size);
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	private static class FixedPopupWidthComboBox<T> extends JComboBox<T>
	{
		private final int popupWidth;
		private boolean layingOut;

		private FixedPopupWidthComboBox(int popupWidth)
		{
			this.popupWidth = popupWidth;
		}

		@Override
		public void doLayout()
		{
			try
			{
				layingOut = true;
				super.doLayout();
			}
			finally
			{
				layingOut = false;
			}
		}

		@Override
		public Dimension getSize()
		{
			Dimension size = super.getSize();
			if (!layingOut)
			{
				size.width = Math.max(size.width, popupWidth);
			}

			return size;
		}
	}

	private static class SelectorItemRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(
				JList<?> list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus)
		{
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value instanceof SelectorItem)
			{
				SelectorItem item = (SelectorItem) value;
				label.setText(index == -1 ? item.selectedLabel : item.label);
			}

			Dimension preferredSize = label.getPreferredSize();
			label.setPreferredSize(new Dimension(preferredSize.width, CONTROL_HEIGHT));

			return label;
		}
	}

	private static class SelectorItem
	{
		private final Object value;
		private final String label;
		private final String selectedLabel;

		private SelectorItem(Object value, String label)
		{
			this(value, label, label);
		}

		private SelectorItem(Object value, String label, String selectedLabel)
		{
			this.value = value;
			this.label = label;
			this.selectedLabel = selectedLabel;
		}

		@Override
		public String toString()
		{
			return label;
		}

		@Override
		public boolean equals(Object other)
		{
			return other instanceof SelectorItem && java.util.Objects.equals(value, ((SelectorItem) other).value);
		}

		@Override
		public int hashCode()
		{
			return java.util.Objects.hashCode(value);
		}
	}
}
