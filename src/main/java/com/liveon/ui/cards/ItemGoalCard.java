package com.liveon.ui.cards;

import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.ui.PanelStyles;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;

/** Lets a member choose one active item goal and shows its current grinding timer. */
public class ItemGoalCard extends JPanel
{
    private final LiveOnApiClient apiClient;
    private final ItemManager itemManager;
    private final JPanel current = PanelStyles.verticalPanel();
    private final JTextField query = new JTextField();
    private final JComboBox<ItemOption> results = new JComboBox<>();
    private final JLabel status = PanelStyles.muted("Busque um item pelo nome.");

    public ItemGoalCard(LiveOnApiClient apiClient, ItemManager itemManager)
    {
        this.apiClient = apiClient;
        this.itemManager = itemManager;
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setOpaque(false);

        add(PanelStyles.sectionTitle("Item desejado", PanelStyles.UiIcon.TARGET));
        PanelStyles.addGap(this);
        add(current);
        PanelStyles.addGap(this);

        PanelStyles.styleField(query);
        query.setToolTipText(null);
        query.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
        query.setAlignmentX(LEFT_ALIGNMENT);
        add(query);
        PanelStyles.addGap(this);

        JButton search = PanelStyles.secondaryButton("Buscar item", PanelStyles.UiIcon.SEARCH);
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        search.addActionListener(event -> searchItems());
        query.addActionListener(event -> searchItems());
        add(search);
        PanelStyles.addGap(this);

        PanelStyles.styleCombo(results);
        results.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        results.setAlignmentX(LEFT_ALIGNMENT);
        results.setVisible(false);
        add(results);

        JButton save = PanelStyles.button("Marcar item", PanelStyles.UiIcon.TARGET);
        JButton clear = PanelStyles.secondaryButton("Remover objetivo", null);
        save.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        clear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        save.addActionListener(event -> saveGoal());
        clear.addActionListener(event -> clearGoal());
        PanelStyles.addGap(this);
        add(save);
        PanelStyles.addGap(this);
        add(clear);
        PanelStyles.addGap(this);
        add(status);
        refresh();
    }

    public void refresh()
    {
        apiClient.getItemGoal(new ApiCallback<ApiModels.ItemGoalResponse>()
        {
            @Override
            public void onSuccess(ApiModels.ItemGoalResponse response)
            {
                SwingUtilities.invokeLater(() -> renderGoal(response == null ? null : response.goal));
            }

            @Override
            public void onFailure(String message)
            {
                SwingUtilities.invokeLater(() -> status.setText(message));
            }
        });
    }

    private void searchItems()
    {
        String text = query.getText().trim();
        if (text.length() < 2)
        {
            status.setText("Digite pelo menos 2 letras.");
            return;
        }
        status.setText("Buscando itens...");
        CompletableFuture.supplyAsync(() -> itemManager.search(text)).thenAccept(found -> SwingUtilities.invokeLater(() ->
        {
            DefaultComboBoxModel<ItemOption> model = new DefaultComboBoxModel<>();
            int count = 0;
            for (ItemPrice item : found)
            {
                model.addElement(new ItemOption(item.getId(), item.getName()));
                if (++count == 12)
                {
                    break;
                }
            }
            results.setModel(model);
            results.setVisible(model.getSize() > 0);
            status.setText(model.getSize() == 0 ? "Nenhum item encontrado." : "Selecione o item correto e confirme.");
            revalidate();
        })).exceptionally(error ->
        {
            SwingUtilities.invokeLater(() -> status.setText("A busca de itens falhou."));
            return null;
        });
    }

    private void saveGoal()
    {
        ItemOption selected = (ItemOption) results.getSelectedItem();
        if (selected == null)
        {
            status.setText("Busque e selecione um item primeiro.");
            return;
        }
        status.setText("Salvando objetivo...");
        apiClient.setItemGoal(new ApiModels.ItemGoalRequest(selected.id, selected.name), new ApiCallback<ApiModels.ItemGoalResponse>()
        {
            @Override
            public void onSuccess(ApiModels.ItemGoalResponse response)
            {
                SwingUtilities.invokeLater(() ->
                {
                    renderGoal(response.goal);
                    status.setText("Objetivo salvo. A Live On avisará quando ele cair!");
                });
            }

            @Override
            public void onFailure(String message)
            {
                SwingUtilities.invokeLater(() -> status.setText(message));
            }
        });
    }

    private void clearGoal()
    {
        apiClient.clearItemGoal(new ApiCallback<ApiModels.StatusResponse>()
        {
            @Override
            public void onSuccess(ApiModels.StatusResponse response)
            {
                SwingUtilities.invokeLater(() ->
                {
                    renderGoal(null);
                    status.setText("Objetivo removido.");
                });
            }

            @Override
            public void onFailure(String message)
            {
                SwingUtilities.invokeLater(() -> status.setText(message));
            }
        });
    }

    private void renderGoal(ApiModels.ItemGoal goal)
    {
        current.removeAll();
        JPanel card = PanelStyles.card();
        if (goal == null)
        {
            card.add(PanelStyles.muted("Nenhum item marcado no momento."));
        }
        else
        {
            JPanel line = new JPanel(new BorderLayout(8, 0));
            line.setOpaque(false);
            JLabel icon = new JLabel();
            itemManager.getImage(goal.itemId).addTo(icon);
            line.add(icon, BorderLayout.WEST);
            JPanel copy = PanelStyles.verticalPanel();
            JLabel itemName = PanelStyles.title(ellipsize(goal.itemName, 22));
            itemName.setToolTipText(goal.itemName);
            copy.add(itemName);
            copy.add(PanelStyles.muted("Em busca desde " + shortDate(goal.startedAt)));
            line.add(copy, BorderLayout.CENTER);
            card.add(line);
        }
        current.add(card);
        current.revalidate();
        current.repaint();
    }

    private String shortDate(String value)
    {
        if (value == null || value.length() < 10)
        {
            return "agora";
        }
        return value.substring(8, 10) + "/" + value.substring(5, 7) + "/" + value.substring(0, 4);
    }

    private String ellipsize(String value, int max)
    {
        if (value == null || value.length() <= max)
        {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }

    private static final class ItemOption
    {
        private final int id;
        private final String name;

        private ItemOption(int id, String name)
        {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }
}
