package com.bahealerorder.sidepanel.onboarding;

import com.bahealerorder.BaUtilitiesConfig;
import com.bahealerorder.common.BaIcons;
import com.bahealerorder.sidepanel.BaPanelUi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

@Singleton
public class BaAssistancePresetPanel extends JPanel
{
    private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
    private static final int TEXT_WIDTH = CONTENT_WIDTH - 12;
    private static final int HEADER_BUTTON_SIZE = 24;
    private static final int BUTTON_HEIGHT = 36;
    private static final int INTRO_TO_PRESETS_SPACING = 20;
    private static final int PRESET_SPACING = 20;
    private static final int PRESET_DESCRIPTION_HEIGHT = 66;
    private static final int PRESET_SECTION_HEIGHT = BUTTON_HEIGHT + 5 + PRESET_DESCRIPTION_HEIGHT;
    private static final int DIVIDER_LINE_WIDTH = 78;
    private static final int DIVIDER_HEIGHT = 12;
    private static final String RIGHT_ARROW = "\u2192";
    private static final Color ACTION_CONTROL_BACKGROUND_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color ACTION_CONTROL_HOVER_BACKGROUND_COLOR = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color ACTION_CONTROL_BORDER_COLOR = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color ACTION_CONTROL_TEXT_COLOR = ColorScheme.TEXT_COLOR;

    private final BaAssistancePresetManager presetManager;
    private Runnable presetSelectedCallback;

    @Inject
    public BaAssistancePresetPanel(BaAssistancePresetManager presetManager)
    {
        this.presetManager = presetManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(5, 5, 5, 5));

