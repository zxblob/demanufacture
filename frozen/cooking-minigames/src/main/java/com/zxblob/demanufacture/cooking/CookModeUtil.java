package com.zxblob.demanufacture.cooking;

import com.zxblob.demanufacture.Demanufacture;
import com.zxblob.demanufacture.network.ToggleCookModePayload;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Shared by CookbookItem (held) and CookbookBlock (placed) - same toggle, two entry points. */
public class CookModeUtil
{
    public static void handleToggleCookMode(ToggleCookModePayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player)
            toggle(player.level(), player);
    }

    public static void toggle(Level level, Player player)
    {
        boolean newState = !player.getData(Demanufacture.COOK_MODE);
        player.setData(Demanufacture.COOK_MODE, newState);
        player.syncData(Demanufacture.COOK_MODE);

        if (!level.isClientSide())
        {
            player.displayClientMessage(Component.translatable(newState
                    ? "message.demanufacture.cook_mode.on"
                    : "message.demanufacture.cook_mode.off"), true);
        }
    }
}
