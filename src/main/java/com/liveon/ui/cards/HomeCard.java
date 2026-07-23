package com.liveon.ui.cards;

import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.auth.AccessSession;
import com.liveon.assets.HiscoreIcons;
import com.liveon.ui.PanelStyles;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;

public class HomeCard extends JPanel
{
    private final LiveOnApiClient apiClient;
    private final JPanel accountContent = PanelStyles.verticalPanel();
    private final JPanel announcementContent = PanelStyles.verticalPanel();
    private final ItemGoalCard goalCard;
    private final HiscoreIcons hiscoreIcons;
    private volatile boolean authorized;
    private volatile String currentRsn;

    public HomeCard(LiveOnApiClient apiClient, ItemManager itemManager, HiscoreIcons hiscoreIcons)
    {
        this.apiClient = apiClient;
        this.hiscoreIcons = hiscoreIcons;
        this.goalCard = new ItemGoalCard(apiClient, itemManager);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setOpaque(false);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel brand = PanelStyles.sectionTitle("Live On Clan Hub", PanelStyles.UiIcon.CROWN);
        JButton refresh = PanelStyles.secondaryButton("", PanelStyles.UiIcon.REFRESH);
        refresh.setToolTipText("Atualizar painel");
        refresh.setPreferredSize(new Dimension(30, 28));
        refresh.addActionListener(event -> refresh());
        heading.add(brand, BorderLayout.WEST);
        heading.add(refresh, BorderLayout.EAST);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        add(heading);
        PanelStyles.addGap(this);
        add(accountContent);
        PanelStyles.addGap(this);
        add(goalCard);
        PanelStyles.addGap(this);
        add(PanelStyles.sectionTitle("Mural do clã", PanelStyles.UiIcon.MEGAPHONE));
        PanelStyles.addGap(this);
        add(announcementContent);
        PanelStyles.showMessage(accountContent, "Aguardando autorização...");
    }

    public void updateSession(AccessSession session)
    {
        authorized = session.isAuthorized();
        currentRsn = session.isAuthorized() ? session.getRsn() : null;
        SwingUtilities.invokeLater(() ->
        {
            if (!session.isAuthorized())
            {
                PanelStyles.showMessage(accountContent, session.getReason());
                return;
            }
            renderAccount(session, null);
            apiClient.getMember(session.getRsn(), new ApiCallback<ApiModels.MemberProfile>()
            {
                @Override
                public void onSuccess(ApiModels.MemberProfile profile)
                {
                    if (profile != null && profile.member != null && session.getRsn().equalsIgnoreCase(currentRsn))
                    {
                        SwingUtilities.invokeLater(() -> renderAccount(session, profile.member));
                    }
                }

                @Override
                public void onFailure(String message)
                {
                    // The account card still works without optional profile enrichment.
                }
            });
            goalCard.refresh();
            refresh();
        });
    }

    private void renderAccount(AccessSession session, ApiModels.MemberSummary member)
    {
        accountContent.removeAll();
        JPanel account = PanelStyles.heroCard();
        JPanel identity = new JPanel(new BorderLayout(7, 0));
        identity.setOpaque(false);
        JLabel avatar = new JLabel();
        avatar.setPreferredSize(new Dimension(34, 34));
        avatar.setHorizontalAlignment(JLabel.CENTER);
        if (member != null)
        {
            avatar.setIcon(hiscoreIcons.accountTypeIcon(member.accountType, 24));
            loadAvatar(member.avatarUrl, avatar);
        }
        else
        {
            avatar.setIcon(PanelStyles.icon(PanelStyles.UiIcon.PEOPLE, PanelStyles.PURPLE, 24));
        }
        identity.add(avatar, BorderLayout.WEST);
        JPanel copy = PanelStyles.verticalPanel();
        JLabel name = PanelStyles.title(session.getRsn());
        hiscoreIcons.addRankIcon(name, session.getRole());
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(name);
        JLabel role = new JLabel(
            session.getRole() == null ? "Membro" : hiscoreIcons.rankDisplayName(session.getRole())
        );
        role.setForeground(PanelStyles.MUTED);
        role.setFont(FontManager.getRunescapeSmallFont());
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(role);
        identity.add(copy, BorderLayout.CENTER);
        JLabel online = PanelStyles.badge("ONLINE", PanelStyles.GREEN);
        online.setVerticalAlignment(JLabel.TOP);
        identity.add(online, BorderLayout.EAST);
        identity.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        identity.setAlignmentX(Component.LEFT_ALIGNMENT);
        account.add(identity);
        accountContent.add(account);
        accountContent.revalidate();
        accountContent.repaint();
    }

    private void loadAvatar(String url, JLabel target)
    {
        if (url == null || url.isEmpty())
        {
            return;
        }
        apiClient.getImage(url, new ApiCallback<BufferedImage>()
        {
            @Override
            public void onSuccess(BufferedImage image)
            {
                SwingUtilities.invokeLater(() -> target.setIcon(PanelStyles.circularIcon(image, 32)));
            }

            @Override
            public void onFailure(String message)
            {
                // Discord avatar is optional.
            }
        });
    }

    public void refresh()
    {
        if (!authorized)
        {
            return;
        }
        PanelStyles.showMessage(announcementContent, "Atualizando mural...");
        apiClient.getAnnouncements(new ApiCallback<ApiModels.AnnouncementResponse>()
        {
            @Override
            public void onSuccess(ApiModels.AnnouncementResponse response)
            {
                SwingUtilities.invokeLater(() -> renderAnnouncements(response));
            }

            @Override
            public void onFailure(String message)
            {
                SwingUtilities.invokeLater(() -> PanelStyles.showMessage(announcementContent, message));
            }
        });
    }

    private void renderAnnouncements(ApiModels.AnnouncementResponse response)
    {
        announcementContent.removeAll();
        if (response == null || response.announcements == null || response.announcements.isEmpty())
        {
            announcementContent.add(PanelStyles.muted("Nenhum anúncio ativo."));
        }
        else
        {
            for (ApiModels.Announcement announcement : response.announcements)
            {
                JPanel card = PanelStyles.card();
                JPanel top = new JPanel(new BorderLayout(5, 0));
                top.setOpaque(false);
                JLabel title = PanelStyles.title(ellipsize(announcement.title, 15));
                title.setToolTipText(announcement.title);
                top.add(title, BorderLayout.CENTER);
                Color kindColor = "item_goal".equals(announcement.kind) ? PanelStyles.GREEN : PanelStyles.GOLD;
                top.add(PanelStyles.badge("item_goal".equals(announcement.kind) ? "CONQUISTA" : "AVISO", kindColor), BorderLayout.EAST);
                top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                top.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(top);
                PanelStyles.addGap(card);
                card.add(fullWidthRow(PanelStyles.text(announcement.message)));
                PanelStyles.addGap(card);
                card.add(fullWidthRow(
                    PanelStyles.muted("Por " + announcement.author + " · " + shortDate(announcement.createdAt))
                ));
                announcementContent.add(card);
                PanelStyles.addGap(announcementContent);
            }
        }
        announcementContent.revalidate();
        announcementContent.repaint();
    }

    private JPanel fullWidthRow(JLabel label)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(label, BorderLayout.WEST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private String shortDate(String value)
    {
        if (value == null || value.length() < 10)
        {
            return "agora";
        }
        return value.substring(8, 10) + "/" + value.substring(5, 7);
    }

    private String ellipsize(String value, int max)
    {
        if (value == null || value.length() <= max)
        {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
