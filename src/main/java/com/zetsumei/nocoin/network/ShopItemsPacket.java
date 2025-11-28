package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.client.ClientShopData;
import com.zetsumei.nocoin.shop.ShopItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet pour synchroniser la liste des articles de boutique du serveur vers le client.
 */
public class ShopItemsPacket {

    private final List<ShopItem> items;

    public ShopItemsPacket(List<ShopItem> items) {
        this.items = items;
    }

    /**
     * Encode le paquet dans le buffer.
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(items.size());
        for (ShopItem item : items) {
            item.toNetwork(buf);
        }
    }

    /**
     * Décode le paquet depuis le buffer.
     */
    public static ShopItemsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ShopItem> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(ShopItem.fromNetwork(buf));
        }
        return new ShopItemsPacket(items);
    }

    /**
     * Traite le paquet côté client.
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx
            .get()
            .enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () ->
                        () -> {
                            ClientShopData.setShopItems(this.items);
                        }
                );
            });
        ctx.get().setPacketHandled(true);
    }
}
