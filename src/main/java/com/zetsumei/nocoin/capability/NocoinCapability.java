package com.zetsumei.nocoin.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * Implémentation de la capability NOCOIN.
 */
public class NocoinCapability implements INocoinCapability {
    
    private static final String NBT_BALANCE = "nocoin_balance";
    
    private long balance = 0;
    
    @Override
    public long getBalance() {
        return balance;
    }
    
    @Override
    public void setBalance(long amount) {
        this.balance = Math.max(0, amount);
    }
    
    @Override
    public void addBalance(long amount) {
        if (amount > 0) {
            this.balance += amount;
        }
    }
    
    @Override
    public boolean removeBalance(long amount) {
        if (amount > 0 && hasEnough(amount)) {
            this.balance -= amount;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean hasEnough(long amount) {
        return this.balance >= amount;
    }
    
    @Override
    public void copyFrom(INocoinCapability other) {
        this.balance = other.getBalance();
    }
    
    @Override
    public void saveNBTData(CompoundTag tag) {
        tag.putLong(NBT_BALANCE, this.balance);
    }
    
    @Override
    public void loadNBTData(CompoundTag tag) {
        this.balance = tag.getLong(NBT_BALANCE);
    }
}
