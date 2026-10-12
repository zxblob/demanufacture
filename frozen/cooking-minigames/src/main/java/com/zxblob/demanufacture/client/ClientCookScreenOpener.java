package com.zxblob.demanufacture.client;

import java.util.List;

import com.zxblob.demanufacture.client.screen.ScramblingMinigameScreen;
import com.zxblob.demanufacture.client.screen.StirringMinigameScreen;
import com.zxblob.demanufacture.cooking.BasketScanner;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Kept separate from the common event handler so the dedicated server's classloader
 * never has a reason to touch anything in net.minecraft.client.
 */
public class ClientCookScreenOpener
{
    public static void openStirring(BlockPos potPos, List<BasketScanner.BasketEntry> basketItems)
    {
        Minecraft.getInstance().setScreen(new StirringMinigameScreen(potPos, basketItems));
    }

    public static void openScrambling(BlockPos potPos, List<BasketScanner.BasketEntry> basketItems)
    {
        Minecraft.getInstance().setScreen(new ScramblingMinigameScreen(potPos, basketItems));
    }
}
