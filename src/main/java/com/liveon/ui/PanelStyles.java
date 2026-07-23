package com.liveon.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.geom.Ellipse2D;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import net.runelite.client.ui.FontManager;

/** Shared Live On visual language. Change the palette here to retheme the entire panel. */
public final class PanelStyles
{
    public static final Color BACKGROUND = new Color(9, 6, 17);
    public static final Color SURFACE = new Color(21, 16, 34);
    public static final Color SURFACE_RAISED = new Color(34, 26, 55);
    public static final Color BORDER = new Color(92, 63, 135);
    public static final Color PURPLE = new Color(185, 140, 255);
    public static final Color PURPLE_STRONG = new Color(124, 58, 237);
    public static final Color GOLD = new Color(242, 193, 78);
    public static final Color GREEN = new Color(53, 214, 164);
    public static final Color RED = new Color(255, 112, 122);
    public static final Color TEXT = new Color(244, 239, 255);
    public static final Color MUTED = new Color(166, 151, 188);

    private PanelStyles()
    {
    }

    public static JPanel verticalPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    public static JPanel card()
    {
        return new RoundedPanel(SURFACE, BORDER, 12, false);
    }

    public static JPanel heroCard()
    {
        return new RoundedPanel(SURFACE, PURPLE_STRONG, 14, true);
    }

    public static JLabel title(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(FontManager.getRunescapeBoldFont());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel sectionTitle(String text, UiIcon icon)
    {
        JLabel label = title(text);
        label.setIcon(icon(icon, PURPLE, 16));
        label.setIconTextGap(7);
        return label;
    }

    public static JLabel text(String text)
    {
        JLabel label = new JLabel("<html><body style='width:190px'>" + escape(text) + "</body></html>");
        label.setForeground(TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel muted(String text)
    {
        JLabel label = text(text);
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel badge(String text, Color color)
    {
        JLabel label = new JLabel(" " + text + " ");
        label.setForeground(color);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120)),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        return label;
    }

    public static JButton button(String text, UiIcon icon)
    {
        JButton button = new LiveButton(text);
        if (text != null && !text.isEmpty())
        {
            button.setHorizontalAlignment(SwingConstants.LEFT);
        }
        if (icon != null)
        {
            button.setIcon(icon(icon, TEXT, 14));
            button.setIconTextGap(6);
        }
        return button;
    }

    public static JButton secondaryButton(String text, UiIcon icon)
    {
        JButton button = button(text, icon);
        button.putClientProperty("secondary", Boolean.TRUE);
        return button;
    }

    public static void styleField(JTextField field)
    {
        field.setBackground(SURFACE);
        field.setForeground(TEXT);
        field.setCaretColor(PURPLE);
        field.setSelectionColor(PURPLE_STRONG);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(5, 7, 5, 7)));
    }

