package com.bahealerorder.healer;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.sidepanel.BaUtilitiesPanel;
import com.bahealerorder.common.BaHealerFoodCounts;
import com.bahealerorder.common.BaHealerSyncMessage;
import com.bahealerorder.common.BaNpcIds;
import com.bahealerorder.common.BaOverviewNpcType;
import com.bahealerorder.common.BaPartySyncService;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.BaRoleDetector;
import com.bahealerorder.common.BaRolePanelOverlay;
import com.bahealerorder.common.BaWaveLifecycleService;
import com.bahealerorder.common.BaWaveLifecycleService.WaveStart;
import com.bahealerorder.common.BaWaveInfo;
import com.bahealerorder.common.BaWaveOverviewService;
import com.bahealerorder.common.NpcIndexOrderer;
import com.bahealerorder.healer.codes.CodeDisplayState;
import com.bahealerorder.healer.codes.HealerCodeStatus;
import com.bahealerorder.healer.codes.WaveCode;
import com.bahealerorder.healer.ttk.HealerTtkPrediction;
import com.bahealerorder.healer.ttk.HealerTtkTracker;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Renderable;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.Hooks;
import net.runelite.client.events.ConfigChanged;
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
	private static final String HEALER_ITEM_MACHINE_NAME = "Healer item machine";
	private static final String TAKE_TOFU_OPTION = "take-tofu";
	private static final String TAKE_WORMS_OPTION = "take-worms";
	private static final String TAKE_MEAT_OPTION = "take-meat";
	private static final String TAKE_VIAL_OPTION = "take-vial";
	private static final String WRONG_FOOD_MESSAGE = "that's the wrong type of poisoned food to use! penalty!";
	private static final String PANEL_ICON_RESOURCE = "/com/bahealerorder/penance_healer.png";
	private static final int MAX_FOOD_PANEL_CODE_CALLS = 3;
	private static final int PENDING_FEED_ATTEMPT_MAX_AGE_TICKS = 10;
	private static final int NO_ATTEMPT_DISTANCE = Integer.MAX_VALUE;

	private static final int BA_HORN_OF_GLORY_GROUP_ID = 484;
	private static final int BA_HEALER_GROUP_ID = 488;
	private static final int BA_HEALER_LISTEN_CHILD_ID = 7;
	private static final int BA_HEALER_CALL_CHILD_ID = 9;
	private static final int BA_HORN_OF_GLORY_DEFENDER_CHILD_ID = 6;

	private static final Color CALLED_FOOD_MENU_COLOR = Color.GREEN;
	private static final Color NOT_STARTED_CODE_COLOR = new Color(255, 60, 60);
	private static final Color IN_PROGRESS_CODE_COLOR = new Color(255, 150, 0);
	private static final Color COMPLETE_CODE_COLOR = new Color(0, 220, 0);
	private static final Color PREVIOUS_CODE_COLOR = new Color(150, 150, 150);


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
	private BaRolePanelOverlay rolePanelOverlay;

	@Inject
	private BaUtilitiesPanel panel;

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

	@Inject
	private BaWaveOverviewService waveOverviewService;

	@Inject
	private BaWaveLifecycleService waveLifecycleService;

	@Inject
	private HealerSharedState sharedState;

	@Getter
	private final Map<Integer, Integer> healerOrderByNpcIndex = new HashMap<>();

	@Getter
	private final Map<NPC, Integer> visibleHealers = new HashMap<>();

	private final Set<Integer> healerIndexesSeenThisWave = new HashSet<>();

	private final Map<Integer, Integer> lastPoisonedFoodCountByItemId = new HashMap<>();
	private final Map<Integer, String> lastSentHealerSyncByOrder = new HashMap<>();

	private final Hooks.RenderableDrawListener drawListener = this::shouldDrawRenderable;

	private int currentCallIndex = 0;
	private int healerIndexBase = -1;
	private String lastCallText;
	private String lastFoodCallText;
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
		rolePanelOverlay.setHealerController(this);
		hooks.registerRenderableDrawListener(drawListener);
		mouseManager.registerMouseListener(rolePanelOverlay);
		overlayManager.add(overlay);
		overlayManager.add(rolePanelOverlay);
		updateNavigationButton();
	}
	public void shutDown()
	{
		removeNavigationButton();
		overlayManager.remove(rolePanelOverlay);
		overlayManager.remove(overlay);
		mouseManager.unregisterMouseListener(rolePanelOverlay);
		rolePanelOverlay.setHealerController(null);
		hooks.unregisterRenderableDrawListener(drawListener);
		resetAllState();
	}

	public void onConfigChanged(ConfigChanged event)
	{
		if (!BaUtilitiesConfig.GROUP_NAME.equals(event.getGroup())) return;

		if ("hideSidePanelButton".equals(event.getKey()))
		{
			updateNavigationButton();
		}
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

		Integer order = getKnownHealerOrder(npcIndex);

		if (order == null) return;

		visibleHealers.put(npc, order);

		ttkTracker.onHealerSpawned(npcIndex, client.getTickCount());
		if (sharedState.recordLocalSpawn(order, npcIndex, getCurrentWaveTick()))
		{
			waveOverviewService.recordHealerStateChanged();
		}

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

		if (visibleHealers.remove(npc) != null)
		{
			log.debug("Removed visible Penance Healer index {}", npc.getIndex());
		}
	}
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!isWaveActive() || !(event.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) event.getActor();
		Hitsplat hitsplat = event.getHitsplat();

		if (!isPenanceHealer(npc))
		{
			return;
		}

		if (!isNonPoisonDamageHitsplat(hitsplat)) return;

		int npcIndex = npc.getIndex();
		Integer healerOrder = getKnownHealerOrder(npcIndex);

		if (healerOrder == null)
		{
			return;
		}

		visibleHealers.put(npc, healerOrder);

		boolean predictionChanged = ttkTracker.switchToHealthRatioTtk(
				npcIndex,
				client.getTickCount(),
				npc.getHealthRatio(),
				npc.getHealthScale());

		boolean modeChanged = ttkTracker.isHealthRatioMode(npcIndex)
				&& sharedState.recordHealthRatioMode(healerOrder);

		if (predictionChanged)
		{
			publishLocalTtkPrediction(healerOrder);
		}
		else if (modeChanged)
		{
			sendHealerSyncForOrder(healerOrder);
		}
	}
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ROOT);
		String target = Text.removeTags(event.getMenuTarget()).toLowerCase(Locale.ROOT);

		handlePoisonedFoodSelection(event, option, target);
		handlePoisonedFoodUseOnHealer(event, option);
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

		updateLocalHealerFoodCounts(event.getItemContainer(), false);
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
		updateHealerDeathState();
		observeVisibleHealerHp();

		if (isHealerRole())
		{
			updateCallIndexFromHealerWidget();
			updateFoodCountsFromHealerListenWidget();
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
				|| waveLifecycleService.getWave() > 0 && event.getWave() != waveLifecycleService.getWave())
		{
			return;
		}

		if (!isWaveActive())
		{
			return;
		}

		List<Integer> newFoodTicks = recordPartyHealerFood(event);
		boolean acceptPrediction = event.isHealthRatioMode() || !newFoodTicks.isEmpty();
		boolean changed = sharedState.updateFromParty(event, acceptPrediction);
		rebuildVisibleHealerOrders();
		if (changed)
		{
			waveOverviewService.recordHealerStateChanged();
		}
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

		String message = Text.removeTags(event.getMessage()).toLowerCase(Locale.ROOT);

		if (message.contains(WRONG_FOOD_MESSAGE))
		{
			log.debug("Wrong poisoned food detected. Cancelling pending feed attempt.");
			pendingFeedAttempts.clear();
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
		return waveLifecycleService.getWave();
	}

	public boolean isWaveActive()
	{
		return waveLifecycleService.isWaveActive();
	}

	public boolean isHealerRole()
	{
		return roleDetector.isRole(BaRole.HEALER)
				|| (waveLifecycleService.isDevWaveActive() && !roleDetector.hasDevRoleOverride());
	}

	public BaUtilitiesConfig.FoodPanelStyle getFoodPanelStyle()
	{
		return config.foodPanelStyle();
	}

	public boolean shouldShowLabels()
	{
		return !config.showLabelsAsHealerOnly() || isHealerRole();
	}

	public boolean shouldShowHealerHighlights()
	{
		return isHealerRole();
	}

	public boolean shouldShowFoodCountOnNpc()
	{
		return isHealerRole() && config.showFoodCountOnNpc();
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

		HealerTtkPrediction prediction = ttkTracker.getPrediction(npc.getIndex());
		Integer healerOrder = getKnownHealerOrder(npc.getIndex());

		if (healerOrder != null)
		{
			String sharedText = getSharedHealerTtkText(healerOrder);
			if (sharedText != null) return sharedText;
		}

		if (!prediction.hasValue())
		{
			return healerOrder == null ? null : getSharedHealerTtkText(healerOrder);
		}

		if (prediction.isUnknown()) return "?";

		int deathTick = prediction.getDeathTick();

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

		String sharedDeathTime = getSharedHealerDeathTime(healerOrder);

		if (isHealerDead(healerOrder)) return sharedDeathTime;

		if (sharedDeathTime != null) return sharedDeathTime;

		NPC npc = getVisibleHealerByOrder(healerOrder);

		if (npc == null)
		{
			return sharedDeathTime;
		}

		HealerTtkPrediction prediction = ttkTracker.getPrediction(npc.getIndex());
		if (!prediction.hasValue()) return sharedDeathTime;
		if (prediction.isUnknown()) return "?";

		return formatTickAsWaveTime(prediction.getDeathTick());
	}

	private String getSharedHealerDeathTime(int healerOrder)
	{
		Integer actualDeathTick = sharedState.getActualDeathTick(healerOrder);
		if (actualDeathTick != null) return formatWaveTickAsTime(actualDeathTick);

		Integer predictedDeathTick = sharedState.getPredictedDeathTick(healerOrder);
		if (predictedDeathTick != null) return formatWaveTickAsTime(predictedDeathTick);

		return sharedState.hasUnknownTtk(healerOrder) ? "?" : null;
	}

	private String getSharedHealerTtkText(int healerOrder)
	{
		Integer deathTick = sharedState.getActualDeathTick(healerOrder);

		if (deathTick == null)
		{
			deathTick = sharedState.getPredictedDeathTick(healerOrder);
		}

		if (deathTick == null)
		{
			return sharedState.hasUnknownTtk(healerOrder) ? "?" : null;
		}

		if (config.healerTtkDisplay() == BaUtilitiesConfig.HealerTtkDisplayMode.TICKS)
		{
			return Math.max(deathTick - getCurrentWaveTick() + 1, 0) + "t";
		}

		if (config.healerTtkDisplay() == BaUtilitiesConfig.HealerTtkDisplayMode.SECONDS)
		{
			return formatTickCountdownAsSeconds(Math.max(deathTick - getCurrentWaveTick() + 1, 0));
		}

		return formatWaveTickAsTime(deathTick);
	}

	public long getCurrentWaveElapsedMillis()
	{
		if (!isWaveActive())
		{
			return 0;
		}

		return waveLifecycleService.getElapsedTimeMs();
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
		WaveCode waveCode = codeManager.getActiveWaveCode(getCurrentWave());
		return waveCode == null ? null : waveCode.getSourceText();
	}

	public String getCurrentWaveCodeName()
	{
		WaveCode waveCode = codeManager.getActiveWaveCode(getCurrentWave());
		return waveCode == null ? null : waveCode.getName();
	}

	public boolean hasActiveWaveCode()
	{
		WaveCode waveCode = codeManager.getActiveWaveCode(getCurrentWave());
		return waveCode != null && !waveCode.getCalls().isEmpty();
	}

	public int getExpectedFoodForOrder(int healerOrder)
	{
		if (healerOrder <= 0) return 0;

		return codeManager.getExpectedFoodForOrder(getCurrentWave(), healerOrder, getEffectiveCurrentCallIndex());
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
		Map<NPC, Integer> trackedHealers = new HashMap<>();

		for (NPC npc : visibleHealers.keySet())
		{
			if (npc == null) continue;

			Integer healerOrder = getKnownHealerOrder(npc.getIndex());
			if (healerOrder != null)
			{
				trackedHealers.put(npc, healerOrder);
			}
		}

		return Collections.unmodifiableMap(trackedHealers);
	}

	public List<Integer> getHealerOrdersForCurrentWave()
	{
		int healerCount = BaWaveInfo.getExpectedCount(getCurrentWave(), BaOverviewNpcType.HEALER);
		if (healerCount <= 0)
		{
			return Collections.emptyList();
		}

		List<Integer> healerOrders = new ArrayList<>();

		for (int healerOrder = 1; healerOrder <= healerCount; healerOrder++)
		{
			healerOrders.add(healerOrder);
		}

		return Collections.unmodifiableList(healerOrders);
	}

	public List<Integer> getFoodPanelCallIndexes()
	{
		WaveCode waveCode = codeManager.getActiveWaveCode(getCurrentWave());

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

		if (label.matches("\\d+\\s+\\(R\\d+\\)"))
		{
			return label.replaceFirst("^(\\d+)(\\s+\\(R\\d+\\))$", "$1s$2");
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

		HealerCodeStatus status = codeManager.getPanelStatusForCall(getCurrentWave(), healerOrder, getEffectiveCurrentCallIndex(), callIndex, sharedState.getFeedEvents());
		String codeText = formatCodeStatus(status);

		if (codeText != null)
		{
			return codeText;
		}

		return formatRawFoodCount(codeManager.getPanelFoodCountForCall(getCurrentWave(), healerOrder, getEffectiveCurrentCallIndex(), callIndex, sharedState.getFeedEvents()));
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

		HealerCodeStatus status = codeManager.getPanelStatusForCall(getCurrentWave(), healerOrder, getEffectiveCurrentCallIndex(), callIndex, sharedState.getFeedEvents());

		return status == null ? null : getFoodPanelCodeStatusColor(status.getState());
	}

	private Color getFoodPanelCodeStatusColor(CodeDisplayState state)
	{
		return state == CodeDisplayState.PREVIOUS ? COMPLETE_CODE_COLOR : getCodeStatusColor(state);
	}

	public boolean hasHealerSpawned(int healerOrder)
	{
		return sharedState.hasSpawned(healerOrder);
	}

	public boolean isHealerDead(int healerOrder)
	{
		return sharedState.isDead(healerOrder);
	}

	public HealerCodeStatus getCurrentCodeStatus(int healerOrder)
	{
		return codeManager.getCurrentStatus(getCurrentWave(), healerOrder, getEffectiveCurrentCallIndex(), sharedState.getFeedEvents());
	}

	public HealerCodeStatus getPreviousCodeStatus(int healerOrder)
	{
		return codeManager.getPreviousStatus(getCurrentWave(), healerOrder, getEffectiveCurrentCallIndex(), sharedState.getFeedEvents());
	}

	public HealerCodeStatus getDisplayCodeStatus(int healerOrder)
	{
		return codeManager.getDisplayStatus(getCurrentWave(), healerOrder, getEffectiveCurrentCallIndex(), sharedState.getFeedEvents());
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
			return COMPLETE_CODE_COLOR;
		}

		if (state == CodeDisplayState.PREVIOUS)
		{
			return PREVIOUS_CODE_COLOR;
		}

		if (state == CodeDisplayState.IN_PROGRESS)
		{
			return IN_PROGRESS_CODE_COLOR;
		}

		return NOT_STARTED_CODE_COLOR;
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

		int wave = getCurrentWave();
		List<String> labelsForWave = BaWaveInfo.getLabels(wave, BaOverviewNpcType.HEALER);

		if (healerOrder <= 0 || healerOrder > labelsForWave.size())
		{
			return String.valueOf(healerOrder);
		}

		int initialHealerCount = BaWaveInfo.getInitialCount(wave, BaOverviewNpcType.HEALER);

		if (healerOrder > initialHealerCount)
		{
			return getReserveHealerLabel(wave, healerOrder, false);
		}

		String label = labelsForWave.get(healerOrder - 1);
		return label.replaceFirst("^(\\d+)s(.*)$", "$1$2");
	}

	private String getReserveHealerLabel(int wave, int healerOrder, boolean includeSecondsSuffix)
	{
		int initialHealerCount = BaWaveInfo.getInitialCount(wave, BaOverviewNpcType.HEALER);
		int reserveNumber = healerOrder - initialHealerCount;

		if (reserveNumber <= 0)
		{
			return String.valueOf(healerOrder);
		}

		int spawnTick = sharedState.getSpawnTick(healerOrder);

		if (spawnTick < 0)
		{
			return "R" + reserveNumber;
		}

		String secondsSuffix = includeSecondsSuffix ? "s" : "";
		return formatWaveTickAsNearestWholeSeconds(spawnTick) + secondsSuffix + " (R" + reserveNumber + ")";
	}

	private int formatWaveTickAsNearestWholeSeconds(int waveTick)
	{
		return Math.round(Math.max(0, waveTick) * 0.6f);
	}

	private String getHealerMenuSuffix(int healerOrder)
	{
		List<String> parts = new ArrayList<>();

		if (shouldShowMenuLabel()
				&& config.healerLabelStyle() != BaUtilitiesConfig.HealerLabelStyle.NONE)
		{
			parts.add(ColorUtil.wrapWithColorTag("(" + getHealerLabel(healerOrder) + ")", config.hullColor()));
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

	public void onWaveStarted(WaveStart waveStart)
	{
		int waveNumber = waveStart.getWave();
		if (waveNumber <= 0) return;

		resetWaveTrackedState();
		sharedState.startWave(waveNumber);
		ttkTracker.startWave(waveStart.getTick(), waveNumber);
		updateLocalHealerFoodCounts(client.getItemContainer(InventoryID.INVENTORY), true);

		log.debug(
				"Starting new BA wave {} at tick {} from {} message node {}",
				waveNumber,
				waveStart.getTick(),
				waveStart.getSource(),
				waveStart.getMessageNodeId()
		);
	}

	public void onWaveEnded()
	{
		resetWaveState();
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

		for (Integer healerOrder : new HashSet<>(getTrackedHealers().values()))
		{
			if (healerOrder != null)
			{
				sendHealerSyncForOrder(healerOrder);
			}
		}
	}

	private void observeVisibleHealerHp()
	{
		boolean incompleteDuoHealerParty = partySyncService.hasIncompleteDuoHealerParty();

		for (Map.Entry<NPC, Integer> entry : getTrackedHealers().entrySet())
		{
			NPC npc = entry.getKey();
			Integer healerOrder = entry.getValue();

			if (npc == null || healerOrder == null) continue;

			int npcIndex = npc.getIndex();
			boolean shouldUseHealthRatio = incompleteDuoHealerParty || sharedState.isHealthRatioMode(healerOrder);
			boolean predictionChanged = shouldUseHealthRatio && !ttkTracker.isHealthRatioMode(npcIndex)
					? ttkTracker.switchToHealthRatioTtk(npcIndex, client.getTickCount(), npc.getHealthRatio(), npc.getHealthScale())
					: ttkTracker.observeHp(npcIndex, client.getTickCount(), npc.getHealthRatio(), npc.getHealthScale());

			boolean modeChanged = ttkTracker.isHealthRatioMode(npcIndex)
					&& sharedState.recordHealthRatioMode(healerOrder);

			if (predictionChanged)
			{
				publishLocalTtkPrediction(healerOrder);
			}
			else if (modeChanged)
			{
				sendHealerSyncForOrder(healerOrder);
			}
		}
	}

	private void publishLocalTtkPrediction(int healerOrder)
	{
		NPC npc = getVisibleHealerByOrder(healerOrder);
		if (npc == null) return;

		HealerTtkPrediction prediction = ttkTracker.getPrediction(npc.getIndex());
		if (!prediction.hasValue()) return;

		int deathTick = prediction.hasDeathTick() ? toWaveTick(prediction.getDeathTick()) : -1;
		if (sharedState.recordPrediction(healerOrder, deathTick, prediction.isUnknown()))
		{
			waveOverviewService.recordHealerStateChanged();
		}

		sendHealerSyncForOrder(healerOrder, prediction, false);
	}

	private void broadcastHealerSyncState()
	{
		if (!partySyncService.isBaPartySyncConnected()) return;

		for (int healerOrder : getHealerOrdersForCurrentWave())
		{
			if (hasHealerSpawned(healerOrder) || isHealerDead(healerOrder) || getFoodFedByHealerOrder().containsKey(healerOrder))
			{
				sendHealerSyncForOrder(healerOrder, null, true);
			}
		}
	}

	private void sendHealerSyncForOrder(int healerOrder)
	{
		sendHealerSyncForOrder(healerOrder, null, false);
	}

	private void sendHealerSyncForOrder(int healerOrder, HealerTtkPrediction prediction, boolean force)
	{
		if (!partySyncService.isBaPartySyncConnected() || getCurrentWave() <= 0 || healerOrder <= 0) return;

		BaHealerSyncMessage message = buildHealerSyncMessage(healerOrder, prediction);
		if (message == null) return;

		String signature = buildHealerSyncSignature(message);

		if (!force && signature.equals(lastSentHealerSyncByOrder.get(healerOrder))) return;

		lastSentHealerSyncByOrder.put(healerOrder, signature);
		partySyncService.sendHealerSync(message);
	}

	private BaHealerSyncMessage buildHealerSyncMessage(int healerOrder, HealerTtkPrediction prediction)
	{
		int npcIndex = sharedState.getNpcIndex(healerOrder);
		if (npcIndex < 0) return null;

		Integer actualDeathTick = sharedState.getActualDeathTick(healerOrder);
		int predictedDeathTick = prediction != null && prediction.hasDeathTick()
				? toWaveTick(prediction.getDeathTick())
				: -1;
		boolean unknownTtk = prediction != null && prediction.isUnknown();

		return new BaHealerSyncMessage(
				client.getWorld(),
				getCurrentWave(),
				npcIndex,
				healerOrder,
				sharedState.getSpawnTick(healerOrder),
				currentCallIndex,
				predictedDeathTick,
				unknownTtk,
				actualDeathTick == null ? -1 : actualDeathTick,
				actualDeathTick != null && sharedState.isObservedDeath(healerOrder),
				ttkTracker.isHealthRatioMode(npcIndex),
				sharedState.getLocalFoodTicks(healerOrder)
		);
	}

	private String buildHealerSyncSignature(BaHealerSyncMessage message)
	{
		return message.getWave()
				+ ":" + message.getNpcIndex()
				+ ":" + message.getHealerOrder()
				+ ":" + message.getSpawnTick()
				+ ":" + message.getCurrentCallIndex()
				+ ":" + message.getPredictedDeathTick()
				+ ":" + message.isUnknownTtk()
				+ ":" + message.getActualDeathTick()
				+ ":" + message.isObservedDeath()
				+ ":" + message.isHealthRatioMode()
				+ ":" + Arrays.toString(message.getFoodTicks());
	}

	private List<Integer> recordPartyHealerFood(BaHealerSyncMessage event)
	{
		if (event.getNpcIndex() < 0 || event.getSpawnTick() < 0) return Collections.emptyList();

		int waveStartTick = ttkTracker.getWaveStartTick();
		if (waveStartTick < 0) return Collections.emptyList();

		ttkTracker.onHealerSpawned(
				event.getNpcIndex(),
				waveStartTick + event.getSpawnTick()
		);

		List<Integer> newFoodTicks = sharedState.recordPartyFoodTicks(
				event.getMemberId(),
				event.getHealerOrder(),
				event.getFoodTicks()
		);

		for (int foodTick : newFoodTicks)
		{
			ttkTracker.onFoodConsumedForHealer(event.getNpcIndex(), waveStartTick + foodTick);
		}

		return newFoodTicks;
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

	private boolean isNonPoisonDamageHitsplat(Hitsplat hitsplat)
	{
		return hitsplat != null
				&& hitsplat.getAmount() > 0
				&& hitsplat.getHitsplatType() != HitsplatID.HEAL
				&& hitsplat.getHitsplatType() != HitsplatID.POISON;
	}

	private void updateHealerDeathState()
	{
		int currentWaveTick = getCurrentWaveTick();

		for (int healerOrder : getHealerOrdersForCurrentWave())
		{
			NPC npc = getVisibleHealerByOrder(healerOrder);
			if (npc != null)
			{
				if (isDeadPenanceHealer(npc))
				{
					recordDeadHealer(healerOrder, currentWaveTick);
				}
				else
				{
					clearPresumedDeadHealer(healerOrder);
				}
				continue;
			}

			if (sharedState.isDead(healerOrder)) continue;

			Integer predictedDeathTick = sharedState.getPredictedDeathTick(healerOrder);
			if (predictedDeathTick != null && currentWaveTick > predictedDeathTick)
			{
				recordPresumedDeadHealer(healerOrder, predictedDeathTick);
			}
		}
	}

	private void clearPresumedDeadHealer(int healerOrder)
	{
		if (sharedState.clearPresumedDeath(healerOrder))
		{
			waveOverviewService.recordHealerStateChanged();
			sendHealerSyncForOrder(healerOrder);
		}
	}

	private void recordDeadHealer(int healerOrder, int deathTick)
	{
		if (sharedState.recordDeath(healerOrder, deathTick))
		{
			waveOverviewService.recordHealerStateChanged();
			sendHealerSyncForOrder(healerOrder);
		}
	}

	private void recordPresumedDeadHealer(int healerOrder, int deathTick)
	{
		if (sharedState.recordPresumedDeath(healerOrder, deathTick))
		{
			waveOverviewService.recordHealerStateChanged();
			sendHealerSyncForOrder(healerOrder);
		}
	}

	private NPC getVisibleHealerByOrder(int healerOrder)
	{
		for (Map.Entry<NPC, Integer> entry : visibleHealers.entrySet())
		{
			NPC npc = entry.getKey();
			Integer knownOrder = npc == null ? null : getKnownHealerOrder(npc.getIndex());

			if (knownOrder != null && knownOrder == healerOrder)
			{
				return npc;
			}
		}

		return null;
	}

	private boolean isBaNpc(NPC npc)
	{
		return BaNpcIds.isPenanceNpc(npc);
	}

	private void rebuildHealerOrderByNpcIndex()
	{
		Map<Integer, Integer> knownOrderByNpcIndex = sharedState.getHealerOrdersByNpcIndex();
		int expectedHealerCount = BaWaveInfo.getExpectedCount(getCurrentWave(), BaOverviewNpcType.HEALER);
		int maxHealerOrder = expectedHealerCount > 0
				? expectedHealerCount
				: healerIndexesSeenThisWave.size() + knownOrderByNpcIndex.size();

		healerOrderByNpcIndex.clear();
		healerOrderByNpcIndex.putAll(NpcIndexOrderer.buildOrderByNpcIndex(
				healerIndexesSeenThisWave,
				knownOrderByNpcIndex,
				healerIndexBase,
				maxHealerOrder
		));
	}

	private Integer getKnownHealerOrder(int npcIndex)
	{
		Integer sharedOrder = sharedState.getHealerOrderForNpcIndex(npcIndex);

		if (sharedOrder != null)
		{
			return sharedOrder;
		}

		return healerOrderByNpcIndex.get(npcIndex);
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

			Integer correctedOrder = getKnownHealerOrder(npc.getIndex());

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
		if (getCurrentWave() <= 0) return;

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
			updateLocalHealerFoodCounts(client.getItemContainer(InventoryID.INVENTORY), false);
			log.debug("BA healer call changed to {} at wave {} call {}", callText, getCurrentWave(), currentCallIndex);
		}
	}

	private void updateFoodCountsFromHealerListenWidget()
	{
		if (getCurrentWave() <= 0) return;

		String callText = getHealerListenText();

		if (callText == null || callText.isEmpty()) return;

		if (lastFoodCallText == null)
		{
			lastFoodCallText = callText;
			updateLocalHealerFoodCounts(client.getItemContainer(InventoryID.INVENTORY), false);
			return;
		}

		if (!lastFoodCallText.equals(callText))
		{
			lastFoodCallText = callText;
			updateLocalHealerFoodCounts(client.getItemContainer(InventoryID.INVENTORY), false);
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
		BaUtilitiesConfig.DispenserOptions dispenserOptions = config.dispenserOptions();

		if (!config.highlightCalledDispenserFood()
				&& !dispenserOptions.removeTakeVial()
				&& !dispenserOptions.moveTakeMeatUp()) return;

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

			if (dispenserOptions.removeTakeVial() && TAKE_VIAL_OPTION.equals(optionText))
			{
				changed = true;
				continue;
			}

			nextEntries.add(entry);
		}

		MenuEntry[] updatedEntries = nextEntries.toArray(new MenuEntry[0]);

		if (dispenserOptions.moveTakeMeatUp())
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

	private void handlePoisonedFoodUseOnHealer(MenuOptionClicked event, String option)
	{
		if (!isHealerRole()) return;

		if (event.getMenuAction() != MenuAction.WIDGET_TARGET_ON_NPC) return;

		if (!"use".equals(option)) return;

		if (selectedPoisonedFoodItemId == null || selectedPoisonedFoodItemId <= 0) return;
		if (!isPoisonedFoodItem(selectedPoisonedFoodItemId)) return;

		int npcIndex = event.getId();
		Integer healerOrder = getKnownHealerOrder(npcIndex);

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
		return getPoisonedFoodType(itemId) != BaHealerFoodCounts.FOOD_NONE;
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

		return getKnownHealerOrder(entry.getIdentifier()) != null;
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

	private void updateLocalHealerFoodCounts(ItemContainer itemContainer, boolean forceSend)
	{
		if (!isHealerRole() || client.getLocalPlayer() == null)
		{
			return;
		}

		partySyncService.updateLocalHealerFoodCounts(
				client.getLocalPlayer().getName(),
				buildLocalHealerFoodCounts(itemContainer),
				forceSend
		);
	}

	private BaHealerFoodCounts buildLocalHealerFoodCounts(ItemContainer itemContainer)
	{
		Item[] items = itemContainer == null ? null : itemContainer.getItems();

		return new BaHealerFoodCounts(
				getItemCount(items, ItemID.BARBASSAULT_POISION_01),
				getItemCount(items, ItemID.BARBASSAULT_POISION_02),
				getItemCount(items, ItemID.BARBASSAULT_POISION_03),
				getCalledFoodType()
		);
	}

	private int getCalledFoodType()
	{
		String callText = getHealerListenText();

		if (callText == null)
		{
			return BaHealerFoodCounts.FOOD_NONE;
		}

		if (callText.contains("tofu"))
		{
			return BaHealerFoodCounts.FOOD_TOFU;
		}

		if (callText.contains("worm"))
		{
			return BaHealerFoodCounts.FOOD_WORMS;
		}

		if (callText.contains("meat"))
		{
			return BaHealerFoodCounts.FOOD_MEAT;
		}

		return BaHealerFoodCounts.FOOD_NONE;
	}

	private int getPoisonedFoodType(int itemId)
	{
		switch (itemId)
		{
			case ItemID.BARBASSAULT_POISION_01:
				return BaHealerFoodCounts.FOOD_TOFU;
			case ItemID.BARBASSAULT_POISION_02:
				return BaHealerFoodCounts.FOOD_WORMS;
			case ItemID.BARBASSAULT_POISION_03:
				return BaHealerFoodCounts.FOOD_MEAT;
			default:
				return BaHealerFoodCounts.FOOD_NONE;
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

		Integer currentOrder = getKnownHealerOrder(attempt.npcIndex);

		if (currentOrder != null)
		{
			sharedState.recordLocalFood(currentOrder, currentCallIndex, Math.round(getCurrentWaveElapsedSeconds()), getCurrentWaveTick());
			publishLocalTtkPrediction(currentOrder);
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
		return BaNpcIds.isPenanceHealer(npc);
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
			return getKnownHealerOrder(npc.getIndex());
		}

		return getKnownHealerOrder(entry.getIdentifier());
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

	private void resetWaveState()
	{
		resetWaveTrackedState();
	}

	private void resetWaveTrackedState()
	{
		visibleHealers.clear();
		healerIndexesSeenThisWave.clear();
		healerOrderByNpcIndex.clear();
		healerIndexBase = -1;
		lastPoisonedFoodCountByItemId.clear();
		lastSentHealerSyncByOrder.clear();
		sharedState.reset();
		ttkTracker.reset();
		pendingFeedAttempts.clear();
		feedAttemptSequence = 0;
		selectedPoisonedFoodItemId = null;
		currentCallIndex = 0;
		lastCallText = null;
		lastFoodCallText = null;
		currentCallText = null;
		currentCallSource = null;
		callTrackingArmed = false;
	}

	private void resetAllState()
	{
		resetWaveState();
	}

	private void updateNavigationButton()
	{
		if (config.hideSidePanelButton())
		{
			removeNavigationButton();
			return;
		}

		if (navigationButton != null) return;

		navigationButton = NavigationButton.builder()
				.tooltip("BA Utilities")
				.icon(createPanelIcon())
				.priority(10)
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navigationButton);
	}

	private void removeNavigationButton()
	{
		if (navigationButton == null) return;

		clientToolbar.removeNavigation(navigationButton);
		navigationButton = null;
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
