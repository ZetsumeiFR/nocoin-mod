package com.zetsumei.nocoin.client;

import com.zetsumei.nocoin.shop.ShopItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Stocke les données de la boutique côté client.
 */
public class ClientShopData {

    private static List<ShopItem> shopItems = new ArrayList<>();
    private static Consumer<Boolean> purchaseCallback = null;

    /**
     * Définit la liste des articles de boutique.
     * @param items la liste des articles
     */
    public static void setShopItems(List<ShopItem> items) {
        shopItems = new ArrayList<>(items);
    }

    /**
     * Récupère la liste des articles de boutique (lecture seule).
     */
    public static List<ShopItem> getShopItems() {
        return Collections.unmodifiableList(shopItems);
    }

    /**
     * Vérifie si les données de boutique sont chargées.
     */
    public static boolean isLoaded() {
        return !shopItems.isEmpty();
    }

    /**
     * Efface les données de boutique.
     */
    public static void clear() {
        shopItems.clear();
    }

    /**
     * Définit un callback pour le résultat d'achat.
     * @param callback le callback à appeler avec le résultat (true = succès)
     */
    public static void setPurchaseCallback(Consumer<Boolean> callback) {
        purchaseCallback = callback;
    }

    /**
     * Appelé quand un résultat d'achat est reçu.
     * @param success si l'achat a réussi
     */
    public static void onPurchaseResult(boolean success) {
        if (purchaseCallback != null) {
            purchaseCallback.accept(success);
            purchaseCallback = null;
        }
    }
}
