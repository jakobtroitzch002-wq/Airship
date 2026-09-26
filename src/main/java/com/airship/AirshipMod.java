package com.airship;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AirshipMod implements ModInitializer {
    public static final String MOD_ID = "airship";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.initialize();

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            var block = level.getBlockState(hit.getBlockPos()).getBlock();

            if (block instanceof AirshipCoreBlock) {
                return AirshipCoreBlock.handleUse(level, hit.getBlockPos(), player);
            }

            if (block == ModBlocks.AIRSHIP_ENGINE) {
                return AirshipEngine.handleUse(level, hit.getBlockPos(), player);
            }

            return InteractionResult.PASS;
        });

        LOGGER.info("Airship mod initialized");
    }
}
