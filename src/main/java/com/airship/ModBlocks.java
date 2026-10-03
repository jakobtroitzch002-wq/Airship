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
    public static final ResourceKey<Block> AIRSHIP_CORE_KEY = key("airship_core");
    public static final ResourceKey<Item> AIRSHIP_CORE_ITEM_KEY = itemKey("airship_core");
    public static final ResourceKey<Block> AIRSHIP_BALLOON_KEY = key("airship_balloon");
    public static final ResourceKey<Item> AIRSHIP_BALLOON_ITEM_KEY = itemKey("airship_balloon");
    public static final ResourceKey<Block> AIRSHIP_ENGINE_KEY = key("airship_engine");
    public static final ResourceKey<Item> AIRSHIP_ENGINE_ITEM_KEY = itemKey("airship_engine");
    public static final ResourceKey<Block> AIRSHIP_BUILD_KEY = key("airship_build");
    public static final ResourceKey<Item> AIRSHIP_BUILD_ITEM_KEY = itemKey("airship_build");
    public static final ResourceKey<Block> AIRSHIP_BUILD_REINFORCED_KEY = key("airship_build_reinforced");
    public static final ResourceKey<Item> AIRSHIP_BUILD_REINFORCED_ITEM_KEY = itemKey("airship_build_reinforced");
    public static final ResourceKey<Block> AIRSHIP_BUILD_PANEL_KEY = key("airship_build_panel");
    public static final ResourceKey<Item> AIRSHIP_BUILD_PANEL_ITEM_KEY = itemKey("airship_build_panel");

    public static final Block AIRSHIP_CORE = register(AIRSHIP_CORE_KEY, AirshipCoreBlock::new, BlockBehaviour.Properties.of());
    public static final Block AIRSHIP_BALLOON = register(AIRSHIP_BALLOON_KEY, Block::new, BlockBehaviour.Properties.of());
    public static final Block AIRSHIP_ENGINE = register(AIRSHIP_ENGINE_KEY, Block::new, BlockBehaviour.Properties.of());
    public static final Block AIRSHIP_BUILD = register(AIRSHIP_BUILD_KEY, AirshipBuildBlock::new, BlockBehaviour.Properties.of());
    public static final Block AIRSHIP_BUILD_REINFORCED = register(AIRSHIP_BUILD_REINFORCED_KEY, AirshipBuildBlock::new, BlockBehaviour.Properties.of());
    public static final Block AIRSHIP_BUILD_PANEL = register(AIRSHIP_BUILD_PANEL_KEY, AirshipBuildBlock::new, BlockBehaviour.Properties.of());

    static {
        registerBlockItem(AIRSHIP_CORE, AIRSHIP_CORE_ITEM_KEY);
        registerBlockItem(AIRSHIP_BALLOON, AIRSHIP_BALLOON_ITEM_KEY);
        registerBlockItem(AIRSHIP_ENGINE, AIRSHIP_ENGINE_ITEM_KEY);
        registerBlockItem(AIRSHIP_BUILD, AIRSHIP_BUILD_ITEM_KEY);
        registerBlockItem(AIRSHIP_BUILD_REINFORCED, AIRSHIP_BUILD_REINFORCED_ITEM_KEY);
        registerBlockItem(AIRSHIP_BUILD_PANEL, AIRSHIP_BUILD_PANEL_ITEM_KEY);
    }
    private ModBlocks() {}
    private static ResourceKey<Block> key(String id) { return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, id)); }
    private static ResourceKey<Item> itemKey(String id) { return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, id)); }
    private static Block register(ResourceKey<Block> key, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) { return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key))); }
    private static void registerBlockItem(Block block, ResourceKey<Item> key) { Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix())); }
    public static boolean isAirshipBuildBlock(Block block) { return block == AIRSHIP_BUILD || block == AIRSHIP_BUILD_REINFORCED || block == AIRSHIP_BUILD_PANEL; }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(AIRSHIP_CORE.asItem()); entries.accept(AIRSHIP_BALLOON.asItem()); entries.accept(AIRSHIP_ENGINE.asItem());
            entries.accept(AIRSHIP_BUILD.asItem()); entries.accept(AIRSHIP_BUILD_REINFORCED.asItem()); entries.accept(AIRSHIP_BUILD_PANEL.asItem());
        });
    }
}
