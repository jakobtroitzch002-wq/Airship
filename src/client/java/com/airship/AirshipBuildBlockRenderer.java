package com.airship;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;

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
            net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(
                blockEntity, state, partialTick, cameraPosition, breakProgress
        );

        state.hasCustomTexture = blockEntity.hasCustomTexture();

        // BlockModelResolver.update() clears the previous model before filling it.
        // When the build block is empty, nothing is submitted here. When it is
        // filled, the only geometry submitted by this renderer is the contained block.
        if (state.hasCustomTexture) {
            blockModelResolver.update(
                    state.blockModel,
                    blockEntity.getDisplayState(),
                    DISPLAY_CONTEXT
            );
        } else {
            state.blockModel.clear();
        }
    }

    @Override
    public void submit(
            AirshipBuildBlockRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        if (!state.hasCustomTexture || state.blockModel.isEmpty()) {
            return;
        }

        // Render the contained block slightly inset so its faces cannot z-fight
        // with the boundary block or leak texture fragments around its edges.
        poseStack.pushPose();
        poseStack.translate(0.01F, 0.01F, 0.01F);
        state.blockModel.submit(
                poseStack,
                collector,
                15728880,
                OverlayTexture.NO_OVERLAY,
                0
        );
        poseStack.popPose();
    }
}
