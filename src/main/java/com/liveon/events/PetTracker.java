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
public class PetTracker
{
    private static final int MAX_WAIT_TICKS = 5;
    private static final Pattern PET_MESSAGE = Pattern.compile(
        "You (?:have a funny feeling like you|feel something weird sneaking|feel like you would have been followed).+",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern UNTRADEABLE_DROP = Pattern.compile("Untradeable drop: (.+)", Pattern.CASE_INSENSITIVE);

    private final LiveOnConfig config;
    private final ClanAccessManager accessManager;
    private final LiveOnApiClient apiClient;
    private String untradeableCandidate;
    private int candidateAge;
    private String pendingGameMessage;
    private int pendingTicks;

    @Inject
    public PetTracker(LiveOnConfig config, ClanAccessManager accessManager, LiveOnApiClient apiClient)
    {
        this.config = config;
        this.accessManager = accessManager;
        this.apiClient = apiClient;
    }

    public void onGameMessage(String message)
    {
        Matcher untradeable = UNTRADEABLE_DROP.matcher(message);
        if (untradeable.matches())
        {
            untradeableCandidate = untradeable.group(1).trim();
            candidateAge = 0;
            if (pendingGameMessage != null)
            {
                submitPending(untradeableCandidate);
            }
            return;
        }
        if (!config.sendPets() || !config.syncEnabled() || !accessManager.isAuthorized() || !PET_MESSAGE.matcher(message).matches())
        {
            return;
        }

        pendingGameMessage = message;
        pendingTicks = 0;
        if (untradeableCandidate != null && candidateAge <= MAX_WAIT_TICKS)
        {
            submitPending(untradeableCandidate);
        }
    }

    public void onGameTick()
    {
        if (untradeableCandidate != null && ++candidateAge > MAX_WAIT_TICKS)
        {
            untradeableCandidate = null;
        }
        if (pendingGameMessage != null && ++pendingTicks >= MAX_WAIT_TICKS)
        {
            submitPending("Novo pet");
        }
    }

    public void reset()
    {
        untradeableCandidate = null;
        pendingGameMessage = null;
        candidateAge = 0;
        pendingTicks = 0;
    }

    private void submitPending(String petName)
    {
        ApiModels.PetPayload payload = new ApiModels.PetPayload();
        payload.petName = petName;
        payload.source = "game_message";
        payload.gameMessage = pendingGameMessage;
        reset();

        apiClient.submitPet(payload, new ApiCallback<ApiModels.StatusResponse>()
        {
            @Override
            public void onSuccess(ApiModels.StatusResponse value)
            {
                log.debug("Live On pet submitted");
            }

            @Override
            public void onFailure(String error)
            {
                log.debug("Live On pet submission failed: {}", error);
            }
        });
    }
}
