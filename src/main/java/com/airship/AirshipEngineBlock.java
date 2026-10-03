package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Engine block. Right-click with coal or charcoal to fuel it. While an airship flies,
 * every fuelled engine burns fuel as long as the pilot is thrusting and makes the ship faster.
 */
public class AirshipEngineBlock extends Block implements EntityBlock {
    /** Thrust ticks gained per piece of coal / charcoal (same as burning time in a furnace). */
    public static final int FUEL_PER_ITEM = 1600;

    public AirshipEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirshipEngineBlockEntity(pos, state);
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
        if (!(itemStack.is(Items.COAL) || itemStack.is(Items.CHARCOAL))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof AirshipEngineBlockEntity engine)) {
            return InteractionResult.PASS;
        }
        if (engine.getFuel() + FUEL_PER_ITEM > AirshipEngineBlockEntity.MAX_FUEL) {
            player.sendSystemMessage(Component.literal("Engine is full"));
            return InteractionResult.SUCCESS;
        }
        engine.addFuel(FUEL_PER_ITEM);
        if (!player.isCreative()) {
            itemStack.shrink(1);
        }
        player.sendSystemMessage(Component.literal("Engine fuel: " + describe(engine.getFuel())));
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
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof AirshipEngineBlockEntity engine) {
            player.sendSystemMessage(Component.literal(
                    "Engine fuel: " + describe(engine.getFuel()) + " (right-click with coal to refuel)"));
        }
        return InteractionResult.SUCCESS;
    }

    private static String describe(int fuelTicks) {
        return (fuelTicks / 20) + "s of thrust";
    }
}
