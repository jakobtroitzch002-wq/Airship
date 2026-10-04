package com.airship;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Remembers which terrain blocks touched the ship when it last landed (positions relative to this Core).
 * The structure scan skips them, so a ship that landed against the ground or a wall does not
 * suddenly include the terrain the next time you sit down.
 */
public class AirshipCoreBlockEntity extends BlockEntity {
    private List<BlockPos> terrain = List.of();

    public AirshipCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIRSHIP_CORE, pos, state);
    }

    public List<BlockPos> getTerrain() {
        return terrain;
    }

    public void setTerrain(List<BlockPos> terrain) {
        this.terrain = List.copyOf(terrain);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Terrain", BlockPos.CODEC.listOf(), new ArrayList<>(terrain));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        terrain = input.read("Terrain", BlockPos.CODEC.listOf()).map(List::copyOf).orElse(List.of());
    }
}
