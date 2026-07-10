package com.bahealerorder.defender;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.common.BaNpcIds;
import com.bahealerorder.common.BaOverviewNpcType;
import com.bahealerorder.common.BaRole;
import com.bahealerorder.common.BaRoleDetector;
import com.bahealerorder.common.BaWaveLifecycleService;
import com.bahealerorder.common.BaWaveOverviewService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Singleton
public class DefenderController
{
	private final Client client;
	private final BaUtilitiesConfig config;
	private final ItemManager itemManager;
	private final OverlayManager overlayManager;
	private final BaWaveOverviewService waveOverviewService;
	private final BaWaveLifecycleService waveLifecycleService;
	private final BaRoleDetector roleDetector;
	private final DefenderGroundItemOverlay groundItemOverlay;
	private final Map<NPC, BaOverviewNpcType> visibleRunnerNpcs = new HashMap<>();
	private final List<DefenderGroundItem> groundItems = new ArrayList<>();

	@Inject
	private DefenderController(
			Client client,
			BaUtilitiesConfig config,
			ItemManager itemManager,
			OverlayManager overlayManager,
			BaWaveOverviewService waveOverviewService,
			BaWaveLifecycleService waveLifecycleService,
			BaRoleDetector roleDetector,
			DefenderGroundItemOverlay groundItemOverlay)
	{
		this.client = client;
		this.config = config;
		this.itemManager = itemManager;
		this.overlayManager = overlayManager;
		this.waveOverviewService = waveOverviewService;
		this.waveLifecycleService = waveLifecycleService;
		this.roleDetector = roleDetector;
		this.groundItemOverlay = groundItemOverlay;
	}

	public void startUp()
	{
		resetState();
		groundItemOverlay.setController(this);
		overlayManager.add(groundItemOverlay);
	}

	public void shutDown()
	{
		overlayManager.remove(groundItemOverlay);
		resetState();
	}

	public void onNpcSpawned(NpcSpawned event)
	{
		if (!waveLifecycleService.isWaveActive()) return;

		NPC npc = event.getNpc();
		if (BaNpcIds.getOverviewType(npc) != BaOverviewNpcType.RUNNER) return;

		visibleRunnerNpcs.put(npc, BaOverviewNpcType.RUNNER);
		waveOverviewService.recordSpawn(BaOverviewNpcType.RUNNER, npc.getIndex());
	}

	public void onNpcDespawned(NpcDespawned event)
	{
		visibleRunnerNpcs.remove(event.getNpc());
	}

	public void onActorDeath(ActorDeath event)
	{
		recordRunnerDeath(event.getActor());
	}

	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();

