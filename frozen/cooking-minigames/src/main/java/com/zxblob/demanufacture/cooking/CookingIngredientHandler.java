package com.zxblob.demanufacture.cooking;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.zxblob.demanufacture.Demanufacture;
import com.zxblob.demanufacture.network.FinishCookingPayload;
import com.zxblob.demanufacture.network.RuinPotPayload;
import com.zxblob.demanufacture.network.ServePotPayload;
import com.zxblob.demanufacture.network.SetPotLiquidPayload;
import com.zxblob.demanufacture.network.TakeBasketItemPayload;
import com.zxblob.demanufacture.network.TakeIngredientOutPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import vectorwing.farmersdelight.common.block.entity.Basket;

/**
 * Server-side handling for all the cooking-progress payloads. Everything here is validated
 * independently of what the client claims - cook mode and reach always; "is this pot actually
 * done" is NOT independently re-timed server-side (see FinishCookingPayload's javadoc) since
 * doneness now depends on the player's live stirring pace, which only the client observes.
 */
public class CookingIngredientHandler
{
    private static final double MAX_REACH_SQ = 10 * 10;
    private static final int MAX_INGREDIENTS = 6;

    public static void handleTakeBasketItem(TakeBasketItemPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player) || !player.getData(Demanufacture.COOK_MODE))
            return;
        if (!withinReach(player, payload.basketPos()) || !withinReach(player, payload.potPos()))
            return;

        Level level = player.level();
        BlockEntity basketBe = level.getBlockEntity(payload.basketPos());
        BlockEntity potBe = level.getBlockEntity(payload.potPos());
        if (!(basketBe instanceof Basket basket) || potBe == null)
            return;

        List<ItemStack> current = potBe.getData(Demanufacture.POT_INGREDIENTS);
        if (current.size() >= MAX_INGREDIENTS)
            return;

        ItemStack taken = basket.removeItem(payload.slot(), 1);
        if (taken.isEmpty())
            return;

        List<ItemStack> updated = new ArrayList<>(current);
        updated.add(taken);
        potBe.setData(Demanufacture.POT_INGREDIENTS, List.copyOf(updated));
        potBe.syncData(Demanufacture.POT_INGREDIENTS);
    }

    public static void handleSetPotLiquid(SetPotLiquidPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player) || !player.getData(Demanufacture.COOK_MODE))
            return;
        if (!withinReach(player, payload.basketPos()) || !withinReach(player, payload.potPos()))
            return;

        Level level = player.level();
        BlockEntity basketBe = level.getBlockEntity(payload.basketPos());
        BlockEntity potBe = level.getBlockEntity(payload.potPos());
        if (!(basketBe instanceof Basket basket) || potBe == null)
            return;

        ItemStack candidate = basket.getItem(payload.slot());
        if (!LiquidItems.isLiquidItem(candidate))
            return;

        boolean wasWaterBucket = LiquidItems.isWaterBucket(candidate);
        ItemStack taken = basket.removeItem(payload.slot(), 1);
        if (taken.isEmpty())
            return;

        // Courtesy: a water bucket shouldn't just vanish - hand the empty bucket back.
        if (wasWaterBucket)
            basket.setItem(payload.slot(), new ItemStack(Items.BUCKET));

        potBe.setData(Demanufacture.POT_LIQUID, taken);
        potBe.syncData(Demanufacture.POT_LIQUID);
    }

    public static void handleFinishCooking(FinishCookingPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player) || !player.getData(Demanufacture.COOK_MODE))
            return;
        if (!withinReach(player, payload.potPos()))
            return;

        Level level = player.level();
        BlockEntity potBe = level.getBlockEntity(payload.potPos());
        if (potBe == null)
            return;

        List<ItemStack> ingredients = potBe.getData(Demanufacture.POT_INGREDIENTS);
        List<ItemStack> cooked = new ArrayList<>(ingredients.size());
        boolean anyChanged = false;
        for (ItemStack stack : ingredients)
        {
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
            if (recipe.isPresent())
            {
                cooked.add(recipe.get().value().getResultItem(level.registryAccess()).copy());
                anyChanged = true;
            }
            else
            {
                cooked.add(stack);
            }
        }

        if (anyChanged)
        {
            potBe.setData(Demanufacture.POT_INGREDIENTS, List.copyOf(cooked));
            potBe.syncData(Demanufacture.POT_INGREDIENTS);
        }
    }

    public static void handleTakeIngredientOut(TakeIngredientOutPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player) || !player.getData(Demanufacture.COOK_MODE))
            return;
        if (!withinReach(player, payload.potPos()))
            return;

        Level level = player.level();
        BlockEntity potBe = level.getBlockEntity(payload.potPos());
        if (potBe == null)
            return;

        List<ItemStack> current = potBe.getData(Demanufacture.POT_INGREDIENTS);
        if (payload.index() < 0 || payload.index() >= current.size())
            return;

        List<ItemStack> updated = new ArrayList<>(current);
        ItemStack removed = updated.remove(payload.index());
        potBe.setData(Demanufacture.POT_INGREDIENTS, List.copyOf(updated));
        potBe.syncData(Demanufacture.POT_INGREDIENTS);

        if (!player.getInventory().add(removed))
            player.drop(removed, false);
    }

    public static void handleRuinPot(RuinPotPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player) || !player.getData(Demanufacture.COOK_MODE))
            return;
        if (!withinReach(player, payload.potPos()))
            return;

        BlockEntity potBe = player.level().getBlockEntity(payload.potPos());
        if (potBe == null)
            return;

        potBe.setData(Demanufacture.POT_INGREDIENTS, List.of());
        potBe.syncData(Demanufacture.POT_INGREDIENTS);
    }

    public static void handleServePot(ServePotPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer player) || !player.getData(Demanufacture.COOK_MODE))
            return;
        if (!withinReach(player, payload.potPos()))
            return;

        BlockEntity potBe = player.level().getBlockEntity(payload.potPos());
        if (potBe == null)
            return;

        List<ItemStack> ingredients = potBe.getData(Demanufacture.POT_INGREDIENTS);
        if (ingredients.isEmpty())
            return;
        ItemStack liquid = potBe.getData(Demanufacture.POT_LIQUID);

        Level level = player.level();
        boolean anyRaw = ingredients.stream().anyMatch(stack -> level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).isPresent());
        DishNaming.Quality quality = DishNaming.gradeQuality(anyRaw, payload.paceAtServe());

        potBe.setData(Demanufacture.POT_INGREDIENTS, List.of());
        potBe.syncData(Demanufacture.POT_INGREDIENTS);
        potBe.setData(Demanufacture.POT_LIQUID, ItemStack.EMPTY);
        potBe.syncData(Demanufacture.POT_LIQUID);

        ItemStack dish = new ItemStack(Demanufacture.COOKED_DISH.get());
        dish.set(DataComponents.CUSTOM_NAME, Component.literal(DishNaming.generateName(ingredients, liquid, quality)));
        dish.set(DataComponents.LORE, new ItemLore(DishNaming.generateLore(quality)));
        KcQuality.apply(dish, quality);

        if (!player.getInventory().add(dish.copy()))
            player.drop(dish.copy(), false);

        List<ItemStack> served = new ArrayList<>(player.getData(Demanufacture.SERVED_DISHES));
        served.add(dish);
        player.setData(Demanufacture.SERVED_DISHES, List.copyOf(served));
        player.syncData(Demanufacture.SERVED_DISHES);
    }

    private static boolean withinReach(ServerPlayer player, BlockPos pos)
    {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= MAX_REACH_SQ;
    }
}
