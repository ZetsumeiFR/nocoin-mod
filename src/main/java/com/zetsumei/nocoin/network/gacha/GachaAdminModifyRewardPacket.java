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
 * Packet client → serveur pour modifier une récompense du catalogue d'une machine gacha (admin).
 */
public class GachaAdminModifyRewardPacket {

    public enum ModifyType {
        WEIGHT,
        RARITY
    }

    private final BlockPos machinePos;
    private final String itemId;
    private final ModifyType modifyType;
    private final double newWeight;
    private final GachaRarity newRarity;

    public GachaAdminModifyRewardPacket(BlockPos machinePos, String itemId, double newWeight) {
        this.machinePos = machinePos;
        this.itemId = itemId;
        this.modifyType = ModifyType.WEIGHT;
        this.newWeight = newWeight;
        this.newRarity = GachaRarity.THREE_STAR; // Non utilisé
    }

    public GachaAdminModifyRewardPacket(BlockPos machinePos, String itemId, GachaRarity newRarity) {
        this.machinePos = machinePos;
        this.itemId = itemId;
        this.modifyType = ModifyType.RARITY;
        this.newWeight = 1.0; // Non utilisé
        this.newRarity = newRarity;
    }

    private GachaAdminModifyRewardPacket(BlockPos machinePos, String itemId, ModifyType modifyType, double newWeight, GachaRarity newRarity) {
        this.machinePos = machinePos;
        this.itemId = itemId;
        this.modifyType = modifyType;
        this.newWeight = newWeight;
        this.newRarity = newRarity;
    }

    public static void encode(GachaAdminModifyRewardPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.machinePos);
        buf.writeUtf(packet.itemId);
        buf.writeEnum(packet.modifyType);
        buf.writeDouble(packet.newWeight);
        buf.writeEnum(packet.newRarity);
    }

    public static GachaAdminModifyRewardPacket decode(FriendlyByteBuf buf) {
        return new GachaAdminModifyRewardPacket(
            buf.readBlockPos(),
            buf.readUtf(),
            buf.readEnum(ModifyType.class),
            buf.readDouble(),
            buf.readEnum(GachaRarity.class)
        );
    }

    public static void handle(GachaAdminModifyRewardPacket packet, Supplier<NetworkEvent.Context> ctx) {
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

            boolean success = false;
            String message = "";

            if (packet.modifyType == ModifyType.WEIGHT) {
                success = gachaBE.setRewardWeight(packet.itemId, packet.newWeight);
                message = success ? "§aPoids modifié: " + packet.newWeight : "§cErreur: récompense non trouvée";
            } else if (packet.modifyType == ModifyType.RARITY) {
                success = gachaBE.setRewardRarity(packet.itemId, packet.newRarity);
                message = success ? "§aRareté modifiée: " + packet.newRarity.getDisplayStars() : "§cErreur: récompense non trouvée";
            }

            player.sendSystemMessage(Component.literal(message));

            if (success) {
                // Rafraîchir l'écran admin
                NocoinNetworkHandler.sendOpenGachaAdminScreen(player, packet.machinePos);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
