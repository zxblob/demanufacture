package com.zxblob.demanufacture.cooking;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * What counts as "a liquid" for the Liquid Flask panel. v1 recognizes vanilla water (bucket or
 * bottled) and KC's Oil item only - wine (kaleidoscope_world_liquor) is deliberately left out
 * until that's taken on as a real dependency rather than guessed at.
 */
public class LiquidItems
{
    public static boolean isLiquidItem(ItemStack stack)
    {
        return isWater(stack) || isOil(stack);
    }

    public static boolean isWater(ItemStack stack)
    {
        return isWaterBucket(stack) || isWaterBottle(stack);
    }

    public static boolean isWaterBucket(ItemStack stack)
    {
        return stack.is(Items.WATER_BUCKET);
    }

    public static boolean isWaterBottle(ItemStack stack)
    {
        if (!stack.is(Items.POTION))
            return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }

    public static boolean isOil(ItemStack stack)
    {
        return stack.is(com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems.OIL.get());
    }
}
