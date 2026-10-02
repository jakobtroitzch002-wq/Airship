package com.airship;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AirshipBuildBlock extends BaseEntityBlock {
    public AirshipBuildBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack itemStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }

        BlockState newDisplayState = blockItem.getBlock().defaultBlockState();

        if (!newDisplayState.isSolidRender()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof AirshipBuildBlockEntity buildEntity)) {
            return InteractionResult.PASS;
        }

        boolean hadOldTexture = buildEntity.hasCustomTexture();
        BlockState oldDisplayState = buildEntity.getDisplayState();
        buildEntity.setDisplayState(newDisplayState);

        if (!player.isCreative()) {
            itemStack.shrink(1);
        }

        if (hadOldTexture) {
            popResource(level, pos, new ItemStack(oldDisplayState.getBlock()));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirshipBuildBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return null;
    }
}
