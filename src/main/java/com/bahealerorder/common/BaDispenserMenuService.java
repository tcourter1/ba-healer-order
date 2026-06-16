package com.bahealerorder.common;

import com.bahealerorder.BaUtilitiesConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.client.util.Text;

@Singleton
public class BaDispenserMenuService
{
	private final Client client;
	private final BaUtilitiesConfig config;
	private final BaRoleDetector roleDetector;

	@Inject
	private BaDispenserMenuService(Client client, BaUtilitiesConfig config, BaRoleDetector roleDetector)
	{
		this.client = client;
		this.config = config;
		this.roleDetector = roleDetector;
	}

	public void apply()
	{
		BaRole role = roleDetector.getCurrentRole();

		if (!config.deprioritizeOtherDispensers() || role == null) return;

		MenuEntry[] menuEntries = client.getMenu().getMenuEntries();

		if (menuEntries.length == 0) return;

		List<MenuEntry> filteredEntries = new ArrayList<>(menuEntries.length);
		boolean changed = false;

		for (MenuEntry entry : menuEntries)
		{
			if (isOtherRoleDispenser(entry, role))
			{
				changed = true;
				continue;
			}

			filteredEntries.add(entry);
		}

		if (changed)
		{
			client.getMenu().setMenuEntries(filteredEntries.toArray(new MenuEntry[0]));
		}
	}

	private boolean isOtherRoleDispenser(MenuEntry entry, BaRole role)
	{
		String target = entry.getTarget();

		if (target == null) return false;

		String targetText = Text.removeTags(target).toLowerCase(Locale.ROOT);

		for (BaRole dispenserRole : BaRole.values())
		{
			if (dispenserRole != role && targetText.contains(dispenserRole.getDispenserName().toLowerCase(Locale.ROOT)))
			{
				return true;
			}
		}

		return false;
	}
}
