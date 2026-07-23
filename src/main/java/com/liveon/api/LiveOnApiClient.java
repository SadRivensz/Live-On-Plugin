package com.liveon.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.liveon.LiveOnConfig;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Singleton
public class LiveOnApiClient
{
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final LiveOnConfig config;
    private volatile String sessionToken;

    @Inject
    public LiveOnApiClient(OkHttpClient httpClient, Gson gson, LiveOnConfig config)
    {
        this.httpClient = httpClient.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(70, TimeUnit.SECONDS)
            .build();
        this.gson = gson;
        this.config = config;
    }

    public void setSessionToken(String token)
    {
        this.sessionToken = token;
    }

    public void clearSessionToken()
    {
        this.sessionToken = null;
    }

    public void verify(ApiModels.AuthRequest body, ApiCallback<ApiModels.AuthResponse> callback)
    {
        post("/v1/auth/verify", body, ApiModels.AuthResponse.class, false, callback);
    }

    public void submitDrop(ApiModels.DropPayload body, ApiCallback<ApiModels.StatusResponse> callback)
    {
        post("/v1/drops", body, ApiModels.StatusResponse.class, true, callback);
    }

    public void submitPet(ApiModels.PetPayload body, ApiCallback<ApiModels.StatusResponse> callback)
    {
        post("/v1/pets", body, ApiModels.StatusResponse.class, true, callback);
    }

    public void submitCollectionLog(ApiModels.CollectionLogPayload body, ApiCallback<ApiModels.StatusResponse> callback)
    {
        post("/v1/collection-log", body, ApiModels.StatusResponse.class, true, callback);
    }

    public void getAnnouncements(ApiCallback<ApiModels.AnnouncementResponse> callback)
    {
        get("/v1/announcements", ApiModels.AnnouncementResponse.class, callback);
    }

    public void publishAnnouncement(ApiModels.PublishAnnouncementRequest body, ApiCallback<ApiModels.Announcement> callback)
    {
        post("/v1/announcements", body, ApiModels.Announcement.class, true, callback);
    }

    public void getRecentDrops(ApiCallback<ApiModels.DropListResponse> callback)
    {
        getRecentDrops(1, 5, callback);
    }

    public void getRecentDrops(int page, int pageSize, ApiCallback<ApiModels.DropListResponse> callback)
    {
        HttpUrl base = baseUrl("/v1/drops");
        if (base == null)
        {
            callback.onFailure("Endereço da API inválido");
            return;
        }
        HttpUrl url = base.newBuilder()
            .addQueryParameter("page", String.valueOf(Math.max(1, page)))
            .addQueryParameter("limit", String.valueOf(Math.max(1, pageSize)))
            .build();
        execute(new Request.Builder().url(url).get(), ApiModels.DropListResponse.class, true, callback);
    }

    public void getItemGoal(ApiCallback<ApiModels.ItemGoalResponse> callback)
    {
        get("/v1/item-goal", ApiModels.ItemGoalResponse.class, callback);
    }

    public void setItemGoal(ApiModels.ItemGoalRequest body, ApiCallback<ApiModels.ItemGoalResponse> callback)
    {
        request("PUT", "/v1/item-goal", body, ApiModels.ItemGoalResponse.class, callback);
    }

    public void clearItemGoal(ApiCallback<ApiModels.StatusResponse> callback)
    {
        request("DELETE", "/v1/item-goal", null, ApiModels.StatusResponse.class, callback);
    }

