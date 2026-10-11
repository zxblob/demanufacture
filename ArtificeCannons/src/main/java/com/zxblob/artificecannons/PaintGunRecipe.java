package com.zxblob.artificecannons;

import io.redspace.irons_artifice.item.GunItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * One gun + a dye and / or a metal item (ingot, raw ore, ore block) -> the same gun (all components kept) with the
 * accent colour and / or metal plating set. Works for every IANA gun.
 */
public class PaintGunRecipe extends CustomRecipe {
    public PaintGunRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int guns = 0, dyes = 0, metals = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof GunItem) guns++;
            else if (s.getItem() instanceof DyeItem) dyes++;
            else if (Metals.metalOf(s) != null) metals++;
            else return false;
        }
        return guns == 1 && dyes <= 1 && metals <= 1 && dyes + metals >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack gun = ItemStack.EMPTY;
        DyeItem dye = null;
        String metal = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.getItem() instanceof GunItem) gun = s;
            else if (s.getItem() instanceof DyeItem d) dye = d;
            else if (!s.isEmpty()) metal = Metals.metalOf(s);
        }
        if (gun.isEmpty() || (dye == null && metal == null)) return ItemStack.EMPTY;
        ItemStack out = gun.copyWithCount(1);
        if (dye != null) out.set(Paint.PAINT.get(), dye.getDyeColor().getName());
        if (metal != null) out.set(Paint.METAL.get(), metal);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Paint.PAINT_RECIPE.get();
    }
}
