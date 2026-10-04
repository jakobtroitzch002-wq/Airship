package com.airship;

/** Positions (in GUI pixels, relative to the top-left corner of the panel) shared by the engine menu and screen. */
public final class AirshipEngineLayout {
    public static final int PANEL_W = 204;
    public static final int PANEL_H = 208;

    /** Top-left of the fuel slot background (18x18). The item sits 1 pixel inside. */
    public static final int FUEL_X = 10;
    public static final int FUEL_Y = 36;

    /** Top-left of the first inventory slot background. */
    public static final int INV_X = 21;
    public static final int INV_Y = 120;

    private AirshipEngineLayout() {}
}
