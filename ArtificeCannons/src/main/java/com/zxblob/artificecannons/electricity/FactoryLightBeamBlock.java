package com.zxblob.artificecannons.electricity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible, intangible light source a Factory Light projects below itself so the floor is actually lit. It behaves
 * like air (placeable-over, no collision) and removes itself when no powered Factory Light is above it any more.
 */
public class FactoryLightBeamBlock extends Block {
    /** 0 = light 10 (light on), 1 = light 15 (full power). */
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 1);
    private static final int CHECK_TICKS = 10;

    public FactoryLightBeamBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(POWER) == 1 ? 15 : 10)
                .noCollission().noLootTable().noOcclusion().replaceable().noTerrainParticles()
                .pushReaction(PushReaction.DESTROY).air());
        registerDefaultState(defaultBlockState().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, CHECK_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (FactoryLightBeams.hasLitSourceAbove(level, pos)) {
            level.scheduleTick(pos, this, CHECK_TICKS);
        } else {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
