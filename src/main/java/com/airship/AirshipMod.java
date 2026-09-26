package com.airship;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AirshipMod {
    public static final String MOD_ID = "airship";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private AirshipMod() {}

    public static void onInitialize() {
        LOGGER.info("Airship mod initializing");
    }
}
