package com.liveon.ui;

import com.liveon.api.ApiModels;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;

/**
 * Collection-log browser backed by RuneProfile. It intentionally lives in a
 * separate window so the 225px RuneLite sidebar never has to squeeze an item
 * grid and a page selector into the same space.
 */
public final class RuneProfileDialog
{
    private static final String MENU = "menu";
    private static final String DETAIL = "detail";

    private final ApiModels.RuneProfileData profile;
    private final ItemManager itemManager;
    private final JDialog dialog;
    private final JPanel browser = new JPanel(new CardLayout());
    private final JPanel detail = new JPanel(new BorderLayout(0, 8));

    private RuneProfileDialog(
        Component parent,
        ApiModels.RuneProfileData profile,
        ItemManager itemManager)
    {
        this.profile = profile;
        this.itemManager = itemManager;
        Window owner = SwingUtilities.getWindowAncestor(parent);
        dialog = new JDialog(
            owner,
            "RuneProfile · " + profile.username,
            Dialog.ModalityType.MODELESS
        );
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setMinimumSize(new Dimension(390, 480));
        dialog.setPreferredSize(new Dimension(470, 620));
        dialog.setContentPane(build());
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
    }

    public static void show(
        Component parent,
        ApiModels.RuneProfileData profile,
        ItemManager itemManager)
    {
        if (profile == null || profile.collectionLog == null)
        {
            return;
        }
        new RuneProfileDialog(parent, profile, itemManager).dialog.setVisible(true);
    }

    private JPanel build()
    {
        JPanel root = new JPanel(new BorderLayout(0, 9));
        root.setBackground(PanelStyles.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel heading = new JPanel(new BorderLayout(8, 0));
        heading.setOpaque(false);
        JLabel title = PanelStyles.sectionTitle(profile.username, PanelStyles.UiIcon.CROWN);
        ApiModels.RuneProfileCollection log = profile.collectionLog;
        heading.add(title, BorderLayout.WEST);
        heading.add(
            PanelStyles.badge(log.obtained + " / " + log.total, PanelStyles.GREEN),
            BorderLayout.EAST
        );
        root.add(heading, BorderLayout.NORTH);

        browser.setOpaque(false);
        browser.add(buildMenu(), MENU);
        detail.setOpaque(false);
        browser.add(detail, DETAIL);
        root.add(browser, BorderLayout.CENTER);

        JLabel source = new JLabel("Dados públicos do RuneProfile · " + shortDate(profile.updatedAt));
        source.setForeground(PanelStyles.MUTED);
        source.setFont(FontManager.getRunescapeSmallFont());
        root.add(source, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildMenu()
    {
        JPanel menu = new JPanel(new BorderLayout());
        menu.setOpaque(false);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        PanelStyles.styleTabs(tabs);
        for (ApiModels.RuneProfileTab tab : profile.collectionLog.tabs)
        {
            tabs.addTab(shortTabName(tab.name), pageList(tab));
            int index = tabs.getTabCount() - 1;
            tabs.setToolTipTextAt(index, tab.name + " · " + tab.obtained + "/" + tab.total);
        }
        menu.add(tabs, BorderLayout.CENTER);
        return menu;
    }

    private JScrollPane pageList(ApiModels.RuneProfileTab tab)
    {
        JPanel list = PanelStyles.verticalPanel();
        list.setBorder(BorderFactory.createEmptyBorder(8, 7, 8, 7));
        JPanel summary = PanelStyles.heroCard();
        summary.add(PanelStyles.title(tab.name));
        summary.add(Box.createRigidArea(new Dimension(0, 4)));
        summary.add(PanelStyles.muted(tab.obtained + " de " + tab.total + " itens obtidos"));
        list.add(summary);
        list.add(Box.createRigidArea(new Dimension(0, 8)));

        for (ApiModels.RuneProfilePage page : tab.pages)
        {
            JButton button = PanelStyles.secondaryButton(
                page.name + "   " + page.obtained + "/" + page.total,
                null
            );
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            button.setToolTipText("Ver itens obtidos e faltantes de " + page.name);
            button.addActionListener(event -> showPage(page));
            list.add(button);
            list.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(15);
        scroll.getViewport().setBackground(PanelStyles.BACKGROUND);
        return scroll;
    }

    private void showPage(ApiModels.RuneProfilePage page)
    {
        detail.removeAll();
        JPanel heading = new JPanel(new BorderLayout(8, 0));
        heading.setOpaque(false);
        JButton back = PanelStyles.secondaryButton("Voltar", null);
        back.addActionListener(event -> showMenu());
        heading.add(back, BorderLayout.WEST);
        JLabel title = PanelStyles.title(page.name);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        heading.add(title, BorderLayout.CENTER);
        heading.add(
            PanelStyles.badge(page.obtained + "/" + page.total, PanelStyles.GREEN),
            BorderLayout.EAST
        );
        detail.add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 4, 6, 6));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(5, 3, 5, 3));
        for (ApiModels.RuneProfileItem item : page.items)
        {
            grid.add(itemTile(item));
        }
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(15);
        scroll.getViewport().setBackground(PanelStyles.BACKGROUND);
        detail.add(scroll, BorderLayout.CENTER);

        ((CardLayout) browser.getLayout()).show(browser, DETAIL);
        detail.revalidate();
        detail.repaint();
    }

    private JPanel itemTile(ApiModels.RuneProfileItem item)
    {
        boolean obtained = item.quantity > 0;
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setOpaque(true);
        tile.setBackground(PanelStyles.SURFACE);
        tile.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(obtained ? PanelStyles.GREEN : PanelStyles.BORDER),
            BorderFactory.createEmptyBorder(6, 4, 6, 4)
        ));
        tile.setToolTipText(item.name);

        JLabel icon = new JLabel();
        icon.setPreferredSize(new Dimension(34, 34));
        icon.setMinimumSize(new Dimension(34, 34));
        icon.setMaximumSize(new Dimension(34, 34));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        itemManager.getImage(item.id).addTo(icon);
        tile.add(icon);
        tile.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel name = new JLabel(
            "<html><div style='width:76px;text-align:center'>" + PanelStyles.escape(item.name) + "</div></html>"
        );
        name.setForeground(obtained ? PanelStyles.TEXT : PanelStyles.MUTED);
        name.setFont(FontManager.getRunescapeSmallFont());
        name.setHorizontalAlignment(SwingConstants.CENTER);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        tile.add(name);
        tile.add(Box.createVerticalGlue());

        JLabel state = new JLabel(obtained ? "x" + item.quantity : "FALTA");
        state.setForeground(obtained ? PanelStyles.GREEN : PanelStyles.RED);
        state.setFont(FontManager.getRunescapeSmallFont());
        state.setAlignmentX(Component.CENTER_ALIGNMENT);
        tile.add(state);
        return tile;
    }

    private void showMenu()
    {
        ((CardLayout) browser.getLayout()).show(browser, MENU);
    }

    private String shortTabName(String name)
    {
        if ("Minigames".equalsIgnoreCase(name))
        {
            return "Ativ.";
        }
        if ("Other".equalsIgnoreCase(name))
        {
            return "Outros";
        }
        return name;
    }

    private String shortDate(String value)
    {
        if (value == null || value.length() < 10)
        {
            return "perfil sincronizado";
        }
        return "atualizado em " + value.substring(8, 10) + "/" + value.substring(5, 7);
    }
}
