package com.zxblob.artificecannons.electricity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Projects the light of a Factory Light segment down onto whatever is below it. */
final class FactoryLightBeams {
    // TUNE: how many blocks below the fixture the light reaches (Power Grid's default was 16)
    static final int RANGE = 16;

    /**
     * Sets the beam column under {@code source} for the segment's POWER state (0 or 1: nothing, 2: light 10,
     * 3: light 15). Stops at the first solid block, like the original.
     */
    static void project(Level level, BlockPos source, int segmentPower) {
        int beam = segmentPower >= 3 ? 1 : (segmentPower == 2 ? 0 : -1);
        for (int i = 1; i < RANGE; i++) {
            BlockPos p = source.below(i);
            if (!level.isLoaded(p)) return;
            BlockState s = level.getBlockState(p);
            boolean isBeam = s.getBlock() instanceof FactoryLightBeamBlock;
            if (beam < 0) {
                if (!isBeam) return;
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            } else if (isBeam) {
                if (s.getValue(FactoryLightBeamBlock.POWER) != beam) {
                    level.setBlock(p, s.setValue(FactoryLightBeamBlock.POWER, beam), 3);
                }
            } else if (s.isAir() && s.getFluidState().isEmpty()) {
                level.setBlock(p, FactoryLightContent.BEAM.get().defaultBlockState().setValue(FactoryLightBeamBlock.POWER, beam), 3);
            } else {
                return;
            }
        }
    }

    /** Is there a powered Factory Light somewhere in the column above {@code pos}? */
    static boolean hasLitSourceAbove(Level level, BlockPos pos) {
        for (int i = 1; i < RANGE; i++) {
            BlockPos p = pos.above(i);
            if (!level.isLoaded(p)) return true; // unknown: keep, the source will refresh or remove it later
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof FactoryLightBlock) return s.getValue(FactoryLightBlock.POWER) >= 2;
            if (!(s.getBlock() instanceof FactoryLightBeamBlock) && !s.isAir()) return false;
        }
        return false;
    }

    private FactoryLightBeams() {}
}
