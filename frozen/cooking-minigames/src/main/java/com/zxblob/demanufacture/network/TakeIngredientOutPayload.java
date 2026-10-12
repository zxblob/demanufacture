package com.zxblob.demanufacture.network;

import com.zxblob.demanufacture.Demanufacture;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent client -> server when the player clicks an ingredient already in the pot: "give this one
 * back to me." The index is into the pot's current POT_INGREDIENTS list as the server sees it.
 */
public record TakeIngredientOutPayload(BlockPos potPos, int index) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<TakeIngredientOutPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "take_ingredient_out"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TakeIngredientOutPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TakeIngredientOutPayload::potPos,
            ByteBufCodecs.VAR_INT, TakeIngredientOutPayload::index,
            TakeIngredientOutPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
