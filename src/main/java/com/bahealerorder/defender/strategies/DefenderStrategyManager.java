package com.bahealerorder.defender.strategies;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
public class DefenderStrategyManager
{
	private static final String CONFIG_GROUP = "bahealerorder";
	private static final String STRATEGY_STORE_KEY = "defenderStrategyStore";
	private static final int MIN_MARKER_OPACITY_PERCENT = 0;
	private static final int MAX_MARKER_OPACITY_PERCENT = 100;
	private static final float MIN_MARKER_BORDER_WIDTH = 0f;
	private static final float MAX_MARKER_BORDER_WIDTH = 8f;

	private final ConfigManager configManager;
	private final Gson gson;
	private final DefenderStrategyStore builtIns;

	private DefenderStrategyStore userStore = new DefenderStrategyStore();

	@Inject
	public DefenderStrategyManager(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson.newBuilder().setPrettyPrinting().create();
		this.builtIns = DefenderStrategyLibrary.create();
	}

	DefenderStrategyManager(DefenderStrategyStore userStore, Gson gson)
	{
		this.configManager = null;
		this.gson = gson.newBuilder().setPrettyPrinting().create();
		this.builtIns = DefenderStrategyLibrary.create();
		this.userStore = userStore == null ? new DefenderStrategyStore() : userStore;
	}

