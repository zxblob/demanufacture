package com.zxblob.demanufacture.cooking;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import vectorwing.farmersdelight.common.block.entity.Basket;

/**
 * Finds every FD Basket in range of a cook block and flattens their contents into one list,
 * per the "merge them all" basket-UI design. Each entry remembers exactly which basket/slot it
 * came from so a later "take this item" request can act on the real inventory, not a copy.
 * <p>
 * Must be called with a server-side Level. Container block entities like Basket never push
 * their contents to nearby clients via the generic block-entity sync packet (only the menu-sync
 * protocol or the chunk's initial load does that), so a client-side scan would see stale data.
 */
public class BasketScanner
{
    public static final int SCAN_RADIUS = 6;

    public record BasketEntry(BlockPos basketPos, int slot, ItemStack stack) {}

    public static List<BasketEntry> findNearbyBaskets(Level level, BlockPos center, int radius)
    {
        List<BasketEntry> entries = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius)))
        {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Basket basket)
            {
                BlockPos immutablePos = pos.immutable();
                for (int slot = 0; slot < basket.getContainerSize(); slot++)
                {
                    ItemStack stack = basket.getItem(slot);
                    if (!stack.isEmpty())
                        entries.add(new BasketEntry(immutablePos, slot, stack));
                }
            }
        }
        return entries;
    }
}
