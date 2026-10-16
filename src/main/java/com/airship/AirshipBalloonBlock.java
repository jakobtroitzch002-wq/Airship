package com.airship;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A placed balloon. A projectile that is strong enough (see {@link AirshipBalloons#pops}) pops it: the block is
 * destroyed without a drop. Balloons of a flying ship are handled by {@link AirshipEntity}.
 */
public class AirshipBalloonBlock extends Block {
    public AirshipBalloonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel && AirshipBalloons.pops(state, projectile)) {
            level.destroyBlock(hit.getBlockPos(), false);
        }
    }
}
