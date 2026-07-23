package com.liveon;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(LiveOnConfig.GROUP)
public interface LiveOnConfig extends Config
{
    String GROUP = "live-on-clan";
    String THIRD_PARTY_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

    @ConfigSection(
        name = "Conexão",
        description = "Acesso ao serviço da Live On",
        position = 0
    )
    String connectionSection = "connection";

    @ConfigSection(
        name = "Notificações",
        description = "Eventos enviados ao clan",
        position = 1
    )
    String notificationSection = "notifications";

    @ConfigItem(
        keyName = "syncEnabled",
        name = "Ativar integração Live On",
        description = "Autoriza o plugin a consultar e enviar dados ao servidor do clã.",
        section = connectionSection,
        position = 0,
        warning = THIRD_PARTY_WARNING
    )
    default boolean syncEnabled()
    {
        return false;
    }

    @ConfigItem(
        keyName = "apiBaseUrl",
        name = "Endereço da API",
        description = "Servidor oficial da Live On. Altere apenas em desenvolvimento.",
        section = connectionSection,
        position = 1
    )
    default String apiBaseUrl()
    {
        return "https://api.liveonclan.com";
    }

    @ConfigItem(
        keyName = "accessCode",
        name = "Código de acesso",
        description = "Código distribuído pela staff. O servidor também valida o seu RSN.",
        section = connectionSection,
        position = 2
    )
    default String accessCode()
    {
        return "";
    }

    @ConfigItem(
        keyName = "minimumDropValue",
        name = "Valor mínimo do drop",
        description = "Valor total mínimo para registrar um drop.",
        section = notificationSection,
        position = 0
    )
    @Range(min = 0)
    default int minimumDropValue()
    {
        return 1_000_000;
    }

    @ConfigItem(
        keyName = "sendPets",
        name = "Registrar pets",
        description = "Envia conquistas de pets ao feed da Live On.",
        section = notificationSection,
        position = 1
    )
    default boolean sendPets()
    {
        return true;
    }

    @ConfigItem(
        keyName = "sendCollectionLog",
        name = "Collection log",
        description = "Registra novas entradas do collection log.",
        section = notificationSection,
        position = 2
    )
    default boolean sendCollectionLog()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showLoginMessage",
        name = "Mensagem ao entrar",
        description = "Mostra a mensagem de login definida pela staff.",
        section = notificationSection,
        position = 3
    )
    default boolean showLoginMessage()
    {
        return true;
    }

    @ConfigItem(
        keyName = "announcementPollMinutes",
        name = "Atualizar anúncios",
        description = "Intervalo entre consultas de novos anúncios.",
        section = notificationSection,
        position = 4
    )
    @Units(Units.MINUTES)
    @Range(min = 1, max = 30)
    default int announcementPollMinutes()
    {
        return 3;
    }
}
