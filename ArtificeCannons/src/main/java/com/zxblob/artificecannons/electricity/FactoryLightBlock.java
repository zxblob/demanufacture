package com.zxblob.artificecannons.electricity;

import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import com.george_vi.electroenergetics.foundation.base.SimpleElectricalDeviceBlock;
import com.george_vi.electroenergetics.foundation.nodes.NodeConfigurator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Ceiling strip light. Placing one against the end of another extends the strip (parts 1-3 along Z, 4-6 along X;
 * part 0 is a lone light). Block state names match the ported models: {@code axis}, {@code part}, {@code power}.
 */
public class FactoryLightBlock extends SimpleElectricalDeviceBlock<FactoryLightDevice> {
    public static final EnumProperty<Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 6);
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 3);

    private static final VoxelShape SINGLE = box(2, 8, 2, 14, 16, 14);
    private static final VoxelShape[] SHAPES = {
            SINGLE,
            box(2, 8, 2, 14, 16, 16),  // 1: north end of a Z strip
            box(2, 8, 0, 14, 16, 16),  // 2: Z middle
            box(2, 8, 0, 14, 16, 14),  // 3: south end
            box(2, 8, 2, 16, 16, 14),  // 4: west end of an X strip
            box(0, 8, 2, 16, 16, 14),  // 5: X middle
            box(0, 8, 2, 14, 16, 14),  // 6: east end
    };

    /** Two wire attachment points on top, spread across the strip. */
    private static final NodeConfigurator NODES = new NodeConfigurator.Builder()
            .add(new Vec3(0.28, 0.92, 0.5))
            .add(new Vec3(0.72, 0.92, 0.5))
            .simple(Direction.NORTH);

    public FactoryLightBlock(Properties properties) {
        super(properties.lightLevel(state -> switch (state.getValue(POWER)) {
            case 2 -> 10;
            case 3 -> 15;
            default -> 0;
        }));
        registerDefaultState(defaultBlockState().setValue(AXIS, Axis.Z).setValue(PART, 0).setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, PART, POWER);
    }

    @Override
    public SimulatedDeviceType<FactoryLightDevice> getDevice() {
        return FactoryLightContent.DEVICE.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PART)];
    }

    private static Direction nodeDirection(BlockState state) {
        return state.getValue(AXIS) == Axis.Z ? Direction.NORTH : Direction.EAST;
    }

    @Override
    public Map<Integer, Vec3> getNodePositions(Level level, BlockPos pos, BlockState state) {
        return NODES.getNodes(nodeDirection(state));
    }

    @Nullable
    @Override
    public Vec3 getNodePosition(Level level, BlockPos pos, BlockState state, int id) {
        return NODES.getNodePos(nodeDirection(state), id);
    }

    // ---- strip shape logic (same rules as the original) ----

    private static boolean isCenter(int part) { return part == 2 || part == 5; }
    private static boolean isNegativeEdge(int part) { return part == 1 || part == 4; }
    private static boolean isPositiveEdge(int part) { return part == 3 || part == 6; }
    private static int center(Axis axis) { return axis == Axis.Z ? 2 : 5; }
    private static int negativeEdge(Axis axis) { return axis == Axis.Z ? 1 : 4; }
    private static int positiveEdge(Axis axis) { return axis == Axis.Z ? 3 : 6; }

    /** Can a new light placed on the {@code dir} face of {@code neighbour} extend its strip? */
    private static boolean canConnect(Direction dir, BlockState neighbour) {
        int part = neighbour.getValue(PART);
        Axis axis = neighbour.getValue(AXIS);
        if (part == 0) return true;
        if (isCenter(part) || axis != dir.getAxis()) return false;
        // a negative end can only grow towards negative, a positive end towards positive
        return isNegativeEdge(part) ? dir.getAxisDirection() == AxisDirection.NEGATIVE : dir.getAxisDirection() == AxisDirection.POSITIVE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = ctx.getClickedFace();
        BlockState clicked = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(dir, -1));
        if (clicked.is(this) && dir.getAxis() != Axis.Y && canConnect(dir, clicked)) {
            int part = dir.getAxisDirection() == AxisDirection.POSITIVE ? positiveEdge(dir.getAxis()) : negativeEdge(dir.getAxis());
            return defaultBlockState().setValue(AXIS, dir.getAxis()).setValue(PART, part);
        }
        return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getAxis());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        int part = state.getValue(PART);
        Axis axis = state.getValue(AXIS);
        if (neighborState.is(this)) {
            if (neighborState.getValue(AXIS) == direction.getAxis()) {
                int nPart = neighborState.getValue(PART);
                Axis a = direction.getAxis();
                AxisDirection d = direction.getAxisDirection();
                if (part == 0 && d == AxisDirection.POSITIVE && (isPositiveEdge(nPart) || isCenter(nPart))) {
                    return state.setValue(AXIS, a).setValue(PART, negativeEdge(a));
                }
                if (part == 0 && d == AxisDirection.NEGATIVE && (isNegativeEdge(nPart) || isCenter(nPart))) {
                    return state.setValue(AXIS, a).setValue(PART, positiveEdge(a));
                }
                if (isNegativeEdge(part) && d == AxisDirection.NEGATIVE && (isNegativeEdge(nPart) || isCenter(nPart))) {
                    return state.setValue(PART, center(a));
                }
                if (isPositiveEdge(part) && d == AxisDirection.POSITIVE && (isPositiveEdge(nPart) || isCenter(nPart))) {
                    return state.setValue(PART, center(a));
                }
            }
        } else if (neighborState.is(Blocks.AIR) && axis == direction.getAxis()) {
            if (isNegativeEdge(part) && direction.getAxisDirection() == AxisDirection.POSITIVE) return state.setValue(PART, 0);
            if (isPositiveEdge(part) && direction.getAxisDirection() == AxisDirection.NEGATIVE) return state.setValue(PART, 0);
            if (isCenter(part)) {
                return state.setValue(PART, direction.getAxisDirection() == AxisDirection.POSITIVE ? positiveEdge(axis) : negativeEdge(axis));
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
