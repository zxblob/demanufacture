package com.zxblob.demanufacture.client.scene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.zxblob.demanufacture.Demanufacture;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * The Farmer's Delight pot has its spoon baked into the block model. During the cook scene we want
 * our own spoon that turns with the stir, so the pot being cooked at drops its spoon quads and the
 * scene draws {@link #SPOON} instead.
 */
@EventBusSubscriber(modid = Demanufacture.MODID, value = Dist.CLIENT)
public final class SpoonModels
{
    public static final ModelResourceLocation SPOON = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "block/cook_spoon"));

    static final ModelProperty<Boolean> HIDE_SPOON = new ModelProperty<>();

    private SpoonModels()
    {
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event)
    {
        event.register(SPOON);
    }

    @SubscribeEvent
    public static void wrap(ModelEvent.ModifyBakingResult event)
    {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        Map<ModelResourceLocation, BakedModel> replaced = new HashMap<>();
        for (Map.Entry<ModelResourceLocation, BakedModel> e : models.entrySet())
        {
            ResourceLocation id = e.getKey().id();
            if (id.getNamespace().equals("farmersdelight") && id.getPath().equals("cooking_pot")
                    && !e.getKey().getVariant().equals("inventory"))
                replaced.put(e.getKey(), new SpoonlessModel(e.getValue()));
        }
        models.putAll(replaced);
    }

    private static final class SpoonlessModel extends BakedModelWrapper<BakedModel>
    {
        SpoonlessModel(BakedModel original)
        {
            super(original);
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData)
        {
            ModelData base = super.getModelData(level, pos, state, modelData);
            return base.derive().with(HIDE_SPOON, CookScene.isCookingAt(pos)).build();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                        ModelData data, @Nullable RenderType renderType)
        {
            List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
            if (!Boolean.TRUE.equals(data.get(HIDE_SPOON)))
                return quads;
            return quads.stream().filter(q -> !isSpoon(q)).toList();
        }

        /**
         * The spoon is the only geometry that lies entirely in the middle of the pot and above its base.
         * Tilted, it spans x/z within 0.35 of the centre and y from 2.6/16 up; the pot walls and
         * handles sit at 0.375 and the pot body starts at y = 0.
         */
        private static boolean isSpoon(BakedQuad quad)
        {
            int[] v = quad.getVertices();
            for (int i = 0; i < 4; i++)
            {
                float x = Float.intBitsToFloat(v[i * 8]);
                float y = Float.intBitsToFloat(v[i * 8 + 1]);
                float z = Float.intBitsToFloat(v[i * 8 + 2]);
                if (Math.abs(x - 0.5f) > 0.36f || Math.abs(z - 0.5f) > 0.36f || y < 2f / 16f)
                    return false;
            }
            return true;
        }
    }
}
