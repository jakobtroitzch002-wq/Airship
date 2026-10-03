package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Seat block. Half a block high; the airship entity seats players on top of it.
 * It has no behaviour of its own: seats are discovered when an airship is assembled.
 */
public class AirshipSeatBlock extends Block {
    /** Height of the seat surface in blocks (must match the shape below). */
    public static final double SEAT_HEIGHT = 0.5;
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, SEAT_HEIGHT, 1.0);

    public AirshipSeatBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
