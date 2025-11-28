package com.zetsumei.nocoin.network.gacha;

import com.zetsumei.nocoin.client.ClientGachaMachineHandler;
import com.zetsumei.nocoin.gacha.GachaHistory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet serveur → client contenant l'historique des tirages du joueur.
 */
public class GachaHistoryPacket {

    private final List<GachaHistory> histories;

    public GachaHistoryPacket(List<GachaHistory> histories) {
        this.histories = histories;
    }

    public static void encode(GachaHistoryPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.histories.size());
        for (GachaHistory history : packet.histories) {
            buf.writeUtf(history.getItemId());
            buf.writeUtf(history.getDisplayName());
            buf.writeInt(history.getStars());
            buf.writeLong(history.getTimestamp());
        }
    }

    public static GachaHistoryPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<GachaHistory> histories = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            histories.add(new GachaHistory(
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
                buf.readLong()
            ));
        }
        return new GachaHistoryPacket(histories);
    }

    public static void handle(GachaHistoryPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientGachaMachineHandler.receiveHistory(packet.histories);
        });
        ctx.get().setPacketHandled(true);
    }

    public List<GachaHistory> getHistories() {
        return histories;
    }
}
