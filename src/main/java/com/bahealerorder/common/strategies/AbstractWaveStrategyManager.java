package com.bahealerorder.common.strategies;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.config.ConfigManager;

public abstract class AbstractWaveStrategyManager<
		P extends WaveRunPreset,
		S extends WaveStrategy,
		STORE extends WaveStrategyStore<P, S>,
		EXPORT>
{
	private final ConfigManager configManager;
	protected final Gson gson;
	private final String configGroup;
	private final String strategyStoreKey;
	private final String userIdNamespace;
	protected final STORE builtIns;
	protected STORE userStore;

	protected AbstractWaveStrategyManager(
			ConfigManager configManager,
			Gson gson,
			String configGroup,
			String strategyStoreKey,
			String userIdNamespace,
			STORE builtIns)
	{
		this.configManager = configManager;
		this.gson = gson.newBuilder().setPrettyPrinting().create();
		this.configGroup = configGroup;
		this.strategyStoreKey = strategyStoreKey;
		this.userIdNamespace = userIdNamespace == null ? "" : userIdNamespace;
		this.builtIns = builtIns == null ? newStore() : builtIns;
		this.userStore = newStore();
	}

	protected abstract STORE newStore();

	protected abstract S copyStrategy(S strategy, String id, boolean builtIn);

	protected abstract P createRunPreset(String id, String name, boolean builtIn, Map<Integer, String> waveStrategyIds);

	protected abstract EXPORT createExport(P preset, List<S> strategies);

	protected abstract Class<EXPORT> exportType();

	protected abstract P getExportPreset(EXPORT export);

	protected abstract List<S> getExportStrategies(EXPORT export);

	protected void setUserStore(STORE userStore)
	{
		this.userStore = userStore == null ? newStore() : userStore;
	}

	@SuppressWarnings("unchecked")
	public void load()
	{
		if (configManager == null)
		{
			return;
		}

		String json = configManager.getConfiguration(configGroup, strategyStoreKey);

		if (isBlank(json))
		{
			userStore = newStore();
			save();
			return;
		}

		try
		{
			userStore = (STORE) gson.fromJson(json, newStore().getClass());
		}
		catch (RuntimeException ex)
		{
			userStore = newStore();
		}

		if (userStore == null)
		{
			userStore = newStore();
		}
	}

	public void save()
	{
		if (configManager == null)
		{
			return;
		}

		configManager.setConfiguration(configGroup, strategyStoreKey, gson.toJson(userStore));
	}

	public String exportRunPresetJson(String presetId)
	{
		P preset = findRunPreset(presetId);

		if (preset == null)
		{
			return null;
		}

		List<S> strategies = new ArrayList<>();
		Map<Integer, String> exportedStrategyNames = new HashMap<>();

		for (String strategyId : preset.getWaveStrategyIds().values())
		{
			S strategy = findWaveStrategy(strategyId);

			if (strategy != null)
			{
				exportedStrategyNames.put(strategy.getWave(), strategy.getName());
				strategies.add(exportWaveStrategy(strategy));
			}
		}

		P exportedPreset = createRunPreset(null, preset.getName(), false, exportedStrategyNames);
		return gson.toJson(createExport(exportedPreset, strategies));
	}

	public boolean importRunPresetJson(String json)
	{
		if (isBlank(json))
		{
			return false;
		}

		try
		{
			EXPORT imported = gson.fromJson(json, exportType());
			P importedExportPreset = imported == null ? null : getExportPreset(imported);

			if (importedExportPreset == null || isBlank(importedExportPreset.getName()))
			{
				return false;
			}

			importOrReplaceWaveStrategies(getExportStrategies(imported));
			P importedPreset = importRunPreset(importedExportPreset);

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

	public List<P> getRunPresets()
	{
		List<P> presets = new ArrayList<>();

		for (P builtIn : builtIns.getRunPresets())
		{
			P override = findStoredUserRunPreset(builtIn.getId());
			presets.add(override != null && override.isBuiltIn() ? override : builtIn);
		}

		for (P userPreset : userStore.getRunPresets())
		{
			if (findBuiltInRunPreset(userPreset.getId()) == null)
			{
				presets.add(userPreset);
			}
		}

		presets.sort(Comparator.comparing((P preset) -> preset.isBuiltIn()).reversed()
				.thenComparing(preset -> safeName(preset.getName()), String.CASE_INSENSITIVE_ORDER));
		return presets;
	}

	public List<S> getWaveStrategiesForWave(int wave)
	{
		List<S> strategies = new ArrayList<>();

		for (S strategy : getWaveStrategies())
		{
			if (strategy.getWave() == wave)
			{
				strategies.add(strategy);
			}
		}

		strategies.sort(Comparator.comparing((S strategy) -> strategy.isBuiltIn()).reversed()
				.thenComparing(strategy -> safeName(strategy.getName()), String.CASE_INSENSITIVE_ORDER));
		return strategies;
	}

	public List<S> getWaveStrategies()
	{
		List<S> strategies = new ArrayList<>();

		for (S builtIn : builtIns.getWaveStrategies())
		{
			S override = findStoredUserWaveStrategy(builtIn.getId());
			strategies.add(override != null && override.isBuiltIn() ? override : builtIn);
		}

		for (S userStrategy : userStore.getWaveStrategies())
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
		P preset = findRunPreset(userStore.getActiveRunPresetId());

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

	public void setActiveRunPresetId(String activeRunPresetId)
	{
		userStore.setActiveRunPresetId(activeRunPresetId);
		save();
	}

	public P findMatchingRunPreset()
	{
		Map<Integer, String> activeStrategies = userStore.getActiveWaveStrategyIds();

		if (activeStrategies.isEmpty())
		{
			return null;
		}

		for (P preset : getRunPresets())
		{
			if (preset.getWaveStrategyIds().equals(activeStrategies))
			{
				return preset;
			}
		}

		return null;
	}

	public P findRunPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (P preset : getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	public S findWaveStrategy(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (S strategy : getWaveStrategies())
		{
			if (id.equals(strategy.getId()))
			{
				return strategy;
			}
		}

		return null;
	}

	public S findBuiltInWaveStrategy(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (S strategy : builtIns.getWaveStrategies())
		{
			if (id.equals(strategy.getId()))
			{
				return strategy;
			}
		}

		return null;
	}

	public S getActiveWaveStrategy(int wave)
	{
		S selected = findWaveStrategy(getActiveWaveStrategyId(wave));

		if (selected != null)
		{
			return selected;
		}

		P preset = findRunPreset(getActiveRunPresetId());

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

		if (isBlank(strategyId))
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
		P preset = findRunPreset(presetId);

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

	public P createUserPreset(String name, Map<Integer, String> waveStrategyIds)
	{
		P existingPreset = findRunPresetByName(name);
		boolean builtIn = existingPreset != null && existingPreset.isBuiltIn();
		P preset = createRunPreset(
				existingPreset == null ? userId("preset", name) : existingPreset.getId(),
				name,
				builtIn,
				waveStrategyIds == null ? new HashMap<>() : new HashMap<>(waveStrategyIds)
		);
		upsertRunPreset(preset);
		userStore.setActiveRunPresetId(preset.getId());
		save();
		return preset;
	}

	public P createUserPresetFromActive(String name)
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
		List<P> presets = new ArrayList<>();

		for (P preset : userStore.getRunPresets())
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

		P builtIn = findBuiltInRunPreset(id);
		Map<Integer, String> strategyIds = waveStrategyIds == null ? new HashMap<>() : new HashMap<>(waveStrategyIds);

		if (builtIn != null)
		{
			upsertRunPreset(createRunPreset(id, name, true, strategyIds));
			save();
			return true;
		}

		for (P preset : userStore.getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				preset.setName(name);
				preset.setWaveStrategyIds(strategyIds);
				save();
				return true;
			}
		}

		return false;
	}

	public S createUserWaveStrategy(S strategy)
	{
		if (strategy == null || isBlank(strategy.getName()))
		{
			return null;
		}

		S stored = importOrReplaceWaveStrategy(withUserIdentity(strategy, null, false));
		save();
		return stored;
	}

	public boolean updateUserWaveStrategy(String id, S strategy)
	{
		if (id == null || strategy == null || isBlank(strategy.getName()))
		{
			return false;
		}

		S updated = withUserIdentity(strategy, id, false);

		for (int i = 0; i < userStore.getWaveStrategies().size(); i++)
		{
			if (id.equals(userStore.getWaveStrategies().get(i).getId()))
			{
				List<S> strategies = new ArrayList<>(userStore.getWaveStrategies());
				strategies.set(i, updated);
				userStore.setWaveStrategies(strategies);
				save();
				return true;
			}
		}

		return false;
	}

	public boolean updateBuiltInWaveStrategy(String id, S strategy)
	{
		S builtIn = findBuiltInWaveStrategy(id);

		if (builtIn == null || strategy == null || isBlank(strategy.getName()))
		{
			return false;
		}

		S updated = copyStrategy(strategy, builtIn.getId(), true);
		updated.setName(builtIn.getName());
		updated.setWave(builtIn.getWave());
		updated.setBuiltIn(true);
		List<S> strategies = new ArrayList<>(userStore.getWaveStrategies());

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
		List<S> strategies = new ArrayList<>();

		for (S strategy : userStore.getWaveStrategies())
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
		List<S> strategies = new ArrayList<>();

		for (S strategy : userStore.getWaveStrategies())
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

		for (P preset : userStore.getRunPresets())
		{
			preset.getWaveStrategyIds().values().removeIf(id::equals);
		}

		save();
		return true;
	}

	protected S exportWaveStrategy(S strategy)
	{
		return copyStrategy(strategy, null, false);
	}

	protected void importOrReplaceWaveStrategies(List<S> strategies)
	{
		if (strategies == null)
		{
			return;
		}

		for (S strategy : strategies)
		{
			if (strategy == null || isBlank(strategy.getName()))
			{
				continue;
			}

			importOrReplaceWaveStrategy(strategy);
		}
	}

	protected S importOrReplaceWaveStrategy(S imported)
	{
		S existingUserStrategy = findStoredUserWaveStrategy(imported.getWave(), imported.getName());
		S builtInStrategy = findBuiltInWaveStrategy(imported.getWave(), imported.getName());
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
		S updated = copyStrategy(imported, id, builtIn);
		List<S> storedStrategies = new ArrayList<>(userStore.getWaveStrategies());

		for (int i = 0; i < storedStrategies.size(); i++)
		{
			S stored = storedStrategies.get(i);

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

	protected P importRunPreset(P importedPreset)
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

			S strategy = findWaveStrategy(wave, importedReference);

			if (strategy != null)
			{
				localStrategyIds.put(wave, strategy.getId());
			}
		}

		P existingPreset = findRunPresetByName(importedPreset.getName());
		String id = existingPreset == null ? userId("preset", importedPreset.getName()) : existingPreset.getId();
		boolean builtIn = existingPreset != null && existingPreset.isBuiltIn();
		return createRunPreset(id, importedPreset.getName(), builtIn, localStrategyIds);
	}

	protected void upsertRunPreset(P importedPreset)
	{
		List<P> presets = new ArrayList<>(userStore.getRunPresets());

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

	protected P findStoredUserRunPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (P preset : userStore.getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	protected P findBuiltInRunPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (P preset : builtIns.getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	protected S withUserIdentity(S strategy, String id, boolean builtIn)
	{
		String strategyId = id == null ? userId("wave", strategy.getWave() + "-" + strategy.getName()) : id;
		return copyStrategy(strategy, strategyId, builtIn);
	}

	protected S findStoredUserWaveStrategy(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (S strategy : userStore.getWaveStrategies())
		{
			if (id.equals(strategy.getId()))
			{
				return strategy;
			}
		}

		return null;
	}

	protected S findStoredUserWaveStrategy(int wave, String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		for (S strategy : userStore.getWaveStrategies())
		{
			if (sameWaveStrategyName(strategy, wave, name))
			{
				return strategy;
			}
		}

		return null;
	}

	protected S findBuiltInWaveStrategy(int wave, String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		for (S strategy : builtIns.getWaveStrategies())
		{
			if (sameWaveStrategyName(strategy, wave, name))
			{
				return strategy;
			}
		}

		return null;
	}

	protected S findWaveStrategy(int wave, String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		S userStrategy = findStoredUserWaveStrategy(wave, name);
		S builtInStrategy = findBuiltInWaveStrategy(wave, name);

		if (userStrategy != null
				&& (builtInStrategy == null || !builtInStrategy.getId().equals(userStrategy.getId()) || userStrategy.isBuiltIn()))
		{
			return userStrategy;
		}

		return builtInStrategy;
	}

	protected P findRunPresetByName(String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		for (P preset : getRunPresets())
		{
			if (sameName(name, preset.getName()))
			{
				return preset;
			}
		}

		return null;
	}

	protected boolean sameWaveStrategyName(S strategy, int wave, String name)
	{
		return strategy != null
				&& strategy.getWave() == wave
				&& sameName(strategy.getName(), name);
	}

	protected static boolean sameName(String first, String second)
	{
		return normalizeName(first).equals(normalizeName(second));
	}

	protected static String normalizeName(String name)
	{
		return name == null ? "" : name.trim().toLowerCase();
	}

	protected static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}

	private static String safeName(String name)
	{
		return name == null ? "" : name;
	}

	protected String userId(String type, String name)
	{
		String cleanName = name == null ? "strategy" : name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");

		if (userIdNamespace.isEmpty())
		{
			return "user:" + type + ":" + cleanName;
		}

		return "user:" + userIdNamespace + ":" + type + ":" + cleanName;
	}
}
