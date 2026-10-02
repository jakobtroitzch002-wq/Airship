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

    private AirshipStructureDetector() {
    }

    public static DetectionResult detect(Level level, BlockPos corePos) {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        found.add(corePos.immutable());
        queue.add(corePos.immutable());

        boolean capped = false;

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);

                if (found.contains(next) || !level.hasChunkAt(next)) {
                    continue;
                }

                BlockState state = level.getBlockState(next);
                if (state.is(ModBlocks.AIRSHIP_BUILD)) {
                    continue;
                }

                if (state.isAir()) {
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

    public record DetectionResult(Set<BlockPos> blocks, boolean capped) {
    }
}
