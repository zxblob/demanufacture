package com.zxblob.artificecannons.electricity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The zapper's bolt: fast, straight, deals 8 x power split between the target and up to two nearby enemies. */
public class ZapProjectileEntity extends AbstractHurtingProjectile {
    private float power = 1.0F;

    public ZapProjectileEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public static ZapProjectileEntity create(Level level, Vec3 position, Vec3 velocity, float power) {
        ZapProjectileEntity e = new ZapProjectileEntity(ZapperContent.ZAP_PROJECTILE.get(), level);
        e.setPosRaw(position.x, position.y, position.z);
        e.setDeltaMovement(velocity);
        ProjectileUtil.rotateTowardsMovement(e, 1.0F);
        e.setOldPosAndRot();
        e.reapplyPosition();
        e.power = power;
        return e;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected boolean shouldBurn() { return false; }
    @Nullable @Override protected net.minecraft.core.particles.ParticleOptions getTrailParticle() { return null; }

    @Override
    public void tick() {
        super.tick();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
        }
        checkInsideBlocks();
        Vec3 v = getDeltaMovement();
        double x = getX() + v.x, y = getY() + v.y, z = getZ() + v.z;
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
        if (isInWater()) {
            kill();
            return;
        }
        RandomSource r = level().random;
        for (int i = 0; i < 4; i++) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, r.nextFloat() - 0.5F, r.nextFloat() - 0.5F, r.nextFloat() - 0.5F);
        }
        setPos(x, y, z);
    }

    private DamageSource zapDamage() {
        var holder = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ZapperContent.ZAP_DAMAGE);
        return new DamageSource(holder, this, getOwner());
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        if (!target.isAlive()) return;
        if (getOwner() instanceof LivingEntity owner) owner.setLastHurtMob(target);
        if (target instanceof WitherBoss wither && wither.isPowered()) return;

        Level level = level();
        DamageSource source = zapDamage();
        List<Entity> nearby = level.getEntities(target, new AABB(target.blockPosition()).inflate(2.0),
                e -> e instanceof LivingEntity && e != getOwner() && !e.isInvulnerableTo(source));
        float damage = 8.0F * power / Math.min(nearby.size() + 1, 3);
        if (!level.isClientSide) {
            if (!target.hurt(source, damage)) {
                kill();
                return;
            }
            if (power > 0.6F && level instanceof ServerLevel sl) {
                List<Entity> hurt = new ArrayList<>();
                for (Entity e : nearby) {
                    if (hurt.size() >= 2) break;
                    if (e.hurt(source, damage)) {
                        hurt.add(e);
                        arc(sl, target.getBoundingBox().getCenter(), e.getBoundingBox().getCenter());
                    }
                }
            }
        }
        kill();
    }

    /** A line of electric sparks from one victim to the next. */
    private static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        int steps = Math.max(2, (int) (from.distanceTo(to) * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level() instanceof ServerLevel sl) {
            Vec3 p = hit.getLocation();
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 12, 0.2, 0.2, 0.2, 0.2);
        }
        kill();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Power", power);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        power = tag.getFloat("Power");
    }
}
