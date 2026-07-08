package com.bahealerorder.sidepanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import net.runelite.client.ui.ColorScheme;

public final class BaTransferDialog
{
	private static final int WIDTH = 285;

	private BaTransferDialog()
	{
	}

	public static void show(Component parent, String title, String message, String action, List<String> summaryLines)
	{
		if (summaryLines == null || summaryLines.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		Window owner = SwingUtilities.getWindowAncestor(parent);
		JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(12, 12, 12, 12));

		JTextArea summary = new JTextArea(summaryText(summaryLines));
		summary.setEditable(false);
		summary.setLineWrap(true);
		summary.setWrapStyleWord(true);
		summary.setBackground(ColorScheme.DARK_GRAY_COLOR);
		summary.setForeground(ColorScheme.TEXT_COLOR);
		summary.setBorder(new EmptyBorder(6, 6, 6, 6));

		JScrollPane scrollPane = new JScrollPane(summary);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
		scrollPane.setPreferredSize(new Dimension(WIDTH, Math.min(220, 28 + summaryLines.size() * 20)));
		scrollPane.setVisible(false);

		JEditorPane messagePane = new JEditorPane();
		messagePane.setContentType("text/html");
		messagePane.setEditable(false);
		messagePane.setOpaque(false);
		messagePane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		messagePane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		messagePane.setText(messageHtml(message, action, false));
		messagePane.addHyperlinkListener(event ->
		{
			if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED)
			{
				return;
			}

			scrollPane.setVisible(!scrollPane.isVisible());
			messagePane.setText(messageHtml(message, action, scrollPane.isVisible()));
			dialog.pack();
			dialog.setLocationRelativeTo(parent);
		});

		JButton ok = new JButton("OK");
		BaPanelUi.styleActionButton(ok);
		ok.addActionListener(event -> dialog.dispose());

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bottom.add(ok, BorderLayout.EAST);

		panel.add(messagePane, BorderLayout.NORTH);
		panel.add(scrollPane, BorderLayout.CENTER);
		panel.add(bottom, BorderLayout.SOUTH);
		dialog.setContentPane(panel);
		dialog.pack();
		dialog.setMinimumSize(new Dimension(WIDTH + 40, dialog.getHeight()));
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	private static String summaryText(List<String> summaryLines)
	{
		StringBuilder builder = new StringBuilder();
		for (String line : summaryLines)
		{
			if (builder.length() > 0)
			{
				builder.append('\n');
			}
			builder.append(line);
		}
		return builder.toString();
	}

	private static String messageHtml(String message, String action, boolean summaryVisible)
	{
		String linkText = (summaryVisible ? "Hide " : "Show ") + action + " Summary";
		return "<html><head><style>"
				+ "body { color:#" + hex(ColorScheme.TEXT_COLOR) + "; margin:0; width:" + WIDTH + "px; }"
				+ "a { color:#" + hex(ColorScheme.BRAND_ORANGE) + "; }"
				+ "</style></head><body>"
				+ escapeHtml(message)
				+ " <a href='summary'>"
				+ escapeHtml(linkText)
				+ "</a>"
				+ "</body></html>";
	}

	private static String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}

		return text
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

	private static String hex(java.awt.Color color)
	{
		return String.format("%06x", color.getRGB() & 0xFFFFFF);
	}
}
