package com.bahealerorder.sidepanel.onboarding;

import com.bahealerorder.common.BaIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
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
        JPanel panel = flatPanel();

        panel.add(textBlock(
                "BA Utilities has been updated! See below for details.",
                TEXT_WIDTH,
                40,
                StyleConstants.ALIGN_CENTER
        ));

        return panel;
    }

    private JPanel changelogSection()
    {
        JPanel panel = section();

        panel.add(scrollableChangelogArea(buildLatestChangelogText(), 296));
        panel.add(Box.createVerticalStrut(8));
        panel.add(previousUpdatesSection());

        return panel;
    }

    private JTextArea changelogArea(String text, int height)
    {
        JTextArea changelog = styledChangelogArea(text);
        fixedSize(changelog, INNER_WIDTH, height);
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
        JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
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
        JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
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
        fixedSize(scrollPane, INNER_WIDTH, height);
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
        return "2.3.0\n\n"
                + "\u2022 Added assistance presets for different BA playstyles.\n\n"
                + "\u2022 Added this update panel for major BA Utilities changes.\n\n"
                + "\u2022 Added onboarding flow for new users near Barbarian Assault.\n\n"
                + "\u2022 Expanded side-panel setup guidance for Healer Codes and Tile Markers.";
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

    private JPanel header(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(ColorScheme.TEXT_COLOR);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        row.setPreferredSize(new Dimension(CONTENT_WIDTH, 30));
        row.setMaximumSize(new Dimension(CONTENT_WIDTH, 30));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(headerBackButton(), BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        row.add(headerSpacer(), BorderLayout.EAST);

        return row;
    }

    private JButton headerBackButton()
    {
        JButton button = new JButton(BaIcons.backIcon());
        button.setToolTipText("Continue");
        button.addActionListener(event -> continueUpdates());
        SwingUtil.removeButtonDecorations(button);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setBorderPainted(false);
        button.setBackground(ColorScheme.DARK_GRAY_COLOR);
        button.setUI(new BasicButtonUI());
        button.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent event)
            {
                button.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent event)
            {
                button.setBackground(ColorScheme.DARK_GRAY_COLOR);
            }
        });
        fixedSize(button, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE);
        return button;
    }

    private JPanel headerSpacer()
    {
        JPanel spacer = new JPanel();
        spacer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        fixedSize(spacer, HEADER_BUTTON_SIZE, HEADER_BUTTON_SIZE);
        return spacer;
    }

    private JPanel section()
    {
        JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel flatPanel()
    {
        JPanel panel = verticalPanel(ColorScheme.DARK_GRAY_COLOR);
        panel.setMaximumSize(new Dimension(CONTENT_WIDTH, Integer.MAX_VALUE));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JTextPane textBlock(String text, int width, int height, int alignment)
    {
        JTextPane textPane = new JTextPane();
        textPane.setText(text);
        textPane.setForeground(ColorScheme.TEXT_COLOR);
        textPane.setEditable(false);
        textPane.setFocusable(false);
        textPane.setOpaque(false);
        textPane.setBorder(null);

        StyledDocument document = textPane.getStyledDocument();
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setAlignment(attributes, alignment);
        StyleConstants.setBold(attributes, false);
        document.setParagraphAttributes(0, document.getLength(), attributes, false);
        document.setCharacterAttributes(0, document.getLength(), attributes, false);

        fixedSize(textPane, width, height);
        return textPane;
    }

    private void fixedSize(JComponent component, int width, int height)
    {
        Dimension size = new Dimension(width, height);
        component.setPreferredSize(size);
        component.setMinimumSize(size);
        component.setMaximumSize(size);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JPanel verticalPanel(Color background)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);
        return panel;
    }
}
