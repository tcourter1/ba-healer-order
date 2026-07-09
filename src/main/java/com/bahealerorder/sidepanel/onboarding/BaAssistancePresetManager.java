package com.bahealerorder.sidepanel.onboarding;

import com.bahealerorder.BaUtilitiesConfig;
import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

@Singleton
public class BaAssistancePresetManager
{
    private final ConfigManager configManager;
    private final BaUtilitiesConfig config;

    @Inject
    public BaAssistancePresetManager(ConfigManager configManager, BaUtilitiesConfig config)
    {
        this.configManager = configManager;
        this.config = config;
    }

    public boolean isUpdateNotesPending()
    {
        return !BaUtilitiesConfig.CURRENT_UPDATE_NOTES_VERSION.equals(config.lastSeenUpdateVersion());
    }

    public boolean isAssistancePresetPending()
    {
        return !BaUtilitiesConfig.CURRENT_ASSISTANCE_PRESET_VERSION.equals(config.assistancePresetVersion());
    }

    public boolean shouldShowOnboarding()
    {
        return isUpdateNotesPending() || isAssistancePresetPending();
    }

    public void markUpdateNotesSeen()
    {
        set(BaUtilitiesConfig.LAST_SEEN_UPDATE_VERSION_KEY, BaUtilitiesConfig.CURRENT_UPDATE_NOTES_VERSION);
    }

    public void resetUpdateNotesForDev()
    {
        set(BaUtilitiesConfig.LAST_SEEN_UPDATE_VERSION_KEY, "");
    }

    public void skipAssistancePreset()
    {
        set(BaUtilitiesConfig.ASSISTANCE_PRESET_CHOICE_KEY, BaUtilitiesConfig.AssistancePreset.SKIPPED);
        set(BaUtilitiesConfig.ASSISTANCE_PRESET_VERSION_KEY, BaUtilitiesConfig.CURRENT_ASSISTANCE_PRESET_VERSION);
    }

    public void resetAssistancePresetForDev()
    {
        set(BaUtilitiesConfig.ASSISTANCE_PRESET_VERSION_KEY, "");
    }

    public void applyPreset(BaUtilitiesConfig.AssistancePreset preset)
    {
        if (preset == null || preset == BaUtilitiesConfig.AssistancePreset.SKIPPED)
        {
            skipAssistancePreset();
            return;
        }

        switch (preset)
        {
            case BEGINNER:
                applyBeginnerPreset();
                break;
            case INTERMEDIATE:
                applyIntermediatePreset();
                break;
            case RECOMMENDED:
                applyRecommendedPreset();
                break;
            case BA_PRO:
                applyBaProPreset();
                break;
            default:
                skipAssistancePreset();
                return;
        }

        set(BaUtilitiesConfig.ASSISTANCE_PRESET_CHOICE_KEY, preset);
        set(BaUtilitiesConfig.ASSISTANCE_PRESET_VERSION_KEY, BaUtilitiesConfig.CURRENT_ASSISTANCE_PRESET_VERSION);
    }

    private void applyBeginnerPreset()
    {
        applyGeneralPreset(
                true,
                true,
                false,
                true,
                BaUtilitiesConfig.HideDeadNpcMode.NONE,
                true,
                BaUtilitiesConfig.OverlayFont.ARIAL,
                16,
                defaultOverlayBackground()
        );

        applyHealerPreset(
                BaUtilitiesConfig.HighlightStyle.HULL,
                defaultHealerHighlightColor(),
                BaUtilitiesConfig.HealerLabelStyle.SPAWN_ORDER,
                true,
                true,
                true,
                false,
                BaUtilitiesConfig.FoodPanelStyle.SIMPLIFIED,
                true,
                true,
                BaUtilitiesConfig.DispenserOptions.BOTH,
                true,
                BaUtilitiesConfig.FoodCountType.COUNT_UP,
                defaultFoodCountColor(),
                100,
                BaUtilitiesConfig.HealerTtkDisplayMode.OFF
        );

        applyAttackerPreset(true);
        applyDefenderPreset(true);
        applyScrollerPreset(true, false);
    }

    private void applyIntermediatePreset()
    {
        applyGeneralPreset(
                true,
                true,
                false,
                true,
                BaUtilitiesConfig.HideDeadNpcMode.HEALERS_ONLY,
                false,
                BaUtilitiesConfig.OverlayFont.DEFAULT,
                16,
                defaultOverlayBackground()
        );

        applyHealerPreset(
                BaUtilitiesConfig.HighlightStyle.NONE,
                defaultHealerHighlightColor(),
                BaUtilitiesConfig.HealerLabelStyle.TIME_BASED_NUMBERING,
                false,
                true,
                true,
                true,
                BaUtilitiesConfig.FoodPanelStyle.SIMPLIFIED,
                true,
                true,
                BaUtilitiesConfig.DispenserOptions.NONE,
                true,
                BaUtilitiesConfig.FoodCountType.COUNT_DOWN,
                defaultFoodCountColor(),
                100,
                BaUtilitiesConfig.HealerTtkDisplayMode.WAVE_TIME
        );

        applyAttackerPreset(true);
        applyDefenderPreset(false);
        applyScrollerPreset(true, true);
    }

