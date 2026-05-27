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
}