package com.zxblob.demanufacture.cooking;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Both the held toggle and the placeable block - a BlockItem so it keeps its usual "place it"
 * behavior, with the cook-mode toggle layered on top. Since both are right-click actions, the
 * usual vanilla convention breaks the tie: plain right-click toggles cook mode (whether or not
 * you're looking at a block), shift+right-click on a block places it instead.
 */
public class CookbookItem extends BlockItem
{
    public CookbookItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND)
            return InteractionResultHolder.pass(stack);

        CookModeUtil.toggle(level, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Player player = context.getPlayer();
        if (player != null && !player.isShiftKeyDown())
        {
            CookModeUtil.toggle(context.getLevel(), player);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        return super.useOn(context);
    }
}
