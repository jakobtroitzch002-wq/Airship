package com.airship;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.entity.Entity;

public final class AirshipClient implements ClientModInitializer {
    /**
     * Descending has its own key (default C, rebindable) instead of the sprint key: the sprint key can
     * report "held" while toggled, which made the ship sink on its own.
     */
    private static final KeyMapping DESCEND = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.airship.descend",
                    InputConstants.KEY_C,
                    KeyMapping.Category.MISC
            )
    );

    private static int lastSentFlags = -1;
    private static int tickCounter;

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlockEntities.AIRSHIP_BUILD, AirshipBuildBlockRenderer::new);
        EntityRendererRegistry.register(ModEntities.AIRSHIP, AirshipEntityRenderer::new);

        // Engine screen (a container menu) and Core screen (opened by the server's check result).
        MenuScreens.register(ModMenus.ENGINE, AirshipEngineScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(AirshipCoreInfoPayload.TYPE, (payload, context) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.gui.screen() instanceof AirshipCoreScreen screen && screen.isFor(payload.pos())) {
                screen.update(payload);
            } else {
                client.gui.setScreen(new AirshipCoreScreen(payload));
            }
        });

        // Exact position and rotation of a ship, sent every tick while it moves.
        ClientPlayNetworking.registerGlobalReceiver(AirshipStatePayload.TYPE, (payload, context) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && client.level.getEntity(payload.entityId()) instanceof AirshipEntity ship) {
                ship.receiveSnapshot(payload);
            }
        });

        // Which passenger sits on which seat.
        ClientPlayNetworking.registerGlobalReceiver(AirshipSeatMapPayload.TYPE, (payload, context) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && client.level.getEntity(payload.entityId()) instanceof AirshipEntity ship) {
                java.util.Map<Integer, Integer> assignment = new java.util.HashMap<>();
                for (int i = 0; i < payload.passengerIds().size() && i < payload.seatIndices().size(); i++) {
                    assignment.put(payload.passengerIds().get(i), payload.seatIndices().get(i));
                }
                ship.setSeatAssignment(assignment);
            }
        });

        // Block data of a ship: apply it now if the entity exists, otherwise park it until it does.
        ClientPlayNetworking.registerGlobalReceiver(AirshipBlocksPayload.TYPE, (payload, context) -> {
            Minecraft client = Minecraft.getInstance();
            Entity entity = client.level == null ? null : client.level.getEntity(payload.entityId());
            if (entity instanceof AirshipEntity ship) {
                ship.setClientData(payload);
            } else {
                AirshipPendingCells.put(payload.entityId(), payload);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            AirshipPendingCells.clear();
            lastSentFlags = -1;
        });

        // While sitting in an airship, report the movement keys to the server.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !(client.player.getVehicle() instanceof AirshipEntity)) {
                lastSentFlags = -1;
                return;
            }

            int flags = 0;
            if (client.gui.screen() == null) {
                Options options = client.options;
                if (options.keyUp.isDown()) flags |= AirshipControlPayload.FORWARD;
                if (options.keyDown.isDown()) flags |= AirshipControlPayload.BACKWARD;
                if (options.keyLeft.isDown()) flags |= AirshipControlPayload.LEFT;
                if (options.keyRight.isDown()) flags |= AirshipControlPayload.RIGHT;
                if (options.keyJump.isDown()) flags |= AirshipControlPayload.UP;
                if (DESCEND.isDown()) flags |= AirshipControlPayload.DOWN;
            }

            tickCounter++;
            // Send on change, plus a heartbeat so the server knows the keys are still held.
            if (flags != lastSentFlags || tickCounter % 5 == 0) {
                ClientPlayNetworking.send(new AirshipControlPayload(flags));
                lastSentFlags = flags;
            }
        });
    }
}
