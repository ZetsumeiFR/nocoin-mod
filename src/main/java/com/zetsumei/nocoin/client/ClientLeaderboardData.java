package com.zetsumei.nocoin.client;

import com.zetsumei.nocoin.leaderboard.LeaderboardEntry;
import com.zetsumei.nocoin.leaderboard.LeaderboardManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stocke les donnees du classement cote client.
 */
public class ClientLeaderboardData {

    private static List<LeaderboardEntry> nocoinEntries = new ArrayList<>();
    private static String currentPlayerName = "";
    private static boolean nocoinLoaded = false;

    /**
     * Definit les donnees du classement.
     * @param type le type de classement
     * @param entries les entrees du classement
     * @param playerName le nom du joueur qui a fait la requete
     */
    public static void setLeaderboardData(LeaderboardManager.LeaderboardType type,
                                           List<LeaderboardEntry> entries,
                                           String playerName) {
        currentPlayerName = playerName;
        if (type == LeaderboardManager.LeaderboardType.NOCOIN) {
            nocoinEntries = new ArrayList<>(entries);
            nocoinLoaded = true;
        }
    }

    /**
     * Recupere les entrees du classement par NOCOIN.
     */
    public static List<LeaderboardEntry> getNocoinEntries() {
        return Collections.unmodifiableList(nocoinEntries);
    }

    /**
     * Recupere le nom du joueur actuel.
     */
    public static String getCurrentPlayerName() {
        return currentPlayerName;
    }

    /**
     * Verifie si les donnees NOCOIN sont chargees.
     */
    public static boolean isNocoinLoaded() {
        return nocoinLoaded;
    }

    /**
     * Trouve la position du joueur actuel dans le classement NOCOIN.
     * @return le rang du joueur, ou -1 si non trouve
     */
    public static int getCurrentPlayerNocoinRank() {
        for (LeaderboardEntry entry : nocoinEntries) {
            if (entry.getPlayerName().equals(currentPlayerName)) {
                return entry.getRank();
            }
        }
        return -1;
    }

    /**
     * Efface toutes les donnees.
     */
    public static void clear() {
        nocoinEntries.clear();
        currentPlayerName = "";
        nocoinLoaded = false;
    }

    /**
     * Reinitialise les indicateurs de chargement.
     */
    public static void resetLoadingState() {
        nocoinLoaded = false;
    }
}
