package com.airship;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: the player right-clicked the Airship Seat of a hovering ship and wants to steer it. */
public record AirshipBoardPayload(int entityId) implements CustomPacketPayload {
    public static final Type<AirshipBoardPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_board")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AirshipBoardPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AirshipBoardPayload::entityId,
            AirshipBoardPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
