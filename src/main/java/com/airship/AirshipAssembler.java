package com.airship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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

    /**
     * Assembles the structure connected to {@code startPos} (a Core or a Seat) into a flying airship.
     * The clicking player is seated; if the click was on a seat, that seat is theirs.
     */
    public static InteractionResult assemble(Level level, BlockPos startPos, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        // 1. Find the Core this block belongs to.
        BlockPos corePos = AirshipStructureDetector.findCore(level, startPos);
        if (corePos == null) {
            tell(player, "No Airship Core is connected to this block");
            return InteractionResult.SUCCESS;
        }

        // 2. Scan the ship, skipping terrain that touched it when it last landed.
        Set<BlockPos> ignored = new HashSet<>();
        if (level.getBlockEntity(corePos) instanceof AirshipCoreBlockEntity coreEntity) {
            for (BlockPos relative : coreEntity.getTerrain()) {
                ignored.add(corePos.offset(relative).immutable());
            }
        }
        AirshipStructureDetector.DetectionResult detection = AirshipStructureDetector.detect(level, startPos, ignored);
        if (detection.capped()) {
            tell(player, "Airship is too large (maximum " + AirshipStructureDetector.MAX_BLOCKS
                    + " blocks). Is it touching the ground? Separate it with Airship Build Blocks.");
            return InteractionResult.SUCCESS;
        }

        Set<BlockPos> blocks = detection.blocks();
        if (!blocks.contains(corePos)) {
            tell(player, "The Airship Core is not connected to this seat");
            return InteractionResult.SUCCESS;
        }

        BlockPos primarySeat = level.getBlockState(startPos).is(ModBlocks.AIRSHIP_SEAT)
                ? new BlockPos(startPos.getX() - corePos.getX(), startPos.getY() - corePos.getY(),
                        startPos.getZ() - corePos.getZ())
                : null;

        // Cushions (entities attached to blocks) on the ship fly along and are seats, too.
        AABB area = boundsOf(blocks);
        List<Entity> cushionEntities = serverLevel.getEntitiesOfClass(
                Entity.class, area, e -> AirshipCushions.isCushion(e) && isAttachedToShip(e, blocks));

        int balloons = 0;
        int seatBlocks = 0;
        int engines = 0;
        for (BlockPos pos : blocks) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.AIRSHIP_BALLOON)) {
                balloons++;
            } else if (state.is(ModBlocks.AIRSHIP_SEAT)) {
                seatBlocks++;
            } else if (state.is(ModBlocks.AIRSHIP_ENGINE)) {
                engines++;
            }
        }
        int seatCount = seatBlocks + cushionEntities.size();

        int required = AirshipLift.requiredBalloons(blocks.size());
        tell(player, "Airship: " + blocks.size() + " blocks, " + balloons + "/" + required
                + " balloons, " + seatCount + " seats (" + cushionEntities.size() + " cushions), "
                + engines + " engines");

        if (!AirshipLift.hasEnoughLift(balloons, required)) {
            tell(player, "Airship cannot fly: not enough balloons (1 per "
                    + AirshipLift.BLOCKS_PER_BALLOON + " blocks)");
            return InteractionResult.SUCCESS;
        }
        if (seatCount == 0) {
            tell(player, "Airship cannot fly: it needs at least one seat or cushion");
            return InteractionResult.SUCCESS;
        }

        // Possible seats, as positions relative to the Core: seat blocks first, then cushions.
        double originX = corePos.getX() + 0.5;
        double originY = corePos.getY();
        double originZ = corePos.getZ() + 0.5;
        List<SeatSpot> spots = new ArrayList<>();
        for (BlockPos pos : blocks) {
            if (level.getBlockState(pos).is(ModBlocks.AIRSHIP_SEAT)) {
                BlockPos relative = new BlockPos(
                        pos.getX() - corePos.getX(), pos.getY() - corePos.getY(), pos.getZ() - corePos.getZ());
                spots.add(new SeatSpot(
                        new Vec3(relative.getX(), relative.getY() + AirshipSeatBlock.SEAT_HEIGHT, relative.getZ()),
                        relative, null));
            }
        }
        spots.sort(Comparator.comparingDouble((SeatSpot spot) -> spot.local().lengthSqr()));
        List<SeatSpot> cushionSpots = new ArrayList<>();
        for (Entity cushion : cushionEntities) {
            cushionSpots.add(new SeatSpot(
                    new Vec3(cushion.getX() - originX,
                            cushion.getY() - originY + AirshipCushions.SEAT_HEIGHT,
                            cushion.getZ() - originZ),
                    null, cushion));
        }
        cushionSpots.sort(Comparator.comparingDouble((SeatSpot spot) -> spot.local().lengthSqr()));
        spots.addAll(cushionSpots);

        // Everybody who sits on a cushion or stands on the ship needs a seat, otherwise they would fall.
        Set<ServerPlayer> riderSet = new LinkedHashSet<>();
        if (player instanceof ServerPlayer starter) {
            riderSet.add(starter);
        }
        for (Entity cushion : cushionEntities) {
            for (Entity passenger : cushion.getPassengers()) {
                if (passenger instanceof ServerPlayer sitting) {
                    riderSet.add(sitting);
                }
            }
        }
        for (ServerPlayer other : serverLevel.getEntitiesOfClass(ServerPlayer.class, area, p -> isOnShip(p, blocks))) {
            riderSet.add(other);
        }
        List<ServerPlayer> riders = new ArrayList<>(riderSet);
        if (riders.size() > seatCount) {
            tell(player, "Not enough seats: " + riders.size() + " players on the airship, " + seatCount + " seats");
            return InteractionResult.SUCCESS;
        }

        // Give every rider a seat: the clicked seat for the starter, their own cushion for cushion sitters,
        // otherwise the nearest free one. Passenger n sits on seat n, so the seat order follows the riders.
        List<SeatSpot> free = new ArrayList<>(spots);
        List<Vec3> seatOrder = new ArrayList<>();
        for (ServerPlayer rider : riders) {
            SeatSpot chosen = null;
            if (rider == player && primarySeat != null) {
                for (SeatSpot spot : free) {
                    if (primarySeat.equals(spot.blockPos())) {
                        chosen = spot;
                        break;
                    }
                }
            }
            if (chosen == null) {
                for (SeatSpot spot : free) {
                    if (spot.cushion() != null && rider.getVehicle() == spot.cushion()) {
                        chosen = spot;
                        break;
                    }
                }
            }
            if (chosen == null) {
                double best = Double.MAX_VALUE;
                for (SeatSpot spot : free) {
                    double distance = new Vec3(originX, originY, originZ).add(spot.local())
                            .distanceToSqr(rider.position());
                    if (distance < best) {
                        best = distance;
                        chosen = spot;
                    }
                }
            }
            free.remove(chosen);
            seatOrder.add(chosen.local());
        }
        for (SeatSpot spot : free) {
            seatOrder.add(spot.local());
        }

        // Take the cushions off the ship so they can be carried along.
        List<AirshipCushion> cushions = new ArrayList<>();
        for (Entity cushion : cushionEntities) {
            cushion.ejectPassengers();
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            cushion.saveWithoutId(output);
            cushions.add(new AirshipCushion(
                    cushion.getX() - originX, cushion.getY() - originY, cushion.getZ() - originZ,
                    cushion.getYRot(), output.buildResult()));
            cushion.discard();
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
        ship.setPos(originX, originY, originZ);
        ship.setCells(cells);
        ship.setCushions(cushions);
        ship.setSeatOrder(seatOrder);
        serverLevel.addFreshEntity(ship);

        for (ServerPlayer rider : riders) {
            rider.startRiding(ship, true, true);
        }

        tell(player, "Airship ready. W/A/S/D move, Space up, Ctrl down. Shift leaves the seat and lands the ship.");
        return InteractionResult.SUCCESS;
    }

    /** A place to sit, relative to the Core: a seat block (blockPos set) or a cushion (cushion set). */
    private record SeatSpot(Vec3 local, BlockPos blockPos, Entity cushion) {}

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

        rememberTerrainContacts(level, cells, anchor);
        restoreCushions(level, ship.getCushions(), anchor, snappedYaw(ship.getYRot()));

        ship.ejectPassengers();
        ship.discard();
        return true;
    }

    private static boolean isAttachedToShip(Entity entity, Set<BlockPos> blocks) {
        BlockPos at = entity.blockPosition();
        BlockPos below = BlockPos.containing(entity.getX(), entity.getY() - 0.01, entity.getZ());
        return blocks.contains(at) || blocks.contains(below);
    }

    /** Re-creates the ship's cushions on the landed ship (the blocks they sit on exist again by now). */
    private static void restoreCushions(ServerLevel level, List<AirshipCushion> cushions, BlockPos anchor, float snappedYaw) {
        Vec3 origin = new Vec3(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
        float rotation = (float) -Math.toRadians(snappedYaw);
        for (AirshipCushion cushion : cushions) {
            Entity entity = EntityTypes.CUSHION.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            if (entity == null) {
                continue;
            }
            CompoundTag data = cushion.data().copy();
            data.remove("UUID"); // the original is gone, the copy gets a fresh identity
            entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));

            Vec3 world = origin.add(new Vec3(cushion.x(), cushion.y(), cushion.z()).yRot(rotation));
            entity.setPos(world.x, world.y, world.z);
            entity.setYRot(cushion.yaw() + snappedYaw);
            level.addFreshEntity(entity);
        }
    }

    /**
     * Stores, in every Core of the landed ship, which neighbouring blocks are terrain (anything next to
     * the ship that is not part of it), so the next scan does not pull that terrain into the ship.
     */
    private static void rememberTerrainContacts(ServerLevel level, List<AirshipCell> cells, BlockPos anchor) {
        Set<BlockPos> shipBlocks = new HashSet<>();
        List<BlockPos> cores = new ArrayList<>();
        for (AirshipCell cell : cells) {
            BlockPos target = anchor.offset(cell.pos()).immutable();
            shipBlocks.add(target);
            if (cell.state().is(ModBlocks.AIRSHIP_CORE)) {
                cores.add(target);
            }
        }

        Set<BlockPos> contacts = new HashSet<>();
        for (BlockPos block : shipBlocks) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = block.relative(direction);
                if (shipBlocks.contains(neighbour)) {
                    continue;
                }
                BlockState state = level.getBlockState(neighbour);
                if (state.isAir() || state.canBeReplaced() || ModBlocks.isAirshipBuildBlock(state.getBlock())) {
                    continue;
                }
                contacts.add(neighbour.immutable());
            }
        }

        for (BlockPos core : cores) {
            if (level.getBlockEntity(core) instanceof AirshipCoreBlockEntity coreEntity) {
                List<BlockPos> relative = new ArrayList<>(contacts.size());
                for (BlockPos contact : contacts) {
                    relative.add(new BlockPos(
                            contact.getX() - core.getX(), contact.getY() - core.getY(), contact.getZ() - core.getZ()));
                }
                coreEntity.setTerrain(relative);
            }
        }
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

    /** The block grid position (of the Core) a ship at this position lands on. */
    public static BlockPos snappedAnchor(Vec3 pos) {
        return BlockPos.containing(Math.round(pos.x - 0.5), Math.round(pos.y), Math.round(pos.z - 0.5));
    }

    /** The ship's yaw rounded to the nearest multiple of 90 degrees. */
    public static float snappedYaw(float yaw) {
        return Math.round(yaw / 90.0F) * 90.0F;
    }

    private static BlockPos findLandingAnchor(ServerLevel level, AirshipEntity ship, List<AirshipCell> cells) {
        BlockPos base = snappedAnchor(ship.position());
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
