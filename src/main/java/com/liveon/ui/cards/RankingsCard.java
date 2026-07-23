package com.liveon.ui.cards;

import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.ui.PanelStyles;
import com.liveon.ui.ResponsiveGridPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.util.QuantityFormatter;

public class RankingsCard extends JPanel
{
    private final LiveOnApiClient apiClient;
    private final JComboBox<MetricOption> metric = new JComboBox<>(MetricOption.values());
    private final JComboBox<PeriodOption> period = new JComboBox<>(PeriodOption.values());
    private final JPanel content = PanelStyles.verticalPanel();
    private ApiModels.RankingResponse lastResponse;
    private int currentPage = 1;
    private long loadedAt;
    private boolean loading;
    private String activeRequest;
    private static final int PAGE_SIZE = 10;
    private static final long CACHE_MILLIS = 5 * 60 * 1000L;

    public RankingsCard(LiveOnApiClient apiClient)
    {
        this.apiClient = apiClient;
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setOpaque(false);
        add(PanelStyles.sectionTitle("Corrida mensal", PanelStyles.UiIcon.TROPHY));
        PanelStyles.addGap(this);

        JPanel intro = PanelStyles.heroCard();
        intro.add(PanelStyles.title("Quem mais evoluiu?"));
        PanelStyles.addGap(intro);
        intro.add(PanelStyles.muted("Compare XP, EHP e EHB do mês atual ou do mês anterior."));
        add(intro);
        PanelStyles.addGap(this);

        PanelStyles.styleCombo(metric);
        PanelStyles.styleCombo(period);
        JPanel filters = new ResponsiveGridPanel(88, 31, 2, 5);
        filters.add(metric);
        filters.add(period);
        filters.setAlignmentX(LEFT_ALIGNMENT);
        add(filters);
        PanelStyles.addGap(this);
        JButton refresh = PanelStyles.button("Atualizar ranking", PanelStyles.UiIcon.REFRESH);
        refresh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
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
        PanelStyles.showMessage(content, "Consultando o Wise Old Man...");
        currentPage = 1;
        String requestKey = selectedMetric.apiValue + ":" + selectedPeriod.apiValue;
        if (loading && requestKey.equals(activeRequest))
        {
            return;
        }
        loading = true;
        activeRequest = requestKey;
        requestRanking(selectedMetric, selectedPeriod, true);
    }

    public void refreshIfNeeded()
    {
        MetricOption selectedMetric = (MetricOption) metric.getSelectedItem();
        PeriodOption selectedPeriod = (PeriodOption) period.getSelectedItem();
        boolean cacheMatches = lastResponse != null
            && selectedMetric != null
            && selectedPeriod != null
            && selectedMetric.apiValue.equals(lastResponse.metric)
            && selectedPeriod.apiValue.equals(lastResponse.period);
        if (cacheMatches && System.currentTimeMillis() - loadedAt < CACHE_MILLIS)
        {
            render(lastResponse);
            return;
        }
        refresh();
    }

    public void prefetch()
    {
        if (lastResponse != null || loading)
        {
            return;
        }
        MetricOption selectedMetric = (MetricOption) metric.getSelectedItem();
        PeriodOption selectedPeriod = (PeriodOption) period.getSelectedItem();
        if (selectedMetric == null || selectedPeriod == null)
        {
            return;
        }
        loading = true;
        activeRequest = selectedMetric.apiValue + ":" + selectedPeriod.apiValue;
        requestRanking(selectedMetric, selectedPeriod, false);
    }

    private void requestRanking(MetricOption selectedMetric, PeriodOption selectedPeriod, boolean allowRetry)
    {
        apiClient.getRankings(selectedMetric.apiValue, selectedPeriod.apiValue, new ApiCallback<ApiModels.RankingResponse>()
        {
            @Override
            public void onSuccess(ApiModels.RankingResponse response)
            {
                loading = false;
                activeRequest = null;
                loadedAt = System.currentTimeMillis();
                SwingUtilities.invokeLater(() -> render(response));
            }

            @Override
            public void onFailure(String message)
            {
                if (allowRetry)
                {
                    SwingUtilities.invokeLater(() -> PanelStyles.showMessage(content, "Reconectando ao ranking..."));
                    javax.swing.Timer retry = new javax.swing.Timer(
                        700,
                        event -> requestRanking(selectedMetric, selectedPeriod, false)
                    );
                    retry.setRepeats(false);
                    retry.start();
                    return;
                }
                loading = false;
                activeRequest = null;
                SwingUtilities.invokeLater(() -> PanelStyles.showMessage(
                    content,
                    message + ". Confirme se a API local continua aberta."
                ));
            }
        });
    }

