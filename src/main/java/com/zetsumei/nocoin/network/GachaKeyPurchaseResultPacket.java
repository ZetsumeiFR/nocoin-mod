package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.client.ClientVendorHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet envoyé du serveur vers le client pour notifier le résultat d'un achat de Clé Gacha.
 */
public class GachaKeyPurchaseResultPacket {

    private final boolean success;
    private final long newBalance;
    private final int quantity;

    public GachaKeyPurchaseResultPacket(
        boolean success,
        long newBalance,
        int quantity
    ) {
        this.success = success;
        this.newBalance = newBalance;
        this.quantity = quantity;
    }

    public static void encode(
        GachaKeyPurchaseResultPacket packet,
        FriendlyByteBuf buf
    ) {
        buf.writeBoolean(packet.success);
        buf.writeLong(packet.newBalance);
        buf.writeVarInt(packet.quantity);
    }

    public static GachaKeyPurchaseResultPacket decode(FriendlyByteBuf buf) {
        return new GachaKeyPurchaseResultPacket(
            buf.readBoolean(),
            buf.readLong(),
            buf.readVarInt()
        );
    }

    public static void handle(
        GachaKeyPurchaseResultPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () ->
                    () ->
                        ClientVendorHandler.handlePurchaseResult(
                            packet.success,
                            packet.newBalance,
                            packet.quantity
                        )
            );
        });
        context.setPacketHandled(true);
    }
}
