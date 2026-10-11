package com.zxblob.artificecannons;

import com.mojang.serialization.Codec;
import io.redspace.irons_artifice.item.GunItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Gun paintjobs. A gun carries two components: {@code paint} (a DyeColor name) tints its wood / accent parts, and
 * {@code metal} (a key of {@link Metals}) tints the main body. Applied by crafting (gun + dye and / or ore, ingot or raw ore) or by
 * Create's Spout: the gun item exposes a tiny fluid-handler capability that accepts a paint fluid, so Create fills it
 * in place and magazine / modifiers are kept. Paint fluids (one per dye colour, including Dye Depot's extras) are
 * made in a Basin from a dye (or an ore / ingot / raw ore for the metals) plus water. Which cubes are accents is data:
 * {@code assets/artifice_cannons/paint_parts.json}. Rendering is in client/PaintClient.
 */
public final class Paint {
    /** mB of paint a gun takes. */
    public static final int PAINT_MB = 50;

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ArtificeCannons.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ArtificeCannons.MOD_ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ArtificeCannons.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, ArtificeCannons.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> PAINT = COMPONENTS.registerComponentType("paint",
            b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** Metal plating key (see {@link Metals}); tints the gun's main body. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> METAL = COMPONENTS.registerComponentType("metal",
            b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PaintGunRecipe>> PAINT_RECIPE =
            SERIALIZERS.register("paint_gun", () -> new SimpleCraftingRecipeSerializer<>(PaintGunRecipe::new));

    /** colour name -> its paint fluid source (DyeColor.values() includes Dye Depot's colours when it is installed). */
    public static final Map<String, DeferredHolder<Fluid, Fluid>> PAINT_FLUIDS = new LinkedHashMap<>();

    /** metal key -> its plating fluid source. */
    public static final Map<String, DeferredHolder<Fluid, Fluid>> METAL_FLUIDS = new LinkedHashMap<>();

    static {
        for (DyeColor color : DyeColor.values()) {
            PAINT_FLUIDS.put(color.getName(), registerFluid("paint_" + color.getName(), 0xFF000000 | color.getTextureDiffuseColor()));
        }
        Metals.COLORS.forEach((metal, rgb) -> METAL_FLUIDS.put(metal, registerFluid("paint_metal_" + metal, 0xFF000000 | rgb)));
    }

    private static DeferredHolder<Fluid, Fluid> registerFluid(String name, int tint) {
        final DeferredHolder<Fluid, Fluid>[] source = new DeferredHolder[1];
        final DeferredHolder<Fluid, Fluid>[] flowing = new DeferredHolder[1];
        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register(name, () -> new PaintFluidType(tint));
        Supplier<BaseFlowingFluid.Properties> props = () -> new BaseFlowingFluid.Properties(type, source[0], flowing[0]);
        source[0] = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(props.get()));
        flowing[0] = FLUIDS.register(name + "_flowing", () -> new BaseFlowingFluid.Flowing(props.get()));
        return source[0];
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        SERIALIZERS.register(modBus);
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        modBus.addListener(Paint::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof GunItem gun) {
                event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new PaintTank(stack), gun);
            }
        }
    }

    public static DyeColor colorByName(String name) {
        return Arrays.stream(DyeColor.values()).filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }

    private Paint() {}
}
