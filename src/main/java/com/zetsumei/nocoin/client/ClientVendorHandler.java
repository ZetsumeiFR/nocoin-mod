package com.zetsumei.nocoin.client;

import com.zetsumei.nocoin.client.screen.GachaVendorScreen;
import net.minecraft.client.Minecraft;

/**
 * Handler côté client pour les interactions avec le vendeur Gacha.
 */
public class ClientVendorHandler {

    private static PurchaseCallback purchaseCallback;

    /**
     * Ouvre l'écran du vendeur Gacha côté client.
     */
    public static void openVendorScreen(long currentBalance, long keyPrice) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            // Mettre à jour le solde local
            ClientNocoinData.setBalance(currentBalance);
            // Ouvrir l'écran
            minecraft.setScreen(new GachaVendorScreen(keyPrice));
        }
    }

    /**
     * Gère le résultat d'un achat de Clé Gacha.
     */
    public static void handlePurchaseResult(boolean success, long newBalance, int quantity) {
        // Mettre à jour le solde local
        ClientNocoinData.setBalance(newBalance);

        // Notifier le callback si défini
        if (purchaseCallback != null) {
            purchaseCallback.onPurchaseResult(success, quantity);
            purchaseCallback = null;
        }
    }

    /**
     * Définit un callback pour le prochain résultat d'achat.
     */
    public static void setPurchaseCallback(PurchaseCallback callback) {
        purchaseCallback = callback;
    }

    /**
     * Interface de callback pour les résultats d'achat.
     */
    @FunctionalInterface
    public interface PurchaseCallback {
        void onPurchaseResult(boolean success, int quantity);
    }
}
