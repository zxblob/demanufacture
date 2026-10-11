package com.zxblob.artificecannons.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.zxblob.artificecannons.ArtificeCannons;
import com.zxblob.artificecannons.Metals;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Colour ramps for the metals, read from the pack's own item textures: the ingot (else raw material, else ore) tagged
 * {@code c:ingots/<metal>} is found, its sprite's pixels are sorted by brightness, and the shadow / base / highlight
 * colours are averaged from the dark, middle and bright parts (ignoring the outline). So zinc looks like the zinc
 * ingot, whichever mod adds it. {@code assets/artifice_cannons/metal_colors.json} can override a metal's base colour;
 * unknown textures fall back to the {@link Metals} table.
 */
public final class MetalPalette {
    private static final Map<String, int[]> CACHE = new HashMap<>();
    private static Map<String, Integer> overrides;

    public static void clear() {
        CACHE.clear();
        overrides = null;
    }

    public static int[] rampOf(String metal) {
        int[] cached = CACHE.get(metal);
        if (cached != null) return cached;
        int[] ramp = null;
        Integer over = overrides().get(metal);
        if (over != null) {
            ramp = PaletteRecolor.ramp(over);
        } else {
            try {
                ramp = fromItemTexture(metal);
            } catch (Exception ex) {
                LogUtils.getLogger().warn("Could not sample the {} texture", metal, ex);
            }
        }
        if (ramp == null) ramp = PaletteRecolor.ramp(Metals.COLORS.getOrDefault(metal, 0xB0B0B0));
        CACHE.put(metal, ramp);
        return ramp;
    }

    @Nullable
    private static int[] fromItemTexture(String metal) {
        for (String kind : new String[]{"ingots", "raw_materials", "ores"}) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", kind + "/" + metal));
            var named = BuiltInRegistries.ITEM.getTag(tag);
            if (named.isEmpty()) continue;
            for (var holder : named.get()) {
                var sprite = Minecraft.getInstance().getItemRenderer()
                        .getModel(new ItemStack(holder.value()), null, null, 0).getParticleIcon(ModelData.EMPTY);
                var img = sprite.contents().getOriginalImage();
                if (img == null) continue;
                int w = sprite.contents().width(), h = Math.min(img.getHeight(), sprite.contents().height());
                int[] px = new int[w * h];
                int n = 0;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int p = img.getPixelRGBA(x, y); // ABGR
                        if ((p >>> 24) < 128) continue;
                        px[n++] = ((p & 255) << 16) | (p & 0xFF00) | ((p >> 16) & 255);
                    }
                }
                if (n < 8) continue;
                Integer[] sorted = new Integer[n];
                for (int i = 0; i < n; i++) sorted[i] = px[i];
                Arrays.sort(sorted, (a, b) -> Double.compare(luma(a), luma(b)));
                int from = (int) (n * 0.12); // outline pixels are the darkest
                int m = n - from;
                return new int[]{
                        mean(sorted, from, from + (int) (m * 0.20)),
                        mean(sorted, from + (int) (m * 0.40), from + (int) (m * 0.60)),
                        mean(sorted, from + (int) (m * 0.85), n)};
            }
        }
        return null;
    }

    private static int mean(Integer[] s, int from, int to) {
        to = Math.max(to, from + 1);
        long r = 0, g = 0, b = 0;
        int c = 0;
        for (int i = from; i < Math.min(to, s.length); i++) {
            r += (s[i] >> 16) & 255; g += (s[i] >> 8) & 255; b += s[i] & 255;
            c++;
        }
        if (c == 0) return 0;
        return ((int) (r / c) << 16) | ((int) (g / c) << 8) | (int) (b / c);
    }

    private static double luma(int p) {
        return 0.299 * ((p >> 16) & 255) + 0.587 * ((p >> 8) & 255) + 0.114 * (p & 255);
    }

    private static Map<String, Integer> overrides() {
        if (overrides == null) {
            Map<String, Integer> map = new HashMap<>();
            try {
                var res = Minecraft.getInstance().getResourceManager()
                        .getResource(ResourceLocation.fromNamespaceAndPath(ArtificeCannons.MOD_ID, "metal_colors.json"));
                if (res.isPresent()) {
                    try (var reader = res.get().openAsReader()) {
                        JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                        for (var e : o.entrySet()) {
                            if (e.getKey().startsWith("_")) continue;
                            map.put(e.getKey(), Integer.parseInt(e.getValue().getAsString().replace("#", ""), 16));
                        }
                    }
                }
            } catch (Exception ex) {
                LogUtils.getLogger().warn("Could not read metal_colors.json", ex);
            }
            overrides = map;
        }
        return overrides;
    }

    private MetalPalette() {}
}
