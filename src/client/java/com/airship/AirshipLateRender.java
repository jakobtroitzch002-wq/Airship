package com.airship;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the see-through blocks of airships (glass, ice, ...) <em>after</em> the water.
 * <p>
 * Minecraft draws entities, and so the ship, before the see-through terrain (water). Glass drawn as part of an
 * entity also writes depth, so the water behind it was cut away and vanished. Fabric offers a stage after the
 * see-through terrain for exactly this, so the see-through ship parts are collected while the ship is extracted
 * and drawn there.
 */
final class AirshipLateRender {
    /** One ship's see-through parts for this frame. */
    record Entry(Vec3 pos, float yaw, int light, List<AirshipRenderPart> parts) {}

    private static final int MAX_ENTRIES = 64;
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private AirshipLateRender() {}

    static void register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(AirshipLateRender::render);
    }

    static void add(Entry entry) {
        if (ENTRIES.size() < MAX_ENTRIES) {
            ENTRIES.add(entry);
        }
    }

    private static void render(LevelRenderContext context) {
        if (ENTRIES.isEmpty()) {
            return;
        }
        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack poseStack = context.poseStack();
        for (Entry entry : ENTRIES) {
            poseStack.pushPose();
            poseStack.translate(entry.pos().x - camera.x, entry.pos().y - camera.y, entry.pos().z - camera.z);
            poseStack.rotate(Axis.YP, -entry.yaw() * Mth.DEG_TO_RAD);
            for (AirshipRenderPart part : entry.parts()) {
                poseStack.pushPose();
                poseStack.translate(part.x() - 0.5F, part.y(), part.z() - 0.5F);
                part.model().submit(poseStack, context.submitNodeCollector(), entry.light(),
                        OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        ENTRIES.clear();
    }
}
