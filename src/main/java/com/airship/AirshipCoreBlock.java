package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

public class AirshipCoreBlock extends Block {
    public AirshipCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        return handleUse(level, pos, player);
    }

    public static InteractionResult handleUse(
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Set<BlockPos> registered = AirshipStructureRegistry.get(level, pos);
        AirshipStructureDetector.DetectionResult result;

        if (registered != null) {
            result = new AirshipStructureDetector.DetectionResult(registered, false);
        } else {
            result = AirshipStructureDetector.detect(level, pos);

            if (result.capped()) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "Airship is too large to move (maximum 4096 blocks)"
                        )
                );
                return InteractionResult.SUCCESS;
            }

            AirshipStructureRegistry.register(level, result);
        }

        int blockCount = result.blocks().size();
        int balloonCount = AirshipLift.countBalloons(level, result);
        int requiredBalloons = AirshipLift.requiredBalloons(blockCount);

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "Airship: " + blockCount + " blocks, "
                                + balloonCount + "/" + requiredBalloons + " balloons"
                )
        );

        if (!AirshipLift.hasEnoughLift(balloonCount, requiredBalloons)) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Airship cannot rise: not enough balloons"
                    )
            );
            return InteractionResult.SUCCESS;
        }

        if (AirshipMovement.move(level, result, Direction.UP)) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Airship lifted 1 block"
                    )
            );
        } else {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Airship cannot rise: destination is blocked"
                    )
            );
        }

        return InteractionResult.SUCCESS;
    }
}
