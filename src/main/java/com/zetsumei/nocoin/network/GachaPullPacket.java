package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.gacha.GachaManager;
import com.zetsumei.nocoin.item.ModItems;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet client → serveur pour demander un tirage Gacha.
 */
public class GachaPullPacket {

    public GachaPullPacket() {}

    public static void encode(GachaPullPacket packet, FriendlyByteBuf buffer) {
        // Rien à encoder
    }

    public static GachaPullPacket decode(FriendlyByteBuf buffer) {
        return new GachaPullPacket();
    }

    public static void handle(
        GachaPullPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

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

            // Effectuer le tirage
            GachaManager.GachaPullResult result =
                GachaManager.getInstance().pullAndGive(player);

            // Compter les clés restantes
            int remainingKeys = countGachaKeys(player);

            // Envoyer le résultat au client
            NocoinNetworkHandler.sendGachaPullResult(
                player,
                true,
                result.reward().getItem().getDescriptionId(),
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
