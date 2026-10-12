package com.zxblob.artificecannons.electricity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** FE storage of an Electro-Zapper stack, kept in its {@code artifice_cannons:energy} component. */
public class ZapperEnergy implements IEnergyStorage {
    private final ItemStack stack;

    public ZapperEnergy(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int stored = getEnergyStored();
        int accepted = Math.max(0, Math.min(maxReceive, Math.min(ElectroZapperItem.CAPACITY - stored, ElectroZapperItem.MAX_CHARGE_RATE)));
        if (!simulate && accepted > 0) stack.set(ZapperContent.ENERGY.get(), stored + accepted);
        return accepted;
    }

    @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
    @Override public int getEnergyStored() { return stack.getOrDefault(ZapperContent.ENERGY.get(), 0); }
    @Override public int getMaxEnergyStored() { return ElectroZapperItem.CAPACITY; }
    @Override public boolean canExtract() { return false; }
    @Override public boolean canReceive() { return true; }
}
