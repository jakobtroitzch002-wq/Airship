package com.airship.mixin;

import com.airship.AirshipShipColliders;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sneaking to the edge of a flying ship: the edge check counts the ship's blocks as ground (see
 * {@link AirshipShipColliders#backOffFromEdge}). Optional: without this method only sneaking stays as before.
 */
@Mixin(Player.class)
public abstract class PlayerSneakMixin {
    @Inject(method = "maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void airship$sneakOnShip(Vec3 movement, MoverType type, CallbackInfoReturnable<Vec3> cir) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide() || self.isPassenger() || self.getAbilities().flying || movement.y > 0.0
                || (type != MoverType.SELF && type != MoverType.PLAYER) || !self.isShiftKeyDown()) {
            return;
        }
        Vec3 result = AirshipShipColliders.backOffFromEdge(self, movement, self.maxUpStep());
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