    public static void styleArea(JTextArea area)
    {
        area.setBackground(SURFACE);
        area.setForeground(TEXT);
        area.setCaretColor(PURPLE);
        area.setSelectionColor(PURPLE_STRONG);
        area.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(6, 7, 6, 7)));
    }

    public static void styleCombo(JComboBox<?> combo)
    {
        combo.setBackground(SURFACE);
        combo.setForeground(TEXT);
        combo.setFocusable(false);
        combo.setBorder(BorderFactory.createLineBorder(BORDER));
        combo.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                label.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
                label.setBackground(selected ? PURPLE_STRONG : SURFACE);
                label.setForeground(TEXT);
                return label;
            }
        });
    }

    public static void styleTabs(JTabbedPane tabs)
    {
        tabs.setBackground(BACKGROUND);
        tabs.setForeground(TEXT);
        tabs.setBorder(BorderFactory.createLineBorder(BORDER));
        tabs.setUI(new BasicTabbedPaneUI()
        {
            @Override
            protected void installDefaults()
            {
                super.installDefaults();
                tabAreaInsets = new Insets(3, 3, 0, 3);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(1, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics graphics, int placement, int index, int x, int y, int width, int height, boolean selected)
            {
                graphics.setColor(selected ? SURFACE_RAISED : BACKGROUND);
                graphics.fillRect(x, y, width, height);
                if (selected)
                {
                    graphics.setColor(GOLD);
                    graphics.fillRect(x + 4, y + height - 2, width - 8, 2);
                }
            }

            @Override
            protected void paintFocusIndicator(Graphics graphics, int placement, Rectangle[] rectangles, int index, Rectangle icon, Rectangle text, boolean focused)
            {
                // The gold underline already communicates selection.
            }
        });
    }

    public static void addGap(JPanel panel)
    {
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    public static JPanel line(String left, String right)
    {
        JPanel line = new JPanel(new BorderLayout(5, 0));
        line.setOpaque(false);
        JLabel leftLabel = new JLabel(left);
        leftLabel.setForeground(TEXT);
        JLabel rightLabel = new JLabel(right);
        rightLabel.setForeground(GOLD);
        rightLabel.setFont(FontManager.getRunescapeBoldFont());
        line.add(leftLabel, BorderLayout.WEST);
        line.add(rightLabel, BorderLayout.EAST);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    public static void showMessage(JPanel panel, String message)
    {
        panel.removeAll();
        JPanel card = card();
        card.add(muted(message == null ? "Algo deu errado." : message));
        panel.add(card);
        panel.revalidate();
        panel.repaint();
    }

    public static Icon icon(UiIcon type, Color color, int size)
    {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(Math.max(1.4f, size / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int m = 2;
        switch (type)
        {
            case HOME:
                graphics.drawLine(m, size / 2, size / 2, m);
                graphics.drawLine(size / 2, m, size - m, size / 2);
                graphics.drawRect(size / 4, size / 2, size / 2, size / 2 - m);
                break;
            case DROP:
                graphics.drawRoundRect(m, size / 3, size - 2 * m, size - size / 3 - m, 3, 3);
                graphics.drawLine(m, size / 3, size / 2, size / 2);
                graphics.drawLine(size - m, size / 3, size / 2, size / 2);
                break;
            case PEOPLE:
                graphics.drawOval(size / 3, m, size / 3, size / 3);
                graphics.drawArc(m, size / 3, size - 2 * m, size - size / 3, 200, 140);
                break;
            case TROPHY:
                graphics.drawRect(size / 3, m, size / 3, size / 2);
                graphics.drawArc(m, m, size / 3, size / 2, 90, 180);
                graphics.drawArc(size * 2 / 3, m, size / 3 - m, size / 2, 270, 180);
                graphics.drawLine(size / 2, size / 2, size / 2, size - 3);
                graphics.drawLine(size / 3, size - 3, size * 2 / 3, size - 3);
                break;
            case MEGAPHONE:
                graphics.drawLine(m, size / 2, size - 3, m + 1);
                graphics.drawLine(m, size / 2, size - 3, size - m);
                graphics.drawLine(size - 3, m + 1, size - 3, size - m);
                graphics.drawLine(size / 3, size / 2, size / 3, size - 2);
                break;
            case SEARCH:
                graphics.drawOval(m, m, size - 6, size - 6);
                graphics.drawLine(size - 5, size - 5, size - 2, size - 2);
                break;
            case REFRESH:
                graphics.drawArc(m, m, size - 2 * m, size - 2 * m, 40, 285);
                graphics.drawLine(size - 3, m + 2, size - 3, size / 2);
                graphics.drawLine(size - 3, m + 2, size / 2, m + 2);
                break;
            case TARGET:
                graphics.drawOval(m, m, size - 2 * m, size - 2 * m);
                graphics.drawOval(size / 3, size / 3, size / 3, size / 3);
                graphics.fillOval(size / 2 - 1, size / 2 - 1, 3, 3);
                break;
            case IMAGE:
                graphics.drawRoundRect(m, m, size - 2 * m, size - 2 * m, 3, 3);
                graphics.drawLine(m + 2, size - 4, size / 2, size / 2);
                graphics.drawLine(size / 2, size / 2, size - 3, size - 4);
                graphics.fillOval(size - 6, m + 2, 3, 3);
                break;
            case CROWN:
                graphics.drawLine(m, size - 4, size - m, size - 4);
                graphics.drawLine(m, size - 4, m, size / 3);
                graphics.drawLine(m, size / 3, size / 3, size / 2);
                graphics.drawLine(size / 3, size / 2, size / 2, m);
                graphics.drawLine(size / 2, m, size * 2 / 3, size / 2);
                graphics.drawLine(size * 2 / 3, size / 2, size - m, size / 3);
                graphics.drawLine(size - m, size / 3, size - m, size - 4);
                break;
            default:
                graphics.fillOval(m, m, size - 2 * m, size - 2 * m);
        }
        graphics.dispose();
        return new ImageIcon(image);
    }

    public static ImageIcon circularIcon(BufferedImage source, int size)
    {
        BufferedImage circle = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = circle.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setClip(new Ellipse2D.Float(0, 0, size, size));
        graphics.drawImage(source.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();
        return new ImageIcon(circle);
    }

    public static String escape(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public enum UiIcon { HOME, DROP, PEOPLE, TROPHY, MEGAPHONE, SEARCH, REFRESH, TARGET, IMAGE, CROWN }

    private static final class RoundedPanel extends JPanel
    {
        private final Color fill;
        private final Color outline;
        private final int radius;
        private final boolean gradient;

        private RoundedPanel(Color fill, Color outline, int radius, boolean gradient)
        {
            this.fill = fill;
            this.outline = outline;
            this.radius = radius;
            this.gradient = gradient;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (gradient)
            {
                copy.setPaint(new GradientPaint(0, 0, SURFACE_RAISED, getWidth(), getHeight(), BACKGROUND));
            }
            else
            {
                copy.setColor(fill);
            }
            copy.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            copy.setColor(outline);
            copy.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            copy.dispose();
            super.paintComponent(graphics);
        }

        @Override
        public Dimension getMaximumSize()
        {
            Dimension preferred = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, preferred.height);
        }
    }

    private static final class LiveButton extends JButton
    {
        private LiveButton(String text)
        {
            super(text);
            setForeground(TEXT);
            setFont(FontManager.getRunescapeBoldFont());
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean secondary = Boolean.TRUE.equals(getClientProperty("secondary"));
            Color top = secondary ? SURFACE_RAISED : PURPLE_STRONG;
            Color bottom = secondary ? SURFACE : new Color(88, 39, 176);
            if (getModel().isRollover())
            {
                top = secondary ? new Color(52, 38, 79) : new Color(145, 82, 244);
            }
            copy.setPaint(new GradientPaint(0, 0, top, getWidth(), getHeight(), bottom));
            copy.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
            copy.setColor(secondary ? BORDER : PURPLE);
            copy.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
            copy.dispose();
            super.paintComponent(graphics);
        }
    }
}
