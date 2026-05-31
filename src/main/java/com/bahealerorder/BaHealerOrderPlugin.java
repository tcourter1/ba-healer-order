package com.bahealerorder;

import com.bahealerorder.codes.CodeDisplayState;
import com.bahealerorder.codes.FeedEvent;
import com.bahealerorder.codes.HealerCodeStatus;
import com.bahealerorder.codes.WaveCode;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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
import javax.imageio.ImageIO;
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
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
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
	private static final String PANEL_ICON_RESOURCE = "/com/bahealerorder/penance_healer.png";
	private static final int BA_HEALER_GROUP_ID = 488;
	private static final int BA_HEALER_CALL_CHILD_ID = 9;
	private static final Pattern WAVE_START_PATTERN = Pattern.compile(".*\\bwave:\\s*(\\d+)\\b.*");
	private static final Pattern WAVE_PATTERN = Pattern.compile(".*---- Wave: (10|[1-9]) ----.*");
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
	private ClientToolbar clientToolbar;

	@Inject
	private BaHealerOrderOverlay overlay;

	@Inject
	private BaHealerOrderFoodOverlay foodOverlay;

	@Inject
	private BaHealerOrderPanel panel;

	@Inject
	private BaHealerCodeManager codeManager;

	@Inject
	private BaHealerOrderConfig config;

	@Getter
	private final Map<Integer, Integer> healerOrderByNpcIndex = new HashMap<>();

	@Getter
	private final Map<NPC, Integer> visibleHealers = new HashMap<>();

	private final Set<Integer> healerIndexesSeenThisWave = new HashSet<>();
	private final Map<Integer, Integer> foodFedByNpcIndex = new HashMap<>();

	@Getter
	private final List<FeedEvent> feedEvents = new ArrayList<>();

	private int currentWave = -1;
	private int currentCallIndex = 0;
	private long waveStartTimeMs = -1;
	private String lastCallText;
	private String currentCallText;
	private String currentCallSource;
	private boolean callTrackingArmed;
	private Integer selectedPoisonedFoodItemId;
	private PendingFeedAttempt pendingFeedAttempt;
	private NavigationButton navigationButton;

	@Override
	protected void startUp()
	{
		codeManager.load();
		panel.refreshAll();
		resetAllState();
		overlayManager.add(overlay);
		overlayManager.add(foodOverlay);
		navigationButton = NavigationButton.builder()
				.tooltip("BA Healer Utilities")
				.icon(createPanelIcon())
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navigationButton);
	}

	@Override
	protected void shutDown()
	{
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
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

		if (currentOrder != null)
		{
			feedEvents.add(new FeedEvent(currentOrder, Math.round(getCurrentWaveElapsedSeconds()), currentCallIndex));
		}

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
	public void onGameTick(GameTick event)
	{
		updateCallIndexFromHealerWidget();
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

	public int getCurrentCallIndex()
	{
		return currentCallIndex;
	}

	public String getLastCallText()
	{
		return lastCallText;
	}

	public String getCurrentCallText()
	{
		return currentCallText;
	}

	public String getCurrentCallSource()
	{
		return currentCallSource;
	}

	public String getCurrentWaveCodeSource()
	{
		WaveCode waveCode = codeManager.getActiveWaveCode(currentWave);
		return waveCode == null ? null : waveCode.getSourceText();
	}

	public int getExpectedFoodForOrder(int healerOrder)
	{
		if (healerOrder <= 0)
		{
			return 0;
		}

		return codeManager.getExpectedFoodForOrder(currentWave, healerOrder, currentCallIndex);
	}

	public String getHealerTarget(int healerOrder)
	{
		if (healerOrder <= 0)
		{
			return null;
		}

		HealerCodeStatus status = getCurrentCodeStatus(healerOrder);
		return status == null ? null : formatCodeStatus(status);
	}

	public Map<NPC, Integer> getTrackedHealers()
	{
		return Collections.unmodifiableMap(visibleHealers);
	}

	public HealerCodeStatus getCurrentCodeStatus(int healerOrder)
	{
		return codeManager.getCurrentStatus(currentWave, healerOrder, currentCallIndex, feedEvents);
	}

	public HealerCodeStatus getPreviousCodeStatus(int healerOrder)
	{
		return codeManager.getPreviousStatus(currentWave, healerOrder, currentCallIndex, feedEvents);
	}

	public HealerCodeStatus getDisplayCodeStatus(int healerOrder)
	{
		return codeManager.getDisplayStatus(currentWave, healerOrder, currentCallIndex, feedEvents);
	}

	public String formatCodeStatus(HealerCodeStatus status)
	{
		if (status == null || status.getInstruction() == null || !status.getInstruction().hasTarget())
		{
			return null;
		}

		StringBuilder builder = new StringBuilder();
		builder.append(status.getFoodFed()).append('/').append(status.getInstruction().getTargetFoodCount());

		if (status.getInstruction().getAfterSeconds() != null)
		{
			builder.append(" (").append(status.getInstruction().getAfterSeconds()).append(')');
		}

		if (status.getInstruction().getBeforeSeconds() != null)
		{
			builder.append(" [").append(status.getInstruction().getBeforeSeconds()).append(']');
		}

		return builder.toString();
	}

	public Color getCodeStatusColor(CodeDisplayState state)
	{
		if (state == CodeDisplayState.COMPLETE)
		{
			return new Color(0, 220, 0);
		}

		if (state == CodeDisplayState.PREVIOUS)
		{
			return new Color(150, 150, 150);
		}

		if (state == CodeDisplayState.IN_PROGRESS)
		{
			return new Color(255, 150, 0);
		}

		return new Color(255, 60, 60);
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

	private void updateCallIndexFromHealerWidget()
	{
		if (currentWave <= 0)
		{
			return;
		}

		String callText = getHealerCallText();

		if (callText == null || callText.isEmpty())
		{
			return;
		}

		if (lastCallText == null)
		{
			lastCallText = callText;
			return;
		}

		if (!callTrackingArmed)
		{
			lastCallText = callText;
			callTrackingArmed = true;
			return;
		}

		if (!lastCallText.equals(callText))
		{
			currentCallIndex++;
			lastCallText = callText;
			log.debug("BA healer call changed to {} at wave {} call {}", callText, currentWave, currentCallIndex);
		}
	}

	private String getHealerCallText()
	{
		WidgetText callText = getWidgetText(BA_HEALER_GROUP_ID, BA_HEALER_CALL_CHILD_ID, "healer call");

		if (callText == null)
		{
			currentCallSource = null;
			currentCallText = null;
			return null;
		}

		currentCallSource = callText.source;
		currentCallText = normalizeCallText(callText.text);
		return currentCallText;
	}

	private WidgetText getWidgetText(int groupId, int childId, String source)
	{
		Widget widget = client.getWidget(groupId, childId);

		if (widget == null || widget.getText() == null)
		{
			return null;
		}

		String text = Text.removeTags(widget.getText()).trim();
		return text.isEmpty() ? null : new WidgetText(text, source + " " + groupId + ":" + childId);
	}

	private String normalizeCallText(String text)
	{
		return text.toLowerCase(Locale.ROOT);
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
		feedEvents.clear();
		pendingFeedAttempt = null;
		selectedPoisonedFoodItemId = null;
		currentCallIndex = 0;
		lastCallText = null;
		currentCallText = null;
		currentCallSource = null;
		callTrackingArmed = false;
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

	private BufferedImage createPanelIcon()
	{
		try (InputStream inputStream = getClass().getResourceAsStream(PANEL_ICON_RESOURCE))
		{
			if (inputStream != null)
			{
				BufferedImage source = ImageIO.read(inputStream);
				BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = image.createGraphics();
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				graphics.drawImage(source, 0, 0, 16, 16, null);
				graphics.dispose();
				return image;
			}
		}
		catch (IOException ex)
		{
			log.debug("Unable to load BA healer panel icon", ex);
		}

		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(0, 125, 70));
		graphics.fillOval(1, 1, 14, 14);
		graphics.dispose();
		return image;
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

	private static class WidgetText
	{
		private final String text;
		private final String source;

		private WidgetText(String text, String source)
		{
			this.text = text;
			this.source = source;
		}
	}
}
