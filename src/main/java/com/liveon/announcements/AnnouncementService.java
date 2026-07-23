package com.liveon.announcements;

import com.liveon.LiveOnConfig;
import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.auth.AccessSession;
import com.liveon.auth.ClanAccessManager;
import java.awt.Color;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

@Slf4j
@Singleton
public class AnnouncementService
{
    private static final String PREFIX = "<col=ffc83d>[Live On]</col> ";

    private final LiveOnConfig config;
    private final LiveOnApiClient apiClient;
    private final ClanAccessManager accessManager;
    private final ChatMessageManager chatMessageManager;
    private final ClientThread clientThread;
    private final Set<Long> shownAnnouncementIds = ConcurrentHashMap.newKeySet();
    private final Consumer<AccessSession> sessionListener = this::onSessionChanged;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollTask;
    private volatile boolean primed;
    private volatile boolean localLoginMessagePending;

    @Inject
    public AnnouncementService(
        LiveOnConfig config,
        LiveOnApiClient apiClient,
        ClanAccessManager accessManager,
        ChatMessageManager chatMessageManager,
        ClientThread clientThread)
    {
        this.config = config;
        this.apiClient = apiClient;
        this.accessManager = accessManager;
        this.chatMessageManager = chatMessageManager;
        this.clientThread = clientThread;
    }

    public void start()
    {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable ->
        {
            Thread thread = new Thread(runnable, "live-on-announcements");
            thread.setDaemon(true);
            return thread;
        });
        accessManager.addListener(sessionListener);
    }

    public void stop()
    {
        accessManager.removeListener(sessionListener);
        if (pollTask != null)
        {
            pollTask.cancel(false);
            pollTask = null;
        }
        if (scheduler != null)
        {
            scheduler.shutdownNow();
            scheduler = null;
        }
        shownAnnouncementIds.clear();
        primed = false;
        localLoginMessagePending = false;
    }

    /** Marks a real login of the local account, not a clan-list refresh. */
    public void onLocalLogin()
    {
        localLoginMessagePending = true;
    }

    public void onLocalLogout()
    {
        localLoginMessagePending = false;
    }

    private void onSessionChanged(AccessSession session)
    {
        if (!session.isAuthorized())
        {
            if (pollTask != null)
            {
                pollTask.cancel(false);
                pollTask = null;
            }
            primed = false;
            shownAnnouncementIds.clear();
            return;
        }

        if (localLoginMessagePending
            && config.showLoginMessage()
            && session.getLoginMessage() != null
            && !session.getLoginMessage().isEmpty())
        {
            queueChat(session.getLoginMessage());
            localLoginMessagePending = false;
        }
        poll();
        if (pollTask == null && scheduler != null)
        {
            pollTask = scheduler.scheduleAtFixedRate(
                this::poll,
                config.announcementPollMinutes(),
                config.announcementPollMinutes(),
                TimeUnit.MINUTES
            );
        }
    }

    private void poll()
    {
        if (!accessManager.isAuthorized())
        {
            return;
        }
        apiClient.getAnnouncements(new ApiCallback<ApiModels.AnnouncementResponse>()
        {
            @Override
            public void onSuccess(ApiModels.AnnouncementResponse response)
            {
                if (response == null || response.announcements == null)
                {
                    return;
                }
                if (!primed)
                {
                    for (ApiModels.Announcement announcement : response.announcements)
                    {
                        shownAnnouncementIds.add(announcement.id);
                    }
                    primed = true;
                    return;
                }
                for (ApiModels.Announcement announcement : response.announcements)
                {
                    if (shownAnnouncementIds.add(announcement.id) && "clan".equals(announcement.kind))
                    {
                        queueChat(announcement.title + ": " + announcement.message);
                    }
                }
            }

            @Override
            public void onFailure(String message)
            {
                log.debug("Could not poll Live On announcements: {}", message);
            }
        });
    }

    private void queueChat(String message)
    {
        clientThread.invoke(() -> chatMessageManager.queue(
            QueuedMessage.builder()
                .type(ChatMessageType.BROADCAST)
                .runeLiteFormattedMessage(PREFIX + message)
                .build()
        ));
    }
}
