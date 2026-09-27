package com.airship;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

public final class AirshipClient implements ClientModInitializer {
    private static final KeyMapping FORWARD = KeyBindingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.airship.forward",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UP,
                    KeyMapping.Category.MISC
            )
    );

    private static final KeyMapping BACKWARD = KeyBindingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.airship.backward",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_DOWN,
                    KeyMapping.Category.MISC
            )
    );

    private static final KeyMapping LEFT = KeyBindingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.airship.left",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT,
                    KeyMapping.Category.MISC
            )
    );

    private static final KeyMapping RIGHT = KeyBindingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.airship.right",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_RIGHT,
                    KeyMapping.Category.MISC
            )
    );

    private static int tickCounter;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                return;
            }

            tickCounter++;

            if (tickCounter % 4 != 0) {
                return;
            }

            if (FORWARD.isDown()) {
                send(AirshipControlPayload.FORWARD);
            } else if (BACKWARD.isDown()) {
                send(AirshipControlPayload.BACKWARD);
            } else if (LEFT.isDown()) {
                send(AirshipControlPayload.LEFT);
            } else if (RIGHT.isDown()) {
                send(AirshipControlPayload.RIGHT);
            }
        });
    }

    private static void send(int direction) {
        ClientPlayNetworking.send(new AirshipControlPayload(direction));
    }
}
