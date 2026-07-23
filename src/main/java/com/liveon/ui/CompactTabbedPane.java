package com.liveon.ui;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JTabbedPane;

/** JTabbedPane whose height follows only the visible tab. */
public class CompactTabbedPane extends JTabbedPane
{
    private static final int TAB_HEADER_HEIGHT = 30;

    public CompactTabbedPane()
    {
        addChangeListener(event ->
        {
            revalidate();
            if (getParent() != null)
            {
                getParent().revalidate();
            }
        });
    }

    @Override
    public Dimension getPreferredSize()
    {
        Component selected = getSelectedComponent();
        Dimension content = selected == null ? new Dimension(0, 0) : selected.getPreferredSize();
        int availableWidth = getParent() == null ? 0 : getParent().getWidth();
        int width = availableWidth > 0 ? availableWidth : Math.min(210, Math.max(0, content.width));
        return new Dimension(width, content.height + TAB_HEADER_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize()
    {
        return new Dimension(0, getPreferredSize().height);
    }

    @Override
    public Dimension getMaximumSize()
    {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
