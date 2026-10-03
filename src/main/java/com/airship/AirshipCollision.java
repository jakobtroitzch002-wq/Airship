package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Swept collision of an airship against the world.
 * <p>
 * Only the "leading" cells of the ship (those without a neighbour in the movement direction)
 * are tested, so the cost scales with the ship's surface, not its volume.
 * Airship build blocks are ignored: ships fly straight through them.
 */
public final class AirshipCollision {
    private static final double EPS = 1.0E-7;

    private AirshipCollision() {}

    public record Result(Vec3 pos, boolean hitX, boolean hitY, boolean hitZ) {}

    public static Result move(Level level, AirshipEntity ship, Vec3 start, Vec3 delta) {
        double x = start.x;
        double y = start.y;
        double z = start.z;
        boolean hitX = false;
        boolean hitY = false;
        boolean hitZ = false;

        if (delta.y != 0.0) {
            double allowed = clip(level, ship, x, y, z, 1, delta.y);
            hitY = allowed != delta.y;
            y += allowed;
        }
        if (delta.x != 0.0) {
            double allowed = clip(level, ship, x, y, z, 0, delta.x);
            hitX = allowed != delta.x;
            x += allowed;
        }
        if (delta.z != 0.0) {
            double allowed = clip(level, ship, x, y, z, 2, delta.z);
            hitZ = allowed != delta.z;
            z += allowed;
        }
        return new Result(new Vec3(x, y, z), hitX, hitY, hitZ);
    }

    /** Returns how far (signed) the ship may move along the axis (0 = x, 1 = y, 2 = z). */
    private static double clip(Level level, AirshipEntity ship, double px, double py, double pz, int axis, double d) {
        Direction direction = switch (axis) {
            case 0 -> d > 0 ? Direction.EAST : Direction.WEST;
            case 1 -> d > 0 ? Direction.UP : Direction.DOWN;
            default -> d > 0 ? Direction.SOUTH : Direction.NORTH;
        };

        double allowed = d;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double[] cellMin = new double[3];

        for (BlockPos cell : ship.leadingCells(direction)) {
            if (allowed == 0.0) {
                return 0.0;
            }

            // The ship's entity position is the centre of the Core block's footprint.
            cellMin[0] = px + cell.getX() - 0.5;
            cellMin[1] = py + cell.getY();
            cellMin[2] = pz + cell.getZ() - 0.5;

            double[] sweepMin = {cellMin[0], cellMin[1], cellMin[2]};
            double[] sweepMax = {cellMin[0] + 1.0, cellMin[1] + 1.0, cellMin[2] + 1.0};
            if (allowed > 0) {
                sweepMax[axis] += allowed;
            } else {
                sweepMin[axis] += allowed;
            }

            int x0 = Mth.floor(sweepMin[0]);
            int y0 = Mth.floor(sweepMin[1]);
            int z0 = Mth.floor(sweepMin[2]);
            int x1 = Mth.floor(sweepMax[0] - EPS);
            int y1 = Mth.floor(sweepMax[1] - EPS);
            int z1 = Mth.floor(sweepMax[2] - EPS);

            for (int bx = x0; bx <= x1; bx++) {
                for (int by = y0; by <= y1; by++) {
                    for (int bz = z0; bz <= z1; bz++) {
                        cursor.set(bx, by, bz);

                        if (!level.hasChunkAt(cursor)) {
                            // Treat unloaded terrain as a solid wall so ships never fly into it.
                            allowed = clipAgainst(new AABB(bx, by, bz, bx + 1, by + 1, bz + 1), cellMin, axis, allowed);
                            continue;
                        }

                        BlockState state = level.getBlockState(cursor);
                        if (ModBlocks.isAirshipBuildBlock(state.getBlock())) {
                            continue;
                        }
                        VoxelShape shape = state.getCollisionShape(level, cursor);
                        if (shape.isEmpty()) {
                            continue;
                        }
                        for (AABB box : shape.toAabbs()) {
                            allowed = clipAgainst(box.move(bx, by, bz), cellMin, axis, allowed);
                        }
                    }
                }
            }
        }
        return allowed;
    }

    private static double clipAgainst(AABB obstacle, double[] cellMin, int axis, double allowed) {
        for (int k = 0; k < 3; k++) {
            if (k == axis) {
                continue;
            }
            if (!(max(obstacle, k) > cellMin[k] + EPS && min(obstacle, k) < cellMin[k] + 1.0 - EPS)) {
                return allowed;
            }
        }
        if (allowed > 0) {
            double gap = min(obstacle, axis) - (cellMin[axis] + 1.0);
            if (gap >= -EPS && gap < allowed) {
                return Math.max(gap, 0.0);
            }
        } else {
            double gap = max(obstacle, axis) - cellMin[axis];
            if (gap <= EPS && gap > allowed) {
                return Math.min(gap, 0.0);
            }
        }
        return allowed;
    }

    private static double min(AABB box, int axis) {
        return axis == 0 ? box.minX : axis == 1 ? box.minY : box.minZ;
    }

    private static double max(AABB box, int axis) {
        return axis == 0 ? box.maxX : axis == 1 ? box.maxY : box.maxZ;
    }
}
