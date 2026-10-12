package com.zxblob.demanufacture.network;

import com.zxblob.demanufacture.Demanufacture;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent client -> server when the player clicks a liquid in the Liquid Flask panel: "pour this
 * basket slot's liquid into this pot," replacing whatever liquid (if any) was already in it.
 */
public record SetPotLiquidPayload(BlockPos basketPos, int slot, BlockPos potPos) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<SetPotLiquidPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "set_pot_liquid"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetPotLiquidPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetPotLiquidPayload::basketPos,
            ByteBufCodecs.VAR_INT, SetPotLiquidPayload::slot,
            BlockPos.STREAM_CODEC, SetPotLiquidPayload::potPos,
            SetPotLiquidPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
