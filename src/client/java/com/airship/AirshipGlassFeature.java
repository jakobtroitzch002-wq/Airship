package com.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.fabricmc.fabric.api.client.renderer.v1.render.ChunkSectionLayerHelper;
import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

/**
 * Draws the see-through blocks of a ship (glass, ice, ...) in the "after terrain" phase.
 * <p>
 * Minecraft draws entities, and so the ship, before the see-through terrain (water). Glass drawn together with the
 * ship also writes depth, so the water behind it was cut away. In the after-terrain phase the water is already
 * drawn, so the glass is simply blended over it.
 */
final class AirshipGlassFeature {
    static final FeatureRendererType<GlassSubmit> TYPE = FeatureRendererType.create("airship_glass");
    private static final Direction[] DIRECTIONS = Direction.values();

    private AirshipGlassFeature() {}

    static void register() {
        FeatureRendererRegistry.register(TYPE, Renderer::new);
    }

    /** One glass block: its pose (relative to the camera), its model parts and the light to draw it with. */
    record GlassSubmit(PoseStack.Pose pose, List<BlockStateModelPart> parts, int light) implements SubmitNode {
        @Override
        public FeatureRendererType<? extends SubmitNode> featureType() {
            return TYPE;
        }
    }

    private static class Renderer extends RenderTypeFeatureRenderer<GlassSubmit> {
        private final QuadInstance quadInstance = new QuadInstance();

        @Override
        protected void buildGroup(FeatureFrameContext context, List<GlassSubmit> submits) {
            for (GlassSubmit submit : submits) {
                quadInstance.setLightCoords(submit.light());
                quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
                quadInstance.setColor(0xFFFFFFFF);
                for (BlockStateModelPart part : submit.parts()) {
                    for (Direction direction : DIRECTIONS) {
                        putQuads(part.getQuads(direction), submit.pose());
                    }
                    putQuads(part.getQuads(null), submit.pose());
                }
            }
        }

        private void putQuads(List<BakedQuad> quads, PoseStack.Pose pose) {
            for (BakedQuad quad : quads) {
                VertexConsumer buffer = getVertexBuilder(
                        ChunkSectionLayerHelper.getMovingBlockRenderType(quad.materialInfo().layer()));
                buffer.putBakedQuad(pose, quad, quadInstance);
            }
        }
    }
}
