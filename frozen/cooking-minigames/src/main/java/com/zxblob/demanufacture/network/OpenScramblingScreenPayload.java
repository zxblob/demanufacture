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
 * Same shape and same reason to exist as OpenStirringScreenPayload (see its javadoc) - server
 * does the basket scan, client just gets told what it found. Separate payload rather than a
 * generic "open any cooking screen" one so each minigame's open-event stays simple to follow;
 * revisit if a third minigame makes the duplication actually annoying rather than just present.
 */
public record OpenScramblingScreenPayload(BlockPos potPos, List<BasketScanner.BasketEntry> basketItems) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<OpenScramblingScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "open_scrambling_screen"));

    private static final StreamCodec<RegistryFriendlyByteBuf, BasketScanner.BasketEntry> ENTRY_STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BasketScanner.BasketEntry::basketPos,
            ByteBufCodecs.VAR_INT, BasketScanner.BasketEntry::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, BasketScanner.BasketEntry::stack,
            BasketScanner.BasketEntry::new
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenScramblingScreenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenScramblingScreenPayload::potPos,
            ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list()), OpenScramblingScreenPayload::basketItems,
            OpenScramblingScreenPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
