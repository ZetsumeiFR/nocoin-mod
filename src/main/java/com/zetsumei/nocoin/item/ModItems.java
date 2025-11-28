package com.zetsumei.nocoin.item;

import com.zetsumei.nocoin.Nocoin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registre des items du mod NOCOIN.
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
        ForgeRegistries.ITEMS,
        Nocoin.MODID
    );

    /**
     * Cle Gacha - permet d'acceder a la machine Gacha.
     */
    public static final RegistryObject<Item> GACHA_KEY = ITEMS.register(
        "gacha_key",
        () ->
            new GachaKeyItem(
                new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)
            )
    );

    /**
     * Enregistre les items sur le bus d'evenements du mod.
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
