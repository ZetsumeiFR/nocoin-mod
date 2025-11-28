package com.zetsumei.nocoin.network.gacha;

import com.zetsumei.nocoin.block.entity.GachaMachineBlockEntity;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet client → serveur pour modifier les probabilités de rareté d'une machine gacha (admin).
 */
public class GachaAdminSetRatesPacket {

    private final BlockPos machinePos;
    private final double fiveStarRate;
    private final double fourStarRate;
    private final double threeStarRate;

    public GachaAdminSetRatesPacket(BlockPos machinePos, double fiveStarRate, double fourStarRate, double threeStarRate) {
        this.machinePos = machinePos;
        this.fiveStarRate = fiveStarRate;
        this.fourStarRate = fourStarRate;
        this.threeStarRate = threeStarRate;
    }

    public static void encode(GachaAdminSetRatesPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.machinePos);
        buf.writeDouble(packet.fiveStarRate);
        buf.writeDouble(packet.fourStarRate);
        buf.writeDouble(packet.threeStarRate);
    }

    public static GachaAdminSetRatesPacket decode(FriendlyByteBuf buf) {
        return new GachaAdminSetRatesPacket(
            buf.readBlockPos(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
    }

    public static void handle(GachaAdminSetRatesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }

            // Récupérer le BlockEntity de la machine
            Level level = player.level();
            BlockEntity be = level.getBlockEntity(packet.machinePos);
            if (!(be instanceof GachaMachineBlockEntity gachaBE)) {
                player.sendSystemMessage(Component.literal("§cErreur: machine gacha non trouvée"));
                return;
            }

            // Vérifier que le total fait 100%
            double total = packet.fiveStarRate + packet.fourStarRate + packet.threeStarRate;
            if (Math.abs(total - 100.0) > 0.01) {
                player.sendSystemMessage(Component.literal("§cErreur: le total doit être 100% (actuel: " + String.format("%.1f", total) + "%)"));
                return;
            }

            gachaBE.setRarityRates(
                packet.fiveStarRate,
                packet.fourStarRate,
                packet.threeStarRate
            );

            player.sendSystemMessage(Component.literal(String.format(
                "§aProbabilités mises à jour: 5★=%.1f%%, 4★=%.1f%%, 3★=%.1f%%",
                packet.fiveStarRate, packet.fourStarRate, packet.threeStarRate
            )));

            // Rafraîchir l'écran admin
            NocoinNetworkHandler.sendOpenGachaAdminScreen(player, packet.machinePos);
        });
        ctx.get().setPacketHandled(true);
    }
}
