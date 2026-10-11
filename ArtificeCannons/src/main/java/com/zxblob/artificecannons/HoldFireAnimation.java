package com.zxblob.artificecannons;

import io.redspace.irons_artifice.api.GunShootEvent;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.network.packets.ClientboundGunAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * While the Flamethrower fires automatically, hold its "fire" animation at {@link #HOLD_SECONDS}; when the player
 * stops, let it play on to the end. IANA restarts "fire" on every shot, so after each shot we re-send IANA's own
 * animation packet with a near-zero playback speed starting at the hold time (a speed of exactly 0 would snap to
 * the first frame), and a normal-speed one once the trigger is released.
 */
final class HoldFireAnimation {
    static final double HOLD_SECONDS = 0.13;
    /** Playback speed used to freeze the pose: slow enough that it does not drift in any realistic burst. */
    private static final double FROZEN_SPEED = 0.001;
    /** Ticks without a shot after which the trigger counts as released (fire delay is 2). */
    private static final int RELEASE_AFTER_TICKS = 4;

    private static final Set<UUID> SHOT_THIS_TICK = new HashSet<>();
    private static final Map<UUID, Long> HOLDING_SINCE_LAST_SHOT = new HashMap<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener(HoldFireAnimation::onShot);
        NeoForge.EVENT_BUS.addListener(HoldFireAnimation::onPlayerTick);
    }

    private static void onShot(GunShootEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getShotProfile().itemStack().is(ArtificeCannons.FLAMETHROWER.get())) {
            SHOT_THIS_TICK.add(player.getUUID());
        }
    }

    private static void send(ServerPlayer player, ItemStack stack, double speed) {
        ClientboundGunAnimationPacket packet = new ClientboundGunAnimationPacket(player.getId(),
                GeoItem.getOrAssignId(stack, (ServerLevel) player.level()), InteractionHand.MAIN_HAND, "fire", speed, HOLD_SECONDS);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, packet);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ArtificeCannons.FLAMETHROWER.get())) {
            SHOT_THIS_TICK.remove(id);
            HOLDING_SINCE_LAST_SHOT.remove(id);
            return;
        }
        long now = player.level().getGameTime();
        if (SHOT_THIS_TICK.remove(id)) {
            send(player, stack, FROZEN_SPEED);
            HOLDING_SINCE_LAST_SHOT.put(id, now);
            return;
        }
        Long last = HOLDING_SINCE_LAST_SHOT.get(id);
        if (last != null && now - last >= RELEASE_AFTER_TICKS) {
            HOLDING_SINCE_LAST_SHOT.remove(id);
            if (!GunItem.isReloading(stack)) send(player, stack, 1.0);
        }
    }

    private HoldFireAnimation() {}
}
