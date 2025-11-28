package com.zetsumei.nocoin.client;

import com.zetsumei.nocoin.client.screen.GachaAdminScreen;
import com.zetsumei.nocoin.client.screen.GachaMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class ClientGachaMachineHandler {

    private static PullResultCallback pullResultCallback;
    private static MultiPullResultCallback multiPullResultCallback;
    private static CatalogCallback catalogCallback;
    private static HistoryCallback historyCallback;

    // Position de la machine gacha actuellement ouverte
    private static BlockPos currentMachinePos;

    // Données en cache pour l'écran
    private static java.util.List<
        com.zetsumei.nocoin.network.gacha.GachaCatalogPacket.CatalogEntry
    > cachedCatalog;
    private static java.util.List<
        com.zetsumei.nocoin.gacha.GachaHistory
    > cachedHistory;
    private static double cachedFiveStarRate;
    private static double cachedFourStarRate;
    private static double cachedThreeStarRate;

    /**
     * Retourne la position de la machine gacha actuellement ouverte.
     */
    public static BlockPos getCurrentMachinePos() {
        return currentMachinePos;
    }

    /**
     * Ouvre l'écran de la machine à Gacha.
     * @param machinePos la position de la machine
     * @param hasKey si le joueur a une clé
     * @param keyCount le nombre de clés du joueur
     */
    public static void openGachaMachineScreen(BlockPos machinePos, boolean hasKey, int keyCount) {
        currentMachinePos = machinePos;
        Minecraft.getInstance().setScreen(
            new GachaMachineScreen(machinePos, hasKey, keyCount)
        );
    }

    /**
     * Ouvre l'écran d'administration du gacha.
     * @param machinePos la position de la machine
     */
    public static void openAdminScreen(
        BlockPos machinePos,
        java.util.List<
            com.zetsumei.nocoin.network.gacha.GachaCatalogPacket.CatalogEntry
        > rewards,
        double fiveStarRate,
        double fourStarRate,
        double threeStarRate
    ) {
        currentMachinePos = machinePos;
        cachedCatalog = rewards;
        cachedFiveStarRate = fiveStarRate;
        cachedFourStarRate = fourStarRate;
        cachedThreeStarRate = threeStarRate;
        Minecraft.getInstance().setScreen(
            new com.zetsumei.nocoin.client.screen.GachaAdminScreen(
                machinePos,
                rewards,
                fiveStarRate,
                fourStarRate,
                threeStarRate
            )
        );
    }

    /**
     * Reçoit le catalogue des récompenses.
     */
    public static void receiveCatalog(
        java.util.List<
            com.zetsumei.nocoin.network.gacha.GachaCatalogPacket.CatalogEntry
        > entries,
        double fiveStarRate,
        double fourStarRate,
        double threeStarRate
    ) {
        cachedCatalog = entries;
        cachedFiveStarRate = fiveStarRate;
        cachedFourStarRate = fourStarRate;
        cachedThreeStarRate = threeStarRate;
        if (catalogCallback != null) {
            catalogCallback.onCatalogReceived(
                entries,
                fiveStarRate,
                fourStarRate,
                threeStarRate
            );
        }
    }

    /**
     * Reçoit l'historique des tirages.
     */
    public static void receiveHistory(
        java.util.List<com.zetsumei.nocoin.gacha.GachaHistory> histories
    ) {
        cachedHistory = histories;
        if (historyCallback != null) {
            historyCallback.onHistoryReceived(histories);
        }
    }

    /**
     * Reçoit le résultat d'un multi-tirage.
     */
    public static void receiveMultiPullResult(
        boolean success,
        java.util.List<
            com.zetsumei.nocoin.network.gacha.GachaMultiPullResultPacket.PullResult
        > results
    ) {
        if (multiPullResultCallback != null) {
            multiPullResultCallback.onResult(success, results);
        }
    }

    /**
     * Gère le résultat d'un tirage simple.
     */
    public static void handlePullResult(
        boolean success,
        String itemId,
        int stars,
        String characterName
    ) {
        if (pullResultCallback != null) {
            pullResultCallback.onResult(success, itemId, stars, characterName);
        }
    }

    // =============== Getters pour les données en cache ===============

    public static java.util.List<
        com.zetsumei.nocoin.network.gacha.GachaCatalogPacket.CatalogEntry
    > getCachedCatalog() {
        return cachedCatalog;
    }

    public static java.util.List<
        com.zetsumei.nocoin.gacha.GachaHistory
    > getCachedHistory() {
        return cachedHistory;
    }

    public static double getCachedFiveStarRate() {
        return cachedFiveStarRate;
    }

    public static double getCachedFourStarRate() {
        return cachedFourStarRate;
    }

    public static double getCachedThreeStarRate() {
        return cachedThreeStarRate;
    }

    // =============== Setters pour les callbacks ===============

    public static void setPullResultCallback(PullResultCallback callback) {
        pullResultCallback = callback;
    }

    public static void setMultiPullResultCallback(
        MultiPullResultCallback callback
    ) {
        multiPullResultCallback = callback;
    }

    public static void setCatalogCallback(CatalogCallback callback) {
        catalogCallback = callback;
    }

    public static void setHistoryCallback(HistoryCallback callback) {
        historyCallback = callback;
    }

    // =============== Interfaces callbacks ===============

    @FunctionalInterface
    public interface PullResultCallback {
        void onResult(
            boolean success,
            String itemId,
            int stars,
            String characterName
        );
    }

    @FunctionalInterface
    public interface MultiPullResultCallback {
        void onResult(
            boolean success,
            java.util.List<
                com.zetsumei.nocoin.network.gacha.GachaMultiPullResultPacket.PullResult
            > results
        );
    }

    @FunctionalInterface
    public interface CatalogCallback {
        void onCatalogReceived(
            java.util.List<
                com.zetsumei.nocoin.network.gacha.GachaCatalogPacket.CatalogEntry
            > entries,
            double fiveStarRate,
            double fourStarRate,
            double threeStarRate
        );
    }

    @FunctionalInterface
    public interface HistoryCallback {
        void onHistoryReceived(
            java.util.List<com.zetsumei.nocoin.gacha.GachaHistory> histories
        );
    }
}
