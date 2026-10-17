package com.airship.mixin;

import com.airship.AirshipShipColliders;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * After Minecraft limited a movement by the world (blocks, world border, stepping up), the blocks of flying ships
 * limit it too (see {@link AirshipShipColliders}).
 */
@Mixin(Entity.class)
public abstract class EntityCollideMixin {
    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"), cancellable = true)
    private void airship$collideWithShips(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (!AirshipShipColliders.enter(self, self.level())) {
            return;
        }
        try {
            Vec3 vanilla = cir.getReturnValue();
            Vec3 limited = AirshipShipColliders.collide(self, vanilla);
            if (!limited.equals(vanilla)) {
                cir.setReturnValue(limited);
            }
        } finally {
            AirshipShipColliders.exit();
        }
    }
}
