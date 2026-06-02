package com.bahealerorder;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("bahealerorder")
public interface BaHealerOrderConfig extends Config
{
	@ConfigSection(
			name = "Healer Highlight",
			description = "Settings for healer highlighting and labels",
			position = 1
	)
	String healerHighlightSection = "healerHighlight";

	@ConfigSection(
			name = "Food Count",
			description = "Settings for poisoned-food count tracking and display",
			position = 2
	)
	String foodCountSection = "foodCount";

	@ConfigSection(
			name = "Dispenser Options",
			description = "Utility helpers for the BA healer role",
			position = 3
	)
	String dispenserOptionsSection = "dispenserOptions";

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

	@ConfigItem(
			keyName = "highlightStyle",
			name = "Highlight Style",
			description = "Choose how tracked Penance Healers are highlighted",
			section = healerHighlightSection,
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
			description = "Color used for the Penance Healer hull or tile highlight",
			section = healerHighlightSection,
			position = 2
	)
	default Color hullColor()
	{
		return new Color(0, 200, 200);
	}

	@Alpha
	@ConfigItem(
			keyName = "textColor",
			name = "Text Color",
			description = "Color used for the healer label above each Penance Healer",
			section = healerHighlightSection,
			position = 3
	)
	default Color textColor()
	{
		return new Color(0, 200, 200);
	}

	@Range(
			min = 12,
			max = 48
	)
	@ConfigItem(
			keyName = "textSize",
			name = "Text Size",
			description = "Font size used for the healer label above each Penance Healer",
			section = healerHighlightSection,
			position = 4
	)
	default int textSize()
	{
		return 20;
	}

	@ConfigItem(
			keyName = "healerLabelStyle",
			name = "Label Style",
			description = "Choose whether healer labels are hidden, spawn order, or time-based BA labels",
			section = healerHighlightSection,
			position = 5
	)
	default HealerLabelStyle healerLabelStyle()
	{
		return HealerLabelStyle.SPAWN_ORDER;
	}

	@ConfigItem(
			keyName = "spreadStackedLabels",
			name = "Spread Stacked Labels",
			description = "Horizontally separates healer labels when multiple Penance Healers occupy the same tile",
			section = healerHighlightSection,
			position = 6
	)
	default boolean spreadStackedLabels()
	{
		return true;
	}

	@Range(
			min = 12,
			max = 64
	)
	@ConfigItem(
			keyName = "stackedLabelSpacing",
			name = "Stacked Label Spacing",
			description = "Horizontal pixel spacing used when spreading labels for stacked Penance Healers",
			section = healerHighlightSection,
			position = 7
	)
	default int stackedLabelSpacing()
	{
		return 28;
	}

	@ConfigItem(
			keyName = "showMenuLabel",
			name = "Show Menu Label",
			description = "Adds the tracked healer label next to Penance Healers in the right-click menu",
			section = healerHighlightSection,
			position = 8
	)
	default boolean showMenuLabel()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showMenuCode",
			name = "Show Menu Code",
			description = "Adds the current healer code next to Penance Healers in the right-click menu",
			section = healerHighlightSection,
			position = 9
	)
	default boolean showMenuCode()
	{
		return false;
	}

	@ConfigItem(
			keyName = "healerFoodOnly",
			name = "Healer Food Only",
			description = "When poisoned food is selected, only Penance Healer Use entries remain clickable",
			section = healerHighlightSection,
			position = 10
	)
	default boolean healerFoodOnly()
	{
		return true;
	}

	@ConfigItem(
			keyName = "highlightCalledDispenserFood",
			name = "Highlight Called Food",
			description = "Highlights the correct Take option on the healer dispenser for the current food call",
			section = dispenserOptionsSection,
			position = 1
	)
	default boolean highlightCalledDispenserFood()
	{
		return false;
	}

	@ConfigItem(
			keyName = "removeTakeVial",
			name = "Remove Take-Vial",
			description = "Removes the Take-Vial option from the healer dispenser menu",
			section = dispenserOptionsSection,
			position = 2
	)
	default boolean removeTakeVial()
	{
		return false;
	}

	@ConfigItem(
			keyName = "moveTakeMeatUp",
			name = "Move Take-Meat up",
			description = "Moves Take-Meat closer to the other healer dispenser food options",
			section = dispenserOptionsSection,
			position = 3
	)
	default boolean moveTakeMeatUp()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showFoodPanel",
			name = "Show Food Panel",
			description = "Shows a panel tracking how much good food has been fed to each Penance Healer",
			section = foodCountSection,
			position = 1
	)
	default boolean showFoodPanel()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showFoodCountOnNpc",
			name = "Show Food Count on NPC",
			description = "Displays the number of food fed directly on each Penance Healer",
			section = foodCountSection,
			position = 2
	)
	default boolean showFoodCountOnNpc()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showPreviousCallCode",
			name = "Show Previous Call Code",
			description = "Shows the previous call's completed code in gray above the active code",
			section = foodCountSection,
			position = 3
	)
	default boolean showPreviousCallCode()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
			keyName = "notStartedCodeColor",
			name = "Not Started Code Color",
			description = "Color used when a selected healer code has not received any food yet",
			section = foodCountSection,
			position = 4
	)
	default Color notStartedCodeColor()
	{
		return new Color(255, 60, 60);
	}

	@Alpha
	@ConfigItem(
			keyName = "inProgressCodeColor",
			name = "In Progress Code Color",
			description = "Color used when a selected healer code has started but is not complete",
			section = foodCountSection,
			position = 5
	)
	default Color inProgressCodeColor()
	{
		return new Color(255, 150, 0);
	}

	@Alpha
	@ConfigItem(
			keyName = "completeCodeColor",
			name = "Complete Code Color",
			description = "Color used when a selected healer code is complete, including any timing requirement",
			section = foodCountSection,
			position = 6
	)
	default Color completeCodeColor()
	{
		return new Color(0, 220, 0);
	}

	@Alpha
	@ConfigItem(
			keyName = "previousCodeColor",
			name = "Previous Call Code Color",
			description = "Color used for completed code text from a previous call when that healer has no active incomplete code",
			section = foodCountSection,
			position = 7
	)
	default Color previousCodeColor()
	{
		return new Color(150, 150, 150);
	}

	@Range(
			min = 10,
			max = 48
	)
	@ConfigItem(
			keyName = "foodCountTextSize",
			name = "Food Count Text Size",
			description = "Font size used for the food count displayed on each Penance Healer",
			section = foodCountSection,
			position = 8
	)
	default int foodCountTextSize()
	{
		return 16;
	}

	@Range(
			min = -100,
			max = 150
	)
	@ConfigItem(
			keyName = "foodCountZOffset",
			name = "Food Count Height",
			description = "Adjusts the vertical position of the food count on each Penance Healer. Higher values move it upward.",
			section = foodCountSection,
			position = 9
	)
	default int foodCountZOffset()
	{
		return 35;
	}

	@ConfigItem(
			keyName = "foodCountType",
			name = "Food Count Type",
			description = "Choose whether NPC food displays count up or count down when a selected code has an expected count",
			section = foodCountSection,
			position = 10
	)
	default FoodCountType foodCountType()
	{
		return FoodCountType.COUNT_DOWN;
	}

	@Alpha
	@ConfigItem(
			keyName = "foodCountColor",
			name = "Food Count Color",
			description = "Fallback color used only when no selected code status applies and the NPC is showing the plain food count",
			section = foodCountSection,
			position = 11
	)
	default Color foodCountColor()
	{
		return new Color(0, 255, 0);
	}
}