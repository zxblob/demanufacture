package com.zxblob.demanufacture.network;

import java.util.List;

import com.zxblob.demanufacture.Demanufacture;
import com.zxblob.demanufacture.cooking.BasketScanner;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Sent server -> client in reply to opening a cook block's screen: here's the pot you clicked,
 * and here's what's actually in range in the baskets right now.
 * <p>
 * This exists because basket contents can't just be read off the client's own level: chest-like
 * container block entities (which Basket extends) never push their inventory to nearby clients
 * via the generic block-entity sync packet - the client only sees it via the menu-sync protocol
 * when it opens that specific container, or via the chunk's initial full NBT on load. A client
 * reading a BasketBlockEntity it hasn't opened itself sees stale (usually empty) data. The
 * server's copy is always live, so the server does the scan and hands the client the answer.
 */
public record OpenStirringScreenPayload(BlockPos potPos, List<BasketScanner.BasketEntry> basketItems) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<OpenStirringScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "open_stirring_screen"));

    private static final StreamCodec<RegistryFriendlyByteBuf, BasketScanner.BasketEntry> ENTRY_STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BasketScanner.BasketEntry::basketPos,
            ByteBufCodecs.VAR_INT, BasketScanner.BasketEntry::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, BasketScanner.BasketEntry::stack,
            BasketScanner.BasketEntry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenStirringScreenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenStirringScreenPayload::potPos,
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list()), OpenStirringScreenPayload::basketItems,
            OpenStirringScreenPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
