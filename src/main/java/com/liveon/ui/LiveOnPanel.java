package com.liveon.ui;

import com.liveon.api.LiveOnApiClient;
import com.liveon.assets.HiscoreIcons;
import com.liveon.auth.AccessSession;
import com.liveon.auth.ClanAccessManager;
import com.liveon.ui.cards.HomeCard;
import com.liveon.ui.cards.MembersCard;
import com.liveon.ui.cards.RankingsCard;
import com.liveon.ui.cards.RecentDropsCard;
import com.liveon.ui.cards.StaffCard;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

public class LiveOnPanel extends PluginPanel
{
    private static final String HOME = "home";
    private static final String DROPS = "drops";
    private static final String MEMBERS = "members";
    private static final String RANKINGS = "rankings";
    private static final String STAFF = "staff";

    private final ClanAccessManager accessManager;
    private final JPanel navigation = new JPanel();
    private final JPanel cards = new JPanel(new CardLayout());
    private final Map<String, JButton> navigationButtons = new LinkedHashMap<>();
    private final HomeCard homeCard;
    private final RecentDropsCard dropsCard;
    private final MembersCard membersCard;
    private final RankingsCard rankingsCard;
    private final StaffCard staffCard;
    private final Consumer<AccessSession> sessionListener = this::updateSession;
    private boolean active;

    public LiveOnPanel(
        ClanAccessManager accessManager,
        LiveOnApiClient apiClient,
        ItemManager itemManager,
        HiscoreIcons hiscoreIcons)
    {
        super(false);
        this.accessManager = accessManager;
        this.homeCard = new HomeCard(apiClient, itemManager, hiscoreIcons);
        this.dropsCard = new RecentDropsCard(apiClient, itemManager);
        this.membersCard = new MembersCard(apiClient, itemManager, hiscoreIcons);
        this.rankingsCard = new RankingsCard(apiClient);
        this.staffCard = new StaffCard(apiClient);

        setLayout(new BorderLayout(0, 9));
        setBackground(PanelStyles.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(8, 6, 6, 6));
        navigation.setOpaque(false);
        navigation.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        add(navigation, BorderLayout.NORTH);

        cards.setOpaque(false);
        cards.add(scroll(homeCard), HOME);
        cards.add(scroll(dropsCard), DROPS);
        cards.add(scroll(membersCard), MEMBERS);
        cards.add(scroll(rankingsCard), RANKINGS);
        cards.add(scroll(staffCard), STAFF);
        add(cards, BorderLayout.CENTER);
        accessManager.addListener(sessionListener);
        rebuildNavigation(false, false);
    }

    @Override
    public void onActivate()
    {
        active = true;
        if (accessManager.isAuthorized())
        {
            homeCard.refresh();
        }
    }

    @Override public void onDeactivate() { active = false; }
    public void shutdown() { accessManager.removeListener(sessionListener); }

    private void updateSession(AccessSession session)
    {
        SwingUtilities.invokeLater(() ->
        {
            homeCard.updateSession(session);
            rebuildNavigation(session.isAuthorized(), session.isStaff());
            showCard(HOME);
            if (session.isAuthorized())
            {
                rankingsCard.prefetch();
            }
            if (active && session.isAuthorized())
            {
                homeCard.refresh();
            }
        });
    }

    private void rebuildNavigation(boolean authorized, boolean staff)
    {
        navigation.removeAll();
        navigationButtons.clear();
        int count = authorized ? (staff ? 5 : 4) : 1;
        navigation.setLayout(new GridLayout(1, count, 4, 0));
        addNavigation(HOME, "Início", PanelStyles.UiIcon.HOME);
        if (authorized)
        {
            addNavigation(DROPS, "Drops", PanelStyles.UiIcon.DROP);
            addNavigation(MEMBERS, "Membros", PanelStyles.UiIcon.PEOPLE);
            addNavigation(RANKINGS, "Ranking", PanelStyles.UiIcon.TROPHY);
            if (staff)
            {
                addNavigation(STAFF, "Staff", PanelStyles.UiIcon.MEGAPHONE);
            }
        }
        selectButton(HOME);
        navigation.revalidate();
        navigation.repaint();
    }

    private void addNavigation(String key, String tooltip, PanelStyles.UiIcon icon)
    {
        JButton button = PanelStyles.secondaryButton("", icon);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(34, 34));
        button.addActionListener(event -> showCard(key));
        navigationButtons.put(key, button);
        navigation.add(button);
    }

    private void showCard(String selected)
    {
        ((CardLayout) cards.getLayout()).show(cards, selected);
        selectButton(selected);
        if (DROPS.equals(selected)) dropsCard.refresh();
        else if (MEMBERS.equals(selected)) membersCard.search();
        else if (RANKINGS.equals(selected)) rankingsCard.refreshIfNeeded();
    }

    private void selectButton(String selected)
    {
        navigationButtons.forEach((key, button) ->
        {
            button.putClientProperty("secondary", !key.equals(selected));
            button.repaint();
        });
    }

    private JScrollPane scroll(JPanel content)
    {
        JScrollPane scroll = new JScrollPane(new ViewportWidthPanel(content));
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBackground(PanelStyles.BACKGROUND);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }
}
