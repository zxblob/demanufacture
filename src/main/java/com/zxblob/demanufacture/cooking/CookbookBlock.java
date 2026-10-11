package com.zxblob.demanufacture.cooking;

import com.zxblob.demanufacture.client.ClientCookbookMenuOpener;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A placed cookbook opens a menu (cook-mode toggle for now, the shell for the real book later)
 * rather than instantly toggling - a shared kitchen station someone else might be mid-click on,
 * not something that should flip state the instant you bump into it. Plain full-cube placeholder
 * geometry/texture until there's real art for "a book resting on a surface."
 */
public class CookbookBlock extends Block
{
    public CookbookBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        if (level.isClientSide())
            ClientCookbookMenuOpener.open();
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
