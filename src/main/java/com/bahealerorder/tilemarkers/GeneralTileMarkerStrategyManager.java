package com.bahealerorder.tilemarkers;

import static com.bahealerorder.tilemarkers.TileMarkerExportSummary.markerSetDisplayName;
import static com.bahealerorder.tilemarkers.TileMarkerExportSummary.strategyPresetDisplayName;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.TileMarkerStyle;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
public class GeneralTileMarkerStrategyManager
{
	private static final String STRATEGY_STORE_KEY = "generalTileMarkerStore";

	private final ConfigManager configManager;
	private final Gson gson;
	private final List<TileMarkerSet> builtInMarkerSets = TileMarkerBuiltInSets.create();
	private final List<TileMarkerStrategyPreset> builtInStrategyPresets = TileMarkerBuiltInStrategies.createStrategyPresets();
	private final List<TileMarkerAssignmentPreset> builtInAssignmentPresets = TileMarkerBuiltInStrategies.createAssignmentPresets();
	private final TileMarkerStrategyResolver strategyResolver;
	private TileMarkerStrategyStore store = new TileMarkerStrategyStore();

	@Inject
	public GeneralTileMarkerStrategyManager(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
		this.strategyResolver = new TileMarkerStrategyResolver(this::findStrategyPreset, this::findMarkerSet);
	}

