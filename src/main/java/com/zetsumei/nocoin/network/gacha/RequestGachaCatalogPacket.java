package com.zetsumei.nocoin.network.gacha;

import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet client → serveur pour demander le catalogue des récompenses gacha.
 * Inclut la position de la machine pour identifier le catalogue spécifique.
 */
public class RequestGachaCatalogPacket {

    private final BlockPos machinePos;

    public RequestGachaCatalogPacket(BlockPos machinePos) {
        this.machinePos = machinePos;
    }

    public static void encode(RequestGachaCatalogPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.machinePos);
    }

    public static RequestGachaCatalogPacket decode(FriendlyByteBuf buf) {
        return new RequestGachaCatalogPacket(buf.readBlockPos());
    }

    public static void handle(RequestGachaCatalogPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                NocoinNetworkHandler.sendGachaCatalogToClient(player, packet.machinePos);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