    public void getImage(String imageUrl, ApiCallback<BufferedImage> callback)
    {
        HttpUrl url = resolveUrl(imageUrl);
        if (url == null)
        {
            callback.onFailure("Imagem inválida");
            return;
        }
        Request.Builder builder = new Request.Builder().url(url).get().header("User-Agent", "Live-On-RuneLite/0.2.6");
        httpClient.newCall(builder.build()).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                callback.onFailure("Não foi possível carregar a screenshot");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                try (Response closeable = response)
                {
                    if (!closeable.isSuccessful() || closeable.body() == null)
                    {
                        callback.onFailure("Screenshot indisponível");
                        return;
                    }
                    BufferedImage image = ImageIO.read(closeable.body().byteStream());
                    if (image == null)
                    {
                        callback.onFailure("Formato de screenshot inválido");
                        return;
                    }
                    callback.onSuccess(image);
                }
            }
        });
    }

    public void searchMembers(String query, ApiCallback<ApiModels.MemberListResponse> callback)
    {
        searchMembers(query, 1, 25, callback);
    }

    public void searchMembers(
        String query,
        int page,
        int limit,
        ApiCallback<ApiModels.MemberListResponse> callback)
    {
        HttpUrl base = baseUrl("/v1/members");
        if (base == null)
        {
            callback.onFailure("Endereço da API inválido");
            return;
        }
        HttpUrl url = base.newBuilder()
            .addQueryParameter("query", query == null ? "" : query)
            .addQueryParameter("page", String.valueOf(page))
            .addQueryParameter("limit", String.valueOf(limit))
            .build();
        execute(new Request.Builder().url(url).get(), ApiModels.MemberListResponse.class, true, callback);
    }

    public void getMember(String rsn, ApiCallback<ApiModels.MemberProfile> callback)
    {
        HttpUrl base = baseUrl("/v1/members/profile");
        if (base == null)
        {
            callback.onFailure("Endereço da API inválido");
            return;
        }
        HttpUrl url = base.newBuilder().addQueryParameter("rsn", rsn).build();
        execute(new Request.Builder().url(url).get(), ApiModels.MemberProfile.class, true, callback);
    }

    public void getRuneProfile(String rsn, ApiCallback<ApiModels.RuneProfileResponse> callback)
    {
        HttpUrl base = baseUrl("/v1/members/runeprofile");
        if (base == null)
        {
            callback.onFailure("Endereço da API inválido");
            return;
        }
        HttpUrl url = base.newBuilder().addQueryParameter("rsn", rsn).build();
        execute(new Request.Builder().url(url).get(), ApiModels.RuneProfileResponse.class, true, callback);
    }

    public void getRankings(String metric, String period, ApiCallback<ApiModels.RankingResponse> callback)
    {
        HttpUrl base = baseUrl("/v1/rankings");
        if (base == null)
        {
            callback.onFailure("Endereço da API inválido");
            return;
        }
        HttpUrl url = base.newBuilder()
            .addQueryParameter("metric", metric)
            .addQueryParameter("period", period)
            .build();
        execute(new Request.Builder().url(url).get(), ApiModels.RankingResponse.class, true, callback);
    }

    private <T> void get(String path, Class<T> responseType, ApiCallback<T> callback)
    {
        HttpUrl url = baseUrl(path);
        if (url == null)
        {
            callback.onFailure("Endereço da API inválido");
            return;
        }
        execute(new Request.Builder().url(url).get(), responseType, true, callback);
    }

    private <T> void post(String path, Object body, Class<T> responseType, boolean authenticated, ApiCallback<T> callback)
    {
        HttpUrl url = baseUrl(path);
        if (url == null)
        {
            callback.onFailure("Endereço da API inválido");
            return;
        }
        RequestBody requestBody = RequestBody.create(JSON, gson.toJson(body));
        execute(new Request.Builder().url(url).post(requestBody), responseType, authenticated, callback);
    }

    private <T> void request(String method, String path, Object body, Class<T> responseType, ApiCallback<T> callback)
    {
        HttpUrl url = baseUrl(path);
        if (url == null)
        {
            callback.onFailure("Endereço da API inválido");
            return;
        }
        Request.Builder builder = new Request.Builder().url(url);
        if ("PUT".equals(method))
        {
            builder.put(RequestBody.create(JSON, gson.toJson(body)));
        }
        else
        {
            builder.delete();
        }
        execute(builder, responseType, true, callback);
    }

    private HttpUrl baseUrl(String path)
    {
        HttpUrl root = HttpUrl.parse(config.apiBaseUrl().trim());
        if (root == null)
        {
            return null;
        }
        boolean localDevelopment = "localhost".equalsIgnoreCase(root.host()) || "127.0.0.1".equals(root.host());
        if (!"https".equalsIgnoreCase(root.scheme()) && !localDevelopment)
        {
            return null;
        }
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        return root.newBuilder().addPathSegments(cleanPath).build();
    }

    private HttpUrl resolveUrl(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return null;
        }
        HttpUrl absolute = HttpUrl.parse(value);
        if (absolute != null && absolute.host() != null)
        {
            return absolute;
        }
        return baseUrl(value);
    }

    private <T> void execute(Request.Builder builder, Class<T> responseType, boolean authenticated, ApiCallback<T> callback)
    {
        if (authenticated)
        {
            String token = sessionToken;
            if (token == null || token.isEmpty())
            {
                callback.onFailure("Sessão Live On não autorizada");
                return;
            }
            builder.header("Authorization", "Bearer " + token);
        }

        builder.header("User-Agent", "Live-On-RuneLite/0.2.6");
        httpClient.newCall(builder.build()).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException exception)
            {
                log.debug("Live On API request failed", exception);
                callback.onFailure("Falha de conexão com a Live On");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                try (Response closeable = response)
                {
                    String responseBody = closeable.body() == null ? "" : closeable.body().string();
                    if (!closeable.isSuccessful())
                    {
                        String detail = null;
                        try
                        {
                            JsonObject error = gson.fromJson(responseBody, JsonObject.class);
                            if (error != null && error.has("detail") && error.get("detail").isJsonPrimitive())
                            {
                                detail = error.get("detail").getAsString();
                            }
                        }
                        catch (RuntimeException ignored)
                        {
                            // Keep the friendly status fallback below.
                        }
                        if (closeable.code() == 401)
                        {
                            callback.onFailure("Sessão expirada. Entre novamente no jogo");
                        }
                        else if (closeable.code() == 502)
                        {
                            callback.onFailure("Serviço de dados temporariamente indisponível");
                        }
                        else
                        {
                            callback.onFailure(detail == null ? "API Live On respondeu " + closeable.code() : detail);
                        }
                        return;
                    }
                    callback.onSuccess(gson.fromJson(responseBody, responseType));
                }
                catch (RuntimeException exception)
                {
                    log.debug("Could not parse Live On API response", exception);
                    callback.onFailure("Resposta inválida da API Live On");
                }
            }
        });
    }
}
