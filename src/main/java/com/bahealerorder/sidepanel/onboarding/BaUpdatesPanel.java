package com.bahealerorder.sidepanel.onboarding;

import com.bahealerorder.common.BaIcons;
import com.bahealerorder.common.TileMarkerStyle;
import com.bahealerorder.sidepanel.BaPanelUi;
import com.bahealerorder.sidepanel.BaUtilitiesPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.StyleConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.SwingUtil;

@Singleton
public class BaUpdatesPanel extends JPanel
{
    private static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 13;
    private static final int INNER_WIDTH = CONTENT_WIDTH - 16;
    private static final int TEXT_WIDTH = CONTENT_WIDTH - 12;
    private static final int HEADER_BUTTON_SIZE = 24;
    private static final int CONTROL_HEIGHT = 28;
    private static final String RIGHT_ARROW = "\u2192";
    private static final Color ACTION_CONTROL_BACKGROUND_COLOR = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color ACTION_CONTROL_BORDER_COLOR = ColorScheme.MEDIUM_GRAY_COLOR;
    private static final Color ACTION_CONTROL_TEXT_COLOR = ColorScheme.TEXT_COLOR;

    private final BaAssistancePresetManager presetManager;
    private final List<JScrollPane> changelogScrollPanes = new ArrayList<>();
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

    @Override
    public void addNotify()
    {
        super.addNotify();
        SwingUtilities.invokeLater(this::refreshChangelogScrollBars);
    }

