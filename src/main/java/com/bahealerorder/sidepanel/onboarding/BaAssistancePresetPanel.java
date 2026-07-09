package com.bahealerorder.sidepanel.onboarding;

import com.bahealerorder.BaUtilitiesConfig;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

@Singleton
public class BaAssistancePresetPanel extends JPanel
{
    private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
    private static final int INNER_WIDTH = CONTENT_WIDTH - 16;
    private static final int BUTTON_HEIGHT = 36;
    private static final int DESCRIPTION_HEIGHT = 66;
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
        JPanel panel = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

        panel.add(header("Choose Assistance Level"));
        panel.add(Box.createVerticalStrut(10));
        panel.add(introSection());
        panel.add(Box.createVerticalStrut(10));

        panel.add(presetCard(
                "Beginner",
                "I'm new to BA and want basic help with each role.",
                BaUtilitiesConfig.AssistancePreset.BEGINNER
        ));

        panel.add(Box.createVerticalStrut(8));

        panel.add(presetCard(
                "Intermediate",
                "I understand the basics of BA and want helpful overlays without every optional feature enabled.",
                BaUtilitiesConfig.AssistancePreset.INTERMEDIATE
        ));

        panel.add(Box.createVerticalStrut(8));

        panel.add(presetCard(
                "Recommended",
                "All features configured to recommended settings by the developers of this plugin to maximize the BA experience.",
                BaUtilitiesConfig.AssistancePreset.RECOMMENDED
        ));

        panel.add(Box.createVerticalStrut(8));

        panel.add(presetCard(
                "BA Pro (Minimum Assistance)",
                "Only QOL options are turned on, all \"overpowered\" features disabled.",
                BaUtilitiesConfig.AssistancePreset.BA_PRO
        ));

        panel.add(Box.createVerticalStrut(12));
        panel.add(skipButton());

        return panel;
    }

    private JPanel introSection()
    {
        JPanel panel = section();

        panel.add(message(
                "BA Utilities is designed to be beginner-friendly, but the more you understand Barbarian Assault, the more powerful the features can be.",
                ColorScheme.DARKER_GRAY_COLOR,
                82,
                false
        ));

        panel.add(Box.createVerticalStrut(8));

        panel.add(message(
                "Choose a starting preset below. You can still customize individual settings afterward.",
                ColorScheme.DARKER_GRAY_COLOR,
                56,
                false
        ));

        panel.add(Box.createVerticalStrut(8));

        panel.add(message(
                "Note for non-beginners: After choosing a preset, be sure to check the Healer Code and Tile Markers options in the side panel to ensure they fit your specific playstyle.",
                ColorScheme.DARKER_GRAY_COLOR,
                104,
                false
        ));

        return panel;
    }

    private JPanel presetCard(String title, String description, BaUtilitiesConfig.AssistancePreset preset)
    {
        JPanel panel = section();

        JButton button = styledButton(title, INNER_WIDTH);
        button.addActionListener(event ->
        {
            presetManager.applyPreset(preset);
            runPresetSelectedCallback();
        });

        panel.add(button);
        panel.add(Box.createVerticalStrut(5));
        panel.add(message(description, ColorScheme.DARKER_GRAY_COLOR, DESCRIPTION_HEIGHT, false));

        return panel;
    }

    private JButton skipButton()
    {
        JButton button = styledButton("Skip - Keep My Current Settings", CONTENT_WIDTH);
        button.addActionListener(event ->
        {
            presetManager.skipAssistancePreset();
            runPresetSelectedCallback();
        });

        return button;
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

    private JPanel header(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(ColorScheme.TEXT_COLOR);
        label.setFont(FontManager.getRunescapeBoldFont());
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        row.setPreferredSize(new Dimension(CONTENT_WIDTH, 30));
        row.setMaximumSize(new Dimension(CONTENT_WIDTH, 30));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(label, BorderLayout.CENTER);

        return row;
    }

    private JPanel section()
    {
        JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JTextArea message(String text, Color background, int height, boolean opaque)
    {
        JTextArea message = new JTextArea(text);
        message.setBackground(background);
        message.setForeground(ColorScheme.TEXT_COLOR);
        message.setFont(FontManager.getRunescapeSmallFont());
        message.setEditable(false);
        message.setFocusable(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setOpaque(opaque);
        message.setBorder(null);
        message.setPreferredSize(new Dimension(INNER_WIDTH, height));
        message.setMaximumSize(new Dimension(INNER_WIDTH, height));
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        return message;
    }

    private JPanel verticalPanel(Color background)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);
        return panel;
    }
}