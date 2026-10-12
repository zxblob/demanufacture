package com.zxblob.artificecannons;

import io.redspace.irons_artifice.client.sounds.GunShotSoundSettings;
import io.redspace.irons_artifice.data.FireMode;
import io.redspace.irons_artifice.data.GunShotSoundStack;
import io.redspace.irons_artifice.data.MuzzleFlashType;
import io.redspace.irons_artifice.data.PlayableSound;
import io.redspace.irons_artifice.data.RecoilProfile;
import io.redspace.irons_artifice.data.ShotComponentTemplate;
import io.redspace.irons_artifice.gun.ArmPoseKind;
import io.redspace.irons_artifice.gun.GunProfile;
import io.redspace.irons_artifice.item.GunItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Potato cannons from Create and TFMG as Iron's Arms 'n Artifice guns.
 * IANA wires GeckoLib renderers for every registered GunItem on its own, using the item's registry id as the
 * asset path (assets/artifice_cannons/geo|animations|textures/item/NAME), so registering the item is enough.
 */
@Mod(ArtificeCannons.MOD_ID)
public class ArtificeCannons {
    public static final String MOD_ID = "artifice_cannons";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    /** Blocks of the electricity module (Factory Light); the guns need none. */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final int QUAD_BARRELS = 4;
    public static final int QUAD_ROUNDS = 8;
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    /** Our own shot sound. assets/artifice_cannons/sounds.json points it at Create's fwoomp; swap the file to change it. */
    public static final DeferredHolder<SoundEvent, SoundEvent> POTATO_CANNON_SHOOT = sound("potato_cannon_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_SHOOT = sound("flamethrower_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZAPPER_SHOOT = sound("electro_zapper_shoot");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MOD_ID, name)));
    }

    /**
     * One builder for all three cannons. Classes (see To Do vault note):
     * potato cannon = heavy slow repeater (arquebus-like), advanced = LMG, quad = four-rocket volley launcher.
     */
    private static GunProfile cannon(FireMode mode, int capacity, int modifierSlots, int reloadTicks, int fireDelayTicks,
                                     double damage, double spread, double blowback, RecoilProfile recoil,
                                     double bulletSpeedMultiplier, int projectiles, MuzzleFlashType... flash) {
        return gun(mode, ArmPoseKind.RIFLE, POTATO_CANNON_SHOOT, capacity, modifierSlots, reloadTicks, fireDelayTicks, damage, spread,
                blowback, recoil, bulletSpeedMultiplier, projectiles, -1.0, flash);
    }

    private static GunProfile gun(FireMode mode, ArmPoseKind pose, DeferredHolder<SoundEvent, SoundEvent> shot, int capacity,
                                  int modifierSlots, int reloadTicks, int fireDelayTicks, double damage, double spread,
                                  double blowback, RecoilProfile recoil, double bulletSpeedMultiplier, int projectiles,
                                  double gravity, MuzzleFlashType... flash) {
        ShotComponentTemplate.Builder shotBuilder = ShotComponentTemplate.builder(damage, spread, blowback, fireDelayTicks, recoil)
                .gunshotSound(() -> new GunShotSoundStack(
                        GunShotSoundSettings.standardShot(shot, 1.0F),
                        PlayableSound.standard(PlayableSound.holder(SoundEvents.DISPENSER_FAIL))))
                .bulletSpeedMultiplier(bulletSpeedMultiplier)
                .projectileCount(projectiles)
                .muzzleFlash(flash);
        if (gravity >= 0) shotBuilder.gravity(gravity);
        return GunProfile.builder(capacity, modifierSlots, reloadTicks, mode, pose, shotBuilder.build()).build();
    }

    /** Heavy slow repeater, closer to the arquebus: 8 shots, long cycle, hard hitting, tight. */
    public static final DeferredItem<GunItem> POTATO_CANNON = ITEMS.registerItem("potato_cannon",
            p -> new GunItem(p, cannon(FireMode.SEMI, 8, 7, 25, 18, 12.0, 1.0, 0.5,
                    RecoilProfile.of(30.0F, 0.4F, 1.8F, 222), 1.0, 1, MuzzleFlashType.LARGE)));
    /** Burst rifle: 3-round bursts (see BurstBehaviour), 24 rounds, accurate. */
    public static final DeferredItem<GunItem> ADVANCED_POTATO_CANNON = ITEMS.registerItem("advanced_potato_cannon",
            p -> new GunItem(p, cannon(FireMode.SEMI, 24, 6, 43, BurstBehaviour.SHOT_INTERVAL_TICKS, 7.0, 1.5, 0.15,
                    RecoilProfile.of(12.0F, 0.4F, 1.0F, 333), 1.2, 1, MuzzleFlashType.TRIANGLE, MuzzleFlashType.SMALL_STAR)));
    /** Rocket launcher: one trigger pull fires 4 slow rockets and uses 4 of the 8 rounds (see RocketBehaviour). */
    public static final DeferredItem<GunItem> QUAD_POTATO_CANNON = ITEMS.registerItem("quad_potato_cannon",
            p -> new GunItem(p, gun(FireMode.SEMI, ArmPoseKind.RIFLE, POTATO_CANNON_SHOOT, QUAD_ROUNDS, 4, 40, 30, 8.0, 4.0, 0.75,
                    RecoilProfile.of(45.0F, 0.3F, 1.2F, 444), 0.18, QUAD_BARRELS, 0.02, MuzzleFlashType.LARGE)));

    /** TFMG Flamethrower: short-range automatic stream of fire (see FlameBehaviour). */
    public static final DeferredItem<GunItem> FLAMETHROWER = ITEMS.registerItem("flamethrower",
            p -> new GunItem(p, gun(FireMode.AUTO, ArmPoseKind.RIFLE, FLAMETHROWER_SHOOT, 50, 5, 30, 2, 4.0, 6.0, 0.0,
                    RecoilProfile.of(2.0F, 0.3F, 0.5F, 555), 0.0667, 2, 0.005, MuzzleFlashType.SMALL_STAR)));
    /** Powergrid Electro-Zapper: one-handed energy pistol, chain lightning (see ZapperBehaviour). */
    public static final DeferredItem<GunItem> ELECTRO_ZAPPER = ITEMS.registerItem("electro_zapper",
            p -> new GunItem(p, gun(FireMode.SEMI, ArmPoseKind.PISTOL, ZAPPER_SHOOT, 12, 5, 38, 10, 7.0, 0.3, 0.0,
                    RecoilProfile.of(8.0F, 0.2F, 1.0F, 666), 2.0, 1, -1.0, MuzzleFlashType.TRIANGLE)));

    public ArtificeCannons(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        SOUNDS.register(modBus);
        com.zxblob.artificecannons.electricity.ElectricityModule.init(modBus);
        RocketBehaviour.register();
        BurstBehaviour.register();
        ExtraGuns.register();
        HoldFireAnimation.register();
        Paint.register(modBus);
    }
}
