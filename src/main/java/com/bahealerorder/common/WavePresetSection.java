package com.bahealerorder.common;

import com.bahealerorder.common.strategies.AbstractWaveStrategyManager;
import com.bahealerorder.common.strategies.WaveRunPreset;
import com.bahealerorder.common.strategies.WaveStrategy;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

public class WavePresetSection extends JPanel
{
	public interface Adapter
	{
		String getActivePresetId();

		List<BaPanelUi.ComboOption> getPresetOptions();

		String getActiveWaveStrategyId(int wave);

		List<BaPanelUi.ComboOption> getWaveStrategyOptions(int wave);

		void onPresetSelected(String presetId);

		void onWaveStrategySelected(int wave, String strategyId);

		void onSaveCurrentSelections();

		void onDeletePreset(String presetId);

		void onClearSelections();

		void onImportPreset();

		void onExportPreset(String presetId);
	}

	public static Adapter managerAdapter(
			AbstractWaveStrategyManager<? extends WaveRunPreset, ? extends WaveStrategy, ?, ?> manager,
			Runnable onPresetSelected,
			BiConsumer<Integer, String> onWaveStrategySelected,
			Runnable onSaveCurrentSelections,
			Consumer<String> onDeletePreset,
			Runnable onImportPreset,
			Consumer<String> onExportPreset)
	{
		return new Adapter()
		{
			@Override
			public String getActivePresetId()
			{
				return manager.getActiveRunPresetId();
			}

			@Override
			public List<BaPanelUi.ComboOption> getPresetOptions()
			{
				List<BaPanelUi.ComboOption> items = new ArrayList<>();
				items.add(new BaPanelUi.ComboOption(null, ""));

				for (WaveRunPreset preset : manager.getRunPresets())
				{
					items.add(new BaPanelUi.ComboOption(preset.getId(), preset.getName()));
				}

				return items;
			}

			@Override
			public String getActiveWaveStrategyId(int wave)
			{
				return manager.getActiveWaveStrategyId(wave);
			}

			@Override
			public List<BaPanelUi.ComboOption> getWaveStrategyOptions(int wave)
			{
				List<BaPanelUi.ComboOption> items = new ArrayList<>();
				items.add(new BaPanelUi.ComboOption(null, ""));

				for (WaveStrategy strategy : manager.getWaveStrategiesForWave(wave))
				{
					items.add(new BaPanelUi.ComboOption(strategy.getId(), strategy.getName()));
				}

				return items;
			}

			@Override
			public void onPresetSelected(String presetId)
			{
				if (presetId == null)
				{
					manager.clearActiveSelections();
				}
				else
				{
					manager.applyRunPreset(presetId);
				}
				onPresetSelected.run();
			}

			@Override
			public void onWaveStrategySelected(int wave, String strategyId)
			{
				manager.setActiveWaveStrategyId(wave, strategyId);
				onWaveStrategySelected.accept(wave, strategyId);
			}

			@Override
			public void onSaveCurrentSelections()
			{
				onSaveCurrentSelections.run();
			}

			@Override
			public void onDeletePreset(String presetId)
			{
				onDeletePreset.accept(presetId);
			}

			@Override
			public void onClearSelections()
			{
				manager.clearActiveSelections();
				onPresetSelected.run();
			}

			@Override
			public void onImportPreset()
			{
				onImportPreset.run();
			}

			@Override
			public void onExportPreset(String presetId)
			{
				onExportPreset.accept(presetId);
			}
		};
	}

	private final int contentWidth;
	private final int controlHeight;
	private final int waveLabelWidth;
	private final Adapter adapter;
	private final JComboBox<BaPanelUi.ComboOption> presetCombo = new JComboBox<>();
	private final Map<Integer, JComboBox<BaPanelUi.ComboOption>> waveCombos = new HashMap<>();
	private boolean refreshing;

