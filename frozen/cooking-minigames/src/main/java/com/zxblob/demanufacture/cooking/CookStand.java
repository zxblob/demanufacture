package com.zxblob.demanufacture.cooking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Where a cook stands. The cooking animations are authored with the player a fixed distance from
 * the pot, so when a minigame opens the player is moved to that spot, on whichever side of the pot
 * they were nearest, facing it. If no side is free the player is left where they are.
 */
public final class CookStand
{
    /** Centre of the pot to the player's centre, in blocks. Matches the rigs (14 px). */
    public static final double STAND_DISTANCE = 0.875;

    private CookStand()
    {
    }

    public static void moveToStand(ServerPlayer player, BlockPos potPos)
    {
        Level level = player.level();
        Vec3 center = Vec3.atCenterOf(potPos);
        double dx = player.getX() - center.x;
        double dz = player.getZ() - center.z;

        List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        sides.sort(Comparator.comparingDouble(d -> -(d.getStepX() * dx + d.getStepZ() * dz)));

        for (Direction side : sides)
        {
            double x = center.x + side.getStepX() * STAND_DISTANCE;
            double z = center.z + side.getStepZ() * STAND_DISTANCE;
            AABB box = player.getBoundingBox().move(x - player.getX(), 0, z - player.getZ());
            boolean roomToStand = level.noCollision(player, box);
            boolean ground = !level.noCollision(player, box.move(0, -0.2, 0));
            if (!roomToStand || !ground)
                continue;

            float yaw = (float) (Mth.atan2(-(center.x - x), center.z - z) * 180.0 / Math.PI);
            player.connection.teleport(x, player.getY(), z, yaw, 20f);
            return;
        }
    }
}
