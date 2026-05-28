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
            description = "Settings for the healer hull outline and spawn-order number",
            position = 1
    )
    String healerHighlightSection = "healerHighlight";

    @ConfigSection(
            name = "Food Count",
            description = "Settings for poisoned-food count tracking and display",
            position = 2
    )
    String foodCountSection = "foodCount";

    enum HealerLabelStyle
    {
        SPAWN_ORDER("Spawn Order"),
        TIME_BASED_NUMBERING("Time-Based Numbering");

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

        enum HealerRole
        {
                NONE("Default"),
                SOLO("Solo"),
                SPAM("Spam"),
                TAG("Tag");

                private final String name;

                HealerRole(String name)
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

    @ConfigSection(
            name = "Solo Role Wave Settings",
            description = "Expected poisoned-food per healer for solo role waves 1-10 (comma-separated)",
            position = 3
    )
    String soloRoleSection = "soloRoleSection";

    @ConfigSection(
            name = "Spam Role Wave Settings",
            description = "Expected poisoned-food per healer for spam role waves 1-10 (comma-separated)",
            position = 4
    )
    String spamRoleSection = "spamRoleSection";

    @ConfigSection(
            name = "Tag Role Wave Settings",
            description = "Expected poisoned-food per healer for tag role waves 1-10 (comma-separated)",
            position = 5
    )
    String tagRoleSection = "tagRoleSection";

    @Alpha
    @ConfigItem(
            keyName = "hullColor",
            name = "Hull Color",
            description = "Color used for the Penance Healer hull outline",
            section = healerHighlightSection,
            position = 1
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
            position = 2
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
            position = 3
    )
    default int textSize()
    {
        return 20;
    }

    @ConfigItem(
            keyName = "healerLabelStyle",
            name = "Healer Label Style",
            description = "Choose whether healers are labeled by spawn order or time-based BA numbering",
            section = healerHighlightSection,
            position = 4
    )
    default HealerLabelStyle healerLabelStyle()
    {
        return HealerLabelStyle.SPAWN_ORDER;
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

    @Alpha
    @ConfigItem(
            keyName = "foodCountColor",
            name = "Food Count Color",
            description = "Color used for the food count text displayed on each Penance Healer",
            section = foodCountSection,
            position = 3
    )
    default Color foodCountColor()
    {
        return new Color(0, 255, 0);
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
            position = 4
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
            position = 5
    )
    default int foodCountZOffset()
    {
        return 35;
    }

    @ConfigItem(
            keyName = "healerRole",
            name = "Healer Role",
            description = "Select which healer role's expected values to reference for NPC labels (empty = none)",
            section = foodCountSection,
            position = 6
    )
    default HealerRole healerRole()
    {
        return HealerRole.NONE;
    }

    @ConfigItem(
            keyName = "foodCountType",
            name = "Food Count Type",
            description = "Choose whether NPC food displays count up (used/expected) or count down (remaining).",
            section = foodCountSection,
            position = 7
    )
    default FoodCountType foodCountType()
    {
        return FoodCountType.COUNT_DOWN;
    }

        @ConfigItem(
                        keyName = "soloWave1",
                        name = "Solo Wave 1",
                        description = "Comma-separated expected food per healer for solo wave 1",
                        section = soloRoleSection,
                        position = 1
        )
        default String soloWave1()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "soloWave2",
                        name = "Solo Wave 2",
                        description = "Comma-separated expected food per healer for solo wave 2",
                        section = soloRoleSection,
                        position = 2
        )
        default String soloWave2()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "soloWave3",
                        name = "Solo Wave 3",
                        description = "Comma-separated expected food per healer for solo wave 3",
                        section = soloRoleSection,
                        position = 3
        )
        default String soloWave3()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "soloWave4",
                        name = "Solo Wave 4",
                        description = "Comma-separated expected food per healer for solo wave 4",
                        section = soloRoleSection,
                        position = 4
        )
        default String soloWave4()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "soloWave5",
                        name = "Solo Wave 5",
                        description = "Comma-separated expected food per healer for solo wave 5",
                        section = soloRoleSection,
                        position = 5
        )
        default String soloWave5()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "soloWave6",
                        name = "Solo Wave 6",
                        description = "Comma-separated expected food per healer for solo wave 6",
                        section = soloRoleSection,
                        position = 6
        )
        default String soloWave6()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "soloWave7",
                        name = "Solo Wave 7",
                        description = "Comma-separated expected food per healer for solo wave 7",
                        section = soloRoleSection,
                        position = 7
        )
        default String soloWave7()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "soloWave8",
                        name = "Solo Wave 8",
                        description = "Comma-separated expected food per healer for solo wave 8",
                        section = soloRoleSection,
                        position = 8
        )
        default String soloWave8()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "soloWave9",
                        name = "Solo Wave 9",
                        description = "Comma-separated expected food per healer for solo wave 9",
                        section = soloRoleSection,
                        position = 9
        )
        default String soloWave9()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "soloWave10",
                        name = "Solo Wave 10",
                        description = "Comma-separated expected food per healer for solo wave 10",
                        section = soloRoleSection,
                        position = 10
        )
        default String soloWave10()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave1",
                        name = "Tag Wave 1",
                        description = "Comma-separated expected food per healer for tag wave 1. Example: 5,5",
                        section = tagRoleSection,
                        position = 1
        )
        default String tagWave1()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave2",
                        name = "Tag Wave 2",
                        description = "Comma-separated expected food per healer for tag wave 2",
                        section = tagRoleSection,
                        position = 2
        )
        default String tagWave2()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave3",
                        name = "Tag Wave 3",
                        description = "Comma-separated expected food per healer for tag wave 3",
                        section = tagRoleSection,
                        position = 3
        )
        default String tagWave3()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave4",
                        name = "Tag Wave 4",
                        description = "Comma-separated expected food per healer for tag wave 4",
                        section = tagRoleSection,
                        position = 4
        )
        default String tagWave4()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave5",
                        name = "Tag Wave 5",
                        description = "Comma-separated expected food per healer for tag wave 5",
                        section = tagRoleSection,
                        position = 5
        )
        default String tagWave5()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave6",
                        name = "Tag Wave 6",
                        description = "Comma-separated expected food per healer for tag wave 6",
                        section = tagRoleSection,
                        position = 6
        )
        default String tagWave6()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave7",
                        name = "Tag Wave 7",
                        description = "Comma-separated expected food per healer for tag wave 7",
                        section = tagRoleSection,
                        position = 7
        )
        default String tagWave7()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave8",
                        name = "Tag Wave 8",
                        description = "Comma-separated expected food per healer for tag wave 8",
                        section = tagRoleSection,
                        position = 8
        )
        default String tagWave8()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave9",
                        name = "Tag Wave 9",
                        description = "Comma-separated expected food per healer for tag wave 9",
                        section = tagRoleSection,
                        position = 9
        )
        default String tagWave9()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave10",
                        name = "Tag Wave 10",
                        description = "Comma-separated expected food per healer for tag wave 10",
                        section = tagRoleSection,
                        position = 10
        )
        default String tagWave10()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave1",
                        name = "Spam Wave 1",
                        description = "Comma-separated expected food per healer for spam wave 1",
                        section = spamRoleSection,
                        position = 11
        )
        default String spamWave1()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave2",
                        name = "Spam Wave 2",
                        description = "Comma-separated expected food per healer for spam wave 2",
                        section = spamRoleSection,
                        position = 12
        )
        default String spamWave2()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave3",
                        name = "Spam Wave 3",
                        description = "Comma-separated expected food per healer for spam wave 3",
                        section = spamRoleSection,
                        position = 13
        )
        default String spamWave3()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave4",
                        name = "Spam Wave 4",
                        description = "Comma-separated expected food per healer for spam wave 4",
                        section = spamRoleSection,
                        position = 14
        )
        default String spamWave4()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave5",
                        name = "Spam Wave 5",
                        description = "Comma-separated expected food per healer for spam wave 5",
                        section = spamRoleSection,
                        position = 15
        )
        default String spamWave5()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave6",
                        name = "Spam Wave 6",
                        description = "Comma-separated expected food per healer for spam wave 6",
                        section = spamRoleSection,
                        position = 16
        )
        default String spamWave6()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave7",
                        name = "Spam Wave 7",
                        description = "Comma-separated expected food per healer for spam wave 7",
                        section = spamRoleSection,
                        position = 17
        )
        default String spamWave7()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave8",
                        name = "Spam Wave 8",
                        description = "Comma-separated expected food per healer for spam wave 8",
                        section = spamRoleSection,
                        position = 18
        )
        default String spamWave8()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave9",
                        name = "Spam Wave 9",
                        description = "Comma-separated expected food per healer for spam wave 9",
                        section = spamRoleSection,
                        position = 19
        )
        default String spamWave9()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave10",
                        name = "Spam Wave 10",
                        description = "Comma-separated expected food per healer for spam wave 10",
                        section = spamRoleSection,
                        position = 20
        )
        default String spamWave10()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "resetOnListChange",
                        name = "Reset On List Change",
                        description = "If enabled, switching the active list will reset healer numbering and counts.",
                        section = foodCountSection,
                        position = 22
        )
        default boolean resetOnListChange()
        {
                return false;
        }

        
}