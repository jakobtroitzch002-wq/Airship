package com.airship;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The flight display above the hotbar: speed, fuel left, and the turbo tank. Only shown to the pilot
 * while flying (the server sends the numbers a few times per second).
 */
final class AirshipHud {
    private static final int WIDTH = 150;
    private static final int BACKGROUND = 0xE02A1C11;
    private static final int TEXT = 0xFFEADBB5;
    private static final int MUTED = 0xFFB79F6E;
    private static final int TRACK = 0xFF000000 | 0x4A3020;

    private static AirshipHudPayload latest;
    private static long receivedAt;

    private AirshipHud() {}

    static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "flight_display"), AirshipHud::render);
    }

    static void update(AirshipHudPayload payload) {
        latest = payload;
        receivedAt = System.currentTimeMillis();
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        AirshipHudPayload data = latest;
        if (data == null || client.player == null || !(client.player.getVehicle() instanceof AirshipEntity)) {
            return;
        }
        if (System.currentTimeMillis() - receivedAt > 2000) {
            return;
        }
        Font font = client.font;

        boolean showTurbo = data.turboTicks() > 0 || data.boosting();
        int height = showTurbo ? 54 : 36;
        int x = (g.guiWidth() - WIDTH) / 2;
        int y = g.guiHeight() - 68 - height;

        g.fill(x, y, x + WIDTH, y + height, AirshipGuiStyle.BRASS);
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + height - 1, BACKGROUND);

        // Speed and engines
        String speed = String.format("%.1f", data.speed()) + " m/s";
        g.text(font, Component.literal(speed).withStyle(ChatFormatting.BOLD), x + 5, y + 4, TEXT, false);
        String engines = data.activeEngines() + "/" + data.engines() + " Antriebe";
        g.text(font, engines, x + WIDTH - 5 - font.width(engines), y + 4, MUTED, false);

        // Fuel
        g.text(font, "Treibstoff", x + 5, y + 16, MUTED, false);
        String fuel = AirshipFuel.time(data.fuelTicks()) + " Min";
        g.text(font, Component.literal(fuel).withStyle(ChatFormatting.BOLD),
                x + WIDTH - 5 - font.width(fuel), y + 16, data.fuelTicks() > 0 ? TEXT : AirshipGuiStyle.BAD, false);
        bar(g, x + 5, y + 27, WIDTH - 10, (float) data.fuelTicks() / AirshipEngineBlockEntity.MAX_FUEL,
                AirshipGuiStyle.FLAME);

        // Turbo
        if (showTurbo) {
            String key = AirshipClient.BOOST.getTranslatedKeyMessage().getString();
            g.text(font, "Turbo [" + key + "]", x + 5, y + 36, data.boosting() ? AirshipGuiStyle.EMBER : MUTED, false);
            String turbo = AirshipFuel.time(data.turboTicks() / AirshipEntity.TURBO_BURN_RATE) + " Min";
            g.text(font, Component.literal(turbo).withStyle(ChatFormatting.BOLD),
                    x + WIDTH - 5 - font.width(turbo), y + 36, TEXT, false);
            bar(g, x + 5, y + 46, WIDTH - 10, (float) data.turboTicks() / AirshipEngineBlockEntity.MAX_FUEL,
                    AirshipGuiStyle.TURBO);
        }
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, float fraction, int color) {
        g.fill(x, y, x + w, y + 4, TRACK);
        int filled = Math.round(w * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (fraction > 0.0F && filled < 1) {
            filled = 1;
        }
        if (filled > 0) {
            g.fill(x, y, x + filled, y + 4, color);
        }
    }
}
