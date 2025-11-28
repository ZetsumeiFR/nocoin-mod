package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.client.ClientVendorHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet envoyé du serveur vers le client pour ouvrir l'écran du vendeur Gacha.
 */
public class OpenVendorScreenPacket {

    private final long currentBalance;
    private final long keyPrice;

    public OpenVendorScreenPacket(long currentBalance, long keyPrice) {
        this.currentBalance = currentBalance;
        this.keyPrice = keyPrice;
    }

    public static void encode(
        OpenVendorScreenPacket packet,
        FriendlyByteBuf buf
    ) {
        buf.writeLong(packet.currentBalance);
        buf.writeLong(packet.keyPrice);
    }

    public static OpenVendorScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenVendorScreenPacket(buf.readLong(), buf.readLong());
    }

    public static void handle(
        OpenVendorScreenPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Exécuter côté client uniquement
            DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () ->
                    () ->
                        ClientVendorHandler.openVendorScreen(
                            packet.currentBalance,
                            packet.keyPrice
                        )
            );
        });
        context.setPacketHandled(true);
    }
}
