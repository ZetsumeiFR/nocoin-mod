package com.zetsumei.nocoin.block;

import com.zetsumei.nocoin.Nocoin;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Registre des blocs du mod NOCOIN.
 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Nocoin.MODID);

    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Nocoin.MODID);

    /**
     * Machine à Gacha - Utilisée pour effectuer des tirages avec les Clés Gacha.
     */
    public static final RegistryObject<Block> GACHA_MACHINE = registerBlock("gacha_machine",
            () -> new GachaMachineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> 7)));

    /**
     * Leaderboard - Panneau d'affichage du classement des joueurs.
     * Affiche le top 10 en 3D avec un effet holographique.
     * Parfait pour être placé au spawn!
     */
    public static final RegistryObject<Block> LEADERBOARD = registerBlock("leaderboard",
            () -> new LeaderboardBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> 10)));

    // ==================== BLOCS DÉCORATIFS - PELUCHES ====================

    /**
     * Peluche Hatsune Miku - Bloc décoratif Vocaloid.
     */
    public static final RegistryObject<Block> MIKU_PLUSH = registerBlock("mikuplush",
            () -> new PlushBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.5F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    /**
     * Peluche Kagamine Len - Bloc décoratif Vocaloid.
     */
    public static final RegistryObject<Block> KAGAMINE_LEN_PLUSH = registerBlock("kagaminelen",
            () -> new PlushBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.5F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    /**
     * Peluche Kagamine Rin - Bloc décoratif Vocaloid.
     */
    public static final RegistryObject<Block> KAGAMINE_RIN_PLUSH = registerBlock("kagaminerin",
            () -> new PlushBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.5F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    /**
     * Peluche Akita Neru - Bloc décoratif Vocaloid.
     */
    public static final RegistryObject<Block> NERU_PLUSH = registerBlock("neruplush",
            () -> new PlushBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.5F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    /**
     * Peluche Skibidi Toilet - Bloc décoratif meme.
     */
    public static final RegistryObject<Block> SKIBIDI_PLUSH = registerBlock("skibidiplush",
            () -> new PlushBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.5F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    /**
     * Enregistre un bloc avec son item associé.
     */
    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registeredBlock = BLOCKS.register(name, block);
        registerBlockItem(name, registeredBlock);
        return registeredBlock;
    }

    /**
     * Enregistre l'item associé à un bloc.
     */
    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    /**
     * Enregistre les blocs sur le bus d'événements du mod.
     */
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ITEMS.register(eventBus);
    }
}
