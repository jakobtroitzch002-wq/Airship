package com.airship;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class AirshipStructureDetector {
    public static final int MAX_BLOCKS = AirshipConfig.get().maxBlocks;
    /** How far (horizontally / vertically) from the Seat the search looks for the Core. */
    private static final int CORE_SEARCH_RADIUS = 32;
    private static final int CORE_SEARCH_HEIGHT = 24;
    private static final int NEAR_RADIUS = 12;
    private static final int NEAR_HEIGHT = 10;

    private AirshipStructureDetector() {}

    /** Blocks that never belong to a ship: air, build blocks, replaceable blocks (grass, snow, water ...). */
    private static boolean isIgnoredBlock(BlockState state) {
        return state.isAir() || ModBlocks.isAirshipBuildBlock(state.getBlock()) || state.canBeReplaced();
    }

    /**
     * Finds the Airship Core that belongs to the block {@code start} (usually the Airship Seat).
     * <p>
     * It looks in a box around the block instead of walking along connected blocks: a ship that rests on the ground
     * is connected to the whole terrain, and walking through that used up the search budget before the Core was
     * reached. If there are several Cores, the one whose registered ship contains this block wins; otherwise the
     * nearest one is returned (the caller then tells the player that the block is not registered).
     */
    public static BlockPos findCore(Level level, BlockPos start) {
        if (ModBlocks.isCore(level.getBlockState(start))) {
            return start.immutable();
        }
        // Almost always the Core is close: look there first and only search the whole box if that finds nothing.
        BlockPos near = searchCore(level, start, NEAR_RADIUS, NEAR_HEIGHT, true);
        if (near != null) {
            return near;
        }
        return searchCore(level, start, CORE_SEARCH_RADIUS, CORE_SEARCH_HEIGHT, false);
    }

    /**
     * Searches a box around {@code start}. Returns the Core whose registered ship contains {@code start}, or (if
     * {@code onlyRegistered} is false) otherwise the nearest Core.
     */
    private static BlockPos searchCore(Level level, BlockPos start, int radius, int height, boolean onlyRegistered) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        BlockPos registered = null;
        double registeredDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                start.offset(-radius, -height, -radius), start.offset(radius, height, radius))) {
            if (!level.hasChunkAt(pos) || !ModBlocks.isCore(level.getBlockState(pos))) {
                continue;
            }
            BlockPos core = pos.immutable();
            double distance = core.distSqr(start);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = core;
            }
            BlockPos relative = new BlockPos(
                    start.getX() - core.getX(), start.getY() - core.getY(), start.getZ() - core.getZ());
            if (distance < registeredDistance
                    && level.getBlockEntity(core) instanceof AirshipCoreBlockEntity coreEntity
                    && coreEntity.getRegistered().contains(relative)) {
                registeredDistance = distance;
                registered = core;
            }
        }
        if (registered != null) {
            return registered;
        }
        return onlyRegistered ? null : nearest;
    }

    public static DetectionResult detect(Level level, BlockPos start) {
        return detect(level, start, Set.of());
    }

    /**
     * Flood fill from {@code start} over all connected ship blocks.
     *
     * @param ignored positions that are never added (terrain the ship touched when it last landed)
     */
    public static DetectionResult detect(Level level, BlockPos start, Set<BlockPos> ignored) {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        found.add(start.immutable());
        queue.add(start.immutable());
        boolean capped = false;

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (found.contains(next) || ignored.contains(next) || !level.hasChunkAt(next)) {
                    continue;
                }
                if (isIgnoredBlock(level.getBlockState(next))) {
                    continue;
                }
                if (found.size() >= MAX_BLOCKS) {
                    capped = true;
                    queue.clear();
                    break;
                }
                BlockPos immutable = next.immutable();
                found.add(immutable);
                queue.addLast(immutable);
            }
        }
        return new DetectionResult(Set.copyOf(found), capped);
    }

    public record DetectionResult(Set<BlockPos> blocks, boolean capped) {}
}
