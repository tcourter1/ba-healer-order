package com.bahealerorder;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
		name = "BA Healer Utilities",
		description = "Displays Barbarian Assault Penance Healer spawn order above each healer.",
		tags = {"barbarian assault", "ba", "healer", "penance", "overlay"}
)
public class BaHealerOrderPlugin extends Plugin
{
	private static final String PENANCE_HEALER_NAME = "Penance Healer";
	private static final String WRONG_FOOD_MESSAGE = "that's the wrong type of poisoned food to use! penalty!";
	private static final Pattern WAVE_START_PATTERN = Pattern.compile(".*\\bwave:\\s*(\\d+)\\b.*");
	private static final Pattern WAVE_PATTERN = Pattern.compile(".*---- Wave: (10|[1-9]) ----.*");
	private static final Pattern FOOD_PART_PATTERN = Pattern.compile("^\\s*(\\d+)(?:\\(([^)]*)\\))?");

	private static final int WAVE_INCREMENT_INTERVAL_SECONDS = 30;

	private static final String[][] TIME_BASED_HEALER_LABELS = {
			{},
			{"6", "12"},
			{"6", "12", "18"},
			{"6", "12", "R1"},
			{"6", "12", "18", "R1"},
			{"6", "12", "18", "24", "R1"},
			{"6", "12", "18", "24", "R1", "R2"},
			{"6", "12", "18", "24", "R1", "R2", "R3"},
			{"6", "12", "18", "24", "30", "R1", "R2"},
			{"6", "12", "18", "24", "30", "36", "R1", "R2"},
			{"6", "12", "18", "24", "R1", "R2", "R3"}
	};

	@Getter
	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BaHealerOrderOverlay overlay;

	@Inject
	private BaHealerOrderFoodOverlay foodOverlay;

	@Inject
	private BaHealerOrderConfig config;

	@Getter
	private final Map<Integer, Integer> healerOrderByNpcIndex = new HashMap<>();

	@Getter
	private final Map<NPC, Integer> visibleHealers = new HashMap<>();

	private final Set<Integer> healerIndexesSeenThisWave = new HashSet<>();
	private final Map<Integer, Integer> foodFedByNpcIndex = new HashMap<>();

	private int currentWave = -1;
	private long waveStartTimeMs = -1;
	private Integer selectedPoisonedFoodItemId;
	private PendingFeedAttempt pendingFeedAttempt;

