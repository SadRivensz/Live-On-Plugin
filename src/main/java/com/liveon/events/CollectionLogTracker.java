package com.liveon.events;

import com.liveon.LiveOnConfig;
import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.auth.ClanAccessManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CollectionLogTracker
{
    private static final Pattern NEW_ITEM = Pattern.compile(
        "New item added to your collection log: (.+)",
        Pattern.CASE_INSENSITIVE
    );

    private final LiveOnConfig config;
    private final ClanAccessManager accessManager;
    private final LiveOnApiClient apiClient;

    @Inject
    public CollectionLogTracker(LiveOnConfig config, ClanAccessManager accessManager, LiveOnApiClient apiClient)
    {
        this.config = config;
        this.accessManager = accessManager;
        this.apiClient = apiClient;
    }

    public void onGameMessage(String message)
    {
        if (!config.sendCollectionLog() || !config.syncEnabled() || !accessManager.isAuthorized())
        {
            return;
        }
        Matcher matcher = NEW_ITEM.matcher(message);
        if (!matcher.matches())
        {
            return;
        }

        ApiModels.CollectionLogPayload payload = new ApiModels.CollectionLogPayload();
        payload.itemName = matcher.group(1).trim();
        payload.source = "collection_log_message";
        apiClient.submitCollectionLog(payload, new ApiCallback<ApiModels.StatusResponse>()
        {
            @Override
            public void onSuccess(ApiModels.StatusResponse value)
            {
                log.debug("Live On collection log entry submitted");
            }

            @Override
            public void onFailure(String error)
            {
                log.debug("Live On collection log submission failed: {}", error);
            }
        });
    }
}
