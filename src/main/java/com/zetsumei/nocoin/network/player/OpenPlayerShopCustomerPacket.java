package com.zetsumei.nocoin.network.player;

import com.zetsumei.nocoin.client.ClientPlayerShopHandler;
import com.zetsumei.nocoin.shop.player.ShopOffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet envoyé au client pour ouvrir l'interface client du magasin (visiteur).
 */
public class OpenPlayerShopCustomerPacket {

    private final BlockPos shopPos;
    private final String shopName;
    private final String ownerName;
    private final List<ShopOffer> sellOffers;
    private final List<ShopOffer> buyOffers;
    private final long customerBalance;

    public OpenPlayerShopCustomerPacket(
        BlockPos shopPos,
        String shopName,
        String ownerName,
        List<ShopOffer> sellOffers,
        List<ShopOffer> buyOffers,
        long customerBalance
    ) {
        this.shopPos = shopPos;
        this.shopName = shopName;
        this.ownerName = ownerName;
        this.sellOffers = sellOffers;
        this.buyOffers = buyOffers;
        this.customerBalance = customerBalance;
    }

    public static void encode(
        OpenPlayerShopCustomerPacket packet,
        FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(packet.shopPos);
        buf.writeUtf(packet.shopName);
        buf.writeUtf(packet.ownerName);

        buf.writeVarInt(packet.sellOffers.size());
        for (ShopOffer offer : packet.sellOffers) {
            offer.toNetwork(buf);
        }

        buf.writeVarInt(packet.buyOffers.size());
        for (ShopOffer offer : packet.buyOffers) {
            offer.toNetwork(buf);
        }

        buf.writeLong(packet.customerBalance);
    }

    public static OpenPlayerShopCustomerPacket decode(FriendlyByteBuf buf) {
        BlockPos shopPos = buf.readBlockPos();
        String shopName = buf.readUtf();
        String ownerName = buf.readUtf();

        int sellCount = buf.readVarInt();
        List<ShopOffer> sellOffers = new ArrayList<>(sellCount);
        for (int i = 0; i < sellCount; i++) {
            sellOffers.add(ShopOffer.fromNetwork(buf));
        }

        int buyCount = buf.readVarInt();
        List<ShopOffer> buyOffers = new ArrayList<>(buyCount);
        for (int i = 0; i < buyCount; i++) {
            buyOffers.add(ShopOffer.fromNetwork(buf));
        }

        long customerBalance = buf.readLong();
        return new OpenPlayerShopCustomerPacket(
            shopPos,
            shopName,
            ownerName,
            sellOffers,
            buyOffers,
            customerBalance
        );
    }

    public static void handle(
        OpenPlayerShopCustomerPacket packet,
        Supplier<NetworkEvent.Context> ctx
    ) {
        ctx
            .get()
            .enqueueWork(() -> {
                ClientPlayerShopHandler.openCustomerScreen(
                    packet.shopPos,
                    packet.shopName,
                    packet.ownerName,
                    packet.sellOffers,
                    packet.buyOffers,
                    packet.customerBalance
                );
            });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getShopPos() {
        return shopPos;
    }

    public String getShopName() {
        return shopName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public List<ShopOffer> getSellOffers() {
        return sellOffers;
    }

    public List<ShopOffer> getBuyOffers() {
        return buyOffers;
    }

    public long getCustomerBalance() {
        return customerBalance;
    }
}
