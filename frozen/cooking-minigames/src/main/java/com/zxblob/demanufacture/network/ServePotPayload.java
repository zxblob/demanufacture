package com.zxblob.demanufacture.network;

import com.zxblob.demanufacture.Demanufacture;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent client -> server when the player clicks "Serve". paceAtServe (0..100) is self-reported by
 * the client, same trust model as FinishCookingPayload - pace only exists client-side. Whether
 * the dish is actually undercooked is re-derived server-side independently (ingredient state is
 * authoritative), so this only affects the quality tier, not whether serving is allowed at all.
 */
public record ServePotPayload(BlockPos potPos, double paceAtServe) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ServePotPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "serve_pot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServePotPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ServePotPayload::potPos,
            ByteBufCodecs.DOUBLE, ServePotPayload::paceAtServe,
            ServePotPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
