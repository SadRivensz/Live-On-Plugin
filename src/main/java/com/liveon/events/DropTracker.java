package com.liveon.events;

import com.liveon.LiveOnConfig;
import com.liveon.api.ApiCallback;
import com.liveon.api.ApiModels;
import com.liveon.api.LiveOnApiClient;
import com.liveon.auth.ClanAccessManager;
import com.liveon.assets.AssetCatalog;
import java.util.Collection;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.DrawManager;
import net.runelite.http.api.loottracker.LootRecordType;

@Slf4j
@Singleton
public class DropTracker
{
    private final LiveOnConfig config;
    private final ClanAccessManager accessManager;
    private final LiveOnApiClient apiClient;
    private final ItemManager itemManager;
    private final AssetCatalog assetCatalog;
    private final DrawManager drawManager;

    @Inject
    public DropTracker(
        LiveOnConfig config,
        ClanAccessManager accessManager,
        LiveOnApiClient apiClient,
        ItemManager itemManager,
        AssetCatalog assetCatalog,
        DrawManager drawManager)
    {
        this.config = config;
        this.accessManager = accessManager;
        this.apiClient = apiClient;
        this.itemManager = itemManager;
        this.assetCatalog = assetCatalog;
        this.drawManager = drawManager;
    }

    public void onNpcLoot(NpcLootReceived event)
    {
        NPC npc = event.getNpc();
        submit(event.getItems(), npc.getName(), "npc", npc.getId());
    }

    public void onOtherLoot(LootReceived event)
    {
        if (event.getType() != LootRecordType.EVENT && event.getType() != LootRecordType.PICKPOCKET)
        {
            return;
        }
        submit(event.getItems(), event.getName(), event.getType().name().toLowerCase(), null);
    }

    private void submit(Collection<ItemStack> itemStacks, String source, String sourceType, Integer npcId)
    {
        if (!config.syncEnabled() || !accessManager.isAuthorized() || itemStacks == null || itemStacks.isEmpty())
        {
            return;
        }

        ApiModels.DropPayload payload = new ApiModels.DropPayload();
        payload.source = source == null ? "Unknown" : source;
        payload.sourceType = sourceType;
        payload.npcId = npcId;

        boolean alwaysNotify = false;
        for (ItemStack stack : itemStacks)
        {
            int unitPrice = Math.max(0, itemManager.getItemPrice(stack.getId()));
            String itemName = itemManager.getItemComposition(stack.getId()).getName();
            AssetCatalog.ItemRule rule = assetCatalog.getItemRule(stack.getId());
            if (rule != null)
            {
                if (rule.alias != null && !rule.alias.trim().isEmpty())
                {
                    itemName = rule.alias.trim();
                }
                alwaysNotify |= rule.alwaysNotify;
            }
            ApiModels.ItemPayload item = new ApiModels.ItemPayload(stack.getId(), itemName, stack.getQuantity(), unitPrice);
            payload.items.add(item);
            payload.totalValue += item.totalPrice;
        }

        if (!alwaysNotify && payload.totalValue < config.minimumDropValue())
        {
            return;
        }

        drawManager.requestNextFrameListener(image -> CompletableFuture.runAsync(() ->
        {
            payload.screenshotBase64 = encodeScreenshot(image);
            apiClient.submitDrop(payload, quietCallback("drop"));
        }));
    }

    private String encodeScreenshot(Image image)
    {
        try
        {
            int originalWidth = image.getWidth(null);
            int originalHeight = image.getHeight(null);
            if (originalWidth <= 0 || originalHeight <= 0)
            {
                return null;
            }
            int width = Math.min(1600, originalWidth);
            int height = Math.max(1, originalHeight * width / originalWidth);
            BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = buffered.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(image, 0, 0, width, height, null);
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(buffered, "jpg", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        }
        catch (Exception exception)
        {
            log.debug("Could not capture Live On drop screenshot", exception);
            return null;
        }
    }

    private ApiCallback<ApiModels.StatusResponse> quietCallback(String eventName)
    {
        return new ApiCallback<ApiModels.StatusResponse>()
        {
            @Override
            public void onSuccess(ApiModels.StatusResponse value)
            {
                log.debug("Live On {} submitted", eventName);
            }

            @Override
            public void onFailure(String message)
            {
                log.debug("Live On {} submission failed: {}", eventName, message);
            }
        };
    }
}
