package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.leaderboard.LeaderboardEntry;
import com.zetsumei.nocoin.leaderboard.LeaderboardManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet pour synchroniser les données du classement du serveur vers le client.
 */
public class LeaderboardDataPacket {

    private final LeaderboardManager.LeaderboardType type;
    private final List<LeaderboardEntry> entries;
    private final String requestingPlayerName;

    public LeaderboardDataPacket(
        LeaderboardManager.LeaderboardType type,
        List<LeaderboardEntry> entries,
        String requestingPlayerName
    ) {
        this.type = type;
        this.entries = entries;
        this.requestingPlayerName = requestingPlayerName;
    }

    /**
     * Encode le paquet dans le buffer.
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(type);
        buf.writeUtf(requestingPlayerName);
        buf.writeVarInt(entries.size());
        for (LeaderboardEntry entry : entries) {
            entry.toNetwork(buf);
        }
    }

    /**
     * Décode le paquet depuis le buffer.
     */
    public static LeaderboardDataPacket decode(FriendlyByteBuf buf) {
        LeaderboardManager.LeaderboardType type = buf.readEnum(
            LeaderboardManager.LeaderboardType.class
        );
        String playerName = buf.readUtf();
        int size = buf.readVarInt();
        List<LeaderboardEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(LeaderboardEntry.fromNetwork(buf));
        }
        return new LeaderboardDataPacket(type, entries, playerName);
    }

    public LeaderboardManager.LeaderboardType getType() {
        return type;
    }

    public List<LeaderboardEntry> getEntries() {
        return entries;
    }

    public String getRequestingPlayerName() {
        return requestingPlayerName;
    }

    /**
     * Traite le paquet côté client.
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        final LeaderboardManager.LeaderboardType packetType = this.type;
        final List<LeaderboardEntry> packetEntries = new ArrayList<>(this.entries);
        final String packetPlayerName = this.requestingPlayerName;

        ctx.get().enqueueWork(() -> {
            // DistExecutor garantit que ce code ne s'exécute que côté client
            // La classe LeaderboardClientHandler est dans le package client et
            // n'est chargée que quand ce Runnable est exécuté (côté client uniquement)
            // IMPORTANT: On utilise le nom complet sans import pour éviter le chargement
            // de la classe sur le serveur dédié
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> new com.zetsumei.nocoin.client.LeaderboardClientHandler(
                    packetType, packetEntries, packetPlayerName));
        });
        ctx.get().setPacketHandled(true);
    }
}
