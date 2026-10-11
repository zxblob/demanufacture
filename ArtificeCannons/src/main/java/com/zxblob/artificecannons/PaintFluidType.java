package com.zxblob.artificecannons;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

/** Water-looking fluid tinted with the paint colour. */
public class PaintFluidType extends FluidType {
    private final int tint;

    public PaintFluidType(int argbTint) {
        super(FluidType.Properties.create().density(1000).viscosity(1000).canSwim(false).canDrown(false).canExtinguish(false));
        this.tint = argbTint;
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return ResourceLocation.withDefaultNamespace("block/water_still"); }
            @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.withDefaultNamespace("block/water_flow"); }
            @Override public int getTintColor() { return tint; }
        });
    }
}
