package com.zetsumei.nocoin.network.player;

import com.zetsumei.nocoin.block.entity.PlayerShopBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet envoyé au serveur pour changer le nom du magasin.
 */
public class UpdateShopNamePacket {

    private static final int MAX_NAME_LENGTH = 32;

    private final BlockPos shopPos;
    private final String newName;

    public UpdateShopNamePacket(BlockPos shopPos, String newName) {
        this.shopPos = shopPos;
        this.newName = newName.length() > MAX_NAME_LENGTH
            ? newName.substring(0, MAX_NAME_LENGTH)
            : newName;
    }

    public static void encode(
        UpdateShopNamePacket packet,
        FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(packet.shopPos);
        buf.writeUtf(packet.newName);
    }

    public static UpdateShopNamePacket decode(FriendlyByteBuf buf) {
        BlockPos shopPos = buf.readBlockPos();
        String newName = buf.readUtf(MAX_NAME_LENGTH);
        return new UpdateShopNamePacket(shopPos, newName);
    }

    public static void handle(
        UpdateShopNamePacket packet,
        Supplier<NetworkEvent.Context> ctx
    ) {
        ctx
            .get()
            .enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                BlockEntity be = player.level().getBlockEntity(packet.shopPos);
                if (!(be instanceof PlayerShopBlockEntity shopEntity)) return;

                // Vérifier que le joueur est le propriétaire
                if (!shopEntity.isOwner(player)) return;

                // Mettre à jour le nom
                shopEntity.setShopName(packet.newName);
            });
        ctx.get().setPacketHandled(true);
    }
}
