package com.liveon.ui.cards;

import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.ui.PanelStyles;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class StaffCard extends JPanel
{
    private final LiveOnApiClient apiClient;
    private final JTextField title = new JTextField();
    private final JTextArea message = new JTextArea(6, 20);
    private final JCheckBox showOnLogin = new JCheckBox("Usar como mensagem de login");
    private final JLabel status = PanelStyles.muted("Somente a staff autorizada pode publicar.");

    public StaffCard(LiveOnApiClient apiClient)
    {
        this.apiClient = apiClient;
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setOpaque(false);
        add(PanelStyles.title("Anúncio da staff"));
        PanelStyles.addGap(this);
        add(PanelStyles.muted("Título"));
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        title.setAlignmentX(LEFT_ALIGNMENT);
        add(title);
        PanelStyles.addGap(this);
        add(PanelStyles.muted("Mensagem"));
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setAlignmentX(LEFT_ALIGNMENT);
        add(message);
        PanelStyles.addGap(this);
        showOnLogin.setOpaque(false);
        showOnLogin.setAlignmentX(LEFT_ALIGNMENT);
        add(showOnLogin);
        PanelStyles.addGap(this);
        JButton publish = new JButton("Publicar");
        publish.setAlignmentX(LEFT_ALIGNMENT);
        publish.addActionListener(event -> publish());
        add(publish);
        PanelStyles.addGap(this);
        add(status);
    }

    private void publish()
    {
        String titleText = title.getText().trim();
        String messageText = message.getText().trim();
        if (titleText.isEmpty() || messageText.isEmpty())
        {
            status.setText("Preencha título e mensagem.");
            return;
        }

        ApiModels.PublishAnnouncementRequest request = new ApiModels.PublishAnnouncementRequest();
        request.title = titleText;
        request.message = messageText;
        request.kind = "clan";
        request.showOnLogin = showOnLogin.isSelected();
        status.setText("Publicando...");
        apiClient.publishAnnouncement(request, new ApiCallback<ApiModels.Announcement>()
        {
            @Override
            public void onSuccess(ApiModels.Announcement value)
            {
                SwingUtilities.invokeLater(() ->
                {
                    status.setText("Anúncio publicado.");
                    message.setText("");
                });
            }

            @Override
            public void onFailure(String error)
            {
                SwingUtilities.invokeLater(() -> status.setText(error));
            }
        });
    }
}
