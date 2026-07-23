package com.liveon.auth;

import com.liveon.LiveOnConfig;
import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;

@Slf4j
@Singleton
public class ClanAccessManager
{
    public static final String TARGET_CLAN = "Live On";
    private static final String PLUGIN_VERSION = "0.2.6";

    private final Client client;
    private final LiveOnConfig config;
    private final LiveOnApiClient apiClient;
    private final List<Consumer<AccessSession>> listeners = new CopyOnWriteArrayList<>();
    private volatile AccessSession session = AccessSession.denied("Aguardando login");
    private volatile boolean authorizationInProgress;

    @Inject
    public ClanAccessManager(Client client, LiveOnConfig config, LiveOnApiClient apiClient)
    {
        this.client = client;
        this.config = config;
        this.apiClient = apiClient;
    }

    public void addListener(Consumer<AccessSession> listener)
    {
        listeners.add(listener);
        listener.accept(session);
    }

    public void removeListener(Consumer<AccessSession> listener)
    {
        listeners.remove(listener);
    }

    public AccessSession getSession()
    {
        return session;
    }

    public boolean isAuthorized()
    {
        return session.isAuthorized();
    }

    /**
     * Must be called on the RuneLite client thread because it reads clan and
     * local-player state. The HTTP request itself is asynchronous.
     */
    public void authorize()
    {
        if (!config.syncEnabled())
        {
            deny("Ative a integração nas configurações");
            return;
        }
        if (authorizationInProgress)
        {
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        ClanChannel clanChannel = client.getClanChannel();
        if (localPlayer == null || localPlayer.getName() == null)
        {
            deny("Personagem ainda não disponível");
            return;
        }
        if (clanChannel == null || clanChannel.getName() == null
            || !TARGET_CLAN.equalsIgnoreCase(clanChannel.getName().trim()))
        {
            deny("Entre no clã Live On para usar a integração");
            return;
        }

        String rsn = localPlayer.getName();
        ClanChannelMember localMember = clanChannel.findMember(rsn);
        if (localMember == null)
        {
            deny("Seu personagem não foi encontrado na lista online do clã");
            return;
        }

        String rank = localMember.getRank() == null ? "member" : localMember.getRank().toString();
        ApiModels.AuthRequest request = new ApiModels.AuthRequest(
            rsn,
            clanChannel.getName(),
            rank,
            config.accessCode().trim(),
            PLUGIN_VERSION
        );

        authorizationInProgress = true;
        apiClient.verify(request, new ApiCallback<ApiModels.AuthResponse>()
        {
            @Override
            public void onSuccess(ApiModels.AuthResponse response)
            {
                authorizationInProgress = false;
                if (response == null || !response.authorized || response.token == null)
                {
                    deny(response == null ? "Acesso negado pelo servidor" : response.reason);
                    return;
                }
                apiClient.setSessionToken(response.token);
                updateSession(AccessSession.authorized(rsn, response.role, response.staff, response.loginMessage));
            }

            @Override
            public void onFailure(String message)
            {
                authorizationInProgress = false;
                deny(message);
            }
        });
    }

    public void clear()
    {
        authorizationInProgress = false;
        apiClient.clearSessionToken();
        updateSession(AccessSession.denied("Aguardando login"));
    }

    private void deny(String reason)
    {
        apiClient.clearSessionToken();
        updateSession(AccessSession.denied(reason == null || reason.isEmpty() ? "Acesso negado" : reason));
    }

    private void updateSession(AccessSession next)
    {
        session = next;
        log.debug("Live On access state changed: authorized={}", next.isAuthorized());
        for (Consumer<AccessSession> listener : listeners)
        {
            listener.accept(next);
        }
    }
}
