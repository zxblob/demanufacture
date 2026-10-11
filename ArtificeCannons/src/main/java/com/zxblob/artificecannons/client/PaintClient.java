package com.zxblob.artificecannons.client;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.zxblob.artificecannons.ArtificeCannons;
import com.zxblob.artificecannons.Metals;
import com.zxblob.artificecannons.Paint;
import io.redspace.irons_artifice.client.gun.GunGeoModel;
import io.redspace.irons_artifice.client.gun.GunInHandRenderer;
import io.redspace.irons_artifice.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Tints painted guns (dye on accents, metal on the body), and swaps IANA's gun renderer for one that does so (ours and IANA's own guns). */
@EventBusSubscriber(modid = ArtificeCannons.MOD_ID, value = Dist.CLIENT)
public final class PaintClient {

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) rm -> {
            clearTextures();
            MetalPalette.clear();
        });
    }

    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

    private static void clearTextures() {
        var tm = Minecraft.getInstance().getTextureManager();
        TEXTURES.values().forEach(tm::release);
        TEXTURES.clear();
    }

    /**
     * Same renderer as IANA's, but a painted gun gets a recoloured copy of its texture: {@link PaletteRecolor} reads the
     * model's cubes: wood (dye only), trim (dye, else metal) and metal (metal colour).
     */
    public static class PaintedGunRenderer extends GunInHandRenderer {
        private final String gunId;

        public PaintedGunRenderer(ResourceLocation modelId) {
            super(new GunGeoModel(modelId));
            this.gunId = modelId.toString();
        }

        @Override
        public ResourceLocation getTextureLocation(GunItem animatable) {
            ResourceLocation base = super.getTextureLocation(animatable);
            ItemStack stack = getCurrentItemStack();
            DyeColor dye = paintOf(stack);
            String metal = metalNameOf(stack);
            if (dye == null && metal == null) return base;
            String key = gunId + "|" + (dye == null ? "" : dye.getName()) + "|" + (metal == null ? "" : metal);
            ResourceLocation cached = TEXTURES.get(key);
            if (cached != null) return cached;
            try {
                ResourceLocation made = bake(animatable, base, key, dye, metal);
                TEXTURES.put(key, made);
                return made;
            } catch (Exception ex) {
                LogUtils.getLogger().warn("Could not recolour {}", gunId, ex);
                TEXTURES.put(key, base);
                return base;
            }
        }

        private ResourceLocation bake(GunItem item, ResourceLocation base, String key, @Nullable DyeColor dye, @Nullable String metal) throws Exception {
            NativeImage img;
            try (InputStream in = Minecraft.getInstance().getResourceManager().open(base)) {
                img = NativeImage.read(in);
            }
            int w = img.getWidth(), h = img.getHeight();
            int[] argb = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = img.getPixelRGBA(x, y); // ABGR
                    argb[y * w + x] = (p & 0xFF00FF00) | ((p & 255) << 16) | ((p >> 16) & 255);
                }
            }
            List<PaletteRecolor.Cube> cubes = new ArrayList<>();
            BakedGeoModel model = getGeoModel().getBakedModel(getGeoModel().getModelResource(item));
            for (GeoBone bone : model.topLevelBones()) collectCubes(bone, cubes, w, h);
            int[] out = PaletteRecolor.recolour(argb, w, h, cubes,
                    dye == null ? null : PaletteRecolor.ramp(dye.getTextureDiffuseColor()),
                    metal == null ? null : MetalPalette.rampOf(metal));
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = out[y * w + x];
                    img.setPixelRGBA(x, y, (p & 0xFF00FF00) | ((p & 255) << 16) | ((p >> 16) & 255));
                }
            }
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(ArtificeCannons.MOD_ID, "painted/" + Integer.toHexString(key.hashCode()) + "_" + gunId.replace(':', '_'));
            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(img));
            return loc;
        }

        /** Every cube of the bone tree with the texel rectangles its faces cover. */
        private static void collectCubes(GeoBone bone, List<PaletteRecolor.Cube> out, int w, int h) {
            for (GeoCube cube : bone.getCubes()) {
                List<Integer> rects = new ArrayList<>();
                for (GeoQuad quad : cube.quads()) {
                    if (quad == null) continue;
                    float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
                    for (GeoVertex v : quad.vertices()) {
                        minU = Math.min(minU, v.texU()); maxU = Math.max(maxU, v.texU());
                        minV = Math.min(minV, v.texV()); maxV = Math.max(maxV, v.texV());
                    }
                    rects.add((int) Math.floor(minU * w + 0.01F));
                    rects.add((int) Math.floor(minV * h + 0.01F));
                    rects.add((int) Math.ceil(maxU * w - 0.01F));
                    rects.add((int) Math.ceil(maxV * h - 0.01F));
                }
                out.add(new PaletteRecolor.Cube(bone.getName(), rects.stream().mapToInt(Integer::intValue).toArray()));
            }
            for (GeoBone child : bone.getChildBones()) collectCubes(child, out, w, h);
        }
    }

    @Nullable
    static DyeColor paintOf(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String name = stack.get(Paint.PAINT.get());
        return name == null ? null : Paint.colorByName(name);
    }

    @Nullable
    static String metalNameOf(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String name = stack.get(Paint.METAL.get());
        return name != null && !name.isEmpty() ? name : null;
    }

    /** LOW priority so this runs after IANA's own RegisterRenderers handler and replaces its provider. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof GunItem gun)) continue;
            final ResourceLocation modelId = BuiltInRegistries.ITEM.getKey(gun);
            gun.geoRenderProvider.setValue(new GeoRenderProvider() {
                private final Supplier<PaintedGunRenderer> renderer = Suppliers.memoize(() -> new PaintedGunRenderer(modelId));

                @Override
                public @Nullable BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                    return renderer.get();
                }
            });
        }
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        DyeColor c = paintOf(event.getItemStack());
        if (c != null) {
            event.getToolTip().add(Component.translatable("item.artifice_cannons.painted", Component.translatable("color.minecraft." + c.getName())).withColor(c.getTextColor()));
        }
        String metal = metalNameOf(event.getItemStack());
        if (metal != null) {
            String pretty = java.util.Arrays.stream(metal.split("_")).map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1)).collect(java.util.stream.Collectors.joining(" "));
            event.getToolTip().add(Component.translatable("item.artifice_cannons.plated", Component.translatableWithFallback("metal.artifice_cannons." + metal, pretty)).withColor(MetalPalette.rampOf(metal)[1]));
        }
    }

    private PaintClient() {}
}
