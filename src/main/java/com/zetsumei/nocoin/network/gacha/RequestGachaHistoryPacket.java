package com.zetsumei.nocoin.network.gacha;

import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet client → serveur pour demander l'historique des tirages.
 * Note: L'historique est global par joueur, pas par machine.
 */
public class RequestGachaHistoryPacket {

    private final BlockPos machinePos;

    public RequestGachaHistoryPacket(BlockPos machinePos) {
        this.machinePos = machinePos;
    }

    public static void encode(RequestGachaHistoryPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.machinePos);
    }

    public static RequestGachaHistoryPacket decode(FriendlyByteBuf buf) {
        return new RequestGachaHistoryPacket(buf.readBlockPos());
    }

    public static void handle(RequestGachaHistoryPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // L'historique est global par joueur, pas par machine
                NocoinNetworkHandler.sendGachaHistoryToClient(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
