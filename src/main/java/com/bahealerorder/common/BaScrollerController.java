package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesConfig;
import java.awt.Color;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
public class BaScrollerController
{
	private static final Color OMEGA_DUPLICATE_ITEM_PENDING_COLOR = Color.RED;
	private static final Color OMEGA_DUPLICATE_ITEM_LOADED_COLOR = Color.GREEN;
	private static final int QUEEN_WAVE = 10;

	private final Client client;
	private final BaUtilitiesConfig config;
	private final BaPartySyncService partySyncService;
	private final BaWaveLifecycleService waveLifecycleService;
	private final OverlayManager overlayManager;
	private final BaScrollerOverlay overlay;
	private final BaScrollerInventoryOverlay inventoryOverlay;

	private final Set<NPC> penanceQueens = new HashSet<>();
	private boolean devLocalPlayerScroller;
	private boolean devQueenSpawned;
	private Boolean devOmegaLoaded;

	@Inject
	private BaScrollerController(
			Client client,
			BaUtilitiesConfig config,
			BaPartySyncService partySyncService,
			BaWaveLifecycleService waveLifecycleService,
			OverlayManager overlayManager,
			BaScrollerOverlay overlay,
			BaScrollerInventoryOverlay inventoryOverlay)
	{
		this.client = client;
		this.config = config;
		this.partySyncService = partySyncService;
		this.waveLifecycleService = waveLifecycleService;
		this.overlayManager = overlayManager;
		this.overlay = overlay;
		this.inventoryOverlay = inventoryOverlay;
	}

	public void startUp()
	{
		overlay.setController(this);
		inventoryOverlay.setController(this);
		overlayManager.add(overlay);
		overlayManager.add(inventoryOverlay);
	}

	public void shutDown()
	{
		resetState();
		overlayManager.remove(inventoryOverlay);
		overlayManager.remove(overlay);
	}

	public void setDevOverrides(boolean localPlayerScroller, boolean queenSpawned, boolean omegaLoaded)
	{
		devLocalPlayerScroller = localPlayerScroller;
		devQueenSpawned = queenSpawned;
		devOmegaLoaded = omegaLoaded;

		log.debug(
				"BA scroller dev overrides: scroller={}, queenSpawned={}, omegaLoaded={}",
				devLocalPlayerScroller,
				devQueenSpawned,
				devOmegaLoaded
		);
	}

	public void onWaveEnded()
	{
		resetState();
	}

	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (isPenanceQueen(npc))
		{
			penanceQueens.add(npc);
			log.debug(
					"BA scroller omega queen spawned: id={}, location={}, wave10Active={}, scroller={}",
					npc.getId(),
					formatWorldPoint(npc.getWorldLocation()),
					isWave10Active(),
					partySyncService.isLocalPlayerBaTeamLeader()
			);
		}
	}

	public void onNpcDespawned(NpcDespawned event)
	{
		penanceQueens.remove(event.getNpc());
	}

	public void onGameTick(GameTick event)
	{
		if (!isWave10Active())
		{
			penanceQueens.clear();
			return;
		}
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();

		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			resetState();
		}
	}

	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() != VarbitID.BARBASSAULT_EGGCOUNT_OMEGA || !shouldTrackOmegaDupeItems())
		{
			return;
		}

		log.debug(
				"BA scroller omega varbit changed: varbit={}, eventValue={}, clientValue={}, queenSpawned={}, tick={}",
				event.getVarbitId(),
				event.getValue(),
				getOmegaEggCount(),
				hasPenanceQueenSpawned(),
				client.getTickCount()
		);
	}

	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event == null || !shouldHighlightCurrentRoomLadder() || !isLadderClick(event))
		{
			return;
		}

		WorldPoint worldPoint = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getWorldLocation();

		log.debug(
				"BA scroller ladder click debug: option='{}', target='{}', id={}, menuAction={}, param0={}, param1={}, playerWorld={}, scan={}",
				event.getMenuOption(),
				event.getMenuTarget(),
				event.getId(),
				event.getMenuAction(),
				event.getParam0(),
				event.getParam1(),
				formatWorldPoint(worldPoint),
				overlay.getDebugSummary()
		);
	}

	boolean shouldHighlightCurrentRoomLadder()
	{
		return config.highlightScrollerLadder() && isLocalPlayerScroller();
	}

	boolean shouldHighlightOmegaDupeItems()
	{
		return shouldTrackOmegaDupeItems() && hasPenanceQueenSpawned();
	}

	Color getOmegaDupeItemHighlightColor()
	{
		if (isOmegaEggLoaded())
		{
			return OMEGA_DUPLICATE_ITEM_LOADED_COLOR;
		}

		return OMEGA_DUPLICATE_ITEM_PENDING_COLOR;
	}

	private boolean shouldTrackOmegaDupeItems()
	{
		return config.highlightOmegaDupeItems()
				&& isLocalPlayerScroller()
				&& isWave10Active();
	}

	private boolean isWave10Active()
	{
		return waveLifecycleService.isWaveActive() && waveLifecycleService.getWave() == QUEEN_WAVE;
	}

	private boolean hasPenanceQueenSpawned()
	{
		if (devQueenSpawned)
		{
			return true;
		}

		if (!penanceQueens.isEmpty())
		{
			return true;
		}

		WorldView worldView = client.getTopLevelWorldView();
		return worldView != null && worldView.npcs().stream().anyMatch(this::isPenanceQueen);
	}

	private boolean isPenanceQueen(NPC npc)
	{
		return npc != null && npc.getId() == NpcID.BARBASSAULT_PEN_QUEEN_NEW;
	}

	private boolean isOmegaEggLoaded()
	{
		if (devOmegaLoaded != null)
		{
			return devOmegaLoaded;
		}

		return getOmegaEggCount() > 0;
	}

	private int getOmegaEggCount()
	{
		return client.getVarbitValue(VarbitID.BARBASSAULT_EGGCOUNT_OMEGA);
	}

	private boolean isLocalPlayerScroller()
	{
		return devLocalPlayerScroller || partySyncService.isLocalPlayerBaTeamLeader();
	}

	private void resetState()
	{
		penanceQueens.clear();
		devLocalPlayerScroller = false;
		devQueenSpawned = false;
		devOmegaLoaded = null;
	}

	private boolean isLadderClick(MenuOptionClicked event)
	{
		String target = Text.removeTags(event.getMenuTarget() == null ? "" : event.getMenuTarget()).toLowerCase(Locale.ROOT);
		return target.contains("ladder");
	}

	private String formatWorldPoint(WorldPoint worldPoint)
	{
		if (worldPoint == null)
		{
			return "null";
		}

		return "(" + worldPoint.getX() + ", " + worldPoint.getY() + ", " + worldPoint.getPlane() + ")";
	}
}
