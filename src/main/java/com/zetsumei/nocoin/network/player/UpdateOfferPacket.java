package com.zetsumei.nocoin.network.player;

import com.zetsumei.nocoin.block.entity.PlayerShopBlockEntity;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import com.zetsumei.nocoin.shop.player.ShopOffer;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet envoyé au serveur pour modifier une offre existante dans le magasin.
 * Permet de modifier le prix, la quantité et l'état actif/inactif.
 */
public class UpdateOfferPacket {

    private final BlockPos shopPos;
    private final UUID offerId;
    private final long newPricePerUnit;
    private final int newQuantity;
    private final boolean active;

    public UpdateOfferPacket(
        BlockPos shopPos,
        UUID offerId,
        long newPricePerUnit,
        int newQuantity,
        boolean active
    ) {
        this.shopPos = shopPos;
        this.offerId = offerId;
        this.newPricePerUnit = newPricePerUnit;
        this.newQuantity = newQuantity;
        this.active = active;
    }

    public static void encode(UpdateOfferPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.shopPos);
        buf.writeUUID(packet.offerId);
        buf.writeLong(packet.newPricePerUnit);
        buf.writeVarInt(packet.newQuantity);
        buf.writeBoolean(packet.active);
    }

    public static UpdateOfferPacket decode(FriendlyByteBuf buf) {
        BlockPos shopPos = buf.readBlockPos();
        UUID offerId = buf.readUUID();
        long newPricePerUnit = buf.readLong();
        int newQuantity = buf.readVarInt();
        boolean active = buf.readBoolean();
        return new UpdateOfferPacket(
            shopPos,
            offerId,
            newPricePerUnit,
            newQuantity,
            active
        );
    }

    public static void handle(
        UpdateOfferPacket packet,
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

                // Récupérer et modifier l'offre
                ShopOffer offer = shopEntity.getOfferById(packet.offerId);
                if (offer == null) return;

                // Valider les nouvelles valeurs
                if (
                    packet.newPricePerUnit <= 0 || packet.newQuantity <= 0
                ) return;

                // Appliquer les modifications
                offer.setPricePerUnit(packet.newPricePerUnit);
                offer.setQuantity(packet.newQuantity);
                offer.setActive(packet.active);

                // Sauvegarder et synchroniser
                shopEntity.setChanged();

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
