package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.block.entity.GachaMachineBlockEntity;
import com.zetsumei.nocoin.gacha.GachaManager;
import com.zetsumei.nocoin.item.ModItems;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet client → serveur pour demander un tirage Gacha.
 * Inclut la position de la machine pour tirer depuis le bon catalogue.
 */
public class GachaPullPacket {

    private final BlockPos machinePos;

    public GachaPullPacket(BlockPos machinePos) {
        this.machinePos = machinePos;
    }

    public static void encode(GachaPullPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.machinePos);
    }

    public static GachaPullPacket decode(FriendlyByteBuf buffer) {
        return new GachaPullPacket(buffer.readBlockPos());
    }

    public static void handle(
        GachaPullPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Récupérer le BlockEntity de la machine
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(packet.machinePos);
            if (!(be instanceof GachaMachineBlockEntity gachaBE)) {
                NocoinNetworkHandler.sendGachaPullResult(
                    player,
                    false,
                    null,
                    0,
                    "invalid_machine"
                );
                return;
            }

            // Vérifier si le catalogue de la machine n'est pas vide
            if (gachaBE.getRewardCount() == 0) {
                NocoinNetworkHandler.sendGachaPullResult(
                    player,
                    false,
                    null,
                    0,
                    "empty_catalog"
                );
                return;
            }

            // Vérifier si le joueur a une clé Gacha
            int keySlot = findGachaKeySlot(player);
            if (keySlot == -1) {
                // Pas de clé
                NocoinNetworkHandler.sendGachaPullResult(
                    player,
                    false,
                    null,
                    0,
                    "no_key"
                );
                return;
            }

            // Consommer la clé
            ItemStack keyStack = player.getInventory().getItem(keySlot);
            keyStack.shrink(1);

            // Effectuer le tirage depuis le catalogue de cette machine
            GachaManager.GachaPullResult result = gachaBE.pull();
            
            if (result == null) {
                NocoinNetworkHandler.sendGachaPullResult(
                    player,
                    false,
                    null,
                    0,
                    "pull_failed"
                );
                return;
            }

            // Donner l'item au joueur
            ItemStack rewardStack = result.reward().createStack();
            if (!player.getInventory().add(rewardStack)) {
                player.drop(rewardStack, false);
            }

            // Compter les clés restantes
            int remainingKeys = countGachaKeys(player);

            // Envoyer le résultat au client
            NocoinNetworkHandler.sendGachaPullResult(
                player,
                true,
                result.reward().getItemId(),
                result.getRarity().getStars(),
                result.getCharacterName()
            );

            // Message dans le chat selon la rareté
            Component message = switch (result.getRarity()) {
                case FIVE_STAR -> Component.literal(
                    "★★★★★ LÉGENDAIRE ! Vous avez obtenu : " +
                        result.getCharacterName()
                ).withStyle(result.getRarity().getColor());
                case FOUR_STAR -> Component.literal(
                    "★★★★ Épique ! Vous avez obtenu : " +
                        result.getCharacterName()
                ).withStyle(result.getRarity().getColor());
                case THREE_STAR -> Component.literal(
                    "★★★ Vous avez obtenu : " + result.getCharacterName()
                ).withStyle(result.getRarity().getColor());
            };
            player.sendSystemMessage(message);
        });
        context.setPacketHandled(true);
    }

    /**
     * Trouve le slot contenant une Clé Gacha, ou -1 si non trouvé.
     */
    private static int findGachaKeySlot(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.GACHA_KEY.get())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Compte le nombre de Clés Gacha.
     */
    private static int countGachaKeys(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.GACHA_KEY.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