    private void render(ApiModels.RankingResponse response)
    {
        lastResponse = response;
        content.removeAll();
        if (response == null || response.entries == null || response.entries.isEmpty())
        {
            content.add(PanelStyles.muted("Sem ganhos registrados neste período."));
        }
        else
        {
            int start = (currentPage - 1) * PAGE_SIZE;
            int end = Math.min(response.entries.size(), start + PAGE_SIZE);
            for (ApiModels.RankingEntry entry : response.entries.subList(start, end))
            {
                JPanel row = entry.rank <= 3 ? PanelStyles.heroCard() : PanelStyles.card();
                JPanel line = new JPanel(new BorderLayout(6, 0));
                line.setOpaque(false);
                Color medal = entry.rank == 1 ? PanelStyles.GOLD : entry.rank == 2 ? new Color(198, 205, 218) : new Color(205, 132, 82);
                JLabel name = new JLabel("#" + entry.rank + "  " + entry.rsn);
                name.setForeground(PanelStyles.TEXT);
                if (entry.rank <= 3)
                {
                    name.setIcon(PanelStyles.icon(PanelStyles.UiIcon.TROPHY, medal, 18));
                }
                JLabel value = new JLabel(formatValue(response.metric, entry.value));
                value.setForeground(entry.rank <= 3 ? medal : PanelStyles.GOLD);
                line.add(name, BorderLayout.WEST);
                line.add(value, BorderLayout.EAST);
                row.add(line);
                content.add(row);
                PanelStyles.addGap(content);
            }
            int totalPages = Math.max(1, (response.entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (totalPages > 1)
            {
                content.add(pagination(totalPages));
            }
        }
        content.revalidate();
        content.repaint();
    }

    private JPanel pagination(int totalPages)
    {
        JPanel navigation = new JPanel(new BorderLayout(6, 0));
        navigation.setOpaque(false);
        JButton previous = PanelStyles.secondaryButton("‹", null);
        JButton next = PanelStyles.secondaryButton("›", null);
        JLabel page = new JLabel(currentPage + " / " + totalPages, JLabel.CENTER);
        page.setForeground(PanelStyles.MUTED);
        previous.setEnabled(currentPage > 1);
        next.setEnabled(currentPage < totalPages);
        previous.addActionListener(event ->
        {
            currentPage--;
            render(lastResponse);
        });
        next.addActionListener(event ->
        {
            currentPage++;
            render(lastResponse);
        });
        navigation.add(previous, BorderLayout.WEST);
        navigation.add(page, BorderLayout.CENTER);
        navigation.add(next, BorderLayout.EAST);
        navigation.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        navigation.setAlignmentX(LEFT_ALIGNMENT);
        return navigation;
    }

    private String formatValue(String responseMetric, double value)
    {
        return "xp".equals(responseMetric)
            ? "+" + QuantityFormatter.quantityToStackSize((long) value) + " xp"
            : "+" + String.format("%.2f h", value);
    }

    private enum MetricOption
    {
        XP("XP ganho", "xp"), EHP("EHP ganho", "ehp"), EHB("EHB ganho", "ehb");
        private final String label;
        private final String apiValue;
        MetricOption(String label, String apiValue) { this.label = label; this.apiValue = apiValue; }
        @Override public String toString() { return label; }
    }

    private enum PeriodOption
    {
        CURRENT("Mês atual", "current"), PREVIOUS("Último mês", "previous");
        private final String label;
        private final String apiValue;
        PeriodOption(String label, String apiValue) { this.label = label; this.apiValue = apiValue; }
        @Override public String toString() { return label; }
    }
}
