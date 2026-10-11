package com.zxblob.artificecannons;

import io.redspace.irons_artifice.api.AmmoEvent;
import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.api.GunShootEvent;
import io.redspace.irons_artifice.item.FireDelayState;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.entity.Bullet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

/** Quad cannon behaviour: 4 rounds per pull, explode where the rockets land. */
final class RocketBehaviour {
    /** Blast strength per rocket. No block damage for now (ExplosionInteraction.NONE); 4 rockets overlap. */
    private static final float BLAST_POWER = 1.3F;

    /** Safety cap: the smoke never outlasts this, even if a reload is never started. */
    private static final int SMOKE_MAX_TICKS = 100;
    private static final Map<UUID, Long> SMOULDER_UNTIL = new HashMap<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener(RocketBehaviour::onShot);
        NeoForge.EVENT_BUS.addListener(RocketBehaviour::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(RocketBehaviour::onAmmoAmount);
        NeoForge.EVENT_BUS.addListener(RocketBehaviour::onImpact);
    }

    /** Approximate positions of the four muzzles in front of the player's view (2x2 block). */
    private static Vec3[] barrels(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        Vec3 base = player.getEyePosition().add(look.scale(1.7)).add(right.scale(0.28)).add(up.scale(-0.22));
        Vec3[] out = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            double dx = (i % 2 == 0 ? -0.07 : 0.07), dy = (i < 2 ? 0.07 : -0.07);
            out[i] = base.add(right.scale(dx)).add(up.scale(dy));
        }
        return out;
    }

    private static void onShot(GunShootEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getShotProfile().itemStack().is(ArtificeCannons.QUAD_POTATO_CANNON.get())) return;
        ServerLevel level = player.serverLevel();
        for (Vec3 b : barrels(player)) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, b.x, b.y, b.z, 4, 0.05, 0.05, 0.05, 0.02);
            level.sendParticles(ParticleTypes.CLOUD, b.x, b.y, b.z, 2, 0.04, 0.04, 0.04, 0.05);
        }
        SMOULDER_UNTIL.put(player.getUUID(), level.getGameTime() + SMOKE_MAX_TICKS);
    }

    /** Wisps of smoke curling off the barrels until the launcher can fire again. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Long until = SMOULDER_UNTIL.get(player.getUUID());
        if (until == null) return;
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        ItemStack held = player.getMainHandItem();
        // Smoke lasts exactly as long as the launcher cannot fire again: fire delay running, or reloading.
        boolean cannotFire = FireDelayState.isActive(player, held) || GunItem.isReloading(held);
        if (now >= until || !cannotFire || !held.is(ArtificeCannons.QUAD_POTATO_CANNON.get())) {
            SMOULDER_UNTIL.remove(player.getUUID());
            return;
        }
        if (now % 3 != 0) return;
        for (Vec3 b : barrels(player)) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, b.x, b.y, b.z, 0, 0.0, 0.04, 0.0, 1.0);
        }
    }

    private static void onAmmoAmount(AmmoEvent.Amount event) {
        if (event.getShotProfile().itemStack().is(ArtificeCannons.QUAD_POTATO_CANNON.get())) {
            event.setAmmoToConsume(ArtificeCannons.QUAD_BARRELS);
        }
    }

    private static void onImpact(BulletImpactEvent event) {
        Bullet bullet = event.getBullet();
        if (!(bullet.level() instanceof ServerLevel level)) return;
        if (!bullet.getProfile().itemStack().is(ArtificeCannons.QUAD_POTATO_CANNON.get())) return;
        Vec3 at = event.getRayTraceResult().getLocation();
        level.explode(bullet.getOwner(), at.x, at.y, at.z, BLAST_POWER, Level.ExplosionInteraction.NONE);
        event.setHitState(Bullet.HitState.DISCARD);
    }

    private RocketBehaviour() {}
}
