package com.liveon.ui.cards;

import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.ui.PanelStyles;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.util.QuantityFormatter;

public class RankingsCard extends JPanel
{
    private final LiveOnApiClient apiClient;
    private final JComboBox<MetricOption> metric = new JComboBox<>(MetricOption.values());
    private final JComboBox<PeriodOption> period = new JComboBox<>(PeriodOption.values());
    private final JPanel content = PanelStyles.verticalPanel();

    public RankingsCard(LiveOnApiClient apiClient)
    {
        this.apiClient = apiClient;
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setOpaque(false);
        add(PanelStyles.title("Ranking mensal"));
        PanelStyles.addGap(this);

        JPanel filters = new JPanel(new GridLayout(1, 2, 4, 0));
        filters.setOpaque(false);
        filters.add(metric);
        filters.add(period);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        filters.setAlignmentX(LEFT_ALIGNMENT);
        add(filters);
        PanelStyles.addGap(this);
        JButton refresh = new JButton("Atualizar ranking");
        refresh.setAlignmentX(LEFT_ALIGNMENT);
        refresh.addActionListener(event -> refresh());
        add(refresh);
        PanelStyles.addGap(this);
        add(content);
    }

    public void refresh()
    {
        MetricOption selectedMetric = (MetricOption) metric.getSelectedItem();
        PeriodOption selectedPeriod = (PeriodOption) period.getSelectedItem();
        if (selectedMetric == null || selectedPeriod == null)
        {
            return;
        }
        PanelStyles.showMessage(content, "Carregando ranking...");
        apiClient.getRankings(selectedMetric.apiValue, selectedPeriod.apiValue, new ApiCallback<ApiModels.RankingResponse>()
        {
            @Override
            public void onSuccess(ApiModels.RankingResponse response)
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

    private void render(ApiModels.RankingResponse response)
    {
        content.removeAll();
        if (response == null || response.entries == null || response.entries.isEmpty())
        {
            content.add(PanelStyles.muted("Sem dados para este período."));
        }
        else
        {
            for (ApiModels.RankingEntry entry : response.entries)
            {
                String value = "xp".equals(response.metric)
                    ? QuantityFormatter.quantityToStackSize((long) entry.value)
                    : String.format("%.2f h", entry.value);
                content.add(PanelStyles.line("#" + entry.rank + "  " + entry.rsn, value));
            }
        }
        content.revalidate();
        content.repaint();
    }

    private enum MetricOption
    {
        XP("XP", "xp"),
        EHP("EHP", "ehp"),
        EHB("EHB", "ehb");

        private final String label;
        private final String apiValue;

        MetricOption(String label, String apiValue)
        {
            this.label = label;
            this.apiValue = apiValue;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private enum PeriodOption
    {
        CURRENT("Mês atual", "current"),
        PREVIOUS("Último mês", "previous");

        private final String label;
        private final String apiValue;

        PeriodOption(String label, String apiValue)
        {
            this.label = label;
            this.apiValue = apiValue;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }
}
