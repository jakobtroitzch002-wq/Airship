package com.airship;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The engine screen: fuel slot, fuel gauge, speed bonus, and the player's inventory. */
public class AirshipEngineScreen extends AbstractContainerScreen<AirshipEngineMenu> {
    public AirshipEngineScreen(AirshipEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AirshipEngineLayout.PANEL_W, AirshipEngineLayout.PANEL_H);
    }

    @Override
    protected void init() {
        super.init();
        // The default labels are replaced by our own drawing.
        titleLabelY = -1000;
        inventoryLabelY = -1000;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        AirshipGuiStyle.panel(g, x, y, imageWidth, imageHeight);

        // Title
        AirshipGuiStyle.flame(g, x + 10, y + 8);
        g.text(font, Component.literal("Luftschiff-Antrieb").withStyle(ChatFormatting.BOLD),
                x + 23, y + 9, AirshipGuiStyle.TEXT, false);
        g.text(font, "Brennt nur bei Schub", x + 10, y + 22, AirshipGuiStyle.MUTED, false);

        // Fuel slot, arrow and gauge
        AirshipGuiStyle.slot(g, x + AirshipEngineLayout.FUEL_X, y + AirshipEngineLayout.FUEL_Y);
        AirshipGuiStyle.arrow(g, x + 32, y + 41, AirshipGuiStyle.MUTED);

        int fuel = menu.getFuel();
        int gaugeLeft = x + 48;
        int gaugeRight = x + imageWidth - 10;
        g.text(font, "Treibstoff", gaugeLeft, y + 34, AirshipGuiStyle.MUTED, false);
        String value = AirshipGuiStyle.time(fuel) + " Min";
        g.text(font, Component.literal(value).withStyle(ChatFormatting.BOLD),
                gaugeRight - font.width(value), y + 34, AirshipGuiStyle.TEXT, false);
        AirshipGuiStyle.bar(g, gaugeLeft, y + 46, gaugeRight - gaugeLeft, 8,
                (float) fuel / AirshipEngineBlockEntity.MAX_FUEL, AirshipGuiStyle.FLAME);
        g.text(font, "von " + AirshipGuiStyle.time(AirshipEngineBlockEntity.MAX_FUEL) + " Min",
                gaugeLeft, y + 57, AirshipGuiStyle.MUTED, false);

        // Stat tiles
        int bonus = (int) Math.round(AirshipEntity.ENGINE_BOOST * (1.0 - AirshipEntity.ENGINE_FALLOFF)
                / AirshipEntity.BASE_SPEED * 100.0);
        tile(g, x + 10, y + 72, 88, "Tempo-Bonus", "+" + bonus + " %");
        tile(g, x + 106, y + 72, 88, "Verbrauch", "nur bei Schub");

        // Inventory
        g.text(font, "Inventar", x + 10, y + 108, AirshipGuiStyle.TEXT, false);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                AirshipGuiStyle.slot(g, x + AirshipEngineLayout.INV_X + column * 18,
                        y + AirshipEngineLayout.INV_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            AirshipGuiStyle.slot(g, x + AirshipEngineLayout.INV_X + column * 18,
                    y + AirshipEngineLayout.INV_Y + 58);
        }
    }

    private void tile(GuiGraphicsExtractor g, int x, int y, int w, String label, String value) {
        g.fill(x, y, x + w, y + 28, AirshipGuiStyle.SLOT_DARK);
        g.fill(x + 1, y + 1, x + w - 1, y + 27, AirshipGuiStyle.PANEL);
        g.text(font, label, x + 5, y + 4, AirshipGuiStyle.MUTED, false);
        g.text(font, Component.literal(value).withStyle(ChatFormatting.BOLD), x + 5, y + 15, AirshipGuiStyle.TEXT, false);
    }
}
