package com.liveon.ui.cards;

import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.auth.AccessSession;
import com.liveon.ui.PanelStyles;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class HomeCard extends JPanel
{
    private final LiveOnApiClient apiClient;
    private final JPanel content = PanelStyles.verticalPanel();
    private final JPanel announcementContent = PanelStyles.verticalPanel();
    private volatile boolean authorized;

    public HomeCard(LiveOnApiClient apiClient)
    {
        this.apiClient = apiClient;
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setOpaque(false);
        add(PanelStyles.title("Live On Clan Hub"));
        PanelStyles.addGap(this);
        JButton refresh = new JButton("Atualizar");
        refresh.setAlignmentX(LEFT_ALIGNMENT);
        refresh.addActionListener(event -> refresh());
        add(refresh);
        PanelStyles.addGap(this);
        add(content);
        PanelStyles.showMessage(content, "Aguardando autorização...");
    }

    public void updateSession(AccessSession session)
    {
        authorized = session.isAuthorized();
        SwingUtilities.invokeLater(() ->
        {
            if (!session.isAuthorized())
            {
                PanelStyles.showMessage(content, session.getReason());
                return;
            }
            content.removeAll();
            JPanel account = PanelStyles.card();
            account.add(PanelStyles.title(session.getRsn()));
            account.add(PanelStyles.muted("Rank: " + (session.getRole() == null ? "membro" : session.getRole())));
            content.add(account);
            PanelStyles.addGap(content);
            PanelStyles.showMessage(announcementContent, "Carregando anúncios...");
            content.add(announcementContent);
            content.revalidate();
            content.repaint();
            refresh();
        });
    }

    public void refresh()
    {
        if (!authorized)
        {
            return;
        }
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
                card.add(PanelStyles.title(announcement.title));
                PanelStyles.addGap(card);
                card.add(PanelStyles.text(announcement.message));
                PanelStyles.addGap(card);
                card.add(PanelStyles.muted("Por " + announcement.author));
                announcementContent.add(card);
                PanelStyles.addGap(announcementContent);
            }
        }
        announcementContent.revalidate();
        announcementContent.repaint();
    }
}
