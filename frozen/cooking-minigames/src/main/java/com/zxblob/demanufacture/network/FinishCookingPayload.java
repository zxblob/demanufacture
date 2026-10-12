package com.zxblob.demanufacture.network;

import com.zxblob.demanufacture.Demanufacture;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent client -> server when the client's own (locally computed) doneness estimate first hits
 * 100%: "I think this pot is done." Doneness depends on the player's live stirring pace, which
 * only the client observes, so unlike the other cooking payloads the server can't independently
 * re-time this one - it trusts the client's timing here (cook mode and reach are still checked).
 * Fine for a friends' SMP; would need real anti-cheat consideration for anything more adversarial.
 */
public record FinishCookingPayload(BlockPos potPos) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<FinishCookingPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "finish_cooking"));

    public static final StreamCodec<ByteBuf, FinishCookingPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(FinishCookingPayload::new, FinishCookingPayload::potPos);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
