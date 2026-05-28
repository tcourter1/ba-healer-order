package com.bahealerorder;

import com.google.inject.Provides;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
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

	@Inject
	private ConfigManager configManager;

	@Getter
	private final Map<Integer, Integer> healerOrderByNpcIndex = new HashMap<>();

	@Getter
	private final Map<NPC, Integer> visibleHealers = new HashMap<>();

	@Getter
	private final Map<Integer, Integer> foodFedByHealerOrder = new HashMap<>();

	private int nextHealerNumber = 1;
	private int currentWave = -1;
	private Integer selectedPoisonedFoodItemId;
	private PendingFeedAttempt pendingFeedAttempt;

	private BaHealerOrderConfig.WaveListType lastWaveListType = null;

	private static final Pattern WAVE_PATTERN = Pattern.compile(".*---- Wave: (10|[1-9]) ----.*");

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
		Integer order = healerOrderByNpcIndex.get(npcIndex);

		if (order == null)
		{
			order = nextHealerNumber++;
			healerOrderByNpcIndex.put(npcIndex, order);
		}

		visibleHealers.put(npc, order);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		visibleHealers.remove(npc);
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

		int healerOrder = pendingFeedAttempt.healerOrder;
		foodFedByHealerOrder.merge(healerOrder, 1, Integer::sum);

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

	public int getCurrentWave()
	{
		return currentWave;
	}

	/**
	 * Sync display (editable) wave fields with the selected storage list.
	 * When switching lists, persist current display into previous storage,
	 * then load the newly selected storage into the display fields.
	 */
	public void syncDisplayWithSelected()
	{
		BaHealerOrderConfig.WaveListType current = config.waveListType();
		if (lastWaveListType == null)
		{
			lastWaveListType = current;
			loadStorageToDisplay(current);
			return;
		}

		if (current != lastWaveListType)
		{
			// Persist current display values into previous storage
			persistDisplayToStorage(lastWaveListType);

			// Load selected storage into display
			loadStorageToDisplay(current);

			lastWaveListType = current;
		}
	}

	private void persistDisplayToStorage(BaHealerOrderConfig.WaveListType target)
	{
		String prefix = (target == BaHealerOrderConfig.WaveListType.TAG) ? "tagWave" : "spamWave";

		for (int i = 1; i <= 10; i++)
		{
			String displayKey = "displayWave" + i;
			String storageKey = prefix + i;
			String val = configManager.getConfiguration("bahealerorder", displayKey, "");
			configManager.setConfiguration("bahealerorder", storageKey, val == null ? "" : val);
		}
	}

	private void loadStorageToDisplay(BaHealerOrderConfig.WaveListType source)
	{
		String prefix = (source == BaHealerOrderConfig.WaveListType.TAG) ? "tagWave" : "spamWave";

		for (int i = 1; i <= 10; i++)
		{
			String displayKey = "displayWave" + i;
			String storageKey = prefix + i;
			String val = configManager.getConfiguration("bahealerorder", storageKey, "");
			configManager.setConfiguration("bahealerorder", displayKey, val == null ? "" : val);
		}
	}

	// Config change listener removed because ConfigChanged event class is not
	// available in this build. The plugin reads config values on-demand, so
	// switching the `Wave List Type` in settings will immediately change which
	// list is used for expected values. If you want automatic reset behavior
	// on change, we can re-add a listener once the appropriate event class is
	// available in the classpath.

	private void startNewWave(int waveNumber)
	{
		if (waveNumber <= 0)
		{
			return;
		}

		this.currentWave = waveNumber;
		// clear previous wave state and start numbering fresh
		visibleHealers.clear();
		healerOrderByNpcIndex.clear();
		foodFedByHealerOrder.clear();
		pendingFeedAttempt = null;
		selectedPoisonedFoodItemId = null;
		nextHealerNumber = 1;

		log.debug("Starting new BA wave {}", waveNumber);
	}

	public int getExpectedFoodForOrder(int healerOrder)
	{
		if (healerOrder <= 0)
		{
			return 0;
		}

		String waveConfig = "";
		boolean useTag = config.waveListType() == BaHealerOrderConfig.WaveListType.TAG;
		switch (currentWave)
		{
			case 1: waveConfig = useTag ? config.tagWave1() : config.spamWave1(); break;
			case 2: waveConfig = useTag ? config.tagWave2() : config.spamWave2(); break;
			case 3: waveConfig = useTag ? config.tagWave3() : config.spamWave3(); break;
			case 4: waveConfig = useTag ? config.tagWave4() : config.spamWave4(); break;
			case 5: waveConfig = useTag ? config.tagWave5() : config.spamWave5(); break;
			case 6: waveConfig = useTag ? config.tagWave6() : config.spamWave6(); break;
			case 7: waveConfig = useTag ? config.tagWave7() : config.spamWave7(); break;
			case 8: waveConfig = useTag ? config.tagWave8() : config.spamWave8(); break;
			case 9: waveConfig = useTag ? config.tagWave9() : config.spamWave9(); break;
			case 10: waveConfig = useTag ? config.tagWave10() : config.spamWave10(); break;
			default: waveConfig = ""; break;
		}

		if (waveConfig == null || waveConfig.trim().isEmpty())
		{
			return 0;
		}

		String[] parts = waveConfig.split(",");

		if (healerOrder > parts.length)
		{
			return 0;
		}

		try
		{
			String part = parts[healerOrder - 1].trim();
			if (part.isEmpty())
			{
				return 0;
			}
			return Integer.parseInt(part);
		}
		catch (Exception ex)
		{
			return 0;
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

	public Map<NPC, Integer> getTrackedHealers()
	{
		return Collections.unmodifiableMap(visibleHealers);
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
				healerOrder,
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

			resetWaveState();
			currentWave = wave;
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
		visibleHealers.clear();
		healerOrderByNpcIndex.clear();
		foodFedByHealerOrder.clear();
		pendingFeedAttempt = null;
		selectedPoisonedFoodItemId = null;
		nextHealerNumber = 1;
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

	private static class PendingFeedAttempt
	{
		private final int healerOrder;
		private final int foodItemId;
		private final int foodCountBeforeUse;

		private PendingFeedAttempt(int healerOrder, int foodItemId, int foodCountBeforeUse)
		{
			this.healerOrder = healerOrder;
			this.foodItemId = foodItemId;
			this.foodCountBeforeUse = foodCountBeforeUse;
		}
	}
}