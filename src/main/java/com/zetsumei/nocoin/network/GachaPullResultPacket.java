package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.client.ClientGachaMachineHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet serveur → client pour envoyer le résultat d'un tirage Gacha.
 */
public class GachaPullResultPacket {

    private final boolean success;
    private final String itemId;
    private final int stars;
    private final String characterName;

    public GachaPullResultPacket(
        boolean success,
        String itemId,
        int stars,
        String characterName
    ) {
        this.success = success;
        this.itemId = itemId != null ? itemId : "";
        this.stars = stars;
        this.characterName = characterName != null ? characterName : "";
    }

    public static void encode(
        GachaPullResultPacket packet,
        FriendlyByteBuf buffer
    ) {
        buffer.writeBoolean(packet.success);
        buffer.writeUtf(packet.itemId);
        buffer.writeInt(packet.stars);
        buffer.writeUtf(packet.characterName);
    }

    public static GachaPullResultPacket decode(FriendlyByteBuf buffer) {
        boolean success = buffer.readBoolean();
        String itemId = buffer.readUtf();
        int stars = buffer.readInt();
        String characterName = buffer.readUtf();
        return new GachaPullResultPacket(success, itemId, stars, characterName);
    }

    public static void handle(
        GachaPullResultPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () ->
                    () ->
                        ClientGachaMachineHandler.handlePullResult(
                            packet.success,
                            packet.itemId,
                            packet.stars,
                            packet.characterName
                        )
            );
        });
        context.setPacketHandled(true);
    }
}
