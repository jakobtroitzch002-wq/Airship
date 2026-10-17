package com.airship;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Lets players stand and walk on a flying ship. A flying ship is one entity, its blocks are only drawn, so the
 * player's own game adds the ship's blocks as collision boxes (see {@code EntityCollideMixin}) and moves a player
 * that stands on the ship along with it ({@link AirshipEntity#carry}).
 * <p>
 * Only the player's own client does this: it decides where its player is, and the ship it sees is the one it
 * stands on. If the server did it too, its ship (a few ticks ahead) would keep pulling the player back.
 */
public final class AirshipShipColliders {
    /** How far around an entity ships are looked for (ships can be much larger than their entity box). */
    private static final double SEARCH = 64.0;
    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);

    private AirshipShipColliders() {}

    /** True if ship collision should be added for this entity; then {@link #exit} must be called afterwards. */
    public static boolean enter(Entity entity, Level level) {
        if (BUSY.get() || !(entity instanceof Player) || level == null || !level.isClientSide() || entity.isPassenger()) {
            return false;
        }
        BUSY.set(true);
        return true;
    }

    public static void exit() {
        BUSY.set(false);
    }

    /** Collision boxes of all flying ships that touch the area. */
    public static List<VoxelShape> shapes(Level level, AABB area) {
        List<VoxelShape> out = new ArrayList<>();
        for (AirshipEntity ship : level.getEntitiesOfClass(AirshipEntity.class, area.inflate(SEARCH))) {
            ship.collisionShapes(area, out);
        }
        return out;
    }

    /** True if one of the ship's blocks is below the entity, at most {@code depth} blocks down. */
    public static boolean isAbove(AirshipEntity ship, Entity entity, double depth) {
        AABB box = entity.getBoundingBox();
        AABB column = new AABB(box.minX, box.minY - depth, box.minZ, box.maxX, box.minY, box.maxZ);
        List<VoxelShape> out = new ArrayList<>();
        ship.collisionShapes(column, out);
        return !out.isEmpty();
    }

    /** True if an entity's feet rest on (or are just inside) one of the ship's blocks. */
    public static boolean standsOn(AirshipEntity ship, Entity entity) {
        AABB box = entity.getBoundingBox();
        AABB feet = new AABB(box.minX, box.minY - 0.25, box.minZ, box.maxX, box.minY + 0.1, box.maxZ);
        List<VoxelShape> out = new ArrayList<>();
        ship.collisionShapes(feet, out);
        return !out.isEmpty();
    }
}
