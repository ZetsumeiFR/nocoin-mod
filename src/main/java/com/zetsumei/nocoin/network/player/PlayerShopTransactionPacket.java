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
 * Paquet envoyé au serveur pour effectuer une transaction dans un magasin joueur.
 */
public class PlayerShopTransactionPacket {

    public enum TransactionType {
        BUY, // Client achète une offre de vente
        SELL, // Client vend via une offre d'achat
    }

    private final BlockPos shopPos;
    private final UUID offerId;
    private final TransactionType type;

    public PlayerShopTransactionPacket(
        BlockPos shopPos,
        UUID offerId,
        TransactionType type
    ) {
        this.shopPos = shopPos;
        this.offerId = offerId;
        this.type = type;
    }

    public static void encode(
        PlayerShopTransactionPacket packet,
        FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(packet.shopPos);
        buf.writeUUID(packet.offerId);
        buf.writeEnum(packet.type);
    }

    public static PlayerShopTransactionPacket decode(FriendlyByteBuf buf) {
        BlockPos shopPos = buf.readBlockPos();
        UUID offerId = buf.readUUID();
        TransactionType type = buf.readEnum(TransactionType.class);
        return new PlayerShopTransactionPacket(shopPos, offerId, type);
    }

    public static void handle(
        PlayerShopTransactionPacket packet,
        Supplier<NetworkEvent.Context> ctx
    ) {
        ctx
            .get()
            .enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                BlockEntity be = player.level().getBlockEntity(packet.shopPos);
                if (!(be instanceof PlayerShopBlockEntity shopEntity)) return;

                // Vérifier que le joueur n'est pas le propriétaire
                if (shopEntity.isOwner(player)) {
                    NocoinNetworkHandler.sendPlayerShopTransactionResult(
                        player,
                        false,
                        PlayerShopBlockEntity.TransactionResult.Status.ERROR,
                        0
                    );
                    return;
                }

                PlayerShopBlockEntity.TransactionResult result;
                if (packet.type == TransactionType.BUY) {
                    result = shopEntity.executeBuyFromShop(
                        player,
                        packet.offerId
                    );
                } else {
                    result = shopEntity.executeSellToShop(
                        player,
                        packet.offerId
                    );
                }

                NocoinNetworkHandler.sendPlayerShopTransactionResult(
                    player,
                    result.isSuccess(),
                    result.getStatus(),
                    result.getAmountTransferred()
                );
            });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getShopPos() {
        return shopPos;
    }

    public UUID getOfferId() {
        return offerId;
    }

    public TransactionType getType() {
        return type;
    }
}
