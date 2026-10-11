package com.zxblob.demanufacture;

import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.zxblob.demanufacture.cooking.CookbookBlock;
import com.zxblob.demanufacture.cooking.CookbookItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// The value here must match the modId entry in src/main/templates/META-INF/neoforge.mods.toml
@Mod(Demanufacture.MODID)
public class Demanufacture
{
    public static final String MODID = "demanufacture";

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    // Whether a player currently has cook mode toggled on. Not persisted - resets on login,
    // which is the behaviour we want (nobody should get stuck in cook mode after a crash).
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> COOK_MODE = ATTACHMENT_TYPES.register("cook_mode",
            () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());

    // Whichever ingredients are currently in a given cook block's pot/pan, if any. Lives on
    // the FD/KC block entity itself via the attachment system - we don't own that class, so this
    // is the only way to hang our own state off it without a mixin.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<ItemStack>>> POT_INGREDIENTS = ATTACHMENT_TYPES.register("pot_ingredients",
            () -> AttachmentType.builder(() -> List.<ItemStack>of())
                    .sync(ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    // Whichever single liquid is currently poured into the pot, if any. Replacing it discards
    // whatever was there before - no mixing liquids in v1.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> POT_LIQUID = ATTACHMENT_TYPES.register("pot_liquid",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY).sync(ItemStack.OPTIONAL_STREAM_CODEC).build());

    // TUNE: how long (in seconds of good-pace stirring) a full raw-to-cooked conversion takes.
    // Doneness only accrues while the player is actively stirring well - see
    // StirringMinigameScreen - it isn't a passive real-time timer.
    public static final double COOK_DURATION_SECONDS = 30.0;

    // Full-cube placeholder geometry/texture until there's real art for "a book on a surface".
    public static final DeferredBlock<Block> COOKBOOK_BLOCK = BLOCKS.register("cookbook_block",
            () -> new CookbookBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f)));

    public static final DeferredItem<Item> COOKBOOK = ITEMS.registerItem("cookbook",
            props -> new CookbookItem(COOKBOOK_BLOCK.get(), props), new Item.Properties().stacksTo(1));

    // Placeholder output of "Serve" - a plain edible stand-in until the real dish-output system
    // (FoodState, naming, quality) exists. Deliberately carries no memory of what went into it
    // yet - that's a bigger, separate design job, not something to half-build here.
    public static final DeferredItem<Item> COOKED_DISH = ITEMS.registerItem("cooked_dish", props -> new Item(props),
            new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).build()));

    // Every dish this player has ever served, oldest first. Just a flat log for now (there's only
    // one dish type to log) - becomes the real per-dish discovery record once dishes actually vary.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<ItemStack>>> SERVED_DISHES = ATTACHMENT_TYPES.register("served_dishes",
            () -> AttachmentType.builder(() -> List.<ItemStack>of())
                    .serialize(ItemStack.OPTIONAL_CODEC.listOf())
                    .copyOnDeath()
                    .sync(ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    public Demanufacture(IEventBus modEventBus, ModContainer modContainer)
    {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);

        LOGGER.info("Demanufacture loading");
    }
}
