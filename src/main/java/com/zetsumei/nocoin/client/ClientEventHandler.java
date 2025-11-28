package com.zetsumei.nocoin.client;

import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.client.screen.NocoinScreen;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gestionnaire d'événements côté client pour NOCOIN.
 */
public class ClientEventHandler {
    
    /**
     * Événements sur le bus FORGE (runtime).
     */
    @Mod.EventBusSubscriber(modid = Nocoin.MODID, value = Dist.CLIENT)
    public static class ForgeEvents {
        
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            
            // Vérifie si le joueur est en jeu et si la touche est pressée
            if (mc.player != null && mc.screen == null) {
                if (KeyBindings.OPEN_NOCOIN_MENU.consumeClick()) {
                    // Demande le solde au serveur avant d'ouvrir le menu
                    NocoinNetworkHandler.requestBalanceFromServer();
                    // Ouvre l'écran NOCOIN
                    mc.setScreen(new NocoinScreen());
                }
            }
        }
    }
    
    /**
     * Événements sur le bus MOD (initialisation).
     */
    @Mod.EventBusSubscriber(modid = Nocoin.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.OPEN_NOCOIN_MENU);
        }
    }
}