	@Override
	protected void startUp()
	{
		resetAllState();
		overlayManager.add(overlay);
		overlayManager.add(foodOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(foodOverlay);
		overlayManager.remove(overlay);
		resetAllState();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();

		if (!isPenanceHealer(npc))
		{
			return;
		}

		int npcIndex = npc.getIndex();
		boolean addedNewIndex = healerIndexesSeenThisWave.add(npcIndex);

		rebuildHealerOrderByNpcIndex();
		rebuildVisibleHealerOrders();

		Integer order = healerOrderByNpcIndex.get(npcIndex);

		if (order == null)
		{
			return;
		}

		visibleHealers.put(npc, order);

		if (addedNewIndex)
		{
			log.debug("Registered Penance Healer index {} as corrected healer #{}", npcIndex, order);
		}
		else
		{
			log.debug("Re-associated Penance Healer index {} with corrected healer #{}", npcIndex, order);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();

		if (visibleHealers.remove(npc) != null)
		{
			log.debug("Removed visible Penance Healer index {}", npc.getIndex());
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ROOT);
		String target = Text.removeTags(event.getMenuTarget()).toLowerCase(Locale.ROOT);

		handlePoisonedFoodSelection(event, option, target);
		handlePoisonedFoodUseOnHealer(event, option, target);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.showMenuLabel()
				|| config.healerLabelStyle() == BaHealerOrderConfig.HealerLabelStyle.NONE)
		{
			return;
		}

		MenuEntry entry = event.getMenuEntry();

		if (entry == null)
		{
			return;
		}

		Integer healerOrder = getHealerOrderForMenuEntry(entry);

		if (healerOrder == null)
		{
			return;
		}

		String target = entry.getTarget();

		if (target == null || Text.removeTags(target).contains(" ("))
		{
			return;
		}

		String label = getHealerLabel(healerOrder);
		entry.setTarget(target + " " + ColorUtil.wrapWithColorTag("(" + label + ")", config.textColor()));
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId())
		{
			return;
		}

		if (pendingFeedAttempt == null)
		{
			return;
		}

		int currentFoodCount = getItemCount(event.getItemContainer().getItems(), pendingFeedAttempt.foodItemId);

		if (currentFoodCount >= pendingFeedAttempt.foodCountBeforeUse)
		{
			return;
		}

		foodFedByNpcIndex.merge(pendingFeedAttempt.npcIndex, 1, Integer::sum);

		Integer currentOrder = healerOrderByNpcIndex.get(pendingFeedAttempt.npcIndex);
		int totalFoodFed = foodFedByNpcIndex.getOrDefault(pendingFeedAttempt.npcIndex, 0);

		log.debug(
				"Counted consumed poisoned food for healer #{} from NPC index {}. Item {} went from {} to {}. Total now {}",
				currentOrder,
				pendingFeedAttempt.npcIndex,
				pendingFeedAttempt.foodItemId,
				pendingFeedAttempt.foodCountBeforeUse,
				currentFoodCount,
				totalFoodFed
		);

		pendingFeedAttempt = null;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		final Matcher waveMatcher = WAVE_PATTERN.matcher(event.getMessage());

		if (waveMatcher.matches())
		{
			try
			{
				int waveNum = Integer.parseInt(waveMatcher.group(1));
				startNewWave(waveNum);
			}
			catch (NumberFormatException ex)
			{
				log.debug("Failed to parse wave number from message: {}", event.getMessage());
			}
		}

		String message = Text.removeTags(event.getMessage()).toLowerCase(Locale.ROOT);

		if (handleWaveStartMessage(message))
		{
			return;
		}

		if (message.contains(WRONG_FOOD_MESSAGE))
		{
			log.debug("Wrong poisoned food detected. Cancelling pending feed attempt.");
			pendingFeedAttempt = null;
			return;
		}

		if (isWaveEndMessage(event.getType(), message))
		{
			resetWaveState();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();

		if (gameState == GameState.LOGIN_SCREEN
				|| gameState == GameState.HOPPING)
		{
			resetAllState();
		}
	}

	public int getCurrentWave()
	{
		return currentWave;
	}

	public long getCurrentWaveElapsedMillis()
	{
		if (waveStartTimeMs <= 0 || currentWave <= 0)
		{
			return 0;
		}

		return System.currentTimeMillis() - waveStartTimeMs;
	}

	public float getCurrentWaveElapsedSeconds()
	{
		return getCurrentWaveElapsedMillis() / 1000f;
	}

	public int getExpectedFoodForOrder(int healerOrder)
	{
		if (healerOrder <= 0)
		{
			return 0;
		}

		String waveConfig = "";
		BaHealerOrderConfig.HealerRole selectedRole = config.healerRole();

		switch (selectedRole)
		{
			case TAG:
				waveConfig = getTagWaveConfig(currentWave);
				break;
			case SPAM:
				waveConfig = getSpamWaveConfig(currentWave);
				break;
			case SOLO:
				waveConfig = getSoloWaveConfig(currentWave);
				break;
			default:
				waveConfig = "";
				break;
		}

		return getExpectedFoodForOrder(healerOrder, waveConfig);
	}

	public int getExpectedFoodForOrder(int healerOrder, BaHealerOrderConfig.HealerRole listRole)
	{
		if (healerOrder <= 0)
		{
			return 0;
		}

		String waveConfig = "";

		switch (listRole)
		{
			case TAG:
				waveConfig = getTagWaveConfig(currentWave);
				break;
			case SPAM:
				waveConfig = getSpamWaveConfig(currentWave);
				break;
			case SOLO:
				waveConfig = getSoloWaveConfig(currentWave);
				break;
			default:
				waveConfig = "";
				break;
		}

		return getExpectedFoodForOrder(healerOrder, waveConfig);
	}

	public String getHealerTarget(int healerOrder)
	{
		if (healerOrder <= 0)
		{
			return null;
		}

		String waveConfig = "";
		BaHealerOrderConfig.HealerRole selectedRole = config.healerRole();

		switch (selectedRole)
		{
			case TAG:
				waveConfig = getTagWaveConfig(currentWave);
				break;
			case SPAM:
				waveConfig = getSpamWaveConfig(currentWave);
				break;
			case SOLO:
				waveConfig = getSoloWaveConfig(currentWave);
				break;
			default:
				waveConfig = "";
				break;
		}

		return getHealerTarget(healerOrder, waveConfig);
	}

	public String getHealerTarget(int healerOrder, BaHealerOrderConfig.HealerRole listRole)
	{
		if (healerOrder <= 0)
		{
			return null;
		}

		String waveConfig = "";

		switch (listRole)
		{
			case TAG:
				waveConfig = getTagWaveConfig(currentWave);
				break;
			case SPAM:
				waveConfig = getSpamWaveConfig(currentWave);
				break;
			case SOLO:
				waveConfig = getSoloWaveConfig(currentWave);
				break;
			default:
				waveConfig = "";
				break;
		}

		return getHealerTarget(healerOrder, waveConfig);
	}

	public Map<NPC, Integer> getTrackedHealers()
	{
		return Collections.unmodifiableMap(visibleHealers);
	}

	public Map<Integer, Integer> getFoodFedByHealerOrder()
	{
		Map<Integer, Integer> foodFedByHealerOrder = new HashMap<>();

		for (Map.Entry<Integer, Integer> entry : foodFedByNpcIndex.entrySet())
		{
			Integer npcIndex = entry.getKey();
			Integer foodFed = entry.getValue();
			Integer healerOrder = healerOrderByNpcIndex.get(npcIndex);

			if (healerOrder == null)
			{
				continue;
			}

			foodFedByHealerOrder.merge(healerOrder, foodFed, Integer::sum);
		}

		return Collections.unmodifiableMap(foodFedByHealerOrder);
	}

	public String getHealerLabel(int healerOrder)
	{
		if (config.healerLabelStyle() == BaHealerOrderConfig.HealerLabelStyle.SPAWN_ORDER)
		{
			return String.valueOf(healerOrder);
		}

		if (currentWave <= 0 || currentWave >= TIME_BASED_HEALER_LABELS.length)
		{
			return String.valueOf(healerOrder);
		}

		String[] labelsForWave = TIME_BASED_HEALER_LABELS[currentWave];

		if (healerOrder <= 0 || healerOrder > labelsForWave.length)
		{
			return String.valueOf(healerOrder);
		}

		return labelsForWave[healerOrder - 1];
	}

	private void startNewWave(int waveNumber)
	{
		if (waveNumber <= 0)
		{
			return;
		}

		this.currentWave = waveNumber;
		this.waveStartTimeMs = System.currentTimeMillis();
		resetWaveTrackedState();

		log.debug("Starting new BA wave {}", waveNumber);
	}

	private void rebuildHealerOrderByNpcIndex()
	{
		List<Integer> sortedIndexes = new ArrayList<>(healerIndexesSeenThisWave);
		Collections.sort(sortedIndexes);

		healerOrderByNpcIndex.clear();

		for (int i = 0; i < sortedIndexes.size(); i++)
		{
			healerOrderByNpcIndex.put(sortedIndexes.get(i), i + 1);
		}
	}

	private void rebuildVisibleHealerOrders()
	{
		if (visibleHealers.isEmpty())
		{
			return;
		}

		for (Map.Entry<NPC, Integer> entry : new ArrayList<>(visibleHealers.entrySet()))
		{
			NPC npc = entry.getKey();

			if (npc == null)
			{
				visibleHealers.remove(npc);
				continue;
			}

			Integer correctedOrder = healerOrderByNpcIndex.get(npc.getIndex());

			if (correctedOrder == null)
			{
				continue;
			}

			Integer previousOrder = entry.getValue();

			if (!correctedOrder.equals(previousOrder))
			{
				log.debug(
						"Corrected Penance Healer index {} from healer #{} to healer #{}",
						npc.getIndex(),
						previousOrder,
						correctedOrder
				);
			}

			visibleHealers.put(npc, correctedOrder);
		}
	}

	private int getExpectedFoodForOrder(int healerOrder, String waveConfig)
	{
		if (healerOrder <= 0 || waveConfig == null || waveConfig.isEmpty())
		{
			return 0;
		}

		List<WaveProgressionStep> progression = parseWaveFoodConfig(waveConfig);

		if (progression.isEmpty())
		{
			return 0;
		}

		int orderIndex = healerOrder - 1;
		int expectedFood = getValueAtLine(progression.get(0).amounts, orderIndex);
		int stepIndex = (int) (getCurrentWaveElapsedSeconds() / WAVE_INCREMENT_INTERVAL_SECONDS);

		for (int i = 1; i < progression.size() && i <= stepIndex; i++)
		{
			expectedFood += getValueAtLine(progression.get(i).amounts, orderIndex);
		}

		return expectedFood;
	}

	private String getHealerTarget(int healerOrder, String waveConfig)
	{
		if (healerOrder <= 0 || waveConfig == null || waveConfig.isEmpty())
		{
			return null;
		}

		List<WaveProgressionStep> progression = parseWaveFoodConfig(waveConfig);

		if (progression.isEmpty())
		{
			return null;
		}

		int orderIndex = healerOrder - 1;
		int stepIndex = (int) (getCurrentWaveElapsedSeconds() / WAVE_INCREMENT_INTERVAL_SECONDS);
		String target = null;

		for (int i = 0; i < progression.size() && i <= stepIndex; i++)
		{
			String lineTarget = getTargetAtLine(progression.get(i), orderIndex);

			if (lineTarget != null)
			{
				target = lineTarget;
			}
		}

		if (target == null)
		{
			return null;
		}

		return "(" + target + ")";
	}

	private List<WaveProgressionStep> parseWaveFoodConfig(String waveConfig)
	{
		List<WaveProgressionStep> progression = new ArrayList<>();

		if (waveConfig == null || waveConfig.isEmpty())
		{
			return progression;
		}

		for (String rawLine : waveConfig.split("\\\\|\\r?\\n"))
		{
			if (rawLine == null)
			{
				continue;
			}

			String line = rawLine.trim();

			if (line.isEmpty())
			{
				continue;
			}

			String[] parts = line.split("\\s*[-,]\\s*");
			int[] values = new int[parts.length];
			String[] targets = new String[parts.length];
			int parsedCount = 0;

			for (String rawPart : parts)
			{
				String part = rawPart.trim();

				if (part.isEmpty())
				{
					continue;
				}

				part = part.replaceAll("\\[[^]]*\\]", "");
				Matcher matcher = FOOD_PART_PATTERN.matcher(part);

				if (!matcher.find())
				{
					continue;
				}

				try
				{
					values[parsedCount] = Integer.parseInt(matcher.group(1));
				}
				catch (NumberFormatException ex)
				{
					values[parsedCount] = 0;
				}

				if (matcher.group(2) != null && !matcher.group(2).trim().isEmpty())
				{
					targets[parsedCount] = matcher.group(2).trim();
				}

				parsedCount++;
			}

			if (parsedCount == 0)
			{
				continue;
			}

			if (parsedCount < values.length)
			{
				int[] trimmedValues = new int[parsedCount];
				String[] trimmedTargets = new String[parsedCount];
				System.arraycopy(values, 0, trimmedValues, 0, parsedCount);
				System.arraycopy(targets, 0, trimmedTargets, 0, parsedCount);
				progression.add(new WaveProgressionStep(trimmedValues, trimmedTargets));
			}
			else
			{
				progression.add(new WaveProgressionStep(values, targets));
			}
		}

		return progression;
	}

	private String getTargetAtLine(WaveProgressionStep step, int orderIndex)
	{
		if (step == null || step.targets == null || orderIndex < 0 || orderIndex >= step.targets.length)
		{
			return null;
		}

		return step.targets[orderIndex];
	}

	private int getValueAtLine(int[] values, int orderIndex)
	{
		if (values == null || orderIndex < 0 || orderIndex >= values.length)
		{
			return 0;
		}

		return values[orderIndex];
	}

	private String getTagWaveConfig(int wave)
	{
		switch (wave)
		{
			case 1: return config.tagWave1();
			case 2: return config.tagWave2();
			case 3: return config.tagWave3();
			case 4: return config.tagWave4();
			case 5: return config.tagWave5();
			case 6: return config.tagWave6();
			case 7: return config.tagWave7();
			case 8: return config.tagWave8();
			case 9: return config.tagWave9();
			case 10: return config.tagWave10();
			default: return "";
		}
	}

	private String getSpamWaveConfig(int wave)
	{
		switch (wave)
		{
			case 1: return config.spamWave1();
			case 2: return config.spamWave2();
			case 3: return config.spamWave3();
			case 4: return config.spamWave4();
			case 5: return config.spamWave5();
			case 6: return config.spamWave6();
			case 7: return config.spamWave7();
			case 8: return config.spamWave8();
			case 9: return config.spamWave9();
			case 10: return config.spamWave10();
			default: return "";
		}
	}

	private String getSoloWaveConfig(int wave)
	{
		switch (wave)
		{
			case 1: return config.soloWave1();
			case 2: return config.soloWave2();
			case 3: return config.soloWave3();
			case 4: return config.soloWave4();
			case 5: return config.soloWave5();
			case 6: return config.soloWave6();
			case 7: return config.soloWave7();
			case 8: return config.soloWave8();
			case 9: return config.soloWave9();
			case 10: return config.soloWave10();
			default: return "";
		}
	}

	private void handlePoisonedFoodSelection(MenuOptionClicked event, String option, String target)
	{
		if (event.getMenuAction() != MenuAction.WIDGET_TARGET)
		{
			return;
		}

		if (!"use".equals(option))
		{
			return;
		}

		if (!target.contains("poisoned"))
		{
			return;
		}

		selectedPoisonedFoodItemId = event.getItemId();
	}

	private void handlePoisonedFoodUseOnHealer(MenuOptionClicked event, String option, String target)
	{
		if (event.getMenuAction() != MenuAction.WIDGET_TARGET_ON_NPC)
		{
			return;
		}

		if (!"use".equals(option))
		{
			return;
		}

		if (!target.contains("poisoned") || !target.contains("penance healer"))
		{
			return;
		}

		if (selectedPoisonedFoodItemId == null || selectedPoisonedFoodItemId <= 0)
		{
			return;
		}

		int npcIndex = event.getId();
		Integer healerOrder = healerOrderByNpcIndex.get(npcIndex);

		if (healerOrder == null)
		{
			return;
		}

		int foodCountBeforeUse = getInventoryItemCount(selectedPoisonedFoodItemId);

		pendingFeedAttempt = new PendingFeedAttempt(
				npcIndex,
				selectedPoisonedFoodItemId,
				foodCountBeforeUse
		);

		log.debug(
				"Pending food feed for healer #{} from NPC index {} using item id {}. Count before use: {}",
				healerOrder,
				npcIndex,
				selectedPoisonedFoodItemId,
				foodCountBeforeUse
		);
	}

	private int getInventoryItemCount(int itemId)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);

