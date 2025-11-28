package com.zetsumei.nocoin.network.gacha;

import com.zetsumei.nocoin.block.entity.GachaMachineBlockEntity;
import com.zetsumei.nocoin.gacha.GachaRarity;
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
 * Packet client → serveur pour ajouter une récompense au catalogue d'une machine gacha (admin).
 */
public class GachaAdminAddRewardPacket {

    private final BlockPos machinePos;
    private final String itemId;
    private final GachaRarity rarity;
    private final String displayName;
    private final double weight;

    public GachaAdminAddRewardPacket(BlockPos machinePos, String itemId, GachaRarity rarity, String displayName, double weight) {
        this.machinePos = machinePos;
        this.itemId = itemId;
        this.rarity = rarity;
        this.displayName = displayName;
        this.weight = weight;
    }

    public static void encode(GachaAdminAddRewardPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.machinePos);
        buf.writeUtf(packet.itemId);
        buf.writeEnum(packet.rarity);
        buf.writeUtf(packet.displayName);
        buf.writeDouble(packet.weight);
    }

    public static GachaAdminAddRewardPacket decode(FriendlyByteBuf buf) {
        return new GachaAdminAddRewardPacket(
            buf.readBlockPos(),
            buf.readUtf(),
            buf.readEnum(GachaRarity.class),
            buf.readUtf(),
            buf.readDouble()
        );
    }

    public static void handle(GachaAdminAddRewardPacket packet, Supplier<NetworkEvent.Context> ctx) {
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

            boolean success = gachaBE.addReward(
                packet.itemId,
                packet.rarity,
                packet.displayName,
                packet.weight
            );

            if (success) {
                player.sendSystemMessage(Component.literal("§aRécompense ajoutée: " + packet.displayName));
                // Rafraîchir l'écran admin
                NocoinNetworkHandler.sendOpenGachaAdminScreen(player, packet.machinePos);
            } else {
                player.sendSystemMessage(Component.literal("§cErreur: item invalide ou déjà existant"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
