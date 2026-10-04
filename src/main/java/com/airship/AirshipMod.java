package com.airship;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
        PayloadTypeRegistry.clientboundPlay().register(
                AirshipStatePayload.TYPE,
                AirshipStatePayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
                AirshipSeatMapPayload.TYPE,
                AirshipSeatMapPayload.CODEC
        );
        AirshipControls.registerServer();

        // Send the ship's blocks to every player that starts seeing the ship.
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (trackedEntity instanceof AirshipEntity ship) {
                ServerPlayNetworking.send(player, new AirshipBlocksPayload(ship.getId(), ship.clientCells(), ship.getClientCushions(), ship.getSeats()));
                ServerPlayNetworking.send(player, ship.statePayload(player.level().getGameTime()));
                ServerPlayNetworking.send(player, ship.seatMapPayload());
            }
        });

        LOGGER.info("Airship mod initialized");
    }
}
