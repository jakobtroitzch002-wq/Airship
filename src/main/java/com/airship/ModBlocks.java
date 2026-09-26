package com.airship;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static final ResourceKey<Block> AIRSHIP_CORE_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_core")
    );

    public static final ResourceKey<Item> AIRSHIP_CORE_ITEM_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_core")
    );

    public static final Block AIRSHIP_CORE = register(
            AIRSHIP_CORE_KEY,
            Block::new,
            BlockBehaviour.Properties.of()
    );

    static {
        BlockItem blockItem = new BlockItem(
                AIRSHIP_CORE,
                new Item.Properties()
                        .setId(AIRSHIP_CORE_ITEM_KEY)
                        .useBlockDescriptionPrefix()
        );
        Registry.register(BuiltInRegistries.ITEM, AIRSHIP_CORE_ITEM_KEY, blockItem);
    }

    private ModBlocks() {
    }

    private static Block register(
            ResourceKey<Block> key,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties
    ) {
        Block block = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    public static void initialize() {
        // Forces class initialization and therefore registry registration.
    }
}
