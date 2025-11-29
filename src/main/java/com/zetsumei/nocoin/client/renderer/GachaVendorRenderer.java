package com.zetsumei.nocoin.client.renderer;

import com.zetsumei.nocoin.entity.GachaVendorEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer pour le vendeur Gacha.
 * Utilise le modèle et la texture du Villager vanilla de Minecraft.
 */
public class GachaVendorRenderer extends MobRenderer<GachaVendorEntity, VillagerModel<GachaVendorEntity>> {

    // Texture du villageois vanilla (plains)
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    public GachaVendorRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(GachaVendorEntity entity) {
        return TEXTURE;
    }
}
