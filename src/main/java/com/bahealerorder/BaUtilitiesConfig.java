package com.bahealerorder;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(BaUtilitiesConfig.GROUP_NAME)
public interface BaUtilitiesConfig extends Config
{
	String GROUP_NAME = "bahealerorder";

	@ConfigSection(
			name = "General",
			description = "General settings and settings that can apply to any BA role",
			position = 1
	)
	String generalSection = "general";

	@ConfigSection(
			name = "Healer",
			description = "Settings for healer highlighting, labels, food counts, and dispenser options",
			position = 2
	)
	String healerSection = "healer";

	@ConfigSection(
			name = "Attacker",
			description = "Utility helpers for the BA attacker role",
			position = 3
	)
	String attackerSection = "attacker";

	enum HighlightStyle
	{
		NONE("None"),
		TILE("Tile"),
		TRUE_TILE("True Tile"),
		HULL("Hull");

		private final String name;

		HighlightStyle(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum HealerLabelStyle
	{
		NONE("None"),
		SPAWN_ORDER("Spawn Order"),
		TIME_BASED_NUMBERING("Time-Based");

		private final String name;

		HealerLabelStyle(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum HideDeadNpcMode
	{
		NONE("None"),
		HEALERS_ONLY("Healers Only"),
		ALL_BA_NPCS("All BA NPCs");

		private final String name;

		HideDeadNpcMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum FoodCountType
	{
		COUNT_UP("Count Up"),
		COUNT_DOWN("Count Down");

		private final String name;

		FoodCountType(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum FoodPanelStyle
	{
		NONE("None"),
		ROWS("Rows"),
		COLUMNS("Columns"),
		SIMPLIFIED("Simplified"),
		CODE_ONLY("Code Only");

		private final String name;

		FoodPanelStyle(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum HealerTtkDisplayMode
	{
		OFF("Off"),
		TICKS("Ticks"),
		SECONDS("Seconds"),
		WAVE_TIME("Wave Time");

		private final String name;

		HealerTtkDisplayMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum DispenserOptions
	{
		NONE("None", false, false),
		REMOVE_TAKE_VIAL("Hide Vial", true, false),
		MOVE_TAKE_MEAT_UP("Move Meat Up", false, true),
		BOTH("Both", true, true);

		private final String name;
		private final boolean removeTakeVial;
		private final boolean moveTakeMeatUp;

		DispenserOptions(String name, boolean removeTakeVial, boolean moveTakeMeatUp)
		{
			this.name = name;
			this.removeTakeVial = removeTakeVial;
			this.moveTakeMeatUp = moveTakeMeatUp;
		}

		public boolean removeTakeVial()
		{
			return removeTakeVial;
		}

		public boolean moveTakeMeatUp()
		{
			return moveTakeMeatUp;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	@ConfigItem(
			keyName = "showAttackerSpawnCountOverlay",
			name = "Show Spawn Count Overlay",
			description = "Shows spawned/total counts above the Ranger and Fighter caves while playing Attacker",
			section = attackerSection,
			position = 1
	)
	default boolean showAttackerSpawnCountOverlay()
	{
		return true;
	}

	@ConfigItem(
			keyName = "attackerSpawnCountTextColor",
			name = "Spawn Count Text Color",
			description = "Color of the Attacker cave spawn count overlay text",
			section = attackerSection,
			position = 2
	)
	default Color attackerSpawnCountTextColor()
	{
		return Color.YELLOW;
	}

	@Range(
			min = 10,
			max = 40
	)
	@ConfigItem(
			keyName = "attackerSpawnCountTextSize",
			name = "Spawn Count Text Size",
			description = "Font size of the Attacker cave spawn count overlay text",
			section = attackerSection,
			position = 3
	)
	default int attackerSpawnCountTextSize()
	{
		return 18;
	}

	@Range(
			min = -10,
			max = 10
	)
	@ConfigItem(
			keyName = "attackerSpawnCountHorizontalOffset",
			name = "Spawn Count Horizontal Offset",
			description = "Moves the Attacker cave spawn count overlay left or right in scene tiles",
			section = attackerSection,
			position = 4
	)
	default int attackerSpawnCountHorizontalOffset()
	{
		return 0;
	}

	@Range(
			min = 0,
			max = 200
	)
	@ConfigItem(
			keyName = "attackerSpawnCountHeightOffset",
			name = "Spawn Count Height Offset",
			description = "Moves the Attacker cave spawn count overlay higher or lower above the cave",
			section = attackerSection,
			position = 5
	)
	default int attackerSpawnCountHeightOffset()
	{
		return 200;
	}

	@ConfigItem(
			keyName = "highlightStyle",
			name = "Highlight Style",
			description = "Choose how tracked Penance Healers are highlighted",
			section = healerSection,
			position = 1
	)
	default HighlightStyle highlightStyle()
	{
		return HighlightStyle.HULL;
	}

	@Alpha
	@ConfigItem(
			keyName = "hullColor",
			name = "Highlight Color",
			description = "Color used for Penance Healer highlights and labels",
			section = healerSection,
			position = 2
	)
	default Color hullColor()
	{
		return new Color(0, 200, 200);
	}

	@ConfigItem(
			keyName = "healerLabelStyle",
			name = "Label Style",
			description = "Choose whether healer labels are hidden, spawn order, or time-based BA labels",
			section = healerSection,
			position = 3
	)
	default HealerLabelStyle healerLabelStyle()
	{
		return HealerLabelStyle.SPAWN_ORDER;
	}

	@ConfigItem(
			keyName = "showLabelsAsHealerOnly",
			name = "Show Labels as Healer Only",
			description = "Only shows Penance Healer labels while playing the Healer role",
			section = healerSection,
			position = 4
	)
	default boolean showLabelsAsHealerOnly()
	{
		return true;
	}

	@ConfigItem(
			keyName = "hideDeadNpcs",
			name = "Hide Dead NPCs",
			description = "Hides selected BA NPCs immediately when they begin dying instead of waiting for their death animation to finish",
			section = generalSection,
			position = 4
	)
	default HideDeadNpcMode hideDeadNpcs()
	{
		return HideDeadNpcMode.NONE;
	}

	@ConfigItem(
			keyName = "spreadStackedLabels",
			name = "Spread Stacked Labels",
			description = "Horizontally separates healer labels when multiple Penance Healers occupy the same tile",
			section = healerSection,
			position = 6
	)
	default boolean spreadStackedLabels()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showMenuLabel",
			name = "Show Menu Label",
			description = "Adds the tracked healer label and current healer code next to Penance Healers in the right-click menu",
			section = healerSection,
			position = 7
	)
	default boolean showMenuLabel()
	{
		return true;
	}

	@ConfigItem(
			keyName = "healerFoodOnly",
			name = "Use-Food on Healers Only",
			description = "When poisoned food is selected, only Penance Healer Use entries remain clickable",
			section = healerSection,
			position = 8
	)
	default boolean healerFoodOnly()
	{
		return true;
	}

	@ConfigItem(
			keyName = "highlightCalledDispenserFood",
			name = "Dispenser Food Highlight",
			description = "Highlights the correct Take option on the healer dispenser for the current food call",
			section = healerSection,
			position = 9
	)
	default boolean highlightCalledDispenserFood()
	{
		return false;
	}

	@ConfigItem(
			keyName = "dispenserOptions",
			name = "Dispenser Options",
			description = "Optional healer dispenser menu cleanup",
			section = healerSection,
			position = 10
	)
	default DispenserOptions dispenserOptions()
	{
		return DispenserOptions.NONE;
	}

	@ConfigItem(
			keyName = "foodPanelStyle",
			name = "Food Panel Style",
			description = "Choose how the food panel displays tracked healer food",
			section = healerSection,
			position = 11
	)
	default FoodPanelStyle foodPanelStyle()
	{
		return FoodPanelStyle.ROWS;
	}

	@ConfigItem(
			keyName = "showFoodPanelAsHealerOnly",
			name = "Show Food Panel as Healer Only",
			description = "Only shows the food panel while playing the Healer role",
			section = healerSection,
			position = 12
	)
	default boolean showFoodPanelAsHealerOnly()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showFoodCountOnNpc",
			name = "Show Food Count on NPC",
			description = "Displays the number of food fed directly on each Penance Healer",
			section = healerSection,
			position = 13
	)
	default boolean showFoodCountOnNpc()
	{
		return false;
	}

	@ConfigItem(
			keyName = "foodCountType",
			name = "Food Count Type",
			description = "Choose whether NPC food displays count up or count down when a selected code has an expected count",
			section = healerSection,
			position = 14
	)
	default FoodCountType foodCountType()
	{
		return FoodCountType.COUNT_UP;
	}

	@Alpha
	@ConfigItem(
			keyName = "foodCountColor",
			name = "Food Count Color",
			description = "Fallback color used only when no selected code status applies and the NPC is showing the plain food count",
			section = healerSection,
			position = 15
	)
	default Color foodCountColor()
	{
		return new Color(0, 255, 0);
	}

	@ConfigItem(
			keyName = "healerTtkDisplay",
			name = "Healer TTK",
			description = "Shows when currently poisoned Penance Healers are expected to die",
			section = healerSection,
			position = 16
	)
	default HealerTtkDisplayMode healerTtkDisplay()
	{
		return HealerTtkDisplayMode.OFF;
	}

	@ConfigItem(
			keyName = "enableBaPartySync",
			name = "Enable BA Party Sync",
			description = "Automatically joins a temporary RuneLite Party with your current Barbarian Assault team while in the BA lobby. The plugin leaves the sync party after each wave.",
			section = generalSection,
			position = 1
	)
	default boolean enableBaPartySync()
	{
		return false;
	}

	@ConfigItem(
			keyName = "hideSidePanelButton",
			name = "Hide Side Panel Button",
			description = "Allows you to hide the side panel button to reduce clutter when not changing codes frequently",
			section = generalSection,
			position = 2
	)
	default boolean hideSidePanelButton()
	{
		return false;
	}

	@ConfigItem(
			keyName = "deprioritizeOtherDispensers",
			name = "Deprioritize Other Dispensers",
			description = "Removes interaction options from dispensers that do not match your current BA role",
			section = generalSection,
			position = 3
	)
	default boolean deprioritizeOtherDispensers()
	{
		return false;
	}
}
