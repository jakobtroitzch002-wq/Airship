package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Engine block. Right-click with a fuel (coal, charcoal, coal block, blaze rod, wood ...) to fuel it, or with
 * an empty hand to open its screen. While an airship flies, every fuelled engine burns fuel as long as the
 * pilot is thrusting and makes the ship faster.
 */
public class AirshipEngineBlock extends Block implements EntityBlock {
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
        int burn = AirshipFuel.burnTime(itemStack);
        if (burn <= 0) {
            // Not a fuel: let the click fall through to the empty-hand action (opening the screen).
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof AirshipEngineBlockEntity engine)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (engine.getFuel() + burn > AirshipEngineBlockEntity.MAX_FUEL) {
            player.sendSystemMessage(Component.literal("Antrieb: Tank voll ("
                    + AirshipFuel.time(engine.getFuel()) + " Min)"));
            return InteractionResult.SUCCESS;
        }
        ItemStack remainder = AirshipFuel.remainder(itemStack);
        engine.addFuel(burn);
        if (!player.isCreative()) {
            itemStack.shrink(1);
            if (itemStack.isEmpty() && !remainder.isEmpty()) {
                player.setItemInHand(hand, remainder);
            }
        }
        player.sendSystemMessage(Component.literal("Antrieb: " + AirshipFuel.time(engine.getFuel())
                + " Min Laufzeit (+" + AirshipFuel.time(burn) + ")"));
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
        MenuProvider provider = state.getMenuProvider(level, pos);
        if (provider != null) {
            player.openMenu(provider);
        }
        return InteractionResult.SUCCESS;
    }

}
