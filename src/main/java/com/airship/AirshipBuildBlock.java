package com.airship;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

public class AirshipBuildBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty HAS_TEXTURE =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("has_texture");

    private static final String DISPLAY_BLOCK_ID = "airship_display_block";

    public AirshipBuildBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HAS_TEXTURE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_TEXTURE);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack drop = new ItemStack(ModBlocks.AIRSHIP_BUILD);
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (blockEntity instanceof AirshipBuildBlockEntity buildEntity && buildEntity.hasCustomTexture()) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(buildEntity.getDisplayState().getBlock());
            CustomData.update(
                    DataComponents.CUSTOM_DATA,
                    drop,
                    tag -> tag.putString(DISPLAY_BLOCK_ID, id.toString())
            );
        }

        return List.of(drop);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity by,
            ItemStack itemStack
    ) {
        super.setPlacedBy(level, pos, state, by, itemStack);

        if (level.isClientSide()) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof AirshipBuildBlockEntity buildEntity)) {
            return;
        }

        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.copyTag().contains(DISPLAY_BLOCK_ID)) {
            return;
        }

        String blockId = customData.copyTag().getString(DISPLAY_BLOCK_ID).orElse("");
        Identifier id = Identifier.tryParse(blockId);
        if (id == null) {
            return;
        }

        Block block = BuiltInRegistries.BLOCK.get(id).orElse(null);
        if (block == null) {
            return;
        }

        BlockState displayState = block.defaultBlockState();
        if (displayState.isSolidRender()) {
            buildEntity.setDisplayState(displayState);
        }
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack itemStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }

        if (blockItem.getBlock() == ModBlocks.AIRSHIP_BUILD) {
            return InteractionResult.FAIL;
        }

        BlockState newDisplayState = blockItem.getBlock().defaultBlockState();

        if (!newDisplayState.isSolidRender()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof AirshipBuildBlockEntity buildEntity)) {
            return InteractionResult.PASS;
        }

        boolean hadOldTexture = buildEntity.hasCustomTexture();
        BlockState oldDisplayState = buildEntity.getDisplayState();
        buildEntity.setDisplayState(newDisplayState);

        if (!player.isCreative()) {
            itemStack.shrink(1);
        }

        if (hadOldTexture) {
            popResource(level, pos, new ItemStack(oldDisplayState.getBlock()));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirshipBuildBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return null;
    }
}
