package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Seat block. Half a block high. Right-click it to sit down: the connected structure turns into a
 * flying airship (it needs a Core). Leaving the seat with Shift lands the ship again.
 */
public class AirshipSeatBlock extends Block {
    /** Height of the seat surface in blocks (must match the shape below). */
    public static final double SEAT_HEIGHT = 0.5;
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, SEAT_HEIGHT, 1.0);

    public AirshipSeatBlock(Properties properties) {
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
        return AirshipAssembler.assemble(level, pos, player);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
