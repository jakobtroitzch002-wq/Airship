package com.airship;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<AirshipBuildBlockEntity> AIRSHIP_BUILD = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_build"),
            FabricBlockEntityTypeBuilder.create(AirshipBuildBlockEntity::new, ModBlocks.AIRSHIP_BUILD).build()
    );

    private ModBlockEntities() {}
    public static void initialize() {}
}
