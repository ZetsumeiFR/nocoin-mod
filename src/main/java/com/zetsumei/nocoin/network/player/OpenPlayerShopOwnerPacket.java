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
 * Paquet envoyé au client pour ouvrir l'interface de configuration du magasin (propriétaire).
 */
public class OpenPlayerShopOwnerPacket {

    private final BlockPos shopPos;
    private final String shopName;
    private final String ownerName;
    private final List<ShopOffer> offers;
    private final long ownerBalance;

    public OpenPlayerShopOwnerPacket(
        BlockPos shopPos,
        String shopName,
        String ownerName,
        List<ShopOffer> offers,
        long ownerBalance
    ) {
        this.shopPos = shopPos;
        this.shopName = shopName;
        this.ownerName = ownerName;
        this.offers = offers;
        this.ownerBalance = ownerBalance;
    }

    public static void encode(
        OpenPlayerShopOwnerPacket packet,
        FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(packet.shopPos);
        buf.writeUtf(packet.shopName);
        buf.writeUtf(packet.ownerName);
        buf.writeVarInt(packet.offers.size());
        for (ShopOffer offer : packet.offers) {
            offer.toNetwork(buf);
        }
        buf.writeLong(packet.ownerBalance);
    }

    public static OpenPlayerShopOwnerPacket decode(FriendlyByteBuf buf) {
        BlockPos shopPos = buf.readBlockPos();
        String shopName = buf.readUtf();
        String ownerName = buf.readUtf();
        int offerCount = buf.readVarInt();
        List<ShopOffer> offers = new ArrayList<>(offerCount);
        for (int i = 0; i < offerCount; i++) {
            offers.add(ShopOffer.fromNetwork(buf));
        }
        long ownerBalance = buf.readLong();
        return new OpenPlayerShopOwnerPacket(
            shopPos,
            shopName,
            ownerName,
            offers,
            ownerBalance
        );
    }

    public static void handle(
        OpenPlayerShopOwnerPacket packet,
        Supplier<NetworkEvent.Context> ctx
    ) {
        ctx
            .get()
            .enqueueWork(() -> {
                ClientPlayerShopHandler.openOwnerScreen(
                    packet.shopPos,
                    packet.shopName,
                    packet.ownerName,
                    packet.offers,
                    packet.ownerBalance
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

    public List<ShopOffer> getOffers() {
        return offers;
    }

    public long getOwnerBalance() {
        return ownerBalance;
    }
}
