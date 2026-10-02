package com.airship;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;

public class AirshipBuildBlockRenderer implements BlockEntityRenderer<
        AirshipBuildBlockEntity,
        AirshipBuildBlockRenderState
        > {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver blockModelResolver;

    public AirshipBuildBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public AirshipBuildBlockRenderState createRenderState() {
        return new AirshipBuildBlockRenderState();
    }

    @Override
    public void extractRenderState(
            AirshipBuildBlockEntity blockEntity,
            AirshipBuildBlockRenderState state,
            float partialTick,
            net.minecraft.world.phys.Vec3 cameraPosition,
            BlockEntityRenderState.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity, state, partialTick, cameraPosition, breakProgress
        );

        blockModelResolver.update(
                state.blockModel,
                blockEntity.getDisplayState(),
                DISPLAY_CONTEXT
        );
    }

    @Override
    public void submit(
            AirshipBuildBlockRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        state.blockModel.submit(
                poseStack,
                collector,
                15728880,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                0
        );
    }
}
