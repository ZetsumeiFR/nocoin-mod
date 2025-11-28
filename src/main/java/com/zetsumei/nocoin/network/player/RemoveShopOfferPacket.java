package com.zetsumei.nocoin.network.player;

import com.zetsumei.nocoin.block.entity.PlayerShopBlockEntity;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet envoyé au serveur pour supprimer une offre du magasin.
 */
public class RemoveShopOfferPacket {

    private final BlockPos shopPos;
    private final UUID offerId;

    public RemoveShopOfferPacket(BlockPos shopPos, UUID offerId) {
        this.shopPos = shopPos;
        this.offerId = offerId;
    }

    public static void encode(
        RemoveShopOfferPacket packet,
        FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(packet.shopPos);
        buf.writeUUID(packet.offerId);
    }

    public static RemoveShopOfferPacket decode(FriendlyByteBuf buf) {
        BlockPos shopPos = buf.readBlockPos();
        UUID offerId = buf.readUUID();
        return new RemoveShopOfferPacket(shopPos, offerId);
    }

    public static void handle(
        RemoveShopOfferPacket packet,
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

                // Supprimer l'offre
                shopEntity.removeOffer(packet.offerId);

                // Rafraîchir l'écran du propriétaire
                NocoinNetworkHandler.sendOpenPlayerShopOwnerScreen(
                    player,
                    packet.shopPos,
                    shopEntity
                );
            });
        ctx.get().setPacketHandled(true);
    }
}
