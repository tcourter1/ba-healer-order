package com.bahealerorder.healer;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.common.BaHealerSyncMessage;
import com.bahealerorder.common.BaPartySyncService;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.BaRoleDetector;
import com.bahealerorder.healer.codes.CodeDisplayState;
import com.bahealerorder.healer.codes.HealerCodeStatus;
import com.bahealerorder.healer.codes.WaveCode;
import com.bahealerorder.healer.ttk.HealerTtkResult;
import com.bahealerorder.healer.ttk.HealerTtkTracker;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Renderable;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.Hooks;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;
import net.runelite.client.party.events.UserJoin;

@Slf4j
public class HealerController
{
	private static final String PENANCE_HEALER_NAME = "Penance Healer";
	private static final String PENANCE_NPC_PREFIX = "Penance ";
	private static final String HEALER_ITEM_MACHINE_NAME = "Healer item machine";
	private static final String TAKE_TOFU_OPTION = "take-tofu";
	private static final String TAKE_WORMS_OPTION = "take-worms";
	private static final String TAKE_MEAT_OPTION = "take-meat";
	private static final String TAKE_VIAL_OPTION = "take-vial";
	private static final String WRONG_FOOD_MESSAGE = "that's the wrong type of poisoned food to use! penalty!";
	private static final String PANEL_ICON_RESOURCE = "/com/bahealerorder/penance_healer.png";
	private static final int MAX_FOOD_PANEL_CODE_CALLS = 3;
	private static final int NPC_INDEX_MODULUS = 1 << 16;
	private static final int PENDING_FEED_ATTEMPT_MAX_AGE_TICKS = 5;
	private static final int NO_ATTEMPT_DISTANCE = Integer.MAX_VALUE;

	private static final int BA_HORN_OF_GLORY_GROUP_ID = 484;
	private static final int BA_HEALER_GROUP_ID = 488;
	private static final int BA_HEALER_LISTEN_CHILD_ID = 7;
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
	private Hooks hooks;

	@Inject
	private NpcUtil npcUtil;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private HealerOverlay overlay;

	@Inject
	private HealerFoodOverlay foodOverlay;

	@Inject
	private HealerCodePanel panel;

	@Inject
	private HealerCodeManager codeManager;

	@Inject
	private HealerTtkTracker ttkTracker;

	@Inject
	private BaUtilitiesConfig config;

	@Inject
	private BaRoleDetector roleDetector;

	@Inject
	private BaPartySyncService partySyncService;

	@Getter
	private final Map<Integer, Integer> healerOrderByNpcIndex = new HashMap<>();

	@Getter
	private final Map<NPC, Integer> visibleHealers = new HashMap<>();

	private final Set<Integer> healerIndexesSeenThisWave = new HashSet<>();
	private final Set<Integer> deadHealerOrders = new HashSet<>();

	private final Map<Integer, Integer> lastPoisonedFoodCountByItemId = new HashMap<>();
	private final Map<Integer, String> lastSentHealerSyncByOrder = new HashMap<>();
	private final HealerSharedState sharedState = new HealerSharedState();

	private final Hooks.RenderableDrawListener drawListener = this::shouldDrawRenderable;

	private int currentWave = -1;
	private int currentCallIndex = 0;
	private int inGameBit;
	private int healerIndexBase = -1;
	private long waveStartTimeMs = -1;
	private String lastCallText;
	private String currentCallText;
	private String currentCallSource;
	private boolean callTrackingArmed;
	private Integer selectedPoisonedFoodItemId;
	private final List<PendingFeedAttempt> pendingFeedAttempts = new ArrayList<>();
	private int feedAttemptSequence;
	private NavigationButton navigationButton;
	public void startUp()
	{
		codeManager.load();
		panel.refreshAll();
		SwingUtilities.updateComponentTreeUI(panel.getWrappedPanel());
		resetAllState();
		overlay.setController(this);
		foodOverlay.setController(this);
		hooks.registerRenderableDrawListener(drawListener);
		mouseManager.registerMouseListener(foodOverlay);
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
	public void shutDown()
	{
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		overlayManager.remove(foodOverlay);
		overlayManager.remove(overlay);
		mouseManager.unregisterMouseListener(foodOverlay);
		hooks.unregisterRenderableDrawListener(drawListener);
		resetAllState();
	}
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();

		if (!isPenanceHealer(npc)) return;

		int npcIndex = npc.getIndex();

		if (healerIndexBase < 0)
		{
			healerIndexBase = npcIndex;
		}

		boolean addedNewIndex = healerIndexesSeenThisWave.add(npcIndex);

		rebuildHealerOrderByNpcIndex();
		rebuildVisibleHealerOrders();

		Integer order = healerOrderByNpcIndex.get(npcIndex);

		if (order == null) return;

		visibleHealers.put(npc, order);
		ttkTracker.onHealerSpawned(npcIndex, order, client.getTickCount());
		sharedState.recordLocalSpawn(order, npcIndex);
		sendHealerSyncForOrder(order);

		if (addedNewIndex)
		{
			log.debug("Registered Penance Healer index {} as corrected healer #{}", npcIndex, order);
		}
		else
		{
			log.debug("Re-associated Penance Healer index {} with corrected healer #{}", npcIndex, order);
		}
	}
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		Integer healerOrder = visibleHealers.get(npc);

		if (healerOrder != null && isDeadPenanceHealer(npc))
		{
			recordDeadHealer(healerOrder);
		}

