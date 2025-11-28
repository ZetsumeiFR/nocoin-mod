package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.client.ClientGachaMachineHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet serveur → client pour ouvrir l'écran de la machine à Gacha.
 */
public class OpenGachaMachinePacket {

    private final boolean hasKey;
    private final int keyCount;

    public OpenGachaMachinePacket(boolean hasKey, int keyCount) {
        this.hasKey = hasKey;
        this.keyCount = keyCount;
    }

    public static void encode(
        OpenGachaMachinePacket packet,
        FriendlyByteBuf buffer
    ) {
        buffer.writeBoolean(packet.hasKey);
        buffer.writeInt(packet.keyCount);
    }

    public static OpenGachaMachinePacket decode(FriendlyByteBuf buffer) {
        boolean hasKey = buffer.readBoolean();
        int keyCount = buffer.readInt();
        return new OpenGachaMachinePacket(hasKey, keyCount);
    }

    public static void handle(
        OpenGachaMachinePacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Exécuter côté client
            DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () ->
                    () ->
                        ClientGachaMachineHandler.openGachaMachineScreen(
                            packet.hasKey,
                            packet.keyCount
                        )
            );
        });
        context.setPacketHandled(true);
    }
}
