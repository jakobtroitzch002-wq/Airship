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
import net.minecraft.world.level.block.state.BlockState;

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
    public static final ResourceKey<Block> AIRSHIP_CORE_LARGE_KEY = key("airship_core_large");
    public static final ResourceKey<Item> AIRSHIP_CORE_LARGE_ITEM_KEY = itemKey("airship_core_large");
    public static final ResourceKey<Block> AIRSHIP_CORE_COLOSSAL_KEY = key("airship_core_colossal");
    public static final ResourceKey<Item> AIRSHIP_CORE_COLOSSAL_ITEM_KEY = itemKey("airship_core_colossal");
    public static final ResourceKey<Block> AIRSHIP_CORE_IRON_KEY = key("airship_core_iron");
    public static final ResourceKey<Item> AIRSHIP_CORE_IRON_ITEM_KEY = itemKey("airship_core_iron");
    public static final ResourceKey<Block> AIRSHIP_BALLOON_SIMPLE_KEY = key("airship_balloon_simple");
    public static final ResourceKey<Item> AIRSHIP_BALLOON_SIMPLE_ITEM_KEY = itemKey("airship_balloon_simple");
    public static final ResourceKey<Block> AIRSHIP_BALLOON_REINFORCED_KEY = key("airship_balloon_reinforced");
    public static final ResourceKey<Item> AIRSHIP_BALLOON_REINFORCED_ITEM_KEY = itemKey("airship_balloon_reinforced");
    public static final ResourceKey<Block> AIRSHIP_BALLOON_LARGE_KEY = key("airship_balloon_large");
    public static final ResourceKey<Item> AIRSHIP_BALLOON_LARGE_ITEM_KEY = itemKey("airship_balloon_large");
    public static final ResourceKey<Block> AIRSHIP_BALLOON_SKY_KEY = key("airship_balloon_sky");
    public static final ResourceKey<Item> AIRSHIP_BALLOON_SKY_ITEM_KEY = itemKey("airship_balloon_sky");
    public static final ResourceKey<Block> AIRSHIP_ENGINE_ADVANCED_KEY = key("airship_engine_advanced");
    public static final ResourceKey<Item> AIRSHIP_ENGINE_ADVANCED_ITEM_KEY = itemKey("airship_engine_advanced");
    public static final ResourceKey<Block> AIRSHIP_ENGINE_SIMPLE_KEY = key("airship_engine_simple");
    public static final ResourceKey<Item> AIRSHIP_ENGINE_SIMPLE_ITEM_KEY = itemKey("airship_engine_simple");

    /** Largest ship of each Core (never more than {@code maxBlocks} from the config). */
    public static final int WOOD_CORE_BLOCKS = 25;
    public static final int IRON_CORE_BLOCKS = 100;
    public static final int CORE_BLOCKS = 250;
    /** Ship blocks a Simple Balloon carries (a normal Balloon carries {@code blocksPerBalloon} from the config). */
    public static final int SIMPLE_BALLOON_CARRY = 3;
    /** How much of the fuel speed boost a Simple Engine gives compared to the normal Engine. */
    public static final double SIMPLE_ENGINE_POWER = 0.6;
    /** How much of the fuel speed boost an Advanced Engine gives compared to the normal Engine. */
    public static final double ADVANCED_ENGINE_POWER = 7.0; // 20 blocks per second with fuel (default config)
    /** The Advanced Engine's turbo tank is this many times as big ... */
    public static final int ADVANCED_TURBO_TANK = 2;
    /** A Simple Engine burns this many ticks of fuel per tick of thrust (the normal Engine burns 1). */
    public static final int SIMPLE_ENGINE_BURN_RATE = 2;

    public static final Block AIRSHIP_CORE = register(AIRSHIP_CORE_KEY, AirshipCoreBlock::new, BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRSHIP_CORE_WOOD = register(AIRSHIP_CORE_WOOD_KEY, AirshipCoreBlock::new, BlockBehaviour.Properties.of().strength(1.5F, 3.0F).sound(SoundType.WOOD));
    public static final Block AIRSHIP_CORE_LARGE = register(AIRSHIP_CORE_LARGE_KEY, AirshipBigCoreBlock.Large::new, BlockBehaviour.Properties.of().strength(5.0F, 12.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRSHIP_CORE_COLOSSAL = register(AIRSHIP_CORE_COLOSSAL_KEY, AirshipBigCoreBlock.Colossal::new, BlockBehaviour.Properties.of().strength(6.0F, 20.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().lightLevel(state -> 7));
    public static final Block AIRSHIP_CORE_IRON = register(AIRSHIP_CORE_IRON_KEY, AirshipCoreBlock::new, BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRSHIP_BALLOON_SIMPLE = register(AIRSHIP_BALLOON_SIMPLE_KEY, AirshipBalloonBlock::new, BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.WOOL));
    public static final Block AIRSHIP_ENGINE_SIMPLE = register(AIRSHIP_ENGINE_SIMPLE_KEY, AirshipEngineBlock::new, BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRSHIP_BALLOON = register(AIRSHIP_BALLOON_KEY, AirshipBalloonBlock::new, BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.WOOL));
    public static final Block AIRSHIP_BALLOON_LARGE = register(AIRSHIP_BALLOON_LARGE_KEY, AirshipBalloonBlock::new, BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.WOOL));
    public static final Block AIRSHIP_BALLOON_SKY = register(AIRSHIP_BALLOON_SKY_KEY, AirshipBalloonBlock::new, BlockBehaviour.Properties.of().strength(2.0F, 4.0F).sound(SoundType.WOOL));
    public static final Block AIRSHIP_ENGINE_ADVANCED = register(AIRSHIP_ENGINE_ADVANCED_KEY, AirshipEngineBlock::new, BlockBehaviour.Properties.of().strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRSHIP_BALLOON_REINFORCED = register(AIRSHIP_BALLOON_REINFORCED_KEY, AirshipBalloonBlock::new, BlockBehaviour.Properties.of().strength(1.5F, 3.0F).sound(SoundType.WOOL));
    public static final Block AIRSHIP_ENGINE = register(AIRSHIP_ENGINE_KEY, AirshipEngineBlock::new, BlockBehaviour.Properties.of().strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final Block AIRSHIP_SEAT = register(AIRSHIP_SEAT_KEY, AirshipSeatBlock::new, BlockBehaviour.Properties.of().noOcclusion().strength(1.5F, 3.0F).sound(SoundType.WOOD));
    public static final Block AIRSHIP_BUILD = register(AIRSHIP_BUILD_KEY, AirshipBuildBlock::new, BlockBehaviour.Properties.of().noOcclusion().strength(0.5F).sound(SoundType.GRASS));

    static {
        registerBlockItem(AIRSHIP_CORE, AIRSHIP_CORE_ITEM_KEY);
        registerBlockItem(AIRSHIP_CORE_WOOD, AIRSHIP_CORE_WOOD_ITEM_KEY);
        registerBlockItem(AIRSHIP_CORE_IRON, AIRSHIP_CORE_IRON_ITEM_KEY);
        registerBlockItem(AIRSHIP_CORE_LARGE, AIRSHIP_CORE_LARGE_ITEM_KEY);
        registerBlockItem(AIRSHIP_CORE_COLOSSAL, AIRSHIP_CORE_COLOSSAL_ITEM_KEY);
        registerBlockItem(AIRSHIP_BALLOON_SIMPLE, AIRSHIP_BALLOON_SIMPLE_ITEM_KEY);
        registerBlockItem(AIRSHIP_ENGINE_SIMPLE, AIRSHIP_ENGINE_SIMPLE_ITEM_KEY);
        registerBlockItem(AIRSHIP_BALLOON, AIRSHIP_BALLOON_ITEM_KEY);
        registerBlockItem(AIRSHIP_BALLOON_REINFORCED, AIRSHIP_BALLOON_REINFORCED_ITEM_KEY);
        registerBlockItem(AIRSHIP_BALLOON_LARGE, AIRSHIP_BALLOON_LARGE_ITEM_KEY);
        registerBlockItem(AIRSHIP_BALLOON_SKY, AIRSHIP_BALLOON_SKY_ITEM_KEY);
        registerBlockItem(AIRSHIP_ENGINE_ADVANCED, AIRSHIP_ENGINE_ADVANCED_ITEM_KEY);
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
        if (state.getBlock() instanceof AirshipBigCoreBlock big) {
            return big.isMaster(state); // only one block of a big Core is the Core itself
        }
        return state.is(AIRSHIP_CORE) || state.is(AIRSHIP_CORE_WOOD) || state.is(AIRSHIP_CORE_IRON);
    }

    /** Any block of a Core, including all parts of a big Core (right-click opens the Core screen). */
    public static boolean isCoreBlock(BlockState state) {
        return isCore(state) || state.getBlock() instanceof AirshipBigCoreBlock;
    }

    /** Largest ship, in blocks, that this kind of Core can hold. */
    public static int coreLimit(BlockState core) {
        if (core.getBlock() instanceof AirshipBigCoreBlock big) {
            return Math.min(big.limit(), AirshipStructureDetector.MAX_BLOCKS);
        }
        if (core.is(AIRSHIP_CORE_WOOD)) {
            return Math.min(WOOD_CORE_BLOCKS, AirshipStructureDetector.MAX_BLOCKS);
        }
        if (core.is(AIRSHIP_CORE_IRON)) {
            return Math.min(IRON_CORE_BLOCKS, AirshipStructureDetector.MAX_BLOCKS);
        }
        return Math.min(CORE_BLOCKS, AirshipStructureDetector.MAX_BLOCKS);
    }

    /** Lift of a block: how many ship blocks it carries (0 for blocks that give none). */
    public static int liftOf(BlockState state) {
        if (state.is(AIRSHIP_BALLOON_LARGE)) {
            return AirshipLift.BLOCKS_PER_BALLOON * 8 / 5; // 8 with the default of 5
        }
        if (state.is(AIRSHIP_BALLOON_SKY)) {
            return AirshipLift.BLOCKS_PER_BALLOON * 12 / 5; // 12 with the default of 5
        }
        if (state.is(AIRSHIP_BALLOON) || state.is(AIRSHIP_BALLOON_REINFORCED)) {
            return AirshipLift.BLOCKS_PER_BALLOON;
        }
        return state.is(AIRSHIP_BALLOON_SIMPLE) ? Math.min(SIMPLE_BALLOON_CARRY, AirshipLift.BLOCKS_PER_BALLOON) : 0;
    }

    public static boolean isEngine(BlockState state) {
        return state.is(AIRSHIP_ENGINE) || state.is(AIRSHIP_ENGINE_SIMPLE) || state.is(AIRSHIP_ENGINE_ADVANCED);
    }

    /** Share of the fuel speed boost that this engine gives. */
    public static double enginePower(BlockState state) {
        if (state.is(AIRSHIP_ENGINE_ADVANCED)) {
            return ADVANCED_ENGINE_POWER;
        }
        return state.is(AIRSHIP_ENGINE_SIMPLE) ? SIMPLE_ENGINE_POWER : 1.0;
    }

    /** Ticks of fuel this engine burns per tick of thrust. */
    public static int engineBurnRate(BlockState state) {
        return state != null && state.is(AIRSHIP_ENGINE_SIMPLE) ? SIMPLE_ENGINE_BURN_RATE : 1;
    }

    /** Size of this engine's turbo tank, in ticks (0 without turbo). */
    public static int maxTurbo(BlockState state) {
        if (!engineHasTurbo(state)) {
            return 0;
        }
        return AirshipEngineBlockEntity.MAX_TURBO * (state.is(AIRSHIP_ENGINE_ADVANCED) ? ADVANCED_TURBO_TANK : 1);
    }

    /** ... and burns turbo fuel this many ticks per tick of turbo (half as much as the normal Engine). */
    public static int turboBurnRate(BlockState state) {
        int rate = AirshipEntity.TURBO_BURN_RATE;
        return state != null && state.is(AIRSHIP_ENGINE_ADVANCED) ? Math.max(1, rate / 2) : rate;
    }

    public static boolean engineHasTurbo(BlockState state) {
        return state.is(AIRSHIP_ENGINE) || state.is(AIRSHIP_ENGINE_ADVANCED);
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(AIRSHIP_CORE_WOOD.asItem());
            entries.accept(AIRSHIP_CORE_IRON.asItem());
            entries.accept(AIRSHIP_CORE.asItem());
            entries.accept(AIRSHIP_CORE_LARGE.asItem());
            entries.accept(AIRSHIP_CORE_COLOSSAL.asItem());
            entries.accept(AIRSHIP_BALLOON_SIMPLE.asItem());
            entries.accept(AIRSHIP_ENGINE_SIMPLE.asItem());
            entries.accept(AIRSHIP_BALLOON.asItem());
            entries.accept(AIRSHIP_BALLOON_REINFORCED.asItem());
            entries.accept(AIRSHIP_BALLOON_LARGE.asItem());
            entries.accept(AIRSHIP_BALLOON_SKY.asItem());
            entries.accept(AIRSHIP_ENGINE.asItem());
            entries.accept(AIRSHIP_ENGINE_ADVANCED.asItem());
            entries.accept(AIRSHIP_SEAT.asItem());
            entries.accept(AIRSHIP_BUILD.asItem());
        });
    }
}
