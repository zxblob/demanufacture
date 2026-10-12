package com.zxblob.artificecannons.electricity;

import com.zxblob.artificecannons.ArtificeCannons;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shoots a lightning bolt that splits its damage between nearby enemies. Costs {@link #COST} FE per shot, drawn from
 * the stack's own charge; charge it with any FE source.
 */
public class ElectroZapperItem extends Item implements CustomArmPoseItem {
    // TUNE: capacity, cost per shot and charge rate, all in FE
    public static final int CAPACITY = 20_000;
    public static final int COST = 500;
    public static final int MAX_CHARGE_RATE = 2_000;
    private static final int BAR_COLOR = 0x5CC8FF;

    public ElectroZapperItem(Properties properties) {
        super(properties);
    }

    private static int stored(ItemStack stack) {
        return stack.getOrDefault(ZapperContent.ENERGY.get(), 0);
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13.0F * stored(stack) / CAPACITY); }
    @Override public int getBarColor(ItemStack stack) { return BAR_COLOR; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean creative = player.getAbilities().instabuild;
        if (!creative && stored(stack) < COST) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("item.artifice_cannons.zapper_tool.empty").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!creative) stack.set(ZapperContent.ENERGY.get(), stored(stack) - COST);

        Vec3 barrel = ShootableGadgetItemMethods.getGunBarrelVec(player, hand == InteractionHand.MAIN_HAND, new Vec3(0.25, -0.15F, 1.0));
        Vec3 correction = ShootableGadgetItemMethods.getGunBarrelVec(player, hand == InteractionHand.MAIN_HAND, Vec3.ZERO)
                .subtract(player.position().add(0.0, player.getEyeHeight(), 0.0));
        Vec3 motion = player.getLookAngle().add(correction).normalize().scale(4.0);
        ZapProjectileEntity bolt = ZapProjectileEntity.create(level, barrel, motion, 1.0F);
        bolt.setOwner(player);
        level.addFreshEntity(bolt);
        level.playSound(null, barrel.x, barrel.y, barrel.z, ArtificeCannons.ZAPPER_SHOOT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        ShootableGadgetItemMethods.applyCooldown(player, stack, hand, s -> s.getItem() instanceof ElectroZapperItem, 10);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.artifice_cannons.zapper_tool.summary").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(Mth.floor(100.0 * stored(stack) / CAPACITY) + "% (" + stored(stack) + " / " + CAPACITY + " FE)")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.artifice_cannons.zapper_tool.cost", COST).withStyle(ChatFormatting.DARK_GREEN));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Nullable
    @Override
    public ArmPose getArmPose(ItemStack stack, AbstractClientPlayer player, InteractionHand hand) {
        return !player.swinging ? ArmPose.CROSSBOW_HOLD : null;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || newStack.getItem() != oldStack.getItem();
    }
}
