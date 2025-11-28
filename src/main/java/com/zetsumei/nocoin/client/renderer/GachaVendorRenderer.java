package com.zetsumei.nocoin.client.renderer;

import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.entity.GachaVendorEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer pour le vendeur Gacha.
 * Utilise le modèle du Villager avec une texture personnalisée.
 */
public class GachaVendorRenderer extends MobRenderer<GachaVendorEntity, VillagerModel<GachaVendorEntity>> {

    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Nocoin.MODID, "textures/entity/gacha_vendor.png");

    public GachaVendorRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(GachaVendorEntity entity) {
        return TEXTURE;
    }
}
