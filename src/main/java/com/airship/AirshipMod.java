package com.airship;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AirshipMod implements ModInitializer {
    public static final String MOD_ID = "airship";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModEntities.initialize();

        PayloadTypeRegistry.serverboundPlay().register(
                AirshipControlPayload.TYPE,
                AirshipControlPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                AirshipBlocksPayload.TYPE,
                AirshipBlocksPayload.CODEC
        );
        AirshipControls.registerServer();

        // Send the ship's blocks to every player that starts seeing the ship.
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (trackedEntity instanceof AirshipEntity ship) {
                ServerPlayNetworking.send(player, new AirshipBlocksPayload(ship.getId(), ship.clientCells()));
            }
        });

        // Right-click the ship to board it; sneak + right-click to land it (turn it back into blocks).
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(entity instanceof AirshipEntity ship) || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.SUCCESS;
            }

            if (player.isShiftKeyDown()) {
                if (!ship.getPassengers().isEmpty()) {
                    player.sendSystemMessage(Component.literal("Someone is still sitting in the airship"));
                } else if (AirshipAssembler.disassemble(serverLevel, ship)) {
                    player.sendSystemMessage(Component.literal("Airship landed"));
                } else {
                    player.sendSystemMessage(Component.literal("No free space to land here"));
                }
            } else if (ship.canAddPassenger(player)) {
                player.startRiding(ship, true, true);
            } else {
                player.sendSystemMessage(Component.literal("No free seat on this airship"));
            }
            return InteractionResult.SUCCESS;
        });

        LOGGER.info("Airship mod initialized");
    }
}