        add(buildContent(), BorderLayout.NORTH);
    }

    public void setPresetSelectedCallback(Runnable presetSelectedCallback)
    {
        this.presetSelectedCallback = presetSelectedCallback;
    }

    private JPanel buildContent()
    {
        JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARK_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

        panel.add(BaPanelUi.centeredHeader(
                "Choose Assistance Level",
                BaIcons.backIcon(),
                "Skip",
                this::skipAssistancePreset,
                CONTENT_WIDTH,
                30,
                HEADER_BUTTON_SIZE
        ));
        panel.add(Box.createVerticalStrut(10));
        panel.add(introSection());
        panel.add(Box.createVerticalStrut(INTRO_TO_PRESETS_SPACING));

        panel.add(presetCard(
                "Beginner",
                "I'm new to BA and want basic help with each role.",
                BaUtilitiesConfig.AssistancePreset.BEGINNER
        ));

        panel.add(Box.createVerticalStrut(PRESET_SPACING));

        panel.add(presetCard(
                "Intermediate",
                "I understand the basics of BA and want helpful overlays without every optional feature enabled.",
                BaUtilitiesConfig.AssistancePreset.INTERMEDIATE
        ));

        panel.add(Box.createVerticalStrut(PRESET_SPACING));

        panel.add(presetCard(
                "Recommended",
                "Use recommended settings to maximize the BA experience.",
                BaUtilitiesConfig.AssistancePreset.RECOMMENDED
        ));

        panel.add(Box.createVerticalStrut(PRESET_SPACING));

        panel.add(presetCard(
                "BA Pro (Minimum Assistance)",
                "Only enable QoL options. All \"overpowered\" features disabled.",
                BaUtilitiesConfig.AssistancePreset.BA_PRO
        ));

        panel.add(Box.createVerticalStrut(PRESET_SPACING));
        panel.add(divider());
        panel.add(Box.createVerticalStrut(PRESET_SPACING));
        panel.add(skipButton());
        panel.add(Box.createVerticalStrut(PRESET_SPACING * 2));
        panel.add(nonBeginnerNote());

        return panel;
    }

    private JPanel introSection()
    {
        JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARK_GRAY_COLOR, CONTENT_WIDTH);

        panel.add(textBlock(
                "BA Utilities is designed to be beginner-friendly, but the more you understand Barbarian Assault, the more powerful the features can be.",
                TEXT_WIDTH,
                84,
                StyleConstants.ALIGN_LEFT
        ));

        panel.add(Box.createVerticalStrut(8));

        panel.add(textBlock(
                "Choose a starting preset below. You can still customize individual settings afterward.",
                TEXT_WIDTH,
                48,
                StyleConstants.ALIGN_LEFT,
                "starting preset"
        ));

        return panel;
    }

    private JPanel nonBeginnerNote()
    {
        JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARK_GRAY_COLOR, CONTENT_WIDTH);
        panel.add(textBlock(
                "Note for Non-Beginners",
                TEXT_WIDTH,
                24,
                StyleConstants.ALIGN_CENTER
        ));
        panel.add(textBlock(
                "After choosing a preset, be sure to check the Healer Code and Tile Markers options in the side panel to ensure they fit your specific playstyle.",
                TEXT_WIDTH,
                96,
                StyleConstants.ALIGN_LEFT,
                "Healer Code",
                "Tile Markers"
        ));
        return panel;
    }

    private JPanel presetCard(String title, String description, BaUtilitiesConfig.AssistancePreset preset)
    {
        JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARK_GRAY_COLOR, CONTENT_WIDTH);

        JButton button = styledButton(title, CONTENT_WIDTH);
        button.addActionListener(event ->
        {
            presetManager.applyPreset(preset);
            runPresetSelectedCallback();
        });

        panel.add(button);
        panel.add(Box.createVerticalStrut(5));
        panel.add(textBlock(description, TEXT_WIDTH, PRESET_DESCRIPTION_HEIGHT, StyleConstants.ALIGN_CENTER, highlightPhrase(preset)));
        BaPanelUi.fixedSize(panel, CONTENT_WIDTH, PRESET_SECTION_HEIGHT);

        return panel;
    }

    private JButton skipButton()
    {
        JButton button = styledButton("Keep My Current Settings " + RIGHT_ARROW, CONTENT_WIDTH);
        button.addActionListener(event -> skipAssistancePreset());

        return button;
    }

    private JPanel divider()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        BaPanelUi.fixedSize(panel, CONTENT_WIDTH, DIVIDER_HEIGHT);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.gridx = 0;
        panel.add(dividerLine(), constraints);

        JLabel label = new JLabel("or");
        label.setForeground(ColorScheme.TEXT_COLOR);
        constraints.gridx = 1;
        constraints.insets = new Insets(0, 8, 0, 8);
        panel.add(label, constraints);

        constraints.gridx = 2;
        constraints.insets = new Insets(0, 0, 0, 0);
        panel.add(dividerLine(), constraints);

        return panel;
    }

    private JComponent dividerLine()
    {
        JComponent line = new JComponent()
        {
            @Override
            protected void paintComponent(Graphics graphics)
            {
                super.paintComponent(graphics);
                graphics.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
                graphics.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
            }
        };
        BaPanelUi.fixedSize(line, DIVIDER_LINE_WIDTH, DIVIDER_HEIGHT);
        return line;
    }

    private void skipAssistancePreset()
    {
        presetManager.skipAssistancePreset();
        runPresetSelectedCallback();
    }

    private JButton styledButton(String text, int width)
    {
        JButton button = new JButton(text);
        button.setBackground(ACTION_CONTROL_BACKGROUND_COLOR);
        button.setForeground(ACTION_CONTROL_TEXT_COLOR);
        button.setBorder(BorderFactory.createLineBorder(ACTION_CONTROL_BORDER_COLOR));
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setPreferredSize(new Dimension(width, BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(width, BUTTON_HEIGHT));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent event)
            {
                button.setBackground(ACTION_CONTROL_HOVER_BACKGROUND_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent event)
            {
                button.setBackground(ACTION_CONTROL_BACKGROUND_COLOR);
            }
        });

        return button;
    }

    private void runPresetSelectedCallback()
    {
        if (presetSelectedCallback != null)
        {
            presetSelectedCallback.run();
        }
    }

    private String highlightPhrase(BaUtilitiesConfig.AssistancePreset preset)
    {
        switch (preset)
        {
            case BEGINNER:
                return "new to BA";
            case INTERMEDIATE:
                return "helpful overlays";
            case RECOMMENDED:
                return "recommended settings";
            case BA_PRO:
                return "QoL options";
            default:
                return null;
        }
    }

    private JTextPane textBlock(String text, int width, int height, int alignment, String... highlights)
    {
        JTextPane textPane = BaPanelUi.textBlock(text, width, height, alignment);
        StyledDocument document = textPane.getStyledDocument();
        highlightTerms(document, text, highlights);
        boldTerms(document, text, "Note for Non-Beginners");
        return textPane;
    }

    private void highlightTerms(StyledDocument document, String text, String... highlights)
    {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, ColorScheme.BRAND_ORANGE);
        StyleConstants.setBold(attributes, true);

        for (String highlight : highlights)
        {
            if (highlight == null || highlight.isEmpty()) continue;

            int index = text.indexOf(highlight);
            while (index >= 0)
            {
                document.setCharacterAttributes(index, highlight.length(), attributes, false);
                index = text.indexOf(highlight, index + highlight.length());
            }
        }
    }

    private void boldTerms(StyledDocument document, String text, String... terms)
    {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setBold(attributes, true);

        for (String term : terms)
        {
            int index = text.indexOf(term);
            while (index >= 0)
            {
                document.setCharacterAttributes(index, term.length(), attributes, false);
                index = text.indexOf(term, index + term.length());
            }
        }
    }

}
