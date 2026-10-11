package com.zxblob.demanufacture.network;

import com.zxblob.demanufacture.Demanufacture;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent client -> server when the player clicks an ingredient in the basket UI: "take this item
 * from this basket slot and make it the active ingredient in this pot."
 */
public record TakeBasketItemPayload(BlockPos basketPos, int slot, BlockPos potPos) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<TakeBasketItemPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "take_basket_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TakeBasketItemPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TakeBasketItemPayload::basketPos,
            ByteBufCodecs.VAR_INT, TakeBasketItemPayload::slot,
            BlockPos.STREAM_CODEC, TakeBasketItemPayload::potPos,
            TakeBasketItemPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
