package com.zetsumei.nocoin.network.gacha;

import com.zetsumei.nocoin.client.ClientGachaMachineHandler;
import com.zetsumei.nocoin.gacha.GachaRarity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet serveur → client pour ouvrir l'écran admin du gacha.
 * Inclut la position de la machine pour modifier le catalogue spécifique.
 */
public class OpenGachaAdminPacket {

    private final BlockPos machinePos;
    private final List<GachaCatalogPacket.CatalogEntry> rewards;
    private final double fiveStarRate;
    private final double fourStarRate;
    private final double threeStarRate;

    public OpenGachaAdminPacket(BlockPos machinePos, List<GachaCatalogPacket.CatalogEntry> rewards, double fiveStarRate, double fourStarRate, double threeStarRate) {
        this.machinePos = machinePos;
        this.rewards = rewards;
        this.fiveStarRate = fiveStarRate;
        this.fourStarRate = fourStarRate;
        this.threeStarRate = threeStarRate;
    }

    public static void encode(OpenGachaAdminPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.machinePos);
        buf.writeDouble(packet.fiveStarRate);
        buf.writeDouble(packet.fourStarRate);
        buf.writeDouble(packet.threeStarRate);
        
        buf.writeInt(packet.rewards.size());
        for (GachaCatalogPacket.CatalogEntry entry : packet.rewards) {
            buf.writeUtf(entry.getItemId());
            buf.writeUtf(entry.getDisplayName());
            buf.writeEnum(entry.getRarity());
            buf.writeDouble(entry.getWeight());
            buf.writeDouble(entry.getEffectiveChance());
        }
    }

    public static OpenGachaAdminPacket decode(FriendlyByteBuf buf) {
        BlockPos machinePos = buf.readBlockPos();
        double fiveStarRate = buf.readDouble();
        double fourStarRate = buf.readDouble();
        double threeStarRate = buf.readDouble();
        
        int size = buf.readInt();
        List<GachaCatalogPacket.CatalogEntry> rewards = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            rewards.add(new GachaCatalogPacket.CatalogEntry(
                buf.readUtf(),
                buf.readUtf(),
                buf.readEnum(GachaRarity.class),
                buf.readDouble(),
                buf.readDouble()
            ));
        }
        
        return new OpenGachaAdminPacket(machinePos, rewards, fiveStarRate, fourStarRate, threeStarRate);
    }

    public static void handle(OpenGachaAdminPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientGachaMachineHandler.openAdminScreen(packet.machinePos, packet.rewards, packet.fiveStarRate, packet.fourStarRate, packet.threeStarRate);
        });
        ctx.get().setPacketHandled(true);
    }
}
