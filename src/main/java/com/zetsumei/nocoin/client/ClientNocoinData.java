package com.zetsumei.nocoin.client;

/**
 * Stockage côté client du solde NOCOIN.
 * Cette classe est mise à jour via les paquets réseau depuis le serveur.
 */
public class ClientNocoinData {
    
    private static long playerBalance = 0;
    
    /**
     * Récupère le solde actuel du joueur (côté client).
     * @return le solde en NOCOIN
     */
    public static long getBalance() {
        return playerBalance;
    }
    
    /**
     * Met à jour le solde du joueur (appelé depuis le packet handler).
     * @param balance le nouveau solde
     */
    public static void setBalance(long balance) {
        playerBalance = balance;
    }
    
    /**
     * Réinitialise les données client (lors de la déconnexion).
     */
    public static void reset() {
        playerBalance = 0;
    }
}
