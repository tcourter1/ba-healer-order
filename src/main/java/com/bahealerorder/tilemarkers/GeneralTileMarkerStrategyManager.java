package com.bahealerorder.tilemarkers;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.TileMarkerStyle;
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

	public List<TileMarkerSet> getUserMarkerSets(TileMarkerWaveMap waveMap)
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
			List<TileMarker> markers)
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
			List<TileMarker> markers)
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
		store.getWaveSelections().removeIf(selection -> selection != null && targetMatches(selection.getTarget(), TileMarkerWaveSelectionType.MARKER_SET, id));
		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			if (preset != null)
			{
				preset.getWaveSelections().values().removeIf(target -> targetMatches(target, TileMarkerWaveSelectionType.MARKER_SET, id));
			}
		}
		save();
	}

	public TileMarkerWaveSelection getWaveSelection(TileMarkerRoleContext context, int wave)
	{
		TileMarkerWaveSelection selection = findWaveSelection(context, wave);
		if (selection != null)
		{
			return selection;
		}

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
			if (selection.getRoleContext() == resolvedContext && selection.getWave() == wave)
			{
				return selection;
			}
		}

		return null;
	}

	public TileMarkerWaveSelectionTarget getWaveSelectionTarget(TileMarkerRoleContext context, int wave)
	{
		TileMarkerWaveSelection selection = findWaveSelection(context, wave);
		return selection == null ? null : new TileMarkerWaveSelectionTarget(selection.getTarget());
	}

	public void setWaveSelectionTarget(TileMarkerRoleContext context, int wave, TileMarkerWaveSelectionTarget target)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		if (target == null || target.isEmpty())
		{
			store.getWaveSelections().removeIf(selection ->
					selection != null
							&& selection.getRoleContext() == resolvedContext
							&& selection.getWave() == wave);
			store.setActiveAssignmentPresetId(resolvedContext, null);
			save();
			return;
		}

		if (!selectionTargetMatchesWave(wave, target))
		{
			return;
		}

		TileMarkerWaveSelection selection = getWaveSelection(resolvedContext, wave);
		selection.setTarget(target);
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

	public List<TileMarkerStrategyPreset> getUserStrategyPresets(TileMarkerWaveMap waveMap)
	{
		TileMarkerWaveMap resolvedWaveMap = resolveWaveMap(waveMap);
		List<TileMarkerStrategyPreset> presets = new ArrayList<>();
		for (TileMarkerStrategyPreset preset : store.getStrategyPresets())
		{
			if (preset != null
					&& preset.getWaveMap() == resolvedWaveMap
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
			if (preset != null && preset.getWaveMap() == resolvedWaveMap)
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
			Map<Integer, TileMarkerWaveSelectionTarget> waveSelections)
	{
		String normalizedName = name == null ? "" : name.trim();
		if (normalizedName.isEmpty())
		{
			return null;
		}

		TileMarkerRoleContext resolvedContext = resolveContext(context);
		Map<Integer, TileMarkerWaveSelectionTarget> resolvedWaveSelections = existingWaveSelections(waveSelections);

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
			preset.setBuiltIn(false);
			preset.setWaveSelections(resolvedWaveSelections);
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
			clearWaveSelections(resolvedContext);
			return;
		}

		TileMarkerAssignmentPreset preset = findAssignmentPreset(id);
		if (preset == null || preset.getRoleContext() != resolvedContext)
		{
			return;
		}

		applyWaveSelections(resolvedContext, preset.getWaveSelections());
		store.setActiveAssignmentPresetId(resolvedContext, preset.getId());
		save();
	}

	public void clearWaveSelections(TileMarkerRoleContext context)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		store.getWaveSelections().removeIf(selection -> selection != null && selection.getRoleContext() == resolvedContext);
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
		store.getWaveSelections().removeIf(selection -> selection != null && targetMatches(selection.getTarget(), TileMarkerWaveSelectionType.STRATEGY_PRESET, id));
		for (TileMarkerAssignmentPreset preset : store.getAssignmentPresets())
		{
			if (preset != null)
			{
				preset.getWaveSelections().values().removeIf(target -> targetMatches(target, TileMarkerWaveSelectionType.STRATEGY_PRESET, id));
			}
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

	public Map<Integer, TileMarkerWaveSelectionTarget> getWaveSelections(TileMarkerRoleContext context)
	{
		Map<Integer, TileMarkerWaveSelectionTarget> selections = new HashMap<>();
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		for (TileMarkerWaveSelection selection : store.getWaveSelections())
		{
			if (selection != null
					&& selection.getRoleContext() == resolvedContext
					&& selectionTargetMatchesWave(selection.getWave(), selection.getTarget()))
			{
				selections.put(selection.getWave(), new TileMarkerWaveSelectionTarget(selection.getTarget()));
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

	public boolean isMarkerEditorHelpVisible()
	{
		return store.isMarkerEditorHelpVisible();
	}

	public void setMarkerEditorHelpVisible(boolean visible)
	{
		store.setMarkerEditorHelpVisible(visible);
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
		Map<Integer, TileMarkerWaveSelectionTarget> waveSelections = getWaveSelections(context);
		if (waveSelections.isEmpty())
		{
			return null;
		}

		TileMarkerAssignmentExport export = new TileMarkerAssignmentExport();
		export.setName(isBlank(name) ? null : name.trim());
		export.setWaveSelections(waveSelections);

		Set<String> strategyIds = new LinkedHashSet<>();
		Set<String> markerSetIds = new LinkedHashSet<>();
		for (TileMarkerWaveSelectionTarget target : waveSelections.values())
		{
			collectExportTarget(target, strategyIds, markerSetIds);
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

		export.setStrategyPresets(strategyPresets);
		export.setMarkerSets(markerSets);
		return new TileMarkerExportResult(
				gson.toJson(export),
				assignmentDisplayName(export.getName()),
				wavesText(waveSelections.keySet()),
				strategyPresets.size(),
				markerSets.size(),
				markerCount
		);
	}

	public TileMarkerExportResult importAssignmentPresetJson(TileMarkerRoleContext context, String json)
	{
		TileMarkerAssignmentExport imported = gson.fromJson(json, TileMarkerAssignmentExport.class);
		if (imported == null || imported.getWaveSelections().isEmpty())
		{
			return null;
		}

		for (TileMarkerSet set : imported.getMarkerSets())
		{
			importOrReplaceMarkerSet(set);
		}
		for (TileMarkerStrategyPreset preset : imported.getStrategyPresets())
		{
			importOrReplaceStrategyPreset(preset);
		}

		Map<Integer, TileMarkerWaveSelectionTarget> waveSelections = existingWaveSelections(imported.getWaveSelections());
		if (waveSelections.isEmpty())
		{
			return null;
		}

		int markerCount = markerCount(imported.getMarkerSets());
		if (isBlank(imported.getName()))
		{
			applyWaveSelections(context, waveSelections);
			store.setActiveAssignmentPresetId(resolveContext(context), null);
			save();
			return new TileMarkerExportResult(
					json,
					assignmentDisplayName(null),
					wavesText(waveSelections.keySet()),
					imported.getStrategyPresets().size(),
					imported.getMarkerSets().size(),
					markerCount
			);
		}

		TileMarkerAssignmentPreset saved = saveAssignmentPreset(
				context,
				null,
				imported.getName().trim(),
				waveSelections
		);
		if (saved == null)
		{
			return null;
		}

		applyAssignmentPreset(context, saved.getId());
		return new TileMarkerExportResult(
				json,
				saved.getName(),
				wavesText(waveSelections.keySet()),
				imported.getStrategyPresets().size(),
				imported.getMarkerSets().size(),
				markerCount
		);
	}

	public TileMarkerExportResult exportStrategyPresetJson(TileMarkerStrategyPreset preset)
	{
		if (preset == null || isBlank(preset.getName()))
		{
			return null;
		}

		TileMarkerStrategyPresetExport export = new TileMarkerStrategyPresetExport();
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
		export.setStrategyPreset(copy);
		export.setMarkerSets(markerSets);
		return new TileMarkerExportResult(
				gson.toJson(export),
				copy.getId(),
				strategyPresetDisplayName(copy),
				null,
				1,
				markerSets.size(),
				markerCount
		);
	}

	public TileMarkerExportResult importStrategyPresetJson(String json, TileMarkerWaveMap expectedWaveMap)
	{
		TileMarkerStrategyPresetExport imported = gson.fromJson(json, TileMarkerStrategyPresetExport.class);
		TileMarkerStrategyPreset importedPreset = imported == null ? null : imported.getStrategyPreset();
		TileMarkerWaveMap resolvedWaveMap = resolveWaveMap(expectedWaveMap);
		if (importedPreset == null
				|| isBlank(importedPreset.getName())
				|| importedPreset.getWaveMap() != resolvedWaveMap)
		{
			return null;
		}

		for (TileMarkerSet set : imported.getMarkerSets())
		{
			importOrReplaceMarkerSet(set);
		}

		TileMarkerStrategyPreset saved = importOrReplaceStrategyPreset(importedPreset);
		if (saved == null || saved.getWaveMap() != resolvedWaveMap)
		{
			return null;
		}

		save();
		return new TileMarkerExportResult(
				json,
				saved.getId(),
				strategyPresetDisplayName(saved),
				null,
				1,
				imported.getMarkerSets().size(),
				markerCount(imported.getMarkerSets())
		);
	}

	public TileMarkerExportResult exportMarkerSetJson(TileMarkerSet set)
	{
		if (set == null)
		{
			return null;
		}

		TileMarkerSetExport export = new TileMarkerSetExport();
		TileMarkerSet copy = copyMarkerSet(set);
		copy.setBuiltIn(false);
		export.setMarkerSet(copy);
		return new TileMarkerExportResult(
				gson.toJson(export),
				copy.getId(),
				markerSetDisplayName(copy),
				null,
				0,
				1,
				copy.getMarkers().size()
		);
	}

	public TileMarkerExportResult importMarkerSetJson(String json)
	{
		TileMarkerSetExport imported = gson.fromJson(json, TileMarkerSetExport.class);
		TileMarkerSet importedSet = imported == null ? null : imported.getMarkerSet();
		if (importedSet == null)
		{
			return null;
		}

		TileMarkerSet saved = importOrReplaceMarkerSet(importedSet);
		save();
		return new TileMarkerExportResult(
				json,
				saved.getId(),
				markerSetDisplayName(saved),
				null,
				0,
				1,
				saved.getMarkers().size()
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
				preset.setWaveSelections(existingWaveSelections(preset.getWaveSelections()));
			}
		}
		store.getWaveSelections().removeIf(selection ->
				selection == null || !selectionTargetMatchesWave(selection.getWave(), selection.getTarget()));
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

	private static String assignmentDisplayName(String name)
	{
		return isBlank(name) ? "current wave strategies" : name.trim();
	}

	private static int markerCount(List<TileMarkerSet> markerSets)
	{
		int markerCount = 0;
		if (markerSets == null)
		{
			return markerCount;
		}

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
			Map<Integer, TileMarkerWaveSelectionTarget> waveSelections)
	{
		TileMarkerRoleContext resolvedContext = resolveContext(context);
		store.getWaveSelections().removeIf(selection -> selection != null && selection.getRoleContext() == resolvedContext);
		for (Map.Entry<Integer, TileMarkerWaveSelectionTarget> entry : existingWaveSelections(waveSelections).entrySet())
		{
			TileMarkerWaveSelection selection = new TileMarkerWaveSelection(waveSelectionId(resolvedContext, entry.getKey()), resolvedContext, entry.getKey());
			selection.setTarget(entry.getValue());
			store.getWaveSelections().add(selection);
		}
	}

	private void collectExportTarget(
			TileMarkerWaveSelectionTarget target,
			Set<String> strategyIds,
			Set<String> markerSetIds)
	{
		if (target == null || target.isEmpty())
		{
			return;
		}

		if (target.getType() == TileMarkerWaveSelectionType.STRATEGY_PRESET)
		{
			strategyIds.add(target.getId());
		}
		else if (target.getType() == TileMarkerWaveSelectionType.MARKER_SET)
		{
			markerSetIds.add(target.getId());
		}
	}

	private TileMarkerSet importOrReplaceMarkerSet(TileMarkerSet imported)
	{
		if (imported == null)
		{
			return null;
		}

		TileMarkerSet builtIn = findBuiltInMarkerSet(imported.getId());
		if (builtIn != null)
		{
			return builtIn;
		}

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
		existing.setMarkers(copyMarkers(imported.getMarkers()));
		existing.setBuiltIn(false);
		return existing;
	}

	private TileMarkerStrategyPreset importOrReplaceStrategyPreset(TileMarkerStrategyPreset imported)
	{
		if (imported == null || isBlank(imported.getName()))
		{
			return null;
		}

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
		if (id == null)
		{
			return null;
		}

		for (TileMarkerSet set : builtInMarkerSets)
		{
			if (set != null && id.equals(set.getId()))
			{
				return set;
			}
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
				copyMarkers(set.getMarkers()),
				set.isBuiltIn()
		);
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

	private static String markerSetDisplayName(TileMarkerSet set)
	{
		return set == null || isBlank(set.getName()) ? "unnamed tile marker set" : set.getName().trim();
	}

	private static String strategyPresetDisplayName(TileMarkerStrategyPreset preset)
	{
		return preset == null || isBlank(preset.getName()) ? "unnamed wave strategy" : preset.getName().trim();
	}

	private static String wavesText(Set<Integer> waves)
	{
		if (waves == null || waves.isEmpty())
		{
			return "no waves";
		}

		List<Integer> sorted = new ArrayList<>(waves);
		java.util.Collections.sort(sorted);
		if (sorted.size() == 10 && sorted.get(0) == 1 && sorted.get(9) == 10)
		{
			return "all waves";
		}

		StringBuilder builder = new StringBuilder();
		for (Integer wave : sorted)
		{
			if (builder.length() > 0)
			{
				builder.append(", ");
			}
			builder.append(wave);
		}
		return builder.toString();
	}

	private Map<Integer, TileMarkerWaveSelectionTarget> existingWaveSelections(
			Map<Integer, TileMarkerWaveSelectionTarget> waveSelections)
	{
		Map<Integer, TileMarkerWaveSelectionTarget> selections = new HashMap<>();
		if (waveSelections == null)
		{
			return selections;
		}

		for (Map.Entry<Integer, TileMarkerWaveSelectionTarget> entry : waveSelections.entrySet())
		{
			Integer wave = entry.getKey();
			TileMarkerWaveSelectionTarget target = entry.getValue();
			if (wave != null && wave >= 1 && wave <= 10 && selectionTargetMatchesWave(wave, target))
			{
				selections.put(wave, new TileMarkerWaveSelectionTarget(target));
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

	private boolean selectionTargetMatchesWave(int wave, TileMarkerWaveSelectionTarget target)
	{
		if (target == null || target.isEmpty())
		{
			return false;
		}

		if (target.getType() == TileMarkerWaveSelectionType.STRATEGY_PRESET)
		{
			TileMarkerStrategyPreset preset = findStrategyPreset(target.getId());
			return preset != null && preset.getWaveMap() == TileMarkerWaveMap.fromWave(wave);
		}

		if (target.getType() == TileMarkerWaveSelectionType.MARKER_SET)
		{
			TileMarkerSet set = findMarkerSet(target.getId());
			return set != null && set.getWaveMap() == TileMarkerWaveMap.fromWave(wave);
		}

		return false;
	}

	private static boolean targetMatches(TileMarkerWaveSelectionTarget target, TileMarkerWaveSelectionType type, String id)
	{
		return target != null && target.getType() == type && id != null && id.equals(target.getId());
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}

}
