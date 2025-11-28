package com.zetsumei.nocoin.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * Capability interface pour stocker les NOCOIN d'un joueur.
 */
public interface INocoinCapability {
    
    /**
     * Récupère le solde actuel de NOCOIN.
     * @return le nombre de NOCOIN
     */
    long getBalance();
    
    /**
     * Définit le solde de NOCOIN.
     * @param amount le nouveau solde
     */
    void setBalance(long amount);
    
    /**
     * Ajoute des NOCOIN au solde.
     * @param amount le montant à ajouter
     */
    void addBalance(long amount);
    
    /**
     * Retire des NOCOIN du solde.
     * @param amount le montant à retirer
     * @return true si le retrait a réussi, false si solde insuffisant
     */
    boolean removeBalance(long amount);
    
    /**
     * Vérifie si le joueur a suffisamment de NOCOIN.
     * @param amount le montant à vérifier
     * @return true si le solde est suffisant
     */
    boolean hasEnough(long amount);
    
    /**
     * Copie les données depuis une autre capability.
     * @param other la capability source
     */
    void copyFrom(INocoinCapability other);
    
    /**
     * Sauvegarde les données dans un CompoundTag.
     * @param tag le tag de destination
     */
    void saveNBTData(CompoundTag tag);
    
    /**
     * Charge les données depuis un CompoundTag.
     * @param tag le tag source
     */
    void loadNBTData(CompoundTag tag);
}
