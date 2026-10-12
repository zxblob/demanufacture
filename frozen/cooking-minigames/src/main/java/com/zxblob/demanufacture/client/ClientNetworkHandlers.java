package com.zxblob.demanufacture.client;

import com.zxblob.demanufacture.network.OpenScramblingScreenPayload;
import com.zxblob.demanufacture.network.OpenStirringScreenPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Handlers for server -> client payloads. A plain class (no client-only supertype), so it's
 * safe for the dedicated server to load when payload registration runs on both sides - the
 * method bodies here, which do touch client-only classes, are simply never invoked server-side.
 */
public class ClientNetworkHandlers
{
    public static void handleOpenStirringScreen(OpenStirringScreenPayload payload, IPayloadContext context)
    {
        ClientCookScreenOpener.openStirring(payload.potPos(), payload.basketItems());
    }

    public static void handleOpenScramblingScreen(OpenScramblingScreenPayload payload, IPayloadContext context)
    {
        ClientCookScreenOpener.openScrambling(payload.potPos(), payload.basketItems());
    }
}
