package com.bahealerorder.tilemarkers;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.defender.TileMarkerMapMode;
import com.bahealerorder.defender.TileMarkerWaveMap;
import com.bahealerorder.defender.strategies.DefenderMarker;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
public class GeneralTileMarkerStrategyManager
{
	private static final String STRATEGY_STORE_KEY = "generalTileMarkerStore";
	private static final int MIN_MARKER_OPACITY_PERCENT = 0;
	private static final int MAX_MARKER_OPACITY_PERCENT = 100;
	private static final float MIN_MARKER_BORDER_WIDTH = 0f;
	private static final float MAX_MARKER_BORDER_WIDTH = 8f;

	private final ConfigManager configManager;
	private final Gson gson;
	private final List<TileMarkerSet> builtInMarkerSets = TileMarkerBuiltInSets.create();
	private final List<TileMarkerStrategyPreset> builtInStrategyPresets = TileMarkerBuiltInStrategies.createStrategyPresets();
	private final List<TileMarkerAssignmentPreset> builtInAssignmentPresets = TileMarkerBuiltInStrategies.createAssignmentPresets();
	private TileMarkerStrategyStore store = new TileMarkerStrategyStore();

	@Inject
	public GeneralTileMarkerStrategyManager(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	public void load()
	{
		String json = configManager.getConfiguration(BaUtilitiesConfig.GROUP_NAME, STRATEGY_STORE_KEY);
		if (json == null || json.trim().isEmpty())
		{
			store = new TileMarkerStrategyStore();
			save();
			return;
		}

		try
		{
			TileMarkerStrategyStore parsed = gson.fromJson(json, TileMarkerStrategyStore.class);
			store = parsed == null ? new TileMarkerStrategyStore() : parsed;
			normalizeStore();
		}
		catch (RuntimeException ex)
		{
			store = new TileMarkerStrategyStore();
			save();
		}
	}

	public void save()
	{
		configManager.setConfiguration(BaUtilitiesConfig.GROUP_NAME, STRATEGY_STORE_KEY, gson.toJson(store));
	}

	public List<TileMarkerSet> getMarkerSets(TileMarkerWaveMap waveMap)
	{
		TileMarkerWaveMap resolvedWaveMap = resolveWaveMap(waveMap);
		List<TileMarkerSet> sets = new ArrayList<>();
		for (TileMarkerSet set : store.getMarkerSets())
		{
			if (set != null && set.getWaveMap() == resolvedWaveMap)
			{
				sets.add(set);
			}
		}
		for (TileMarkerSet set : builtInMarkerSets)
		{
			if (set != null && set.getWaveMap() == resolvedWaveMap)
			{
				sets.add(set);
			}
		}
		return sets;
	}

	public TileMarkerSet findMarkerSet(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (TileMarkerSet set : builtInMarkerSets)
		{
			if (id.equals(set.getId()))
			{
				return set;
			}
		}

		for (TileMarkerSet set : store.getMarkerSets())
		{
			if (set != null && id.equals(set.getId()))
			{
				return set;
			}
		}

		return null;
	}

	public TileMarkerSet createMarkerSet(
			String name,
			TileMarkerMapMode mapMode,
			TileMarkerWaveMap waveMap,
			List<DefenderMarker> markers)
	{
		TileMarkerSet set = new TileMarkerSet(userMarkerSetId(), name, mapMode, resolveWaveMap(waveMap), markers);
		set.setBuiltIn(false);
		store.getMarkerSets().add(set);
		setLastMapMode(mapMode);
		setLastWaveMap(waveMap);
		save();
		return set;
	}

	public boolean updateMarkerSet(
			String id,
			String name,
			TileMarkerMapMode mapMode,
			TileMarkerWaveMap waveMap,
			List<DefenderMarker> markers)
	{
		TileMarkerSet set = findUserMarkerSet(id);
		if (set == null)
		{
			return false;
		}

		set.setName(name);
		set.setMapMode(mapMode);
		set.setWaveMap(resolveWaveMap(waveMap));
		set.setBuiltIn(false);
		set.setMarkers(markers);
		setLastMapMode(mapMode);
		setLastWaveMap(waveMap);
		save();
		return true;
	}

	public void deleteMarkerSet(String id)
	{
		if (id == null)
		{
			return;
		}

		if (findUserMarkerSet(id) == null)
		{
			return;
		}

		store.getMarkerSets().removeIf(set -> id.equals(set.getId()));
		for (TileMarkerStrategyPreset preset : store.getStrategyPresets())
		{
			if (preset != null)
			{
				preset.setMarkerSetIds(without(preset.getMarkerSetIds(), id));
			}
		}
		save();
	}

	public TileMarkerWaveStrategy getWaveStrategy(TileMarkerRoleContext context, int wave)
	{
		TileMarkerWaveStrategy strategy = findWaveStrategy(context, wave);
		if (strategy != null)
		{
			return strategy;
		}

		TileMarkerWaveStrategy created = new TileMarkerWaveStrategy(waveStrategyId(context, wave), context, wave);
		store.getWaveStrategies().add(created);
		save();
		return created;
	}

	public TileMarkerWaveStrategy findWaveStrategy(TileMarkerRoleContext context, int wave)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		for (TileMarkerWaveStrategy strategy : store.getWaveStrategies())
		{
			if (strategy.getRoleContext() == resolvedContext && strategy.getWave() == wave)
			{
				return strategy;
			}
		}

		return null;
	}

