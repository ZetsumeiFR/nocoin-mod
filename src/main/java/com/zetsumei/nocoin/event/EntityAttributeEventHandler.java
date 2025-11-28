package com.zetsumei.nocoin.event;

import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.entity.GachaVendorEntity;
import com.zetsumei.nocoin.entity.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gestionnaire d'événements pour l'enregistrement des attributs d'entités.
 */
@Mod.EventBusSubscriber(modid = Nocoin.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityAttributeEventHandler {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GACHA_VENDOR.get(), GachaVendorEntity.createAttributes().build());
    }
}
