package com.airship;

import java.util.function.BiConsumer;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A big Core: a cube of {@code size x size x size} blocks, placed and broken as one (like a bed).
 * <p>
 * Every block of the cube has a {@code part} (its place in the cube) and the cube's {@code facing}. Part 0 (the
 * lower front-left corner) is the real Core: it holds the registration and is the ship's origin; the other parts
 * are ordinary ship blocks that show their piece of the big design. When a ship lands turned, only {@code facing}
 * changes, so the parts keep fitting together.
 */
public abstract class AirshipBigCoreBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected AirshipBigCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(partProperty(), 0));
    }

    /** Edge length of the cube in blocks. */
    public abstract int size();

    /** Largest ship this Core holds. */
    public abstract int limit();

    /** The part property; must return a static constant (it is used while the block is being built). */
    protected abstract IntegerProperty partProperty();

    public int part(BlockState state) {
        return state.getValue(partProperty());
    }

    public boolean isMaster(BlockState state) {
        return part(state) == 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, partProperty());
    }

    private static Rotation rotationOf(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * Where a part sits relative to part 0. Seen from the placing player the cube grows to the right (x),
     * upwards (y) and away from the player (z).
     */
    public BlockPos offset(int part, Direction facing) {
        int n = size();
        int dx = part % n;
        int dy = (part / n) % n;
        int dz = part / (n * n);
        return new BlockPos(dx, dy, -dz).rotate(rotationOf(facing));
    }

    public BlockPos masterPos(BlockPos pos, BlockState state) {
        return pos.subtract(offset(part(state), state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        int parts = size() * size() * size();
        for (int part = 1; part < parts; part++) {
            BlockPos target = pos.offset(offset(part, facing));
            if (level.isOutsideBuildHeight(target) || !level.getBlockState(target).canBeReplaced()) {
                return null; // no room for the whole cube
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(partProperty(), 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !isMaster(state)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        int parts = size() * size() * size();
        for (int part = 1; part < parts; part++) {
            level.setBlock(pos.offset(offset(part, facing)), state.setValue(partProperty(), part), Block.UPDATE_ALL);
        }
    }

    /** Breaking any part removes the whole cube; the item drops once (from part 0). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            BlockPos master = masterPos(pos, state);
            int parts = size() * size() * size();
            for (int part = 0; part < parts; part++) {
                BlockPos target = master.offset(offset(part, facing));
                if (target.equals(pos)) {
                    continue;
                }
                BlockState other = level.getBlockState(target);
                if (!other.is(this) || part(other) != part || other.getValue(FACING) != facing) {
                    continue;
                }
                if (part == 0 && !player.isCreative()) {
                    level.destroyBlock(target, true, player); // drops the item
                } else {
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Removes every block of this cube that is still there, without drops. */
    private void removeCube(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos master = masterPos(pos, state);
        int parts = size() * size() * size();
        for (int part = 0; part < parts; part++) {
            BlockPos target = master.offset(offset(part, facing));
            BlockState other = level.getBlockState(target);
            if (other.is(this) && part(other) == part && other.getValue(FACING) == facing) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
    }

    /** True if every block of this cube is in place. */
    private boolean isComplete(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos master = masterPos(pos, state);
        int parts = size() * size() * size();
        for (int part = 0; part < parts; part++) {
            BlockState other = level.getBlockState(master.offset(offset(part, facing)));
            if (!other.is(this) || part(other) != part || other.getValue(FACING) != facing) {
                return false;
            }
        }
        return true;
    }

    /**
     * An explosion that hits any block of the cube destroys the whole cube; the item drops once. (Without this,
     * TNT would leave the other blocks behind as broken pieces.)
     */
    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
            BiConsumer<ItemStack, BlockPos> dropConsumer) {
        removeCube(level, pos, state);
        dropConsumer.accept(new ItemStack(this), pos);
    }

    /** Cleans up pieces of a cube that lost blocks in some other way (they look broken and do nothing). */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isComplete(level, pos, state)) {
            removeCube(level, pos, state);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isMaster(state) ? new AirshipCoreBlockEntity(pos, state) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        return AirshipAssembler.inspect(level, masterPos(pos, state), player);
    }

    /** The 2x2x2 Core: ships of up to 500 blocks. */
    public static class Large extends AirshipBigCoreBlock {
        public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);

        public Large(Properties properties) {
            super(properties);
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public int limit() {
            return 500;
        }

        @Override
        protected IntegerProperty partProperty() {
            return PART;
        }
    }

    /** The 3x3x3 Core: ships of up to 1000 blocks. */
    public static class Colossal extends AirshipBigCoreBlock {
        public static final IntegerProperty PART = IntegerProperty.create("part", 0, 26);

        public Colossal(Properties properties) {
            super(properties);
        }

        @Override
        public int size() {
            return 3;
        }

        @Override
        public int limit() {
            return 1000;
        }

        @Override
        protected IntegerProperty partProperty() {
            return PART;
        }
    }
}
