package com.airship;

import net.minecraft.world.entity.player.Player;

/** The fuel tanks of one engine: a placed engine block, or an engine on a flying ship. */
public interface AirshipEngineTank {
    int getFuel();

    int getTurbo();

    int maxTurbo();

    void addFuel(int ticks);

    void addTurbo(int ticks);

    /** True while the player may keep the engine screen open. */
    boolean isUsableBy(Player player);
}
