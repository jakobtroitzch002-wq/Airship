package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores the remaining fuel (in ticks of thrust) of one engine block. */
public class AirshipEngineBlockEntity extends BlockEntity {
    public static final int MAX_FUEL = 1600 * 8;

    private int fuel;

    public AirshipEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIRSHIP_ENGINE, pos, state);
    }

    public int getFuel() {
        return fuel;
    }

    public void setFuel(int fuel) {
        this.fuel = Math.max(0, Math.min(MAX_FUEL, fuel));
        setChanged();
    }

    public void addFuel(int ticks) {
        setFuel(fuel + ticks);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fuel", fuel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel = input.getIntOr("fuel", 0);
    }
}
