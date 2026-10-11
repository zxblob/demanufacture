package com.zxblob.artificecannons;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metals a gun's main body can be plated with. Any item tagged {@code c:ingots/<metal>}, {@code c:raw_materials/<metal>}
 * or {@code c:ores/<metal>} counts. Colours are multiplied over the model texture, so they run a little bright.
 */
public final class Metals {
    /** metal key -> RGB tint. Keys match the c: tag names. */
    public static final Map<String, Integer> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("iron", 0xD8D8D8);
        COLORS.put("gold", 0xFAD64A);
        COLORS.put("copper", 0xD0734F);
        COLORS.put("netherite", 0x5A545A);
        COLORS.put("zinc", 0xA9B7B5);
        COLORS.put("brass", 0xD6AE4C);
        COLORS.put("lead", 0x6A7288);
        COLORS.put("nickel", 0xBCC6B4);
        COLORS.put("aluminum", 0xDDE2E7);
        COLORS.put("steel", 0x929AA0);
        COLORS.put("bronze", 0xB08040);
        COLORS.put("platinum", 0xDCEBF2);
        COLORS.put("electrum", 0xE6CF6E);
        COLORS.put("constantan", 0xCB8E6E);
        COLORS.put("cast_iron", 0x707070);
        COLORS.put("uranium", 0x78B455);
        COLORS.put("silver", 0xE0E5EB);
        COLORS.put("lithium", 0xC9C9DA);
    }

    private static final String[] KINDS = {"ingots", "raw_materials", "ores"};

    /**
     * The metal an item stands for (ingot, raw ore or ore block), or null. Any {@code c:ingots/<x>},
     * {@code c:raw_materials/<x>} or {@code c:ores/<x>} tag counts, so metals from other mods work too; metals in
     * {@link #COLORS} are preferred when an item carries several.
     */
    @Nullable
    public static String metalOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        String found = null;
        for (var tag : (Iterable<TagKey<Item>>) stack.getTags()::iterator) {
            ResourceLocation id = tag.location();
            if (!id.getNamespace().equals("c")) continue;
            String path = id.getPath();
            for (String kind : KINDS) {
                if (!path.startsWith(kind + "/")) continue;
                String metal = path.substring(kind.length() + 1);
                if (metal.isEmpty() || metal.contains("/")) continue;
                if (COLORS.containsKey(metal)) return metal;
                if (found == null) found = metal;
            }
        }
        return found;
    }

    private Metals() {}
}
