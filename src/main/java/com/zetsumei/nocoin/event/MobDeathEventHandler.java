package com.zetsumei.nocoin.event;

import com.mojang.logging.LogUtils;
import com.zetsumei.nocoin.Config;
import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.capability.NocoinCapabilityProvider;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Random;

/**
 * Gestionnaire d'événements pour la mort des mobs.
 * Attribue des NOCOIN au joueur qui tue un mob.
 */
@Mod.EventBusSubscriber(modid = Nocoin.MODID)
public class MobDeathEventHandler {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    
    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        Entity killer = event.getSource().getEntity();
        
        // Vérifie que le tueur est un joueur serveur
        if (!(killer instanceof ServerPlayer player)) {
            return;
        }
        
        // Ne compte pas les joueurs tués
        if (deadEntity instanceof Player) {
            return;
        }
        
        // Calcule le montant de NOCOIN à donner
        long nocoinAmount = calculateNocoinDrop(deadEntity);
        
        if (nocoinAmount <= 0) {
            return;
        }
        
        // Ajoute les NOCOIN au joueur
        player.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
            cap.addBalance(nocoinAmount);
            
            // Notifie le joueur
            player.displayClientMessage(
                Component.literal("+" + nocoinAmount + " NOCOIN")
                    .withStyle(ChatFormatting.GOLD),
                true // ActionBar
            );
            
            // Synchronise le solde avec le client
            NocoinNetworkHandler.sendBalanceToClient(player, cap.getBalance());
            
            LOGGER.debug("Player {} earned {} NOCOIN for killing {}", 
                player.getName().getString(), 
                nocoinAmount,
                ForgeRegistries.ENTITY_TYPES.getKey(deadEntity.getType())
            );
        });
    }
    
    /**
     * Calcule le montant de NOCOIN à drop selon le type de mob.
     * @param entity l'entité tuée
     * @return le montant de NOCOIN à donner
     */
    private static long calculateNocoinDrop(LivingEntity entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        
        // Vérifie si un montant personnalisé est défini pour ce mob
        if (Config.mobDrops.containsKey(entityId)) {
            return Config.mobDrops.get(entityId);
        }
        
        // Valeurs par défaut selon le type d'entité
        if (entity instanceof Monster) {
            // Mobs hostiles: entre min et max défini dans la config
            int min = Config.defaultMonsterDropMin;
            int max = Config.defaultMonsterDropMax;
            return min + RANDOM.nextInt(Math.max(1, max - min + 1));
        }
        
        // Autres mobs passifs: valeur par défaut
        return Config.defaultPassiveDrops;
    }
}
