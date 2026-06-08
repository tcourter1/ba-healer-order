package com.bahealerorder.healer.ttk;

import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.client.game.NPCManager;

public class HealerHealthEstimator
{
	private final NPCManager npcManager;

	@Inject
	public HealerHealthEstimator(NPCManager npcManager)
	{
		this.npcManager = npcManager;
	}

	public Optional<ObservedHealerHp> estimate(NPC npc)
	{
		if (npc == null)
		{
			return Optional.empty();
		}

		int healthRatio = npc.getHealthRatio();
		int healthScale = npc.getHealthScale();
		Integer maxHealth = npcManager.getHealth(npc.getId());

		if (healthRatio < 0 || healthScale <= 0 || maxHealth == null)
		{
			return Optional.empty();
		}

		return Optional.of(new ObservedHealerHp(calculateHealth(healthRatio, healthScale, maxHealth), maxHealth));
	}

	static int calculateHealth(int healthRatio, int healthScale, int maxHealth)
	{
		if (healthRatio <= 0)
		{
			return 0;
		}

		int minHealth = 1;
		int estimatedMaxHealth;

		if (healthScale > 1)
		{
			if (healthRatio > 1)
			{
				minHealth = (maxHealth * (healthRatio - 1) + healthScale - 2) / (healthScale - 1);
			}

			estimatedMaxHealth = (maxHealth * healthRatio - 1) / (healthScale - 1);
			estimatedMaxHealth = Math.min(estimatedMaxHealth, maxHealth);
		}
		else
		{
			estimatedMaxHealth = maxHealth;
		}

		return (minHealth + estimatedMaxHealth + 1) / 2;
	}
}
