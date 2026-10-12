package com.zxblob.demanufacture.network;

import com.zxblob.demanufacture.Demanufacture;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent client -> server when the player's pace meter bottoms out: the pot's ingredients are
 * ruined. Same trust model as FinishCookingPayload - pace only exists client-side, so the server
 * can't independently re-verify this, just the usual cook-mode/reach checks.
 */
public record RuinPotPayload(BlockPos potPos) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<RuinPotPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "ruin_pot"));

    public static final StreamCodec<ByteBuf, RuinPotPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(RuinPotPayload::new, RuinPotPayload::potPos);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
