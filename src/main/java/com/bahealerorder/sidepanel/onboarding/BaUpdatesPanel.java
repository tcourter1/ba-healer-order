package com.bahealerorder.sidepanel.onboarding;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

@Singleton
public class BaUpdatesPanel extends JPanel
{
    private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
    private static final int INNER_WIDTH = CONTENT_WIDTH - 16;
    private static final int CONTROL_HEIGHT = 28;
    private static final Color ACTION_CONTROL_BACKGROUND_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color ACTION_CONTROL_BORDER_COLOR = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color ACTION_CONTROL_TEXT_COLOR = ColorScheme.TEXT_COLOR;

    private final BaAssistancePresetManager presetManager;
    private Runnable continueCallback;

    @Inject
    public BaUpdatesPanel(BaAssistancePresetManager presetManager)
    {
        this.presetManager = presetManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(5, 5, 5, 5));

        add(buildContent(), BorderLayout.NORTH);
    }

    public void setContinueCallback(Runnable continueCallback)
    {
        this.continueCallback = continueCallback;
    }

    private JPanel buildContent()
    {
        JPanel panel = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

        panel.add(header("BA Utilities Updates"));
        panel.add(Box.createVerticalStrut(10));
        panel.add(introSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(changelogSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(continueButton());

        return panel;
    }

    private JPanel introSection()
    {
        JPanel panel = section();

        panel.add(message(
                "Here are the latest major BA Utilities changes. This panel will appear after major updates so new features are easier to discover.",
                ColorScheme.DARKER_GRAY_COLOR,
                100,
                false
        ));

        return panel;
    }

    private JPanel changelogSection()
    {
        JPanel panel = section();

        JTextArea changelog = new JTextArea(buildChangelogText());
        changelog.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        changelog.setForeground(ColorScheme.TEXT_COLOR);
        changelog.setFont(FontManager.getRunescapeFont());
        changelog.setEditable(false);
        changelog.setFocusable(false);
        changelog.setLineWrap(true);
        changelog.setWrapStyleWord(true);
        changelog.setBorder(new EmptyBorder(6, 6, 6, 6));

        JScrollPane scrollPane = new JScrollPane(changelog);
        scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
        scrollPane.setPreferredSize(new Dimension(INNER_WIDTH, 560));
        scrollPane.setMaximumSize(new Dimension(INNER_WIDTH, 560));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(scrollPane);

        return panel;
    }

    private String buildChangelogText()
    {
        return "2.3.0\n"
                + "- Added assistance presets for different BA playstyles.\n"
                + "- Added this update panel for major BA Utilities changes.\n"
                + "- Added onboarding flow for new users near Barbarian Assault.\n"
                + "- Expanded side-panel setup guidance for Healer Codes and Tile Markers.\n"
                + "\n"
                + "2.2.0\n"
                + "- Added general tile markers and role-based marker support.\n"
                + "- Added side-panel tools for configuring tile marker strategies.\n"
                + "- Improved Attacker cave marker positioning and controls.\n"
                + "\n"
                + "2.1.0\n"
                + "- Added Healer Code configuration improvements.\n"
                + "- Improved built-in code strategy support.\n"
                + "- Added more flexible code editing and preview behavior.\n"
                + "\n"
                + "2.0.0\n"
                + "- Expanded the plugin from Healer utilities into broader BA Utilities.\n"
                + "- Added support for additional Barbarian Assault roles.\n"
                + "- Added new role-aware overlays, menus, and side-panel tools.";
    }

    private JButton continueButton()
    {
        JButton button = new JButton("Continue");
        button.setBackground(ACTION_CONTROL_BACKGROUND_COLOR);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setForeground(ACTION_CONTROL_TEXT_COLOR);
        button.setBorder(BorderFactory.createLineBorder(ACTION_CONTROL_BORDER_COLOR));
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(CONTENT_WIDTH, CONTROL_HEIGHT));
        button.setMaximumSize(new Dimension(CONTENT_WIDTH, CONTROL_HEIGHT));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event ->
        {
            presetManager.markUpdateNotesSeen();

            if (continueCallback != null)
            {
                continueCallback.run();
            }
        });

        return button;
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
        message.setFont(FontManager.getRunescapeFont());
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