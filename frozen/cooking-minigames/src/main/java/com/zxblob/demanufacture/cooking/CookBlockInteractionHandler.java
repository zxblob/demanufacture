package com.zxblob.demanufacture.cooking;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.zxblob.demanufacture.Demanufacture;
import com.zxblob.demanufacture.network.OpenScramblingScreenPayload;
import com.zxblob.demanufacture.network.OpenStirringScreenPayload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Intercepts right-clicks on supported cook blocks while cook mode is active, replacing their
 * vanilla menu with one of our minigames. Each minigame gets its own block set and payload type -
 * the interception/basket-scan plumbing below doesn't change per minigame, only which payload
 * (and therefore which screen) gets opened for a given block.
 */
@EventBusSubscriber(modid = Demanufacture.MODID)
public class CookBlockInteractionHandler
{
    private static final Set<Supplier<Block>> STIRRING_BLOCKS = Set.of(
            () -> vectorwing.farmersdelight.common.registry.ModBlocks.COOKING_POT.get(),
            () -> com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks.STOCKPOT.get()
    );

    private static final Set<Supplier<Block>> SCRAMBLING_BLOCKS = Set.of(
            () -> vectorwing.farmersdelight.common.registry.ModBlocks.SKILLET.get()
    );

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (!event.getEntity().getData(Demanufacture.COOK_MODE))
            return;

        Block clicked = event.getLevel().getBlockState(event.getPos()).getBlock();
        boolean isStirring = STIRRING_BLOCKS.stream().anyMatch(supplier -> supplier.get() == clicked);
        boolean isScrambling = !isStirring && SCRAMBLING_BLOCKS.stream().anyMatch(supplier -> supplier.get() == clicked);
        if (!isStirring && !isScrambling)
            return;

        event.setCanceled(true);

        // Basket contents can only be read reliably server-side (see OpenStirringScreenPayload's
        // javadoc for why) - so the server does the scan and tells the client what it found,
        // rather than the client opening the screen and scanning its own, possibly-stale level.
        if (event.getSide() == LogicalSide.SERVER && event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            List<BasketScanner.BasketEntry> basketItems = BasketScanner.findNearbyBaskets(event.getLevel(), event.getPos(), BasketScanner.SCAN_RADIUS);
            if (isStirring)
                CookStand.moveToStand(serverPlayer, event.getPos());
            if (isStirring)
                PacketDistributor.sendToPlayer(serverPlayer, new OpenStirringScreenPayload(event.getPos(), basketItems));
            else
                PacketDistributor.sendToPlayer(serverPlayer, new OpenScramblingScreenPayload(event.getPos(), basketItems));
        }
    }
}
