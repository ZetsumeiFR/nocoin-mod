package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.client.ClientNocoinData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet pour synchroniser le solde NOCOIN du serveur vers le client.
 */
public class SyncBalancePacket {

    private final long balance;

    public SyncBalancePacket(long balance) {
        this.balance = balance;
    }

    /**
     * Encode le paquet dans le buffer.
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(balance);
    }

    /**
     * Décode le paquet depuis le buffer.
     */
    public static SyncBalancePacket decode(FriendlyByteBuf buf) {
        return new SyncBalancePacket(buf.readLong());
    }

    /**
     * Traite le paquet côté client.
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx
            .get()
            .enqueueWork(() -> {
                // Exécuter uniquement côté client
                DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                        () -> {
                            ClientNocoinData.setBalance(this.balance);
                        }
                );
            });
        ctx.get().setPacketHandled(true);
    }
}
