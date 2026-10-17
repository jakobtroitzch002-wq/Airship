package com.airship;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Lets players stand and walk on a flying ship. A flying ship is one entity, its blocks are only drawn, so the
 * player's own game checks the player's movement against the ship's blocks (see {@code EntityCollideMixin}): in
 * the ship's own rotated grid, with the blocks' exact shapes, so it matches what is drawn at every angle. A player
 * that stands on the ship is moved along with it ({@link AirshipEntity#carry}).
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

    /** Client only: how far the ship carries the player this tick, added to the player's next movement. */
    private static Vec3 pendingCarry = Vec3.ZERO;

    public static void carryBy(Vec3 shift) {
        pendingCarry = pendingCarry.add(shift);
    }

    /**
     * Called at the start of each client tick: a carry that was not used (the player did not move last tick) is
     * applied directly.
     */
    public static void flushCarry(Entity player) {
        if (!pendingCarry.equals(Vec3.ZERO)) {
            player.setPos(player.position().add(pendingCarry));
            pendingCarry = Vec3.ZERO;
        }
    }

    /**
     * The player's movement this tick: the ship's carry plus the player's own movement (already limited by the
     * world), which the blocks of all flying ships nearby limit too, starting from the carried position.
     */
    public static Vec3 collide(Entity entity, Vec3 movement) {
        Vec3 carry = pendingCarry;
        pendingCarry = Vec3.ZERO;
        AABB box = entity.getBoundingBox().move(carry);
        AABB area = box.expandTowards(movement).inflate(SEARCH);
        Vec3 result = movement;
        for (AirshipEntity ship : entity.level().getEntitiesOfClass(AirshipEntity.class, area)) {
            result = ship.collideEntity(entity, box, result);
        }
        return carry.add(result);
    }

    /** True if the box overlaps a block of any flying ship nearby. */
    public static boolean touchesAnyShip(Entity entity, AABB box) {
        for (AirshipEntity ship : entity.level().getEntitiesOfClass(AirshipEntity.class, box.inflate(SEARCH))) {
            if (ship.touches(box)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sneaking on a ship: Minecraft stops a sneaking player at the edge of the ground, but only knows real blocks
     * there. On a ship the same rule is used with the ship's blocks counted as ground too.
     * Returns null if the player does not stand on a ship (then Minecraft's own rule is used).
     */
    public static Vec3 backOffFromEdge(Entity player, Vec3 movement, double stepHeight) {
        AABB box = player.getBoundingBox();
        if (!touchesAnyShip(player, below(box, 0.0, 0.0, stepHeight))) {
            return null;
        }
        double x = movement.x;
        double z = movement.z;
        double step = 0.05;
        while (x != 0.0 && canFall(player, box, x, 0.0, stepHeight)) {
            x = Math.abs(x) <= step ? 0.0 : x - Math.copySign(step, x);
        }
        while (z != 0.0 && canFall(player, box, 0.0, z, stepHeight)) {
            z = Math.abs(z) <= step ? 0.0 : z - Math.copySign(step, z);
        }
        while (x != 0.0 && z != 0.0 && canFall(player, box, x, z, stepHeight)) {
            x = Math.abs(x) <= step ? 0.0 : x - Math.copySign(step, x);
            z = Math.abs(z) <= step ? 0.0 : z - Math.copySign(step, z);
        }
        return new Vec3(x, movement.y, z);
    }

    /** The strip just below the box, moved sideways. */
    private static AABB below(AABB box, double dx, double dz, double depth) {
        return new AABB(box.minX + dx, box.minY - depth - 1.0E-5, box.minZ + dz,
                box.maxX + dx, box.minY, box.maxZ + dz).deflate(1.0E-7, 0.0, 1.0E-7);
    }

    /** True if there is neither a real block nor a ship block under the moved box. */
    private static boolean canFall(Entity player, AABB box, double dx, double dz, double depth) {
        AABB strip = below(box, dx, dz, depth);
        return player.level().noCollision(player, strip) && !touchesAnyShip(player, strip);
    }

    /** True if one of the ship's blocks is below the entity, at most {@code depth} blocks down. */
    public static boolean isAbove(AirshipEntity ship, Entity entity, double depth) {
        AABB box = entity.getBoundingBox();
        return ship.touches(new AABB(box.minX, box.minY - depth, box.minZ, box.maxX, box.minY, box.maxZ));
    }

    /** True if an entity's feet rest on (or are just inside) one of the ship's blocks. */
    public static boolean standsOn(AirshipEntity ship, Entity entity) {
        AABB box = entity.getBoundingBox();
        return ship.touches(new AABB(box.minX, box.minY - 0.25, box.minZ, box.maxX, box.minY + 0.1, box.maxZ));
    }
}