    private void applyRecommendedPreset()
    {
        applyGeneralPreset(
                true,
                true,
                false,
                true,
                BaUtilitiesConfig.HideDeadNpcMode.ALL_BA_NPCS,
                true,
                BaUtilitiesConfig.OverlayFont.ARIAL,
                16,
                defaultOverlayBackground()
        );

        applyHealerPreset(
                BaUtilitiesConfig.HighlightStyle.HULL,
                defaultHealerHighlightColor(),
                BaUtilitiesConfig.HealerLabelStyle.TIME_BASED_NUMBERING,
                false,
                true,
                true,
                true,
                BaUtilitiesConfig.FoodPanelStyle.SIMPLIFIED,
                true,
                true,
                BaUtilitiesConfig.DispenserOptions.BOTH,
                true,
                BaUtilitiesConfig.FoodCountType.COUNT_UP,
                defaultFoodCountColor(),
                100,
                BaUtilitiesConfig.HealerTtkDisplayMode.WAVE_TIME
        );

        applyAttackerPreset(true);
        applyDefenderPreset(true);
        applyScrollerPreset(true, true);
    }

    private void applyBaProPreset()
    {
        applyGeneralPreset(
                true,
                true,
                false,
                true,
                BaUtilitiesConfig.HideDeadNpcMode.NONE,
                false,
                BaUtilitiesConfig.OverlayFont.DEFAULT,
                16,
                defaultOverlayBackground()
        );

        applyHealerPreset(
                BaUtilitiesConfig.HighlightStyle.NONE,
                defaultHealerHighlightColor(),
                BaUtilitiesConfig.HealerLabelStyle.NONE,
                true,
                false,
                false,
                false,
                BaUtilitiesConfig.FoodPanelStyle.NONE,
                false,
                true,
                BaUtilitiesConfig.DispenserOptions.NONE,
                false,
                BaUtilitiesConfig.FoodCountType.COUNT_UP,
                defaultFoodCountColor(),
                35,
                BaUtilitiesConfig.HealerTtkDisplayMode.OFF
        );

        applyAttackerPreset(false);
        applyDefenderPreset(false);
        applyScrollerPreset(false, false);
    }

    private void applyGeneralPreset(
            boolean enableBaPartySync,
            boolean enablePartyChat,
            boolean hideSidePanelButton,
            boolean deprioritizeOtherDispensers,
            BaUtilitiesConfig.HideDeadNpcMode hideDeadNpcs,
            boolean showOverlayPanel,
            BaUtilitiesConfig.OverlayFont overlayFont,
            int overlayTextSize,
            Color overlayBackground)
    {
        set("enableBaPartySync", enableBaPartySync);
        set("enablePartyChat", enablePartyChat);
        set("hideSidePanelButton", hideSidePanelButton);
        set("deprioritizeOtherDispensers", deprioritizeOtherDispensers);
        set("hideDeadNpcs", hideDeadNpcs);
        set("showOverlayPanel", showOverlayPanel);
        set("overlayFont", overlayFont);
        set("foodPanelOverlayTextSize", overlayTextSize);
        set("foodPanelOverlayBackgroundColor", overlayBackground);
    }

    private void applyHealerPreset(
            BaUtilitiesConfig.HighlightStyle highlightStyle,
            Color highlightColor,
            BaUtilitiesConfig.HealerLabelStyle healerLabelStyle,
            boolean showLabelsAsHealerOnly,
            boolean spreadStackedLabels,
            boolean showMenuLabel,
            boolean showMenuCode,
            BaUtilitiesConfig.FoodPanelStyle foodPanelStyle,
            boolean healerFoodOnly,
            boolean highlightCalledDispenserFood,
            BaUtilitiesConfig.DispenserOptions dispenserOptions,
            boolean showFoodCountOnNpc,
            BaUtilitiesConfig.FoodCountType foodCountType,
            Color foodCountColor,
            int foodCountHeight,
            BaUtilitiesConfig.HealerTtkDisplayMode healerTtkDisplay)
    {
        set("highlightStyle", highlightStyle);
        set("hullColor", highlightColor);
        set("healerLabelStyle", healerLabelStyle);
        set("showLabelsAsHealerOnly", showLabelsAsHealerOnly);
        set("spreadStackedLabels", spreadStackedLabels);
        set("showMenuLabel", showMenuLabel);
        set("showMenuCode", showMenuCode);
        set("foodPanelStyle", foodPanelStyle);
        set("healerFoodOnly", healerFoodOnly);
        set("highlightCalledDispenserFood", highlightCalledDispenserFood);
        set("dispenserOptions", dispenserOptions);
        set("showFoodCountOnNpc", showFoodCountOnNpc);
        set("foodCountType", foodCountType);
        set("foodCountColor", foodCountColor);
        set("foodCountHeight", foodCountHeight);
        set("healerTtkDisplay", healerTtkDisplay);
    }

    private void applyAttackerPreset(boolean showSpawnCountOverlay)
    {
        set("showAttackerSpawnCountOverlay", showSpawnCountOverlay);
        set("attackerSpawnCountTextColor", Color.YELLOW);
        set("attackerSpawnCountTextSize", 18);
        set("attackerSpawnCountHorizontalOffset", 0);
        set("attackerSpawnCountHeightOffset", 200);
    }

    private void applyDefenderPreset(boolean highlightHammer)
    {
        set("highlightDefenderHammer", highlightHammer);
    }

    private void applyScrollerPreset(boolean highlightCurrentWaveLadder, boolean highlightOmegaDupeItems)
    {
        set("highlightScrollerLadder", highlightCurrentWaveLadder);
        set("highlightOmegaDupeItems", highlightOmegaDupeItems);
    }

    private Color defaultOverlayBackground()
    {
        return new Color(70, 61, 50, 156);
    }

    private Color defaultHealerHighlightColor()
    {
        return new Color(0, 200, 200);
    }

    private Color defaultFoodCountColor()
    {
        return new Color(0, 255, 0);
    }

    private void set(String key, Object value)
    {
        configManager.setConfiguration(BaUtilitiesConfig.GROUP_NAME, key, value);
    }
}
