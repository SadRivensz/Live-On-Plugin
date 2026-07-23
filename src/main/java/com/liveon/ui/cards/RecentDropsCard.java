package com.liveon.ui.cards;

import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.ui.PanelStyles;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.QuantityFormatter;

public class RecentDropsCard extends JPanel
{
    private final LiveOnApiClient apiClient;
    private final ItemManager itemManager;
    private final JPanel content = PanelStyles.verticalPanel();
    private final Map<String, BufferedImage> screenshots = new ConcurrentHashMap<>();
    private int currentPage = 1;
    private int totalPages = 1;

    public RecentDropsCard(LiveOnApiClient apiClient, ItemManager itemManager)
    {
        this.apiClient = apiClient;
        this.itemManager = itemManager;
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(PanelStyles.sectionTitle("Drops recentes", PanelStyles.UiIcon.DROP), BorderLayout.WEST);
        JButton refresh = PanelStyles.secondaryButton("", PanelStyles.UiIcon.REFRESH);
        refresh.setToolTipText("Atualizar drops");
        refresh.addActionListener(event -> refresh());
        header.add(refresh, BorderLayout.EAST);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        header.setAlignmentX(LEFT_ALIGNMENT);
        add(header);
        PanelStyles.addGap(this);
        add(content);
    }

    public void refresh()
    {
        PanelStyles.showMessage(content, "Carregando drops...");
        apiClient.getRecentDrops(currentPage, 5, new ApiCallback<ApiModels.DropListResponse>()
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
        currentPage = response == null ? 1 : Math.max(1, response.page);
        totalPages = response == null ? 1 : Math.max(1, response.totalPages);
        if (response == null || response.drops == null || response.drops.isEmpty())
        {
            content.add(PanelStyles.muted("Nenhum drop registrado."));
        }
        else
        {
            for (ApiModels.DropView drop : response.drops)
            {
                JPanel card = PanelStyles.card();
                JPanel top = new JPanel(new BorderLayout(5, 0));
                top.setOpaque(false);
                JLabel player = PanelStyles.title(ellipsize(drop.playerName, 12));
                player.setToolTipText(drop.playerName);
                player.setIcon(PanelStyles.icon(PanelStyles.UiIcon.DROP, PanelStyles.PURPLE, 16));
                top.add(player, BorderLayout.WEST);
                top.add(PanelStyles.badge(QuantityFormatter.quantityToStackSize(drop.totalValue) + " gp", PanelStyles.GOLD), BorderLayout.EAST);
                card.add(top);
                PanelStyles.addGap(card);
                card.add(PanelStyles.muted(ellipsize(drop.source, 21) + " · " + shortDate(drop.createdAt)));

                if (drop.screenshotUrl != null && !drop.screenshotUrl.isEmpty())
                {
                    PanelStyles.addGap(card);
                    ScreenshotButton preview = new ScreenshotButton();
                    preview.setForeground(PanelStyles.MUTED);
                    preview.setBackground(PanelStyles.BACKGROUND);
                    preview.setBorder(javax.swing.BorderFactory.createLineBorder(PanelStyles.BORDER));
                    preview.setFocusPainted(false);
                    preview.setPreferredSize(new Dimension(180, 104));
                    preview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));
                    preview.setAlignmentX(LEFT_ALIGNMENT);
                    preview.addActionListener(event -> openScreenshot(drop.screenshotUrl));
                    card.add(preview);
                    loadThumbnail(drop.screenshotUrl, preview);
                }

                if (drop.items != null)
                {
                    for (ApiModels.ItemPayload item : drop.items)
                    {
                        PanelStyles.addGap(card);
                        card.add(itemLine(item));
                    }
                }
                content.add(card);
                PanelStyles.addGap(content);
            }
        }
        if (totalPages > 1)
        {
            content.add(pagination());
        }
        content.revalidate();
        content.repaint();
    }

    private JPanel itemLine(ApiModels.ItemPayload item)
    {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        JLabel name = new JLabel(item.quantity + "×  " + ellipsize(item.name, 15));
        name.setToolTipText(item.quantity + "× " + item.name);
        name.setForeground(PanelStyles.TEXT);
        itemManager.getImage(item.itemId, item.quantity, item.quantity > 1).addTo(name);
        JLabel value = new JLabel(QuantityFormatter.quantityToStackSize(item.totalPrice) + " gp");
        value.setForeground(PanelStyles.GOLD);
        row.add(name, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private JPanel pagination()
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
            refresh();
        });
        next.addActionListener(event ->
        {
            currentPage++;
            refresh();
        });
        navigation.add(previous, BorderLayout.WEST);
        navigation.add(page, BorderLayout.CENTER);
        navigation.add(next, BorderLayout.EAST);
        navigation.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        navigation.setAlignmentX(LEFT_ALIGNMENT);
        return navigation;
    }

    private void loadThumbnail(String url, ScreenshotButton target)
    {
        BufferedImage cached = screenshots.get(url);
        if (cached != null)
        {
            setThumbnail(target, cached);
            return;
        }
        apiClient.getImage(url, new ApiCallback<BufferedImage>()
        {
            @Override
            public void onSuccess(BufferedImage image)
            {
                screenshots.put(url, image);
                SwingUtilities.invokeLater(() -> setThumbnail(target, image));
            }

            @Override
            public void onFailure(String message)
            {
                SwingUtilities.invokeLater(() -> target.setText("Screenshot indisponível"));
            }
        });
    }

    private void setThumbnail(ScreenshotButton button, BufferedImage image)
    {
        button.setImage(image);
        button.setToolTipText("Clique para ampliar");
    }

    private void openScreenshot(String url)
    {
        BufferedImage image = screenshots.get(url);
        if (image == null)
        {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Drops do Live On", Dialog.ModalityType.MODELESS);
        int width = Math.min(1100, image.getWidth());
        int height = Math.max(1, image.getHeight() * width / image.getWidth());
        JLabel label = new JLabel(new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
        JScrollPane scroll = new JScrollPane(label);
        scroll.getViewport().setBackground(PanelStyles.BACKGROUND);
        dialog.setContentPane(scroll);
        dialog.setSize(Math.min(width + 30, 1120), Math.min(height + 50, 760));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private String shortDate(String value)
    {
        if (value == null || value.length() < 16)
        {
            return "agora";
        }
        return value.substring(8, 10) + "/" + value.substring(5, 7) + " " + value.substring(11, 16);
    }

    private String ellipsize(String value, int max)
    {
        if (value == null)
        {
            return "--";
        }
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static final class ScreenshotButton extends JButton
    {
        private BufferedImage image;

        private ScreenshotButton()
        {
            super("Carregando screenshot...");
        }

        private void setImage(BufferedImage image)
        {
            this.image = image;
            setText("");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);
            if (image == null)
            {
                return;
            }
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int availableWidth = Math.max(1, getWidth() - 6);
            int availableHeight = Math.max(1, getHeight() - 6);
            double scale = Math.min(
                (double) availableWidth / image.getWidth(),
                (double) availableHeight / image.getHeight()
            );
            int width = Math.max(1, (int) (image.getWidth() * scale));
            int height = Math.max(1, (int) (image.getHeight() * scale));
            int x = (getWidth() - width) / 2;
            int y = (getHeight() - height) / 2;
            copy.drawImage(image, x, y, width, height, null);
            copy.dispose();
        }
    }
}
