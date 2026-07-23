package com.liveon.ui.cards;

import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.assets.HiscoreIcons;
import com.liveon.ui.CompactTabbedPane;
import com.liveon.ui.PanelStyles;
import com.liveon.ui.ResponsiveGridPanel;
import com.liveon.ui.RuneProfileDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.item.ItemPrice;

public class MembersCard extends JPanel
{
    private static final int MEMBER_PAGE_SIZE = 25;
    private final LiveOnApiClient apiClient;
    private final ItemManager itemManager;
    private final HiscoreIcons hiscoreIcons;
    private final JTextField searchField = new JTextField();
    private final JPanel content = PanelStyles.verticalPanel();
    private final Map<String, ApiModels.RuneProfileData> runeProfiles = new HashMap<>();
    private int currentPage = 1;
    private int totalPages = 1;
    private String currentQuery = "";

    public MembersCard(
        LiveOnApiClient apiClient,
        ItemManager itemManager,
        HiscoreIcons hiscoreIcons)
    {
        this.apiClient = apiClient;
        this.itemManager = itemManager;
        this.hiscoreIcons = hiscoreIcons;
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setOpaque(false);
        add(PanelStyles.sectionTitle("Explorar membros", PanelStyles.UiIcon.PEOPLE));
        PanelStyles.addGap(this);

        JPanel search = new JPanel(new BorderLayout(5, 0));
        search.setOpaque(false);
        PanelStyles.styleField(searchField);
        searchField.setToolTipText("Nome do membro");
        JButton button = PanelStyles.button("", PanelStyles.UiIcon.SEARCH);
        button.setToolTipText("Buscar membro");
        button.addActionListener(event -> search());
        searchField.addActionListener(event -> search());
        search.add(searchField, BorderLayout.CENTER);
        search.add(button, BorderLayout.EAST);
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
        search.setAlignmentX(LEFT_ALIGNMENT);
        add(search);
        PanelStyles.addGap(this);
        add(content);
    }

    public void search()
    {
        currentQuery = searchField.getText().trim();
        currentPage = 1;
        requestMembers();
    }

