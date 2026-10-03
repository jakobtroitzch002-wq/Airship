package com.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Renders all blocks of an airship. The resolved block models are cached per entity
 * and only rebuilt when the ship's block data changes.
 */
public class AirshipEntityRenderer extends EntityRenderer<AirshipEntity, AirshipEntityRenderState> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    private record Cache(int version, List<AirshipRenderPart> parts) {}

    private final BlockModelResolver blockModelResolver;
    private final Map<AirshipEntity, Cache> caches = new WeakHashMap<>();

    public AirshipEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
    }

    @Override
    public AirshipEntityRenderState createRenderState() {
        return new AirshipEntityRenderState();
    }

    @Override
    public void extractRenderState(AirshipEntity entity, AirshipEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        Cache cache = caches.get(entity);
        if (cache == null || cache.version() != entity.getCellVersion()) {
            List<AirshipRenderPart> parts = new ArrayList<>();
            for (AirshipClientCell cell : entity.getExposedCells()) {
                BlockModelRenderState model = new BlockModelRenderState();
                blockModelResolver.update(model, cell.state(), DISPLAY_CONTEXT);
                parts.add(new AirshipRenderPart(cell.pos().getX(), cell.pos().getY(), cell.pos().getZ(), model));
            }
            cache = new Cache(entity.getCellVersion(), List.copyOf(parts));
            caches.put(entity, cache);
        }
        state.parts = cache.parts();
    }

    @Override
    public void submit(
            AirshipEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        for (AirshipRenderPart part : state.parts) {
            poseStack.pushPose();
            // The entity position is the centre of the Core block's footprint.
            poseStack.translate(part.x() - 0.5F, part.y(), part.z() - 0.5F);
            part.model().submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }

    /** The entity's own bounding box is tiny, so never cull the ship by it. */
    @Override
    public boolean shouldRender(AirshipEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }
}
