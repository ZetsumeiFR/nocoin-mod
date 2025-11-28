package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.shop.ShopManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet pour demander la liste des articles de boutique au serveur.
 */
public class RequestShopItemsPacket {

    public RequestShopItemsPacket() {}

    /**
     * Encode le paquet (vide, juste une requête).
     */
    public void encode(FriendlyByteBuf buf) {
        // Pas de données à envoyer
    }

    /**
     * Décode le paquet.
     */
    public static RequestShopItemsPacket decode(FriendlyByteBuf buf) {
        return new RequestShopItemsPacket();
    }

    /**
     * Traite la requête côté serveur et renvoie les articles.
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx
            .get()
            .enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    NocoinNetworkHandler.sendShopItemsToClient(player);
                }
            });
        ctx.get().setPacketHandled(true);
    }
}
