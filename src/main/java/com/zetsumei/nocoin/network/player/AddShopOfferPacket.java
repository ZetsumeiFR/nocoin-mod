package com.zetsumei.nocoin.network.player;

import com.zetsumei.nocoin.block.entity.PlayerShopBlockEntity;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import com.zetsumei.nocoin.shop.player.ShopOffer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet envoyé au serveur pour ajouter une offre au magasin.
 */
public class AddShopOfferPacket {

    private final BlockPos shopPos;
    private final ShopOffer.OfferType type;
    private final String itemId;
    private final int quantity;
    private final long pricePerUnit;
    private final int stock;

    public AddShopOfferPacket(
        BlockPos shopPos,
        ShopOffer.OfferType type,
        String itemId,
        int quantity,
        long pricePerUnit,
        int stock
    ) {
        this.shopPos = shopPos;
        this.type = type;
        this.itemId = itemId;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.stock = stock;
    }

    public static void encode(AddShopOfferPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.shopPos);
        buf.writeEnum(packet.type);
        buf.writeUtf(packet.itemId);
        buf.writeVarInt(packet.quantity);
        buf.writeLong(packet.pricePerUnit);
        buf.writeVarInt(packet.stock);
    }

    public static AddShopOfferPacket decode(FriendlyByteBuf buf) {
        BlockPos shopPos = buf.readBlockPos();
        ShopOffer.OfferType type = buf.readEnum(ShopOffer.OfferType.class);
        String itemId = buf.readUtf();
        int quantity = buf.readVarInt();
        long pricePerUnit = buf.readLong();
        int stock = buf.readVarInt();
        return new AddShopOfferPacket(
            shopPos,
            type,
            itemId,
            quantity,
            pricePerUnit,
            stock
        );
    }

    public static void handle(
        AddShopOfferPacket packet,
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

                // Vérifier qu'on peut ajouter une offre
                if (!shopEntity.canAddOffer()) return;

                // Créer et ajouter l'offre
                ShopOffer newOffer = new ShopOffer(
                    packet.type,
                    packet.itemId,
                    packet.quantity,
                    packet.pricePerUnit,
                    packet.stock
                );

                if (newOffer.isValid()) {
                    shopEntity.addOffer(newOffer);
                    // Rafraîchir l'écran du propriétaire
                    NocoinNetworkHandler.sendOpenPlayerShopOwnerScreen(
                        player,
                        packet.shopPos,
                        shopEntity
                    );
                }
            });
        ctx.get().setPacketHandled(true);
    }
}