    private void requestMembers()
    {
        PanelStyles.showMessage(
            content,
            currentQuery.isEmpty() ? "Carregando membros do clã..." : "Buscando membros..."
        );
        apiClient.searchMembers(
            currentQuery,
            currentPage,
            MEMBER_PAGE_SIZE,
            new ApiCallback<ApiModels.MemberListResponse>()
        {
            @Override public void onSuccess(ApiModels.MemberListResponse response) { SwingUtilities.invokeLater(() -> renderSearch(response)); }
            @Override public void onFailure(String message) { SwingUtilities.invokeLater(() -> PanelStyles.showMessage(content, message)); }
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
            totalPages = Math.max(1, response.totalPages);
            currentPage = Math.max(1, response.page);
            JLabel count = PanelStyles.muted(
                response.total + (response.total == 1 ? " membro" : " membros")
            );
            content.add(count);
            PanelStyles.addGap(content);
            String previousRole = null;
            for (ApiModels.MemberSummary member : response.members)
            {
                String role = member.role == null ? "member" : member.role;
                if (!role.equals(previousRole))
                {
                    JLabel roleTitle = PanelStyles.title(hiscoreIcons.rankDisplayName(role));
                    hiscoreIcons.addRankIcon(roleTitle, role);
                    content.add(roleTitle);
                    PanelStyles.addGap(content);
                    previousRole = role;
                }
                JButton memberButton = PanelStyles.secondaryButton(member.rsn, null);
                hiscoreIcons.addRankIcon(memberButton, role);
                memberButton.setToolTipText("Rank: " + hiscoreIcons.rankDisplayName(role));
                memberButton.setHorizontalAlignment(JButton.LEFT);
                memberButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                memberButton.addActionListener(event -> loadProfile(member.rsn));
                content.add(memberButton);
                PanelStyles.addGap(content);
            }
            if (totalPages > 1)
            {
                content.add(memberPagination());
            }
        }
        content.revalidate();
        content.repaint();
    }

    private JPanel memberPagination()
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
            requestMembers();
        });
        next.addActionListener(event ->
        {
            currentPage++;
            requestMembers();
        });
        previous.setPreferredSize(new Dimension(32, 28));
        next.setPreferredSize(new Dimension(32, 28));
        navigation.add(previous, BorderLayout.WEST);
        navigation.add(page, BorderLayout.CENTER);
        navigation.add(next, BorderLayout.EAST);
        navigation.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        navigation.setAlignmentX(LEFT_ALIGNMENT);
        return navigation;
    }

    private void loadProfile(String rsn)
    {
        PanelStyles.showMessage(content, "Carregando " + rsn + "...");
        apiClient.getMember(rsn, new ApiCallback<ApiModels.MemberProfile>()
        {
            @Override public void onSuccess(ApiModels.MemberProfile profile) { SwingUtilities.invokeLater(() -> renderProfile(profile)); }
            @Override public void onFailure(String message) { SwingUtilities.invokeLater(() -> PanelStyles.showMessage(content, message)); }
        });
    }

    private void renderProfile(ApiModels.MemberProfile profile)
    {
        content.removeAll();
        if (profile == null || profile.member == null)
        {
            content.add(PanelStyles.muted("Perfil indisponível."));
            return;
        }

        ApiModels.MemberSummary member = profile.member;
        JPanel summary = PanelStyles.heroCard();
        JPanel top = new JPanel(new BorderLayout(7, 0));
        top.setOpaque(false);
        JLabel avatar = new JLabel();
        avatar.setPreferredSize(new Dimension(34, 34));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setIcon(hiscoreIcons.accountTypeIcon(member.accountType, 24));
        top.add(avatar, BorderLayout.WEST);
        JPanel identity = PanelStyles.verticalPanel();
        JLabel name = PanelStyles.title(member.rsn);
        hiscoreIcons.addRankIcon(name, member.role);
        identity.add(name);
        JLabel role = new JLabel(
            member.role == null ? "Membro" : hiscoreIcons.rankDisplayName(member.role)
        );
        role.setForeground(PanelStyles.MUTED);
        role.setFont(FontManager.getRunescapeSmallFont());
        role.setAlignmentX(LEFT_ALIGNMENT);
        identity.add(role);
        top.add(identity, BorderLayout.CENTER);
        summary.add(top);
        loadAvatar(member.avatarUrl, avatar);
        PanelStyles.addGap(summary);
        JPanel overview = new ResponsiveGridPanel(86, 46, 2, 5);
        overview.add(metricTile("XP TOTAL", QuantityFormatter.quantityToStackSize(member.totalXp), PanelStyles.PURPLE));
        overview.add(metricTile("EHP", String.format("%.2f", member.ehp), PanelStyles.GREEN));
        overview.add(metricTile("EHB", String.format("%.2f", member.ehb), PanelStyles.GOLD));
        overview.add(metricTile("ATUALIZADO", shortDate(profile.lastUpdated), PanelStyles.TEXT));
        summary.add(overview);
        PanelStyles.addGap(summary);
        JButton runeProfile = PanelStyles.secondaryButton(
            "RuneProfile · verificando...",
            PanelStyles.UiIcon.SEARCH
        );
        runeProfile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        runeProfile.setEnabled(false);
        summary.add(runeProfile);
        prepareRuneProfile(member.rsn, runeProfile);
        content.add(summary);
        PanelStyles.addGap(content);

        JTabbedPane details = new CompactTabbedPane();
        details.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        PanelStyles.styleTabs(details);
        details.addTab("Skills", skillGrid(profile.skills));
        details.addTab("Boss", scoreGrid(profile.bosses));
        details.addTab("Ativ.", scoreGrid(profile.activities));
        details.addTab("Log", collectionGrid(profile.collectionLog));
        details.setAlignmentX(LEFT_ALIGNMENT);
        content.add(details);
        content.revalidate();
        content.repaint();
    }

    private void prepareRuneProfile(String rsn, JButton button)
    {
        String key = rsn.toLowerCase(Locale.ROOT);
        ApiModels.RuneProfileData cached = runeProfiles.get(key);
        if (cached != null)
        {
            configureRuneProfileButton(button, cached);
            return;
        }
        apiClient.getRuneProfile(rsn, new ApiCallback<ApiModels.RuneProfileResponse>()
        {
            @Override
            public void onSuccess(ApiModels.RuneProfileResponse response)
            {
                SwingUtilities.invokeLater(() ->
                {
                    if (response == null || response.profile == null || response.profile.collectionLog == null)
                    {
                        showRuneProfileUnavailable(button, "Perfil incompleto no RuneProfile");
                        return;
                    }
                    runeProfiles.put(key, response.profile);
                    configureRuneProfileButton(button, response.profile);
                });
            }

            @Override
            public void onFailure(String message)
            {
                SwingUtilities.invokeLater(() -> showRuneProfileUnavailable(button, message));
            }
        });
    }

    private void configureRuneProfileButton(JButton button, ApiModels.RuneProfileData runeProfile)
    {
        button.setText(
            "RuneProfile  "
                + runeProfile.collectionLog.obtained
                + "/"
                + runeProfile.collectionLog.total
        );
        button.setToolTipText("Abrir bosses, clues e atividades detalhadas");
        button.setEnabled(true);
        button.addActionListener(event ->
            RuneProfileDialog.show(this, runeProfile, itemManager)
        );
    }

    private void showRuneProfileUnavailable(JButton button, String reason)
    {
        button.setText("Sem perfil no RuneProfile");
        button.setToolTipText(reason);
        button.setEnabled(false);
    }

    private JPanel metricTile(String name, String value, Color accent)
    {
        JPanel tile = new JPanel(new BorderLayout());
        tile.setBackground(PanelStyles.SURFACE);
        tile.setBorder(BorderFactory.createLineBorder(PanelStyles.BORDER));
        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setForeground(PanelStyles.MUTED);
        JLabel valueLabel = new JLabel(value == null ? "--" : value, SwingConstants.CENTER);
        valueLabel.setForeground(accent);
        tile.add(nameLabel, BorderLayout.NORTH);
        tile.add(valueLabel, BorderLayout.CENTER);
        return tile;
    }

    private JPanel skillGrid(List<ApiModels.MetricEntry> entries)
    {
        JPanel grid = new ResponsiveGridPanel(58, 27, 3, 2);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
        if (entries == null || entries.isEmpty())
        {
            return emptyDetails("Sem dados de skills.");
        }
        for (ApiModels.MetricEntry entry : entries)
        {
            grid.add(metricTile(
                entry,
                String.valueOf(entry.level),
                entry.name + " · " + QuantityFormatter.quantityToStackSize(entry.value) + " xp"
            ));
        }
        return grid;
    }

    private JPanel scoreGrid(List<ApiModels.MetricEntry> entries)
    {
        JPanel grid = new ResponsiveGridPanel(58, 27, 3, 2);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
        if (entries == null || entries.isEmpty())
        {
            return emptyDetails("Sem dados desta categoria.");
        }
        for (ApiModels.MetricEntry entry : entries)
        {
            grid.add(metricTile(
                entry,
                entry.value <= 0 ? "--" : QuantityFormatter.quantityToStackSize(entry.value),
                entry.name
            ));
        }
        return grid;
    }

    private JPanel metricTile(ApiModels.MetricEntry entry, String value, String tooltip)
    {
        JPanel tile = new JPanel(new BorderLayout(3, 0));
        tile.setOpaque(false);
        tile.setToolTipText(tooltip);
        tile.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        JLabel icon = new JLabel("", SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(20, 20));
        icon.setToolTipText(tooltip);
        hiscoreIcons.addMetricIcon(icon, entry.name);

        JLabel number = new JLabel(value);
        number.setForeground(PanelStyles.TEXT);
        number.setFont(FontManager.getRunescapeSmallFont());
        number.setToolTipText(tooltip);
        tile.add(icon, BorderLayout.WEST);
        tile.add(number, BorderLayout.CENTER);
        return tile;
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
                // Discord linking is optional; keep the account type icon.
            }
        });
    }

    private JPanel collectionGrid(List<String> entries)
    {
        JPanel grid = new ResponsiveGridPanel(58, 27, 3, 2);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
        if (entries == null || entries.isEmpty())
        {
            return emptyDetails("Nenhuma entrada sincronizada.");
        }
        for (String item : entries)
        {
            JLabel tile = new JLabel("✓");
            tile.setForeground(PanelStyles.GREEN);
            tile.setToolTipText(item);
            tile.setBorder(BorderFactory.createEmptyBorder(5, 3, 5, 3));
            grid.add(tile);
            CompletableFuture.supplyAsync(() -> itemManager.search(item)).thenAccept(found ->
            {
                if (found != null && !found.isEmpty())
                {
                    ItemPrice result = found.get(0);
                    SwingUtilities.invokeLater(() -> itemManager.getImage(result.getId()).addTo(tile));
                }
            });
        }
        return grid;
    }

    private JPanel emptyDetails(String message)
    {
        JPanel empty = PanelStyles.verticalPanel();
        empty.setBorder(BorderFactory.createEmptyBorder(8, 7, 8, 7));
        empty.add(PanelStyles.muted(message));
        return empty;
    }

    private String shortDate(String value)
    {
        if (value == null || value.length() < 10)
        {
            return "--";
        }
        return value.substring(8, 10) + "/" + value.substring(5, 7);
    }
}
