package com.zetsumei.nocoin.client;

import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.block.entity.ModBlockEntities;
import com.zetsumei.nocoin.client.renderer.GachaVendorRenderer;
import com.zetsumei.nocoin.client.renderer.LeaderboardBlockEntityRenderer;
import com.zetsumei.nocoin.entity.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Événements côté client pour le mod NOCOIN.
 */
@Mod.EventBusSubscriber(modid = Nocoin.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Entités
        event.registerEntityRenderer(ModEntities.GACHA_VENDOR.get(), GachaVendorRenderer::new);
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // BlockEntities
        event.registerBlockEntityRenderer(ModBlockEntities.LEADERBOARD.get(), LeaderboardBlockEntityRenderer::new);
    }
}