	public String getWaveStrategyPresetId(TileMarkerRoleContext context, int wave)
	{
		TileMarkerWaveStrategy strategy = findWaveStrategy(context, wave);
		return strategy == null ? null : strategy.getStrategyPresetId();
	}

	public void setWaveStrategyPresetId(TileMarkerRoleContext context, int wave, String presetId)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		if (presetId == null || presetId.trim().isEmpty())
		{
			store.getWaveStrategies().removeIf(strategy ->
					strategy != null
							&& strategy.getRoleContext() == resolvedContext
							&& strategy.getWave() == wave);
			store.setActiveAssignmentPresetId(resolvedContext, null);
			save();
			return;
		}

		if (!strategyPresetMatchesWave(wave, presetId))
		{
			return;
		}

		TileMarkerWaveStrategy strategy = getWaveStrategy(resolvedContext, wave);
		strategy.setStrategyPresetId(presetId);
		store.setActiveAssignmentPresetId(resolvedContext, null);
		save();
	}

	public List<TileMarkerStrategyPreset> getStrategyPresets(TileMarkerWaveMap waveMap)
	{
		TileMarkerWaveMap resolvedWaveMap = resolveWaveMap(waveMap);
		List<TileMarkerStrategyPreset> presets = new ArrayList<>();
		for (TileMarkerStrategyPreset preset : builtInStrategyPresets)
		{
			if (preset != null && preset.getWaveMap() == resolvedWaveMap)
			{
				TileMarkerStrategyPreset override = findUserStrategyPreset(preset.getId());
				presets.add(override != null && override.isBuiltIn() ? override : preset);
			}
		}
		for (TileMarkerStrategyPreset preset : store.getStrategyPresets())
		{
			if (preset != null
					&& preset.getWaveMap() == resolvedWaveMap
					&& findBuiltInStrategyPreset(preset.getId()) == null)
			{
				presets.add(preset);
			}
		}
		return presets;
	}

	public List<TileMarkerAssignmentPreset> getAssignmentPresets(TileMarkerRoleContext context)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		List<TileMarkerAssignmentPreset> presets = new ArrayList<>();
		for (TileMarkerAssignmentPreset preset : builtInAssignmentPresets)
		{
			if (preset != null && preset.getRoleContext() == resolvedContext)
			{
				presets.add(preset);
			}
		}
		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			if (preset != null && preset.getRoleContext() == resolvedContext)
			{
				presets.add(preset);
			}
		}
		return presets;
	}

	public TileMarkerStrategyPreset findStrategyPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		TileMarkerStrategyPreset userPreset = findUserStrategyPreset(id);
		TileMarkerStrategyPreset builtInPreset = findBuiltInStrategyPreset(id);
		if (userPreset != null && (builtInPreset == null || userPreset.isBuiltIn()))
		{
			return userPreset;
		}

		return builtInPreset == null ? userPreset : builtInPreset;
	}

	public TileMarkerAssignmentPreset findAssignmentPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (TileMarkerAssignmentPreset preset : builtInAssignmentPresets)
		{
			if (preset != null && id.equals(preset.getId()))
			{
				return preset;
			}
		}

		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			if (preset != null && id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	public TileMarkerStrategyPreset saveStrategyPreset(
			String id,
			String name,
			String notes,
			TileMarkerWaveMap waveMap,
			List<String> markerSetIds)
	{
		String normalizedName = name == null ? "" : name.trim();
		if (normalizedName.isEmpty())
		{
			return null;
		}

		TileMarkerWaveMap resolvedWaveMap = resolveWaveMap(waveMap);
		TileMarkerStrategyPreset builtInPreset = findBuiltInStrategyPreset(id);
		TileMarkerStrategyPreset preset = findUserStrategyPreset(id);
		if (builtInPreset != null)
		{
			resolvedWaveMap = builtInPreset.getWaveMap();
		}
		else if (preset == null || preset.getWaveMap() != resolvedWaveMap)
		{
			preset = findStrategyPresetByName(resolvedWaveMap, normalizedName);
		}
		if (preset == null)
		{
			preset = new TileMarkerStrategyPreset(
					builtInPreset == null ? strategyPresetId() : builtInPreset.getId(),
					normalizedName,
					notes,
					resolvedWaveMap,
					existingMarkerSetIds(resolvedWaveMap, markerSetIds),
					builtInPreset != null
			);
			store.getStrategyPresets().add(preset);
		}
		else
		{
			preset.setName(normalizedName);
			preset.setNotes(notes);
			preset.setWaveMap(resolvedWaveMap);
			preset.setBuiltIn(builtInPreset != null);
			preset.setMarkerSetIds(existingMarkerSetIds(resolvedWaveMap, markerSetIds));
		}

		save();
		return preset;
	}

	public TileMarkerAssignmentPreset saveAssignmentPreset(
			TileMarkerRoleContext context,
			String id,
			String name,
			Map<Integer, String> waveStrategyPresetIds)
	{
		String normalizedName = name == null ? "" : name.trim();
		if (normalizedName.isEmpty())
		{
			return null;
		}

		TileMarkerRoleContext resolvedContext = resolveContext(context);
		TileMarkerAssignmentPreset preset = findUserAssignmentPreset(id);
		if (preset == null)
		{
			preset = findAssignmentPresetByName(resolvedContext, normalizedName);
		}
		if (preset == null)
		{
			preset = new TileMarkerAssignmentPreset(
					assignmentPresetId(),
					normalizedName,
					resolvedContext,
					existingStrategyPresetIds(waveStrategyPresetIds)
			);
			store.getAssignmentPresets().add(preset);
		}
		else
		{
			preset.setName(normalizedName);
			preset.setRoleContext(resolvedContext);
			preset.setBuiltIn(false);
			preset.setWaveStrategyPresetIds(existingStrategyPresetIds(waveStrategyPresetIds));
		}

		store.setActiveAssignmentPresetId(resolvedContext, preset.getId());
		save();
		return preset;
	}

	public void deleteAssignmentPreset(String id)
	{
		if (id == null)
		{
			return;
		}

		if (findUserAssignmentPreset(id) == null)
		{
			return;
		}

		store.getAssignmentPresets().removeIf(preset -> preset != null && id.equals(preset.getId()));
		for (TileMarkerRoleContext context : TileMarkerRoleContext.values())
		{
			if (id.equals(getActiveAssignmentPresetId(context)))
			{
				store.setActiveAssignmentPresetId(context, null);
			}
		}
		save();
	}

	public void applyAssignmentPreset(TileMarkerRoleContext context, String id)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		if (id == null || id.trim().isEmpty())
		{
			clearWaveStrategyPresetIds(resolvedContext);
			return;
		}

		TileMarkerAssignmentPreset preset = findAssignmentPreset(id);
		if (preset == null || preset.getRoleContext() != resolvedContext)
		{
			return;
		}

		store.getWaveStrategies().removeIf(strategy -> strategy != null && strategy.getRoleContext() == resolvedContext);
		for (Map.Entry<Integer, String> entry : existingStrategyPresetIds(preset.getWaveStrategyPresetIds()).entrySet())
		{
			TileMarkerWaveStrategy strategy = new TileMarkerWaveStrategy(waveStrategyId(resolvedContext, entry.getKey()), resolvedContext, entry.getKey());
			strategy.setStrategyPresetId(entry.getValue());
			store.getWaveStrategies().add(strategy);
		}
		store.setActiveAssignmentPresetId(resolvedContext, preset.getId());
		save();
	}

	public void clearWaveStrategyPresetIds(TileMarkerRoleContext context)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		store.getWaveStrategies().removeIf(strategy -> strategy != null && strategy.getRoleContext() == resolvedContext);
		store.setActiveAssignmentPresetId(resolvedContext, null);
		save();
	}

	public void deleteStrategyPreset(String id)
	{
		if (id == null)
		{
			return;
		}

		if (findUserStrategyPreset(id) == null)
		{
			return;
		}

		store.getStrategyPresets().removeIf(preset -> id.equals(preset.getId()));
		store.getWaveStrategies().removeIf(strategy -> strategy != null && id.equals(strategy.getStrategyPresetId()));
		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			if (preset != null)
			{
				preset.getWaveStrategyPresetIds().values().removeIf(id::equals);
			}
		}
		save();
	}

	public List<DefenderMarker> getActiveMarkers(int wave, BaRole role)
	{
		return getActiveMarkers(wave, TileMarkerRoleContext.fromRole(role));
	}

	public List<DefenderMarker> getActiveMarkers(int wave, TileMarkerRoleContext context)
	{
		List<DefenderMarker> markers = new ArrayList<>();
		Set<String> seenSetIds = new LinkedHashSet<>();
		addStrategyMarkers(markers, seenSetIds, findWaveStrategy(TileMarkerRoleContext.GLOBAL, wave));

		TileMarkerRoleContext resolvedContext = resolveContext(context);
		if (resolvedContext != TileMarkerRoleContext.GLOBAL)
		{
			addStrategyMarkers(markers, seenSetIds, findWaveStrategy(resolvedContext, wave));
		}

		return markers;
	}

	public List<DefenderMarker> getMarkersForStrategyPreset(TileMarkerStrategyPreset preset)
	{
		List<DefenderMarker> markers = new ArrayList<>();
		addPresetMarkers(markers, new LinkedHashSet<>(), preset);
		return markers;
	}

	public String getActiveNotes(int wave, TileMarkerRoleContext context)
	{
		List<String> notes = new ArrayList<>();
		addStrategyNotes(notes, findWaveStrategy(TileMarkerRoleContext.GLOBAL, wave));

		TileMarkerRoleContext resolvedContext = resolveContext(context);
		if (resolvedContext != TileMarkerRoleContext.GLOBAL)
		{
			addStrategyNotes(notes, findWaveStrategy(resolvedContext, wave));
		}

		return String.join("\n\n", notes);
	}

	public TileMarkerRoleContext getSelectedRoleContext()
	{
		return store.getSelectedRoleContext();
	}

	public void setSelectedRoleContext(TileMarkerRoleContext context)
	{
		store.setSelectedRoleContext(resolveContext(context));
		save();
	}

	public String getActiveAssignmentPresetId(TileMarkerRoleContext context)
	{
		String id = store.getActiveAssignmentPresetIds().get(resolveContext(context).name());
		return findAssignmentPreset(id) == null ? null : id;
	}

	public Map<Integer, String> getWaveStrategyPresetIds(TileMarkerRoleContext context)
	{
		Map<Integer, String> ids = new HashMap<>();
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		for (TileMarkerWaveStrategy strategy : store.getWaveStrategies())
		{
			if (strategy != null
					&& strategy.getRoleContext() == resolvedContext
					&& strategyPresetMatchesWave(strategy.getWave(), strategy.getStrategyPresetId()))
			{
				ids.put(strategy.getWave(), strategy.getStrategyPresetId());
			}
		}
		return ids;
	}

	public TileMarkerMapMode getLastMapMode()
	{
		return store.getLastMapMode();
	}

	public void setLastMapMode(TileMarkerMapMode mode)
	{
		store.setLastMapMode(mode);
		save();
	}

	public TileMarkerWaveMap getLastWaveMap()
	{
		return store.getLastWaveMap();
	}

	public void setLastWaveMap(TileMarkerWaveMap waveMap)
	{
		store.setLastWaveMap(resolveWaveMap(waveMap));
		save();
	}

	public String getLastMarkerColor()
	{
		return store.getLastMarkerColor();
	}

	public int getLastMarkerOpacityPercent()
	{
		return clampOpacity(store.getLastMarkerOpacityPercent());
	}

	public float getLastMarkerBorderWidth()
	{
		return clampBorderWidth(store.getLastMarkerBorderWidth());
	}

	public void setLastMarkerStyle(String color, int opacityPercent, float borderWidth)
	{
		store.setLastMarkerColor(color);
		store.setLastMarkerOpacityPercent(clampOpacity(opacityPercent));
		store.setLastMarkerBorderWidth(clampBorderWidth(borderWidth));
		save();
	}

	public String exportMarkers(TileMarkerWaveMap waveMap, List<DefenderMarker> markers)
	{
		if (waveMap == null || markers == null || markers.isEmpty())
		{
			return null;
		}

		return gson.toJson(TileMarkerClipboard.fromMarkers(waveMap, markers));
	}

	public List<DefenderMarker> importMarkers(TileMarkerWaveMap waveMap, String json)
	{
		if (waveMap == null || json == null || json.trim().isEmpty())
		{
			return java.util.Collections.emptyList();
		}

		try
		{
			TileMarkerClipboard clipboard = gson.fromJson(json, TileMarkerClipboard.class);
			return clipboard == null ? java.util.Collections.emptyList() : clipboard.toMarkers(waveMap);
		}
		catch (RuntimeException ex)
		{
			return java.util.Collections.emptyList();
		}
	}

	private void addStrategyMarkers(List<DefenderMarker> markers, Set<String> seenSetIds, TileMarkerWaveStrategy strategy)
	{
		if (strategy == null)
		{
			return;
		}

		TileMarkerStrategyPreset preset = findStrategyPreset(strategy.getStrategyPresetId());
		if (preset == null || preset.getWaveMap() != TileMarkerWaveMap.fromWave(strategy.getWave()))
		{
			return;
		}

		addPresetMarkers(markers, seenSetIds, preset);
	}

	private void addPresetMarkers(List<DefenderMarker> markers, Set<String> seenSetIds, TileMarkerStrategyPreset preset)
	{
		if (preset == null)
		{
			return;
		}

		for (String setId : preset.getMarkerSetIds())
		{
			if (setId == null || !seenSetIds.add(setId))
			{
				continue;
			}

			TileMarkerSet set = findMarkerSet(setId);
			if (set != null)
			{
				markers.addAll(set.getMarkers());
			}
		}
	}

	private void addStrategyNotes(List<String> notes, TileMarkerWaveStrategy strategy)
	{
		if (strategy == null)
		{
			return;
		}

		TileMarkerStrategyPreset preset = findStrategyPreset(strategy.getStrategyPresetId());
		if (preset == null || preset.getWaveMap() != TileMarkerWaveMap.fromWave(strategy.getWave()))
		{
			return;
		}

		String text = preset.getNotes();
		if (text != null && !text.trim().isEmpty())
		{
			notes.add(text.trim());
		}
	}

	private void normalizeStore()
	{
		store.setLastMarkerOpacityPercent(clampOpacity(store.getLastMarkerOpacityPercent()));
		store.setLastMarkerBorderWidth(clampBorderWidth(store.getLastMarkerBorderWidth()));
		store.setLastMapMode(store.getLastMapMode());
		store.setLastWaveMap(store.getLastWaveMap());
		List<TileMarkerSet> sets = new ArrayList<>();
		Set<String> setIds = new LinkedHashSet<>();

		for (TileMarkerSet set : store.getMarkerSets())
		{
			if (set == null || isBlank(set.getId()))
			{
				continue;
			}

			if (setIds.add(set.getId()))
			{
				set.setMapMode(set.getMapMode());
				set.setWaveMap(set.getWaveMap());
				set.setBuiltIn(false);
				sets.add(set);
			}
		}

		store.setMarkerSets(sets);
		for (TileMarkerStrategyPreset preset : store.getStrategyPresets())
		{
			if (preset != null)
			{
				preset.setWaveMap(preset.getWaveMap());
				preset.setBuiltIn(false);
				preset.setMarkerSetIds(existingMarkerSetIds(preset.getWaveMap(), preset.getMarkerSetIds()));
			}
		}
		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			if (preset != null)
			{
				preset.setBuiltIn(false);
				preset.setRoleContext(resolveContext(preset.getRoleContext()));
				preset.setWaveStrategyPresetIds(existingStrategyPresetIds(preset.getWaveStrategyPresetIds()));
			}
		}
		store.getWaveStrategies().removeIf(strategy ->
				strategy == null || !strategyPresetMatchesWave(strategy.getWave(), strategy.getStrategyPresetId()));
		store.getAssignmentPresets().removeIf(preset -> preset == null || isBlank(preset.getId()));
		for (TileMarkerRoleContext context : TileMarkerRoleContext.values())
		{
			String activeId = store.getActiveAssignmentPresetIds().get(context.name());
			TileMarkerAssignmentPreset active = findAssignmentPreset(activeId);
			if (active == null || active.getRoleContext() != context)
			{
				store.setActiveAssignmentPresetId(context, null);
			}
		}
		save();
	}

	private TileMarkerSet findUserMarkerSet(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (TileMarkerSet set : store.getMarkerSets())
		{
			if (set != null && id.equals(set.getId()))
			{
				return set;
			}
		}

		return null;
	}

	private TileMarkerStrategyPreset findUserStrategyPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (TileMarkerStrategyPreset preset : store.getStrategyPresets())
		{
			if (preset != null && id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	private TileMarkerStrategyPreset findBuiltInStrategyPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (TileMarkerStrategyPreset preset : builtInStrategyPresets)
		{
			if (preset != null && id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	private TileMarkerAssignmentPreset findUserAssignmentPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			if (preset != null && id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	private TileMarkerStrategyPreset findStrategyPresetByName(TileMarkerWaveMap waveMap, String name)
	{
		TileMarkerWaveMap resolvedWaveMap = resolveWaveMap(waveMap);
		for (TileMarkerStrategyPreset preset : store.getStrategyPresets())
		{
			if (preset != null
					&& preset.getWaveMap() == resolvedWaveMap
					&& preset.getName() != null
					&& preset.getName().trim().equalsIgnoreCase(name))
			{
				return preset;
			}
		}

		return null;
	}

	private TileMarkerAssignmentPreset findAssignmentPresetByName(TileMarkerRoleContext context, String name)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			if (preset != null
					&& preset.getRoleContext() == resolvedContext
					&& preset.getName() != null
					&& preset.getName().trim().equalsIgnoreCase(name))
			{
				return preset;
			}
		}

		return null;
	}

	private Map<Integer, String> existingStrategyPresetIds(Map<Integer, String> waveStrategyPresetIds)
	{
		Map<Integer, String> ids = new HashMap<>();
		if (waveStrategyPresetIds == null)
		{
			return ids;
		}

		for (Map.Entry<Integer, String> entry : waveStrategyPresetIds.entrySet())
		{
			Integer wave = entry.getKey();
			String strategyPresetId = entry.getValue();
			if (wave != null && wave >= 1 && wave <= 10 && strategyPresetMatchesWave(wave, strategyPresetId))
			{
				ids.put(wave, strategyPresetId);
			}
		}
		return ids;
	}

	private List<String> existingMarkerSetIds(TileMarkerWaveMap waveMap, List<String> markerSetIds)
	{
		TileMarkerWaveMap resolvedWaveMap = resolveWaveMap(waveMap);
		List<String> ids = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (String id : markerSetIds == null ? java.util.Collections.<String>emptyList() : markerSetIds)
		{
			TileMarkerSet markerSet = findMarkerSet(id);
			if (markerSet != null && markerSet.getWaveMap() == resolvedWaveMap && seen.add(id))
			{
				ids.add(id);
			}
		}
		return ids;
	}

	private List<String> without(List<String> ids, String removedId)
	{
		List<String> result = new ArrayList<>();
		for (String id : ids)
		{
			if (!removedId.equals(id))
			{
				result.add(id);
			}
		}
		return result;
	}

	private String userMarkerSetId()
	{
		return "tile-marker-set:" + System.nanoTime();
	}

	private String waveStrategyId(TileMarkerRoleContext context, int wave)
	{
		return "tile-marker-strategy:" + resolveContext(context).name().toLowerCase() + ":" + wave;
	}

	private String strategyPresetId()
	{
		return "tile-marker-strategy-preset:" + System.nanoTime();
	}

	private String assignmentPresetId()
	{
		return "tile-marker-assignment-preset:" + System.nanoTime();
	}

	private TileMarkerRoleContext resolveContext(TileMarkerRoleContext context)
	{
		return context == null ? TileMarkerRoleContext.DEFENDER : context;
	}

	private TileMarkerWaveMap resolveWaveMap(TileMarkerWaveMap waveMap)
	{
		return waveMap == null ? TileMarkerWaveMap.WAVES_1_TO_9 : waveMap;
	}

	private boolean strategyPresetMatchesWave(int wave, String presetId)
	{
		TileMarkerStrategyPreset preset = findStrategyPreset(presetId);
		return preset != null && preset.getWaveMap() == TileMarkerWaveMap.fromWave(wave);
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
}
