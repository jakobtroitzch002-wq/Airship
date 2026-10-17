package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: the player right-clicked a block of a flying or hovering ship (its Airship Seat, an engine or
 * the Core). {@code cell} is the block's position relative to the ship's Core.
 */
public record AirshipShipClickPayload(int entityId, BlockPos cell) implements CustomPacketPayload {
    public static final Type<AirshipShipClickPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_ship_click")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AirshipShipClickPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AirshipShipClickPayload::entityId,
            BlockPos.STREAM_CODEC, AirshipShipClickPayload::cell,
            AirshipShipClickPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
