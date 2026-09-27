package com.airship;

import java.util.Set;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

public final class AirshipControls {
    private AirshipControls() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(
                AirshipControlPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();

                    execute(player, payload.direction());
                }
        );
    }

    private static void execute(ServerPlayer player, int control) {
        Level level = player.level();

        BlockPos playerPos = BlockPos.containing(
                player.getX(),
                player.getY() - 0.01,
                player.getZ()
        );

        Set<BlockPos> structure = AirshipStructureRegistry.get(level, playerPos);

        if (structure == null) {
            return;
        }

        AirshipStructureDetector.DetectionResult result =
                new AirshipStructureDetector.DetectionResult(structure, false);

        if (AirshipEngine.countEngines(level, result) <= 0) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Airship cannot move: no engine"
                    )
            );
            return;
        }

        int blockCount = result.blocks().size();
        int balloonCount = AirshipLift.countBalloons(level, result);
        int requiredBalloons = AirshipLift.requiredBalloons(blockCount);

        if (!AirshipLift.hasEnoughLift(balloonCount, requiredBalloons)) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Airship cannot move: not enough balloons"
                    )
            );
            return;
        }

        Direction facing = player.getDirection();
        Direction direction;

        switch (control) {
            case AirshipControlPayload.FORWARD -> direction = facing;
            case AirshipControlPayload.BACKWARD -> direction = facing.getOpposite();
            case AirshipControlPayload.LEFT -> direction = facing.getCounterClockWise();
            case AirshipControlPayload.RIGHT -> direction = facing.getClockWise();
            default -> {
                return;
            }
        }

        if (!direction.getAxis().isHorizontal()) {
            return;
        }

        AirshipMovement.move(level, result, direction);
    }
}
