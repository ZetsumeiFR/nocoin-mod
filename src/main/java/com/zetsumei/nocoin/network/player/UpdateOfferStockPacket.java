package com.zetsumei.nocoin.network.player;

import com.zetsumei.nocoin.block.entity.PlayerShopBlockEntity;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import com.zetsumei.nocoin.shop.player.ShopOffer;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet envoyé au serveur pour mettre à jour le stock d'une offre de vente.
 * Le propriétaire peut ajouter des items de son inventaire au stock.
 */
public class UpdateOfferStockPacket {

    public enum Action {
        ADD, // Ajouter des items depuis l'inventaire
        REMOVE, // Retirer des items vers l'inventaire
    }

    private final BlockPos shopPos;
    private final UUID offerId;
    private final Action action;
    private final int amount;

    public UpdateOfferStockPacket(
        BlockPos shopPos,
        UUID offerId,
        Action action,
        int amount
    ) {
        this.shopPos = shopPos;
        this.offerId = offerId;
        this.action = action;
        this.amount = amount;
    }

    public static void encode(
        UpdateOfferStockPacket packet,
        FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(packet.shopPos);
        buf.writeUUID(packet.offerId);
        buf.writeEnum(packet.action);
        buf.writeVarInt(packet.amount);
    }

    public static UpdateOfferStockPacket decode(FriendlyByteBuf buf) {
        BlockPos shopPos = buf.readBlockPos();
        UUID offerId = buf.readUUID();
        Action action = buf.readEnum(Action.class);
        int amount = buf.readVarInt();
        return new UpdateOfferStockPacket(shopPos, offerId, action, amount);
    }

    public static void handle(
        UpdateOfferStockPacket packet,
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

                ShopOffer offer = shopEntity.getOfferById(packet.offerId);
                if (
                    offer == null || offer.getType() != ShopOffer.OfferType.SELL
                ) return;

                if (packet.action == Action.ADD) {
                    // Retirer les items de l'inventaire et ajouter au stock
                    ItemStack required = offer.createItemStack();
                    required.setCount(packet.amount);

                    int available = countItemsInInventory(player, required);
                    int toAdd = Math.min(available, packet.amount);

                    if (toAdd > 0) {
                        removeItemsFromInventory(
                            player,
                            required.getItem(),
                            toAdd
                        );
                        offer.addStock(toAdd);
                        shopEntity.setChanged();
                    }
                } else {
                    // Retirer du stock et ajouter à l'inventaire
                    int currentStock = offer.getStock();
                    int toRemove = Math.min(currentStock, packet.amount);

                    if (toRemove > 0) {
                        offer.removeStock(toRemove);

                        ItemStack toGive = offer.createItemStack();
                        toGive.setCount(toRemove);

                        if (!player.getInventory().add(toGive)) {
                            player.drop(toGive, false);
                        }
                        shopEntity.setChanged();
                    }
                }

                // Rafraîchir l'écran
                NocoinNetworkHandler.sendOpenPlayerShopOwnerScreen(
                    player,
                    packet.shopPos,
                    shopEntity
                );
            });
        ctx.get().setPacketHandled(true);
    }

    private static int countItemsInInventory(
        ServerPlayer player,
        ItemStack match
    ) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, match)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItemsFromInventory(
        ServerPlayer player,
        net.minecraft.world.item.Item item,
        int amount
    ) {
        int remaining = amount;
        for (
            int i = 0;
            i < player.getInventory().items.size() && remaining > 0;
            i++
        ) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(item)) {
                int remove = Math.min(remaining, stack.getCount());
                stack.shrink(remove);
                remaining -= remove;
            }
        }
    }
}
