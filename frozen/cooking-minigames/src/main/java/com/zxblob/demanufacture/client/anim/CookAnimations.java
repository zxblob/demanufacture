package com.zxblob.demanufacture.client.anim;

import java.util.Map;

import com.zxblob.demanufacture.Demanufacture;

import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Plays the Blockbench cooking animations (assets/demanufacture/player_animations, produced by
 * tools/export_player_animations.py) on the local player through Player Animator.
 * Only this class touches Player Animator types; callers must check the mod is loaded first.
 */
public final class CookAnimations
{
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, "cook");

    private CookAnimations()
    {
    }

    public static void init()
    {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000, player -> new ModifierLayer<>());
    }

    public static Map<String, IPlayable> available()
    {
        return PlayerAnimationRegistry.getModAnimations(Demanufacture.MODID);
    }

    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> layer()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return null;
        return (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
    }

    /** @return false when there is no such animation or no local player */
    public static boolean play(String name)
    {
        ModifierLayer<IAnimation> layer = layer();
        IPlayable animation = PlayerAnimationRegistry.getAnimation(
                ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, name));
        if (layer == null || animation == null)
            return false;
        layer.setAnimation(animation.playAnimation());
        return true;
    }

    /** Like {@link #play} but the pose follows {@link #setPhase} instead of the clock. */
    public static boolean playPhase(String name)
    {
        ModifierLayer<IAnimation> layer = layer();
        IPlayable animation = PlayerAnimationRegistry.getAnimation(
                ResourceLocation.fromNamespaceAndPath(Demanufacture.MODID, name));
        if (layer == null || !(animation instanceof dev.kosmx.playerAnim.core.data.KeyframeAnimation keyframes))
            return false;
        layer.setAnimation(PhaseAnimation.of(keyframes));
        return true;
    }

    /** Turns: 1.0 is one full pass through the animation. */
    public static void setPhase(double turns)
    {
        PhaseAnimation.phase = turns;
    }

    public static void stop()
    {
        ModifierLayer<IAnimation> layer = layer();
        if (layer != null)
            layer.setAnimation(null);
    }
}
