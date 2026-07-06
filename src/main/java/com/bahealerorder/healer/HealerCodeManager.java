package com.bahealerorder.healer;

import com.bahealerorder.healer.codes.BuiltInStrategyLibrary;
import com.bahealerorder.healer.codes.CallCode;
import com.bahealerorder.healer.codes.FeedEvent;
import com.bahealerorder.healer.codes.HealerCodeExport;
import com.bahealerorder.healer.codes.HealerCodeExportResult;
import com.bahealerorder.healer.codes.HealerCodeExportType;
import com.bahealerorder.healer.codes.HealerCodeFormatter;
import com.bahealerorder.healer.codes.HealerCodeParser;
import com.bahealerorder.healer.codes.HealerCodeProgress;
import com.bahealerorder.healer.codes.HealerCodeStatus;
import com.bahealerorder.healer.codes.HealerCodeStoreNormalizer;
import com.bahealerorder.healer.codes.HealerInstruction;
import com.bahealerorder.healer.codes.RunPreset;
import com.bahealerorder.healer.codes.StrategyStore;
import com.bahealerorder.healer.codes.WaveCode;
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
public class HealerCodeManager
{
	private static final String CONFIG_GROUP = "bahealerorder";
	private static final String STRATEGY_STORE_KEY = "strategyStore";
	private static final String STRATEGY_STORE_UPGRADE_BACKUP_KEY = "strategyStoreBeforeV2Upgrade";
	private static final int CURRENT_STORE_VERSION = 2;

	private final ConfigManager configManager;
	private final Gson gson;
	private final StrategyStore builtIns = BuiltInStrategyLibrary.create();

	private StrategyStore userStore = new StrategyStore();

	@Inject
	public HealerCodeManager(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson.newBuilder().setPrettyPrinting().create();
	}

	HealerCodeManager(StrategyStore userStore, Gson gson)
	{
		this.configManager = null;
		this.gson = gson.newBuilder().setPrettyPrinting().create();
		this.userStore = userStore == null ? new StrategyStore() : userStore;
	}

