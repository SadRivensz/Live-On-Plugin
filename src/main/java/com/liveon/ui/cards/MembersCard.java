package com.liveon.ui.cards;

import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.ui.PanelStyles;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.client.util.QuantityFormatter;

public class MembersCard extends JPanel
{
    private final LiveOnApiClient apiClient;
    private final JTextField searchField = new JTextField();
    private final JPanel content = PanelStyles.verticalPanel();

    public MembersCard(LiveOnApiClient apiClient)
    {
        this.apiClient = apiClient;
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setOpaque(false);
        add(PanelStyles.title("Membros"));
        PanelStyles.addGap(this);

        JPanel search = new JPanel(new BorderLayout(4, 0));
        search.setOpaque(false);
        searchField.setToolTipText("Nome do membro");
        JButton button = new JButton("Buscar");
        button.addActionListener(event -> search());
        searchField.addActionListener(event -> search());
        search.add(searchField, BorderLayout.CENTER);
        search.add(button, BorderLayout.EAST);
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        search.setAlignmentX(LEFT_ALIGNMENT);
        add(search);
        PanelStyles.addGap(this);
        add(content);
    }

    public void search()
    {
        PanelStyles.showMessage(content, "Buscando membros...");
        apiClient.searchMembers(searchField.getText().trim(), new ApiCallback<ApiModels.MemberListResponse>()
        {
            @Override
            public void onSuccess(ApiModels.MemberListResponse response)
            {
                SwingUtilities.invokeLater(() -> renderSearch(response));
            }

            @Override
            public void onFailure(String message)
            {
                SwingUtilities.invokeLater(() -> PanelStyles.showMessage(content, message));
            }
        });
    }

    private void renderSearch(ApiModels.MemberListResponse response)
    {
        content.removeAll();
        if (response == null || response.members == null || response.members.isEmpty())
        {
            content.add(PanelStyles.muted("Nenhum membro encontrado."));
        }
        else
        {
            for (ApiModels.MemberSummary member : response.members)
            {
                JButton memberButton = new JButton(member.rsn + " · " + member.role);
                memberButton.setHorizontalAlignment(JButton.LEFT);
                memberButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                memberButton.setAlignmentX(LEFT_ALIGNMENT);
                memberButton.addActionListener(event -> loadProfile(member.rsn));
                content.add(memberButton);
                PanelStyles.addGap(content);
            }
        }
        content.revalidate();
        content.repaint();
    }

    private void loadProfile(String rsn)
    {
        PanelStyles.showMessage(content, "Carregando " + rsn + "...");
        apiClient.getMember(rsn, new ApiCallback<ApiModels.MemberProfile>()
        {
            @Override
            public void onSuccess(ApiModels.MemberProfile profile)
            {
                SwingUtilities.invokeLater(() -> renderProfile(profile));
            }

            @Override
            public void onFailure(String message)
            {
                SwingUtilities.invokeLater(() -> PanelStyles.showMessage(content, message));
            }
        });
    }

    private void renderProfile(ApiModels.MemberProfile profile)
    {
        content.removeAll();
        if (profile == null || profile.member == null)
        {
            content.add(PanelStyles.muted("Perfil indisponível."));
        }
        else
        {
            ApiModels.MemberSummary member = profile.member;
            JPanel summary = PanelStyles.card();
            summary.add(PanelStyles.title(member.rsn));
            PanelStyles.addGap(summary);
            JPanel overview = new JPanel(new GridLayout(2, 2, 4, 4));
            overview.setOpaque(false);
            overview.add(metricTile("XP", QuantityFormatter.quantityToStackSize(member.totalXp)));
            overview.add(metricTile("Rank", member.role));
            overview.add(metricTile("EHP", String.format("%.2f", member.ehp)));
            overview.add(metricTile("EHB", String.format("%.2f", member.ehb)));
            overview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            overview.setAlignmentX(LEFT_ALIGNMENT);
            summary.add(overview);
            if (profile.lastUpdated != null)
            {
                PanelStyles.addGap(summary);
                summary.add(PanelStyles.muted("Atualizado: " + profile.lastUpdated));
            }
            content.add(summary);

            PanelStyles.addGap(content);
            JTabbedPane details = new JTabbedPane();
            details.addTab("Skills", metricTable(profile.skills, true));
            details.addTab("Bosses", metricTable(profile.bosses, false));
            details.addTab("Ativ.", metricTable(profile.activities, false));
            details.addTab("Log", collectionTable(profile.collectionLog));
            details.setAlignmentX(LEFT_ALIGNMENT);
            content.add(details);
        }
        content.revalidate();
        content.repaint();
    }

    private JPanel metricTile(String name, String value)
    {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setBackground(new java.awt.Color(30, 30, 30));
        JLabel nameLabel = PanelStyles.muted(name);
        JLabel valueLabel = PanelStyles.title(value == null ? "--" : value);
        nameLabel.setHorizontalAlignment(JLabel.CENTER);
        valueLabel.setHorizontalAlignment(JLabel.CENTER);
        tile.add(nameLabel, BorderLayout.NORTH);
        tile.add(valueLabel, BorderLayout.CENTER);
        return tile;
    }

    private JPanel metricTable(java.util.List<ApiModels.MetricEntry> entries, boolean skillColumns)
    {
        JPanel table = PanelStyles.verticalPanel();
        if (entries == null || entries.isEmpty())
        {
            table.add(PanelStyles.muted("Sem dados."));
            return table;
        }
        table.add(PanelStyles.line(skillColumns ? "Skill" : "Atividade", skillColumns ? "Nível · XP" : "KC / Pontos"));
        for (ApiModels.MetricEntry entry : entries)
        {
            String value = skillColumns
                ? entry.level + " · " + QuantityFormatter.quantityToStackSize(entry.value)
                : QuantityFormatter.quantityToStackSize(entry.value);
            table.add(PanelStyles.line(entry.name, value));
        }
        return table;
    }

    private JPanel collectionTable(java.util.List<String> entries)
    {
        JPanel table = PanelStyles.verticalPanel();
        if (entries == null || entries.isEmpty())
        {
            table.add(PanelStyles.muted("Nenhuma entrada sincronizada."));
            return table;
        }
        for (String item : entries)
        {
            table.add(PanelStyles.muted("• " + item));
        }
        return table;
    }
}
