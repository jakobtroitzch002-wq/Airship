package com.airship;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/** Server -> client: the blocks of one airship entity, sent when a player starts tracking it. */
public record AirshipBlocksPayload(int entityId, List<AirshipClientCell> cells) implements CustomPacketPayload {
    public static final Type<AirshipBlocksPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AirshipMod.MOD_ID, "airship_blocks")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, AirshipBlocksPayload> CODEC =
            new StreamCodec<RegistryFriendlyByteBuf, AirshipBlocksPayload>() {
                @Override
                public AirshipBlocksPayload decode(RegistryFriendlyByteBuf buf) {
                    int entityId = buf.readVarInt();
                    int size = buf.readVarInt();
                    List<AirshipClientCell> cells = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        int x = buf.readShort();
                        int y = buf.readShort();
                        int z = buf.readShort();
                        int stateId = buf.readVarInt();
                        cells.add(new AirshipClientCell(new BlockPos(x, y, z), Block.stateById(stateId)));
                    }
                    return new AirshipBlocksPayload(entityId, cells);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, AirshipBlocksPayload payload) {
                    buf.writeVarInt(payload.entityId());
                    buf.writeVarInt(payload.cells().size());
                    for (AirshipClientCell cell : payload.cells()) {
                        buf.writeShort(cell.pos().getX());
                        buf.writeShort(cell.pos().getY());
                        buf.writeShort(cell.pos().getZ());
                        buf.writeVarInt(Block.getId(cell.state()));
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
