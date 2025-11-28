package com.zetsumei.nocoin.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider pour attacher la capability NOCOIN aux entités (joueurs).
 */
public class NocoinCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    
    public static final Capability<INocoinCapability> NOCOIN_CAPABILITY = 
            CapabilityManager.get(new CapabilityToken<>() {});
    
    private INocoinCapability nocoinCap = null;
    private final LazyOptional<INocoinCapability> optional = LazyOptional.of(this::createNocoinCapability);
    
    private INocoinCapability createNocoinCapability() {
        if (this.nocoinCap == null) {
            this.nocoinCap = new NocoinCapability();
        }
        return this.nocoinCap;
    }
    
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == NOCOIN_CAPABILITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }
    
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        createNocoinCapability().saveNBTData(tag);
        return tag;
    }
    
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createNocoinCapability().loadNBTData(nbt);
    }
}
