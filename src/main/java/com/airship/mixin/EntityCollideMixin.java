package com.airship.mixin;

import com.airship.AirshipShipColliders;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the blocks of flying ships to the player's collision (see {@link AirshipShipColliders}). */
@Mixin(Entity.class)
public abstract class EntityCollideMixin {
    @Inject(method = "collideBoundingBox", at = @At("HEAD"), cancellable = true)
    private static void airship$collideWithShips(@Nullable Entity entity, Vec3 movement, AABB box, Level level,
            List<VoxelShape> shapes, CallbackInfoReturnable<Vec3> cir) {
        if (!AirshipShipColliders.enter(entity, level)) {
            return;
        }
        try {
            List<VoxelShape> ships = AirshipShipColliders.shapes(level, box.expandTowards(movement));
            if (!ships.isEmpty()) {
                List<VoxelShape> all = new ArrayList<>(shapes);
                all.addAll(ships);
                cir.setReturnValue(Entity.collideBoundingBox(entity, movement, box, level, all));
            }
        } finally {
            AirshipShipColliders.exit();
        }
    }

    /**
     * Stepping up (walking onto a slab, a stair or a block edge) collects its obstacles separately. Without the
     * ship there, walking against a wall of the ship could "step" the player into it. Optional: if this method
     * does not exist in this Minecraft version, only the injection above is used.
     */
    @Inject(method = "collectColliders(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private static void airship$collectShips(@Nullable Entity entity, Level level, List<VoxelShape> collisions,
            AABB box, CallbackInfoReturnable<List<VoxelShape>> cir) {
        if (!AirshipShipColliders.enter(entity, level)) {
            return;
        }
        try {
            List<VoxelShape> ships = AirshipShipColliders.shapes(level, box);
            if (!ships.isEmpty()) {
                List<VoxelShape> all = new ArrayList<>(cir.getReturnValue());
                all.addAll(ships);
                cir.setReturnValue(all);
            }
        } finally {
            AirshipShipColliders.exit();
        }
    }
}
