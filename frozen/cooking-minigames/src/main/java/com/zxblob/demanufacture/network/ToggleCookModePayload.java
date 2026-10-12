package com.zxblob.demanufacture.network;

import com.zxblob.demanufacture.Demanufacture;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Sent client -> server from the cookbook menu's toggle button - no fields needed. */
public record ToggleCookModePayload() implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ToggleCookModePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "toggle_cook_mode"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, ToggleCookModePayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleCookModePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
