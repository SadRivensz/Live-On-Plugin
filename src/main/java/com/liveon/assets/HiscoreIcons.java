package com.liveon.assets;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.plugins.hiscore.HiscorePanel;
import net.runelite.client.util.ImageUtil;

/**
 * Reuses the exact sprite IDs and account-type resources used by RuneLite's
 * built-in Hiscore panel.
 */
@Singleton
public class HiscoreIcons
{
    private static final int METRIC_ICON_SIZE = 18;
    private static final int RANK_ICON_SIZE = 13;
    private static final Map<String, HiscoreSkill> METRIC_ALIASES = aliases();
    private static final Map<String, String> CUSTOM_METRIC_ICONS = customMetricIcons();

    private final SpriteManager spriteManager;
    private final AssetCatalog catalog;

    @Inject
    public HiscoreIcons(SpriteManager spriteManager, AssetCatalog catalog)
    {
        this.spriteManager = spriteManager;
        this.catalog = catalog;
    }

    public void addMetricIcon(JLabel label, String metricName)
    {
        String customResource = CUSTOM_METRIC_ICONS.get(normalize(metricName));
        if (customResource != null)
        {
            BufferedImage image = ImageUtil.loadImageResource(
                HiscoreIcons.class,
                "/icons/clues/" + customResource
            );
            label.setIcon(scaledIcon(image, METRIC_ICON_SIZE));
            return;
        }
        HiscoreSkill metric = findMetric(metricName);
        if (metric == null)
        {
            return;
        }
        spriteManager.getSpriteAsync(metric.getSpriteId(), 0, image ->
            SwingUtilities.invokeLater(() -> label.setIcon(scaledIcon(image, METRIC_ICON_SIZE))));
    }

    public void addRankIcon(JLabel label, String role)
    {
        ImageIcon icon = rankIcon(role);
        if (icon != null)
        {
            label.setIcon(icon);
            label.setIconTextGap(4);
        }
    }

    public void addRankIcon(AbstractButton button, String role)
    {
        ImageIcon icon = rankIcon(role);
        if (icon != null)
        {
            button.setIcon(icon);
            button.setIconTextGap(5);
        }
    }

    public ImageIcon accountTypeIcon(String accountType, int size)
    {
        String resource = accountTypeResource(accountType);
        BufferedImage image = ImageUtil.loadImageResource(
            HiscorePanel.class,
            "/net/runelite/client/plugins/hiscore/" + resource + ".png"
        );
        return new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }

    public String rankDisplayName(String role)
    {
        AssetCatalog.RankEntry entry = catalog.getRank(role);
        return entry == null ? role : entry.displayName;
    }

    private ImageIcon rankIcon(String role)
    {
        AssetCatalog.RankEntry rank = catalog.getRank(role);
        if (rank == null)
        {
            return null;
        }
        String resource = rank.icon == null || rank.icon.isEmpty()
            ? "icons/ranks/" + rank.key + ".png"
            : rank.icon;
        if (!resource.startsWith("/"))
        {
            resource = "/" + resource;
        }
        BufferedImage image = ImageUtil.loadImageResource(
            HiscoreIcons.class,
            resource
        );
        return image == null ? null : scaledIcon(image, RANK_ICON_SIZE);
    }

    private ImageIcon scaledIcon(BufferedImage image, int size)
    {
        return new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }

    private HiscoreSkill findMetric(String name)
    {
        String wanted = normalize(name);
        HiscoreSkill aliased = METRIC_ALIASES.get(wanted);
        if (aliased != null)
        {
            return aliased;
        }
        for (HiscoreSkill skill : HiscoreSkill.values())
        {
            if (normalize(skill.name()).equals(wanted) || normalize(skill.getName()).equals(wanted))
            {
                return skill;
            }
        }
        return null;
    }

    private static Map<String, HiscoreSkill> aliases()
    {
        Map<String, HiscoreSkill> aliases = new HashMap<>();
        aliases.put("cluescrollsall", HiscoreSkill.CLUE_SCROLL_ALL);
        aliases.put("cluescrollsbeginner", HiscoreSkill.CLUE_SCROLL_BEGINNER);
        aliases.put("cluescrollseasy", HiscoreSkill.CLUE_SCROLL_EASY);
        aliases.put("cluescrollsmedium", HiscoreSkill.CLUE_SCROLL_MEDIUM);
        aliases.put("cluescrollshard", HiscoreSkill.CLUE_SCROLL_HARD);
        aliases.put("cluescrollselite", HiscoreSkill.CLUE_SCROLL_ELITE);
        aliases.put("cluescrollsmaster", HiscoreSkill.CLUE_SCROLL_MASTER);
        aliases.put("pvparena", HiscoreSkill.PVP_ARENA_RANK);
        aliases.put("guardiansoftherift", HiscoreSkill.RIFTS_CLOSED);
        aliases.put("runecrafting", HiscoreSkill.RUNECRAFT);
        return aliases;
    }

    private static Map<String, String> customMetricIcons()
    {
        Map<String, String> icons = new HashMap<>();
        icons.put("cluescrollseasy", "clue-easy.png");
        icons.put("cluescrollsmedium", "clue-medium.png");
        icons.put("cluescrollshard", "clue-hard.png");
        icons.put("cluescrollselite", "clue-elite.png");
        icons.put("cluescrollsmaster", "clue-master.png");
        return icons;
    }

    private String accountTypeResource(String value)
    {
        if (value == null)
        {
            return "normal";
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        switch (normalized)
        {
            case "ironman":
            case "hardcore_ironman":
            case "ultimate_ironman":
                return normalized;
            default:
                return "normal";
        }
    }

    private String normalize(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
