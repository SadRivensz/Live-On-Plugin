package com.liveon.ui;

import com.liveon.api.LiveOnApiClient;
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
import java.util.function.Consumer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

public class LiveOnPanel extends PluginPanel
{
    private static final String HOME = "Início";
    private static final String DROPS = "Drops";
    private static final String MEMBERS = "Membros";
    private static final String RANKINGS = "Ranking";
    private static final String STAFF = "Staff";

    private final ClanAccessManager accessManager;
    private final JComboBox<String> navigation = new JComboBox<>();
    private final JPanel cards = new JPanel(new CardLayout());
    private final HomeCard homeCard;
    private final RecentDropsCard dropsCard;
    private final MembersCard membersCard;
    private final RankingsCard rankingsCard;
    private final StaffCard staffCard;
    private final Consumer<AccessSession> sessionListener = this::updateSession;
    private boolean active;

    public LiveOnPanel(ClanAccessManager accessManager, LiveOnApiClient apiClient)
    {
        super(false);
        this.accessManager = accessManager;
        this.homeCard = new HomeCard(apiClient);
        this.dropsCard = new RecentDropsCard(apiClient);
        this.membersCard = new MembersCard(apiClient);
        this.rankingsCard = new RankingsCard(apiClient);
        this.staffCard = new StaffCard(apiClient);

        setLayout(new BorderLayout(0, 8));
        navigation.setFocusable(false);
        navigation.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        navigation.addActionListener(event -> showSelectedCard());
        add(navigation, BorderLayout.NORTH);

        cards.setOpaque(false);
        cards.add(scroll(homeCard), HOME);
        cards.add(scroll(dropsCard), DROPS);
        cards.add(scroll(membersCard), MEMBERS);
        cards.add(scroll(rankingsCard), RANKINGS);
        cards.add(scroll(staffCard), STAFF);
        add(cards, BorderLayout.CENTER);
        accessManager.addListener(sessionListener);
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

    @Override
    public void onDeactivate()
    {
        active = false;
    }

    public void shutdown()
    {
        accessManager.removeListener(sessionListener);
    }

    private void updateSession(AccessSession session)
    {
        SwingUtilities.invokeLater(() ->
        {
            homeCard.updateSession(session);
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement(HOME);
            if (session.isAuthorized())
            {
                model.addElement(DROPS);
                model.addElement(MEMBERS);
                model.addElement(RANKINGS);
                if (session.isStaff())
                {
                    model.addElement(STAFF);
                }
            }
            navigation.setModel(model);
            navigation.setSelectedItem(HOME);
            if (active && session.isAuthorized())
            {
                homeCard.refresh();
            }
        });
    }

    private void showSelectedCard()
    {
        String selected = (String) navigation.getSelectedItem();
        if (selected == null)
        {
            return;
        }
        ((CardLayout) cards.getLayout()).show(cards, selected);
        if (DROPS.equals(selected))
        {
            dropsCard.refresh();
        }
        else if (MEMBERS.equals(selected))
        {
            membersCard.search();
        }
        else if (RANKINGS.equals(selected))
        {
            rankingsCard.refresh();
        }
    }

    private JScrollPane scroll(JPanel content)
    {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }
}
