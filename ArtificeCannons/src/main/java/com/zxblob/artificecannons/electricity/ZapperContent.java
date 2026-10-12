package com.zxblob.artificecannons.electricity;

import com.mojang.serialization.Codec;
import com.zxblob.artificecannons.ArtificeCannons;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registry entries for the Electro-Zapper (ported from Create: Power Grid, now powered by plain FE). */
public final class ZapperContent {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ArtificeCannons.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ArtificeCannons.MOD_ID);

    /** Stored FE in the zapper stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.registerComponentType("energy",
            b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredItem<Item> ZAPPER = ArtificeCannons.ITEMS.registerItem("zapper_tool", ElectroZapperItem::new,
            new Item.Properties().stacksTo(1));

    public static final DeferredHolder<EntityType<?>, EntityType<ZapProjectileEntity>> ZAP_PROJECTILE = ENTITIES.register("zap_projectile",
            () -> EntityType.Builder.<ZapProjectileEntity>of(ZapProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(1).build("zap_projectile"));

    public static final ResourceKey<DamageType> ZAP_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ArtificeCannons.MOD_ID, "zap"));

    static void init(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener(ZapperContent::registerCapabilities);
        modBus.addListener(ZapperContent::fillCreativeTab);
        if (ElectricityModule.isClient()) {
            modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                    e.registerEntityRenderer(ZAP_PROJECTILE.get(), ZapProjectileRenderer::new));
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> new ZapperEnergy(stack), ZAPPER.get());
    }

    private static void fillCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ZAPPER.get());
            ItemStack charged = new ItemStack(ZAPPER.get());
            charged.set(ENERGY.get(), ElectroZapperItem.CAPACITY);
            event.accept(charged);
        }
    }

    private ZapperContent() {}
}
