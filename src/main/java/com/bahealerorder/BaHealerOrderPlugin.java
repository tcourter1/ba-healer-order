package com.bahealerorder;

import com.bahealerorder.codes.CodeDisplayState;
import com.bahealerorder.codes.FeedEvent;
import com.bahealerorder.codes.HealerCodeStatus;
import com.bahealerorder.codes.WaveCode;
import com.bahealerorder.ttk.HealerTtkResult;
import com.bahealerorder.ttk.HealerTtkTracker;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
import java.util.LinkedHashSet;
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
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.input.MouseManager;
import net.runelite.client.party.PartyService;
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
	private static final String PENANCE_NPC_PREFIX = "Penance ";
	private static final String HEALER_ITEM_MACHINE_NAME = "Healer item machine";
	private static final String ATTACKER_ITEM_MACHINE_NAME = "Attacker item machine";
	private static final String DEFENDER_ITEM_MACHINE_NAME = "Defender item machine";
	private static final String COLLECTOR_CONVERTER_NAME = "Collector Converter";
	private static final String TAKE_TOFU_OPTION = "take-tofu";
	private static final String TAKE_WORMS_OPTION = "take-worms";
	private static final String TAKE_MEAT_OPTION = "take-meat";
	private static final String TAKE_VIAL_OPTION = "take-vial";
	private static final String WRONG_FOOD_MESSAGE = "that's the wrong type of poisoned food to use! penalty!";
	private static final String PANEL_ICON_RESOURCE = "/com/bahealerorder/penance_healer.png";
	private static final String BA_SYNC_PASSPHRASE_SUFFIX = "BASYNC69";
	private static final int MAX_FOOD_PANEL_CODE_CALLS = 3;
	private static final int NPC_INDEX_MODULUS = 1 << 16;
	private static final int PENDING_FEED_ATTEMPT_MAX_AGE_TICKS = 5;
	private static final int NO_ATTEMPT_DISTANCE = Integer.MAX_VALUE;
	private static final int BA_TEAM_WIDGET_DEBUG_INTERVAL_TICKS = 10;

	private static final int CHATBOX_GROUP_ID = 162;
	private static final int BA_TEAM_GROUP_ID = 256;
	private static final int BA_HORN_OF_GLORY_GROUP_ID = 484;
	private static final int BA_ATTACKER_GROUP_ID = 485;
	private static final int BA_COLLECTOR_GROUP_ID = 486;
	private static final int BA_DEFENDER_GROUP_ID = 487;
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
	private BaHealerOrderOverlay overlay;

	@Inject
	private BaHealerOrderFoodOverlay foodOverlay;

	@Inject
	private BaHealerOrderPanel panel;

	@Inject
	private BaHealerCodeManager codeManager;

	@Inject
	private HealerTtkTracker ttkTracker;

	@Inject
	private PartyService partyService;

	@Inject
	private BaHealerOrderConfig config;

	@Getter
	private final Map<Integer, Integer> healerOrderByNpcIndex = new HashMap<>();

	@Getter
	private final Map<NPC, Integer> visibleHealers = new HashMap<>();

	private final Set<Integer> healerIndexesSeenThisWave = new HashSet<>();
	private final Set<Integer> deadHealerOrders = new HashSet<>();

	private final Map<Integer, Integer> foodFedByNpcIndex = new HashMap<>();
	private final Map<Integer, Integer> lastTtkDeathTickByHealerOrder = new HashMap<>();
	private final Map<Integer, Integer> lastPoisonedFoodCountByItemId = new HashMap<>();

	@Getter
	private final List<FeedEvent> feedEvents = new ArrayList<>();

	private final Hooks.RenderableDrawListener drawListener = this::shouldDrawRenderable;

	private int currentWave = -1;
	private int currentCallIndex = 0;
	private int inGameBit;
	private int healerIndexBase = -1;
	private int feedAttemptSequence;
	private int lastBaTeamWidgetDebugTick = -BA_TEAM_WIDGET_DEBUG_INTERVAL_TICKS;
	private long waveStartTimeMs = -1;
	private String lastCallText;
	private String currentCallText;
	private String currentCallSource;
	private String baPartySyncStatus = "Off";
	private String baPartySyncProgenitorName;
	private String baPartySyncPassphrase;
	private String lastDisplayedBaPartySyncStatus;
	private String lastDisplayedBaPartySyncProgenitorName;
	private boolean callTrackingArmed;
	private boolean baSyncManagedParty;
	private Integer selectedPoisonedFoodItemId;
	private final List<PendingFeedAttempt> pendingFeedAttempts = new ArrayList<>();
	private NavigationButton navigationButton;
	private Role currentRole;

	private enum Role
	{
		ATTACKER(BA_ATTACKER_GROUP_ID, InterfaceID.BARBASSAULT_OVER_ATT),
		COLLECTOR(BA_COLLECTOR_GROUP_ID, InterfaceID.BARBASSAULT_OVER_COL),
		DEFENDER(BA_DEFENDER_GROUP_ID, InterfaceID.BARBASSAULT_OVER_DEF),
		HEALER(BA_HEALER_GROUP_ID, InterfaceID.BARBASSAULT_OVER_HEAL);

		private final int groupId;
		private final int interfaceGroupId;

		Role(int groupId, int interfaceGroupId)
		{
			this.groupId = groupId;
			this.interfaceGroupId = interfaceGroupId;
		}

		private static Role fromGroupId(int groupId)
		{
			for (Role role : values())
			{
				if (groupId == role.groupId || groupId == role.interfaceGroupId)
				{
					return role;
				}
			}

			return null;
		}
	}

	@Override
	protected void startUp()
	{
		codeManager.load();
		panel.refreshAll();
		SwingUtilities.updateComponentTreeUI(panel.getWrappedPanel());
		resetAllState();
		updateBaPartySyncPanelStatus();
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

	@Override
	protected void shutDown()
	{
		leaveBaSyncParty("plugin shutdown");

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

	@Subscribe
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

	@Subscribe(priority = -2)
	public void onPostMenuSort(PostMenuSort event)
	{
		filterPoisonedFoodUseEntries();
		applyDispenserMenuOptions();
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		applyDispenserMenuOptions(true);
	}

	@Subscribe
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

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (currentRole == null)
		{
			detectRoleFromLoadedWidgets();
		}

		if (isHealerRole())
		{
			updateCallIndexFromHealerWidget();
			updateDeadHealerOrders();
			ttkTracker.observeVisibleHealers(visibleHealers.keySet(), client.getTickCount());
		}

		updateBaPartySync();

		if (client.isMenuOpen())
		{
			applyDispenserMenuOptions(true);
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		Role role = Role.fromGroupId(event.getGroupId());
		if (role != null) setRole(role);
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

		if (handleWaveStartMessage(message)) return;

		if (message.contains(WRONG_FOOD_MESSAGE))
		{
			log.debug("Wrong poisoned food detected. Cancelling pending feed attempts.");
			pendingFeedAttempts.clear();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int currentInGameBit = client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING);

		if (inGameBit == currentInGameBit) return;

		inGameBit = currentInGameBit;

		if (currentInGameBit == 0)
		{
			leaveBaSyncParty("BA wave or activity ended");
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
			leaveBaSyncParty("game state changed to " + gameState);
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
		return currentRole == Role.HEALER;
	}

	public boolean shouldShowFoodPanel()
	{
		return config.foodPanelStyle() != BaHealerOrderConfig.FoodPanelStyle.NONE
				&& (!config.showFoodPanelAsHealerOnly() || isHealerRole());
	}

	public BaHealerOrderConfig.FoodPanelStyle getFoodPanelStyle()
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
		return isHealerRole() && config.healerTtkDisplay() != BaHealerOrderConfig.HealerTtkDisplayMode.OFF;
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

		if (!result.isPresent())
		{
			if (ttkTracker.hasPoisonedHealerWithUnknownTtk(npc.getIndex()))
			{
				return "?";
			}

			return null;
		}

		int deathTick = result.get().getDeathTick();
		Integer healerOrder = visibleHealers.get(npc);

		if (healerOrder != null)
		{
			lastTtkDeathTickByHealerOrder.put(healerOrder, deathTick);
		}

		int ticksRemaining = Math.max(deathTick - client.getTickCount() + 1, 0);

		if (config.healerTtkDisplay() == BaHealerOrderConfig.HealerTtkDisplayMode.TICKS)
		{
			return ticksRemaining + "t";
		}

		if (config.healerTtkDisplay() == BaHealerOrderConfig.HealerTtkDisplayMode.SECONDS)
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
		if (!isHealerRole() || healerOrder <= 0)
		{
			return null;
		}

		Integer cachedDeathTick = lastTtkDeathTickByHealerOrder.get(healerOrder);

		if (isHealerDead(healerOrder))
		{
			return cachedDeathTick == null ? null : formatTickAsWaveTime(cachedDeathTick);
		}

		NPC npc = getVisibleHealerByOrder(healerOrder);

		if (npc == null)
		{
			return cachedDeathTick == null ? null : formatTickAsWaveTime(cachedDeathTick) + "?";
		}

		Optional<HealerTtkResult> result = ttkTracker.getTtk(npc.getIndex(), client.getTickCount());

		if (!result.isPresent())
		{
			return ttkTracker.hasPoisonedHealerWithUnknownTtk(npc.getIndex()) ? "?" : null;
		}

		int deathTick = result.get().getDeathTick();
		lastTtkDeathTickByHealerOrder.put(healerOrder, deathTick);
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
		if (!isHealerRole()) return null;

		WaveCode waveCode = codeManager.getActiveWaveCode(currentWave);
		return waveCode == null ? null : waveCode.getSourceText();
	}

	public String getCurrentWaveCodeName()
	{
		if (!isHealerRole()) return null;

		WaveCode waveCode = codeManager.getActiveWaveCode(currentWave);
		return waveCode == null ? null : waveCode.getName();
	}

	public boolean hasActiveWaveCode()
	{
		if (!isHealerRole()) return false;

		WaveCode waveCode = codeManager.getActiveWaveCode(currentWave);
		return waveCode != null && !waveCode.getCalls().isEmpty();
	}

	public int getExpectedFoodForOrder(int healerOrder)
	{
		if (healerOrder <= 0 || !isHealerRole()) return 0;

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
		int lastVisibleCallIndex = Math.min(currentCallIndex, MAX_FOOD_PANEL_CODE_CALLS - 1);

		for (int callIndex = 0; callIndex <= lastVisibleCallIndex; callIndex++)
		{
			callIndexes.add(callIndex);
		}

		return Collections.unmodifiableList(callIndexes);
	}

	public String getFoodPanelHealerLabel(int healerOrder)
	{
		if (config.healerLabelStyle() == BaHealerOrderConfig.HealerLabelStyle.TIME_BASED_NUMBERING)
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
		if (!isHealerRole())
		{
			return "";
		}

		if (isHealerDead(healerOrder))
		{
			return "-";
		}

		if (callIndex < 0)
		{
			int foodFed = getFoodFedByHealerOrder().getOrDefault(healerOrder, 0);
			return getFoodCountText(healerOrder, foodFed);
		}

		HealerCodeStatus status = codeManager.getPanelStatusForCall(currentWave, healerOrder, currentCallIndex, callIndex, feedEvents);
		String codeText = formatCodeStatus(status);

		if (codeText != null)
		{
			return codeText;
		}

		return formatRawFoodCount(codeManager.getPanelFoodCountForCall(currentWave, healerOrder, currentCallIndex, callIndex, feedEvents));
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

		HealerCodeStatus status = codeManager.getPanelStatusForCall(currentWave, healerOrder, currentCallIndex, callIndex, feedEvents);

		return status == null ? null : getFoodPanelCodeStatusColor(status.getState());
	}

	private Color getFoodPanelCodeStatusColor(CodeDisplayState state)
	{
		return state == CodeDisplayState.PREVIOUS ? config.completeCodeColor() : getCodeStatusColor(state);
	}

	public boolean hasHealerSpawned(int healerOrder)
	{
		return healerOrderByNpcIndex.containsValue(healerOrder);
	}

	public boolean isHealerDead(int healerOrder)
	{
		if (deadHealerOrders.contains(healerOrder))
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

		Integer deathTick = lastTtkDeathTickByHealerOrder.get(healerOrder);
		return deathTick != null && client.getTickCount() > deathTick;
	}

	public HealerCodeStatus getCurrentCodeStatus(int healerOrder)
	{
		if (!isHealerRole()) return null;

		return codeManager.getCurrentStatus(currentWave, healerOrder, currentCallIndex, feedEvents);
	}

	public HealerCodeStatus getPreviousCodeStatus(int healerOrder)
	{
		if (!isHealerRole()) return null;

		return codeManager.getPreviousStatus(currentWave, healerOrder, currentCallIndex, feedEvents);
	}

	public HealerCodeStatus getDisplayCodeStatus(int healerOrder)
	{
		if (!isHealerRole()) return null;

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

		if (config.foodCountType() == BaHealerOrderConfig.FoodCountType.COUNT_UP)
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
		if (!isHealerRole()) return Collections.emptyMap();

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

	private String getHealerMenuSuffix(int healerOrder)
	{
		List<String> parts = new ArrayList<>();

		if (shouldShowMenuLabel()
				&& config.healerLabelStyle() != BaHealerOrderConfig.HealerLabelStyle.NONE)
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

	private void setRole(Role role)
	{
		if (currentRole == role || currentRole != null && isWaveActive()) return;

		currentRole = role;
		log.debug("BA role detected as {}", role);
	}

	private void detectRoleFromLoadedWidgets()
	{
		if (client.getVarbitValue(VarbitID.BARBASSAULT_AREAEXIT_PENDING) != 1 && !isWaveActive()) return;

		for (Role role : Role.values())
		{
			if (client.getWidget(role.groupId, 0) != null)
			{
				setRole(role);
				return;
			}
		}
	}

	private void updateBaPartySync()
	{
		if (!config.enableBaPartySync())
		{
			if (baSyncManagedParty)
			{
				leaveBaSyncParty("BA Party Sync disabled");
			}

			setBaPartySyncStatus("Off", null);
			return;
		}

		if (isWaveActive())
		{
			setBaPartySyncStatus(partyService.isInParty() ? "In wave" : "In wave - not connected", baPartySyncProgenitorName);
			return;
		}

		Optional<BaTeamRoster> teamRoster = getBaTeamRosterFromWidgetScanner();

		if (partyService.isInParty())
		{
			String currentPassphrase = partyService.getPartyPassphrase();

			if (baSyncManagedParty && baPartySyncPassphrase != null && !baPartySyncPassphrase.equals(currentPassphrase))
			{
				log.debug(
						"Current Party passphrase no longer matches managed BA sync party. Managed={}, current={}",
						baPartySyncPassphrase,
						currentPassphrase
				);

				baSyncManagedParty = false;
				baPartySyncPassphrase = null;
				setBaPartySyncStatus("Already in Party", null);
				return;
			}

			if (baSyncManagedParty && !teamRoster.isPresent())
			{
				leaveBaSyncParty("BA team widget no longer visible");
				return;
			}

			setBaPartySyncStatus(baSyncManagedParty ? "Connected" : "Already in Party", baPartySyncProgenitorName);
			return;
		}

		baSyncManagedParty = false;
		baPartySyncPassphrase = null;

		if (!teamRoster.isPresent())
		{
			baPartySyncProgenitorName = null;
			setBaPartySyncStatus("Waiting for BA team", null);
			return;
		}

		String progenitorName = teamRoster.get().getProgenitorName();

		if (progenitorName == null || progenitorName.isEmpty())
		{
			baPartySyncProgenitorName = null;
			setBaPartySyncStatus("Waiting for BA team", null);
			return;
		}

		baPartySyncProgenitorName = progenitorName;
		String passphrase = buildBaPartySyncPassphrase(progenitorName);

		setBaPartySyncStatus("Joining team party", progenitorName);

		try
		{
			partyService.changeParty(passphrase);
			baSyncManagedParty = true;
			baPartySyncPassphrase = passphrase;
			setBaPartySyncStatus("Connected", progenitorName);

			log.debug(
					"Joined or created BA sync party using progenitor {} and passphrase {}. Team roster: {}",
					progenitorName,
					passphrase,
					teamRoster.get().names
			);
		}
		catch (RuntimeException ex)
		{
			baSyncManagedParty = false;
			baPartySyncPassphrase = null;
			setBaPartySyncStatus("Join failed", progenitorName);
			log.debug("Failed to join BA sync party using passphrase {}", passphrase, ex);
		}
	}

	private String buildBaPartySyncPassphrase(String progenitorName)
	{
		return progenitorName + BA_SYNC_PASSPHRASE_SUFFIX;
	}

	private void leaveBaSyncParty(String reason)
	{
		if (!baSyncManagedParty)
		{
			baPartySyncPassphrase = null;
			baPartySyncProgenitorName = null;
			updateBaPartySyncPanelStatus();
			return;
		}

		try
		{
			if (partyService.isInParty())
			{
				log.debug("Leaving managed BA sync party. Reason: {}", reason);
				partyService.changeParty(null);
			}
		}
		catch (RuntimeException ex)
		{
			log.debug("Failed to leave managed BA sync party. Reason: {}", reason, ex);
		}
		finally
		{
			baSyncManagedParty = false;
			baPartySyncPassphrase = null;
			baPartySyncProgenitorName = null;
			setBaPartySyncStatus(config.enableBaPartySync() ? "Waiting for BA team" : "Off", null);
		}
	}

	private void setBaPartySyncStatus(String status, String progenitorName)
	{
		baPartySyncStatus = status;
		baPartySyncProgenitorName = progenitorName;
		updateBaPartySyncPanelStatus();
	}

	private void updateBaPartySyncPanelStatus()
	{
		if (panel == null)
		{
			return;
		}

		if (baPartySyncStatus != null
				&& baPartySyncStatus.equals(lastDisplayedBaPartySyncStatus)
				&& ((baPartySyncProgenitorName == null && lastDisplayedBaPartySyncProgenitorName == null)
				|| baPartySyncProgenitorName != null && baPartySyncProgenitorName.equals(lastDisplayedBaPartySyncProgenitorName)))
		{
			return;
		}

		lastDisplayedBaPartySyncStatus = baPartySyncStatus;
		lastDisplayedBaPartySyncProgenitorName = baPartySyncProgenitorName;
		panel.updatePartySyncStatus(baPartySyncStatus, baPartySyncProgenitorName);
	}

	private Optional<BaTeamRoster> getBaTeamRosterFromWidgetScanner()
	{
		Widget[] roots = client.getWidgetRoots();

		if (roots == null)
		{
			return Optional.empty();
		}

		List<WidgetTextCandidate> candidates = new ArrayList<>();
		List<String> contextTexts = new ArrayList<>();

		for (Widget root : roots)
		{
			collectVisibleWidgetTextCandidates(root, candidates, contextTexts);
		}

		Optional<BaTeamRoster> directBaTeamRoster = getBaTeamRosterFromKnownWidgetGroup(candidates, contextTexts);

		if (directBaTeamRoster.isPresent())
		{
			debugBaTeamWidgetScan(
					"Detected BA team roster from known widget group",
					candidates,
					contextTexts,
					directBaTeamRoster.get()
			);

			return directBaTeamRoster;
		}

		debugBaTeamWidgetScan(
				hasCurrentTeamContextText(contextTexts)
						? "Current team widget detected, but no visible BA team names found"
						: "No known BA team widget detected",
				candidates,
				contextTexts,
				null
		);

		return Optional.empty();
	}

	private boolean hasCurrentTeamContextText(List<String> contextTexts)
	{
		for (String text : contextTexts)
		{
			if ("current team".equalsIgnoreCase(text))
			{
				return true;
			}
		}

		return false;
	}

	private Optional<BaTeamRoster> getBaTeamRosterFromKnownWidgetGroup(List<WidgetTextCandidate> candidates, List<String> contextTexts)
	{
		boolean hasCurrentTeamText = false;

		for (String text : contextTexts)
		{
			if ("current team".equalsIgnoreCase(text))
			{
				hasCurrentTeamText = true;
				break;
			}
		}

		if (!hasCurrentTeamText)
		{
			return Optional.empty();
		}

		List<WidgetTextCandidate> teamCandidates = new ArrayList<>();
		LinkedHashSet<String> names = new LinkedHashSet<>();

		for (WidgetTextCandidate candidate : candidates)
		{
			if ((candidate.widgetId >>> 16) != BA_TEAM_GROUP_ID)
			{
				continue;
			}

			if (!isLikelyBaTeamPlayerName(candidate.text))
			{
				continue;
			}

			if (names.add(candidate.text))
			{
				teamCandidates.add(candidate);
			}
		}

		if (teamCandidates.isEmpty())
		{
			log.debug("BA party sync found Current team widget, but no group {} team member names were visible yet", BA_TEAM_GROUP_ID);
			return Optional.empty();
		}

		teamCandidates.sort(Comparator.comparingInt(candidate -> candidate.bounds.y));

		return Optional.of(new BaTeamRoster(new ArrayList<>(names), teamCandidates));
	}

	private boolean isIgnoredWidgetForBaTeamScan(Widget widget)
	{
		int groupId = getWidgetGroupId(widget);

		if (groupId == CHATBOX_GROUP_ID)
		{
			return true;
		}

		Rectangle bounds = widget.getBounds();

		if (bounds == null)
		{
			return false;
		}

		/*
		 * The chatbox can still expose nested text widgets that look like player names
		 * or role calls. Keep the scanner from treating lower-screen chat text as a
		 * BA team roster.
		 */
		return bounds.y >= 480;
	}

	private int getWidgetGroupId(Widget widget)
	{
		return widget.getId() >>> 16;
	}

	private void collectVisibleWidgetTextCandidates(Widget widget, List<WidgetTextCandidate> candidates, List<String> contextTexts)
	{
		if (widget == null || widget.isHidden() || isIgnoredWidgetForBaTeamScan(widget))
		{
			return;
		}

		collectWidgetTextCandidate(widget, widget.getText(), candidates, contextTexts);
		collectWidgetTextCandidate(widget, widget.getName(), candidates, contextTexts);

		Widget[] dynamicChildren = widget.getDynamicChildren();
		if (dynamicChildren != null)
		{
			for (Widget child : dynamicChildren)
			{
				collectVisibleWidgetTextCandidates(child, candidates, contextTexts);
			}
		}

		Widget[] staticChildren = widget.getStaticChildren();
		if (staticChildren != null)
		{
			for (Widget child : staticChildren)
			{
				collectVisibleWidgetTextCandidates(child, candidates, contextTexts);
			}
		}

		Widget[] nestedChildren = widget.getNestedChildren();
		if (nestedChildren != null)
		{
			for (Widget child : nestedChildren)
			{
				collectVisibleWidgetTextCandidates(child, candidates, contextTexts);
			}
		}
	}

	private void collectWidgetTextCandidate(Widget widget, String rawText, List<WidgetTextCandidate> candidates, List<String> contextTexts)
	{
		String text = cleanWidgetText(rawText);

		if (text.isEmpty())
		{
			return;
		}

		contextTexts.add(text);

		if (!isLikelyPlayerName(text))
		{
			return;
		}

		Rectangle bounds = widget.getBounds();

		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}

		candidates.add(new WidgetTextCandidate(text, bounds, widget.getId()));
	}

	private Optional<BaTeamRoster> findBestBaTeamRoster(List<WidgetTextCandidate> candidates)
	{
		List<WidgetTextCandidate> sortedCandidates = new ArrayList<>(candidates);
		sortedCandidates.sort(
				Comparator.comparingInt((WidgetTextCandidate candidate) -> candidate.bounds.x)
						.thenComparingInt(candidate -> candidate.bounds.y)
		);

		BaTeamRoster bestRoster = null;

		for (WidgetTextCandidate anchor : sortedCandidates)
		{
			LinkedHashSet<String> names = new LinkedHashSet<>();
			List<WidgetTextCandidate> group = new ArrayList<>();

			for (WidgetTextCandidate candidate : sortedCandidates)
			{
				if (Math.abs(candidate.bounds.x - anchor.bounds.x) > 80)
				{
					continue;
				}

				if (candidate.bounds.y < anchor.bounds.y || candidate.bounds.y > anchor.bounds.y + 180)
				{
					continue;
				}

				if (names.add(candidate.text))
				{
					group.add(candidate);
				}
			}

			if (group.size() < 2 || group.size() > 5)
			{
				continue;
			}

			group.sort(Comparator.comparingInt(candidate -> candidate.bounds.y));

			BaTeamRoster roster = new BaTeamRoster(new ArrayList<>(names), group);

			if (bestRoster == null || roster.names.size() > bestRoster.names.size())
			{
				bestRoster = roster;
			}
		}

		return Optional.ofNullable(bestRoster);
	}

	private boolean hasBaTeamContextText(List<String> contextTexts)
	{
		for (String text : contextTexts)
		{
			String lower = text.toLowerCase(Locale.ROOT);

			if (lower.contains("current team")
					|| lower.contains("leader:")
					|| lower.contains("player 1:")
					|| lower.contains("player 2:")
					|| lower.contains("player 3:")
					|| lower.contains("player 4:")
					|| lower.contains("barbarian assault")
					|| lower.contains("wave")
					|| lower.contains("attacker")
					|| lower.contains("collector")
					|| lower.contains("defender")
					|| lower.contains("healer")
					|| lower.contains("penance"))
			{
				return true;
			}
		}

		return false;
	}

	private boolean isLikelyPlayerName(String text)
	{
		return isLikelyBaTeamPlayerName(text);
	}

	private boolean isLikelyBaTeamPlayerName(String text)
	{
		if (text == null || text.length() < 1 || text.length() > 12)
		{
			return false;
		}

		String lower = text.toLowerCase(Locale.ROOT);

		if (lower.contains("current team")
				|| lower.startsWith("leader")
				|| lower.startsWith("player ")
				|| lower.contains("-----")
				|| lower.contains("wave")
				|| lower.contains("attacker")
				|| lower.contains("collector")
				|| lower.contains("defender")
				|| lower.contains("healer")
				|| lower.contains("penance")
				|| lower.contains("points")
				|| lower.contains("role")
				|| lower.contains("level"))
		{
			return false;
		}

		if (!text.matches("[A-Za-z0-9 _\\-]+") || !text.matches(".*[A-Za-z].*"))
		{
			return false;
		}

		return !text.matches("\\d+");
	}
	private String cleanWidgetText(String text)
	{
		if (text == null)
		{
			return "";
		}

		return Text.removeTags(text)
				.replace('\u00A0', ' ')
				.replaceAll("\\s+", " ")
				.trim();
	}

	private void debugBaTeamWidgetScan(String message, List<WidgetTextCandidate> candidates, List<String> contextTexts, BaTeamRoster roster)
	{
		if (!config.enableBaPartySync())
		{
			return;
		}

		int tick = client.getTickCount();

		if (tick - lastBaTeamWidgetDebugTick < BA_TEAM_WIDGET_DEBUG_INTERVAL_TICKS)
		{
			return;
		}

		lastBaTeamWidgetDebugTick = tick;

		List<String> candidateTexts = new ArrayList<>();

		for (WidgetTextCandidate candidate : candidates)
		{
			candidateTexts.add(candidate.text + "@" + candidate.bounds.x + "," + candidate.bounds.y + "#" + candidate.widgetId + "/g" + (candidate.widgetId >>> 16));
		}

		List<String> interestingContextTexts = new ArrayList<>();

		for (String text : contextTexts)
		{
			String lower = text.toLowerCase(Locale.ROOT);

			if (lower.contains("current team")
					|| lower.contains("leader:")
					|| lower.contains("player 1:")
					|| lower.contains("player 2:")
					|| lower.contains("player 3:")
					|| lower.contains("player 4:")
					|| lower.contains("barbarian")
					|| lower.contains("wave")
					|| lower.contains("attacker")
					|| lower.contains("collector")
					|| lower.contains("defender")
					|| lower.contains("healer")
					|| lower.contains("penance"))
			{
				interestingContextTexts.add(text);
			}
		}

		log.debug(
				"BA party sync widget scan: {}. Roster: {}. Candidates: {}. Context: {}",
				message,
				roster == null ? null : roster.names,
				candidateTexts,
				interestingContextTexts
		);
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
		BaHealerOrderConfig.HideDeadNpcMode mode = config.hideDeadNpcs();

		if (mode == BaHealerOrderConfig.HideDeadNpcMode.NONE
				|| !isWaveActive())
		{
			return false;
		}

		if (mode == BaHealerOrderConfig.HideDeadNpcMode.HEALERS_ONLY)
		{
			return isDeadPenanceHealer(npc);
		}

		if (mode == BaHealerOrderConfig.HideDeadNpcMode.ALL_BA_NPCS)
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
			lastTtkDeathTickByHealerOrder.put(healerOrder, client.getTickCount());
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
				&& !config.moveTakeMeatUp()
				&& !config.deprioritizeOtherDispensers()) return;

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

			if (config.deprioritizeOtherDispensers() && isOtherDispenserTarget(targetText))
			{
				changed = true;
				continue;
			}

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

	private boolean isOtherDispenserTarget(String targetText)
	{
		return targetText.contains(ATTACKER_ITEM_MACHINE_NAME.toLowerCase(Locale.ROOT))
				|| targetText.contains(DEFENDER_ITEM_MACHINE_NAME.toLowerCase(Locale.ROOT))
				|| targetText.contains(COLLECTOR_CONVERTER_NAME.toLowerCase(Locale.ROOT));
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
				if (item == null || item.getId() <= 0 || !isPoisonedFoodItem(item.getId()))
				{
					continue;
				}

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

			if (previousCount == null)
			{
				continue;
			}

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
		if (itemContainer == null)
		{
			return;
		}

		Item[] items = itemContainer.getItems();

		if (items == null)
		{
			return;
		}

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
			if (attempt.foodItemId != consumedItemId)
			{
				continue;
			}

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
		if (client.getLocalPlayer() == null)
		{
			return NO_ATTEMPT_DISTANCE;
		}

		NPC npc = attempt.npc;

		if (npc == null)
		{
			npc = getVisibleHealerByOrder(attempt.healerOrder);
		}

		if (npc == null)
		{
			return NO_ATTEMPT_DISTANCE;
		}

		return client.getLocalPlayer().getWorldLocation().distanceTo(npc.getWorldLocation());
	}

	private void recordConsumedFoodForPendingAttempt(PendingFeedAttempt attempt)
	{
		foodFedByNpcIndex.merge(attempt.npcIndex, 1, Integer::sum);
		ttkTracker.onFoodConsumedForHealer(attempt.npcIndex, client.getTickCount());

		Integer currentOrder = healerOrderByNpcIndex.get(attempt.npcIndex);
		int totalFoodFed = foodFedByNpcIndex.getOrDefault(attempt.npcIndex, 0);

		if (currentOrder != null)
		{
			feedEvents.add(new FeedEvent(currentOrder, Math.round(getCurrentWaveElapsedSeconds()), currentCallIndex));
		}

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

			if (!isPendingFeedAttemptStale(attempt))
			{
				continue;
			}

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
		currentRole = null;
	}

	private void resetWaveTrackedState()
	{
		visibleHealers.clear();
		healerIndexesSeenThisWave.clear();
		healerOrderByNpcIndex.clear();
		healerIndexBase = -1;
		deadHealerOrders.clear();
		foodFedByNpcIndex.clear();
		lastTtkDeathTickByHealerOrder.clear();
		lastPoisonedFoodCountByItemId.clear();
		feedEvents.clear();
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
		baSyncManagedParty = false;
		baPartySyncPassphrase = null;
		baPartySyncProgenitorName = null;
		setBaPartySyncStatus(config.enableBaPartySync() ? "Waiting for BA team" : "Off", null);
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

	private static class BaTeamRoster
	{
		private final List<String> names;
		private final List<WidgetTextCandidate> candidates;

		private BaTeamRoster(List<String> names, List<WidgetTextCandidate> candidates)
		{
			this.names = names;
			this.candidates = candidates;
		}

		private String getProgenitorName()
		{
			if (candidates == null || candidates.isEmpty())
			{
				return names.isEmpty() ? null : names.get(0);
			}

			WidgetTextCandidate topCandidate = candidates.get(0);

			for (WidgetTextCandidate candidate : candidates)
			{
				if (candidate.bounds.y < topCandidate.bounds.y)
				{
					topCandidate = candidate;
				}
			}

			return topCandidate.text;
		}
	}

	private static class WidgetTextCandidate
	{
		private final String text;
		private final Rectangle bounds;
		private final int widgetId;

		private WidgetTextCandidate(String text, Rectangle bounds, int widgetId)
		{
			this.text = text;
			this.bounds = bounds;
			this.widgetId = widgetId;
		}
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