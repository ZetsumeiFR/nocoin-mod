package com.zetsumei.nocoin.client;

import com.zetsumei.nocoin.client.screen.PlayerShopCustomerScreen;
import com.zetsumei.nocoin.client.screen.PlayerShopOwnerScreen;
import com.zetsumei.nocoin.shop.player.ShopOffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Handler côté client pour les interactions avec les magasins joueurs.
 */
public class ClientPlayerShopHandler {

    private static TransactionCallback transactionCallback;

    /**
     * Ouvre l'écran de configuration du magasin pour le propriétaire.
     */
    public static void openOwnerScreen(BlockPos shopPos, String shopName, String ownerName,
                                        List<ShopOffer> offers, long ownerBalance) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            ClientNocoinData.setBalance(ownerBalance);
            minecraft.setScreen(new PlayerShopOwnerScreen(shopPos, shopName, ownerName, offers));
        }
    }

    /**
     * Ouvre l'écran d'achat/vente pour les visiteurs.
     */
    public static void openCustomerScreen(BlockPos shopPos, String shopName, String ownerName,
                                           List<ShopOffer> sellOffers, List<ShopOffer> buyOffers,
                                           long customerBalance) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            ClientNocoinData.setBalance(customerBalance);
            minecraft.setScreen(new PlayerShopCustomerScreen(shopPos, shopName, ownerName,
                    sellOffers, buyOffers));
        }
    }

    /**
     * Gère le résultat d'une transaction dans un magasin joueur.
     */
    public static void handleTransactionResult(boolean success,
                                                com.zetsumei.nocoin.block.entity.PlayerShopBlockEntity.TransactionResult.Status status,
                                                long amountTransferred) {
        // Demander une mise à jour du solde
        com.zetsumei.nocoin.network.NocoinNetworkHandler.requestBalanceFromServer();

        if (transactionCallback != null) {
            String message = status.name();
            transactionCallback.onTransactionResult(success, message);
            transactionCallback = null;
        }
    }

    /**
     * Définit un callback pour le prochain résultat de transaction.
     */
    public static void setTransactionCallback(TransactionCallback callback) {
        transactionCallback = callback;
    }

    /**
     * Interface de callback pour les résultats de transaction.
     */
    @FunctionalInterface
    public interface TransactionCallback {
        void onTransactionResult(boolean success, String message);
    }
}
