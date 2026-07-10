package com.bahealerorder;

import java.awt.Color;
import lombok.RequiredArgsConstructor;
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

	String CURRENT_UPDATE_NOTES_VERSION = "2.3.0";
	String CURRENT_ASSISTANCE_PRESET_VERSION = "2.3.0";

	String SHOW_OVERVIEW_RANGERS_KEY = "showOverviewRangers";
	String SHOW_OVERVIEW_FIGHTERS_KEY = "showOverviewFighters";
	String SHOW_OVERVIEW_RUNNERS_KEY = "showOverviewRunners";
	String SHOW_OVERVIEW_HEALERS_KEY = "showOverviewHealers";
	String FOOD_PANEL_CODE_COLLAPSED_KEY = "foodPanelCodeCollapsed";
	String SIDE_PANEL_TAB_KEY = "sidePanelTab";

	String LAST_SEEN_UPDATE_VERSION_KEY = "lastSeenUpdateVersion";
	String ASSISTANCE_PRESET_VERSION_KEY = "assistancePresetVersion";
	String ASSISTANCE_PRESET_CHOICE_KEY = "assistancePresetChoice";

	@ConfigSection(
			name = "General",
			description = "General settings and settings that can apply to any BA role",
			position = 1
	)
	String generalSection = "general";

	@ConfigSection(
			name = "Healer",
			description = "Settings for healer highlighting, labels, food counts, and dispenser options",
			position = 2,
			closedByDefault = true
	)
	String healerSection = "healer";

	@ConfigSection(
			name = "Attacker",
			description = "Utility helpers for the BA attacker role",
			position = 3,
			closedByDefault = true
	)
	String attackerSection = "attacker";

	@ConfigSection(
			name = "Defender",
			description = "Utility helpers for the BA defender role",
			position = 4,
			closedByDefault = true
	)
	String defenderSection = "defender";

	@ConfigSection(
			name = "Scroller",
			description = "Utility helpers for the BA team scroller",
			position = 5,
			closedByDefault = true
	)
	String scrollerSection = "scroller";

	@RequiredArgsConstructor
	enum AssistancePreset
	{
		BEGINNER("Beginner"),
		INTERMEDIATE("Intermediate"),
		RECOMMENDED("Recommended"),
		BA_PRO("BA Pro (Minimum Assistance)"),
		SKIPPED("Skipped");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@RequiredArgsConstructor
	enum HighlightStyle
	{
		NONE("None"),
		TILE("Tile"),
		TRUE_TILE("True Tile"),
		HULL("Hull");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@RequiredArgsConstructor
	enum HealerLabelStyle
	{
		NONE("None"),
		SPAWN_ORDER("Spawn Order"),
		TIME_BASED_NUMBERING("Time-Based");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@RequiredArgsConstructor
	enum HideDeadNpcMode
	{
		NONE("None"),
		HEALERS_ONLY("Healers Only"),
		ALL_BA_NPCS("All BA NPCs");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@RequiredArgsConstructor
	enum FoodCountType
	{
		COUNT_UP("Count Up"),
		COUNT_DOWN("Count Down");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@RequiredArgsConstructor
	enum FoodPanelStyle
	{
		NONE("None"),
		ROWS("Rows"),
		COLUMNS("Columns"),
		SIMPLIFIED("Simplified"),
		CODE_ONLY("Code Only");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@RequiredArgsConstructor
	enum OverlayFont
	{
		DEFAULT("Default"),
		ARIAL("Arial"),
		ARIAL_BOLD("Arial Bold");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@RequiredArgsConstructor
	enum HealerTtkDisplayMode
	{
		OFF("Off"),
		TICKS("Ticks"),
		SECONDS("Seconds"),
		WAVE_TIME("Wave Time");

		private final String name;

		@Override
		public String toString()
		{
			return name;
		}
	}

	@RequiredArgsConstructor
	enum DispenserOptions
	{
		NONE("None", false, false),
		REMOVE_TAKE_VIAL("Hide Vial", true, false),
		MOVE_TAKE_MEAT_UP("Move Meat Up", false, true),
		BOTH("Both", true, true);

		private final String name;
		private final boolean removeTakeVial;
		private final boolean moveTakeMeatUp;

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
			keyName = "enableBaPartySync",
			name = "Enable BA Party Sync",
			description = "Automatically joins a temporary RuneLite Party with your current Barbarian Assault team while in the BA lobby.",
			section = generalSection,
			position = 1
	)
	default boolean enableBaPartySync()
	{
		return true;
	}

	@ConfigItem(
			keyName = "enablePartyChat",
			name = "Enable Party Chat",
			description = "When in a BA Party, enable public chat in the entire BA arena between party members.",
			section = generalSection,
			position = 2
	)
	default boolean enablePartyChat()
	{
		return true;
	}

	@ConfigItem(
			keyName = "hideSidePanelButton",
			name = "Hide Side Panel Button",
			description = "Allows you to hide the side panel button to reduce clutter when not changing codes frequently",
			section = generalSection,
			position = 3
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
			position = 4
	)
	default boolean deprioritizeOtherDispensers()
	{
		return false;
	}

	@ConfigItem(
			keyName = "hideDeadNpcs",
			name = "Hide Dead NPCs",
			description = "Hides selected BA NPCs immediately when they begin dying instead of waiting for their death animation to finish",
			section = generalSection,
			position = 5
	)
	default HideDeadNpcMode hideDeadNpcs()
	{
		return HideDeadNpcMode.NONE;
	}

	@ConfigItem(
			keyName = "showOverlayPanel",
			name = "Show Overlay Panel",
			description = "Shows the role-aware overlay panel when the current role has panel content",
			section = generalSection,
			position = 6
	)
	default boolean showOverlayPanel()
	{
		return true;
	}

	@ConfigItem(
			keyName = "overlayFont",
			name = "Overlay Font",
			description = "Font used by the on-screen BA Utilities overlay panel",
			section = generalSection,
			position = 7
	)
	default OverlayFont overlayFont()
	{
		return OverlayFont.DEFAULT;
	}

	@Range(
			min = 8,
			max = 24
	)
	@ConfigItem(
			keyName = "foodPanelOverlayTextSize",
			name = "Overlay Text Size",
			description = "Font size used by the on-screen BA Utilities overlay panel. Panel spacing scales proportionally with this value.",
			section = generalSection,
			position = 8
	)
	default int foodPanelOverlayTextSize()
	{
		return 16;
	}

	@Alpha
	@ConfigItem(
			keyName = "foodPanelOverlayBackgroundColor",
			name = "Overlay Panel Background",
			description = "Background color used by the on-screen BA Utilities overlay panel",
			section = generalSection,
			position = 9
	)
	default Color foodPanelOverlayBackgroundColor()
	{
		return new Color(70, 61, 50, 156);
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
			description = "Adds the tracked healer label next to Penance Healers in the right-click menu",
			section = healerSection,
			position = 7
	)
	default boolean showMenuLabel()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showMenuCode",
			name = "Show Menu Code",
			description = "Adds the current healer code next to Penance Healers in the right-click menu",
			section = healerSection,
			position = 8
	)
	default boolean showMenuCode()
	{
		return true;
	}

	@ConfigItem(
			keyName = "foodPanelStyle",
			name = "Healer Panel Style",
			description = "Choose how the overlay panel displays tracked healer food",
			section = healerSection,
			position = 9
	)
	default FoodPanelStyle foodPanelStyle()
	{
		return FoodPanelStyle.SIMPLIFIED;
	}

	@ConfigItem(
			keyName = "healerFoodOnly",
			name = "Use-Food on Healers Only",
			description = "When poisoned food is selected, only Penance Healer Use entries remain clickable",
			section = healerSection,
			position = 10
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
			position = 11
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
			position = 12
	)
	default DispenserOptions dispenserOptions()
	{
		return DispenserOptions.NONE;
	}

	@ConfigItem(
			keyName = "showFoodCountOnNpc",
			name = "Show Food Count on NPC",
			description = "Displays the number of food fed directly on each Penance Healer",
			section = healerSection,
			position = 14
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
			position = 15
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
			position = 16
	)
	default Color foodCountColor()
	{
		return new Color(0, 255, 0);
	}

	@Range(
			min = -200,
			max = 300
	)
	@ConfigItem(
			keyName = "foodCountHeight",
			name = "Food Count Height",
			description = "Adjusts the vertical position of food-count text displayed on Penance Healers",
			section = healerSection,
			position = 17
	)
	default int foodCountHeight()
	{
		return 35;
	}

	@ConfigItem(
			keyName = "healerTtkDisplay",
			name = "Healer TTK",
			description = "Shows when currently poisoned Penance Healers are expected to die",
			section = healerSection,
			position = 18
	)
	default HealerTtkDisplayMode healerTtkDisplay()
	{
		return HealerTtkDisplayMode.OFF;
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

	@Alpha
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
			keyName = "highlightDefenderHammer",
			name = "Highlight Hammer",
			description = "Highlights ground hammer items while playing Defender when you need a hammer",
			section = defenderSection,
			position = 1
	)
	default boolean highlightDefenderHammer()
	{
		return true;
	}

	@ConfigItem(
			keyName = "highlightScrollerLadder",
			name = "Highlight Current Wave Ladder",
			description = "Highlights the Barbarian Assault ladder for the current wave room while you are the BA team scroller",
			section = scrollerSection,
			position = 1
	)
	default boolean highlightScrollerLadder()
	{
		return true;
	}

	@ConfigItem(
			keyName = "highlightOmegaDupeItems",
			name = "Highlight Omega Dupe Items",
			description = "Highlights book of egg, shrink, and shields when omega egg is loaded",
			section = scrollerSection,
			position = 2
	)
	default boolean highlightOmegaDupeItems()
	{
		return true;
	}

	@ConfigItem(
			keyName = SIDE_PANEL_TAB_KEY,
			name = "Side Panel Tab",
			description = "Remembers the last selected BA Utilities side-panel tab",
			section = generalSection,
			position = 99,
			hidden = true
	)
	default String sidePanelTab()
	{
		return "healer";
	}

	@ConfigItem(
			keyName = LAST_SEEN_UPDATE_VERSION_KEY,
			name = "Last Seen Update Version",
			description = "Remembers the latest BA Utilities update notes version the user has seen",
			section = generalSection,
			position = 100,
			hidden = true
	)
	default String lastSeenUpdateVersion()
	{
		return "";
	}

	@ConfigItem(
			keyName = ASSISTANCE_PRESET_VERSION_KEY,
			name = "Assistance Preset Version",
			description = "Remembers the latest BA Utilities assistance preset onboarding version the user has completed",
			section = generalSection,
			position = 101,
			hidden = true
	)
	default String assistancePresetVersion()
	{
		return "";
	}

	@ConfigItem(
			keyName = ASSISTANCE_PRESET_CHOICE_KEY,
			name = "Assistance Preset Choice",
			description = "Remembers the BA Utilities assistance preset selected by the user",
			section = generalSection,
			position = 102,
			hidden = true
	)
	default AssistancePreset assistancePresetChoice()
	{
		return AssistancePreset.SKIPPED;
	}

	@ConfigItem(
			keyName = SHOW_OVERVIEW_RANGERS_KEY,
			name = "Show Overview Rangers",
			description = "Shows Penance Rangers in the side-panel wave overview",
			section = generalSection,
			position = 103,
			hidden = true
	)
	default boolean showOverviewRangers()
	{
		return true;
	}

	@ConfigItem(
			keyName = SHOW_OVERVIEW_FIGHTERS_KEY,
			name = "Show Overview Fighters",
			description = "Shows Penance Fighters in the side-panel wave overview",
			section = generalSection,
			position = 104,
			hidden = true
	)
	default boolean showOverviewFighters()
	{
		return true;
	}

	@ConfigItem(
			keyName = SHOW_OVERVIEW_RUNNERS_KEY,
			name = "Show Overview Runners",
			description = "Shows Penance Runners in the side-panel wave overview",
			section = generalSection,
			position = 105,
			hidden = true
	)
	default boolean showOverviewRunners()
	{
		return true;
	}

	@ConfigItem(
			keyName = SHOW_OVERVIEW_HEALERS_KEY,
			name = "Show Overview Healers",
			description = "Shows Penance Healers in the side-panel wave overview",
			section = generalSection,
			position = 106,
			hidden = true
	)
	default boolean showOverviewHealers()
	{
		return true;
	}

	@ConfigItem(
			keyName = FOOD_PANEL_CODE_COLLAPSED_KEY,
			name = "Food Panel Code Collapsed",
			description = "Remembers whether the food panel code section is collapsed",
			section = healerSection,
			position = 100,
			hidden = true
	)
	default boolean foodPanelCodeCollapsed()
	{
		return false;
	}
}
