package com.airship;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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

    public static InteractionResult handleUse(Level level, BlockPos enginePos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Set<BlockPos> registered = AirshipStructureRegistry.get(level, enginePos);

        if (registered == null) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Use the Airship Core once to register the airship before using the engine"
                    )
            );
            return InteractionResult.SUCCESS;
        }

        AirshipStructureDetector.DetectionResult result =
                new AirshipStructureDetector.DetectionResult(registered, false);

        if (countEngines(level, result) <= 0) {
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