		if (inventory == null)
		{
			return 0;
		}

		return getItemCount(inventory.getItems(), itemId);
	}

	private int getItemCount(Item[] items, int itemId)
	{
		int count = 0;

		if (items == null)
		{
			return count;
		}

		for (Item item : items)
		{
			if (item != null && item.getId() == itemId)
			{
				count += item.getQuantity();
			}
		}

		return count;
	}

	private boolean isPenanceHealer(NPC npc)
	{
		if (npc == null || npc.getName() == null)
		{
			return false;
		}

		String name = Text.removeTags(npc.getName());
		return PENANCE_HEALER_NAME.equals(name);
	}

	private Integer getHealerOrderForMenuEntry(MenuEntry entry)
	{
		if (!isNpcMenuAction(entry.getType()))
		{
			return null;
		}

		NPC npc = entry.getNpc();

		if (npc != null)
		{
			return healerOrderByNpcIndex.get(npc.getIndex());
		}

		return healerOrderByNpcIndex.get(entry.getIdentifier());
	}

	private boolean isNpcMenuAction(MenuAction action)
	{
		return action == MenuAction.NPC_FIRST_OPTION
				|| action == MenuAction.NPC_SECOND_OPTION
				|| action == MenuAction.NPC_THIRD_OPTION
				|| action == MenuAction.NPC_FOURTH_OPTION
				|| action == MenuAction.NPC_FIFTH_OPTION
				|| action == MenuAction.WIDGET_TARGET_ON_NPC
				|| action == MenuAction.ITEM_USE_ON_NPC
				|| action == MenuAction.EXAMINE_NPC;
	}

	private boolean handleWaveStartMessage(String message)
	{
		Matcher matcher = WAVE_START_PATTERN.matcher(message);

		if (!matcher.matches())
		{
			return false;
		}

		try
		{
			int wave = Integer.parseInt(matcher.group(1));

			if (wave < 1 || wave > 10)
			{
				return false;
			}

			startNewWave(wave);
			return true;
		}
		catch (NumberFormatException ex)
		{
			log.debug("Failed to parse BA wave start message: {}", message, ex);
			return false;
		}
	}

	private boolean isWaveEndMessage(ChatMessageType type, String message)
	{
		if (type != ChatMessageType.GAMEMESSAGE
				&& type != ChatMessageType.SPAM
				&& type != ChatMessageType.WELCOME
				&& type != ChatMessageType.CONSOLE)
		{
			return false;
		}

		return message.contains("wave")
				&& (
				message.contains("complete")
						|| message.contains("completed")
						|| message.contains("duration")
						|| message.contains("congratulations")
						|| message.contains("queen")
		);
	}

	private void resetWaveState()
	{
		resetWaveTrackedState();
		waveStartTimeMs = -1;
	}

	private void resetWaveTrackedState()
	{
		visibleHealers.clear();
		healerIndexesSeenThisWave.clear();
		healerOrderByNpcIndex.clear();
		foodFedByNpcIndex.clear();
		pendingFeedAttempt = null;
		selectedPoisonedFoodItemId = null;
	}

	private void resetAllState()
	{
		resetWaveState();
		currentWave = -1;
	}

	@Provides
	BaHealerOrderConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BaHealerOrderConfig.class);
	}

	private static class WaveProgressionStep
	{
		private final int[] amounts;
		private final String[] targets;

		private WaveProgressionStep(int[] amounts, String[] targets)
		{
			this.amounts = amounts;
			this.targets = targets;
		}
	}

	private static class PendingFeedAttempt
	{
		private final int npcIndex;
		private final int foodItemId;
		private final int foodCountBeforeUse;

		private PendingFeedAttempt(int npcIndex, int foodItemId, int foodCountBeforeUse)
		{
			this.npcIndex = npcIndex;
			this.foodItemId = foodItemId;
			this.foodCountBeforeUse = foodCountBeforeUse;
		}
	}
}