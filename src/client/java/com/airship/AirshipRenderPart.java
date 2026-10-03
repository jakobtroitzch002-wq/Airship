package com.airship;

import net.minecraft.client.renderer.block.BlockModelRenderState;

/** One visible ship block: its position relative to the Core and its resolved block model. */
public record AirshipRenderPart(int x, int y, int z, BlockModelRenderState model) {}
