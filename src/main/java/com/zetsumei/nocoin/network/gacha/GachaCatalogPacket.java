package com.zetsumei.nocoin.network.gacha;

import com.zetsumei.nocoin.client.ClientGachaMachineHandler;
import com.zetsumei.nocoin.gacha.GachaRarity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet serveur → client contenant le catalogue complet des récompenses gacha.
 */
public class GachaCatalogPacket {

    private final List<CatalogEntry> entries;
    private final double fiveStarRate;
    private final double fourStarRate;
    private final double threeStarRate;

    public GachaCatalogPacket(List<CatalogEntry> entries, double fiveStarRate, double fourStarRate, double threeStarRate) {
        this.entries = entries;
        this.fiveStarRate = fiveStarRate;
        this.fourStarRate = fourStarRate;
        this.threeStarRate = threeStarRate;
    }

    public static void encode(GachaCatalogPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.fiveStarRate);
        buf.writeDouble(packet.fourStarRate);
        buf.writeDouble(packet.threeStarRate);
        
        buf.writeInt(packet.entries.size());
        for (CatalogEntry entry : packet.entries) {
            buf.writeUtf(entry.itemId);
            buf.writeUtf(entry.displayName);
            buf.writeEnum(entry.rarity);
            buf.writeDouble(entry.weight);
            buf.writeDouble(entry.effectiveChance);
        }
    }

    public static GachaCatalogPacket decode(FriendlyByteBuf buf) {
        double fiveStarRate = buf.readDouble();
        double fourStarRate = buf.readDouble();
        double threeStarRate = buf.readDouble();
        
        int size = buf.readInt();
        List<CatalogEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new CatalogEntry(
                buf.readUtf(),
                buf.readUtf(),
                buf.readEnum(GachaRarity.class),
                buf.readDouble(),
                buf.readDouble()
            ));
        }
        
        return new GachaCatalogPacket(entries, fiveStarRate, fourStarRate, threeStarRate);
    }

    public static void handle(GachaCatalogPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientGachaMachineHandler.receiveCatalog(packet.entries, packet.fiveStarRate, packet.fourStarRate, packet.threeStarRate);
        });
        ctx.get().setPacketHandled(true);
    }

    public List<CatalogEntry> getEntries() {
        return entries;
    }

    /**
     * Représente une entrée du catalogue.
     */
    public static class CatalogEntry {
        private final String itemId;
        private final String displayName;
        private final GachaRarity rarity;
        private final double weight;
        private final double effectiveChance;

        public CatalogEntry(String itemId, String displayName, GachaRarity rarity, double weight, double effectiveChance) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.rarity = rarity;
            this.weight = weight;
            this.effectiveChance = effectiveChance;
        }

        public String getItemId() {
            return itemId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public GachaRarity getRarity() {
            return rarity;
        }

        public double getWeight() {
            return weight;
        }

        public double getEffectiveChance() {
            return effectiveChance;
        }
    }
}
