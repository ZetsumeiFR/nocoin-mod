package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.client.ClientLeaderboardData;
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
                            ClientLeaderboardData.setLeaderboardData(
                                type,
                                entries,
                                requestingPlayerName
                            );
                            // Ouvrir l'écran du leaderboard si aucun écran n'est ouvert
                            // (quand le joueur clique sur le bloc Leaderboard)
                            net.minecraft.client.Minecraft mc =
                                net.minecraft.client.Minecraft.getInstance();
                            if (mc.screen == null) {
                                mc.setScreen(
                                    new com.zetsumei.nocoin.client.screen.LeaderboardScreen()
                                );
                            }
                        }
                );
            });
        ctx.get().setPacketHandled(true);
    }
}
