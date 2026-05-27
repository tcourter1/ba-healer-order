package com.bahealerorder;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("bahealerorder")
public interface BaHealerOrderConfig extends Config
{
    @Alpha
    @ConfigItem(
            keyName = "hullColor",
            name = "Hull Color",
            description = "Color used for the Penance Healer hull outline",
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
            position = 4
    )
    default boolean showFoodPanel()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showFoodCountOnNpc",
            name = "Show Food Count on NPC",
            description = "Displays the number of food fed directly on top of each Penance Healer",
            position = 5
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
            position = 6
    )
    default Color foodCountColor()
    {
        return new Color(0, 255, 0);
    }
}