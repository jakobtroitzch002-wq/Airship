package com.airship;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AirshipMod implements ModInitializer {
    public static final String MOD_ID = "airship";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModBlockEntities.initialize();

        PayloadTypeRegistry.serverboundPlay().register(
                AirshipControlPayload.TYPE,
                AirshipControlPayload.CODEC
        );
        AirshipControls.registerServer();

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (player.getItemInHand(hand).getItem() instanceof BlockItem) {
                var targetPos = hit.getBlockPos().relative(hit.getDirection());

                if (level.getBlockState(targetPos).isAir()) {
                    AirshipStructureRegistry.onBlockPlaced(level, targetPos);
                }

                return InteractionResult.PASS;
            }

            var block = level.getBlockState(hit.getBlockPos()).getBlock();

            if (block instanceof AirshipCoreBlock) {
                return AirshipCoreBlock.handleUse(level, hit.getBlockPos(), player);
            }

            if (block == ModBlocks.AIRSHIP_ENGINE) {
                return AirshipEngine.handleUse(level, hit.getBlockPos(), player);
            }

            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            AirshipStructureRegistry.onBlockBroken(level, pos);
        });

        LOGGER.info("Airship mod initialized");
    }
}
