package com.airship;

import java.util.List;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class AirshipEntityRenderState extends EntityRenderState {
    public List<AirshipRenderPart> parts = List.of();
}
