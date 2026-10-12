package com.zxblob.artificecannons.electricity;

import com.george_vi.electroenergetics.CEERegistries;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import com.zxblob.artificecannons.ArtificeCannons;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registry entries for the Factory Light, an Electro Energetics device (ported from Create: Power Grid). */
public final class FactoryLightContent {
    public static final DeferredRegister<SimulatedDeviceType<?>> DEVICES =
            DeferredRegister.create(CEERegistries.SIMULATED_DEVICE_TYPE, ArtificeCannons.MOD_ID);

    public static final DeferredHolder<SimulatedDeviceType<?>, SimulatedDeviceType<FactoryLightDevice>> DEVICE = DEVICES.register("factory_light",
            () -> new SimulatedDeviceType<>(ResourceLocation.fromNamespaceAndPath(ArtificeCannons.MOD_ID, "factory_light"),
                    (type, level, pos, sd) -> new FactoryLightDevice(level, pos, sd, type)));

    public static final DeferredBlock<Block> FACTORY_LIGHT = ArtificeCannons.BLOCKS.register("factory_light",
            () -> new FactoryLightBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()));

    /** Invisible light block projected below a lit Factory Light. */
    public static final DeferredBlock<Block> BEAM = ArtificeCannons.BLOCKS.register("factory_light_beam",
            () -> new FactoryLightBeamBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)));

    public static final DeferredItem<BlockItem> FACTORY_LIGHT_ITEM = ArtificeCannons.ITEMS.registerSimpleBlockItem("factory_light", FACTORY_LIGHT);

    static void init(IEventBus modBus) {
        DEVICES.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(FACTORY_LIGHT_ITEM.get());
        });
    }

    private FactoryLightContent() {}
}
