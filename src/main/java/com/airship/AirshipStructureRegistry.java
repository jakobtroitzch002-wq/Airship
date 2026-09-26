package com.airship;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
        Set<BlockPos> structure = Set.copyOf(result.blocks());
        AIRSHIPS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(structure);
        return structure;
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
            moved.add(pos.offset(offset.getX(), offset.getY(), offset.getZ()).immutable());
        }

        structures.remove(structure);
        structures.add(Set.copyOf(moved));
        return moved;
    }
}
