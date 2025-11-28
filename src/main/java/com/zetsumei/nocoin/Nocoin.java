package com.zetsumei.nocoin;

import com.mojang.logging.LogUtils;
import com.zetsumei.nocoin.block.ModBlocks;
import com.zetsumei.nocoin.block.entity.ModBlockEntities;
import com.zetsumei.nocoin.entity.ModEntities;
import com.zetsumei.nocoin.item.ModItems;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Mod principal NOCOIN - Système de monnaie virtuelle pour Minecraft.
 * Les joueurs gagnent des NOCOIN en tuant des mobs.
 */
@Mod(Nocoin.MODID)
public class Nocoin {

    public static final String MODID = "nocoin";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Nocoin(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Enregistrer les items, blocs, entités et block entities
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("NOCOIN mod initialized!");

        // Enregistrement du réseau
        event.enqueueWork(() -> {
            NocoinNetworkHandler.register();
            LOGGER.info("NOCOIN network registered!");
        });
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("NOCOIN: Server starting - currency system active");
    }
}
