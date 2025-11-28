package com.zetsumei.nocoin.entity;

import com.zetsumei.nocoin.Nocoin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registre des entités du mod NOCOIN.
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Nocoin.MODID);

    /**
     * Vendeur Gacha - PNJ qui vend des Clés Gacha contre des NOCOIN.
     */
    public static final RegistryObject<EntityType<GachaVendorEntity>> GACHA_VENDOR =
            ENTITIES.register("gacha_vendor",
                    () -> EntityType.Builder.<GachaVendorEntity>of(GachaVendorEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build("gacha_vendor"));

    /**
     * Enregistre les entités sur le bus d'événements du mod.
     */
    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
