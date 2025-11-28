package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.shop.ShopManager;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet pour effectuer un achat dans la boutique.
 * Envoyé du client vers le serveur.
 */
public class PurchasePacket {

    private final int shopItemId;

    public PurchasePacket(int shopItemId) {
        this.shopItemId = shopItemId;
    }

    /**
     * Encode le paquet dans le buffer.
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(shopItemId);
    }

    /**
     * Décode le paquet depuis le buffer.
     */
    public static PurchasePacket decode(FriendlyByteBuf buf) {
        return new PurchasePacket(buf.readVarInt());
    }

    /**
     * Traite la requête d'achat côté serveur.
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx
            .get()
            .enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    ShopManager.PurchaseResult result =
                        ShopManager.getInstance().processPurchase(
                            player,
                            shopItemId
                        );

                    // Envoyer le message de résultat au joueur
                    if (result.isSuccess()) {
                        player.sendSystemMessage(
                            result
                                .getMessageComponent()
                                .copy()
                                .withStyle(ChatFormatting.GREEN)
                        );
                        // Envoyer la réponse de succès au client pour mettre à jour l'UI
                        NocoinNetworkHandler.sendPurchaseResultToClient(
                            player,
                            true,
                            result.getNewBalance()
                        );
                    } else {
                        player.sendSystemMessage(
                            result
                                .getMessageComponent()
                                .copy()
                                .withStyle(ChatFormatting.RED)
                        );
                        NocoinNetworkHandler.sendPurchaseResultToClient(
                            player,
                            false,
                            -1
                        );
                    }
                }
            });
        ctx.get().setPacketHandled(true);
    }
}
