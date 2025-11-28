package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.capability.NocoinCapabilityProvider;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet pour demander le solde NOCOIN au serveur.
 */
public class RequestBalancePacket {

    public RequestBalancePacket() {}

    /**
     * Encode le paquet (vide, juste une requête).
     */
    public void encode(FriendlyByteBuf buf) {
        // Pas de données à envoyer
    }

    /**
     * Décode le paquet.
     */
    public static RequestBalancePacket decode(FriendlyByteBuf buf) {
        return new RequestBalancePacket();
    }

    /**
     * Traite la requête côté serveur et renvoie le solde.
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx
            .get()
            .enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    player
                        .getCapability(
                            NocoinCapabilityProvider.NOCOIN_CAPABILITY
                        )
                        .ifPresent(cap -> {
                            NocoinNetworkHandler.sendBalanceToClient(
                                player,
                                cap.getBalance()
                            );
                        });
                }
            });
        ctx.get().setPacketHandled(true);
    }
}
