package com.liveon.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JPanel;

/**
 * Fixed-height cells that wrap according to the actual RuneLite sidebar width.
 *
 * Swing's GridLayout stretches short lists to the height of the largest tab.
 * This panel keeps compact cells and recalculates the column count on resize.
 */
public class ResponsiveGridPanel extends JPanel
{
    private final int minimumCellWidth;
    private final int cellHeight;
    private final int maximumColumns;
    private final int gap;

    public ResponsiveGridPanel(int minimumCellWidth, int cellHeight, int maximumColumns, int gap)
    {
        this.minimumCellWidth = minimumCellWidth;
        this.cellHeight = cellHeight;
        this.maximumColumns = maximumColumns;
        this.gap = gap;
        setLayout(null);
        setOpaque(false);
    }

    @Override
    public void doLayout()
    {
        Insets insets = getInsets();
        int visibleWidth = getVisibleRect().width;
        int layoutWidth = visibleWidth > 0 ? Math.min(getWidth(), visibleWidth) : getWidth();
        int available = Math.max(minimumCellWidth, layoutWidth - insets.left - insets.right);
        int columns = columnsFor(available);
        int cellWidth = Math.max(1, (available - gap * (columns - 1)) / columns);

        for (int index = 0; index < getComponentCount(); index++)
        {
            int row = index / columns;
            int column = index % columns;
            int x = insets.left + column * (cellWidth + gap);
            int y = insets.top + row * (cellHeight + gap);
            getComponent(index).setBounds(x, y, cellWidth, cellHeight);
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        Insets insets = getInsets();
        int width = 0;
        if (getParent() != null)
        {
            width = getParent().getWidth();
        }
        if (width <= 0)
        {
            width = getWidth();
        }
        int visibleWidth = getVisibleRect().width;
        if (visibleWidth > 0)
        {
            width = width <= 0 ? visibleWidth : Math.min(width, visibleWidth);
        }
        if (width <= 0)
        {
            width = 210;
        }
        int available = Math.max(minimumCellWidth, width - insets.left - insets.right);
        int columns = columnsFor(available);
        int rows = Math.max(1, (getComponentCount() + columns - 1) / columns);
        int height = insets.top + insets.bottom + rows * cellHeight + Math.max(0, rows - 1) * gap;
        return new Dimension(width, height);
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

    private int columnsFor(int availableWidth)
    {
        int columns = Math.max(1, (availableWidth + gap) / (minimumCellWidth + gap));
        return Math.min(maximumColumns, columns);
    }
}
