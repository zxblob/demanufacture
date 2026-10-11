package com.zxblob.artificecannons;

import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.entity.Bullet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;
import java.util.List;

/** Behaviour for the Flamethrower (short-lived burning flames) and the Electro-Zapper (chain lightning). */
final class ExtraGuns {
    /** Flames die after this many ticks, which sets the flamethrower's range. */
    private static final int FLAME_LIFETIME_TICKS = 10;
    private static final int FLAME_BURN_SECONDS = 5;
    private static final double ZAPPER_CHAIN_RADIUS = 5.0;
    private static final int ZAPPER_CHAIN_TARGETS = 3;
    /** Damage each chained target takes, as a fraction of the shot's damage. */
    private static final float ZAPPER_CHAIN_FRACTION = 0.5F;

    static void register() {
        NeoForge.EVENT_BUS.addListener(ExtraGuns::onBulletTick);
        NeoForge.EVENT_BUS.addListener(ExtraGuns::onImpact);
    }

    private static boolean isGun(Bullet bullet, net.minecraft.world.item.Item item) {
        return bullet.getProfile().itemStack().is(item);
    }

    private static void onBulletTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bullet bullet) || !(bullet.level() instanceof ServerLevel level)) return;
        if (isGun(bullet, ArtificeCannons.FLAMETHROWER.get())) {
            level.sendParticles(ParticleTypes.FLAME, bullet.getX(), bullet.getY(), bullet.getZ(), 1, 0.05, 0.05, 0.05, 0.01);
            if (bullet.tickCount % 3 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, bullet.getX(), bullet.getY(), bullet.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
            }
            if (bullet.tickCount >= FLAME_LIFETIME_TICKS) bullet.discard();
        } else if (isGun(bullet, ArtificeCannons.ELECTRO_ZAPPER.get())) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, bullet.getX(), bullet.getY(), bullet.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void onImpact(BulletImpactEvent event) {
        Bullet bullet = event.getBullet();
        if (!(bullet.level() instanceof ServerLevel level)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)) return;
        Entity target = hit.getEntity();
        if (isGun(bullet, ArtificeCannons.FLAMETHROWER.get())) {
            target.igniteForSeconds(FLAME_BURN_SECONDS);
        } else if (isGun(bullet, ArtificeCannons.ELECTRO_ZAPPER.get()) && target instanceof LivingEntity victim) {
            chain(level, bullet, victim);
        }
    }

    private static void chain(ServerLevel level, Bullet bullet, LivingEntity first) {
        Entity owner = bullet.getOwner();
        float damage = bullet.resolveDamage() * ZAPPER_CHAIN_FRACTION;
        Vec3 from = first.position().add(0, first.getBbHeight() * 0.5, 0);
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, first.getBoundingBox().inflate(ZAPPER_CHAIN_RADIUS),
                e -> e != first && e != owner && e.isAlive() && (owner == null || !e.isAlliedTo(owner)));
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(first)));
        DamageSource source = level.damageSources().indirectMagic(bullet, owner);
        int n = 0;
        for (LivingEntity e : near) {
            if (n++ >= ZAPPER_CHAIN_TARGETS) break;
            e.hurt(source, damage);
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0);
            int steps = Math.max(2, (int) from.distanceTo(to) * 3);
            for (int i = 0; i <= steps; i++) {
                Vec3 p = from.lerp(to, i / (double) steps);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    private ExtraGuns() {}
}