	public void load()
	{
		String json = configManager.getConfiguration(BaUtilitiesConfig.GROUP_NAME, STRATEGY_STORE_KEY);
		if (json == null || json.isBlank())
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
			if (set.getWaveMap() == resolvedWaveMap)
			{
				sets.add(set);
			}
		}
		for (TileMarkerSet set : builtInMarkerSets)
		{
			if (set.getWaveMap() == resolvedWaveMap)
			{
				sets.add(set);
			}
		}
		return sets;
	}

	public TileMarkerSet findMarkerSet(String id)
	{
		TileMarkerSet builtIn = findBuiltInMarkerSet(id);
		return builtIn == null ? findUserMarkerSet(id) : builtIn;
	}

	public TileMarkerSet createMarkerSet(
			String name,
			TileMarkerMapMode mapMode,
			TileMarkerWaveMap waveMap,
			List<TileMarker> markers)
	{
		TileMarkerSet set = new TileMarkerSet(userMarkerSetId(), name, mapMode, resolveWaveMap(waveMap), markers);
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
			List<TileMarker> markers)
	{
		TileMarkerSet set = findUserMarkerSet(id);
		if (set == null) return false;

		set.setName(name);
		set.setMapMode(mapMode);
		set.setWaveMap(resolveWaveMap(waveMap));
		set.setMarkers(markers);
		setLastMapMode(mapMode);
		setLastWaveMap(waveMap);
		save();
		return true;
	}

	public List<String> getStrategyNamesUsingMarkerSet(String id)
	{
		List<String> names = new ArrayList<>();
		if (id == null) return names;

		for (TileMarkerStrategyPreset preset : store.getStrategyPresets())
		{
			if (preset != null && preset.getMarkerSetIds().contains(id))
			{
				names.add(strategyPresetDisplayName(preset));
			}
		}
		return names;
	}

	public boolean deleteMarkerSet(String id)
	{
		if (!getStrategyNamesUsingMarkerSet(id).isEmpty()
				|| id == null
				|| !store.getMarkerSets().removeIf(set -> id.equals(set.getId()))) return false;

		save();
		return true;
	}

	public TileMarkerWaveSelection getWaveSelection(TileMarkerRoleContext context, int wave)
	{
		TileMarkerWaveSelection selection = findWaveSelection(context, wave);
		if (selection != null) return selection;

		TileMarkerWaveSelection created = new TileMarkerWaveSelection(waveSelectionId(context, wave), context, wave);
		store.getWaveSelections().add(created);
		save();
		return created;
	}

	public TileMarkerWaveSelection findWaveSelection(TileMarkerRoleContext context, int wave)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		for (TileMarkerWaveSelection selection : store.getWaveSelections())
		{
			if (selection.getRoleContext() == resolvedContext && selection.getWave() == wave) return selection;
		}

		return null;
	}

	public String getWaveSelectionStrategyId(TileMarkerRoleContext context, int wave)
	{
		TileMarkerWaveSelection selection = findWaveSelection(context, wave);
		return selection == null ? null : selection.getStrategyId();
	}

	public void setWaveSelectionStrategyId(TileMarkerRoleContext context, int wave, String strategyId)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		if (isBlank(strategyId))
		{
			store.getWaveSelections().removeIf(selection ->
					selection.getRoleContext() == resolvedContext
							&& selection.getWave() == wave);
			store.setActiveAssignmentPresetId(resolvedContext, null);
			save();
			return;
		}

		if (!strategyMatchesWave(wave, strategyId)) return;

		TileMarkerWaveSelection selection = getWaveSelection(resolvedContext, wave);
		selection.setStrategyId(strategyId);
		store.setActiveAssignmentPresetId(resolvedContext, null);
		save();
	}

	public List<TileMarkerStrategyPreset> getStrategyPresets(TileMarkerWaveMap waveMap)
	{
		List<TileMarkerStrategyPreset> presets = getUserStrategyPresets(waveMap);
		presets.addAll(getBuiltInStrategyPresets(waveMap));
		return presets;
	}

	public List<TileMarkerStrategyPreset> getUserStrategyPresets(TileMarkerWaveMap waveMap)
	{
		TileMarkerWaveMap resolvedWaveMap = resolveWaveMap(waveMap);
		List<TileMarkerStrategyPreset> presets = new ArrayList<>();
		for (TileMarkerStrategyPreset preset : store.getStrategyPresets())
		{
			if (preset.getWaveMap() == resolvedWaveMap
					&& !preset.isBuiltIn()
					&& findBuiltInStrategyPreset(preset.getId()) == null)
			{
				presets.add(preset);
			}
		}
		return presets;
	}

	public List<TileMarkerStrategyPreset> getBuiltInStrategyPresets(TileMarkerWaveMap waveMap)
	{
		TileMarkerWaveMap resolvedWaveMap = resolveWaveMap(waveMap);
		List<TileMarkerStrategyPreset> presets = new ArrayList<>();
		for (TileMarkerStrategyPreset preset : builtInStrategyPresets)
		{
			if (preset.getWaveMap() == resolvedWaveMap)
			{
				TileMarkerStrategyPreset override = findUserStrategyPreset(preset.getId());
				presets.add(override != null && override.isBuiltIn() ? override : preset);
			}
		}
		return presets;
	}

	public List<TileMarkerAssignmentPreset> getAssignmentPresets(TileMarkerRoleContext context)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		List<TileMarkerAssignmentPreset> presets = new ArrayList<>();
		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			if (preset.getRoleContext() == resolvedContext && !preset.isBuiltIn())
			{
				presets.add(preset);
			}
		}
		for (TileMarkerAssignmentPreset preset : builtInAssignmentPresets)
		{
			if (preset.getRoleContext() == resolvedContext)
			{
				presets.add(preset);
			}
		}
		return presets;
	}

	public TileMarkerStrategyPreset findStrategyPreset(String id)
	{
		TileMarkerStrategyPreset userPreset = findUserStrategyPreset(id);
		TileMarkerStrategyPreset builtInPreset = findBuiltInStrategyPreset(id);
		if (userPreset != null && (builtInPreset == null || userPreset.isBuiltIn())) return userPreset;

		return builtInPreset == null ? userPreset : builtInPreset;
	}

	public TileMarkerAssignmentPreset findAssignmentPreset(String id)
	{
		TileMarkerAssignmentPreset builtIn = findById(builtInAssignmentPresets, id, TileMarkerAssignmentPreset::getId);
		return builtIn == null ? findUserAssignmentPreset(id) : builtIn;
	}

	public TileMarkerStrategyPreset saveStrategyPreset(
			String id,
			String name,
			String notes,
			TileMarkerWaveMap waveMap,
			List<String> markerSetIds)
	{
		String normalizedName = name == null ? "" : name.trim();
		if (normalizedName.isEmpty()) return null;

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
			Map<Integer, String> waveSelections)
	{
		String normalizedName = name == null ? "" : name.trim();
		if (normalizedName.isEmpty()) return null;

		TileMarkerRoleContext resolvedContext = resolveContext(context);
		Map<Integer, String> resolvedWaveSelections = existingWaveSelections(waveSelections);

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
					resolvedWaveSelections
			);
			store.getAssignmentPresets().add(preset);
		}
		else
		{
			preset.setName(normalizedName);
			preset.setRoleContext(resolvedContext);
			preset.setWaveSelections(resolvedWaveSelections);
		}

		store.setActiveAssignmentPresetId(resolvedContext, preset.getId());
		save();
		return preset;
	}

	public void deleteAssignmentPreset(String id)
	{
		if (id == null || !store.getAssignmentPresets().removeIf(preset -> id.equals(preset.getId()))) return;
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
		if (id == null || id.isBlank())
		{
			clearWaveSelections(resolvedContext);
			return;
		}

		TileMarkerAssignmentPreset preset = findAssignmentPreset(id);
		if (preset == null || preset.getRoleContext() != resolvedContext) return;

		applyWaveSelections(resolvedContext, preset.getWaveSelections());
		store.setActiveAssignmentPresetId(resolvedContext, preset.getId());
		save();
	}

	public void clearWaveSelections(TileMarkerRoleContext context)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		store.getWaveSelections().removeIf(selection -> selection.getRoleContext() == resolvedContext);
		store.setActiveAssignmentPresetId(resolvedContext, null);
		save();
	}

	public void deleteStrategyPreset(String id)
	{
		if (id == null || !store.getStrategyPresets().removeIf(preset -> id.equals(preset.getId()))) return;
		store.getWaveSelections().removeIf(selection -> strategyIdMatches(selection.getStrategyId(), id));
		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			preset.getWaveSelections().values().removeIf(strategyId -> strategyIdMatches(strategyId, id));
		}
		save();
	}

	public List<TileMarker> getActiveMarkers(int wave, BaRole role)
	{
		return getActiveMarkers(wave, TileMarkerRoleContext.fromRole(role));
	}

	public List<TileMarker> getActiveMarkers(int wave, TileMarkerRoleContext context)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		return strategyResolver.activeMarkers(
				findWaveSelection(TileMarkerRoleContext.GLOBAL, wave),
				resolvedContext == TileMarkerRoleContext.GLOBAL ? null : findWaveSelection(resolvedContext, wave)
		);
	}

	public List<TileMarker> getMarkersForStrategyPreset(TileMarkerStrategyPreset preset)
	{
		return strategyResolver.markersForPreset(preset);
	}

	public String getActiveNotes(int wave, TileMarkerRoleContext context)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		return strategyResolver.activeNotes(
				findWaveSelection(TileMarkerRoleContext.GLOBAL, wave),
				resolvedContext == TileMarkerRoleContext.GLOBAL ? null : findWaveSelection(resolvedContext, wave)
		);
	}

	public String getActiveStrategyName(int wave, TileMarkerRoleContext context)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		List<String> names = new ArrayList<>();
		addActiveStrategyName(names, findWaveSelection(TileMarkerRoleContext.GLOBAL, wave));

		if (resolvedContext != TileMarkerRoleContext.GLOBAL)
		{
			addActiveStrategyName(names, findWaveSelection(resolvedContext, wave));
		}

		return String.join(" + ", names);
	}

	private void addActiveStrategyName(List<String> names, TileMarkerWaveSelection selection)
	{
		TileMarkerStrategyPreset preset = selection == null ? null : findStrategyPreset(selection.getStrategyId());
		if (preset == null || preset.getWaveMap() != TileMarkerWaveMap.fromWave(selection.getWave()) || isBlank(preset.getNotes())) return;

		names.add(strategyPresetDisplayName(preset));
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

	public Map<Integer, String> getWaveSelections(TileMarkerRoleContext context)
	{
		Map<Integer, String> selections = new HashMap<>();
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		for (TileMarkerWaveSelection selection : store.getWaveSelections())
		{
			if (selection.getRoleContext() == resolvedContext
					&& strategyMatchesWave(selection.getWave(), selection.getStrategyId()))
			{
				selections.put(selection.getWave(), selection.getStrategyId());
			}
		}
		return selections;
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
		return TileMarkerStyle.clampOpacityPercent(store.getLastMarkerOpacityPercent());
	}

	public float getLastMarkerBorderWidth()
	{
		return TileMarkerStyle.clampBorderWidth(store.getLastMarkerBorderWidth());
	}

	public void setLastMarkerStyle(String color, int opacityPercent, float borderWidth)
	{
		store.setLastMarkerColor(color);
		store.setLastMarkerOpacityPercent(TileMarkerStyle.clampOpacityPercent(opacityPercent));
		store.setLastMarkerBorderWidth(TileMarkerStyle.clampBorderWidth(borderWidth));
		save();
	}

	public boolean isMarkerEditorLegendVisible()
	{
		return store.isMarkerEditorLegendVisible();
	}

	public void setMarkerEditorLegendVisible(boolean visible)
	{
		store.setMarkerEditorLegendVisible(visible);
		save();
	}

	public boolean isBeginnerPromptDismissed()
	{
		return store.isBeginnerPromptDismissed();
	}

	public void dismissBeginnerPrompt()
	{
		store.setBeginnerPromptDismissed(true);
		save();
	}

	public TileMarkerExportResult exportAssignmentPresetJson(TileMarkerRoleContext context, String name)
	{
		Map<Integer, String> waveSelections = getWaveSelections(context);
		if (waveSelections.isEmpty()) return null;

		Set<String> strategyIds = new LinkedHashSet<>();
		Set<String> markerSetIds = new LinkedHashSet<>();
		for (String strategyId : waveSelections.values())
		{
			if (!isBlank(strategyId))
			{
				strategyIds.add(strategyId);
			}
		}

		List<TileMarkerStrategyPreset> strategyPresets = new ArrayList<>();
		for (String strategyId : strategyIds)
		{
			TileMarkerStrategyPreset preset = findStrategyPreset(strategyId);
			if (preset != null)
			{
				strategyPresets.add(copyStrategyPreset(preset));
				markerSetIds.addAll(preset.getMarkerSetIds());
			}
		}

		List<TileMarkerSet> markerSets = new ArrayList<>();
		int markerCount = 0;
		for (String markerSetId : markerSetIds)
		{
			TileMarkerSet set = findMarkerSet(markerSetId);
			if (set != null)
			{
				TileMarkerSet copy = copyMarkerSet(set);
				markerSets.add(copy);
				markerCount += copy.getMarkers().size();
			}
		}

		String exportName = isBlank(name) ? null : name.trim();
		TileMarkerAssignmentExport export = new TileMarkerAssignmentExport(
				exportName, waveSelections, strategyPresets, markerSets);
		return new TileMarkerExportResult(
				gson.toJson(export),
				assignmentDisplayName(exportName),
				TileMarkerExportType.ASSIGNMENT_PRESET,
				TileMarkerExportSummary.assignment(waveSelections, this::strategyDisplayName),
				markerCount
		);
	}

	public TileMarkerExportResult exportAllMarkerSetsJson()
	{
		List<TileMarkerSet> markerSets = new ArrayList<>();
		Set<String> seenIds = new LinkedHashSet<>();
		int markerCount = 0;
		for (TileMarkerSet set : store.getMarkerSets())
		{
			if (seenIds.add(set.getId()))
			{
				TileMarkerSet copy = copyMarkerSet(set);
				copy.setBuiltIn(false);
				markerSets.add(copy);
				markerCount += copy.getMarkers().size();
			}
		}

		if (markerSets.isEmpty()) return null;

		TileMarkerSetCollectionExport export = new TileMarkerSetCollectionExport(markerSets);
		return new TileMarkerExportResult(
				gson.toJson(export),
				"all tile markers",
				TileMarkerExportType.MARKER_SET_COLLECTION,
				TileMarkerExportSummary.markerSets(markerSets),
				markerCount
		);
	}

	public TileMarkerExportResult exportAllStrategyPresetsJson()
	{
		List<TileMarkerStrategyPreset> strategyPresets = new ArrayList<>();
		Set<String> strategyIds = new LinkedHashSet<>();
		Set<String> markerSetIds = new LinkedHashSet<>();
		for (TileMarkerStrategyPreset preset : store.getStrategyPresets())
		{
			if (strategyIds.add(preset.getId()))
			{
				TileMarkerStrategyPreset copy = copyStrategyPreset(preset);
				copy.setBuiltIn(false);
				strategyPresets.add(copy);
				markerSetIds.addAll(copy.getMarkerSetIds());
			}
		}

		List<TileMarkerSet> markerSets = exportMarkerSets(markerSetIds);
		if (strategyPresets.isEmpty()) return null;

		TileMarkerStrategyCollectionExport export = new TileMarkerStrategyCollectionExport(strategyPresets, markerSets);
		return new TileMarkerExportResult(
				gson.toJson(export),
				"all wave strategies",
				TileMarkerExportType.STRATEGY_COLLECTION,
				TileMarkerExportSummary.strategies(strategyPresets),
				markerCount(markerSets)
		);
	}

	public TileMarkerExportResult importAssignmentPresetJson(TileMarkerRoleContext context, String json)
	{
		TileMarkerAssignmentExport imported = gson.fromJson(json, TileMarkerAssignmentExport.class);
		if (imported == null || imported.getWaveSelections().isEmpty()) return null;

		for (TileMarkerSet set : imported.getMarkerSets())
		{
			importOrReplaceMarkerSet(set);
		}
		for (TileMarkerStrategyPreset preset : imported.getStrategyPresets())
		{
			importOrReplaceStrategyPreset(preset);
		}

		Map<Integer, String> waveSelections = existingWaveSelections(imported.getWaveSelections());
		if (waveSelections.isEmpty()) return null;

		int markerCount = markerCount(imported.getMarkerSets());
		if (isBlank(imported.getName()))
		{
			applyWaveSelections(context, waveSelections);
			store.setActiveAssignmentPresetId(resolveContext(context), null);
			save();
			return new TileMarkerExportResult(
					json,
					assignmentDisplayName(null),
					TileMarkerExportType.ASSIGNMENT_PRESET,
					TileMarkerExportSummary.assignment(waveSelections, this::strategyDisplayName),
					markerCount
			);
		}

		TileMarkerAssignmentPreset saved = saveAssignmentPreset(
				context,
				null,
				imported.getName().trim(),
				waveSelections
		);
		if (saved == null) return null;

		applyAssignmentPreset(context, saved.getId());
		return new TileMarkerExportResult(
				json,
				saved.getName(),
				TileMarkerExportType.ASSIGNMENT_PRESET,
				TileMarkerExportSummary.assignment(waveSelections, this::strategyDisplayName),
				markerCount
		);
	}

	public TileMarkerExportResult importMarkerExportJson(TileMarkerRoleContext context, String json)
	{
		return importDetectedMarkerExport(context, json, null, null);
	}

	public TileMarkerExportResult importMarkerExportJson(String json, TileMarkerWaveMap expectedWaveMap)
	{
		return importDetectedMarkerExport(null, json, expectedWaveMap, null);
	}

	public TileMarkerExportResult importMarkerExportForWave(TileMarkerRoleContext context, int wave, String json)
	{
		return importDetectedMarkerExport(context, json, TileMarkerWaveMap.fromWave(wave), wave);
	}

	private TileMarkerExportResult importDetectedMarkerExport(
			TileMarkerRoleContext context,
			String json,
			TileMarkerWaveMap expectedWaveMap,
			Integer wave)
	{
		TileMarkerExportType type = detectExportType(json);
		if (type == null) return null;

		switch (type)
		{
			case ASSIGNMENT_PRESET:
				return context == null ? importAssignmentPresetContentsJson(json) : importAssignmentPresetJson(context, json);
			case STRATEGY_PRESET:
				TileMarkerExportResult result = importStrategyPresetJson(json, expectedWaveMap);
				if (result != null && wave != null)
				{
					setWaveSelectionStrategyId(context, wave, result.getId());
				}
				return result;
			case MARKER_SET:
				return importMarkerSetJson(json, expectedWaveMap);
			case STRATEGY_COLLECTION:
				return importStrategyCollectionJson(json);
			case MARKER_SET_COLLECTION:
				return importMarkerSetCollectionJson(json);
			default:
				return null;
		}
	}

	public TileMarkerExportResult exportStrategyPresetJson(TileMarkerStrategyPreset preset)
	{
		if (preset == null || isBlank(preset.getName())) return null;

		TileMarkerStrategyPreset copy = copyStrategyPreset(preset);
		copy.setBuiltIn(false);

		List<TileMarkerSet> markerSets = new ArrayList<>();
		List<String> markerSetIds = new ArrayList<>();
		int markerCount = 0;
		for (String markerSetId : preset.getMarkerSetIds())
		{
			TileMarkerSet set = findMarkerSet(markerSetId);
			if (set != null && set.getWaveMap() == preset.getWaveMap())
			{
				TileMarkerSet markerSetCopy = copyMarkerSet(set);
				markerSetCopy.setBuiltIn(false);
				markerSets.add(markerSetCopy);
				markerSetIds.add(markerSetCopy.getId());
				markerCount += markerSetCopy.getMarkers().size();
			}
		}

		copy.setMarkerSetIds(markerSetIds);
		TileMarkerStrategyPresetExport export = new TileMarkerStrategyPresetExport(copy, markerSets);
		return new TileMarkerExportResult(
				gson.toJson(export),
				copy.getId(),
				strategyPresetDisplayName(copy),
				TileMarkerExportType.STRATEGY_PRESET,
				TileMarkerExportSummary.strategyPreset(copy, markerSets),
				markerCount
		);
	}

	public TileMarkerExportResult importStrategyPresetJson(String json, TileMarkerWaveMap expectedWaveMap)
	{
		TileMarkerStrategyPresetExport imported = gson.fromJson(json, TileMarkerStrategyPresetExport.class);
		TileMarkerStrategyPreset importedPreset = imported == null ? null : imported.getStrategyPreset();
		if (importedPreset == null
				|| isBlank(importedPreset.getName())
				|| (expectedWaveMap != null && importedPreset.getWaveMap() != expectedWaveMap))
		{
			return null;
		}

		for (TileMarkerSet set : imported.getMarkerSets())
		{
			importOrReplaceMarkerSet(set);
		}

		TileMarkerStrategyPreset saved = importOrReplaceStrategyPreset(importedPreset);
		if (saved == null || (expectedWaveMap != null && saved.getWaveMap() != expectedWaveMap)) return null;

		save();
		return new TileMarkerExportResult(
				json,
				saved.getId(),
				strategyPresetDisplayName(saved),
				TileMarkerExportType.STRATEGY_PRESET,
				TileMarkerExportSummary.strategyPreset(saved, imported.getMarkerSets()),
				markerCount(imported.getMarkerSets())
		);
	}

	public TileMarkerExportResult exportMarkerSetJson(TileMarkerSet set)
	{
		if (set == null) return null;

		TileMarkerSet copy = copyMarkerSet(set);
		copy.setBuiltIn(false);
		TileMarkerSetExport export = new TileMarkerSetExport(copy);
		return new TileMarkerExportResult(
				gson.toJson(export),
				copy.getId(),
				markerSetDisplayName(copy),
				TileMarkerExportType.MARKER_SET,
				copy.getMarkers().size()
		);
	}

	public TileMarkerExportResult importMarkerSetJson(String json)
	{
		return importMarkerSetJson(json, null);
	}

	public TileMarkerExportResult importMarkerSetJson(String json, TileMarkerWaveMap expectedWaveMap)
	{
		TileMarkerSetExport imported = gson.fromJson(json, TileMarkerSetExport.class);
		TileMarkerSet importedSet = imported == null ? null : imported.getMarkerSet();
		if (importedSet == null || (expectedWaveMap != null && importedSet.getWaveMap() != expectedWaveMap)) return null;

		TileMarkerSet saved = importOrReplaceMarkerSet(importedSet);
		save();
		return new TileMarkerExportResult(
				json,
				saved.getId(),
				markerSetDisplayName(saved),
				TileMarkerExportType.MARKER_SET,
				saved.getMarkers().size()
		);
	}

	private TileMarkerExportResult importAssignmentPresetContentsJson(String json)
	{
		TileMarkerAssignmentExport imported = gson.fromJson(json, TileMarkerAssignmentExport.class);
		if (imported == null) return null;

		for (TileMarkerSet set : imported.getMarkerSets())
		{
			importOrReplaceMarkerSet(set);
		}
		for (TileMarkerStrategyPreset preset : imported.getStrategyPresets())
		{
			importOrReplaceStrategyPreset(preset);
		}

		save();
		return new TileMarkerExportResult(
				json,
				assignmentDisplayName(imported.getName()),
				TileMarkerExportType.ASSIGNMENT_PRESET,
				TileMarkerExportSummary.assignment(existingWaveSelections(imported.getWaveSelections()), this::strategyDisplayName),
				markerCount(imported.getMarkerSets())
		);
	}

	private TileMarkerExportResult importMarkerSetCollectionJson(String json)
	{
		TileMarkerSetCollectionExport imported = gson.fromJson(json, TileMarkerSetCollectionExport.class);
		if (imported == null || imported.getMarkerSets().isEmpty()) return null;

		String firstId = null;
		for (TileMarkerSet set : imported.getMarkerSets())
		{
			TileMarkerSet saved = importOrReplaceMarkerSet(set);
			if (firstId == null && saved != null)
			{
				firstId = saved.getId();
			}
		}

		save();
		return new TileMarkerExportResult(
				json,
				firstId,
				"tile marker collection",
				TileMarkerExportType.MARKER_SET_COLLECTION,
				TileMarkerExportSummary.markerSets(imported.getMarkerSets()),
				markerCount(imported.getMarkerSets())
		);
	}

	private TileMarkerExportResult importStrategyCollectionJson(String json)
	{
		TileMarkerStrategyCollectionExport imported = gson.fromJson(json, TileMarkerStrategyCollectionExport.class);
		if (imported == null || imported.getStrategyPresets().isEmpty()) return null;

		for (TileMarkerSet set : imported.getMarkerSets())
		{
			importOrReplaceMarkerSet(set);
		}

		String firstId = null;
		int strategyCount = 0;
		for (TileMarkerStrategyPreset preset : imported.getStrategyPresets())
		{
			TileMarkerStrategyPreset saved = importOrReplaceStrategyPreset(preset);
			if (saved != null)
			{
				strategyCount++;
				if (firstId == null)
				{
					firstId = saved.getId();
				}
			}
		}

		if (strategyCount == 0) return null;

		save();
		return new TileMarkerExportResult(
				json,
				firstId,
				"wave strategy collection",
				TileMarkerExportType.STRATEGY_COLLECTION,
				TileMarkerExportSummary.strategies(imported.getStrategyPresets()),
				markerCount(imported.getMarkerSets())
		);
	}

	private void normalizeStore()
	{
		store.setLastMarkerOpacityPercent(TileMarkerStyle.clampOpacityPercent(store.getLastMarkerOpacityPercent()));
		store.setLastMarkerBorderWidth(TileMarkerStyle.clampBorderWidth(store.getLastMarkerBorderWidth()));
		store.setLastMapMode(store.getLastMapMode());
		store.setLastWaveMap(store.getLastWaveMap());
		List<TileMarkerSet> sets = new ArrayList<>();
		Set<String> setIds = new LinkedHashSet<>();

		for (TileMarkerSet set : store.getMarkerSets())
		{
			if (set == null || isBlank(set.getId())) continue;

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
		store.getStrategyPresets().removeIf(preset -> preset == null || isBlank(preset.getId()));
		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			if (preset != null)
			{
				preset.setBuiltIn(false);
				preset.setRoleContext(resolveContext(preset.getRoleContext()));
				preset.setWaveSelections(existingWaveSelections(preset.getWaveSelections()));
			}
		}
		store.getWaveSelections().removeIf(selection ->
				selection == null || !strategyMatchesWave(selection.getWave(), selection.getStrategyId()));
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
		return findById(store.getMarkerSets(), id, TileMarkerSet::getId);
	}

	private TileMarkerStrategyPreset findUserStrategyPreset(String id)
	{
		return findById(store.getStrategyPresets(), id, TileMarkerStrategyPreset::getId);
	}

	private TileMarkerStrategyPreset findBuiltInStrategyPreset(String id)
	{
		return findById(builtInStrategyPresets, id, TileMarkerStrategyPreset::getId);
	}

	private TileMarkerAssignmentPreset findUserAssignmentPreset(String id)
	{
		return findById(store.getAssignmentPresets(), id, TileMarkerAssignmentPreset::getId);
	}

	private TileMarkerStrategyPreset findStrategyPresetByName(TileMarkerWaveMap waveMap, String name)
	{
		TileMarkerWaveMap resolvedWaveMap = resolveWaveMap(waveMap);
		for (TileMarkerStrategyPreset preset : store.getStrategyPresets())
		{
			if (preset.getWaveMap() == resolvedWaveMap
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
			if (preset.getRoleContext() == resolvedContext
					&& preset.getName() != null
					&& preset.getName().trim().equalsIgnoreCase(name))
			{
				return preset;
			}
		}

		return null;
	}

	private static String assignmentDisplayName(String name)
	{
		return isBlank(name) ? "current wave strategies" : name.trim();
	}

	private static int markerCount(List<TileMarkerSet> markerSets)
	{
		int markerCount = 0;
		if (markerSets == null) return markerCount;

		for (TileMarkerSet set : markerSets)
		{
			if (set != null)
			{
				markerCount += set.getMarkers().size();
			}
		}
		return markerCount;
	}

	private void applyWaveSelections(
			TileMarkerRoleContext context,
			Map<Integer, String> waveSelections)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		store.getWaveSelections().removeIf(selection -> selection.getRoleContext() == resolvedContext);
		for (Map.Entry<Integer, String> entry : existingWaveSelections(waveSelections).entrySet())
		{
			TileMarkerWaveSelection selection = new TileMarkerWaveSelection(waveSelectionId(resolvedContext, entry.getKey()), resolvedContext, entry.getKey());
			selection.setStrategyId(entry.getValue());
			store.getWaveSelections().add(selection);
		}
	}

	private String strategyDisplayName(String strategyId)
	{
		if (isBlank(strategyId)) return "None";

		TileMarkerStrategyPreset preset = findStrategyPreset(strategyId);
		return strategyPresetDisplayName(preset);
	}

	private List<TileMarkerSet> exportMarkerSets(Set<String> markerSetIds)
	{
		List<TileMarkerSet> markerSets = new ArrayList<>();
		for (String markerSetId : markerSetIds)
		{
			TileMarkerSet set = findMarkerSet(markerSetId);
			if (set != null)
			{
				TileMarkerSet copy = copyMarkerSet(set);
				copy.setBuiltIn(false);
				markerSets.add(copy);
			}
		}
		return markerSets;
	}

	private TileMarkerExportType detectExportType(String json)
	{
		JsonObject object = parseExportObject(json);
		if (object == null) return null;

		if (object.has("waveSelections")) return TileMarkerExportType.ASSIGNMENT_PRESET;
		if (object.has("strategyPreset")) return TileMarkerExportType.STRATEGY_PRESET;
		if (object.has("markerSet")) return TileMarkerExportType.MARKER_SET;
		if (object.has("strategyPresets")) return TileMarkerExportType.STRATEGY_COLLECTION;
		if (object.has("markerSets")) return TileMarkerExportType.MARKER_SET_COLLECTION;
		return null;
	}

	private JsonObject parseExportObject(String json)
	{
		if (json == null || json.isBlank()) return null;

		try
		{
			JsonElement element = gson.fromJson(json, JsonElement.class);
			return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private TileMarkerSet importOrReplaceMarkerSet(TileMarkerSet imported)
	{
		if (imported == null) return null;

		TileMarkerSet builtIn = findBuiltInMarkerSet(imported.getId());
		if (builtIn != null) return builtIn;

		TileMarkerSet existing = findUserMarkerSet(imported.getId());
		if (existing == null)
		{
			TileMarkerSet added = copyMarkerSet(imported);
			if (isBlank(added.getId()))
			{
				added.setId(userMarkerSetId());
			}
			added.setBuiltIn(false);
			store.getMarkerSets().add(added);
			return added;
		}

		existing.setName(imported.getName());
		existing.setMapMode(imported.getMapMode());
		existing.setWaveMap(imported.getWaveMap());
		existing.setMarkers(TileMarker.copyAll(imported.getMarkers()));
		existing.setBuiltIn(false);
		return existing;
	}

	private TileMarkerStrategyPreset importOrReplaceStrategyPreset(TileMarkerStrategyPreset imported)
	{
		if (imported == null || isBlank(imported.getName())) return null;

		TileMarkerStrategyPreset existing = findUserStrategyPreset(imported.getId());

		if (existing == null)
		{
			TileMarkerStrategyPreset added = copyStrategyPreset(imported);
			if (isBlank(added.getId()))
			{
				added.setId(strategyPresetId());
			}
			added.setBuiltIn(findBuiltInStrategyPreset(added.getId()) != null);
			added.setMarkerSetIds(existingMarkerSetIds(added.getWaveMap(), added.getMarkerSetIds()));
			store.getStrategyPresets().add(added);
			return added;
		}

		existing.setName(imported.getName());
		existing.setNotes(imported.getNotes());
		existing.setWaveMap(imported.getWaveMap());
		existing.setBuiltIn(findBuiltInStrategyPreset(existing.getId()) != null);
		existing.setMarkerSetIds(existingMarkerSetIds(existing.getWaveMap(), imported.getMarkerSetIds()));
		return existing;
	}

	private TileMarkerSet findBuiltInMarkerSet(String id)
	{
		return findById(builtInMarkerSets, id, TileMarkerSet::getId);
	}

	private static <T> T findById(List<T> values, String id, Function<T, String> idGetter)
	{
		if (id == null) return null;
		for (T value : values)
		{
			if (value != null && id.equals(idGetter.apply(value))) return value;
		}
		return null;
	}

	private static TileMarkerStrategyPreset copyStrategyPreset(TileMarkerStrategyPreset preset)
	{
		return new TileMarkerStrategyPreset(
				preset.getId(),
				preset.getName(),
				preset.getNotes(),
				preset.getWaveMap(),
				preset.getMarkerSetIds(),
				preset.isBuiltIn()
		);
	}

	private static TileMarkerSet copyMarkerSet(TileMarkerSet set)
	{
		return new TileMarkerSet(
				set.getId(),
				set.getName(),
				set.getMapMode(),
				set.getWaveMap(),
				TileMarker.copyAll(set.getMarkers()),
				set.isBuiltIn()
		);
	}

	private Map<Integer, String> existingWaveSelections(Map<Integer, String> waveSelections)
	{
		Map<Integer, String> selections = new HashMap<>();
		if (waveSelections == null) return selections;

		for (Map.Entry<Integer, String> entry : waveSelections.entrySet())
		{
			Integer wave = entry.getKey();
			String strategyId = entry.getValue();
			if (wave != null && wave >= 1 && wave <= 10 && strategyMatchesWave(wave, strategyId))
			{
				selections.put(wave, strategyId);
			}
		}
		return selections;
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

	private String userMarkerSetId()
	{
		return "tile-marker-set:" + System.nanoTime();
	}

	private String waveSelectionId(TileMarkerRoleContext context, int wave)
	{
		return "tile-marker-selection:" + resolveContext(context).name().toLowerCase() + ":" + wave;
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

	private boolean strategyMatchesWave(int wave, String strategyId)
	{
		if (isBlank(strategyId)) return false;

		TileMarkerStrategyPreset preset = findStrategyPreset(strategyId);
		return preset != null && preset.getWaveMap() == TileMarkerWaveMap.fromWave(wave);
	}

	private static boolean strategyIdMatches(String strategyId, String id)
	{
		return strategyId != null && id != null && id.equals(strategyId);
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.isBlank();
	}

}
