package com.liveon;

import com.google.inject.Provides;
import com.liveon.announcements.AnnouncementService;
import com.liveon.assets.AssetCatalog;
import com.liveon.assets.HiscoreIcons;
import com.liveon.auth.ClanAccessManager;
import com.liveon.events.CollectionLogTracker;
import com.liveon.events.DropTracker;
import com.liveon.events.PetTracker;
import com.liveon.ui.LiveOnPanel;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
    name = "Live On",
    description = "Hub do clã Live On com drops, pets, anúncios, perfis e rankings.",
    tags = {"live on", "clan", "drops", "pets", "ehp", "ehb", "rankings"}
)
public class LiveOnPlugin extends Plugin
{
    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ClientThread clientThread;

    @Inject
    private Client client;

    @Inject
    private LiveOnConfig config;

    @Inject
    private ClanAccessManager accessManager;

    @Inject
    private com.liveon.api.LiveOnApiClient apiClient;

    @Inject
    private DropTracker dropTracker;

    @Inject
    private PetTracker petTracker;

    @Inject
    private CollectionLogTracker collectionLogTracker;

    @Inject
    private AnnouncementService announcementService;

    @Inject
    private AssetCatalog assetCatalog;

    @Inject
    private ItemManager itemManager;

    @Inject
    private HiscoreIcons hiscoreIcons;

    private NavigationButton navigationButton;
    private LiveOnPanel panel;
    private boolean localLoginActive;

    @Provides
    LiveOnConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(LiveOnConfig.class);
    }

    @Override
    protected void startUp()
    {
        assetCatalog.load();
        panel = new LiveOnPanel(accessManager, apiClient, itemManager, hiscoreIcons);
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/liveon-icon.png");
        navigationButton = NavigationButton.builder()
            .tooltip("Live On")
            .icon(icon)
            .priority(7)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navigationButton);
        announcementService.start();
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            localLoginActive = true;
            announcementService.onLocalLogin();
            clientThread.invokeLater(accessManager::authorize);
        }
        log.info("Live On plugin started");
    }

    @Override
    protected void shutDown()
    {
        announcementService.stop();
        accessManager.clear();
        if (panel != null)
        {
            panel.shutdown();
            panel = null;
        }
        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(navigationButton);
            navigationButton = null;
        }
        log.info("Live On plugin stopped");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            if (!localLoginActive)
            {
                localLoginActive = true;
                announcementService.onLocalLogin();
            }
            clientThread.invokeLater(accessManager::authorize);
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            localLoginActive = false;
            announcementService.onLocalLogout();
            petTracker.reset();
            accessManager.clear();
        }
    }

    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged event)
    {
        if (!event.isGuest())
        {
            clientThread.invokeLater(accessManager::authorize);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!LiveOnConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }
        if (config.syncEnabled())
        {
            clientThread.invokeLater(accessManager::authorize);
        }
        else
        {
            accessManager.clear();
        }
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
        dropTracker.onNpcLoot(event);
    }

    @Subscribe
    public void onLootReceived(LootReceived event)
    {
        dropTracker.onOtherLoot(event);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
        {
            return;
        }
        String message = Text.removeTags(event.getMessage());
        petTracker.onGameMessage(message);
        collectionLogTracker.onGameMessage(message);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        petTracker.onGameTick();
    }
}
