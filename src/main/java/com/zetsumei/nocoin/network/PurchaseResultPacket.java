package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.client.ClientNocoinData;
import com.zetsumei.nocoin.client.ClientShopData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet pour notifier le client du résultat d'un achat.
 * Envoyé du serveur vers le client.
 */
public class PurchaseResultPacket {

    private final boolean success;
    private final long newBalance;

    public PurchaseResultPacket(boolean success, long newBalance) {
        this.success = success;
        this.newBalance = newBalance;
    }

    /**
     * Encode le paquet dans le buffer.
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeLong(newBalance);
    }

    /**
     * Décode le paquet depuis le buffer.
     */
    public static PurchaseResultPacket decode(FriendlyByteBuf buf) {
        return new PurchaseResultPacket(buf.readBoolean(), buf.readLong());
    }

    /**
     * Traite le résultat côté client.
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx
            .get()
            .enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                        () -> {
                            if (success && newBalance >= 0) {
                                ClientNocoinData.setBalance(newBalance);
                            }
                            ClientShopData.onPurchaseResult(success);
                        }
                );
            });
        ctx.get().setPacketHandled(true);
    }
}
