package com.zxblob.artificecannons;

import io.redspace.irons_artifice.api.GunAboutToShootEvent;
import io.redspace.irons_artifice.api.GunShootEvent;
import io.redspace.irons_artifice.item.FireDelayState;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Burst fire for the Advanced cannon. IANA only has SEMI and AUTO, so the gun is SEMI with a short fire delay
 * (the gap between shots in a burst); one trigger pull fires the first shot, then this class fires the rest on the
 * server and adds a longer cooldown after the last one. The player cannot interrupt a burst.
 */
final class BurstBehaviour {
    static final int BURST_SIZE = 3;
    /** Ticks between shots inside a burst (the gun's own fire delay). */
    static final int SHOT_INTERVAL_TICKS = 4;
    /** Extra ticks of lockout after the last shot of a burst (on top of the normal fire delay). */
    static final int BURST_COOLDOWN_TICKS = 8;

    private static final Map<UUID, Integer> REMAINING = new HashMap<>();
    private static boolean followUp = false;
    /** Client only: game time until which the local player's trigger is locked (the server fires the rest of the burst). */
    private static long clientLockUntil = 0;

    static void register() {
        NeoForge.EVENT_BUS.addListener(BurstBehaviour::onShot);
        NeoForge.EVENT_BUS.addListener(BurstBehaviour::onAboutToShoot);
        NeoForge.EVENT_BUS.addListener(BurstBehaviour::onPlayerTick);
    }

    private static boolean isBurstGun(ItemStack stack) {
        return stack.is(ArtificeCannons.ADVANCED_POTATO_CANNON.get());
    }

    private static void onShot(GunShootEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isBurstGun(event.getShotProfile().itemStack())) return;
        UUID id = player.getUUID();
        if (!followUp) {
            REMAINING.put(id, BURST_SIZE - 1);
            return;
        }
        int left = REMAINING.getOrDefault(id, 0) - 1;
        if (left <= 0) {
            REMAINING.remove(id);
            ItemStack stack = event.getShotProfile().itemStack();
            FireDelayState.start(player, stack, event.getShotProfile().fireDelayTicks() + BURST_COOLDOWN_TICKS, 1.0F);
        } else {
            REMAINING.put(id, left);
        }
    }

    /**
     * A pull during a burst is ignored. This must be decided on BOTH sides: IANA's client runs its own tryFire and
     * applies camera recoil and sends a packet when it thinks it fired, so a server-only veto leaves a phantom shot
     * (recoil, no sound, no bullet). The client cannot see the server's burst counter, so it uses a time window
     * that mirrors the burst: (shots - 1) intervals, then the last shot's delay plus the cooldown.
     */
    private static void onAboutToShoot(GunAboutToShootEvent event) {
        if (!isBurstGun(event.getShotProfile().itemStack())) return;
        if (event.getEntity().level().isClientSide()) {
            long now = event.getEntity().level().getGameTime();
            if (now < clientLockUntil) {
                event.setCanceled(true);
            } else {
                clientLockUntil = now + (long) (BURST_SIZE - 1) * SHOT_INTERVAL_TICKS + SHOT_INTERVAL_TICKS + BURST_COOLDOWN_TICKS;
            }
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!followUp && REMAINING.getOrDefault(player.getUUID(), 0) > 0) {
            event.setCanceled(true);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Integer left = REMAINING.get(player.getUUID());
        if (left == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!isBurstGun(stack) || GunItem.isReloading(stack) || GunItem.getMagazine(stack).count() <= 0) {
            REMAINING.remove(player.getUUID());
            return;
        }
        if (FireDelayState.isActive(player, stack)) return;
        followUp = true;
        try {
            GunplayManager.tryFire(player, player.getLookAngle());
        } finally {
            followUp = false;
        }
    }

    private BurstBehaviour() {}
}