	public void load()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, STRATEGY_STORE_KEY);

		if (json == null || json.trim().isEmpty())
		{
			userStore = createEmptyUserStore();
			save();
			return;
		}

		try
		{
			userStore = gson.fromJson(json, StrategyStore.class);
		}
		catch (RuntimeException ex)
		{
			backupRawStrategyStore(json);
			userStore = createEmptyUserStore();
			return;
		}

		if (userStore == null)
		{
			backupRawStrategyStore(json);
			userStore = createEmptyUserStore();
			return;
		}

		int loadedVersion = userStore.getVersion();
		if (loadedVersion != CURRENT_STORE_VERSION)
		{
			backupRawStrategyStore(json);
		}

		if (HealerCodeStoreNormalizer.normalize(userStore, CURRENT_STORE_VERSION))
		{
			save();
		}
	}

	public void save()
	{
		if (configManager == null)
		{
			return;
		}

		userStore.setVersion(CURRENT_STORE_VERSION);
		configManager.setConfiguration(CONFIG_GROUP, STRATEGY_STORE_KEY, gson.toJson(userStore));
	}

	private void backupRawStrategyStore(String json)
	{
		if (configManager == null || isBlank(json))
		{
			return;
		}

		String existingBackup = configManager.getConfiguration(CONFIG_GROUP, STRATEGY_STORE_UPGRADE_BACKUP_KEY);
		if (isBlank(existingBackup))
		{
			configManager.setConfiguration(CONFIG_GROUP, STRATEGY_STORE_UPGRADE_BACKUP_KEY, json);
		}
	}

	private StrategyStore createEmptyUserStore()
	{
		StrategyStore store = new StrategyStore();
		store.setVersion(CURRENT_STORE_VERSION);
		return store;
	}

	public String exportRunPresetJson(String presetId)
	{
		HealerCodeExportResult result = exportRunPreset(presetId);
		return result == null ? null : result.getJson();
	}

	public HealerCodeExportResult exportCurrentRunPreset(String name)
	{
		return exportRunPreset(name, userStore.getActiveWaveCodeIds(), null);
	}

	public HealerCodeExportResult exportRunPreset(String presetId)
	{
		RunPreset preset = findRunPreset(presetId);

		if (preset == null)
		{
			return null;
		}

		return exportRunPreset(preset.getName(), preset.getWaveCodeIds(), preset.getId());
	}

	private HealerCodeExportResult exportRunPreset(String name, Map<Integer, String> waveCodeIds, String id)
	{
		if (waveCodeIds == null || waveCodeIds.isEmpty())
		{
			return null;
		}

		List<WaveCode> waveCodes = new ArrayList<>();
		Map<Integer, String> exportedWaveCodeNames = new HashMap<>();

		for (String waveCodeId : waveCodeIds.values())
		{
			WaveCode waveCode = findWaveCode(waveCodeId);

			if (waveCode != null)
			{
				exportedWaveCodeNames.put(waveCode.getWave(), waveCode.getName());
				waveCodes.add(exportableWaveCode(waveCode));
			}
		}

		if (exportedWaveCodeNames.isEmpty())
		{
			return null;
		}

		RunPreset exportedPreset = new RunPreset(null, normalizedExportName(name), false, exportedWaveCodeNames);
		String json = gson.toJson(new HealerCodeExport(
				CURRENT_STORE_VERSION,
				HealerCodeExportType.RUN_PRESET,
				exportedPreset,
				null,
				waveCodes
		));
		return new HealerCodeExportResult(
				json,
				id,
				runPresetDisplayName(exportedPreset),
				0,
				HealerCodeExportType.RUN_PRESET,
				presetSummary("Export", exportedPreset, waveCodes)
		);
	}

	public HealerCodeExportResult exportWaveCode(String waveCodeId)
	{
		WaveCode waveCode = findWaveCode(waveCodeId);

		if (waveCode == null)
		{
			return null;
		}

		WaveCode exported = exportableWaveCode(waveCode);
		String json = gson.toJson(new HealerCodeExport(
				CURRENT_STORE_VERSION,
				HealerCodeExportType.WAVE_CODE,
				null,
				exported,
				null
		));
		return new HealerCodeExportResult(
				json,
				waveCode.getId(),
				waveCodeDisplayName(waveCode),
				waveCode.getWave(),
				HealerCodeExportType.WAVE_CODE,
				waveSummary("Export", exported)
		);
	}

	public boolean importRunPresetJson(String json)
	{
		return importHealerCodeJson(json) != null;
	}

	public HealerCodeExportResult importHealerCodeJson(String json)
	{
		return importHealerCodeJson(json, null);
	}

	public HealerCodeExportResult importHealerCodeJson(String json, Integer expectedWave)
	{
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}

		try
		{
			HealerCodeExport imported = gson.fromJson(json, HealerCodeExport.class);

			if (imported == null || imported.getVersion() != CURRENT_STORE_VERSION || imported.getType() == null)
			{
				return null;
			}

			if (imported.getType() == HealerCodeExportType.WAVE_CODE)
			{
				return importWaveCodeExport(imported.getWaveCode(), expectedWave);
			}

			if (expectedWave != null)
			{
				return null;
			}

			return importRunPresetExport(imported);
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	public List<RunPreset> getRunPresets()
	{
		List<RunPreset> presets = new ArrayList<>();
		presets.addAll(builtIns.getRunPresets());
		presets.addAll(userStore.getRunPresets());
		presets.sort(Comparator.comparing(RunPreset::isBuiltIn)
				.thenComparing(HealerCodeManager::runPresetDisplayName, String.CASE_INSENSITIVE_ORDER));
		return presets;
	}

	public List<WaveCode> getWaveCodesForWave(int wave)
	{
		List<WaveCode> waveCodes = new ArrayList<>();

		for (WaveCode code : getWaveCodes())
		{
			if (code.getWave() == wave)
			{
				waveCodes.add(code);
			}
		}

		waveCodes.sort(Comparator.comparing(WaveCode::isBuiltIn).reversed().thenComparing(HealerCodeManager::waveCodeDisplayName, String.CASE_INSENSITIVE_ORDER));
		return waveCodes;
	}

	public List<WaveCode> getWaveCodes()
	{
		List<WaveCode> waveCodes = new ArrayList<>();

		for (WaveCode builtIn : builtIns.getWaveCodes())
		{
			WaveCode override = findStoredUserWaveCode(builtIn.getId());
			waveCodes.add(override == null || sameWaveCodeContent(builtIn, override) ? builtIn : override);
		}

		for (WaveCode userCode : userStore.getWaveCodes())
		{
			if (userCode == null || userCode.isBuiltIn() || isBuiltInWaveCodeId(userCode.getId()))
			{
				continue;
			}

			waveCodes.add(userCode);
		}

		return waveCodes;
	}

	public String getActiveRunPresetId()
	{
		RunPreset preset = findRunPreset(userStore.getActiveRunPresetId());

		if (preset != null && preset.getWaveCodeIds().equals(userStore.getActiveWaveCodeIds()))
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

	public boolean isCodeEditorHelpVisible()
	{
		return userStore.isCodeEditorHelpVisible();
	}

	public void setCodeEditorHelpVisible(boolean visible)
	{
		userStore.setCodeEditorHelpVisible(visible);
		save();
	}

	public RunPreset getActiveRunPreset()
	{
		return findRunPreset(getActiveRunPresetId());
	}

	public RunPreset findMatchingRunPreset()
	{
		Map<Integer, String> activeWaveCodes = userStore.getActiveWaveCodeIds();

		if (activeWaveCodes.isEmpty())
		{
			return null;
		}

		for (RunPreset preset : getRunPresets())
		{
			if (preset.getWaveCodeIds().equals(activeWaveCodes))
			{
				return preset;
			}
		}

		return null;
	}

	public RunPreset findRunPreset(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (RunPreset preset : getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				return preset;
			}
		}

		return null;
	}

	public WaveCode findWaveCode(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (WaveCode code : getWaveCodes())
		{
			if (id.equals(code.getId()))
			{
				return code;
			}
		}

		return null;
	}

	public WaveCode findBuiltInWaveCode(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (WaveCode code : builtIns.getWaveCodes())
		{
			if (id.equals(code.getId()))
			{
				return code;
			}
		}

		return null;
	}

	public WaveCode getActiveWaveCode(int wave)
	{
		WaveCode selectedWaveCode = findWaveCode(getActiveWaveCodeId(wave));

		if (selectedWaveCode != null)
		{
			return selectedWaveCode;
		}

		RunPreset preset = getActiveRunPreset();

		if (preset != null)
		{
			return findWaveCode(preset.getWaveCodeId(wave));
		}

		return null;
	}

	public String getActiveWaveCodeId(int wave)
	{
		return userStore.getActiveWaveCodeIds().get(wave);
	}

	public Map<Integer, String> getActiveWaveCodeIds()
	{
		return new HashMap<>(userStore.getActiveWaveCodeIds());
	}

	public Integer getExpectedTimeSeconds(int wave, int healerOrder)
	{
		WaveCode waveCode = getActiveWaveCode(wave);
		return waveCode == null ? null : waveCode.getExpectedTimeSeconds(healerOrder);
	}

	public void setActiveWaveCodeId(int wave, String waveCodeId)
	{
		userStore.setActiveRunPresetId(null);

		if (waveCodeId == null || waveCodeId.trim().isEmpty())
		{
			userStore.getActiveWaveCodeIds().remove(wave);
		}
		else
		{
			userStore.getActiveWaveCodeIds().put(wave, waveCodeId);
		}

		save();
	}

	public void applyRunPreset(String runPresetId)
	{
		userStore.setActiveRunPresetId(runPresetId);
		RunPreset preset = findRunPreset(runPresetId);

		if (preset != null)
		{
			userStore.setActiveWaveCodeIds(preset.getWaveCodeIds());
		}

		save();
	}

	public void clearActiveSelections()
	{
		userStore.setActiveRunPresetId(null);
		userStore.getActiveWaveCodeIds().clear();
		save();
	}

	public int getExpectedFoodForOrder(int wave, int healerOrder, int currentCallIndex)
	{
		return HealerCodeProgress.getExpectedFoodForOrder(getActiveWaveCode(wave), healerOrder, currentCallIndex);
	}

	public HealerCodeStatus getCurrentStatus(int wave, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		return HealerCodeProgress.getCurrentStatus(getActiveWaveCode(wave), healerOrder, currentCallIndex, feedEvents);
	}

	public HealerCodeStatus getPreviousStatus(int wave, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		return HealerCodeProgress.getPreviousStatus(getActiveWaveCode(wave), healerOrder, currentCallIndex, feedEvents);
	}

	public HealerCodeStatus getDisplayStatus(int wave, int healerOrder, int currentCallIndex, List<FeedEvent> feedEvents)
	{
		return HealerCodeProgress.getDisplayStatus(getActiveWaveCode(wave), healerOrder, currentCallIndex, feedEvents);
	}

	public HealerCodeStatus getPanelStatusForCall(int wave, int healerOrder, int currentCallIndex, int panelCallIndex, List<FeedEvent> feedEvents)
	{
		return HealerCodeProgress.getPanelStatusForCall(getActiveWaveCode(wave), healerOrder, currentCallIndex, panelCallIndex, feedEvents);
	}

	public int getPanelFoodCountForCall(int wave, int healerOrder, int currentCallIndex, int panelCallIndex, List<FeedEvent> feedEvents)
	{
		return HealerCodeProgress.getPanelFoodCountForCall(getActiveWaveCode(wave), healerOrder, currentCallIndex, panelCallIndex, feedEvents);
	}

	public RunPreset createUserPreset(String name, Map<Integer, String> waveCodeIds)
	{
		RunPreset existingPreset = findStoredUserRunPresetByName(name);
		String savedName = existingPreset == null && findRunPresetByName(name) != null ? uniquePresetName(name) : name;
		RunPreset preset = new RunPreset(
				existingPreset == null ? userId("preset", savedName) : existingPreset.getId(),
				savedName,
				false,
				new HashMap<>(waveCodeIds)
		);
		upsertRunPreset(preset);
		userStore.setActiveRunPresetId(preset.getId());
		save();
		return preset;
	}

	public RunPreset createUserPresetFromActive(String name)
	{
		return createUserPreset(name, userStore.getActiveWaveCodeIds());
	}

	public boolean deleteUserPreset(String id)
	{
		if (id == null)
		{
			return false;
		}

		boolean removed = false;
		List<RunPreset> presets = new ArrayList<>();

		for (RunPreset preset : userStore.getRunPresets())
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
			userStore.getActiveWaveCodeIds().clear();
		}

		save();
		return true;
	}

	public boolean updateUserPreset(String id, String name, Map<Integer, String> waveCodeIds)
	{
		if (id == null || name == null || name.trim().isEmpty())
		{
			return false;
		}

		for (RunPreset preset : userStore.getRunPresets())
		{
			if (id.equals(preset.getId()))
			{
				preset.setName(name);
				preset.setWaveCodeIds(new HashMap<>(waveCodeIds));
				save();
				return true;
			}
		}

		return false;
	}

	public WaveCode saveWaveCode(String selectedId, WaveCode draft)
	{
		if (draft == null || isBlank(draft.getName()))
		{
			return null;
		}

		WaveCode builtIn = findBuiltInWaveCode(selectedId);
		if (builtIn != null)
		{
			WaveCode updated = copyWaveCode(draft, builtIn.getId(), true);
			if (sameWaveCodeContent(builtIn, updated))
			{
				removeStoredWaveCode(builtIn.getId());
				save();
				return builtIn;
			}

			upsertStoredWaveCode(updated);
			save();
			return updated;
		}

		WaveCode existing = findStoredUserWaveCode(selectedId);
		if (existing != null)
		{
			WaveCode updated = copyWaveCode(draft, existing.getId(), false);
			replaceStoredUserWaveCode(updated);
			save();
			return updated;
		}

		WaveCode saved = importWaveCode(draft);
		save();
		return saved;
	}

	public boolean isModifiedBuiltInWaveCode(String id)
	{
		WaveCode builtIn = findBuiltInWaveCode(id);
		WaveCode override = findStoredUserWaveCode(id);
		return builtIn != null && override != null && !sameWaveCodeContent(builtIn, override);
	}

	public boolean resetBuiltInWaveCode(String id)
	{
		if (findBuiltInWaveCode(id) == null || !removeStoredWaveCode(id))
		{
			return false;
		}

		save();
		return true;
	}

	public boolean deleteUserWaveCode(String id)
	{
		if (id == null)
		{
			return false;
		}

		boolean removed = false;
		List<WaveCode> waveCodes = new ArrayList<>();

		for (WaveCode code : userStore.getWaveCodes())
		{
			if (id.equals(code.getId()))
			{
				removed = true;
			}
			else
			{
				waveCodes.add(code);
			}
		}

		if (!removed)
		{
			return false;
		}

		userStore.setWaveCodes(waveCodes);
		userStore.getActiveWaveCodeIds().values().removeIf(id::equals);

		for (RunPreset preset : userStore.getRunPresets())
		{
			preset.getWaveCodeIds().values().removeIf(id::equals);
		}

		save();
		return true;
	}

	private HealerCodeExportResult importWaveCodeExport(WaveCode importedWaveCode, Integer expectedWave)
	{
		if (importedWaveCode == null || isBlank(importedWaveCode.getName()))
		{
			return null;
		}

		if (expectedWave != null && importedWaveCode.getWave() != expectedWave)
		{
			return null;
		}

		WaveCode saved = importWaveCode(importedWaveCode);
		userStore.setActiveRunPresetId(null);
		userStore.getActiveWaveCodeIds().put(saved.getWave(), saved.getId());
		save();
		return new HealerCodeExportResult(
				null,
				saved.getId(),
				waveCodeDisplayName(saved),
				saved.getWave(),
				HealerCodeExportType.WAVE_CODE,
				waveSummary("Import", saved)
		);
	}

	private HealerCodeExportResult importRunPresetExport(HealerCodeExport imported)
	{
		RunPreset importedPreset = imported.getPreset();
		if (importedPreset == null)
		{
			return null;
		}

		Map<String, WaveCode> importedWaveCodesByReference = new HashMap<>();
		List<WaveCode> savedWaveCodes = new ArrayList<>();
		for (WaveCode waveCode : imported.getWaveCodes())
		{
			if (waveCode == null || isBlank(waveCode.getName()))
			{
				continue;
			}

			WaveCode saved = importWaveCode(waveCode);
			importedWaveCodesByReference.put(waveReference(waveCode.getWave(), waveCode.getName()), saved);
			savedWaveCodes.add(saved);
		}

		Map<Integer, String> localWaveCodeIds = new HashMap<>();
		for (Map.Entry<Integer, String> entry : importedPreset.getWaveCodeIds().entrySet())
		{
			Integer wave = entry.getKey();
			String importedReference = entry.getValue();
			if (wave == null || isBlank(importedReference))
			{
				continue;
			}

			WaveCode waveCode = importedWaveCodesByReference.get(waveReference(wave, importedReference));
			if (waveCode == null)
			{
				waveCode = findWaveCode(wave, importedReference);
			}

			if (waveCode != null)
			{
				localWaveCodeIds.put(wave, waveCode.getId());
			}
		}

		if (localWaveCodeIds.isEmpty())
		{
			return null;
		}

		if (isBlank(importedPreset.getName()))
		{
			userStore.setActiveRunPresetId(null);
			userStore.setActiveWaveCodeIds(localWaveCodeIds);
			save();
			RunPreset activePreset = new RunPreset(null, null, false, localWaveCodeIds);
			return new HealerCodeExportResult(
					null,
					null,
					runPresetDisplayName(activePreset),
					0,
					HealerCodeExportType.RUN_PRESET,
					presetSummary("Import", activePreset, savedWaveCodes)
			);
		}

		RunPreset preset = importRunPreset(importedPreset.getName(), localWaveCodeIds);
		userStore.setActiveRunPresetId(preset.getId());
		userStore.setActiveWaveCodeIds(preset.getWaveCodeIds());
		save();
		return new HealerCodeExportResult(
				null,
				preset.getId(),
				runPresetDisplayName(preset),
				0,
				HealerCodeExportType.RUN_PRESET,
				presetSummary("Import", preset, savedWaveCodes)
		);
	}

	private WaveCode importWaveCode(WaveCode importedWaveCode)
	{
		WaveCode candidate = copyWaveCode(importedWaveCode, null, false);
		WaveCode existing = findWaveCode(candidate.getWave(), candidate.getName());

		if (existing != null && sameWaveCodeContent(existing, candidate))
		{
			return existing;
		}

		if (existing != null)
		{
			candidate.setName(uniqueWaveCodeName(candidate.getWave(), candidate.getName()));
		}

		candidate.setId(uniqueUserId("wave", candidate.getWave() + "-" + candidate.getName()));
		candidate.setBuiltIn(false);
		List<WaveCode> waveCodes = new ArrayList<>(userStore.getWaveCodes());
		waveCodes.add(candidate);
		userStore.setWaveCodes(waveCodes);
		return candidate;
	}

	private RunPreset importRunPreset(String name, Map<Integer, String> waveCodeIds)
	{
		RunPreset existing = findRunPresetByName(name);
		if (existing != null && existing.getWaveCodeIds().equals(waveCodeIds))
		{
			return existing;
		}

		String savedName = existing == null ? name : uniquePresetName(name);
		RunPreset preset = new RunPreset(uniqueUserId("preset", savedName), savedName, false, waveCodeIds);
		List<RunPreset> presets = new ArrayList<>(userStore.getRunPresets());
		presets.add(preset);
		userStore.setRunPresets(presets);
		return preset;
	}

	private void upsertRunPreset(RunPreset importedPreset)
	{
		List<RunPreset> presets = new ArrayList<>(userStore.getRunPresets());

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

	private WaveCode exportableWaveCode(WaveCode waveCode)
	{
		return copyWaveCode(waveCode, null, false);
	}

	private WaveCode copyWaveCode(WaveCode source, String id, boolean builtIn)
	{
		WaveCode copy = new WaveCode(
				id,
				source.getName() == null ? "" : source.getName().trim(),
				source.getWave(),
				builtIn,
				copyCalls(source.getCalls())
		);
		copy.setOverstock(source.getOverstock());
		copy.setAlchHorn(source.isAlchHorn());
		copy.setRestockingInstructions(source.getRestockingInstructions());
		copy.setAdditionalNotes(source.getAdditionalNotes());
		copy.setExpectedTimesSeconds(source.getExpectedTimesSeconds());
		ensureCallCapacity(copy);
		return copy;
	}

	private static List<CallCode> copyCalls(List<CallCode> source)
	{
		List<CallCode> calls = new ArrayList<>();
		if (source != null)
		{
			for (CallCode call : source)
			{
				if (call != null)
				{
					calls.add(new CallCode(call.getCallIndex(), copyInstructions(call.getHealerInstructions()), call.getNote()));
				}
			}
		}
		return calls;
	}

	private static List<HealerInstruction> copyInstructions(List<HealerInstruction> source)
	{
		List<HealerInstruction> instructions = new ArrayList<>();
		if (source != null)
		{
			for (HealerInstruction instruction : source)
			{
				instructions.add(instruction == null ? new HealerInstruction() : instruction.copy());
			}
		}
		return instructions;
	}

	private static void ensureCallCapacity(WaveCode code)
	{
		List<CallCode> calls = new ArrayList<>(code.getCalls());
		while (calls.size() < HealerCodeFormatter.CALL_COUNT)
		{
			calls.add(new CallCode(calls.size(), new ArrayList<>(), null));
		}
		code.setCalls(calls);
	}

	private void replaceStoredUserWaveCode(WaveCode updated)
	{
		List<WaveCode> waveCodes = new ArrayList<>(userStore.getWaveCodes());
		for (int i = 0; i < waveCodes.size(); i++)
		{
			if (updated.getId().equals(waveCodes.get(i).getId()))
			{
				waveCodes.set(i, updated);
				userStore.setWaveCodes(waveCodes);
				return;
			}
		}
	}

	private void upsertStoredWaveCode(WaveCode updated)
	{
		List<WaveCode> waveCodes = new ArrayList<>(userStore.getWaveCodes());
		for (int i = 0; i < waveCodes.size(); i++)
		{
			if (updated.getId().equals(waveCodes.get(i).getId()))
			{
				waveCodes.set(i, updated);
				userStore.setWaveCodes(waveCodes);
				return;
			}
		}

		waveCodes.add(updated);
		userStore.setWaveCodes(waveCodes);
	}

	private boolean removeStoredWaveCode(String id)
	{
		if (id == null)
		{
			return false;
		}

		List<WaveCode> waveCodes = new ArrayList<>(userStore.getWaveCodes());
		boolean removed = waveCodes.removeIf(code -> code != null && id.equals(code.getId()));
		if (removed)
		{
			userStore.setWaveCodes(waveCodes);
		}
		return removed;
	}

	private boolean sameWaveCodeContent(WaveCode first, WaveCode second)
	{
		if (first == null || second == null)
		{
			return first == second;
		}

		return first.getWave() == second.getWave()
				&& sameText(first.getName(), second.getName())
				&& first.getOverstock() == second.getOverstock()
				&& first.isAlchHorn() == second.isAlchHorn()
				&& sameText(first.getRestockingInstructions(), second.getRestockingInstructions())
				&& sameText(first.getAdditionalNotes(), second.getAdditionalNotes())
				&& first.getExpectedTimesSeconds().equals(second.getExpectedTimesSeconds())
				&& sameCalls(first.getCalls(), second.getCalls());
	}

	private boolean sameCalls(List<CallCode> first, List<CallCode> second)
	{
		for (int callIndex = 0; callIndex < HealerCodeFormatter.CALL_COUNT; callIndex++)
		{
			CallCode firstCall = callAt(first, callIndex);
			CallCode secondCall = callAt(second, callIndex);
			if (!sameText(firstCall == null ? null : firstCall.getNote(), secondCall == null ? null : secondCall.getNote())
					|| !sameInstructions(
							firstCall == null ? null : firstCall.getHealerInstructions(),
							secondCall == null ? null : secondCall.getHealerInstructions()
					))
			{
				return false;
			}
		}
		return true;
	}

	private static CallCode callAt(List<CallCode> calls, int callIndex)
	{
		if (calls == null)
		{
			return null;
		}

		for (CallCode call : calls)
		{
			if (call != null && call.getCallIndex() == callIndex)
			{
				return call;
			}
		}
		return null;
	}

	private static boolean sameInstructions(List<HealerInstruction> first, List<HealerInstruction> second)
	{
		int size = Math.max(first == null ? 0 : first.size(), second == null ? 0 : second.size());
		for (int index = 0; index < size; index++)
		{
			HealerInstruction firstInstruction = instructionAt(first, index);
			HealerInstruction secondInstruction = instructionAt(second, index);
			if (firstInstruction.getTargetFoodCount() != secondInstruction.getTargetFoodCount()
					|| firstInstruction.getPostRestockFoodCount() != secondInstruction.getPostRestockFoodCount()
					|| firstInstruction.isAdvanced() != secondInstruction.isAdvanced()
					|| !sameInteger(firstInstruction.getAfterSeconds(), secondInstruction.getAfterSeconds())
					|| !sameInteger(firstInstruction.getBeforeSeconds(), secondInstruction.getBeforeSeconds())
					|| !sameInteger(firstInstruction.getExactSeconds(), secondInstruction.getExactSeconds())
					|| !sameText(advancedRaw(firstInstruction), advancedRaw(secondInstruction)))
			{
				return false;
			}
		}
		return true;
	}

	private static String advancedRaw(HealerInstruction instruction)
	{
		return instruction != null && instruction.isAdvanced() ? instruction.getRaw() : null;
	}

	private static HealerInstruction instructionAt(List<HealerInstruction> instructions, int index)
	{
		if (instructions == null || index < 0 || index >= instructions.size() || instructions.get(index) == null)
		{
			return new HealerInstruction();
		}
		return instructions.get(index);
	}

	private static boolean sameInteger(Integer first, Integer second)
	{
		return first == null ? second == null : first.equals(second);
	}

	private static boolean sameText(String first, String second)
	{
		return normalizeNullable(first).equals(normalizeNullable(second));
	}

	private String uniqueWaveCodeName(int wave, String baseName)
	{
		String cleanBaseName = isBlank(baseName) ? "Wave Code" : baseName.trim();
		String candidate = cleanBaseName;
		int suffix = 1;
		while (findWaveCode(wave, candidate) != null)
		{
			candidate = cleanBaseName + " (" + suffix++ + ")";
		}
		return candidate;
	}

	private String uniquePresetName(String baseName)
	{
		String cleanBaseName = isBlank(baseName) ? "Run Preset" : baseName.trim();
		String candidate = cleanBaseName;
		int suffix = 1;
		while (findRunPresetByName(candidate) != null)
		{
			candidate = cleanBaseName + " (" + suffix++ + ")";
		}
		return candidate;
	}

	private String uniqueUserId(String type, String name)
	{
		String baseId = userId(type, name);
		String candidate = baseId;
		int suffix = 1;
		while (findRunPreset(candidate) != null || findWaveCode(candidate) != null)
		{
			candidate = baseId + "-" + suffix++;
		}
		return candidate;
	}

	private List<String> waveSummary(String action, WaveCode code)
	{
		List<String> lines = new ArrayList<>();
		lines.add(action + " wave code: Wave " + code.getWave() + " - " + waveCodeDisplayName(code));
		addCodeSummaryLines(lines, code);
		return lines;
	}

	private List<String> presetSummary(String action, RunPreset preset, List<WaveCode> waveCodes)
	{
		List<String> lines = new ArrayList<>();
		lines.add(action + " run preset: " + runPresetDisplayName(preset));
		for (Map.Entry<Integer, String> entry : new java.util.TreeMap<>(preset.getWaveCodeIds()).entrySet())
		{
			WaveCode waveCode = findSummaryWaveCode(entry.getKey(), entry.getValue(), waveCodes);
			lines.add("");
			lines.add("Wave " + entry.getKey() + " - " + (waveCode == null ? entry.getValue() : waveCodeDisplayName(waveCode)));
			addCodeSummaryLines(lines, waveCode);
		}
		return lines;
	}

	private WaveCode findSummaryWaveCode(Integer wave, String reference, List<WaveCode> waveCodes)
	{
		WaveCode waveCode = findWaveCode(reference);
		if (waveCode != null)
		{
			return waveCode;
		}

		if (waveCodes == null)
		{
			return null;
		}

		for (WaveCode candidate : waveCodes)
		{
			if (candidate != null
					&& wave != null
					&& candidate.getWave() == wave
					&& sameName(candidate.getName(), reference))
			{
				return candidate;
			}
		}
		return null;
	}

	private static void addCodeSummaryLines(List<String> lines, WaveCode code)
	{
		if (code == null || isBlank(code.getSourceText()))
		{
			lines.add("No code text.");
			return;
		}

		for (String line : code.getSourceText().split("\\r?\\n", -1))
		{
			lines.add(line);
		}
	}

	private static String waveReference(int wave, String name)
	{
		return wave + ":" + normalizeName(name);
	}

	private WaveCode findStoredUserWaveCode(String id)
	{
		if (id == null)
		{
			return null;
		}

		for (WaveCode code : userStore.getWaveCodes())
		{
			if (id.equals(code.getId()))
			{
				return code;
			}
		}

		return null;
	}

	private WaveCode findWaveCode(int wave, String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		for (WaveCode code : getWaveCodes())
		{
			if (sameWaveCodeName(code, wave, name))
			{
				return code;
			}
		}

		return null;
	}

	private RunPreset findRunPresetByName(String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		for (RunPreset preset : getRunPresets())
		{
			if (sameName(name, preset.getName()))
			{
				return preset;
			}
		}

		return null;
	}

	private RunPreset findStoredUserRunPresetByName(String name)
	{
		if (isBlank(name))
		{
			return null;
		}

		for (RunPreset preset : userStore.getRunPresets())
		{
			if (sameName(name, preset.getName()))
			{
				return preset;
			}
		}

		return null;
	}

	private boolean sameWaveCodeName(WaveCode code, int wave, String name)
	{
		return code != null
				&& code.getWave() == wave
				&& sameName(code.getName(), name);
	}

	private static boolean sameName(String first, String second)
	{
		return normalizeName(first).equals(normalizeName(second));
	}

	private static String normalizeName(String name)
	{
		return name == null ? "" : name.trim().toLowerCase();
	}

	private static String normalizeNullable(String value)
	{
		return value == null ? "" : value.trim();
	}

	private static String normalizedExportName(String name)
	{
		return isBlank(name) ? null : name.trim();
	}

	public static String runPresetDisplayName(RunPreset preset)
	{
		return preset == null || isBlank(preset.getName()) ? "unnamed run preset" : preset.getName().trim();
	}

	public static String waveCodeDisplayName(WaveCode code)
	{
		return code == null || isBlank(code.getName()) ? "unnamed wave code" : code.getName().trim();
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}

	private static boolean isBuiltInWaveCodeId(String id)
	{
		return id != null && id.startsWith("builtin:");
	}

	private String userId(String type, String name)
	{
		String cleanName = name == null ? "code" : name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return "user:" + type + ":" + cleanName;
	}
}
