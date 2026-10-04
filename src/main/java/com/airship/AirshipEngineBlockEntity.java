package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores the remaining fuel (in ticks of thrust) of one engine block. */
public class AirshipEngineBlockEntity extends BlockEntity implements MenuProvider {
    /** Tank size in ticks: 20 minutes (a coal block alone is 13:20). Also fits in the 16-bit menu data. */
    public static final int MAX_FUEL = 24000;

    private int fuel;

    /** Lets the menu read the fuel; the menu never writes it through this. */
    private final ContainerData fuelData = new ContainerData() {
        @Override
        public int get(int index) {
            return fuel;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

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
    public Component getDisplayName() {
        return Component.literal("Luftschiff-Antrieb");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AirshipEngineMenu(containerId, inventory, this, fuelData);
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