	public WavePresetSection(String title, int contentWidth, int controlHeight, int waveLabelWidth, Adapter adapter)
	{
		this.contentWidth = contentWidth;
		this.controlHeight = controlHeight;
		this.waveLabelWidth = waveLabelWidth;
		this.adapter = adapter;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setMaximumSize(new Dimension(contentWidth, Integer.MAX_VALUE));
		setAlignmentX(LEFT_ALIGNMENT);

		add(BaPanelUi.centeredLabelRow(title, true, ColorScheme.DARKER_GRAY_COLOR, contentWidth, controlHeight));
		add(Box.createVerticalStrut(6));
		addPresetControls();
		add(Box.createVerticalStrut(26));
		addWaveSelectors();
	}

	public void refreshAll()
	{
		refreshing = true;
		try
		{
			populatePresetCombo();
			populateWaveCombos();
		}
		finally
		{
			refreshing = false;
		}
	}

	public void refreshPresetCombo()
	{
		boolean previousRefreshing = refreshing;
		refreshing = true;
		try
		{
			populatePresetCombo();
		}
		finally
		{
			refreshing = previousRefreshing;
		}
	}

	private void addPresetControls()
	{
		BaPanelUi.styleCombo(presetCombo, contentWidth - 16, controlHeight);
		presetCombo.addActionListener(event ->
		{
			if (refreshing)
			{
				return;
			}

			adapter.onPresetSelected(BaPanelUi.selectedId(presetCombo));
		});
		add(presetCombo);
		add(Box.createVerticalStrut(6));
		add(BaPanelUi.action("Save Current Selections", adapter::onSaveCurrentSelections, contentWidth - 16, controlHeight));
		add(Box.createVerticalStrut(5));

		JPanel presetActionRow = BaPanelUi.horizontalActionRow(contentWidth - 16, controlHeight);
		presetActionRow.add(BaPanelUi.action("Delete", () -> adapter.onDeletePreset(getSelectedPresetId()), contentWidth - 16, controlHeight));
		presetActionRow.add(BaPanelUi.action("Clear", adapter::onClearSelections, contentWidth - 16, controlHeight));
		add(presetActionRow);
		add(Box.createVerticalStrut(5));

		JPanel jsonActionRow = BaPanelUi.horizontalActionRow(contentWidth - 16, controlHeight);
		jsonActionRow.add(BaPanelUi.action("Import", adapter::onImportPreset, contentWidth - 16, controlHeight));
		jsonActionRow.add(BaPanelUi.action("Export", () -> adapter.onExportPreset(getSelectedPresetId()), contentWidth - 16, controlHeight));
		add(jsonActionRow);
	}

	private void addWaveSelectors()
	{
		for (int wave = 1; wave <= 10; wave++)
		{
			JComboBox<BaPanelUi.ComboOption> comboBox = new JComboBox<>();
			final int selectedWave = wave;
			BaPanelUi.styleCombo(comboBox, contentWidth - 16 - waveLabelWidth - 6, controlHeight);
			comboBox.addActionListener(event ->
			{
				if (refreshing)
				{
					return;
				}

				adapter.onWaveStrategySelected(selectedWave, BaPanelUi.selectedId(comboBox));
			});
			waveCombos.put(wave, comboBox);
			add(BaPanelUi.comboRow("Wave " + wave, comboBox, contentWidth, controlHeight, waveLabelWidth));
			add(Box.createVerticalStrut(6));
		}
	}

	private void populatePresetCombo()
	{
		BaPanelUi.setComboItems(presetCombo, adapter.getPresetOptions(), adapter.getActivePresetId());
	}

	private void populateWaveCombos()
	{
		for (Map.Entry<Integer, JComboBox<BaPanelUi.ComboOption>> entry : waveCombos.entrySet())
		{
			int wave = entry.getKey();
			BaPanelUi.setComboItems(entry.getValue(), adapter.getWaveStrategyOptions(wave), adapter.getActiveWaveStrategyId(wave));
		}
	}

	private String getSelectedPresetId()
	{
		return BaPanelUi.selectedId(presetCombo);
	}
}
