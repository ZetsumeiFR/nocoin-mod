package com.zetsumei.nocoin.event;

import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.capability.INocoinCapability;
import com.zetsumei.nocoin.capability.NocoinCapabilityProvider;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gestionnaire d'événements pour les capabilities NOCOIN.
 */
@Mod.EventBusSubscriber(modid = Nocoin.MODID)
public class NocoinEventHandler {
    
    /**
     * Attache la capability NOCOIN aux joueurs.
     */
    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).isPresent()) {
                event.addCapability(
                    ResourceLocation.fromNamespaceAndPath(Nocoin.MODID, "nocoin_balance"),
                    new NocoinCapabilityProvider()
                );
            }
        }
    }
    
    /**
     * Persiste les NOCOIN à travers la mort du joueur.
     */
    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // Récupère les données de l'ancien joueur
            event.getOriginal().reviveCaps();
            event.getOriginal().getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(oldCap -> {
                event.getEntity().getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(newCap -> {
                    newCap.copyFrom(oldCap);
                });
            });
            event.getOriginal().invalidateCaps();
        }
    }
    
    /**
     * Synchronise le solde NOCOIN avec le client à la connexion.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
                NocoinNetworkHandler.sendBalanceToClient(player, cap.getBalance());
            });
        }
    }
    
    /**
     * Synchronise le solde après respawn.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
                NocoinNetworkHandler.sendBalanceToClient(player, cap.getBalance());
            });
        }
    }
    
    /**
     * Classe pour l'enregistrement de la capability sur le bus MOD.
     */
    @Mod.EventBusSubscriber(modid = Nocoin.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(INocoinCapability.class);
        }
    }
}
