package com.zetsumei.nocoin.block.entity;

import com.mojang.logging.LogUtils;
import com.zetsumei.nocoin.gacha.GachaManager;
import com.zetsumei.nocoin.gacha.GachaRarity;
import com.zetsumei.nocoin.gacha.GachaReward;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

/**
 * BlockEntity pour la machine à Gacha.
 * Stocke un catalogue de récompenses indépendant pour chaque machine.
 */
public class GachaMachineBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static final String TAG_MACHINE_ID = "machineId";
    private static final String TAG_MACHINE_NAME = "machineName";
    private static final String TAG_REWARDS = "rewards";
    private static final String TAG_FIVE_STAR_RATE = "fiveStarRate";
    private static final String TAG_FOUR_STAR_RATE = "fourStarRate";
    private static final String TAG_THREE_STAR_RATE = "threeStarRate";

    // Identifiant unique de la machine
    private UUID machineId;
    
    // Nom personnalisé de la machine (optionnel)
    private String machineName = "";
    
    // Catalogue de récompenses propre à cette machine
    private final List<GachaReward> rewards = new ArrayList<>();
    
    // Probabilités de rareté propres à cette machine
    private double fiveStarRate = 3.0;   // 3%
    private double fourStarRate = 15.0;  // 15%
    private double threeStarRate = 82.0; // 82%
    
    private final Random random = new Random();

    public GachaMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GACHA_MACHINE.get(), pos, state);
        // Générer un ID unique à la création
        this.machineId = UUID.randomUUID();
    }

    // ============== Identité de la machine ==============

    public UUID getMachineId() {
        return machineId;
    }

    public String getMachineName() {
        return machineName.isEmpty() ? "Gacha Machine" : machineName;
    }

    public void setMachineName(String name) {
        this.machineName = name != null ? name : "";
        setChanged();
        syncToClient();
    }

    // ============== Gestion du catalogue ==============

    public List<GachaReward> getAllRewards() {
        return Collections.unmodifiableList(rewards);
    }

    /**
     * Alias pour getAllRewards() pour la compatibilité.
     */
    public List<GachaReward> getRewards() {
        return Collections.unmodifiableList(rewards);
    }

    public List<GachaReward> getRewardsByRarity(GachaRarity rarity) {
        return rewards.stream()
                .filter(r -> r.getRarity() == rarity)
                .toList();
    }

    public int getRewardCount() {
        return rewards.size();
    }

    /**
     * Ajoute une récompense au catalogue de cette machine.
     * @return true si ajoutée avec succès, false si invalide ou déjà existante
     */
    public boolean addReward(String itemId, GachaRarity rarity, String displayName, double weight) {
        // Vérifier si l'item existe déjà dans ce catalogue
        if (findRewardByItemId(itemId).isPresent()) {
            LOGGER.warn("Récompense {} déjà présente dans la machine {}", itemId, machineId);
            return false;
        }

        GachaReward reward = new GachaReward(itemId, rarity, displayName, weight);
        if (!reward.isValid()) {
            LOGGER.warn("Récompense invalide: {} (item non trouvé)", itemId);
            return false;
        }

        rewards.add(reward);
        setChanged();
        syncToClient();
        LOGGER.info("Récompense ajoutée à la machine {}: {} ({}★)", machineId, displayName, rarity.getStars());
        return true;
    }

    /**
     * Supprime une récompense du catalogue de cette machine.
     * @return true si supprimée, false si non trouvée
     */
    public boolean removeReward(String itemId) {
        boolean removed = rewards.removeIf(r -> r.getItemId().equals(itemId));
        if (removed) {
            setChanged();
            syncToClient();
            LOGGER.info("Récompense {} supprimée de la machine {}", itemId, machineId);
        }
        return removed;
    }

    /**
     * Vide le catalogue de cette machine.
     */
    public void clearAllRewards() {
        rewards.clear();
        setChanged();
        syncToClient();
        LOGGER.info("Catalogue vidé pour la machine {}", machineId);
    }

    /**
     * Modifie le poids d'une récompense.
     */
    public boolean setRewardWeight(String itemId, double weight) {
        Optional<GachaReward> rewardOpt = findRewardByItemId(itemId);
        if (rewardOpt.isPresent()) {
            GachaReward oldReward = rewardOpt.get();
            int index = rewards.indexOf(oldReward);
            rewards.set(index, oldReward.withWeight(weight));
            setChanged();
            syncToClient();
            return true;
        }
        return false;
    }

    /**
     * Modifie la rareté d'une récompense.
     */
    public boolean setRewardRarity(String itemId, GachaRarity newRarity) {
        Optional<GachaReward> rewardOpt = findRewardByItemId(itemId);
        if (rewardOpt.isPresent()) {
            GachaReward oldReward = rewardOpt.get();
            int index = rewards.indexOf(oldReward);
            rewards.set(index, new GachaReward(
                    oldReward.getItemId(),
                    newRarity,
                    oldReward.getDisplayName(),
                    oldReward.getWeight()
            ));
            setChanged();
            syncToClient();
            return true;
        }
        return false;
    }

    public Optional<GachaReward> findRewardByItemId(String itemId) {
        return rewards.stream()
                .filter(r -> r.getItemId().equals(itemId))
                .findFirst();
    }

    // ============== Gestion des taux ==============

    public void setRarityRates(double fiveStar, double fourStar, double threeStar) {
        this.fiveStarRate = fiveStar;
        this.fourStarRate = fourStar;
        this.threeStarRate = threeStar;
        setChanged();
        syncToClient();
        LOGGER.info("Taux mis à jour pour la machine {}: 5★={}%, 4★={}%, 3★={}%", 
                machineId, fiveStar, fourStar, threeStar);
    }

    public double getFiveStarRate() {
        return fiveStarRate;
    }

    public double getFourStarRate() {
        return fourStarRate;
    }

    public double getThreeStarRate() {
        return threeStarRate;
    }

    // ============== Tirage ==============

    /**
     * Effectue un tirage aléatoire dans le catalogue de cette machine.
     * @return le résultat du tirage, ou null si le catalogue est vide
     */
    @Nullable
    public GachaManager.GachaPullResult pull() {
        if (rewards.isEmpty()) {
            LOGGER.warn("Tentative de tirage sur une machine vide: {}", machineId);
            return null;
        }

        // Déterminer la rareté
        GachaRarity rarity = determineRarity();

        // Récupérer les récompenses de cette rareté
        List<GachaReward> possibleRewards = getRewardsByRarity(rarity);

        // Si aucune récompense de cette rareté, essayer les autres
        if (possibleRewards.isEmpty()) {
            for (GachaRarity fallbackRarity : GachaRarity.values()) {
                possibleRewards = getRewardsByRarity(fallbackRarity);
                if (!possibleRewards.isEmpty()) {
                    rarity = fallbackRarity;
                    break;
                }
            }
        }

        if (possibleRewards.isEmpty()) {
            return null;
        }

        // Tirage pondéré
        double totalWeight = possibleRewards.stream()
                .mapToDouble(GachaReward::getWeight)
                .sum();
        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0;

        for (GachaReward reward : possibleRewards) {
            cumulative += reward.getWeight();
            if (roll <= cumulative) {
                return new GachaManager.GachaPullResult(reward, reward.createStack());
            }
        }

        // Fallback au dernier
        GachaReward lastReward = possibleRewards.get(possibleRewards.size() - 1);
        return new GachaManager.GachaPullResult(lastReward, lastReward.createStack());
    }

    private GachaRarity determineRarity() {
        double roll = random.nextDouble() * 100;
        if (roll < fiveStarRate) {
            return GachaRarity.FIVE_STAR;
        } else if (roll < fiveStarRate + fourStarRate) {
            return GachaRarity.FOUR_STAR;
        } else {
            return GachaRarity.THREE_STAR;
        }
    }

    // ============== Donner la récompense au joueur ==============

    /**
     * Donne la récompense au joueur.
     * @param player le joueur destinataire
     * @param reward la récompense à donner
     */
    public void giveRewardToPlayer(net.minecraft.server.level.ServerPlayer player, GachaReward reward) {
        if (player == null || reward == null) return;

        net.minecraft.world.item.ItemStack itemStack = reward.createStack();
        if (!itemStack.isEmpty()) {
            if (!player.getInventory().add(itemStack.copy())) {
                // Inventaire plein, drop l'item
                player.drop(itemStack.copy(), false);
            }
        }
    }

    // ============== Copie depuis le catalogue global ==============

    /**
     * Copie le catalogue global dans cette machine.
     * Utile pour initialiser une nouvelle machine avec les récompenses par défaut.
     */
    public void copyFromGlobalCatalog() {
        GachaManager global = GachaManager.getInstance();
        rewards.clear();
        rewards.addAll(global.getAllRewards());
        fiveStarRate = global.getFiveStarRate();
        fourStarRate = global.getFourStarRate();
        threeStarRate = global.getThreeStarRate();
        setChanged();
        syncToClient();
        LOGGER.info("Catalogue global copié vers la machine {}: {} récompenses", machineId, rewards.size());
    }

    // ============== Sauvegarde NBT ==============

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putUUID(TAG_MACHINE_ID, machineId);
        tag.putString(TAG_MACHINE_NAME, machineName);
        
        tag.putDouble(TAG_FIVE_STAR_RATE, fiveStarRate);
        tag.putDouble(TAG_FOUR_STAR_RATE, fourStarRate);
        tag.putDouble(TAG_THREE_STAR_RATE, threeStarRate);

        ListTag rewardsList = new ListTag();
        for (GachaReward reward : rewards) {
            rewardsList.add(reward.toNbt());
        }
        tag.put(TAG_REWARDS, rewardsList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.hasUUID(TAG_MACHINE_ID)) {
            machineId = tag.getUUID(TAG_MACHINE_ID);
        }
        machineName = tag.getString(TAG_MACHINE_NAME);

        fiveStarRate = tag.contains(TAG_FIVE_STAR_RATE) ? tag.getDouble(TAG_FIVE_STAR_RATE) : 3.0;
        fourStarRate = tag.contains(TAG_FOUR_STAR_RATE) ? tag.getDouble(TAG_FOUR_STAR_RATE) : 15.0;
        threeStarRate = tag.contains(TAG_THREE_STAR_RATE) ? tag.getDouble(TAG_THREE_STAR_RATE) : 82.0;

        rewards.clear();
        if (tag.contains(TAG_REWARDS)) {
            ListTag rewardsList = tag.getList(TAG_REWARDS, Tag.TAG_COMPOUND);
            for (int i = 0; i < rewardsList.size(); i++) {
                try {
                    GachaReward reward = GachaReward.fromNbt(rewardsList.getCompound(i));
                    if (reward.isValid()) {
                        rewards.add(reward);
                    }
                } catch (Exception e) {
                    LOGGER.error("Erreur lors du chargement d'une récompense", e);
                }
            }
        }
    }

    // ============== Synchronisation Client ==============

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