		if (visibleHealers.remove(npc) != null)
		{
			log.debug("Removed visible Penance Healer index {}", npc.getIndex());
		}
	}
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ROOT);
		String target = Text.removeTags(event.getMenuTarget()).toLowerCase(Locale.ROOT);

		handlePoisonedFoodSelection(event, option, target);
		handlePoisonedFoodUseOnHealer(event, option, target);
	}
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!shouldShowMenuLabel() && !shouldShowMenuCode()) return;

		MenuEntry entry = event.getMenuEntry();

		if (entry == null) return;

		Integer healerOrder = getHealerOrderForMenuEntry(entry);

		if (healerOrder == null) return;

		String target = entry.getTarget();

		if (target == null || hasHealerMenuSuffix(target)) return;

		String suffix = getHealerMenuSuffix(healerOrder);

		if (suffix == null) return;

		entry.setTarget(target + " " + suffix);
	}
	public void onPostMenuSort(PostMenuSort event)
	{
		filterPoisonedFoodUseEntries();
		applyDispenserMenuOptions();
	}
	public void onMenuOpened(MenuOpened event)
	{
		applyDispenserMenuOptions(true);
	}
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId()) return;

		if (!isHealerRole())
		{
			cachePoisonedFoodCounts(event.getItemContainer());
			return;
		}

		Map<Integer, Integer> consumedPoisonedFoodByItemId = getConsumedPoisonedFoodByItemId(event.getItemContainer());

		if (consumedPoisonedFoodByItemId.isEmpty())
		{
			expireStalePendingFeedAttempts();
			return;
		}

		for (Map.Entry<Integer, Integer> consumedEntry : consumedPoisonedFoodByItemId.entrySet())
		{
			int itemId = consumedEntry.getKey();
			int consumedCount = consumedEntry.getValue();

			for (int i = 0; i < consumedCount; i++)
			{
				PendingFeedAttempt attempt = findBestPendingFeedAttempt(itemId);

				if (attempt == null)
				{
					log.debug("Poisoned food item {} was consumed with no matching pending healer feed attempt", itemId);
					continue;
				}

				recordConsumedFoodForPendingAttempt(attempt);
				pendingFeedAttempts.remove(attempt);
			}
		}
	}
	public void onGameTick(GameTick event)
	{
		if (isHealerRole())
		{
			updateCallIndexFromHealerWidget();
			updateDeadHealerOrders();
			ttkTracker.observeVisibleHealers(visibleHealers.keySet(), client.getTickCount());
		}

		if (client.isMenuOpen())
		{
			applyDispenserMenuOptions(true);
		}

		sendVisibleHealerSync();
	}

	public void onBaHealerSyncMessage(BaHealerSyncMessage event)
	{
		if (event == null
				|| partySyncService.isLocalPartyMember(event.getMemberId())
				|| event.getWorld() != client.getWorld()
				|| currentWave > 0 && event.getWave() != currentWave)
		{
			return;
		}

		if (currentWave <= 0)
		{
			currentWave = event.getWave();
		}

		sharedState.updateFromParty(event);
	}

	public void onPartyUserJoin(UserJoin event)
	{
		if (event == null || partySyncService.isLocalPartyMember(event.getMemberId()))
		{
			return;
		}

		broadcastHealerSyncState();
	}

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

		if (handleWaveStartMessage(message)) return;

		if (message.contains(WRONG_FOOD_MESSAGE))
		{
			log.debug("Wrong poisoned food detected. Cancelling pending feed attempt.");
			pendingFeedAttempts.clear();
		}
	}
	public void onVarbitChanged(VarbitChanged event)
	{
		int currentInGameBit = client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING);

		if (inGameBit == currentInGameBit) return;

		inGameBit = currentInGameBit;

		if (currentInGameBit == 0)
		{
			resetWaveState();
		}
	}
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

	public boolean isHealerRole()
	{
		return roleDetector.isRole(BaRole.HEALER);
	}

	public boolean shouldShowFoodPanel()
	{
		return config.foodPanelStyle() != BaUtilitiesConfig.FoodPanelStyle.NONE
				&& (!config.showFoodPanelAsHealerOnly() || isHealerRole());
	}

	public BaUtilitiesConfig.FoodPanelStyle getFoodPanelStyle()
	{
		return config.foodPanelStyle();
	}

	public boolean shouldShowLabels()
	{
		return !config.showLabelsAsHealerOnly() || isHealerRole() || partySyncService.isBaPartySyncConnected();
	}

	public boolean shouldShowHealerHighlights()
	{
		return isHealerRole();
	}

	public boolean shouldShowFoodCountOnNpc()
	{
		return config.showFoodCountOnNpc() && (isHealerRole() || partySyncService.isBaPartySyncConnected());
	}

	public boolean shouldShowHealerTtk()
	{
		return config.healerTtkDisplay() != BaUtilitiesConfig.HealerTtkDisplayMode.OFF
				&& (isHealerRole() || partySyncService.isBaPartySyncConnected());
	}

	public Color getHealerTtkColor()
	{
		return Color.ORANGE;
	}

	public String getHealerTtkText(NPC npc)
	{
		if (!shouldShowHealerTtk() || npc == null)
		{
			return null;
		}

		Optional<HealerTtkResult> result = ttkTracker.getTtk(npc.getIndex(), client.getTickCount());
		Integer healerOrder = visibleHealers.get(npc);

		if (!result.isPresent())
		{
			if (ttkTracker.hasPoisonedHealerWithUnknownTtk(npc.getIndex()))
			{
				if (healerOrder != null)
				{
					sharedState.recordLocalPrediction(healerOrder, -1, true, getCurrentWaveTick());
				}

				return "?";
			}

			return null;
		}

		int deathTick = result.get().getDeathTick();

		if (healerOrder != null)
		{
			sharedState.recordLocalPrediction(healerOrder, toWaveTick(deathTick), false, getCurrentWaveTick());
		}

		int ticksRemaining = Math.max(deathTick - client.getTickCount() + 1, 0);

		if (config.healerTtkDisplay() == BaUtilitiesConfig.HealerTtkDisplayMode.TICKS)
		{
			return ticksRemaining + "t";
		}

		if (config.healerTtkDisplay() == BaUtilitiesConfig.HealerTtkDisplayMode.SECONDS)
		{
			return formatTickCountdownAsSeconds(ticksRemaining);
		}

		return formatTickAsWaveTime(deathTick);
	}

	public String getHealerPanelTtkText(int healerOrder)
	{
		String ttkText = getHealerPanelDeathTime(healerOrder);

		if (ttkText == null)
		{
			return null;
		}

		return "dead at " + ttkText;
	}

	public String getHealerPanelDeathTime(int healerOrder)
	{
		if (healerOrder <= 0)
		{
			return null;
		}

		if (isHealerDead(healerOrder))
		{
			Integer deathTick = sharedState.getActualDeathTick(healerOrder);
			return deathTick == null ? null : formatWaveTickAsTime(deathTick);
		}

		NPC npc = getVisibleHealerByOrder(healerOrder);

		if (npc == null)
		{
			Integer deathTick = sharedState.getActualDeathTick(healerOrder);
			if (deathTick != null) return formatWaveTickAsTime(deathTick);

			Integer predictedDeathTick = sharedState.getPredictedDeathTick(healerOrder);
			if (predictedDeathTick != null) return formatWaveTickAsTime(predictedDeathTick);

			if (sharedState.hasUnknownTtk(healerOrder)) return "?";

			return null;
		}

		Optional<HealerTtkResult> result = ttkTracker.getTtk(npc.getIndex(), client.getTickCount());

		if (!result.isPresent())
		{
			boolean unknownTtk = ttkTracker.hasPoisonedHealerWithUnknownTtk(npc.getIndex());
			if (unknownTtk)
			{
				sharedState.recordLocalPrediction(healerOrder, -1, true, getCurrentWaveTick());
			}

			return unknownTtk ? "?" : null;
		}

		int deathTick = result.get().getDeathTick();
		sharedState.recordLocalPrediction(healerOrder, toWaveTick(deathTick), false, getCurrentWaveTick());
		return formatTickAsWaveTime(deathTick);
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

	public boolean hasActiveWaveCode()
	{
		WaveCode waveCode = codeManager.getActiveWaveCode(currentWave);
		return waveCode != null && !waveCode.getCalls().isEmpty();
	}

	public int getExpectedFoodForOrder(int healerOrder)
	{
		if (healerOrder <= 0) return 0;

		return codeManager.getExpectedFoodForOrder(currentWave, healerOrder, getEffectiveCurrentCallIndex());
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

	public List<Integer> getHealerOrdersForCurrentWave()
	{
		if (currentWave <= 0 || currentWave >= TIME_BASED_HEALER_LABELS.length)
		{
			return Collections.emptyList();
		}

		List<Integer> healerOrders = new ArrayList<>();

		for (int healerOrder = 1; healerOrder <= TIME_BASED_HEALER_LABELS[currentWave].length; healerOrder++)
		{
			healerOrders.add(healerOrder);
		}

		return Collections.unmodifiableList(healerOrders);
	}

	public List<Integer> getFoodPanelCallIndexes()
	{
		WaveCode waveCode = codeManager.getActiveWaveCode(currentWave);

		if (waveCode == null || waveCode.getCalls().isEmpty())
		{
			return Collections.emptyList();
		}

		List<Integer> callIndexes = new ArrayList<>();
		int lastVisibleCallIndex = Math.min(getEffectiveCurrentCallIndex(), MAX_FOOD_PANEL_CODE_CALLS - 1);

		for (int callIndex = 0; callIndex <= lastVisibleCallIndex; callIndex++)
		{
			callIndexes.add(callIndex);
		}

		return Collections.unmodifiableList(callIndexes);
	}

	public String getFoodPanelHealerLabel(int healerOrder)
	{
		if (config.healerLabelStyle() == BaUtilitiesConfig.HealerLabelStyle.TIME_BASED_NUMBERING)
		{
			String label = getHealerLabel(healerOrder);
			return label == null ? String.valueOf(healerOrder) : formatTimeBasedHealerLabel(label);
		}

		return "#" + healerOrder;
	}

	private String formatTimeBasedHealerLabel(String label)
	{
		if (label.matches("\\d+"))
		{
			return label + "s";
		}

		return label;
	}

	public String getFoodPanelText(int healerOrder, int callIndex)
	{
		if (isHealerDead(healerOrder))
		{
			return "-";
		}

		if (callIndex < 0)
		{
			int foodFed = getFoodFedByHealerOrder().getOrDefault(healerOrder, 0);
			return getFoodCountText(healerOrder, foodFed);
		}

		HealerCodeStatus status = codeManager.getPanelStatusForCall(currentWave, healerOrder, getEffectiveCurrentCallIndex(), callIndex, sharedState.getFeedEvents());
		String codeText = formatCodeStatus(status);

		if (codeText != null)
		{
			return codeText;
		}

		return formatRawFoodCount(codeManager.getPanelFoodCountForCall(currentWave, healerOrder, getEffectiveCurrentCallIndex(), callIndex, sharedState.getFeedEvents()));
	}

	private String formatRawFoodCount(int foodFed)
	{
		return String.valueOf(Math.max(foodFed, 0));
	}

	public Color getFoodPanelTextColor(int healerOrder, int callIndex)
	{
		if (callIndex < 0)
		{
			HealerCodeStatus status = getDisplayCodeStatus(healerOrder);
			return status == null ? null : getFoodPanelCodeStatusColor(status.getState());
		}

		HealerCodeStatus status = codeManager.getPanelStatusForCall(currentWave, healerOrder, getEffectiveCurrentCallIndex(), callIndex, sharedState.getFeedEvents());

		return status == null ? null : getFoodPanelCodeStatusColor(status.getState());
	}

	private Color getFoodPanelCodeStatusColor(CodeDisplayState state)
	{
		return state == CodeDisplayState.PREVIOUS ? config.completeCodeColor() : getCodeStatusColor(state);
	}

	public boolean hasHealerSpawned(int healerOrder)
	{
		return sharedState.hasSpawned(healerOrder);
	}

	public boolean isHealerDead(int healerOrder)
	{
		if (deadHealerOrders.contains(healerOrder))
		{
			return true;
		}

		if (sharedState.isDead(healerOrder))
		{
			return true;
		}

		NPC npc = getVisibleHealerByOrder(healerOrder);
		return npc != null && isDeadPenanceHealer(npc);
	}

	public boolean isHealerPresumedDead(int healerOrder)
	{
		if (isHealerDead(healerOrder) || getVisibleHealerByOrder(healerOrder) != null)
		{
			return false;
		}

		Integer deathTick = sharedState.getPredictedDeathTick(healerOrder);
		return deathTick != null && getCurrentWaveTick() > deathTick;
	}

	public HealerCodeStatus getCurrentCodeStatus(int healerOrder)
	{
		return codeManager.getCurrentStatus(currentWave, healerOrder, getEffectiveCurrentCallIndex(), sharedState.getFeedEvents());
	}

	public HealerCodeStatus getPreviousCodeStatus(int healerOrder)
	{
		return codeManager.getPreviousStatus(currentWave, healerOrder, getEffectiveCurrentCallIndex(), sharedState.getFeedEvents());
	}

	public HealerCodeStatus getDisplayCodeStatus(int healerOrder)
	{
		return codeManager.getDisplayStatus(currentWave, healerOrder, getEffectiveCurrentCallIndex(), sharedState.getFeedEvents());
	}

	public String formatCodeStatus(HealerCodeStatus status)
	{
		if (status == null || status.getInstruction() == null || !status.getInstruction().hasTarget())
		{
			return null;
		}

		StringBuilder builder = new StringBuilder();
		int targetFoodCount = status.getInstruction().getTargetFoodCount();

		if (config.foodCountType() == BaUtilitiesConfig.FoodCountType.COUNT_DOWN)
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

	public String getFoodCountText(int healerOrder, int foodFed)
	{
		HealerCodeStatus status = getDisplayCodeStatus(healerOrder);
		String codeText = formatCodeStatus(status);

		if (codeText != null)
		{
			return codeText;
		}

		int expected = getExpectedFoodForOrder(healerOrder);

		if (expected <= 0)
		{
			return formatRawFoodCount(foodFed);
		}

		if (config.foodCountType() == BaUtilitiesConfig.FoodCountType.COUNT_UP)
		{
			return foodFed + "/" + expected;
		}

		return String.valueOf(Math.max(expected - foodFed, 0));
	}

	public Color getFoodCountColor(int healerOrder, int foodFed)
	{
		HealerCodeStatus status = getDisplayCodeStatus(healerOrder);

		if (status != null)
		{
			return getCodeStatusColor(status.getState());
		}

		int expected = getExpectedFoodForOrder(healerOrder);

		if (expected > 0)
		{
			return getCodeStatusColor(getFallbackCodeState(foodFed, expected));
		}

		return config.foodCountColor();
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

	private CodeDisplayState getFallbackCodeState(int foodFed, int expected)
	{
		if (foodFed <= 0)
		{
			return CodeDisplayState.NOT_STARTED;
		}

		if (foodFed < expected)
		{
			return CodeDisplayState.IN_PROGRESS;
		}

		return CodeDisplayState.COMPLETE;
	}

	public Map<Integer, Integer> getFoodFedByHealerOrder()
	{
		return Collections.unmodifiableMap(sharedState.getFoodFedByHealerOrder());
	}

	private int getEffectiveCurrentCallIndex()
	{
		return Math.max(currentCallIndex, sharedState.getCurrentCallIndex());
	}

	public String getHealerLabel(int healerOrder)
	{
		if (config.healerLabelStyle() == BaUtilitiesConfig.HealerLabelStyle.SPAWN_ORDER)
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

	private String getHealerMenuSuffix(int healerOrder)
	{
		List<String> parts = new ArrayList<>();

		if (shouldShowMenuLabel()
				&& config.healerLabelStyle() != BaUtilitiesConfig.HealerLabelStyle.NONE)
		{
			parts.add(ColorUtil.wrapWithColorTag("(" + getHealerLabel(healerOrder) + ")", config.textColor()));
		}

		if (shouldShowMenuCode())
		{
			int foodFed = getFoodFedByHealerOrder().getOrDefault(healerOrder, 0);
			String codeText = getFoodCountText(healerOrder, foodFed);

			if (codeText != null)
			{
				parts.add(ColorUtil.wrapWithColorTag("(" + codeText + ")", getFoodCountColor(healerOrder, foodFed)));
			}
		}

		if (parts.isEmpty())
		{
			return null;
		}

		return String.join(" ", parts);
	}

	private boolean hasHealerMenuSuffix(String target)
	{
		return Text.removeTags(target).contains(" (");
	}

	private void startNewWave(int waveNumber)
	{
		if (waveNumber <= 0) return;

		this.currentWave = waveNumber;
		this.waveStartTimeMs = System.currentTimeMillis();
		resetWaveTrackedState();
		sharedState.startWave(waveNumber);
		ttkTracker.startWave(client.getTickCount());

		log.debug("Starting new BA wave {}", waveNumber);
	}

	private String formatTickCountdownAsSeconds(int ticks)
	{
		return (int) Math.ceil(ticks * 0.6d) + "s";
	}

	private String formatTickAsWaveTime(int tick)
	{
		int waveStartTick = ttkTracker.getWaveStartTick();

		if (waveStartTick < 0)
		{
			return null;
		}

		double elapsedSeconds = Math.max(0, tick - waveStartTick) * 0.6d;
		return String.format(Locale.ROOT, "%.1f", elapsedSeconds);
	}

	private String formatWaveTickAsTime(int waveTick)
	{
		return String.format(Locale.ROOT, "%.1f", Math.max(0, waveTick) * 0.6d);
	}

	private int getCurrentWaveTick()
	{
		int waveStartTick = ttkTracker.getWaveStartTick();
		return waveStartTick < 0 ? -1 : Math.max(0, client.getTickCount() - waveStartTick);
	}

	private int toWaveTick(int tick)
	{
		int waveStartTick = ttkTracker.getWaveStartTick();
		return tick < 0 || waveStartTick < 0 ? -1 : Math.max(0, tick - waveStartTick);
	}

	private void sendVisibleHealerSync()
	{
		if (!partySyncService.isBaPartySyncConnected()) return;

		for (Integer healerOrder : new HashSet<>(visibleHealers.values()))
		{
			if (healerOrder != null)
			{
				sendHealerSyncForOrder(healerOrder);
			}
		}
	}

	private void broadcastHealerSyncState()
	{
		if (!partySyncService.isBaPartySyncConnected()) return;

		for (int healerOrder : getHealerOrdersForCurrentWave())
		{
			if (hasHealerSpawned(healerOrder) || isHealerDead(healerOrder) || getFoodFedByHealerOrder().containsKey(healerOrder))
			{
				sendHealerSyncForOrder(healerOrder, true);
			}
		}
	}

	private void sendHealerSyncForOrder(int healerOrder)
	{
		sendHealerSyncForOrder(healerOrder, false);
	}

	private void sendHealerSyncForOrder(int healerOrder, boolean force)
	{
		if (!partySyncService.isBaPartySyncConnected() || currentWave <= 0 || healerOrder <= 0) return;

		BaHealerSyncMessage message = buildHealerSyncMessage(healerOrder);
		if (message == null) return;

		String signature = buildHealerSyncSignature(message);

		if (!force && signature.equals(lastSentHealerSyncByOrder.get(healerOrder))) return;

		lastSentHealerSyncByOrder.put(healerOrder, signature);
		partySyncService.sendHealerSync(message);
	}

	private BaHealerSyncMessage buildHealerSyncMessage(int healerOrder)
	{
		updateLocalTtkState(healerOrder);

		HealerSharedState.LocalSnapshot snapshot = sharedState.getLocalSnapshot(healerOrder);

		if (snapshot == null || snapshot.getNpcIndex() < 0) return null;

		return new BaHealerSyncMessage(
				client.getWorld(),
				currentWave,
				snapshot.getNpcIndex(),
				healerOrder,
				currentCallIndex,
				snapshot.getPredictedDeathTick(),
				snapshot.isUnknownTtk(),
				snapshot.getActualDeathTick(),
				getCurrentWaveTick()
		);
	}

	private String buildHealerSyncSignature(BaHealerSyncMessage message)
	{
		return message.getWave()
				+ ":" + message.getNpcIndex()
				+ ":" + message.getHealerOrder()
				+ ":" + message.getCurrentCallIndex()
				+ ":" + message.getPredictedDeathTick()
				+ ":" + message.isUnknownTtk()
				+ ":" + message.getActualDeathTick();
	}

	private void updateLocalTtkState(int healerOrder)
	{
		NPC npc = getVisibleHealerByOrder(healerOrder);

		if (npc == null) return;

		Optional<HealerTtkResult> result = ttkTracker.getTtk(npc.getIndex(), client.getTickCount());

		if (result.isPresent())
		{
			sharedState.recordLocalPrediction(healerOrder, toWaveTick(result.get().getDeathTick()), false, getCurrentWaveTick());
			return;
		}

		if (ttkTracker.hasPoisonedHealerWithUnknownTtk(npc.getIndex()))
		{
			sharedState.recordLocalPrediction(healerOrder, -1, true, getCurrentWaveTick());
		}
	}

	private boolean shouldShowMenuLabel()
	{
		return config.showMenuLabel() && shouldShowLabels();
	}

	private boolean shouldShowMenuCode()
	{
		return config.showMenuCode() && isHealerRole();
	}

	private boolean shouldDrawRenderable(Renderable renderable, boolean drawingUi)
	{
		if (!(renderable instanceof NPC))
		{
			return true;
		}

		return !shouldHideDeadNpc((NPC) renderable);
	}

	public boolean shouldHideDeadNpc(NPC npc)
	{
		BaUtilitiesConfig.HideDeadNpcMode mode = config.hideDeadNpcs();

		if (mode == BaUtilitiesConfig.HideDeadNpcMode.NONE
				|| !isWaveActive())
		{
			return false;
		}

		if (mode == BaUtilitiesConfig.HideDeadNpcMode.HEALERS_ONLY)
		{
			return isDeadPenanceHealer(npc);
		}

		if (mode == BaUtilitiesConfig.HideDeadNpcMode.ALL_BA_NPCS)
		{
			return isDeadPenanceHealer(npc) || isBaNpc(npc) && npcUtil.isDying(npc);
		}

		return false;
	}

	private boolean isDeadPenanceHealer(NPC npc)
	{
		return isPenanceHealer(npc) && npc.getHealthRatio() == 0;
	}

	private void updateDeadHealerOrders()
	{
		for (Map.Entry<NPC, Integer> entry : visibleHealers.entrySet())
		{
			if (entry.getKey() != null && entry.getValue() != null && isDeadPenanceHealer(entry.getKey()))
			{
				recordDeadHealer(entry.getValue());
			}
		}
	}

	private void recordDeadHealer(int healerOrder)
	{
		if (deadHealerOrders.add(healerOrder))
		{
			sharedState.recordLocalDeath(healerOrder, getCurrentWaveTick());
			sendHealerSyncForOrder(healerOrder);
		}
	}

	private NPC getVisibleHealerByOrder(int healerOrder)
	{
		for (Map.Entry<NPC, Integer> entry : visibleHealers.entrySet())
		{
			if (entry.getValue() != null && entry.getValue() == healerOrder)
			{
				return entry.getKey();
			}
		}

		return null;
	}

	private boolean isBaNpc(NPC npc)
	{
		if (npc == null || npc.getName() == null)
		{
			return false;
		}

		String name = Text.removeTags(npc.getName());
		return name.startsWith(PENANCE_NPC_PREFIX);
	}

	private void rebuildHealerOrderByNpcIndex()
	{
		List<Integer> sortedIndexes = new ArrayList<>(healerIndexesSeenThisWave);
		sortedIndexes.sort(Comparator.comparingInt(this::normalizeNpcIndexForWave));

		healerOrderByNpcIndex.clear();

		for (int i = 0; i < sortedIndexes.size(); i++)
		{
			healerOrderByNpcIndex.put(sortedIndexes.get(i), i + 1);
		}
	}

	private int normalizeNpcIndexForWave(int npcIndex)
	{
		if (healerIndexBase < 0)
		{
			return npcIndex;
		}

		return Math.floorMod(npcIndex - healerIndexBase, NPC_INDEX_MODULUS);
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
		if (currentWave <= 0) return;

		String callText = getHealerCallText();

		if (callText == null || callText.isEmpty()) return;

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
			sharedState.recordLocalCallIndex(currentCallIndex);
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

	private void applyDispenserMenuOptions()
	{
		applyDispenserMenuOptions(false);
	}

	private void applyDispenserMenuOptions(boolean forceHighlightTopEntry)
	{
		if (!config.highlightCalledDispenserFood()
				&& !config.removeTakeVial()
				&& !config.moveTakeMeatUp()) return;

		if (!isHealerRole()) return;

		MenuEntry[] menuEntries = client.getMenu().getMenuEntries();

		if (menuEntries.length == 0) return;

		List<MenuEntry> nextEntries = new ArrayList<>(menuEntries.length);
		boolean changed = false;

		for (MenuEntry entry : menuEntries)
		{
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
				changed |= highlightCalledDispenserFood(updatedEntries, calledFoodOption, forceHighlightTopEntry);
			}
		}

		if (changed)
		{
			client.getMenu().setMenuEntries(updatedEntries);
		}
	}

	private String getCalledDispenserFoodOption()
	{
		String callText = getHealerListenText();

		if (callText == null)
		{
			return null;
		}

		if (callText.contains("tofu"))
		{
			return TAKE_TOFU_OPTION;
		}

		if (callText.contains("worm"))
		{
			return TAKE_WORMS_OPTION;
		}

		if (callText.contains("meat"))
		{
			return TAKE_MEAT_OPTION;
		}

		return null;
	}

	private boolean highlightCalledDispenserFood(MenuEntry[] entries, String calledFoodOption, boolean forceHighlightTopEntry)
	{
		boolean changed = false;

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

			if (calledFoodOption.equals(optionText) && isHealerItemMachineTarget(targetText))
			{
				if (!forceHighlightTopEntry && i == entries.length - 1)
				{
					continue;
				}

				entry.setOption("");
				entry.setTarget(ColorUtil.prependColorTag(Text.removeTags(option), CALLED_FOOD_MENU_COLOR) + " " + target);
				changed = true;
			}
		}

		return changed;
	}

	private boolean restoreHighlightedDispenserEntry(MenuEntry entry)
	{
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
		if (!isHealerRole()) return null;

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

		movedEntries.add(wormsIndex, meatEntry);
		return movedEntries.toArray(new MenuEntry[0]);
	}

	private void handlePoisonedFoodSelection(MenuOptionClicked event, String option, String target)
	{
		if (!isHealerRole()) return;

		if (event.getMenuAction() != MenuAction.WIDGET_TARGET) return;

		if (!"use".equals(option)) return;

		if (!target.contains("poisoned")) return;

		selectedPoisonedFoodItemId = event.getItemId();
	}

	private void filterPoisonedFoodUseEntries()
	{
		if (!config.healerFoodOnly()
				|| client.getGameState() != GameState.LOGGED_IN
				|| client.isMenuOpen()
				|| !isHealerRole()
				|| !isPoisonedFoodSelected())
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenu().getMenuEntries();
		List<MenuEntry> filteredEntries = new ArrayList<>(menuEntries.length);
		boolean changed = false;

		for (MenuEntry entry : menuEntries)
		{
			if (!isSelectedPoisonedFoodUseEntry(entry))
			{
				filteredEntries.add(entry);
				continue;
			}

			if (isPenanceHealerUseEntry(entry))
			{
				filteredEntries.add(entry);
				continue;
			}

			changed = true;
		}

		if (!changed) return;

		client.getMenu().setMenuEntries(filteredEntries.toArray(new MenuEntry[0]));
	}

	private void handlePoisonedFoodUseOnHealer(MenuOptionClicked event, String option, String target)
	{
		if (!isHealerRole()) return;

		if (event.getMenuAction() != MenuAction.WIDGET_TARGET_ON_NPC) return;

		if (!"use".equals(option)) return;

		if (!target.contains("poisoned") || !target.contains("penance healer")) return;

		if (selectedPoisonedFoodItemId == null || selectedPoisonedFoodItemId <= 0) return;

		int npcIndex = event.getId();
		Integer healerOrder = healerOrderByNpcIndex.get(npcIndex);

		if (healerOrder == null) return;

		NPC npc = getVisibleHealerByOrder(healerOrder);
		PendingFeedAttempt attempt = new PendingFeedAttempt(
				++feedAttemptSequence,
				client.getTickCount(),
				npcIndex,
				healerOrder,
				selectedPoisonedFoodItemId,
				npc
		);

		pendingFeedAttempts.add(attempt);
		expireStalePendingFeedAttempts();

		log.debug(
				"Queued pending food feed sequence {} for healer #{} from NPC index {} using item id {}",
				attempt.sequence,
				healerOrder,
				npcIndex,
				selectedPoisonedFoodItemId
		);
	}

	private boolean isPoisonedFoodSelected()
	{
		if (!client.isWidgetSelected())
		{
			selectedPoisonedFoodItemId = null;
			return false;
		}

		Widget selectedWidget = client.getSelectedWidget();

		if (selectedWidget == null)
		{
			return selectedPoisonedFoodItemId != null && selectedPoisonedFoodItemId > 0;
		}

		int itemId = selectedWidget.getItemId();

		if (itemId > 0)
		{
			if (isPoisonedFoodItem(itemId))
			{
				selectedPoisonedFoodItemId = itemId;
				return true;
			}

			selectedPoisonedFoodItemId = null;
			return false;
		}

		String widgetName = normalizeMenuText(selectedWidget.getName());

		if (widgetName.contains("poisoned"))
		{
			return true;
		}

		return selectedPoisonedFoodItemId != null && selectedPoisonedFoodItemId > 0;
	}

	private boolean isPoisonedFoodItem(int itemId)
	{
		ItemComposition itemComposition = client.getItemDefinition(itemId);

		if (itemComposition == null)
		{
			return false;
		}

		String itemName = normalizeMenuText(itemComposition.getName());
		return itemName.contains("poisoned") && (itemName.contains("tofu") || itemName.contains("worms") || itemName.contains("meat"));
	}

	private boolean isSelectedPoisonedFoodUseEntry(MenuEntry entry)
	{
		if (entry == null || !isPoisonedFoodUseMenuAction(entry.getType()))
		{
			return false;
		}

		return "use".equals(normalizeMenuText(entry.getOption()));
	}

	private boolean isPenanceHealerUseEntry(MenuEntry entry)
	{
		if (entry.getType() != MenuAction.WIDGET_TARGET_ON_NPC
				&& entry.getType() != MenuAction.ITEM_USE_ON_NPC)
		{
			return false;
		}

		NPC npc = entry.getNpc();

		if (isPenanceHealer(npc))
		{
			return true;
		}

		return normalizeMenuText(entry.getTarget()).contains("penance healer");
	}

	private boolean isPoisonedFoodUseMenuAction(MenuAction action)
	{
		return action == MenuAction.WIDGET_TARGET_ON_NPC
				|| action == MenuAction.ITEM_USE_ON_NPC
				|| action == MenuAction.WIDGET_TARGET_ON_PLAYER
				|| action == MenuAction.ITEM_USE_ON_PLAYER
				|| action == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT
				|| action == MenuAction.ITEM_USE_ON_GAME_OBJECT
				|| action == MenuAction.WIDGET_TARGET_ON_GROUND_ITEM
				|| action == MenuAction.ITEM_USE_ON_GROUND_ITEM
				|| action == MenuAction.WIDGET_TARGET_ON_WIDGET
				|| action == MenuAction.WIDGET_USE_ON_ITEM
				|| action == MenuAction.ITEM_USE_ON_ITEM;
	}

	private String normalizeMenuText(String text)
	{
		if (text == null)
		{
			return "";
		}

		return Text.removeTags(text).toLowerCase(Locale.ROOT);
	}

	private Map<Integer, Integer> getConsumedPoisonedFoodByItemId(ItemContainer itemContainer)
	{
		Map<Integer, Integer> consumedByItemId = new HashMap<>();

		if (itemContainer == null)
		{
			return consumedByItemId;
		}

		Set<Integer> observedPoisonedFoodItemIds = new HashSet<>();
		Item[] items = itemContainer.getItems();

		if (items != null)
		{
			for (Item item : items)
			{
				if (item == null || item.getId() <= 0 || !isPoisonedFoodItem(item.getId())) continue;

				observedPoisonedFoodItemIds.add(item.getId());
			}
		}

		if (selectedPoisonedFoodItemId != null && selectedPoisonedFoodItemId > 0)
		{
			observedPoisonedFoodItemIds.add(selectedPoisonedFoodItemId);
		}

		for (PendingFeedAttempt attempt : pendingFeedAttempts)
		{
			observedPoisonedFoodItemIds.add(attempt.foodItemId);
		}

		for (Integer itemId : observedPoisonedFoodItemIds)
		{
			int currentCount = getItemCount(items, itemId);
			Integer previousCount = lastPoisonedFoodCountByItemId.put(itemId, currentCount);

			if (previousCount == null) continue;

			int consumedCount = previousCount - currentCount;

			if (consumedCount > 0)
			{
				consumedByItemId.put(itemId, consumedCount);
			}
		}

		return consumedByItemId;
	}

	private void cachePoisonedFoodCounts(ItemContainer itemContainer)
	{
		if (itemContainer == null) return;

		Item[] items = itemContainer.getItems();

		if (items == null) return;

		for (Item item : items)
		{
			if (item != null && item.getId() > 0 && isPoisonedFoodItem(item.getId()))
			{
				lastPoisonedFoodCountByItemId.put(item.getId(), getItemCount(items, item.getId()));
			}
		}
	}

	private PendingFeedAttempt findBestPendingFeedAttempt(int consumedItemId)
	{
		expireStalePendingFeedAttempts();

		PendingFeedAttempt bestAttempt = null;
		int bestDistance = NO_ATTEMPT_DISTANCE;

		for (PendingFeedAttempt attempt : pendingFeedAttempts)
		{
			if (attempt.foodItemId != consumedItemId) continue;

			int distance = getPendingFeedAttemptDistance(attempt);

			if (bestAttempt == null
					|| distance < bestDistance
					|| distance == bestDistance && attempt.sequence > bestAttempt.sequence)
			{
				bestAttempt = attempt;
				bestDistance = distance;
			}
		}

		return bestAttempt;
	}

	private int getPendingFeedAttemptDistance(PendingFeedAttempt attempt)
	{
		if (client.getLocalPlayer() == null) return NO_ATTEMPT_DISTANCE;

		NPC npc = attempt.npc;

		if (npc == null)
		{
			npc = getVisibleHealerByOrder(attempt.healerOrder);
		}

		if (npc == null) return NO_ATTEMPT_DISTANCE;

		return client.getLocalPlayer().getWorldLocation().distanceTo(npc.getWorldLocation());
	}

	private void recordConsumedFoodForPendingAttempt(PendingFeedAttempt attempt)
	{
		ttkTracker.onFoodConsumedForHealer(attempt.npcIndex, client.getTickCount());

		Integer currentOrder = healerOrderByNpcIndex.get(attempt.npcIndex);

		if (currentOrder != null)
		{
			sharedState.recordLocalFood(currentOrder, currentCallIndex, Math.round(getCurrentWaveElapsedSeconds()));
			sendHealerSyncForOrder(currentOrder);
		}

		int totalFoodFed = currentOrder == null ? 0 : getFoodFedByHealerOrder().getOrDefault(currentOrder, 0);

		log.debug(
				"Counted consumed poisoned food for healer #{} from NPC index {} using item {} from pending sequence {}. Distance at consume {}. Total now {}",
				currentOrder,
				attempt.npcIndex,
				attempt.foodItemId,
				attempt.sequence,
				getPendingFeedAttemptDistance(attempt),
				totalFoodFed
		);
	}

	private void expireStalePendingFeedAttempts()
	{
		Iterator<PendingFeedAttempt> iterator = pendingFeedAttempts.iterator();

		while (iterator.hasNext())
		{
			PendingFeedAttempt attempt = iterator.next();

			if (!isPendingFeedAttemptStale(attempt)) continue;

			log.debug(
					"Expiring pending food feed sequence {} for healer #{} NPC index {} item {} from tick {}",
					attempt.sequence,
					attempt.healerOrder,
					attempt.npcIndex,
					attempt.foodItemId,
					attempt.tick
			);

			iterator.remove();
		}
	}

	private boolean isPendingFeedAttemptStale(PendingFeedAttempt attempt)
	{
		return client.getTickCount() - attempt.tick > PENDING_FEED_ATTEMPT_MAX_AGE_TICKS;
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
		healerIndexBase = -1;
		deadHealerOrders.clear();
		lastPoisonedFoodCountByItemId.clear();
		lastSentHealerSyncByOrder.clear();
		sharedState.reset();
		ttkTracker.reset();
		pendingFeedAttempts.clear();
		feedAttemptSequence = 0;
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
		private final int sequence;
		private final int tick;
		private final int npcIndex;
		private final int healerOrder;
		private final int foodItemId;
		private final NPC npc;

		private PendingFeedAttempt(int sequence, int tick, int npcIndex, int healerOrder, int foodItemId, NPC npc)
		{
			this.sequence = sequence;
			this.tick = tick;
			this.npcIndex = npcIndex;
			this.healerOrder = healerOrder;
			this.foodItemId = foodItemId;
			this.npc = npc;
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
