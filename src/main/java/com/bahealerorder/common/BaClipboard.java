package com.bahealerorder.common;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.JOptionPane;

public final class BaClipboard
{
	private BaClipboard()
	{
	}

	public static void copyText(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	public static String readText(Component parent, String title)
	{
		try
		{
			return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		}
		catch (UnsupportedFlavorException | IOException | RuntimeException ex)
		{
			JOptionPane.showMessageDialog(parent, "Clipboard does not contain valid text.", title, JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
}
