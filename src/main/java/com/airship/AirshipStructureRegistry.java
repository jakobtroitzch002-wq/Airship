package com.airship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class AirshipStructureRegistry {
    private static final Map<Level, List<Set<BlockPos>>> AIRSHIPS = new HashMap<>();

    private AirshipStructureRegistry() {
    }

    public static Set<BlockPos> get(Level level, BlockPos position) {
        List<Set<BlockPos>> structures = AIRSHIPS.get(level);

        if (structures == null) {
            return null;
        }

        cleanup(level, structures);

        for (Set<BlockPos> structure : structures) {
            if (structure.contains(position)) {
                return structure;
            }
        }

        return null;
    }

    public static Set<BlockPos> register(
            Level level,
            AirshipStructureDetector.DetectionResult result
    ) {
        Set<BlockPos> structure = new HashSet<>(result.blocks());

        AIRSHIPS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(structure);
        return structure;
    }

    public static void onBlockPlaced(Level level, BlockPos pos) {
        List<Set<BlockPos>> structures = AIRSHIPS.get(level);

        if (structures == null) {
            return;
        }

        cleanup(level, structures);

        for (Set<BlockPos> structure : structures) {
            if (isAdjacentTo(structure, pos)) {
                if (structure.size() < AirshipStructureDetector.MAX_BLOCKS) {
                    structure.add(pos.immutable());
                }
                return;
            }
        }
    }

    public static void onBlockBroken(Level level, BlockPos pos) {
        List<Set<BlockPos>> structures = AIRSHIPS.get(level);

        if (structures == null) {
            return;
        }

        for (Iterator<Set<BlockPos>> iterator = structures.iterator(); iterator.hasNext();) {
            Set<BlockPos> structure = iterator.next();

            if (structure.remove(pos)) {
                if (structure.isEmpty()) {
                    iterator.remove();
                }
                return;
            }
        }
    }

    public static Set<BlockPos> move(
            Level level,
            Set<BlockPos> structure,
            BlockPos offset
    ) {
        List<Set<BlockPos>> structures = AIRSHIPS.get(level);

        if (structures == null) {
            return structure;
        }

        Set<BlockPos> moved = new HashSet<>();

        for (BlockPos pos : structure) {
            moved.add(
                    pos.offset(
                            offset.getX(),
                            offset.getY(),
                            offset.getZ()
                    ).immutable()
            );
        }

        structures.remove(structure);
        structures.add(moved);

        return moved;
    }

    private static void cleanup(Level level, List<Set<BlockPos>> structures) {
        Iterator<Set<BlockPos>> structureIterator = structures.iterator();

        while (structureIterator.hasNext()) {
            Set<BlockPos> structure = structureIterator.next();

            structure.removeIf(pos -> level.getBlockState(pos).isAir());

            if (structure.isEmpty()) {
                structureIterator.remove();
            }
        }
    }

    private static boolean isAdjacentTo(Set<BlockPos> structure, BlockPos pos) {
        for (BlockPos existing : structure) {
            if (existing.getX() == pos.getX()
                    && existing.getY() == pos.getY()
                    && Math.abs(existing.getZ() - pos.getZ()) == 1) {
                return true;
            }

            if (existing.getX() == pos.getX()
                    && existing.getZ() == pos.getZ()
                    && Math.abs(existing.getY() - pos.getY()) == 1) {
                return true;
            }

            if (existing.getY() == pos.getY()
                    && existing.getZ() == pos.getZ()
                    && Math.abs(existing.getX() - pos.getX()) == 1) {
                return true;
            }
        }

        return false;
    }
}
