package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AirshipBuildBlockEntity extends BlockEntity {
    private BlockState displayState = Blocks.IRON_BLOCK.defaultBlockState();
    private boolean hasCustomTexture;

    public AirshipBuildBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIRSHIP_BUILD, pos, state);
    }

    public BlockState getDisplayState() {
        return displayState;
    }

    public boolean hasCustomTexture() {
        return hasCustomTexture;
    }

    public void setDisplayState(BlockState displayState) {
        this.displayState = displayState;
        this.hasCustomTexture = true;
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("has_custom_texture", hasCustomTexture);
        if (hasCustomTexture) {
            output.store("display_block", BlockState.CODEC, displayState);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hasCustomTexture = input.getBooleanOr("has_custom_texture", false);
        displayState = input.read("display_block", BlockState.CODEC)
                .orElse(Blocks.IRON_BLOCK.defaultBlockState());
    }
}
