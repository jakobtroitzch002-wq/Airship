package com.airship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side buffer for ship block data that arrives before the ship entity exists on the client.
 * Only touched from the client main thread.
 */
public final class AirshipPendingCells {
    private static final Map<Integer, List<AirshipClientCell>> PENDING = new HashMap<>();

    private AirshipPendingCells() {}

    public static void put(int entityId, List<AirshipClientCell> cells) {
        PENDING.put(entityId, cells);
    }

    public static List<AirshipClientCell> take(int entityId) {
        return PENDING.remove(entityId);
    }

    public static void clear() {
        PENDING.clear();
    }
}
