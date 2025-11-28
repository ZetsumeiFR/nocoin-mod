package com.zetsumei.nocoin.block.entity;

import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registre des BlockEntities du mod NOCOIN.
 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Nocoin.MODID);

    /**
     * BlockEntity pour le magasin joueur.
     */
    public static final RegistryObject<BlockEntityType<PlayerShopBlockEntity>> PLAYER_SHOP =
            BLOCK_ENTITIES.register("player_shop",
                    () -> BlockEntityType.Builder.of(
                            PlayerShopBlockEntity::new,
                            ModBlocks.PLAYER_SHOP.get()
                    ).build(null));

    /**
     * BlockEntity pour le panneau de classement.
     */
    public static final RegistryObject<BlockEntityType<LeaderboardBlockEntity>> LEADERBOARD =
            BLOCK_ENTITIES.register("leaderboard",
                    () -> BlockEntityType.Builder.of(
                            LeaderboardBlockEntity::new,
                            ModBlocks.LEADERBOARD.get()
                    ).build(null));

    /**
     * BlockEntity pour la machine à Gacha.
     * Chaque machine a son propre catalogue de récompenses.
     */
    public static final RegistryObject<BlockEntityType<GachaMachineBlockEntity>> GACHA_MACHINE =
            BLOCK_ENTITIES.register("gacha_machine",
                    () -> BlockEntityType.Builder.of(
                            GachaMachineBlockEntity::new,
                            ModBlocks.GACHA_MACHINE.get()
                    ).build(null));

    /**
     * Enregistre les BlockEntities sur le bus d'événements du mod.
     */
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
