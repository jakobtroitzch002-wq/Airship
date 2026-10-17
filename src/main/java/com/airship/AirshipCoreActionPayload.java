package com.airship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: a button on the Core screen was pressed. */
/**
 * {@code shipId} is the entity id of a flying ship whose Core screen this is, or -1 for a placed Core.
 */
public record AirshipCoreActionPayload(BlockPos pos, int action, int shipId) implements CustomPacketPayload {
    public static final int RECHECK = 0;
    public static final int FORGET_TERRAIN = 1;
    /** Flying ship: snap it to the block grid and turn it into blocks, so it can be rebuilt. */
    public static final int ALIGN = 2;

    public static final Type<AirshipCoreActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_core_action")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AirshipCoreActionPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AirshipCoreActionPayload::pos,
            ByteBufCodecs.VAR_INT, AirshipCoreActionPayload::action,
            ByteBufCodecs.VAR_INT, AirshipCoreActionPayload::shipId,
            AirshipCoreActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
