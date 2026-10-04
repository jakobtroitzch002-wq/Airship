package com.airship;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.entity.Entity;

public final class AirshipClient implements ClientModInitializer {
    private static int lastSentFlags = -1;
    private static int tickCounter;

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlockEntities.AIRSHIP_BUILD, AirshipBuildBlockRenderer::new);
        EntityRendererRegistry.register(ModEntities.AIRSHIP, AirshipEntityRenderer::new);

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
                if (options.keySprint.isDown()) flags |= AirshipControlPayload.DOWN;
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
