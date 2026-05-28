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

    @ConfigSection(
            name = "Wave Settings",
            description = "Expected poisoned-food per healer for waves 1-10 (comma-separated)",
            position = 3
    )
    String waveSection = "waveSection";

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
            description = "Color used for the spawn order number above each Penance Healer",
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
            description = "Font size used for the spawn order number above each Penance Healer",
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
            description = "Displays the number of food fed directly on top of each Penance Healer",
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
            keyName = "decrementOnFeed",
            name = "Show Remaining on NPC",
            description = "If enabled, NPC food labels start at the expected amount and decrement as food is fed.",
            section = foodCountSection,
            position = 6
    )
    default boolean decrementOnFeed()
    {
        return false;
    }

        @ConfigItem(
                        keyName = "tagWave1",
                        name = "Tag Wave 1",
                        description = "Comma-separated expected food per healer for tag wave 1. Example: 5,5",
                        section = waveSection,
                        position = 101
        )
        default String tagWave1()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave2",
                        name = "Tag Wave 2",
                        description = "Comma-separated expected food per healer for tag wave 2",
                        section = waveSection,
                        position = 102
        )
        default String tagWave2()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave3",
                        name = "Tag Wave 3",
                        description = "Comma-separated expected food per healer for tag wave 3",
                        section = waveSection,
                        position = 103
        )
        default String tagWave3()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave4",
                        name = "Tag Wave 4",
                        description = "Comma-separated expected food per healer for tag wave 4",
                        section = waveSection,
                        position = 104
        )
        default String tagWave4()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave5",
                        name = "Tag Wave 5",
                        description = "Comma-separated expected food per healer for tag wave 5",
                        section = waveSection,
                        position = 105
        )
        default String tagWave5()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave6",
                        name = "Tag Wave 6",
                        description = "Comma-separated expected food per healer for tag wave 6",
                        section = waveSection,
                        position = 106
        )
        default String tagWave6()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave7",
                        name = "Tag Wave 7",
                        description = "Comma-separated expected food per healer for tag wave 7",
                        section = waveSection,
                        position = 107
        )
        default String tagWave7()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave8",
                        name = "Tag Wave 8",
                        description = "Comma-separated expected food per healer for tag wave 8",
                        section = waveSection,
                        position = 108
        )
        default String tagWave8()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave9",
                        name = "Tag Wave 9",
                        description = "Comma-separated expected food per healer for tag wave 9",
                        section = waveSection,
                        position = 109
        )
        default String tagWave9()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "tagWave10",
                        name = "Tag Wave 10",
                        description = "Comma-separated expected food per healer for tag wave 10",
                        section = waveSection,
                        position = 110
        )
        default String tagWave10()
        {
                return "";
        }

        /* Display fields (only these should be shown/edited by user). The
         * plugin will copy values between the active storage (tag/spam)
         * and these display fields when the selected list changes. */
        @ConfigItem(
                        keyName = "displayWave1",
                        name = "Wave 1",
                        description = "Comma-separated expected food per healer for wave 1 (active list)",
                        section = waveSection,
                        position = 1
        )
        default String displayWave1()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "displayWave2",
                        name = "Wave 2",
                        description = "Comma-separated expected food per healer for wave 2 (active list)",
                        section = waveSection,
                        position = 2
        )
        default String displayWave2()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "displayWave3",
                        name = "Wave 3",
                        description = "Comma-separated expected food per healer for wave 3 (active list)",
                        section = waveSection,
                        position = 3
        )
        default String displayWave3()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "displayWave4",
                        name = "Wave 4",
                        description = "Comma-separated expected food per healer for wave 4 (active list)",
                        section = waveSection,
                        position = 4
        )
        default String displayWave4()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "displayWave5",
                        name = "Wave 5",
                        description = "Comma-separated expected food per healer for wave 5 (active list)",
                        section = waveSection,
                        position = 5
        )
        default String displayWave5()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "displayWave6",
                        name = "Wave 6",
                        description = "Comma-separated expected food per healer for wave 6 (active list)",
                        section = waveSection,
                        position = 6
        )
        default String displayWave6()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "displayWave7",
                        name = "Wave 7",
                        description = "Comma-separated expected food per healer for wave 7 (active list)",
                        section = waveSection,
                        position = 7
        )
        default String displayWave7()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "displayWave8",
                        name = "Wave 8",
                        description = "Comma-separated expected food per healer for wave 8 (active list)",
                        section = waveSection,
                        position = 8
        )
        default String displayWave8()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "displayWave9",
                        name = "Wave 9",
                        description = "Comma-separated expected food per healer for wave 9 (active list)",
                        section = waveSection,
                        position = 9
        )
        default String displayWave9()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "displayWave10",
                        name = "Wave 10",
                        description = "Comma-separated expected food per healer for wave 10 (active list)",
                        section = waveSection,
                        position = 10
        )
        default String displayWave10()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "useTagList",
                        name = "Use Tag List",
                        description = "When enabled uses the 'Tag' list (wave1..wave10). When disabled uses the 'Spam' list.",
                        section = waveSection,
                        position = 11
        )
        default boolean useTagList()
        {
                return true;
        }

        @ConfigItem(
                        keyName = "waveListType",
                        name = "Wave List Type",
                        description = "Choose which wave list to use: Tag or Spam",
                        section = waveSection,
                        position = 11
        )
        default WaveListType waveListType()
        {
                return WaveListType.TAG;
        }

        @ConfigItem(
                        keyName = "resetOnListChange",
                        name = "Reset On List Change",
                        description = "If enabled, switching the active list will reset healer numbering and counts.",
                        section = waveSection,
                        position = 22
        )
        default boolean resetOnListChange()
        {
                return false;
        }

        @ConfigItem(
                        keyName = "spamWave1",
                        name = "Spam Wave 1",
                        description = "Comma-separated expected food per healer for spam wave 1",
                        section = waveSection,
                        position = 12
        )
        default String spamWave1()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave2",
                        name = "Spam Wave 2",
                        description = "Comma-separated expected food per healer for spam wave 2",
                        section = waveSection,
                        position = 13
        )
        default String spamWave2()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave3",
                        name = "Spam Wave 3",
                        description = "Comma-separated expected food per healer for spam wave 3",
                        section = waveSection,
                        position = 14
        )
        default String spamWave3()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave4",
                        name = "Spam Wave 4",
                        description = "Comma-separated expected food per healer for spam wave 4",
                        section = waveSection,
                        position = 15
        )
        default String spamWave4()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave5",
                        name = "Spam Wave 5",
                        description = "Comma-separated expected food per healer for spam wave 5",
                        section = waveSection,
                        position = 16
        )
        default String spamWave5()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave6",
                        name = "Spam Wave 6",
                        description = "Comma-separated expected food per healer for spam wave 6",
                        section = waveSection,
                        position = 17
        )
        default String spamWave6()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave7",
                        name = "Spam Wave 7",
                        description = "Comma-separated expected food per healer for spam wave 7",
                        section = waveSection,
                        position = 18
        )
        default String spamWave7()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave8",
                        name = "Spam Wave 8",
                        description = "Comma-separated expected food per healer for spam wave 8",
                        section = waveSection,
                        position = 19
        )
        default String spamWave8()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave9",
                        name = "Spam Wave 9",
                        description = "Comma-separated expected food per healer for spam wave 9",
                        section = waveSection,
                        position = 20
        )
        default String spamWave9()
        {
                return "";
        }

        @ConfigItem(
                        keyName = "spamWave10",
                        name = "Spam Wave 10",
                        description = "Comma-separated expected food per healer for spam wave 10",
                        section = waveSection,
                        position = 21
        )
        default String spamWave10()
        {
                return "";
        }

        enum WaveListType
        {
                TAG,
                SPAM
        }
}