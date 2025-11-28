package com.zetsumei.nocoin.client;

import com.zetsumei.nocoin.client.screen.GachaMachineScreen;
import net.minecraft.client.Minecraft;

/**
 * Gestionnaire client pour la machine à Gacha.
 * Gère l'ouverture de l'écran et les callbacks des tirages.
 */
public class ClientGachaMachineHandler {

    private static PullResultCallback pullResultCallback;

    /**
     * Ouvre l'écran de la machine à Gacha.
     * @param hasKey si le joueur a une clé
     * @param keyCount le nombre de clés du joueur
     */
    public static void openGachaMachineScreen(boolean hasKey, int keyCount) {
        Minecraft.getInstance().setScreen(new GachaMachineScreen(hasKey, keyCount));
    }

    /**
     * Gère le résultat d'un tirage.
     * @param success si le tirage a réussi
     * @param itemId l'ID de l'item obtenu
     * @param stars le nombre d'étoiles
     * @param characterName le nom du personnage
     */
    public static void handlePullResult(boolean success, String itemId, int stars, String characterName) {
        if (pullResultCallback != null) {
            pullResultCallback.onResult(success, itemId, stars, characterName);
        }
    }

    /**
     * Définit le callback pour les résultats de tirage.
     */
    public static void setPullResultCallback(PullResultCallback callback) {
        pullResultCallback = callback;
    }

    /**
     * Interface callback pour les résultats de tirage.
     */
    @FunctionalInterface
    public interface PullResultCallback {
        void onResult(boolean success, String itemId, int stars, String characterName);
    }
}
