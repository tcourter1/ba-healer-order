package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesConfig;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.OverlayManager;

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
		}
	}

	public void onNpcDespawned(NpcDespawned event)
	{
		penanceQueens.remove(event.getNpc());
	}

	public void onGameTick()
	{
		if (!isWave10Active()) penanceQueens.clear();
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();

		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			resetState();
		}
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
		return isOmegaEggLoaded() ? OMEGA_DUPLICATE_ITEM_LOADED_COLOR : OMEGA_DUPLICATE_ITEM_PENDING_COLOR;
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
		if (devQueenSpawned) return true;

		if (!penanceQueens.isEmpty()) return true;

		WorldView worldView = client.getTopLevelWorldView();
		return worldView != null && worldView.npcs().stream().anyMatch(this::isPenanceQueen);
	}

	private boolean isPenanceQueen(NPC npc)
	{
		return npc != null && npc.getId() == NpcID.BARBASSAULT_PEN_QUEEN_NEW;
	}

	private boolean isOmegaEggLoaded()
	{
		if (devOmegaLoaded != null) return devOmegaLoaded;

		return client.getVarbitValue(VarbitID.BARBASSAULT_EGGCOUNT_OMEGA) > 0;
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

}
