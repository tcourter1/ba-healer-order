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
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
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
	private static final String HEALER_ITEM_MACHINE_NAME = "Healer item machine";
	private static final String TAKE_TOFU_OPTION = "take-tofu";
	private static final String TAKE_WORMS_OPTION = "take-worms";
	private static final String TAKE_MEAT_OPTION = "take-meat";
	private static final String TAKE_VIAL_OPTION = "take-vial";
	private static final String WRONG_FOOD_MESSAGE = "that's the wrong type of poisoned food to use! penalty!";
	private static final String PANEL_ICON_RESOURCE = "/com/bahealerorder/penance_healer.png";
	private static final int BA_HORN_OF_GLORY_GROUP_ID = 484;
	private static final int BA_HEALER_GROUP_ID = 488;
	private static final int BA_HEALER_LISTEN_CHILD_ID = 7;
	// This is the healer-side "to call" widget, not the defender's actual horn call.
	// It changes with the real BA call cycle even if the defender calls late or not at all.
	private static final int BA_HEALER_CALL_CHILD_ID = 9;
	private static final int BA_HORN_OF_GLORY_DEFENDER_CHILD_ID = 6;
	private static final Color CALLED_FOOD_MENU_COLOR = Color.GREEN;
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
	// Food has to be counted by NPC index first. Healer order can be corrected as later
	// NPC indexes appear, but the index itself remains stable for the spawned healer.
	private final Map<Integer, Integer> foodFedByNpcIndex = new HashMap<>();

	@Getter
	private final List<FeedEvent> feedEvents = new ArrayList<>();

	private int currentWave = -1;
	private int currentCallIndex = 0;
	private int inGameBit;
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
				.priority(10)
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
		MenuEntry entry = event.getMenuEntry();

		if (entry == null)
		{
			return;
		}

		addHealerMenuLabel(entry);
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		applyDispenserMenuOptions();
	}

	private void addHealerMenuLabel(MenuEntry entry)
	{
		if (!config.showMenuLabel()
				|| config.healerLabelStyle() == BaHealerOrderConfig.HealerLabelStyle.NONE)
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

	private void applyDispenserMenuOptions()
	{
		if (!config.highlightCalledDispenserFood()
				&& !config.removeTakeVial()
				&& !config.moveTakeMeatUp())
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenu().getMenuEntries();

		if (menuEntries.length == 0)
		{
			return;
		}

		List<MenuEntry> nextEntries = new ArrayList<>(menuEntries.length);
		boolean changed = false;

		for (MenuEntry entry : menuEntries)
		{
			// The highlight workaround rewrites option text into the target, so undo it
			// before applying this pass again to avoid stacking duplicate text.
			if (restoreHighlightedDispenserEntry(entry))
			{
				changed = true;
			}

			String option = entry.getOption();
			String target = entry.getTarget();

			if (option == null || target == null)
			{
				nextEntries.add(entry);
				continue;
			}

			String optionText = Text.removeTags(option).toLowerCase(Locale.ROOT);
			String targetText = Text.removeTags(target).toLowerCase(Locale.ROOT);

			if (!isHealerItemMachineTarget(targetText))
			{
				nextEntries.add(entry);
				continue;
			}

			if (config.removeTakeVial() && TAKE_VIAL_OPTION.equals(optionText))
			{
				changed = true;
				continue;
			}

			nextEntries.add(entry);
		}

		MenuEntry[] updatedEntries = nextEntries.toArray(new MenuEntry[0]);

		if (config.moveTakeMeatUp())
		{
			MenuEntry[] movedEntries = moveTakeMeatUp(updatedEntries);

			if (movedEntries != updatedEntries)
			{
				updatedEntries = movedEntries;
				changed = true;
			}
		}

		if (config.highlightCalledDispenserFood())
		{
			String calledFoodOption = getCalledDispenserFoodOption();

			if (calledFoodOption != null)
			{
				changed |= highlightCalledDispenserFood(updatedEntries, calledFoodOption);
			}
		}

		if (changed)
		{
			client.getMenu().setMenuEntries(updatedEntries);
		}
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

		// The menu click only tells us an attempt was made. The inventory delta confirms
		// that a poisoned food item was actually consumed and should count.
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

		if (client.isMenuOpen())
		{
			applyDispenserMenuOptions();
		}
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
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int currentInGameBit = client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING);

		if (inGameBit == currentInGameBit)
		{
			return;
		}

		inGameBit = currentInGameBit;

		if (currentInGameBit == 0)
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

	public boolean isWaveActive()
	{
		return waveStartTimeMs > 0 && currentWave > 0;
	}

	public long getCurrentWaveElapsedMillis()
	{
		if (!isWaveActive())
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

	public String getCurrentWaveCodeName()
	{
		WaveCode waveCode = codeManager.getActiveWaveCode(currentWave);
		return waveCode == null ? null : waveCode.getName();
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
		int targetFoodCount = status.getInstruction().getTargetFoodCount();

		if (config.foodCountType() == BaHealerOrderConfig.FoodCountType.COUNT_DOWN)
		{
			builder.append(Math.max(targetFoodCount - status.getFoodFed(), 0));
		}
		else
		{
			builder.append(status.getFoodFed()).append('/').append(targetFoodCount);
		}

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
			return config.completeCodeColor();
		}

		if (state == CodeDisplayState.PREVIOUS)
		{
			return config.previousCodeColor();
		}

		if (state == CodeDisplayState.IN_PROGRESS)
		{
			return config.inProgressCodeColor();
		}

		return config.notStartedCodeColor();
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

		// BA healer NPC indexes sort in spawn order for the wave. Rebuilding the whole map
		// lets earlier visible healers be corrected when a lower/higher index arrives later.
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
			// The widget can settle during wave startup; require one stable tick before a
			// changed value is treated as a real call advance.
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

	private String getCalledDispenserFoodOption()
	{
		String callText = getHealerListenText();

		if (callText == null)
		{
			return null;
		}

		if (callText.contains("tofu")) return TAKE_TOFU_OPTION;
		if (callText.contains("worm")) return TAKE_WORMS_OPTION;
		if (callText.contains("meat")) return TAKE_MEAT_OPTION;

		return null;
	}

	private boolean highlightCalledDispenserFood(MenuEntry[] entries, String calledFoodOption)
	{
		boolean changed = false;

		for (MenuEntry entry : entries)
		{
			String option = entry.getOption();
			String target = entry.getTarget();

			if (option == null || target == null)
			{
				continue;
			}

			String optionText = Text.removeTags(option).toLowerCase(Locale.ROOT);
			String targetText = Text.removeTags(target).toLowerCase(Locale.ROOT);

			if (calledFoodOption.equals(optionText) && isHealerItemMachineTarget(targetText))
			{
				// Object menu options do not reliably render color tags in the option column.
				// Moving the option text into the target side makes the selected row visibly green.
				entry.setOption("");
				entry.setTarget(ColorUtil.prependColorTag(Text.removeTags(option), CALLED_FOOD_MENU_COLOR) + " " + target);
				changed = true;
			}
		}

		return changed;
	}

	private boolean restoreHighlightedDispenserEntry(MenuEntry entry)
	{
		// See highlightCalledDispenserFood: highlighted dispenser entries have an empty
		// option and a target that starts with the original Take-* option.
		String option = entry.getOption();
		String target = entry.getTarget();

		if (option == null || target == null || !Text.removeTags(option).isEmpty())
		{
			return false;
		}

		String targetText = Text.removeTags(target);
		String optionText = getDispenserOptionPrefix(targetText);

		if (optionText == null)
		{
			return false;
		}

		String restoredTarget = target;
		int optionIndex = restoredTarget.indexOf(optionText);

		if (optionIndex >= 0)
		{
			restoredTarget = restoredTarget.substring(optionIndex + optionText.length()).trim();
		}

		if (!isHealerItemMachineTarget(Text.removeTags(restoredTarget).toLowerCase(Locale.ROOT)))
		{
			return false;
		}

		entry.setOption(optionText);
		entry.setTarget(restoredTarget);
		return true;
	}

	private String getDispenserOptionPrefix(String targetText)
	{
		if (targetText.startsWith("Take-Tofu "))
		{
			return "Take-Tofu";
		}

		if (targetText.startsWith("Take-Worms "))
		{
			return "Take-Worms";
		}

		if (targetText.startsWith("Take-Meat "))
		{
			return "Take-Meat";
		}

		return null;
	}

	private String getHealerListenText()
	{
		WidgetText hornOfGloryListen = getWidgetText(
				BA_HORN_OF_GLORY_GROUP_ID,
				BA_HORN_OF_GLORY_DEFENDER_CHILD_ID,
				"horn defender listen"
		);

		if (hornOfGloryListen != null)
		{
			return normalizeCallText(hornOfGloryListen.text);
		}

		WidgetText healerListen = getWidgetText(BA_HEALER_GROUP_ID, BA_HEALER_LISTEN_CHILD_ID, "healer listen");
		return healerListen == null ? null : normalizeCallText(healerListen.text);
	}

	private boolean isHealerItemMachineTarget(String targetText)
	{
		return targetText.contains(HEALER_ITEM_MACHINE_NAME.toLowerCase(Locale.ROOT));
	}

	private MenuEntry[] moveTakeMeatUp(MenuEntry[] entries)
	{
		int meatIndex = -1;
		int wormsIndex = -1;

		for (int i = 0; i < entries.length; i++)
		{
			MenuEntry entry = entries[i];
			String option = entry.getOption();
			String target = entry.getTarget();

			if (option == null || target == null)
			{
				continue;
			}

			String optionText = Text.removeTags(option).toLowerCase(Locale.ROOT);
			String targetText = Text.removeTags(target).toLowerCase(Locale.ROOT);

			if (!isHealerItemMachineTarget(targetText))
			{
				continue;
			}

			if (TAKE_MEAT_OPTION.equals(optionText))
			{
				meatIndex = i;
			}
			else if (TAKE_WORMS_OPTION.equals(optionText))
			{
				wormsIndex = i;
			}
		}

		if (meatIndex == -1 || wormsIndex == -1 || meatIndex == wormsIndex - 1)
		{
			return entries;
		}

		List<MenuEntry> movedEntries = new ArrayList<>(entries.length);
		Collections.addAll(movedEntries, entries);
		MenuEntry meatEntry = movedEntries.remove(meatIndex);

		if (meatIndex < wormsIndex)
		{
			wormsIndex--;
		}

		// RuneLite stores menu entries in the opposite order from how the right-click
		// menu is drawn, so inserting Meat before Worms in the array displays it below Worms.
		movedEntries.add(wormsIndex, meatEntry);
		return movedEntries.toArray(new MenuEntry[0]);
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
		inGameBit = 0;
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
