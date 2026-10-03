package com.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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

        // Blend between the last two tick positions so the ship moves smoothly.
        Vec3 smooth = entity.getSmoothPos(partialTick);
        Vec3 base = entity.position();
        state.offsetX = smooth.x - base.x;
        state.offsetY = smooth.y - base.y;
        state.offsetZ = smooth.z - base.z;
        state.yaw = entity.getSmoothYaw(partialTick);
    }

    @Override
    public void submit(
            AirshipEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        poseStack.pushPose();
        poseStack.translate((float) state.offsetX, (float) state.offsetY, (float) state.offsetZ);
        // Ship yaw 0 = unrotated; positive yaw turns the ship clockwise seen from above.
        poseStack.rotate(Axis.YP, -state.yaw * Mth.DEG_TO_RAD);
        for (AirshipRenderPart part : state.parts) {
            poseStack.pushPose();
            // The entity position is the centre of the Core block's footprint.
            poseStack.translate(part.x() - 0.5F, part.y(), part.z() - 0.5F);
            part.model().submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
