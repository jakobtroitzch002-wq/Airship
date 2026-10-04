package com.airship;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class AirshipStructureDetector {
    public static final int MAX_BLOCKS = 4096;
    /** How many blocks the search for a Core may visit before giving up. */
    private static final int CORE_SEARCH_LIMIT = 8192;

    private AirshipStructureDetector() {}

    /** Blocks that never belong to a ship: air, build blocks, replaceable blocks (grass, snow, water ...). */
    private static boolean isIgnoredBlock(BlockState state) {
        return state.isAir() || ModBlocks.isAirshipBuildBlock(state.getBlock()) || state.canBeReplaced();
    }

    /**
     * Finds the Airship Core connected to {@code start} (nearest by walking distance), or null.
     * Walks through everything that could belong to a ship, including terrain the ship touches.
     */
    public static BlockPos findCore(Level level, BlockPos start) {
        if (level.getBlockState(start).is(ModBlocks.AIRSHIP_CORE)) {
            return start.immutable();
        }
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start.immutable());
        queue.add(start.immutable());

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (visited.contains(next) || !level.hasChunkAt(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (isIgnoredBlock(state)) {
                    continue;
                }
                if (state.is(ModBlocks.AIRSHIP_CORE)) {
                    return next.immutable();
                }
                if (visited.size() >= CORE_SEARCH_LIMIT) {
                    return null;
                }
                BlockPos immutable = next.immutable();
                visited.add(immutable);
                queue.addLast(immutable);
            }
        }
        return null;
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