		if (actor != null && actor.getAnimation() == AnimationID.BARBASSAULT_PENANCE_RUNNER_DEATH)
		{
			recordRunnerDeath(actor);
		}
	}

	public void onGameTick(GameTick event)
	{
		if (!waveLifecycleService.isWaveActive()) return;

		for (Map.Entry<NPC, BaOverviewNpcType> entry : visibleRunnerNpcs.entrySet())
		{
			NPC npc = entry.getKey();
			if (npc != null && npc.getHealthRatio() == 0)
			{
				waveOverviewService.recordDeath(entry.getValue(), npc.getIndex());
			}
		}
	}

	public void onWaveStarted()
	{
		resetWaveState();
		refreshGroundItems();
	}

	public void onItemSpawned(ItemSpawned event)
	{
		if (!waveLifecycleService.isWaveActive()) return;

		DefenderGroundItem groundItem = createGroundItem(event.getTile(), event.getItem());

		if (groundItem != null)
		{
			groundItems.add(groundItem);
		}
	}

	public void onItemDespawned(ItemDespawned event)
	{
		if (!waveLifecycleService.isWaveActive()) return;

		removeGroundItem(event.getTile(), event.getItem());
	}

	public void onWaveEnded()
	{
		resetWaveState();
	}

	public boolean shouldShowGroundItemOverlay()
	{
		return isDefenderRole()
				&& waveLifecycleService.isWaveActive()
				&& !getHighlightedGroundItems().isEmpty();
	}

	public List<DefenderGroundItem> getHighlightedGroundItems()
	{
		if (!isDefenderRole() || !waveLifecycleService.isWaveActive()) return Collections.emptyList();

		boolean highlightHammer = config.highlightDefenderHammer() && !hasHammer();

		if (!highlightHammer) return Collections.emptyList();

		List<DefenderGroundItem> highlighted = new ArrayList<>();

		for (DefenderGroundItem groundItem : groundItems)
		{
			if (groundItem.getType() == DefenderGroundItem.Type.HAMMER && highlightHammer)
			{
				highlighted.add(groundItem);
			}
		}

		return highlighted;
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();

		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.HOPPING)
		{
			resetState();
		}
	}

	private void recordRunnerDeath(Actor actor)
	{
		if (!waveLifecycleService.isWaveActive() || !(actor instanceof NPC)) return;

		NPC npc = (NPC) actor;
		if (visibleRunnerNpcs.containsKey(npc))
		{
			waveOverviewService.recordDeath(BaOverviewNpcType.RUNNER, npc.getIndex());
		}
	}

	private void resetWaveState()
	{
		visibleRunnerNpcs.clear();
		groundItems.clear();
	}

	private void resetState()
	{
		resetWaveState();
	}

	public boolean isDefenderRole()
	{
		return roleDetector.isRole(BaRole.DEFENDER);
	}

	private boolean hasHammer()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (isHammer(item)) return true;
			}
		}

		return isEquippedImcandoHammer(KitType.WEAPON) || isEquippedImcandoHammer(KitType.SHIELD);
	}

	private boolean isHammer(Item item)
	{
		if (item == null || item.getId() <= 0) return false;

		return DefenderInventory.isHammer(item.getId(), getItemName(item.getId()));
	}

	private boolean isEquippedImcandoHammer(KitType kitType)
	{
		PlayerComposition composition = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getPlayerComposition();

		if (composition == null) return false;

		int itemId = composition.getEquipmentId(kitType);

		return DefenderInventory.isImcandoHammer(itemId, getItemName(itemId));
	}

	private DefenderGroundItem createGroundItem(Tile tile, TileItem item)
	{
		if (tile == null || item == null || item.getId() <= 0) return null;

		String name = getItemName(item.getId());

		if (DefenderInventory.isHammer(item.getId(), name)) return new DefenderGroundItem(tile, item, DefenderGroundItem.Type.HAMMER);

		return null;
	}

	private String getItemName(int itemId)
	{
		try
		{
			return Text.removeTags(itemManager.getItemComposition(itemId).getName());
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private void removeGroundItem(Tile tile, TileItem item)
	{
		if (tile == null || item == null) return;

		for (int i = 0; i < groundItems.size(); i++)
		{
			DefenderGroundItem groundItem = groundItems.get(i);

			if (groundItem.getItem() == item
					|| groundItem.getItem().getId() == item.getId()
					&& sameWorldTile(groundItem.getTile(), tile))
			{
				groundItems.remove(i);
				return;
			}
		}
	}

	private boolean sameWorldTile(Tile first, Tile second)
	{
		return first != null
				&& second != null
				&& first.getWorldLocation() != null
				&& first.getWorldLocation().equals(second.getWorldLocation());
	}

	private void refreshGroundItems()
	{
		groundItems.clear();

		if (client.getTopLevelWorldView() == null || client.getTopLevelWorldView().getScene() == null) return;

		Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();

		for (Tile[][] plane : tiles)
		{
			if (plane == null) continue;

			for (Tile[] row : plane)
			{
				if (row == null) continue;

				for (Tile tile : row)
				{
					if (tile == null || tile.getGroundItems() == null) continue;

					for (TileItem item : tile.getGroundItems())
					{
						DefenderGroundItem groundItem = createGroundItem(tile, item);

						if (groundItem != null)
						{
							groundItems.add(groundItem);
						}
					}
				}
			}
		}
	}
}
