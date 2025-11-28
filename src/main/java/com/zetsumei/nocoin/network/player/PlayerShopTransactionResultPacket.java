package com.zetsumei.nocoin.network.player;

import com.zetsumei.nocoin.block.entity.PlayerShopBlockEntity;
import com.zetsumei.nocoin.client.ClientPlayerShopHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet envoyé au client avec le résultat d'une transaction.
 */
public class PlayerShopTransactionResultPacket {

    private final boolean success;
    private final PlayerShopBlockEntity.TransactionResult.Status status;
    private final long amountTransferred;

    public PlayerShopTransactionResultPacket(
        boolean success,
        PlayerShopBlockEntity.TransactionResult.Status status,
        long amountTransferred
    ) {
        this.success = success;
        this.status = status;
        this.amountTransferred = amountTransferred;
    }

    public static void encode(
        PlayerShopTransactionResultPacket packet,
        FriendlyByteBuf buf
    ) {
        buf.writeBoolean(packet.success);
        buf.writeEnum(packet.status);
        buf.writeLong(packet.amountTransferred);
    }

    public static PlayerShopTransactionResultPacket decode(
        FriendlyByteBuf buf
    ) {
        boolean success = buf.readBoolean();
        PlayerShopBlockEntity.TransactionResult.Status status = buf.readEnum(
            PlayerShopBlockEntity.TransactionResult.Status.class
        );
        long amountTransferred = buf.readLong();
        return new PlayerShopTransactionResultPacket(
            success,
            status,
            amountTransferred
        );
    }

    public static void handle(
        PlayerShopTransactionResultPacket packet,
        Supplier<NetworkEvent.Context> ctx
    ) {
        ctx
            .get()
            .enqueueWork(() -> {
                ClientPlayerShopHandler.handleTransactionResult(
                    packet.success,
                    packet.status,
                    packet.amountTransferred
                );
            });
        ctx.get().setPacketHandled(true);
    }

    public boolean isSuccess() {
        return success;
    }

    public PlayerShopBlockEntity.TransactionResult.Status getStatus() {
        return status;
    }

    public long getAmountTransferred() {
        return amountTransferred;
    }
}
