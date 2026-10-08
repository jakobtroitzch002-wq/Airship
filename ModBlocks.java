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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static final ResourceKey<Block> AIRSHIP_CORE_KEY = key("airship_core");
    public static final ResourceKey<Item> AIRSHIP_CORE_ITEM_KEY = itemKey("airship_core");
    public static final ResourceKey<Block> AIRSHIP_BALLOON_KEY = key("airship_balloon");
    public static final ResourceKey<Item> AIRSHIP_BALLOON_ITEM_KEY = itemKey("airship_balloon");
    public static final ResourceKey<Block> AIRSHIP_ENGINE_KEY = key("airship_engine");
    public static final ResourceKey<Item> AIRSHIP_ENGINE_ITEM_KEY = itemKey("airship_engine");
    public static final ResourceKey<Block> AIRSHIP_SEAT_KEY = key("airship_seat");
    public static final ResourceKey<Item> AIRSHIP_SEAT_ITEM_KEY = itemKey("airship_seat");
    public static final ResourceKey<Block> AIRSHIP_BUILD_KEY = key("airship_build");
    public static final ResourceKey<Item> AIRSHIP_BUILD_ITEM_KEY = itemKey("airship_build");

    public static final ResourceKey<Block> AIRSHIP_CORE_WOOD_KEY = key("airship_core_wood");
    public static final ResourceKey<Item> AIRSHIP_CORE_WOOD_ITEM_KEY = itemKey("airship_core_wood");
    public static final ResourceKey<Block> AIRSHIP_CORE_IRON_KEY = key("airship_core_iron");
    public static final ResourceKey<Item> AIRSHIP_CORE_IRON_ITEM_KEY = itemKey("airship_core_iron");
    public static final ResourceKey<Block> AIRSHIP_BALLOON_SIMPLE_KEY = key("airship_balloon_simple");
    public static final ResourceKey<Item> AIRSHIP_BALLOON_SIMPLE_ITEM_KEY = itemKey("airship_balloon_simple");
    public static final ResourceKey<Block> AIRSHIP_ENGINE_SIMPLE_KEY = key("airship_engine_simple");
    public static final ResourceKey<Item> AIRSHIP_ENGINE_SIMPLE_ITEM_KEY = itemKey("airship_engine_simple");

    /** Largest ship of the wooden and the iron Core (the normal Core uses the limit from the config). */
    public static final int WOOD_CORE_BLOCKS = 25;
    public static final int IRON_CORE_BLOCKS = 250;
    /** Lift in half balloons: a Balloon is 2, a Simple Balloon 1. */
    public static final int BALLOON_LIFT = 2;
    public static final int SIMPLE_BALLOON_LIFT = 1;
    /** How much of the fuel speed boost a Simple Engine gives compared to the normal Engine. */
    public static final double SIMPLE_ENGINE_POWER = 0.6;

    public static final Block AIRSHIP_CORE = register(AIRSHIP_CORE_KEY, AirshipCoreBlock::new, BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRSHIP_CORE_WOOD = register(AIRSHIP_CORE_WOOD_KEY, AirshipCoreBlock::new, BlockBehaviour.Properties.of().strength(1.5F, 3.0F).sound(SoundType.WOOD));
    public static final Block AIRSHIP_CORE_IRON = register(AIRSHIP_CORE_IRON_KEY, AirshipCoreBlock::new, BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRSHIP_BALLOON_SIMPLE = register(AIRSHIP_BALLOON_SIMPLE_KEY, Block::new, BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.WOOL));
    public static final Block AIRSHIP_ENGINE_SIMPLE = register(AIRSHIP_ENGINE_SIMPLE_KEY, AirshipEngineBlock::new, BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRSHIP_BALLOON = register(AIRSHIP_BALLOON_KEY, Block::new, BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.WOOL));
    public static final Block AIRSHIP_ENGINE = register(AIRSHIP_ENGINE_KEY, AirshipEngineBlock::new, BlockBehaviour.Properties.of().strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRSHIP_SEAT = register(AIRSHIP_SEAT_KEY, AirshipSeatBlock::new, BlockBehaviour.Properties.of().noOcclusion().strength(1.5F, 3.0F).sound(SoundType.WOOD));
    public static final Block AIRSHIP_BUILD = register(AIRSHIP_BUILD_KEY, AirshipBuildBlock::new, BlockBehaviour.Properties.of().noOcclusion().strength(0.5F).sound(SoundType.GRASS));

    static {
        registerBlockItem(AIRSHIP_CORE, AIRSHIP_CORE_ITEM_KEY);
        registerBlockItem(AIRSHIP_CORE_WOOD, AIRSHIP_CORE_WOOD_ITEM_KEY);
        registerBlockItem(AIRSHIP_CORE_IRON, AIRSHIP_CORE_IRON_ITEM_KEY);
        registerBlockItem(AIRSHIP_BALLOON_SIMPLE, AIRSHIP_BALLOON_SIMPLE_ITEM_KEY);
        registerBlockItem(AIRSHIP_ENGINE_SIMPLE, AIRSHIP_ENGINE_SIMPLE_ITEM_KEY);
        registerBlockItem(AIRSHIP_BALLOON, AIRSHIP_BALLOON_ITEM_KEY);
        registerBlockItem(AIRSHIP_ENGINE, AIRSHIP_ENGINE_ITEM_KEY);
        registerBlockItem(AIRSHIP_SEAT, AIRSHIP_SEAT_ITEM_KEY);
        registerBlockItem(AIRSHIP_BUILD, AIRSHIP_BUILD_ITEM_KEY);
    }

    private ModBlocks() {}
    private static ResourceKey<Block> key(String id) { return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, id)); }
    private static ResourceKey<Item> itemKey(String id) { return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, id)); }
    private static Block register(ResourceKey<Block> key, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) { return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key))); }
    private static void registerBlockItem(Block block, ResourceKey<Item> key) { Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix())); }
    public static boolean isAirshipBuildBlock(Block block) { return block == AIRSHIP_BUILD; }

    public static boolean isCore(BlockState state) {
        return state.is(AIRSHIP_CORE) || state.is(AIRSHIP_CORE_WOOD) || state.is(AIRSHIP_CORE_IRON);
    }

    /** Largest ship, in blocks, that this kind of Core can hold. */
    public static int coreLimit(BlockState core) {
        if (core.is(AIRSHIP_CORE_WOOD)) {
            return Math.min(WOOD_CORE_BLOCKS, AirshipStructureDetector.MAX_BLOCKS);
        }
        if (core.is(AIRSHIP_CORE_IRON)) {
            return Math.min(IRON_CORE_BLOCKS, AirshipStructureDetector.MAX_BLOCKS);
        }
        return AirshipStructureDetector.MAX_BLOCKS;
    }

    /** Lift of a block in half balloons (0 for blocks that give none). */
    public static int liftOf(BlockState state) {
        if (state.is(AIRSHIP_BALLOON)) {
            return BALLOON_LIFT;
        }
        return state.is(AIRSHIP_BALLOON_SIMPLE) ? SIMPLE_BALLOON_LIFT : 0;
    }

    public static boolean isEngine(BlockState state) {
        return state.is(AIRSHIP_ENGINE) || state.is(AIRSHIP_ENGINE_SIMPLE);
    }

    /** Share of the fuel speed boost that this engine gives. */
    public static double enginePower(BlockState state) {
        return state.is(AIRSHIP_ENGINE_SIMPLE) ? SIMPLE_ENGINE_POWER : 1.0;
    }

    public static boolean engineHasTurbo(BlockState state) {
        return state.is(AIRSHIP_ENGINE);
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(AIRSHIP_CORE_WOOD.asItem());
            entries.accept(AIRSHIP_CORE_IRON.asItem());
            entries.accept(AIRSHIP_CORE.asItem());
            entries.accept(AIRSHIP_BALLOON_SIMPLE.asItem());
            entries.accept(AIRSHIP_ENGINE_SIMPLE.asItem());
            entries.accept(AIRSHIP_BALLOON.asItem());
            entries.accept(AIRSHIP_ENGINE.asItem());
            entries.accept(AIRSHIP_SEAT.asItem());
            entries.accept(AIRSHIP_BUILD.asItem());
        });
    }
}
