package com.liveon.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

public final class PanelStyles
{
    public static final Color GOLD = new Color(255, 200, 61);
    public static final Color MUTED = new Color(170, 170, 170);

    private PanelStyles()
    {
    }

    public static JPanel verticalPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        return panel;
    }

    public static JPanel card()
    {
        JPanel panel = verticalPanel();
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    public static JLabel title(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(GOLD);
        label.setFont(FontManager.getRunescapeBoldFont());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel text(String text)
    {
        JLabel label = new JLabel("<html>" + escape(text) + "</html>");
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel muted(String text)
    {
        JLabel label = text(text);
        label.setForeground(MUTED);
        return label;
    }

    public static void addGap(JPanel panel)
    {
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
    }

    public static JPanel line(String left, String right)
    {
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        JLabel leftLabel = new JLabel(left);
        leftLabel.setForeground(Color.WHITE);
        JLabel rightLabel = new JLabel(right);
        rightLabel.setForeground(GOLD);
        line.add(leftLabel, BorderLayout.WEST);
        line.add(rightLabel, BorderLayout.EAST);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    public static void showMessage(JPanel panel, String message)
    {
        panel.removeAll();
        panel.add(muted(message));
        panel.revalidate();
        panel.repaint();
    }

    public static String escape(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
