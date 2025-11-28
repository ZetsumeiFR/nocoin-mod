package com.zetsumei.nocoin.block.entity;

import com.zetsumei.nocoin.leaderboard.LeaderboardEntry;
import com.zetsumei.nocoin.leaderboard.LeaderboardManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BlockEntity pour le bloc Leaderboard.
 * Stocke et synchronise les données du classement pour l'affichage en 3D.
 * 
 * Fonctionnalités:
 * - Mise à jour automatique des données toutes les 5 secondes (côté serveur)
 * - Stockage local du top 10 pour affichage rapide
 * - Synchronisation vers les clients pour le rendu
 * - Animation de particules côté client
 */
public class LeaderboardBlockEntity extends BlockEntity {

    private static final String TAG_ENTRIES = "leaderboardEntries";
    private static final String TAG_TYPE = "leaderboardType";
    private static final int UPDATE_INTERVAL_TICKS = 100; // 5 secondes (20 ticks/seconde)
    private static final int MAX_DISPLAY_ENTRIES = 10;

    // Données du classement
    private List<LeaderboardEntry> entries = new ArrayList<>();
    private LeaderboardManager.LeaderboardType currentType = LeaderboardManager.LeaderboardType.NOCOIN;

    // Timers et animation
    private int tickCounter = 0;
    private int animationTick = 0;
    private final Random random = new Random();

    public LeaderboardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEADERBOARD.get(), pos, state);
    }

    // ============== Getters ==============

    public List<LeaderboardEntry> getEntries() {
        return entries;
    }

    public LeaderboardManager.LeaderboardType getCurrentType() {
        return currentType;
    }

    public int getAnimationTick() {
        return animationTick;
    }

    // ============== Tick Methods ==============

    /**
     * Tick côté serveur - Met à jour les données du classement périodiquement.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, LeaderboardBlockEntity blockEntity) {
        blockEntity.tickCounter++;

        // Mettre à jour les données toutes les 5 secondes
        if (blockEntity.tickCounter >= UPDATE_INTERVAL_TICKS) {
            blockEntity.tickCounter = 0;
            blockEntity.updateLeaderboardData();
        }
    }

    /**
     * Tick côté client - Gère les animations et particules.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, LeaderboardBlockEntity blockEntity) {
        blockEntity.animationTick++;

        // Spawn de particules dorées pour l'effet visuel (toutes les 10 ticks)
        if (blockEntity.animationTick % 10 == 0) {
            blockEntity.spawnAmbientParticles(level, pos);
        }

        // Reset l'animation tick pour éviter overflow
        if (blockEntity.animationTick > 10000) {
            blockEntity.animationTick = 0;
        }
    }

    /**
     * Met à jour les données du classement depuis le serveur.
     */
    private void updateLeaderboardData() {
        if (level == null || level.isClientSide) return;

        MinecraftServer server = level.getServer();
        if (server == null) return;

        // Récupérer le classement NOCOIN
        List<LeaderboardEntry> newEntries = LeaderboardManager.getLeaderboardByNocoin(server);

        // Limiter aux top entries pour l'affichage
        if (newEntries.size() > MAX_DISPLAY_ENTRIES) {
            newEntries = new ArrayList<>(newEntries.subList(0, MAX_DISPLAY_ENTRIES));
        }

        // Vérifier si les données ont changé
        if (!entriesEqual(entries, newEntries)) {
            entries = newEntries;
            setChanged();
            syncToClient();
        }
    }

    /**
     * Vérifie si deux listes d'entrées sont identiques.
     */
    private boolean entriesEqual(List<LeaderboardEntry> list1, List<LeaderboardEntry> list2) {
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            LeaderboardEntry e1 = list1.get(i);
            LeaderboardEntry e2 = list2.get(i);
            if (!e1.getPlayerName().equals(e2.getPlayerName()) ||
                e1.getNocoinBalance() != e2.getNocoinBalance() ||
                e1.getRank() != e2.getRank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Génère des particules ambiantes autour du leaderboard.
     */
    private void spawnAmbientParticles(Level level, BlockPos pos) {
        // Particules dorées scintillantes autour du panneau
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.5;
        double y = pos.getY() + 1.0 + random.nextDouble() * 1.5;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.5;

        // Utiliser des particules d'enchantement ou de poussière dorée
        if (random.nextFloat() < 0.3f) {
            level.addParticle(
                    ParticleTypes.END_ROD,
                    x, y, z,
                    (random.nextDouble() - 0.5) * 0.02,
                    random.nextDouble() * 0.02,
                    (random.nextDouble() - 0.5) * 0.02
            );
        }
    }

    // ============== Sauvegarde NBT ==============

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putString(TAG_TYPE, currentType.name());

        ListTag entriesList = new ListTag();
        for (LeaderboardEntry entry : entries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("name", entry.getPlayerName());
            entryTag.putLong("balance", entry.getNocoinBalance());
            entryTag.putInt("rank", entry.getRank());
            entriesList.add(entryTag);
        }
        tag.put(TAG_ENTRIES, entriesList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains(TAG_TYPE)) {
            try {
                currentType = LeaderboardManager.LeaderboardType.valueOf(tag.getString(TAG_TYPE));
            } catch (IllegalArgumentException e) {
                currentType = LeaderboardManager.LeaderboardType.NOCOIN;
            }
        }

        entries.clear();
        if (tag.contains(TAG_ENTRIES)) {
            ListTag entriesList = tag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
            for (int i = 0; i < entriesList.size(); i++) {
                CompoundTag entryTag = entriesList.getCompound(i);
                entries.add(new LeaderboardEntry(
                        entryTag.getString("name"),
                        entryTag.getLong("balance"),
                        entryTag.getInt("rank")
                ));
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
