package com.airship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;

/** Turns a block structure into a flying {@link AirshipEntity} and back. */
public final class AirshipAssembler {
    /**
     * Placing/removing ship blocks must not drop items, pop off torches or trigger shape updates,
     * otherwise attached blocks (torches, buttons, carpets, ...) would break while the ship is moved.
     */
    private static final int WRITE_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private static final List<int[]> LANDING_OFFSETS = buildLandingOffsets();

    private AirshipAssembler() {}

    // ------------------------------------------------------------------ assemble

    public static InteractionResult assemble(Level level, BlockPos corePos, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        AirshipStructureDetector.DetectionResult detection = AirshipStructureDetector.detect(level, corePos);
        if (detection.capped()) {
            tell(player, "Airship is too large (maximum " + AirshipStructureDetector.MAX_BLOCKS + " blocks)");
            return InteractionResult.SUCCESS;
        }

        Set<BlockPos> blocks = detection.blocks();
        int balloons = 0;
        int seatCount = 0;
        int engines = 0;
        for (BlockPos pos : blocks) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.AIRSHIP_BALLOON)) {
                balloons++;
            } else if (state.is(ModBlocks.AIRSHIP_SEAT)) {
                seatCount++;
            } else if (state.is(ModBlocks.AIRSHIP_ENGINE)) {
                engines++;
            }
        }

        int required = AirshipLift.requiredBalloons(blocks.size());
        tell(player, "Airship: " + blocks.size() + " blocks, " + balloons + "/" + required
                + " balloons, " + seatCount + " seats, " + engines + " engines");

        if (!AirshipLift.hasEnoughLift(balloons, required)) {
            tell(player, "Airship cannot fly: not enough balloons (1 per "
                    + AirshipLift.BLOCKS_PER_BALLOON + " blocks)");
            return InteractionResult.SUCCESS;
        }
        if (seatCount == 0) {
            tell(player, "Airship cannot fly: it needs at least one seat");
            return InteractionResult.SUCCESS;
        }

        // Everybody standing on the ship must get a seat, otherwise they would fall through the deck.
        Set<ServerPlayer> riders = new LinkedHashSet<>();
        if (player instanceof ServerPlayer starter) {
            riders.add(starter);
        }
        AABB area = boundsOf(blocks);
        for (ServerPlayer other : serverLevel.getEntitiesOfClass(ServerPlayer.class, area, p -> isOnShip(p, blocks))) {
            riders.add(other);
        }
        if (riders.size() > seatCount) {
            tell(player, "Not enough seats: " + riders.size() + " players on the airship, " + seatCount + " seats");
            return InteractionResult.SUCCESS;
        }

        // Snapshot all blocks, then remove them from the world.
        List<AirshipCell> cells = new ArrayList<>(blocks.size());
        for (BlockPos pos : blocks) {
            BlockState state = level.getBlockState(pos);
            Optional<CompoundTag> blockEntityTag = Optional.empty();
            int fuel = 0;

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AirshipEngineBlockEntity engine) {
                fuel = engine.getFuel();
            } else if (blockEntity != null) {
                blockEntityTag = Optional.of(blockEntity.saveWithFullMetadata(level.registryAccess()));
                if (blockEntity instanceof Clearable clearable) {
                    clearable.clearContent(); // otherwise chests would drop their items when removed
                }
            }

            BlockPos relative = new BlockPos(
                    pos.getX() - corePos.getX(),
                    pos.getY() - corePos.getY(),
                    pos.getZ() - corePos.getZ());
            cells.add(new AirshipCell(relative, state, blockEntityTag, fuel));
        }
        for (BlockPos pos : blocks) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), WRITE_FLAGS);
        }

        AirshipEntity ship = new AirshipEntity(ModEntities.AIRSHIP, level);
        ship.setPos(corePos.getX() + 0.5, corePos.getY(), corePos.getZ() + 0.5);
        ship.setCells(cells);
        serverLevel.addFreshEntity(ship);

        for (ServerPlayer rider : riders) {
            rider.startRiding(ship, true, true);
        }

        tell(player, "Airship ready. W/A/S/D move, Space up, Ctrl down, Shift leaves the seat. Land: stand next to it and sneak + right-click.");
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ disassemble

    /**
     * Turns the ship back into blocks, snapped to the nearest 90 degrees.
     * Returns false if there is no free space to land.
     */
    public static boolean disassemble(ServerLevel level, AirshipEntity ship) {
        List<AirshipCell> cells = snappedCells(ship);
        BlockPos anchor = findLandingAnchor(level, ship, cells);
        if (anchor == null) {
            return false;
        }

        for (AirshipCell cell : cells) {
            level.setBlock(anchor.offset(cell.pos()), cell.state(), WRITE_FLAGS);
        }
        for (AirshipCell cell : cells) {
            BlockPos target = anchor.offset(cell.pos());
            BlockEntity blockEntity = level.getBlockEntity(target);
            if (blockEntity == null) {
                continue;
            }
            if (blockEntity instanceof AirshipEngineBlockEntity engine) {
                engine.setFuel(cell.fuel());
            } else if (cell.blockEntity().isPresent()) {
                CompoundTag tag = cell.blockEntity().get();
                blockEntity.loadWithComponents(
                        TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                blockEntity.setChanged();
            }
        }

        ship.ejectPassengers();
        ship.discard();
        return true;
    }

    /** The ship's cells rotated by the ship's yaw, rounded to a multiple of 90 degrees. */
    private static List<AirshipCell> snappedCells(AirshipEntity ship) {
        int quarterTurns = Math.floorMod(Math.round(ship.getYRot() / 90.0F), 4);
        Rotation rotation = switch (quarterTurns) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };

        List<AirshipCell> result = new ArrayList<>();
        for (AirshipCell cell : ship.getCellsWithFuel()) {
            int x = cell.pos().getX();
            int z = cell.pos().getZ();
            for (int i = 0; i < quarterTurns; i++) {
                int rotatedX = -z;
                z = x;
                x = rotatedX;
            }
            result.add(new AirshipCell(
                    new BlockPos(x, cell.pos().getY(), z),
                    cell.state().rotate(rotation),
                    cell.blockEntity(),
                    cell.fuel()));
        }
        return result;
    }

    private static BlockPos findLandingAnchor(ServerLevel level, AirshipEntity ship, List<AirshipCell> cells) {
        BlockPos base = BlockPos.containing(
                Math.round(ship.getX() - 0.5), Math.round(ship.getY()), Math.round(ship.getZ() - 0.5));
        for (int[] offset : LANDING_OFFSETS) {
            BlockPos anchor = base.offset(offset[0], offset[1], offset[2]);
            if (fits(level, cells, anchor)) {
                return anchor;
            }
        }
        return null;
    }

    private static boolean fits(ServerLevel level, List<AirshipCell> cells, BlockPos anchor) {
        for (AirshipCell cell : cells) {
            BlockPos target = anchor.offset(cell.pos());
            if (level.isOutsideBuildHeight(target) || !level.hasChunkAt(target)) {
                return false;
            }
            BlockState existing = level.getBlockState(target);
            if (!existing.isAir() && !existing.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    /** Candidate shifts around the rounded position, nearest first, preferring upwards. */
    private static List<int[]> buildLandingOffsets() {
        List<int[]> offsets = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    offsets.add(new int[] {x, y, z});
                }
            }
        }
        offsets.sort(Comparator
                .comparingInt((int[] o) -> o[0] * o[0] + o[1] * o[1] + o[2] * o[2])
                .thenComparingInt(o -> -o[1]));
        return List.copyOf(offsets);
    }

    // ------------------------------------------------------------------ helpers

    private static AABB boundsOf(Set<BlockPos> blocks) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new AABB(minX - 1.0, minY - 1.0, minZ - 1.0, maxX + 2.0, maxY + 3.0, maxZ + 2.0);
    }

    private static boolean isOnShip(ServerPlayer player, Set<BlockPos> blocks) {
        BlockPos feet = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        BlockPos below = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.01, player.getZ());
        return blocks.contains(feet) || blocks.contains(below);
    }

    private static void tell(Player player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }
}
