package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        AirshipStructureDetector.DetectionResult result =
                AirshipStructureDetector.detect(level, pos);

        int count = result.blocks().size();
        if (result.capped()) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Airship structure detected: " + count + " blocks (search limit reached)"
                    )
            );
        } else {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Airship structure detected: " + count + " blocks"
                    )
            );
        }

        return InteractionResult.SUCCESS;
    }
}
