package com.liveon.ui.cards;

import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.ui.PanelStyles;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.util.QuantityFormatter;

public class RecentDropsCard extends JPanel
{
    private final LiveOnApiClient apiClient;
    private final JPanel content = PanelStyles.verticalPanel();

    public RecentDropsCard(LiveOnApiClient apiClient)
    {
        this.apiClient = apiClient;
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setOpaque(false);
        add(PanelStyles.title("Drops recentes"));
        PanelStyles.addGap(this);
        JButton refresh = new JButton("Atualizar feed");
        refresh.setAlignmentX(LEFT_ALIGNMENT);
        refresh.addActionListener(event -> refresh());
        add(refresh);
        PanelStyles.addGap(this);
        add(content);
    }

    public void refresh()
    {
        PanelStyles.showMessage(content, "Carregando drops...");
        apiClient.getRecentDrops(new ApiCallback<ApiModels.DropListResponse>()
        {
            @Override
            public void onSuccess(ApiModels.DropListResponse response)
            {
                SwingUtilities.invokeLater(() -> render(response));
            }

            @Override
            public void onFailure(String message)
            {
                SwingUtilities.invokeLater(() -> PanelStyles.showMessage(content, message));
            }
        });
    }

    private void render(ApiModels.DropListResponse response)
    {
        content.removeAll();
        if (response == null || response.drops == null || response.drops.isEmpty())
        {
            content.add(PanelStyles.muted("Nenhum drop registrado."));
        }
        else
        {
            for (ApiModels.DropView drop : response.drops)
            {
                JPanel card = PanelStyles.card();
                card.add(PanelStyles.title(drop.playerName));
                card.add(PanelStyles.line("Origem", drop.source));
                card.add(PanelStyles.line("Valor", QuantityFormatter.quantityToStackSize(drop.totalValue) + " gp"));
                if (drop.items != null && !drop.items.isEmpty())
                {
                    ApiModels.ItemPayload best = drop.items.get(0);
                    for (ApiModels.ItemPayload item : drop.items)
                    {
                        if (item.totalPrice > best.totalPrice)
                        {
                            best = item;
                        }
                    }
                    card.add(PanelStyles.muted(best.quantity + "x " + best.name));
                }
                content.add(card);
                PanelStyles.addGap(content);
            }
        }
        content.revalidate();
        content.repaint();
    }
}
