package com.zxblob.demanufacture.client.anim;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.zxblob.demanufacture.Demanufacture;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Wires {@link CookAnimations} up when Player Animator is installed, and adds a test command:
 * /cookanim play fd_cooking_pot.work | stop | list   (view yourself with F5).
 * No Player Animator types appear in this class, so it loads fine without the mod.
 */
public final class CookAnimationBootstrap
{
    private static boolean present()
    {
        return ModList.get().isLoaded("playeranimator");
    }

    /** Start a cooking animation if Player Animator is installed; silently nothing otherwise. */
    public static void playIfPresent(String name)
    {
        if (present())
            CookAnimations.play(name);
    }

    public static void playPhaseIfPresent(String name)
    {
        if (present())
            CookAnimations.playPhase(name);
    }

    public static void setPhaseIfPresent(double turns)
    {
        if (present())
            CookAnimations.setPhase(turns);
    }

    public static void stopIfPresent()
    {
        if (present())
            CookAnimations.stop();
    }

    @EventBusSubscriber(modid = Demanufacture.MODID, value = Dist.CLIENT)
    public static final class ModEvents
    {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event)
        {
            if (present())
                event.enqueueWork(CookAnimations::init);
        }
    }

    @EventBusSubscriber(modid = Demanufacture.MODID, value = Dist.CLIENT)
    public static final class GameEvents
    {
        @SubscribeEvent
        public static void commands(RegisterClientCommandsEvent event)
        {
            if (!present())
                return;
            event.getDispatcher().register(Commands.literal("cookanim")
                    .then(Commands.literal("stop").executes(ctx ->
                    {
                        CookAnimations.stop();
                        return 1;
                    }))
                    .then(Commands.literal("list").executes(ctx ->
                    {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                String.join(", ", CookAnimations.available().keySet())), false);
                        return 1;
                    }))
                    .then(Commands.literal("play").then(Commands.argument("name", StringArgumentType.string())
                            .suggests((ctx, builder) ->
                            {
                                CookAnimations.available().keySet().forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx ->
                            {
                                String name = StringArgumentType.getString(ctx, "name");
                                boolean ok = CookAnimations.play(name);
                                if (!ok)
                                    ctx.getSource().sendFailure(Component.literal("No animation '" + name + "' (try /cookanim list)"));
                                return ok ? 1 : 0;
                            }))));
        }
    }
}
