package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AirshipBuildBlockEntity extends BlockEntity {
    private BlockState displayState = net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState();

    public AirshipBuildBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIRSHIP_BUILD, pos, state);
    }

    public BlockState getDisplayState() {
        return displayState;
    }

    public void setDisplayState(BlockState displayState) {
        this.displayState = displayState;
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("display_block", BlockState.CODEC, displayState);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        displayState = input.read("display_block", BlockState.CODEC)
                .orElse(net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState());
    }
}
