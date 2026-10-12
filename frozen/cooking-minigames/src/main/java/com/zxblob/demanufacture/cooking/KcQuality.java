package com.zxblob.demanufacture.cooking;

import java.util.List;

import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * The only place that touches Kaleidoscope Cookery's quality API, so the rest of the mod never
 * loads KC classes. Every method is a no-op when KC is not installed.
 */
public final class KcQuality
{
    private static Boolean loaded;

    private KcQuality()
    {
    }

    private static boolean present()
    {
        if (loaded == null)
            loaded = ModList.get().isLoaded("kaleidoscope_cookery");
        return loaded;
    }

    /** Our five joke tiers collapse onto KC's four steps (Undercooked and Questionable are both Poor). */
    public static void apply(ItemStack stack, DishNaming.Quality quality)
    {
        if (!present())
            return;
        QualityUtils.setQuality(stack, toKc(quality));
    }

    public static FoodProperties modifyFood(FoodProperties base, ItemStack stack)
    {
        if (!present() || !QualityUtils.hasQuality(stack))
            return base;
        return QualityUtils.modifyFoodProperties(base, QualityUtils.getQuality(stack));
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip)
    {
        if (!present() || !QualityUtils.hasQuality(stack))
            return;
        tooltip.add(QualityUtils.getQuality(stack).getTooltip());
    }

    private static Quality toKc(DishNaming.Quality quality)
    {
        return switch (quality)
        {
            case UNDERCOOKED, MEDIOCRE -> Quality.POOR;
            case DECENT -> Quality.STANDARD;
            case GOOD -> Quality.EXCELLENT;
            case EXCELLENT -> Quality.SUPERB;
        };
    }
}
