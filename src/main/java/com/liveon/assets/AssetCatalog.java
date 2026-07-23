package com.liveon.assets;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads the small, human-editable catalogs stored in resources/catalog.
 *
 * The complete item database intentionally remains RuneLite's ItemManager:
 * it is updated with the game cache and avoids maintaining thousands of stale
 * item IDs here. items.json contains only Live On-specific aliases and rules.
 */
@Slf4j
@Singleton
public class AssetCatalog
{
    private final Gson gson;
    private List<BossEntry> bosses = Collections.emptyList();
    private List<RankEntry> ranks = Collections.emptyList();
    private List<ItemRule> items = Collections.emptyList();
    private Map<Integer, ItemRule> itemRulesById = Collections.emptyMap();
    private Map<String, RankEntry> ranksByKey = Collections.emptyMap();

    @Inject
    public AssetCatalog(Gson gson)
    {
        this.gson = gson;
    }

    public void load()
    {
        bosses = loadList("/catalog/bosses.json", BossCatalog.class).bosses;
        ranks = loadList("/catalog/ranks.json", RankCatalog.class).ranks;
        items = loadList("/catalog/items.json", ItemCatalog.class).items;
        Map<Integer, ItemRule> ruleMap = new HashMap<>();
        for (ItemRule rule : items)
        {
            ruleMap.put(rule.itemId, rule);
        }
        itemRulesById = Collections.unmodifiableMap(ruleMap);
        Map<String, RankEntry> rankMap = new HashMap<>();
        for (RankEntry rank : ranks)
        {
            rankMap.put(rank.key.toLowerCase(), rank);
        }
        ranksByKey = Collections.unmodifiableMap(rankMap);
        log.debug("Loaded Live On catalogs: {} bosses, {} ranks, {} item rules", bosses.size(), ranks.size(), items.size());
    }

    public List<BossEntry> getBosses()
    {
        return Collections.unmodifiableList(bosses);
    }

    public List<RankEntry> getRanks()
    {
        return Collections.unmodifiableList(ranks);
    }

    public List<ItemRule> getItems()
    {
        return Collections.unmodifiableList(items);
    }

    public ItemRule getItemRule(int itemId)
    {
        return itemRulesById.get(itemId);
    }

    public RankEntry getRank(String key)
    {
        if (key == null)
        {
            return null;
        }
        return ranksByKey.get(key.toLowerCase().replace(' ', '_'));
    }

    private <T> T loadList(String path, Class<T> type)
    {
        try (InputStream stream = AssetCatalog.class.getResourceAsStream(path))
        {
            if (stream == null)
            {
                throw new IllegalStateException("Missing catalog " + path);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
            {
                return gson.fromJson(reader, type);
            }
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Could not read catalog " + path, exception);
        }
    }

    private static final class BossCatalog
    {
        private List<BossEntry> bosses = Collections.emptyList();
    }

    private static final class RankCatalog
    {
        private List<RankEntry> ranks = Collections.emptyList();
    }

    private static final class ItemCatalog
    {
        private List<ItemRule> items = Collections.emptyList();
    }

    public static final class BossEntry
    {
        public String key;
        public String displayName;
        public String womMetric;
        public List<Integer> npcIds = Collections.emptyList();
        public String icon;
        public int iconItemId;
    }

    public static final class RankEntry
    {
        public String key;
        public String displayName;
        public boolean staff;
        public String icon;
        public int iconIndex = -1;
    }

    public static final class ItemRule
    {
        public int itemId;
        public String alias;
        public boolean alwaysNotify;
        public String iconOverride;
    }
}
