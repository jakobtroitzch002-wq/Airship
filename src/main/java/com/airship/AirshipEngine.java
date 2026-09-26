package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class AirshipEngine {
    private AirshipEngine() {
    }

    public static int countEngines(Level level, AirshipStructureDetector.DetectionResult result) {
        int count = 0;

        for (BlockPos pos : result.blocks()) {
            if (level.getBlockState(pos).is(ModBlocks.AIRSHIP_ENGINE)) {
                count++;
            }
        }

        return count;
    }

    public static BlockPos findCore(Level level, AirshipStructureDetector.DetectionResult result) {
        for (BlockPos pos : result.blocks()) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.AIRSHIP_CORE)) {
                return pos;
            }
        }

        return null;
    }

    public static InteractionResult handleUse(Level level, BlockPos enginePos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        AirshipStructureDetector.DetectionResult result =
                AirshipStructureDetector.detect(level, enginePos);

        BlockPos corePos = findCore(level, result);

        if (corePos == null) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Engine is not connected to an Airship Core"
                    )
            );
            return InteractionResult.SUCCESS;
        }

        int engineCount = countEngines(level, result);

        if (engineCount <= 0) {
            return InteractionResult.SUCCESS;
        }

        Direction direction = player.getDirection();

        if (!direction.getAxis().isHorizontal()) {
            direction = Direction.NORTH;
        }

        if (AirshipMovement.move(level, result, direction)) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Airship thrust forward 1 block (" + direction.getName() + ")"
                    )
            );
        } else {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Airship cannot move forward: destination is blocked"
                    )
            );
        }

        return InteractionResult.SUCCESS;
    }
}