	public void load()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, STRATEGY_STORE_KEY);

		if (json == null || json.trim().isEmpty())
		{
			userStore = new DefenderStrategyStore();
			save();
			return;
		}

		try
		{
			userStore = gson.fromJson(json, DefenderStrategyStore.class);
		}
		catch (RuntimeException ex)
		{
			userStore = new DefenderStrategyStore();
		}

		if (userStore == null)
		{
			userStore = new DefenderStrategyStore();
		}
	}

	public void save()
	{
		if (configManager == null)
		{
			return;
		}

		configManager.setConfiguration(CONFIG_GROUP, STRATEGY_STORE_KEY, gson.toJson(userStore));
	}

	public String exportRunPresetJson(String presetId)
	{
		DefenderRunPreset preset = findRunPreset(presetId);

		if (preset == null)
		{
			return null;
		}

		List<DefenderWaveStrategy> waveStrategies = new ArrayList<>();
		Map<Integer, String> exportedWaveStrategyNames = new HashMap<>();

		for (String strategyId : preset.getWaveStrategyIds().values())
		{
			DefenderWaveStrategy strategy = findWaveStrategy(strategyId);

			if (strategy != null)
			{
				exportedWaveStrategyNames.put(strategy.getWave(), strategy.getName());
				waveStrategies.add(exportWaveStrategy(strategy));
			}
		}

		DefenderRunPreset exportedPreset = new DefenderRunPreset(null, preset.getName(), false, exportedWaveStrategyNames);
		return gson.toJson(new DefenderRunPresetExport(exportedPreset, waveStrategies));
	}

	public boolean importRunPresetJson(String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return false;
		}

		try
		{
			DefenderRunPresetExport imported = gson.fromJson(json, DefenderRunPresetExport.class);

			if (imported == null || imported.getPreset() == null || isBlank(imported.getPreset().getName()))
			{
				return false;
			}

			importOrReplaceWaveStrategies(imported.getWaveStrategies());
			DefenderRunPreset importedPreset = importRunPreset(imported.getPreset());

			upsertRunPreset(importedPreset);
			userStore.setActiveRunPresetId(importedPreset.getId());
			userStore.setActiveWaveStrategyIds(importedPreset.getWaveStrategyIds());
			save();
			return true;
		}
		catch (RuntimeException ex)
		{
			return false;
		}
	}

	public String exportWaveStrategyTemplateJson(DefenderWaveStrategy strategy)
	{
		if (strategy == null)
		{
			return null;
		}

		return gson.toJson(DefenderWaveStrategyTemplate.fromStrategy(strategy));
	}

	public DefenderWaveStrategy importWaveStrategyTemplateJson(String json, int wave, String id, boolean builtIn)
	{
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}

		try
		{
			DefenderWaveStrategyTemplate template = gson.fromJson(json, DefenderWaveStrategyTemplate.class);
			return template == null ? null : template.toStrategy(wave, id, builtIn);
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	public String exportMarkerClipboardJson(int wave, List<DefenderMarker> markers)
	{
		if (markers == null || markers.isEmpty())
		{
			return null;
		}

		return gson.toJson(DefenderMarkerClipboard.fromMarkers(wave, markers));
	}

	public List<DefenderMarker> importMarkerClipboardJson(int wave, String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}

		try
		{
			DefenderMarkerClipboard clipboard = gson.fromJson(json, DefenderMarkerClipboard.class);
			return clipboard == null ? null : clipboard.toMarkers(wave);
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	public List<DefenderRunPreset> getRunPresets()
	{
		List<DefenderRunPreset> presets = new ArrayList<>();

		for (DefenderRunPreset builtIn : builtIns.getRunPresets())
		{
			DefenderRunPreset override = findStoredUserRunPreset(builtIn.getId());
			presets.add(override != null && override.isBuiltIn() ? override : builtIn);
		}

		for (DefenderRunPreset userPreset : userStore.getRunPresets())
		{
			if (findBuiltInRunPreset(userPreset.getId()) == null)
			{
				presets.add(userPreset);
			}
		}

		presets.sort(Comparator.comparing(DefenderRunPreset::isBuiltIn).reversed().thenComparing(DefenderRunPreset::getName, String.CASE_INSENSITIVE_ORDER));
		return presets;
	}

	public List<DefenderWaveStrategy> getWaveStrategiesForWave(int wave)
	{
		List<DefenderWaveStrategy> strategies = new ArrayList<>();

		for (DefenderWaveStrategy strategy : getWaveStrategies())
		{
			if (strategy.getWave() == wave)
			{
				strategies.add(strategy);
			}
		}

		strategies.sort(Comparator.comparing(DefenderWaveStrategy::isBuiltIn).reversed().thenComparing(DefenderWaveStrategy::getName, String.CASE_INSENSITIVE_ORDER));
		return strategies;
	}

	public List<DefenderWaveStrategy> getWaveStrategies()
	{
		List<DefenderWaveStrategy> strategies = new ArrayList<>();

		for (DefenderWaveStrategy builtIn : builtIns.getWaveStrategies())
		{
			DefenderWaveStrategy override = findStoredUserWaveStrategy(builtIn.getId());
			strategies.add(override != null && override.isBuiltIn() ? override : builtIn);
		}

		for (DefenderWaveStrategy userStrategy : userStore.getWaveStrategies())
		{
			if (findBuiltInWaveStrategy(userStrategy.getId()) == null)
			{
				strategies.add(userStrategy);
			}
		}

		return strategies;
	}

	public String getActiveRunPresetId()
	{
		DefenderRunPreset preset = findRunPreset(userStore.getActiveRunPresetId());

		if (preset != null && preset.getWaveStrategyIds().equals(userStore.getActiveWaveStrategyIds()))
		{
			return preset.getId();
		}

		preset = findMatchingRunPreset();

		if (preset != null)
		{
			userStore.setActiveRunPresetId(preset.getId());
		}

		return preset == null ? null : preset.getId();
	}

	public DefenderRunPreset findMatchingRunPreset()
	{
		Map<Integer, String> activeStrategies = userStore.getActiveWaveStrategyIds();

		if (activeStrategies.isEmpty())
		{
			return null;
		}

		for (DefenderRunPreset preset : getRunPresets())
		{
			if (preset.getWaveStrategyIds().equals(activeStrategies))
			{
				return preset;
			}
		}

		return null;
	}

	public DefenderRunPreset findRunPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (DefenderRunPreset preset : getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	public DefenderWaveStrategy findWaveStrategy(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (DefenderWaveStrategy strategy : getWaveStrategies())
		{
			if (id.equals(strategy.getId()))
			{
				return strategy;
			}
		}

		return null;
	}

	public DefenderWaveStrategy findBuiltInWaveStrategy(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (DefenderWaveStrategy strategy : builtIns.getWaveStrategies())
		{
			if (id.equals(strategy.getId()))
			{
				return strategy;
			}
		}

		return null;
	}

	public DefenderWaveStrategy getActiveWaveStrategy(int wave)
	{
		DefenderWaveStrategy selected = findWaveStrategy(getActiveWaveStrategyId(wave));

		if (selected != null)
		{
			return selected;
		}

		DefenderRunPreset preset = findRunPreset(getActiveRunPresetId());

		if (preset != null)
		{
			return findWaveStrategy(preset.getWaveStrategyId(wave));
		}

		return null;
	}

	public String getActiveWaveStrategyId(int wave)
	{
		return userStore.getActiveWaveStrategyIds().get(wave);
	}

	public Map<Integer, String> getActiveWaveStrategyIds()
	{
		return new HashMap<>(userStore.getActiveWaveStrategyIds());
	}

	public void setActiveWaveStrategyId(int wave, String strategyId)
	{
		userStore.setActiveRunPresetId(null);

		if (strategyId == null || strategyId.trim().isEmpty())
		{
			userStore.getActiveWaveStrategyIds().remove(wave);
		}
		else
		{
			userStore.getActiveWaveStrategyIds().put(wave, strategyId);
		}

		save();
	}

	public void applyRunPreset(String presetId)
	{
		userStore.setActiveRunPresetId(presetId);
		DefenderRunPreset preset = findRunPreset(presetId);

		if (preset != null)
		{
			userStore.setActiveWaveStrategyIds(preset.getWaveStrategyIds());
		}

		save();
	}

	public void clearActiveSelections()
	{
		userStore.setActiveRunPresetId(null);
		userStore.getActiveWaveStrategyIds().clear();
		save();
	}

	public String getLastMarkerColor()
	{
		return userStore.getLastMarkerColor();
	}

	public int getLastMarkerOpacityPercent()
	{
		return clampOpacity(userStore.getLastMarkerOpacityPercent());
	}

	public float getLastMarkerBorderWidth()
	{
		return clampBorderWidth(userStore.getLastMarkerBorderWidth());
	}

	public void setLastMarkerStyle(String color, int opacityPercent, float borderWidth)
	{
		userStore.setLastMarkerColor(color);
		userStore.setLastMarkerOpacityPercent(clampOpacity(opacityPercent));
		userStore.setLastMarkerBorderWidth(clampBorderWidth(borderWidth));
		save();
	}

	public DefenderRunPreset createUserPreset(String name, Map<Integer, String> waveStrategyIds)
	{
		DefenderRunPreset existingPreset = findRunPresetByName(name);
		boolean builtIn = existingPreset != null && existingPreset.isBuiltIn();
		DefenderRunPreset preset = new DefenderRunPreset(
				existingPreset == null ? userId("preset", name) : existingPreset.getId(),
				name,
				builtIn,
				new HashMap<>(waveStrategyIds)
		);
		upsertRunPreset(preset);
		userStore.setActiveRunPresetId(preset.getId());
		save();
		return preset;
	}

	public DefenderRunPreset createUserPresetFromActive(String name)
	{
		return createUserPreset(name, userStore.getActiveWaveStrategyIds());
	}

	public boolean deleteUserPreset(String id)
	{
		if (id == null)
		{
			return false;
		}

		boolean removed = false;
		List<DefenderRunPreset> presets = new ArrayList<>();

		for (DefenderRunPreset preset : userStore.getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				removed = true;
			}
			else
			{
				presets.add(preset);
			}
		}

		if (!removed)
		{
			return false;
		}

		userStore.setRunPresets(presets);

		if (id.equals(userStore.getActiveRunPresetId()))
		{
			userStore.setActiveRunPresetId(null);
			userStore.getActiveWaveStrategyIds().clear();
		}

		save();
		return true;
	}

	public boolean updateUserPreset(String id, String name, Map<Integer, String> waveStrategyIds)
	{
		if (id == null || isBlank(name))
		{
			return false;
		}

		DefenderRunPreset builtIn = findBuiltInRunPreset(id);

		if (builtIn != null)
		{
			upsertRunPreset(new DefenderRunPreset(id, name, true, waveStrategyIds));
			save();
			return true;
		}

		for (DefenderRunPreset preset : userStore.getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				preset.setName(name);
				preset.setWaveStrategyIds(new HashMap<>(waveStrategyIds));
				save();
				return true;
			}
		}

		return false;
	}

	public DefenderWaveStrategy createUserWaveStrategy(DefenderWaveStrategy strategy)
	{
		DefenderWaveStrategy stored = importOrReplaceWaveStrategy(withUserIdentity(strategy, null, false));
		save();
		return stored;
	}

	public boolean updateUserWaveStrategy(String id, DefenderWaveStrategy strategy)
	{
		if (id == null || strategy == null || isBlank(strategy.getName()))
		{
			return false;
		}

		DefenderWaveStrategy updated = withUserIdentity(strategy, id, false);

		for (int i = 0; i < userStore.getWaveStrategies().size(); i++)
		{
			if (id.equals(userStore.getWaveStrategies().get(i).getId()))
			{
				List<DefenderWaveStrategy> strategies = new ArrayList<>(userStore.getWaveStrategies());
				strategies.set(i, updated);
				userStore.setWaveStrategies(strategies);
				save();
				return true;
			}
		}

		return false;
	}

	public boolean updateBuiltInWaveStrategy(String id, DefenderWaveStrategy strategy)
	{
		DefenderWaveStrategy builtIn = findBuiltInWaveStrategy(id);

		if (builtIn == null || strategy == null || isBlank(strategy.getName()))
		{
			return false;
		}

		DefenderWaveStrategy updated = new DefenderWaveStrategy(
				builtIn.getId(),
				builtIn.getName(),
				builtIn.getWave(),
				true,
				strategy.getNotes(),
				strategy.getNumberOfLogs(),
				strategy.getMarkers()
		);
		List<DefenderWaveStrategy> strategies = new ArrayList<>(userStore.getWaveStrategies());

		for (int i = 0; i < strategies.size(); i++)
		{
			if (id.equals(strategies.get(i).getId()))
			{
				strategies.set(i, updated);
				userStore.setWaveStrategies(strategies);
				save();
				return true;
			}
		}

		strategies.add(updated);
		userStore.setWaveStrategies(strategies);
		save();
		return true;
	}

	public boolean resetBuiltInWaveStrategy(String id)
	{
		if (findBuiltInWaveStrategy(id) == null)
		{
			return false;
		}

		boolean removed = false;
		List<DefenderWaveStrategy> strategies = new ArrayList<>();

		for (DefenderWaveStrategy strategy : userStore.getWaveStrategies())
		{
			if (id.equals(strategy.getId()))
			{
				removed = true;
			}
			else
			{
				strategies.add(strategy);
			}
		}

		if (!removed)
		{
			return false;
		}

		userStore.setWaveStrategies(strategies);
		save();
		return true;
	}

	public boolean deleteUserWaveStrategy(String id)
	{
		if (id == null)
		{
			return false;
		}

		boolean removed = false;
		List<DefenderWaveStrategy> strategies = new ArrayList<>();

		for (DefenderWaveStrategy strategy : userStore.getWaveStrategies())
		{
			if (id.equals(strategy.getId()))
			{
				removed = true;
			}
			else
			{
				strategies.add(strategy);
			}
		}

		if (!removed)
		{
			return false;
		}

		userStore.setWaveStrategies(strategies);
		userStore.getActiveWaveStrategyIds().values().removeIf(id::equals);

		for (DefenderRunPreset preset : userStore.getRunPresets())
		{
			preset.getWaveStrategyIds().values().removeIf(id::equals);
		}

		save();
		return true;
	}

	private DefenderWaveStrategy exportWaveStrategy(DefenderWaveStrategy strategy)
	{
		return copy(strategy, null, false);
	}

	private void importOrReplaceWaveStrategies(List<DefenderWaveStrategy> strategies)
	{
		for (DefenderWaveStrategy strategy : strategies)
		{
			if (strategy == null || isBlank(strategy.getName()))
			{
				continue;
			}

			importOrReplaceWaveStrategy(strategy);
		}
	}

	private DefenderWaveStrategy importOrReplaceWaveStrategy(DefenderWaveStrategy imported)
	{
		DefenderWaveStrategy existingUserStrategy = findStoredUserWaveStrategy(imported.getWave(), imported.getName());
		DefenderWaveStrategy builtInStrategy = findBuiltInWaveStrategy(imported.getWave(), imported.getName());
		boolean replacingBuiltIn = builtInStrategy != null
				&& (existingUserStrategy == null || builtInStrategy.getId().equals(existingUserStrategy.getId()));
		String id;

		if (replacingBuiltIn)
		{
			id = builtInStrategy.getId();
		}
		else if (existingUserStrategy != null)
		{
			id = existingUserStrategy.getId();
		}
		else
		{
			id = userId("wave", imported.getWave() + "-" + imported.getName());
		}

		boolean builtIn = replacingBuiltIn || (existingUserStrategy != null && existingUserStrategy.isBuiltIn());
		DefenderWaveStrategy updated = copy(imported, id, builtIn);
		List<DefenderWaveStrategy> storedStrategies = new ArrayList<>(userStore.getWaveStrategies());

		for (int i = 0; i < storedStrategies.size(); i++)
		{
			DefenderWaveStrategy stored = storedStrategies.get(i);

			if (sameWaveStrategyName(stored, imported.getWave(), imported.getName()))
			{
				storedStrategies.set(i, updated);
				userStore.setWaveStrategies(storedStrategies);
				return updated;
			}
		}

		storedStrategies.add(updated);
		userStore.setWaveStrategies(storedStrategies);
		return updated;
	}

	private DefenderRunPreset importRunPreset(DefenderRunPreset importedPreset)
	{
		Map<Integer, String> localStrategyIds = new HashMap<>();

		for (Map.Entry<Integer, String> entry : importedPreset.getWaveStrategyIds().entrySet())
		{
			Integer wave = entry.getKey();
			String importedReference = entry.getValue();

			if (wave == null || isBlank(importedReference))
			{
				continue;
			}

			DefenderWaveStrategy strategy = findWaveStrategy(wave, importedReference);

			if (strategy != null)
			{
				localStrategyIds.put(wave, strategy.getId());
			}
		}

		DefenderRunPreset existingPreset = findRunPresetByName(importedPreset.getName());
		String id = existingPreset == null ? userId("preset", importedPreset.getName()) : existingPreset.getId();
		boolean builtIn = existingPreset != null && existingPreset.isBuiltIn();
		return new DefenderRunPreset(id, importedPreset.getName(), builtIn, localStrategyIds);
	}

	private void upsertRunPreset(DefenderRunPreset importedPreset)
	{
		List<DefenderRunPreset> presets = new ArrayList<>(userStore.getRunPresets());

		for (int i = 0; i < presets.size(); i++)
		{
			if (sameName(importedPreset.getName(), presets.get(i).getName()))
			{
				presets.set(i, importedPreset);
				userStore.setRunPresets(presets);
				return;
			}
		}

		presets.add(importedPreset);
		userStore.setRunPresets(presets);
	}

	private DefenderRunPreset findStoredUserRunPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (DefenderRunPreset preset : userStore.getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	private DefenderRunPreset findBuiltInRunPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (DefenderRunPreset preset : builtIns.getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	private DefenderWaveStrategy withUserIdentity(DefenderWaveStrategy strategy, String id, boolean builtIn)
	{
		String strategyId = id == null ? userId("wave", strategy.getWave() + "-" + strategy.getName()) : id;
		return copy(strategy, strategyId, builtIn);
	}

	private DefenderWaveStrategy copy(DefenderWaveStrategy strategy, String id, boolean builtIn)
	{
		return new DefenderWaveStrategy(
				id,
				strategy.getName(),
				strategy.getWave(),
				builtIn,
				strategy.getNotes(),
				strategy.getNumberOfLogs(),
				strategy.getMarkers()
		);
	}

	private DefenderWaveStrategy findStoredUserWaveStrategy(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (DefenderWaveStrategy strategy : userStore.getWaveStrategies())
		{
			if (id.equals(strategy.getId()))
			{
				return strategy;
			}
		}

		return null;
	}

	private DefenderWaveStrategy findStoredUserWaveStrategy(int wave, String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		for (DefenderWaveStrategy strategy : userStore.getWaveStrategies())
		{
			if (sameWaveStrategyName(strategy, wave, name))
			{
				return strategy;
			}
		}

		return null;
	}

	private DefenderWaveStrategy findBuiltInWaveStrategy(int wave, String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		for (DefenderWaveStrategy strategy : builtIns.getWaveStrategies())
		{
			if (sameWaveStrategyName(strategy, wave, name))
			{
				return strategy;
			}
		}

		return null;
	}

	private DefenderWaveStrategy findWaveStrategy(int wave, String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		DefenderWaveStrategy userStrategy = findStoredUserWaveStrategy(wave, name);
		DefenderWaveStrategy builtInStrategy = findBuiltInWaveStrategy(wave, name);

		if (userStrategy != null
				&& (builtInStrategy == null || !builtInStrategy.getId().equals(userStrategy.getId()) || userStrategy.isBuiltIn()))
		{
			return userStrategy;
		}

		return builtInStrategy;
	}

	private DefenderRunPreset findRunPresetByName(String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		for (DefenderRunPreset preset : getRunPresets())
		{
			if (sameName(name, preset.getName()))
			{
				return preset;
			}
		}

		return null;
	}

	private boolean sameWaveStrategyName(DefenderWaveStrategy strategy, int wave, String name)
	{
		return strategy != null
				&& strategy.getWave() == wave
				&& sameName(strategy.getName(), name);
	}

	private static boolean sameName(String first, String second)
	{
		return normalizeName(first).equals(normalizeName(second));
	}

	private static String normalizeName(String name)
	{
		return name == null ? "" : name.trim().toLowerCase();
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}

	private static int clampOpacity(int opacityPercent)
	{
		return Math.max(MIN_MARKER_OPACITY_PERCENT, Math.min(MAX_MARKER_OPACITY_PERCENT, opacityPercent));
	}

	private static float clampBorderWidth(float borderWidth)
	{
		return Math.max(MIN_MARKER_BORDER_WIDTH, Math.min(MAX_MARKER_BORDER_WIDTH, borderWidth));
	}

	private String userId(String type, String name)
	{
		String cleanName = name == null ? "strategy" : name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return "user:defender:" + type + ":" + cleanName;
	}
}
