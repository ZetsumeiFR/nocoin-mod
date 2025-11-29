package com.zetsumei.nocoin.client;

import com.zetsumei.nocoin.client.screen.LeaderboardScreen;
import com.zetsumei.nocoin.leaderboard.LeaderboardEntry;
import com.zetsumei.nocoin.leaderboard.LeaderboardManager;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Handler client pour le traitement du paquet LeaderboardDataPacket.
 * Cette classe implémente Runnable et est instanciée uniquement côté client
 * via DistExecutor.unsafeRunWhenOn.
 *
 * IMPORTANT: Cette classe ne doit JAMAIS être référencée directement dans du code
 * qui s'exécute côté serveur, sinon une erreur de chargement de classe se produira.
 */
public class LeaderboardClientHandler implements Runnable {

    private final LeaderboardManager.LeaderboardType type;
    private final List<LeaderboardEntry> entries;
    private final String playerName;

    public LeaderboardClientHandler(
            LeaderboardManager.LeaderboardType type,
            List<LeaderboardEntry> entries,
            String playerName) {
        this.type = type;
        this.entries = entries;
        this.playerName = playerName;
    }

    @Override
    public void run() {
        // Stocker les données du classement
        ClientLeaderboardData.setLeaderboardData(type, entries, playerName);

        // Ouvrir l'écran du leaderboard si aucun écran n'est ouvert
        // (quand le joueur clique sur le bloc Leaderboard)
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new LeaderboardScreen());
        }
    }
}
