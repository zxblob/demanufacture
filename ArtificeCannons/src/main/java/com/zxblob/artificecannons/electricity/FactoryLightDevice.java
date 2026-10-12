package com.zxblob.artificecannons.electricity;

import com.george_vi.electroenergetics.devices.device.DevicesSavedData;
import com.george_vi.electroenergetics.devices.device.SimulatedDeviceType;
import com.george_vi.electroenergetics.foundation.device.SimpleElectricalDevice;
import com.george_vi.electroenergetics.foundation.nodes.InWorldNode;
import com.george_vi.electroenergetics.simulation.BridgeCollector;
import com.george_vi.electroenergetics.simulation.SimulationResults;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One segment of a Factory Light. Each segment is a lamp (a resistor between its two nodes, like EE's own Bulb), and
 * segments of one strip are tied together with near-zero-ohm links so wiring any segment powers the whole strip.
 * Brightness follows the voltage across the segment.
 */
public class FactoryLightDevice extends SimpleElectricalDevice {
    // TUNE: matches EE's default bulb resistance (1000 ohm) so a light behaves like a bulb on the same network
    private static final double SEGMENT_RESISTANCE = 1000.0;

    /** Resistance of a joining link between two segments, in ohms. */
    private static final double LINK_RESISTANCE = 0.01;

    public FactoryLightDevice(Level level, BlockPos pos, DevicesSavedData deviceSD, SimulatedDeviceType<?> type) {
        super(level, pos, deviceSD, type);
    }

    @Override
    public void preTick(BridgeCollector bridges) {
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FactoryLightBlock)) return;

        bridges.builder(pos).resistor(0, 1, SEGMENT_RESISTANCE);

        BlockPos next = pos.relative(state.getValue(FactoryLightBlock.AXIS), 1);
        if (level.isLoaded(next)) {
            BlockState other = level.getBlockState(next);
            if (other.getBlock() == state.getBlock() && other.getValue(FactoryLightBlock.AXIS) == state.getValue(FactoryLightBlock.AXIS)) {
                bridges.bridge(new InWorldNode(0, pos), new InWorldNode(0, next), LINK_RESISTANCE, 0.0, 0.0);
                bridges.bridge(new InWorldNode(1, pos), new InWorldNode(1, next), LINK_RESISTANCE, 0.0, 0.0);
            }
        }
    }

    @Override
    public void postTick(SimulationResults results) {
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FactoryLightBlock)) return;
        double volts = Math.abs(results.getVoltageAt(pos, 0, 1));
        int light = (int) Math.min(15, Math.round(volts / 19.0)); // same curve as EE's Bulb
        int power = light == 0 ? 0 : (light < 6 ? 1 : (light < 12 ? 2 : 3));
        boolean changed = state.getValue(FactoryLightBlock.POWER) != power;
        if (changed) {
            level.setBlockAndUpdate(pos, state.setValue(FactoryLightBlock.POWER, power));
        }
        // refresh the light below on change, and now and then so it follows terrain edits
        if (changed || Math.floorMod(level.getGameTime() + pos.asLong(), 20L) == 0L) {
            FactoryLightBeams.project(level, pos, power);
        }
    }

    @Override
    public void onDestroy() {
        FactoryLightBeams.project(level, pos, 0);
        super.onDestroy();
    }

    @Override
    public boolean shouldRemove(BlockState oldState, BlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
