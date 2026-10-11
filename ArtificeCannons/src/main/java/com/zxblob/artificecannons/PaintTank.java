package com.zxblob.artificecannons;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Makes a gun "fillable" with paint. Never stores fluid: filling just sets the paint (dye) or metal component on the stack.
 * Create's Spout finds this through the item fluid capability and applies it in place (so the magazine and
 * installed modifiers are kept).
 */
public class PaintTank implements IFluidHandlerItem {
    private final ItemStack container;

    public PaintTank(ItemStack container) {
        this.container = container;
    }

    @Override public ItemStack getContainer() { return container; }
    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) { return FluidStack.EMPTY; }
    @Override public int getTankCapacity(int tank) { return Paint.PAINT_MB; }

    private static String keyOf(java.util.Map<String, net.neoforged.neoforge.registries.DeferredHolder<Fluid, Fluid>> fluids, Fluid fluid) {
        for (var e : fluids.entrySet()) {
            if (e.getValue().get() == fluid) return e.getKey();
        }
        return null;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        Fluid f = stack.getFluid();
        return keyOf(Paint.PAINT_FLUIDS, f) != null || keyOf(Paint.METAL_FLUIDS, f) != null;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.getAmount() < Paint.PAINT_MB) return 0;
        String dye = keyOf(Paint.PAINT_FLUIDS, resource.getFluid());
        if (dye != null) {
            if (action.execute()) container.set(Paint.PAINT.get(), dye);
            return Paint.PAINT_MB;
        }
        String metal = keyOf(Paint.METAL_FLUIDS, resource.getFluid());
        if (metal != null) {
            if (action.execute()) container.set(Paint.METAL.get(), metal);
            return Paint.PAINT_MB;
        }
        return 0;
    }

    @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
    @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
}
