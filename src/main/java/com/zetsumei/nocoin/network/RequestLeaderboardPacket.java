package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.leaderboard.LeaderboardManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Paquet pour demander les données du classement au serveur.
 * Envoyé du client vers le serveur.
 */
public class RequestLeaderboardPacket {

    private final LeaderboardManager.LeaderboardType type;

    public RequestLeaderboardPacket(LeaderboardManager.LeaderboardType type) {
        this.type = type;
    }

    /**
     * Encode le paquet dans le buffer.
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(type);
    }

    /**
     * Décode le paquet depuis le buffer.
     */
    public static RequestLeaderboardPacket decode(FriendlyByteBuf buf) {
        LeaderboardManager.LeaderboardType type = buf.readEnum(
            LeaderboardManager.LeaderboardType.class
        );
        return new RequestLeaderboardPacket(type);
    }

    /**
     * Traite le paquet côté serveur.
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx
            .get()
            .enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null && player.getServer() != null) {
                    // Envoyer les données du classement au client
                    NocoinNetworkHandler.sendLeaderboardToClient(player, type);
                }
            });
        ctx.get().setPacketHandled(true);
    }

    public LeaderboardManager.LeaderboardType getType() {
        return type;
    }
}
