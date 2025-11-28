package com.zetsumei.nocoin.network.gacha;

import com.zetsumei.nocoin.client.ClientGachaMachineHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet serveur → client contenant les résultats d'un multi-tirage.
 */
public class GachaMultiPullResultPacket {

    private final boolean success;
    private final List<PullResult> results;

    public GachaMultiPullResultPacket(boolean success, List<PullResult> results) {
        this.success = success;
        this.results = results;
    }

    public static void encode(GachaMultiPullResultPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.success);
        buf.writeInt(packet.results.size());
        for (PullResult result : packet.results) {
            buf.writeUtf(result.itemId);
            buf.writeUtf(result.displayName);
            buf.writeInt(result.stars);
        }
    }

    public static GachaMultiPullResultPacket decode(FriendlyByteBuf buf) {
        boolean success = buf.readBoolean();
        int size = buf.readInt();
        List<PullResult> results = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            results.add(new PullResult(
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt()
            ));
        }
        return new GachaMultiPullResultPacket(success, results);
    }

    public static void handle(GachaMultiPullResultPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientGachaMachineHandler.receiveMultiPullResult(packet.success, packet.results);
        });
        ctx.get().setPacketHandled(true);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<PullResult> getResults() {
        return results;
    }

    /**
     * Représente un résultat de tirage.
     */
    public static class PullResult {
        private final String itemId;
        private final String displayName;
        private final int stars;

        public PullResult(String itemId, String displayName, int stars) {
            this.itemId = itemId;
            this.displayName = displayName;
            this.stars = stars;
        }

        public String getItemId() {
            return itemId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getStars() {
            return stars;
        }
    }
}
