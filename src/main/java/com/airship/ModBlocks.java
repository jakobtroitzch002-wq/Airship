package com.airship;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
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

    public static final ResourceKey<Block> AIRSHIP_BALLOON_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_balloon")
    );

    public static final ResourceKey<Item> AIRSHIP_BALLOON_ITEM_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_balloon")
    );

    public static final ResourceKey<Block> AIRSHIP_ENGINE_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_engine")
    );

    public static final ResourceKey<Item> AIRSHIP_ENGINE_ITEM_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_engine")
    );

    public static final Block AIRSHIP_CORE = register(
            AIRSHIP_CORE_KEY,
            AirshipCoreBlock::new,
            BlockBehaviour.Properties.of()
    );

    public static final Block AIRSHIP_BALLOON = register(
            AIRSHIP_BALLOON_KEY,
            Block::new,
            BlockBehaviour.Properties.of()
    );

    public static final Block AIRSHIP_ENGINE = register(
            AIRSHIP_ENGINE_KEY,
            Block::new,
            BlockBehaviour.Properties.of()
    );

    static {
        registerBlockItem(AIRSHIP_CORE, AIRSHIP_CORE_ITEM_KEY);
        registerBlockItem(AIRSHIP_BALLOON, AIRSHIP_BALLOON_ITEM_KEY);
        registerBlockItem(AIRSHIP_ENGINE, AIRSHIP_ENGINE_ITEM_KEY);
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

    private static void registerBlockItem(Block block, ResourceKey<Item> key) {
        BlockItem blockItem = new BlockItem(
                block,
                new Item.Properties()
                        .setId(key)
                        .useBlockDescriptionPrefix()
        );
        Registry.register(BuiltInRegistries.ITEM, key, blockItem);
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(AIRSHIP_CORE.asItem());
            entries.accept(AIRSHIP_BALLOON.asItem());
            entries.accept(AIRSHIP_ENGINE.asItem());
        });
    }
}
