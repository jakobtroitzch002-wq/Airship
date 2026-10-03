package com.airship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.entity.EntityTypeTest;

public final class AirshipMovement {
    private AirshipMovement() {
    }

    public static boolean move(Level level, AirshipStructureDetector.DetectionResult result, Direction direction) {
        List<BlockPos> sourcePositions = new ArrayList<>();
        for (BlockPos pos : result.blocks()) {
            if (!ModBlocks.isAirshipBuildBlock(level.getBlockState(pos).getBlock())) {
                sourcePositions.add(pos);
            }
        }
        BlockPos offset = new BlockPos(direction.getStepX(), direction.getStepY(), direction.getStepZ());

        for (BlockPos source : sourcePositions) {
            BlockPos target = source.offset(offset.getX(), offset.getY(), offset.getZ());

            if (!result.blocks().contains(target) && !level.getBlockState(target).isAir()) {
                return false;
            }
        }

        sourcePositions.sort(Comparator.comparingInt(pos -> movementCoordinate(pos, direction)));

        if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            sourcePositions.sort(Comparator.comparingInt(pos -> -movementCoordinate(pos, direction)));
        }

        List<BlockState> states = new ArrayList<>(sourcePositions.size());
        for (BlockPos source : sourcePositions) {
            states.add(level.getBlockState(source));
        }

        List<Entity> passengers = new ArrayList<>();
        for (Entity entity : level.getEntities(EntityTypeTest.forClass(Entity.class),
                    new AABB(
                            result.blocks().stream().mapToDouble(BlockPos::getX).min().orElse(0.0) - 1.0,
                            result.blocks().stream().mapToDouble(BlockPos::getY).min().orElse(0.0) - 1.0,
                            result.blocks().stream().mapToDouble(BlockPos::getZ).min().orElse(0.0) - 1.0,
                            result.blocks().stream().mapToDouble(BlockPos::getX).max().orElse(0.0) + 2.0,
                            result.blocks().stream().mapToDouble(BlockPos::getY).max().orElse(0.0) + 2.0,
                            result.blocks().stream().mapToDouble(BlockPos::getZ).max().orElse(0.0) + 2.0
                    ),
                    entity -> true)) {
            if (!entity.isAlive()) {
                continue;
            }

            if (isOnAirship(entity, result.blocks())) {
                passengers.add(entity);
            }
        }

        for (BlockPos source : sourcePositions) {
            level.removeBlock(source, false);
        }

        for (int i = 0; i < sourcePositions.size(); i++) {
            BlockPos source = sourcePositions.get(i);
            BlockPos target = source.offset(offset.getX(), offset.getY(), offset.getZ());
            level.setBlock(target, states.get(i), 3);
        }

        AirshipStructureRegistry.move(level, result.blocks(), offset);

        for (Entity entity : passengers) {
            entity.setPos(
                    entity.getX() + offset.getX(),
                    entity.getY() + offset.getY(),
                    entity.getZ() + offset.getZ()
            );
            entity.setDeltaMovement(
                    entity.getDeltaMovement().x,
                    entity.getDeltaMovement().y,
                    entity.getDeltaMovement().z
            );
            entity.resetFallDistance();
        }

        return true;
    }

    private static boolean isOnAirship(Entity entity, java.util.Set<BlockPos> structure) {
        BlockPos feet = BlockPos.containing(
                entity.getX(),
                entity.getBoundingBox().minY - 0.01,
                entity.getZ()
        );

        BlockPos below = feet.below();

        return structure.contains(feet) || structure.contains(below);
    }

    private static int movementCoordinate(BlockPos pos, Direction direction) {
        return switch (direction.getAxis()) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }
}
