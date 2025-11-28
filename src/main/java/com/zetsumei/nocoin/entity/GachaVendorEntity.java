package com.zetsumei.nocoin.entity;

import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Vendeur Gacha - PNJ statique qui vend des Clés Gacha contre des NOCOIN.
 * Inspiré du Villager mais avec une IA simplifiée.
 */
public class GachaVendorEntity extends PathfinderMob {

    public GachaVendorEntity(EntityType<? extends GachaVendorEntity> entityType, Level level) {
        super(entityType, level);
        this.setCustomName(Component.literal("Vendeur Gacha"));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        // IA basique : reste sur place, regarde les joueurs
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    /**
     * Attributs du vendeur (immortel, ne bouge pas beaucoup).
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)  // Ne bouge pas
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);  // Résiste aux knockbacks
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            // Ouvrir l'interface d'achat côté client
            if (player instanceof ServerPlayer serverPlayer) {
                NocoinNetworkHandler.sendOpenVendorScreenToClient(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Le vendeur est invulnérable (sauf mode créatif ou commandes)
        if (source.getEntity() instanceof Player player) {
            if (!player.isCreative()) {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // Ne disparaît jamais
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        // Toujours persistant
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
    }
}