    private JPanel buildContent()
    {
        JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARK_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));

        panel.add(BaPanelUi.centeredHeader(
                "BA Utilities Updates",
                BaIcons.backIcon(),
                "Continue",
                this::continueUpdates,
                CONTENT_WIDTH,
                30,
                HEADER_BUTTON_SIZE
        ));
        panel.add(Box.createVerticalStrut(10));
        panel.add(introSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(changelogSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(continueButton());
        panel.add(Box.createVerticalStrut(30));
        panel.add(discordFeedbackButton());

        return panel;
    }

    private JPanel introSection()
    {
        JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARK_GRAY_COLOR, CONTENT_WIDTH);

        panel.add(BaPanelUi.textBlock(
                "BA Utilities has been updated! See below for details.",
                TEXT_WIDTH,
                40,
                StyleConstants.ALIGN_CENTER
        ));

        return panel;
    }

    private JPanel changelogSection()
    {
        JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR, CONTENT_WIDTH);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        panel.add(scrollableChangelogArea(buildLatestChangelogText(), 480));
        panel.add(Box.createVerticalStrut(8));
        panel.add(previousUpdatesSection());

        return panel;
    }

    private JTextArea changelogArea(String text, int height)
    {
        JTextArea changelog = styledChangelogArea(text);
        BaPanelUi.fixedSize(changelog, INNER_WIDTH, height);
        return changelog;
    }

    private JTextArea styledChangelogArea(String text)
    {
        JTextArea changelog = new JTextArea(text);
        changelog.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        changelog.setForeground(ColorScheme.TEXT_COLOR);
        changelog.setEditable(false);
        changelog.setFocusable(false);
        changelog.setLineWrap(true);
        changelog.setWrapStyleWord(true);
        changelog.setBorder(new EmptyBorder(6, 6, 6, 6));
        changelog.setAlignmentX(Component.LEFT_ALIGNMENT);
        return changelog;
    }

    private JPanel previousUpdatesSection()
    {
        JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(INNER_WIDTH, Integer.MAX_VALUE));

        JButton toggle = styledButton("Show Previous Updates", INNER_WIDTH);
        JPanel content = previousUpdatesContent();
        content.setVisible(false);
        toggle.addActionListener(event ->
        {
            boolean expanded = !content.isVisible();
            content.setVisible(expanded);
            toggle.setText(expanded ? "Hide Previous Updates" : "Show Previous Updates");
            revalidate();
            repaint();
        });

        panel.add(toggle);
        panel.add(content);
        return panel;
    }

    private JPanel previousUpdatesContent()
    {
        JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(INNER_WIDTH, Integer.MAX_VALUE));
        panel.add(Box.createVerticalStrut(8));

        panel.add(scrollableChangelogArea(buildPreviousChangelogText(), 260));

        return panel;
    }

    private JScrollPane scrollableChangelogArea(String text, int height)
    {
        JTextArea changelog = changelogArea(text, wrappedTextHeight(text));
        changelog.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(changelog);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        BaPanelUi.fixedSize(scrollPane, INNER_WIDTH, height);
        changelogScrollPanes.add(scrollPane);
        return scrollPane;
    }

    private void refreshChangelogScrollBars()
    {
        for (JScrollPane scrollPane : changelogScrollPanes)
        {
            SwingUtilities.updateComponentTreeUI(scrollPane);
            scrollPane.revalidate();
            scrollPane.repaint();
        }
    }

    private int wrappedTextHeight(String text)
    {
        JTextArea probe = styledChangelogArea(text);
        probe.setSize(new Dimension(INNER_WIDTH - PluginPanel.SCROLLBAR_WIDTH, Short.MAX_VALUE));
        return probe.getPreferredSize().height + 12;
    }

    private String buildLatestChangelogText()
    {
        return "2.3.2\n\n"
                + "\u2022 Fixed BA Party chat relaying game callouts and duplicating messages.\n\n"
                + "\u2022 Corrected built-in healer codes for waves 4, 7, 8, and 10.\n\n"
                + "2.3.1\n\n"
                + "\u2022 Updated tile marker set workflow and fixed side panel update flag and font cache growth\n\n"
                + "\u2022 Fixed minor bugs in the healer code editor.\n\n"
                + "\u2022 Adjusted tile marker chunk gridlines.\n\n"
                + "2.3.0\n\n"
                + "\u2022 Added onboarding and update side panel.\n\n"
                + "\u2022 Enhanced healer code parsing and introduced user-friendly healer code editor.\n\n"
                + "\u2022 Introduced highlight options for omega egg dupe and current wave ladder.\n\n"
                + "\u2022 Added public chat between BA Party members.\n\n"
                + "\u2022 Fixed a bug where renamed Penance NPCs could sometimes not be properly identified.";
    }

    private String buildPreviousChangelogText()
    {
        return "2.2.0\n\n"
                + "\u2022 Added general tile markers and role-based marker support.\n\n"
                + "\u2022 Added side-panel tools for configuring tile marker strategies.\n\n"
                + "\u2022 Improved Attacker cave marker positioning and controls.\n\n"
                + "2.1.0\n\n"
                + "\u2022 Added Healer Code configuration improvements.\n\n"
                + "\u2022 Improved built-in code strategy support.\n\n"
                + "\u2022 Added more flexible code editing and preview behavior.\n\n"
                + "2.0.0\n\n"
                + "\u2022 Expanded the plugin from Healer utilities into broader BA Utilities.\n\n"
                + "\u2022 Added support for additional Barbarian Assault roles.\n\n"
                + "\u2022 Added new role-aware overlays, menus, and side-panel tools.";
    }

    private JButton continueButton()
    {
        JButton button = styledButton("Continue to Plugin " + RIGHT_ARROW, CONTENT_WIDTH);
        button.addActionListener(event -> continueUpdates());

        return button;
    }

    private JButton discordFeedbackButton()
    {
        JButton button = new JButton("<html><div style='text-align:center;' width='" + (CONTENT_WIDTH - 50)
                + "'><u><font color='" + TileMarkerStyle.toHex(ColorScheme.BRAND_ORANGE)
                + "'>Join our discord</font></u> to report a bug or give us feedback and suggestions!</div></html>", BaIcons.discordIcon());
        SwingUtil.removeButtonDecorations(button);
        button.setMargin(new Insets(8, 0, 8, 0));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.addActionListener(event -> LinkBrowser.browse(BaUtilitiesPanel.DISCORD_URL));
        BaPanelUi.fixedSize(button, CONTENT_WIDTH, 64);
        return button;
    }

    private JButton styledButton(String text, int width)
    {
        JButton button = new JButton(text);
        button.setBackground(ACTION_CONTROL_BACKGROUND_COLOR);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setForeground(ACTION_CONTROL_TEXT_COLOR);
        button.setBorder(BorderFactory.createLineBorder(ACTION_CONTROL_BORDER_COLOR));
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(width, CONTROL_HEIGHT));
        button.setMaximumSize(new Dimension(width, CONTROL_HEIGHT));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    private void continueUpdates()
    {
        presetManager.markUpdateNotesSeen();

        if (continueCallback != null)
        {
            continueCallback.run();
        }
    }

}
