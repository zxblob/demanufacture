package com.zxblob.demanufacture.cooking;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The dish a minigame serves. Carries Kaleidoscope Cookery's QUALITY component (when KC is
 * present) and applies it the same way KC's own food does: scaled food values and effects, plus
 * the quality line in the tooltip.
 */
public class CookedDishItem extends Item
{
    public CookedDishItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity)
    {
        FoodProperties base = super.getFoodProperties(stack, entity);
        if (base == null)
            return null;
        return KcQuality.modifyFood(base, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        super.appendHoverText(stack, context, tooltip, flag);
        KcQuality.appendTooltip(stack, tooltip);
    }
}
