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
 * Packet client → serveur pour supprimer une récompense du catalogue d'une machine gacha (admin).
 */
public class GachaAdminRemoveRewardPacket {

    private final BlockPos machinePos;
    private final String itemId;

    public GachaAdminRemoveRewardPacket(BlockPos machinePos, String itemId) {
        this.machinePos = machinePos;
        this.itemId = itemId;
    }

    public static void encode(GachaAdminRemoveRewardPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.machinePos);
        buf.writeUtf(packet.itemId);
    }

    public static GachaAdminRemoveRewardPacket decode(FriendlyByteBuf buf) {
        return new GachaAdminRemoveRewardPacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(GachaAdminRemoveRewardPacket packet, Supplier<NetworkEvent.Context> ctx) {
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

            boolean success = gachaBE.removeReward(packet.itemId);

            if (success) {
                player.sendSystemMessage(Component.literal("§aRécompense supprimée: " + packet.itemId));
                // Rafraîchir l'écran admin
                NocoinNetworkHandler.sendOpenGachaAdminScreen(player, packet.machinePos);
            } else {
                player.sendSystemMessage(Component.literal("§cErreur: récompense non trouvée"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
