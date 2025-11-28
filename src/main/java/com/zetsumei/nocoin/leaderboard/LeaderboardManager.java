package com.zetsumei.nocoin.leaderboard;

import com.zetsumei.nocoin.capability.NocoinCapabilityProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Gestionnaire du classement des joueurs.
 * Calcule et fournit le classement par NOCOIN.
 */
public class LeaderboardManager {

    private static final int MAX_ENTRIES = 50;

    /**
     * Recupere le classement par solde NOCOIN.
     * @param server le serveur Minecraft
     * @return liste des entrees classees par NOCOIN (decroissant)
     */
    public static List<LeaderboardEntry> getLeaderboardByNocoin(
        MinecraftServer server
    ) {
        List<PlayerData> playerDataList = collectPlayerData(server);

        // Trier par NOCOIN decroissant
        playerDataList.sort(
            Comparator.comparingLong(PlayerData::nocoinBalance).reversed()
        );

        return buildRankedList(playerDataList);
    }

    /**
     * Collecte les donnees de tous les joueurs connectes.
     */
    private static List<PlayerData> collectPlayerData(MinecraftServer server) {
        List<PlayerData> dataList = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String name = player.getGameProfile().getName();
            long balance = getPlayerBalance(player);
            dataList.add(new PlayerData(name, balance));
        }

        return dataList;
    }

    /**
     * Recupere le solde NOCOIN d'un joueur.
     */
    private static long getPlayerBalance(ServerPlayer player) {
        return player
            .getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY)
            .map(cap -> cap.getBalance())
            .orElse(0L);
    }

    /**
     * Construit la liste classee avec les rangs.
     */
    private static List<LeaderboardEntry> buildRankedList(
        List<PlayerData> sortedData
    ) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        int rank = 1;
        for (int i = 0; i < Math.min(sortedData.size(), MAX_ENTRIES); i++) {
            PlayerData data = sortedData.get(i);

            // Gerer les egalites de rang
            if (i > 0) {
                PlayerData prev = sortedData.get(i - 1);
                if (data.nocoinBalance() != prev.nocoinBalance()) {
                    rank = i + 1;
                }
            }

            entries.add(
                new LeaderboardEntry(
                    data.playerName(),
                    data.nocoinBalance(),
                    rank
                )
            );
        }

        return entries;
    }

    /**
     * Types de classement disponibles.
     */
    public enum LeaderboardType {
        NOCOIN,
    }

    /**
     * Donnees internes d'un joueur pour le classement.
     */
    private record PlayerData(
        String playerName,
        long nocoinBalance
    ) {}
}
