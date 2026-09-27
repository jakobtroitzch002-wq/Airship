package com.airship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class AirshipMovement {
    private AirshipMovement() {
    }

    public static boolean move(Level level, AirshipStructureDetector.DetectionResult result, Direction direction) {
        List<BlockPos> sourcePositions = new ArrayList<>(result.blocks());
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

        List<Player> passengers = new ArrayList<>();
        for (Player player : level.players()) {
            BlockPos playerFeet = BlockPos.containing(
                    player.getX(),
                    player.getY() - 0.01,
                    player.getZ()
            );

            BlockPos playerBelow = playerFeet.below();

            if (result.blocks().contains(playerFeet)
                    || result.blocks().contains(playerBelow)) {
                passengers.add(player);
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

        for (Player player : passengers) {
            player.setPos(
                    player.getX() + offset.getX(),
                    player.getY() + offset.getY(),
                    player.getZ() + offset.getZ()
            );
        }

        return true;
    }

    private static int movementCoordinate(BlockPos pos, Direction direction) {
        return switch (direction.getAxis()) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }
}
