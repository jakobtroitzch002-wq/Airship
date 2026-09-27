package com.airship;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AirshipControlPayload(int direction) implements CustomPacketPayload {
    public static final Type<AirshipControlPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_control")
    );

    public static final StreamCodec<
            net.minecraft.network.RegistryFriendlyByteBuf,
            AirshipControlPayload
            > CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    AirshipControlPayload::direction,
                    AirshipControlPayload::new
            );

    public static final int FORWARD = 0;
    public static final int BACKWARD = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
