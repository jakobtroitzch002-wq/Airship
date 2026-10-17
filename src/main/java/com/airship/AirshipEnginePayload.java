package com.airship;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client: whether a ship's engines are running (for their smoke): {@link #OFF}, {@link #RUNNING} or
 * {@link #TURBO}. Sent when it changes, plus a heartbeat while they run.
 */
public record AirshipEnginePayload(int entityId, int state) implements CustomPacketPayload {
    public static final int OFF = 0;
    public static final int RUNNING = 1;
    public static final int TURBO = 2;

    public static final Type<AirshipEnginePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_engines")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AirshipEnginePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AirshipEnginePayload::entityId,
            ByteBufCodecs.VAR_INT, AirshipEnginePayload::state,
            AirshipEnginePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
