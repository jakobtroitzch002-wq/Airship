package com.airship;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An assembled airship. All blocks of the ship live inside this entity (relative to the former
 * Core position). The ship flies while a pilot sits on the control seat, turns to face the way the
 * pilot looks, and turns back into blocks as soon as the pilot leaves the seat.
 */
public class AirshipEntity extends Entity {
    // --- flight tuning (blocks per tick) ---
    /** Top speeds, in blocks per tick (the numbers are given in blocks per second): no fuel, fuel, turbo. */
    public static final double BASE_SPEED = AirshipConfig.get().speedNoFuel / 20.0;
    public static final double FUEL_SPEED = AirshipConfig.get().speedFuel / 20.0;
    public static final double TURBO_SPEED = AirshipConfig.get().speedTurbo / 20.0;
    /** The turbo tank burns this many times as fast as the normal one ... */
    public static final int TURBO_BURN_RATE = AirshipConfig.get().turboBurnRate;
    /** ... and, while it burns, the ship also accelerates this many times as fast. */
    public static final double TURBO_ACCEL_MULTIPLIER = 3.0;
    private static final double VERTICAL_SPEED = 0.08;
    private static final double HORIZONTAL_ACCEL = 0.012;
    private static final double VERTICAL_ACCEL = 0.02;
    /** Maximum turn rate in degrees per tick. */
    private static final float MAX_TURN = 3.5F;
    /** How fast a ship without enough lift sinks (blocks per tick). */
    private static final double SINK_SPEED = 0.05;

    private List<AirshipCell> cells = List.of();
    private final Map<BlockPos, BlockState> stateByPos = new HashMap<>();
    private final Map<BlockPos, Integer> engineFuel = new HashMap<>();
    private final Map<BlockPos, Integer> turboFuel = new HashMap<>();
    private List<Vec3> seats = List.of();
    private List<AirshipCushion> cushions = List.of();
    private List<AirshipClientCushion> clientCushions = List.of();
    private int landCooldown;
    private int leadRecoveryTicks;
    private int minCellY;
    private int maxCellY;
    private boolean landed;
    private BlockPos[] surface;
    private int[] surfaceMask;
    private List<AirshipClientCell> exposed;
    private int cellVersion;
    /** Lift of all balloons on board and the lift the ship needs (see {@link AirshipLift}). */
    private int lift;
    private int requiredLift;
    /** Largest horizontal distance of a cell from the origin, used to find projectiles near the ship. */
    private double radius;
    /** Engine cells (relative to the Core), for their smoke. */
    private List<BlockPos> engineCells = List.of();
    /** What the engines do (see {@link AirshipEnginePayload}): on the server what was sent last, on clients what came. */
    private int engineState = AirshipEnginePayload.OFF;

    /** Server only: the ship rests on the ground (then it turns into blocks as soon as nobody steers it). */
    private boolean resting;
    /** Server only: projectiles that already hit a balloon -> how many more balloons they may pop. */
    private final Map<Integer, Integer> projectileHits = new HashMap<>();

    // server only
    private Vec3 velocity = Vec3.ZERO;
    // seat assignment: passenger entity id -> seat index (0 = control seat); and the ship's "front"
    private final Map<Integer, Integer> seatByPassenger = new HashMap<>();
    private float controlYaw;

    // client only: buffered snapshots from the server, played back a little behind real time
    private record Snapshot(long tick, Vec3 pos, float yaw) {}

    private static final double INTERPOLATION_DELAY_TICKS = 2.0;
    private final List<Snapshot> snapshots = new ArrayList<>();
    private long latestTick;
    private double renderTick;
    private boolean snapshotsReady;

    // server only: what the clients were told last
    private Vec3 lastSentPos;
    private float lastSentYaw;
    private long lastSentTick = Long.MIN_VALUE / 2;

    // client only: positions of the last two ticks, used to render smoothly between them
    private Vec3 prevPos;
    private Vec3 curPos;
    private float prevYaw;
    private float curYaw;
    private boolean smoothReady;

    public AirshipEntity(EntityType<? extends AirshipEntity> type, Level level) {
        super(type, level);
    }

    // ------------------------------------------------------------------ data

    public void setCells(List<AirshipCell> newCells) {
        this.cells = List.copyOf(newCells);
        rebuild();
    }

    /** Client side: replace the block data with what the server sent. */
    public void setClientCells(List<AirshipClientCell> clientCells) {
        List<AirshipCell> converted = new ArrayList<>(clientCells.size());
        for (AirshipClientCell cell : clientCells) {
            converted.add(new AirshipCell(cell.pos(), cell.state(), Optional.empty(), 0, 0));
        }
        setCells(converted);
    }

    private void rebuild() {
        stateByPos.clear();
        engineFuel.clear();
        turboFuel.clear();
        for (AirshipCell cell : cells) {
            BlockPos pos = cell.pos().immutable();
            stateByPos.put(pos, cell.state());
            if (ModBlocks.isEngine(cell.state())) {
                engineFuel.put(pos, cell.fuel());
                turboFuel.put(pos, cell.turbo());
            }
        }

        // Height of the ship (lowest and highest block), used to keep it inside the world.
        this.minCellY = 0;
        this.maxCellY = 0;
        boolean firstCell = true;
        for (BlockPos pos : stateByPos.keySet()) {
            this.minCellY = firstCell ? pos.getY() : Math.min(this.minCellY, pos.getY());
            this.maxCellY = firstCell ? pos.getY() : Math.max(this.maxCellY, pos.getY());
            firstCell = false;
        }

        this.lift = 0;
        this.radius = 0.0;
        for (Map.Entry<BlockPos, BlockState> entry : stateByPos.entrySet()) {
            this.lift += ModBlocks.liftOf(entry.getValue());
            BlockPos p = entry.getKey();
            this.radius = Math.max(this.radius, Math.sqrt((double) p.getX() * p.getX() + (double) p.getZ() * p.getZ()));
        }
        this.requiredLift = AirshipLift.requiredLift(stateByPos.size());
        List<BlockPos> engines = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : stateByPos.entrySet()) {
            if (ModBlocks.isEngine(entry.getValue())) {
                engines.add(entry.getKey());
            }
        }
        this.engineCells = List.copyOf(engines);

        // The Airship Seat faces the front of the ship.
        this.controlYaw = 0.0F;
        for (BlockState state : stateByPos.values()) {
            if (state.is(ModBlocks.AIRSHIP_SEAT)) {
                this.controlYaw = state.getValue(AirshipSeatBlock.FACING).toYRot();
                break;
            }
        }
        computeSeats();

        this.surface = null;
        this.surfaceMask = null;
        this.exposed = null;
        this.cellVersion++;
    }

    /** Cushions riding along with the ship (server side; they are re-created when the ship lands). */
    public void setCushions(List<AirshipCushion> newCushions) {
        this.cushions = List.copyOf(newCushions);
        List<AirshipClientCushion> forClients = new ArrayList<>(newCushions.size());
        for (AirshipCushion cushion : newCushions) {
            forClients.add(new AirshipClientCushion(
                    cushion.x(), cushion.y(), cushion.z(), cushion.yaw(), AirshipCushions.colorOf(cushion.data())));
        }
        this.clientCushions = List.copyOf(forClients);
        computeSeats();
        this.cellVersion++;
    }

    /** Sets the order of the seats: the n-th passenger sits on the n-th seat. */
    public void setSeatOrder(List<Vec3> order) {
        this.seats = List.copyOf(order);
    }

    /** Fixed seat for each passenger (passenger entity id -> seat index). */
    public void setSeatAssignment(Map<Integer, Integer> assignment) {
        seatByPassenger.clear();
        seatByPassenger.putAll(assignment);
    }

    public AirshipSeatMapPayload seatMapPayload() {
        List<Integer> ids = new ArrayList<>(seatByPassenger.keySet());
        List<Integer> indices = new ArrayList<>();
        for (int id : ids) {
            indices.add(seatByPassenger.get(id));
        }
        return new AirshipSeatMapPayload(getId(), ids, indices);
    }

    public List<Vec3> getSeats() {
        return seats;
    }

    public List<AirshipCushion> getCushions() {
        return cushions;
    }

    public List<AirshipClientCushion> getClientCushions() {
        return clientCushions;
    }

    /** Client side: apply everything the server sent about this ship. */
    public void setClientData(AirshipBlocksPayload payload) {
        setClientCells(payload.cells());
        this.clientCushions = List.copyOf(payload.cushions());
        computeSeats();
        if (!payload.seats().isEmpty()) {
            setSeatOrder(payload.seats());
        }
        this.cellVersion++;
    }

    /** For a moment after takeoff, dropped leads nearby go back to the pilot (their leashes were released). */
    public void recoverLeads() {
        this.leadRecoveryTicks = 5;
    }

    private void computeSeats() {
        List<BlockPos> seatCells = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : stateByPos.entrySet()) {
            if (entry.getValue().is(ModBlocks.AIRSHIP_SEAT)) {
                seatCells.add(entry.getKey());
            }
        }
        // The seat closest to the Core first (a ship has exactly one control seat).
        seatCells.sort(Comparator
                .comparingDouble((BlockPos p) -> (double) p.getX() * p.getX() + (double) p.getY() * p.getY()
                        + (double) p.getZ() * p.getZ())
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getZ));

        List<Vec3> found = new ArrayList<>();
        for (BlockPos p : seatCells) {
            found.add(new Vec3(p.getX(), p.getY() + AirshipSeatBlock.SEAT_HEIGHT, p.getZ()));
        }
        // Cushions are seats too, after the seat blocks.
        List<Vec3> cushionSeats = new ArrayList<>();
        for (AirshipClientCushion cushion : clientCushions) {
            cushionSeats.add(new Vec3(cushion.x(), cushion.y() + AirshipCushions.SEAT_HEIGHT, cushion.z()));
        }
        cushionSeats.sort(Comparator.comparingDouble(Vec3::lengthSqr));
        found.addAll(cushionSeats);
        this.seats = List.copyOf(found);
    }

    /** All cells with the current engine fuel merged in (used for saving and landing). */
    public List<AirshipCell> getCellsWithFuel() {
        List<AirshipCell> result = new ArrayList<>(cells.size());
        for (AirshipCell cell : cells) {
            result.add(new AirshipCell(
                    cell.pos(), cell.state(), cell.blockEntity(),
                    engineFuel.getOrDefault(cell.pos(), 0),
                    turboFuel.getOrDefault(cell.pos(), 0)));
        }
        return result;
    }

    public List<AirshipClientCell> clientCells() {
        List<AirshipClientCell> result = new ArrayList<>(cells.size());
        for (AirshipCell cell : cells) {
            result.add(new AirshipClientCell(cell.pos(), cell.state()));
        }
        return result;
    }

    public int getCellVersion() {
        return cellVersion;
    }

    /** Cells that can be seen from outside (used by the renderer to skip hidden interior blocks). */
    public List<AirshipClientCell> getExposedCells() {
        if (exposed == null) {
            List<AirshipClientCell> result = new ArrayList<>();
            for (Map.Entry<BlockPos, BlockState> entry : stateByPos.entrySet()) {
                BlockPos pos = entry.getKey();
                boolean visible = false;
                for (Direction direction : Direction.values()) {
                    BlockState neighbour = stateByPos.get(pos.relative(direction));
                    if (neighbour == null || !neighbour.isSolidRender()) {
                        visible = true;
                        break;
                    }
                }
                if (visible) {
                    result.add(new AirshipClientCell(pos, entry.getValue()));
                }
            }
            exposed = result;
        }
        return exposed;
    }

    private void buildSurface() {
        List<BlockPos> cellsOut = new ArrayList<>();
        List<Integer> masksOut = new ArrayList<>();
        for (BlockPos pos : stateByPos.keySet()) {
            int mask = 0;
            for (Direction d : Direction.values()) {
                if (!stateByPos.containsKey(pos.relative(d))) {
                    mask |= 1 << d.ordinal();
                }
            }
            if (mask != 0) {
                cellsOut.add(pos);
                masksOut.add(mask);
            }
        }
        surface = cellsOut.toArray(new BlockPos[0]);
        surfaceMask = new int[masksOut.size()];
        for (int i = 0; i < surfaceMask.length; i++) {
            surfaceMask[i] = masksOut.get(i);
        }
    }

    /** Cells with at least one exposed face (relative to the Core). */
    public BlockPos[] surfaceCells() {
        if (surface == null) {
            buildSurface();
        }
        return surface;
    }

    /** For each surface cell: bit (1 << Direction.ordinal()) is set if that local face is exposed. */
    public int[] surfaceMasks() {
        if (surfaceMask == null) {
            buildSurface();
        }
        return surfaceMask;
    }

    // ------------------------------------------------------------------ walking on the ship (client)

    /** Collision shapes of the ship's block types, at the origin (looked up once per block state). */
    private static final Map<BlockState, VoxelShape> SHAPES = new java.util.concurrent.ConcurrentHashMap<>();

    private static VoxelShape shapeOf(BlockState state) {
        return SHAPES.computeIfAbsent(state, s -> {
            if (ModBlocks.isAirshipBuildBlock(s.getBlock())) {
                return Shapes.empty();
            }
            try {
                return s.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            } catch (RuntimeException e) {
                return Shapes.block();
            }
        });
    }

    /** A world offset turned into the ship's own (unrotated) grid. */
    private Vec3 toLocal(Vec3 world) {
        double angle = -Math.toRadians(getYRot());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(world.x * cos - world.z * sin, world.y, world.x * sin + world.z * cos);
    }

    /**
     * An entity's box in the ship's grid. Cells are centred on whole numbers there (a cell spans x-0.5..x+0.5),
     * heights stay the same. The footprint of a player is a square, so it keeps its size when turned.
     */
    private AABB toLocalBox(AABB world) {
        Vec3 center = toLocal(world.getCenter().subtract(position()));
        double half = Math.max(world.getXsize(), world.getZsize()) / 2.0;
        double baseY = position().y;
        return new AABB(center.x - half, world.minY - baseY, center.z - half,
                center.x + half, world.maxY - baseY, center.z + half);
    }

    /** The exact collision shapes (in the ship's grid) of the cells that touch the local box. */
    private List<VoxelShape> localShapes(AABB local) {
        List<VoxelShape> out = new ArrayList<>();
        int x0 = Mth.floor(local.minX + 0.5);
        int x1 = Mth.floor(local.maxX + 0.5);
        int y0 = Math.max(minCellY, Mth.floor(local.minY) - 1); // fences and walls reach 1.5 blocks up
        int y1 = Math.min(maxCellY, Mth.floor(local.maxY));
        int z0 = Mth.floor(local.minZ + 0.5);
        int z1 = Mth.floor(local.maxZ + 0.5);
        if ((long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1) > 4096) {
            return out; // never happens for a player; guards against huge boxes
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    BlockState state = stateByPos.get(cursor.set(x, y, z));
                    if (state == null) {
                        continue;
                    }
                    VoxelShape shape = shapeOf(state);
                    if (!shape.isEmpty()) {
                        out.add(shape.move(x - 0.5, y, z - 0.5));
                    }
                }
            }
        }
        return out;
    }

    /** Same order as Minecraft: up/down first, then the larger of the two sideways directions. */
    private static Vec3 collideAxes(Vec3 movement, AABB box, List<VoxelShape> shapes) {
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        if (y != 0.0) {
            y = Shapes.collide(Direction.Axis.Y, box, shapes, y);
            if (y != 0.0) {
                box = box.move(0.0, y, 0.0);
            }
        }
        boolean zFirst = Math.abs(x) < Math.abs(z);
        if (zFirst && z != 0.0) {
            z = Shapes.collide(Direction.Axis.Z, box, shapes, z);
            if (z != 0.0) {
                box = box.move(0.0, 0.0, z);
            }
        }
        if (x != 0.0) {
            x = Shapes.collide(Direction.Axis.X, box, shapes, x);
            if (!zFirst && x != 0.0) {
                box = box.move(x, 0.0, 0.0);
            }
        }
        if (!zFirst && z != 0.0) {
            z = Shapes.collide(Direction.Axis.Z, box, shapes, z);
        }
        return new Vec3(x, y, z);
    }

    /**
     * Client side: limits an entity's movement (already checked against the world) by the ship's blocks, with
     * their exact shapes in the ship's own rotated grid. Includes stepping up onto slabs and block edges.
     */
    public Vec3 collideEntity(Entity entity, AABB worldBox, Vec3 movement) {
        AABB box = toLocalBox(worldBox);
        Vec3 local = toLocal(movement);
        double step = Math.max(0.0, entity.maxUpStep());
        List<VoxelShape> shapes = localShapes(box.expandTowards(local).inflate(step + 0.1));
        if (shapes.isEmpty()) {
            return movement;
        }
        Vec3 result = collideAxes(local, box, shapes);

        boolean blockedSideways = result.x != local.x || result.z != local.z;
        boolean grounded = entity.onGround() || (local.y < 0.0 && result.y != local.y);
        if (step > 0.0 && blockedSideways && grounded) {
            Vec3 stepped = collideAxes(new Vec3(local.x, step, local.z), box, shapes);
            double down = Shapes.collide(Direction.Axis.Y, box.move(stepped), shapes, -stepped.y + Math.min(0.0, local.y));
            stepped = stepped.add(0.0, down, 0.0);
            if (stepped.horizontalDistanceSqr() > result.horizontalDistanceSqr() + 1.0E-7) {
                result = stepped;
            }
        }
        if (result.equals(local)) {
            return movement;
        }
        double angle = -Math.toRadians(getYRot());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(result.x * cos + result.z * sin, result.y, result.z * cos - result.x * sin);
    }

    /** Client side: true if the world box overlaps one of the ship's blocks (exact shapes). */
    public boolean touches(AABB world) {
        AABB local = toLocalBox(world);
        for (VoxelShape shape : localShapes(local)) {
            for (AABB part : shape.toAabbs()) {
                if (part.intersects(local)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static final int CARRY_NONE = 0;
    public static final int CARRY_ABOVE = 1;
    public static final int CARRY_STANDING = 2;

    /**
     * Client side, after the ship moved this tick: an entity standing on the ship moves and turns with it, and
     * can still walk around on it. An entity that just jumped ({@code wasOnShip}) keeps moving along sideways as
     * long as it is above the ship, so it lands on the ship again instead of being left behind.
     *
     * @return {@link #CARRY_STANDING}, {@link #CARRY_ABOVE} or {@link #CARRY_NONE}
     */
    public int carry(Entity entity, boolean wasOnShip) {
        if (!smoothReady || entity.isPassenger()) {
            return CARRY_NONE;
        }
        boolean standing = entity.getDeltaMovement().y <= 0.2 && AirshipShipColliders.standsOn(this, entity);
        if (!standing && !(wasOnShip && AirshipShipColliders.isAbove(this, entity, 4.0))) {
            return CARRY_NONE;
        }
        float turn = Mth.wrapDegrees(curYaw - prevYaw);
        if (!curPos.equals(prevPos) || turn != 0.0F) {
            Vec3 offset = entity.position().subtract(prevPos).yRot((float) -Math.toRadians(turn));
            Vec3 shift = curPos.add(offset).subtract(entity.position());
            // In the air only the sideways movement is taken over: jumping and falling stay the player's own.
            if (!standing) {
                shift = new Vec3(shift.x, 0.0, shift.z);
            }
            // Applied as part of the player's own movement this tick (see AirshipShipColliders), so the player is
            // drawn moving smoothly with the ship instead of jumping once per tick.
            AirshipShipColliders.carryBy(shift);
            entity.setYRot(entity.getYRot() + turn);
        }
        return standing ? CARRY_STANDING : CARRY_ABOVE;
    }

    public int getFuel(BlockPos relativePos) {
        return engineFuel.getOrDefault(relativePos, 0);
    }

    // ------------------------------------------------------------------ entity plumbing

    @Override
    public void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        input.read("Cells", AirshipCell.CODEC.listOf()).ifPresent(this::setCells);
        input.read("Cushions", AirshipCushion.CODEC.listOf()).ifPresent(this::setCushions);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        output.store("Cells", AirshipCell.CODEC.listOf(), getCellsWithFuel());
        output.store("Cushions", AirshipCushion.CODEC.listOf(), new ArrayList<>(cushions));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    // ------------------------------------------------------------------ seats

    /** The point the ship turns around: the first seat (the pilot's), so the pilot does not swing around. */
    public Vec3 getPivot() {
        if (seats.isEmpty()) {
            return Vec3.ZERO;
        }
        Vec3 seat = seats.get(0);
        return new Vec3(seat.x, 0.0, seat.z);
    }

    /** A point of the ship (relative to its origin) rotated by the ship's yaw, in degrees. */
    public static Vec3 rotateLocal(Vec3 local, float yawDegrees) {
        return local.yRot((float) -Math.toRadians(yawDegrees));
    }

    /** Called when the ship was landed on purpose, so removing the entity afterwards is fine. */
    public void markLanded() {
        this.landed = true;
    }

    /**
     * The ship's blocks only exist inside this entity. If it is removed in any way other than landing (for
     * example with /kill), it tries to land first instead of destroying everything on board.
     */
    @Override
    public void remove(RemovalReason reason) {
        if (!landed && !cells.isEmpty() && level() instanceof ServerLevel serverLevel
                && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            landed = true;
            if (AirshipAssembler.disassemble(serverLevel, this)) {
                return; // the landing removed the ship itself
            }
        }
        super.remove(reason);
    }

    private Vec3 toWorldOffset(Vec3 local) {
        return local.yRot((float) -Math.toRadians(getYRot()));
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < seats.size();
    }

    @Override
    public Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        if (seats.isEmpty()) {
            return Vec3.ZERO;
        }
        Integer assigned = seatByPassenger.get(passenger.getId());
        int index = assigned != null ? assigned : Math.max(0, getPassengers().indexOf(passenger));
        return toWorldOffset(seats.get(index % seats.size()));
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (seats.isEmpty()) {
            return super.getDismountLocationForPassenger(passenger);
        }
        // A ship on the ground turns into blocks when its pilot leaves: put everybody on its landed spot.
        // A hovering ship stays where it is: the passenger stands up right where it sat.
        ServerPlayer pilot = findPilot();
        if (resting && (pilot == null || pilot == passenger)) {
            return landingSpot(passenger);
        }
        // Stand on top of the seat block (or on the cushion). The players see the ship a few ticks behind the
        // server, so while it moves, the spot is moved back by that much; otherwise they would land beside it.
        Vec3 seat = seatOf(passenger);
        BlockPos cell = new BlockPos(Mth.floor(seat.x + 0.5), Mth.floor(seat.y), Mth.floor(seat.z + 0.5));
        BlockState below = stateByPos.get(cell);
        Vec3 spot = below != null && below.is(ModBlocks.AIRSHIP_SEAT)
                ? new Vec3(seat.x, cell.getY() + 1.0, seat.z)
                : new Vec3(seat.x, seat.y + 0.1, seat.z);
        Vec3 lag = velocity.scale(-(INTERPOLATION_DELAY_TICKS + 1.0));
        return position().add(lag).add(toWorldOffset(spot));
    }

    private Vec3 seatOf(Entity passenger) {
        Integer assigned = seatByPassenger.get(passenger.getId());
        int index = assigned != null ? assigned : Math.max(0, getPassengers().indexOf(passenger));
        return seats.get(index % seats.size());
    }

    /**
     * A player wants to steer this hovering ship (right-click on its Airship Seat). Works if nobody steers it,
     * there is a free seat and the player is close to the seat.
     */
    public void board(ServerPlayer player) {
        if (seats.isEmpty() || player.isPassenger() || findPilot() != null || !canAddPassenger(player)) {
            return;
        }
        Vec3 seat = position().add(toWorldOffset(seats.get(0)));
        if (player.distanceToSqr(seat) > 36.0) {
            return;
        }
        seatByPassenger.values().removeIf(index -> index == 0); // whoever steered before has left
        if (!player.startRiding(this, true, true)) {
            return;
        }
        seatByPassenger.put(player.getId(), 0);
        velocity = Vec3.ZERO;
        AirshipSeatMapPayload seatMap = seatMapPayload();
        for (ServerPlayer tracking : PlayerLookup.tracking(this)) {
            ServerPlayNetworking.send(tracking, seatMap);
        }
    }

    /** A block of the ship hit by the player's view: its cell (relative to the Core), state and distance. */
    public record ShipHit(BlockPos cell, BlockState state, double distance) {}

    /** Client side: the first block of the ship on the player's view line within {@code reach}, or null. */
    public ShipHit pick(Vec3 eye, Vec3 look, double reach) {
        Vec3 origin = position();
        double angle = -Math.toRadians(getYRot());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        for (double d = 0.0; d <= reach; d += 0.05) {
            Vec3 point = eye.add(look.scale(d));
            double dx = point.x - origin.x;
            double dz = point.z - origin.z;
            BlockPos cell = new BlockPos(
                    Mth.floor(dx * cos - dz * sin + 0.5),
                    Mth.floor(point.y - origin.y),
                    Mth.floor(dx * sin + dz * cos + 0.5));
            BlockState state = stateByPos.get(cell);
            if (state != null) {
                return new ShipHit(cell, state, d);
            }
        }
        return null;
    }

    /** True for the ship's blocks that can be right-clicked while it flies. */
    public static boolean isClickable(BlockState state) {
        return state.is(ModBlocks.AIRSHIP_SEAT) || ModBlocks.isEngine(state) || ModBlocks.isCoreBlock(state);
    }

    /** World position of the centre of a cell. */
    private Vec3 cellCenter(BlockPos cell) {
        return position().add(toWorldOffset(new Vec3(cell.getX(), cell.getY() + 0.5, cell.getZ())));
    }

    /** Server side: a player right-clicked a block of this ship. */
    public void click(ServerPlayer player, BlockPos cell) {
        BlockState state = stateByPos.get(cell);
        if (state == null || isRemoved() || player.distanceToSqr(cellCenter(cell)) > 49.0) {
            return;
        }
        if (state.is(ModBlocks.AIRSHIP_SEAT)) {
            board(player);
        } else if (ModBlocks.isEngine(state)) {
            openEngine(player, cell, state);
        } else if (ModBlocks.isCoreBlock(state)) {
            ServerPlayNetworking.send(player, coreInfo());
        }
    }

    /** The engine screen for an engine on the flying ship: its fuel lives in this entity until the ship lands. */
    private void openEngine(ServerPlayer player, BlockPos cell, BlockState state) {
        boolean hasTurbo = ModBlocks.engineHasTurbo(state);
        BlockPos key = cell.immutable();
        AirshipEngineTank tank = new AirshipEngineTank() {
            @Override
            public int getFuel() {
                return engineFuel.getOrDefault(key, 0);
            }

            @Override
            public int getTurbo() {
                return turboFuel.getOrDefault(key, 0);
            }

            @Override
            public int maxTurbo() {
                return ModBlocks.maxTurbo(state);
            }

            @Override
            public void addFuel(int ticks) {
                engineFuel.put(key, Math.min(AirshipEngineBlockEntity.MAX_FUEL, getFuel() + ticks));
            }

            @Override
            public void addTurbo(int ticks) {
                turboFuel.put(key, Math.min(maxTurbo(), getTurbo() + ticks));
            }

            @Override
            public boolean isUsableBy(Player user) {
                return !isRemoved() && stateByPos.containsKey(key) && user.distanceToSqr(cellCenter(key)) <= 64.0;
            }
        };
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 2) {
                    return hasTurbo ? 1 : 0;
                }
                if (index == 3) {
                    return (int) Math.round(ModBlocks.enginePower(state) * 100.0);
                }
                if (index == 4) {
                    return ModBlocks.turboBurnRate(state);
                }
                if (index == 5) {
                    return ModBlocks.maxTurbo(state) / 20;
                }
                return (index == 0 ? tank.getFuel() : tank.getTurbo()) / 20; // seconds, see AirshipEngineBlockEntity
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 6;
            }
        };
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, user) -> new AirshipEngineMenu(containerId, inventory, tank, data),
                Component.translatable(state.getBlock().getDescriptionId())));
    }

    /** What the Core screen shows for this flying ship. */
    public AirshipCoreInfoPayload coreInfo() {
        int seatBlocks = 0;
        int fuel = 0;
        for (BlockState state : stateByPos.values()) {
            if (state.is(ModBlocks.AIRSHIP_SEAT)) {
                seatBlocks++;
            }
        }
        for (int value : engineFuel.values()) {
            fuel += value;
        }
        int animals = 0;
        for (Entity passenger : getPassengers()) {
            if (!(passenger instanceof Player)) {
                animals++;
            }
        }
        BlockState core = stateByPos.get(BlockPos.ZERO);
        int max = core != null ? ModBlocks.coreLimit(core) : stateByPos.size();
        return new AirshipCoreInfoPayload(
                BlockPos.containing(position()), true, "", stateByPos.size(), max, lift, requiredLift, seatBlocks,
                cushions.size(), engineFuel.size(), fuel, engineFuel.size() * AirshipEngineBlockEntity.MAX_FUEL,
                animals, getId());
    }

    /** A button on the Core screen of this flying ship was pressed. */
    public void coreAction(ServerPlayer player, int action) {
        if (isRemoved() || player.distanceToSqr(cellCenter(BlockPos.ZERO)) > 64.0) {
            return;
        }
        if (action == AirshipCoreActionPayload.ALIGN && level() instanceof ServerLevel serverLevel) {
            // Snap the ship to the block grid right where it is and turn it into blocks, so it can be rebuilt.
            // Whoever sits on it is put down on their spot; sitting on the Airship Seat again lifts it off.
            if (AirshipAssembler.disassemble(serverLevel, this)) {
                return;
            }
            player.sendOverlayMessage(Component.translatable("message.airship.cannot_land"));
        }
        ServerPlayNetworking.send(player, coreInfo());
    }

    /**
     * Where a passenger is put down when the ship lands: its own spot on the ship, in the block grid the ship
     * snaps to. Used for living passengers when they get off and for boats, which are placed there directly.
     */
    public Vec3 landingSpot(Entity passenger) {
        // The ship lands as soon as the pilot leaves, snapped to the block grid and to 90 degrees.
        // Put the passenger on its spot of the landed ship (a player on top of the seat block).
        BlockPos anchor = AirshipAssembler.landingAnchor(this);
        Vec3 anchorPos = new Vec3(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
        double snappedRad = -Math.toRadians(AirshipAssembler.snappedYaw(getYRot()));

        Integer assigned = seatByPassenger.get(passenger.getId());
        if (assigned != null && assigned >= 0 && assigned < seats.size()) {
            return anchorPos.add(seats.get(assigned).yRot((float) snappedRad));
        }
        Vec3 best = anchorPos.add(seats.get(0).yRot((float) snappedRad));
        double bestDistance = Double.MAX_VALUE;
        for (Vec3 seat : seats) {
            double distance = position().add(toWorldOffset(seat)).distanceToSqr(passenger.position());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = anchorPos.add(seat.yRot((float) snappedRad));
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ smooth rendering (client)

    public Vec3 getSmoothPos(float partialTick) {
        if (!smoothReady) {
            return position();
        }
        return new Vec3(
                Mth.lerp(partialTick, prevPos.x, curPos.x),
                Mth.lerp(partialTick, prevPos.y, curPos.y),
                Mth.lerp(partialTick, prevPos.z, curPos.z));
    }

    public float getSmoothYaw(float partialTick) {
        if (!smoothReady) {
            return getYRot();
        }
        return prevYaw + Mth.wrapDegrees(curYaw - prevYaw) * partialTick;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel) {
            serverTick(serverLevel);
        } else {
            clientTick();
        }
    }

    private void clientTick() {
        if (cells.isEmpty()) {
            AirshipBlocksPayload pending = AirshipPendingCells.take(getId());
            if (pending != null) {
                setClientData(pending);
            }
        }
        clientAdvance();
        advanced = false;
        spawnEngineSmoke();
    }

    /** Client side: already moved this tick (by {@link #clientAdvance} from the start of the client tick). */
    private boolean advanced;

    /**
     * Client side: moves the ship one tick along the snapshots from the server. Called at the start of each
     * client tick, before players move, so a player standing on the ship is carried first and then walks and
     * collides with the ship where it is in this tick (otherwise a rising ship could push into the player's feet).
     * The ship's own tick calls it again; it only moves once per tick.
     */
    public void clientAdvance() {
        if (advanced) {
            return;
        }
        advanced = true;
        applySnapshots();

        // The server sends the position 20 times per second. Remember the last two positions so the
        // renderer can blend between them instead of jumping.
        if (!smoothReady) {
            prevPos = position();
            curPos = position();
            prevYaw = getYRot();
            curYaw = getYRot();
            smoothReady = true;
        } else {
            prevPos = curPos;
            prevYaw = curYaw;
            curPos = position();
            curYaw = getYRot();
        }
    }

    /** Client side: a new snapshot of the ship's position and rotation arrived from the server. */
    public void receiveSnapshot(AirshipStatePayload payload) {
        Snapshot snapshot = new Snapshot(payload.tick(), new Vec3(payload.x(), payload.y(), payload.z()), payload.yaw());
        if (!snapshots.isEmpty()) {
            Snapshot last = snapshots.get(snapshots.size() - 1);
            if (snapshot.tick() < last.tick()) {
                return; // older than what we already have
            }
            if (snapshot.tick() == last.tick()) {
                snapshots.set(snapshots.size() - 1, snapshot);
                return;
            }
        }
        snapshots.add(snapshot);
        latestTick = snapshot.tick();
        if (!snapshotsReady) {
            snapshotsReady = true;
            renderTick = latestTick - INTERPOLATION_DELAY_TICKS;
        }
        while (snapshots.size() > 16) {
            snapshots.remove(0);
        }
    }

    /**
     * Moves the client ship along the buffered snapshots at a steady pace (one server tick per client tick),
     * a couple of ticks behind the latest one. Uneven packet arrival is absorbed by that buffer.
     */
    private void applySnapshots() {
        if (!snapshotsReady || snapshots.isEmpty()) {
            return;
        }
        renderTick = Mth.clamp(renderTick + 1.0, latestTick - 3.0, (double) latestTick);

        Snapshot first = snapshots.get(0);
        Snapshot last = snapshots.get(snapshots.size() - 1);
        Vec3 pos = last.pos();
        float yaw = last.yaw();
        if (renderTick <= first.tick()) {
            pos = first.pos();
            yaw = first.yaw();
        } else if (renderTick < last.tick()) {
            for (int i = 0; i < snapshots.size() - 1; i++) {
                Snapshot a = snapshots.get(i);
                Snapshot b = snapshots.get(i + 1);
                if (renderTick >= a.tick() && renderTick <= b.tick()) {
                    double f = (renderTick - a.tick()) / (double) (b.tick() - a.tick());
                    pos = new Vec3(
                            a.pos().x + (b.pos().x - a.pos().x) * f,
                            a.pos().y + (b.pos().y - a.pos().y) * f,
                            a.pos().z + (b.pos().z - a.pos().z) * f);
                    yaw = a.yaw() + Mth.wrapDegrees(b.yaw() - a.yaw()) * (float) f;
                    break;
                }
            }
        }

        while (snapshots.size() > 2 && snapshots.get(1).tick() <= renderTick) {
            snapshots.remove(0);
        }
        setPos(pos.x, pos.y, pos.z);
        setYRot(yaw);
    }

    /** Server side: the current state, as sent to clients. */
    public AirshipStatePayload statePayload(long gameTick) {
        Vec3 pos = position();
        return new AirshipStatePayload(getId(), gameTick, pos.x, pos.y, pos.z, getYRot());
    }

    /** Server side: tell everybody who sees the ship where it is (only when it changed, plus a heartbeat). */
    private void broadcastState(ServerLevel level) {
        Vec3 pos = position();
        float yaw = getYRot();
        long now = level.getGameTime();
        if (pos.equals(lastSentPos) && yaw == lastSentYaw && now - lastSentTick < 20) {
            return;
        }
        lastSentPos = pos;
        lastSentYaw = yaw;
        lastSentTick = now;
        AirshipStatePayload payload = statePayload(now);
        for (ServerPlayer tracking : PlayerLookup.tracking(this)) {
            ServerPlayNetworking.send(tracking, payload);
        }
    }

    /** The pilot is whoever sits on the control seat (seat 0). Cushion sitters are only passengers. */
    private ServerPlayer findPilot() {
        List<Entity> passengers = getPassengers();
        for (int i = 0; i < passengers.size(); i++) {
            if (!(passengers.get(i) instanceof ServerPlayer player)) {
                continue;
            }
            Integer seat = seatByPassenger.get(player.getId());
            // Without a seat assignment (e.g. after loading the world) the first passenger is the pilot.
            if (seat != null ? seat == 0 : (seatByPassenger.isEmpty() && i == 0)) {
                return player;
            }
        }
        return null;
    }

    private void serverTick(ServerLevel level) {
        if (cells.isEmpty()) {
            discard();
            return;
        }

        resting = AirshipCollision.collides(level, this, position().add(0.0, -0.05, 0.0), getYRot());

        // Without a pilot on the control seat the ship stays where it is, hovering. Only when it rests on the
        // ground does it turn back into solid blocks (passengers on cushions are put down on their cushions).
        ServerPlayer pilot = findPilot();
        if (pilot == null && !resting) {
            hoverTick(level);
            return;
        }
        if (pilot == null) {
            velocity = Vec3.ZERO;
            if (landCooldown > 0) {
                landCooldown--;
            } else if (!AirshipAssembler.disassemble(level, this)) {
                landCooldown = 20;
                if (level.getGameTime() % 100 == 0) {
                    for (ServerPlayer nearby : level.getEntitiesOfClass(
                            ServerPlayer.class, getBoundingBox().inflate(32.0))) {
                        nearby.sendOverlayMessage(Component.translatable("message.airship.cannot_land"));
                    }
                }
            }
            return;
        }

        if (leadRecoveryTicks > 0) {
            leadRecoveryTicks--;
            for (ItemEntity dropped : level.getEntitiesOfClass(
                    ItemEntity.class, getBoundingBox().inflate(24.0), item -> item.getItem().is(Items.LEAD))) {
                ItemStack stack = dropped.getItem();
                pilot.getInventory().add(stack);
                if (stack.isEmpty()) {
                    dropped.discard();
                }
            }
        }

        int flags = AirshipControls.flags(pilot, level.getGameTime());

        // --- turn so that the front of the ship (the way the Airship Seat faces) points where the pilot looks ---
        float target = pilot.getYRot() - controlYaw;
        float diff = Mth.wrapDegrees(target - getYRot());
        float step = Mth.clamp(diff, -MAX_TURN, MAX_TURN);
        if (Math.abs(step) > 1.0E-3F) {
            float newYaw = Mth.wrapDegrees(getYRot() + step);
            // Turn around the pilot's seat, not the Core: otherwise the pilot's camera swings around
            // the Core on a circle every time the ship turns, which looks like violent shaking.
            Vec3 pivot = getPivot();
            Vec3 shift = rotateLocal(pivot, getYRot()).subtract(rotateLocal(pivot, newYaw));
            Vec3 newPos = position().add(shift);
            if (!AirshipCollision.collides(level, this, newPos, newYaw)) {
                setPos(newPos.x, newPos.y, newPos.z);
                setYRot(newYaw);
            }
        }

        // --- horizontal input, relative to the pilot's look direction ---
        float lookYaw = pilot.getYRot();
        double yawRad = Math.toRadians(lookYaw);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);

        double inputX = 0.0;
        double inputZ = 0.0;
        if ((flags & AirshipControlPayload.FORWARD) != 0) {
            inputX += forwardX;
            inputZ += forwardZ;
        }
        if ((flags & AirshipControlPayload.BACKWARD) != 0) {
            inputX -= forwardX;
            inputZ -= forwardZ;
        }
        if ((flags & AirshipControlPayload.RIGHT) != 0) {
            inputX += rightX;
            inputZ += rightZ;
        }
        if ((flags & AirshipControlPayload.LEFT) != 0) {
            inputX -= rightX;
            inputZ -= rightZ;
        }
        double length = Math.hypot(inputX, inputZ);
        boolean thrusting = length > 1.0E-4;

        // --- engines: each fuelled engine makes the ship faster and burns fuel while thrusting.
        // While the boost key is held, an engine with turbo fuel burns that instead (5x as fast) for a big
        // speed boost; engines without turbo fuel keep using their normal tank. ---
        boolean wantBoost = thrusting && (flags & AirshipControlPayload.BOOST) != 0;
        int normalActive = 0;
        int turboActive = 0;
        double activePower = 0.0; // a Simple Engine only gives part of the fuel speed boost
        for (Map.Entry<BlockPos, Integer> entry : engineFuel.entrySet()) {
            if (wantBoost && turboFuel.getOrDefault(entry.getKey(), 0) > 0) {
                turboActive++;
                BlockState turboState = stateByPos.get(entry.getKey());
                activePower += turboState == null ? 1.0 : ModBlocks.enginePower(turboState);
            } else if (entry.getValue() > 0) {
                normalActive++;
                BlockState engineState = stateByPos.get(entry.getKey());
                activePower += engineState == null ? 1.0 : ModBlocks.enginePower(engineState);
            }
        }
        int activeEngines = normalActive + turboActive;
        double turboShare = engineFuel.isEmpty() ? 0.0 : (double) turboActive / engineFuel.size();
        // Top speed: BASE with no fuel, FUEL with fuel, TURBO with turbo; engines count by their share.
        int engineCount = engineFuel.size();
        double fuelShare = engineCount == 0 ? 0.0 : activePower / engineCount;
        double maxSpeed = BASE_SPEED + (FUEL_SPEED - BASE_SPEED) * fuelShare + (TURBO_SPEED - FUEL_SPEED) * turboShare;
        double accel = HORIZONTAL_ACCEL * (1.0 + (TURBO_ACCEL_MULTIPLIER - 1.0) * turboShare);
        if (thrusting) {
            for (Map.Entry<BlockPos, Integer> entry : engineFuel.entrySet()) {
                int turbo = turboFuel.getOrDefault(entry.getKey(), 0);
                if (wantBoost && turbo > 0) {
                    int rate = ModBlocks.turboBurnRate(stateByPos.get(entry.getKey()));
                    turboFuel.put(entry.getKey(), Math.max(0, turbo - rate));
                } else if (entry.getValue() > 0) {
                    int burn = ModBlocks.engineBurnRate(stateByPos.get(entry.getKey()));
                    entry.setValue(Math.max(0, entry.getValue() - burn));
                }
            }
        }

        double targetX = thrusting ? inputX / length * maxSpeed : 0.0;
        double targetZ = thrusting ? inputZ / length * maxSpeed : 0.0;

        // --- vertical: balloons carry the ship, so with no input it simply hovers ---
        double targetY = 0.0;
        boolean up = (flags & AirshipControlPayload.UP) != 0;
        boolean down = (flags & AirshipControlPayload.DOWN) != 0;
        if (lift < requiredLift) {
            // balloons were shot down: the ship can no longer climb and slowly sinks
            targetY = down ? -VERTICAL_SPEED : -SINK_SPEED;
            if (level.getGameTime() % 40 == 0) {
                pilot.sendOverlayMessage(Component.translatable("message.airship.sinking"));
            }
        } else if (up) {
            targetY = VERTICAL_SPEED; // ascending always wins if both keys are held
        } else if (down) {
            targetY = -VERTICAL_SPEED;
        }

        velocity = new Vec3(
                approach(velocity.x, targetX, accel),
                approach(velocity.y, targetY, VERTICAL_ACCEL),
                approach(velocity.z, targetZ, accel)
        );

        // --- move with collision ---
        if (velocity.lengthSqr() > 1.0E-10) {
            AirshipCollision.Result result = AirshipCollision.move(level, this, position(), velocity);
            velocity = new Vec3(
                    result.hitX() ? 0.0 : velocity.x,
                    result.hitY() ? 0.0 : velocity.y,
                    result.hitZ() ? 0.0 : velocity.z);
            Vec3 newPos = result.pos();
            double lowest = level.getMinY() + 1 - minCellY;
            double highest = Math.max(lowest, level.getMaxY() - 1 - maxCellY);
            double clampedY = Mth.clamp(newPos.y, lowest, highest);
            if (clampedY != newPos.y) {
                newPos = new Vec3(newPos.x, clampedY, newPos.z);
                velocity = new Vec3(velocity.x, 0.0, velocity.z);
            }
            setPos(newPos.x, newPos.y, newPos.z);
        }
        setDeltaMovement(velocity);
        checkProjectiles(level);
        broadcastState(level);
        sendEngineState(level, !thrusting || activeEngines == 0 ? AirshipEnginePayload.OFF
                : turboActive > 0 ? AirshipEnginePayload.TURBO : AirshipEnginePayload.RUNNING);

        // Flight display for the pilot (a few times per second is plenty).
        if (level.getGameTime() % 5 == 0) {
            int fuelMax = 0;
            int turboMax = 0;
            for (Map.Entry<BlockPos, Integer> entry : engineFuel.entrySet()) {
                // shown as thrust time: a Simple Engine's tank empties faster
                fuelMax = Math.max(fuelMax, entry.getValue() / ModBlocks.engineBurnRate(stateByPos.get(entry.getKey())));
            }
            for (Map.Entry<BlockPos, Integer> entry : turboFuel.entrySet()) {
                // sent as if burnt at the normal rate, so the display shows the turbo time of every engine kind
                BlockState engineState = stateByPos.get(entry.getKey());
                long normalized = (long) entry.getValue() * TURBO_BURN_RATE / ModBlocks.turboBurnRate(engineState);
                turboMax = (int) Math.max(turboMax, Math.min(Integer.MAX_VALUE, normalized));
            }
            ServerPlayNetworking.send(pilot, new AirshipHudPayload(
                    fuelMax, turboMax, engineFuel.size(), activeEngines,
                    (float) (velocity.length() * 20.0), turboActive > 0));
        }
    }

    /**
     * A ship nobody steers: it stays in the air. Without enough lift (balloons shot down) it slowly sinks until
     * it rests on the ground, where it lands.
     */
    private void hoverTick(ServerLevel level) {
        velocity = Vec3.ZERO;
        if (lift < requiredLift) {
            AirshipCollision.Result result = AirshipCollision.move(level, this, position(), new Vec3(0.0, -SINK_SPEED, 0.0));
            Vec3 newPos = result.pos();
            double lowest = level.getMinY() + 1 - minCellY;
            setPos(newPos.x, Math.max(lowest, newPos.y), newPos.z);
            if (!result.hitY()) {
                velocity = new Vec3(0.0, -SINK_SPEED, 0.0);
            }
        }
        setDeltaMovement(velocity);
        checkProjectiles(level);
        broadcastState(level);
        sendEngineState(level, AirshipEnginePayload.OFF);
    }

    /** Server side: tells everybody who sees the ship when its engines start, stop or switch to turbo. */
    private void sendEngineState(ServerLevel level, int state) {
        boolean heartbeat = state != AirshipEnginePayload.OFF && level.getGameTime() % 40 == 0;
        if (state == engineState && !heartbeat) {
            return;
        }
        engineState = state;
        AirshipEnginePayload payload = new AirshipEnginePayload(getId(), state);
        for (ServerPlayer tracking : PlayerLookup.tracking(this)) {
            ServerPlayNetworking.send(tracking, payload);
        }
    }

    /** Client side: what the engines do now. */
    public void setEngineState(int state) {
        this.engineState = state;
    }

    /**
     * Client side: running engines smoke from their top. The faster the ship, the more smoke and the faster it
     * puffs out; with turbo it is thick, dark smoke. The smoke is left behind as the ship moves on.
     */
    private void spawnEngineSmoke() {
        if (engineState == AirshipEnginePayload.OFF || engineCells.isEmpty() || !smoothReady) {
            return;
        }
        boolean turbo = engineState == AirshipEnginePayload.TURBO;
        Vec3 motion = curPos.subtract(prevPos);
        double speed = motion.length(); // blocks per tick
        double perEngine = (0.25 + speed * 1.6) * (turbo ? 1.8 : 1.0);
        SimpleParticleType smoke = turbo ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE;
        for (BlockPos cell : engineCells) {
            int count = (int) perEngine + (random.nextDouble() < perEngine - (int) perEngine ? 1 : 0);
            Vec3 top = position().add(toWorldOffset(new Vec3(cell.getX(), cell.getY() + 1.02, cell.getZ())));
            for (int i = 0; i < count; i++) {
                double up = 0.04 + speed * 0.08 + random.nextDouble() * 0.03;
                level().addParticle(i % 3 == 2 ? ParticleTypes.LARGE_SMOKE : smoke,
                        top.x + (random.nextDouble() - 0.5) * 0.5,
                        top.y + random.nextDouble() * 0.1,
                        top.z + (random.nextDouble() - 0.5) * 0.5,
                        -motion.x * 0.25 + (random.nextDouble() - 0.5) * 0.02,
                        up,
                        -motion.z * 0.25 + (random.nextDouble() - 0.5) * 0.02);
            }
        }
    }

    // ------------------------------------------------------------------ projectiles

    /**
     * Balloons of a flying ship can be shot down. The ship's blocks only exist inside this entity, so the path
     * of every projectile near the ship is followed through the ship's block grid. The first balloon on the path
     * pops if the projectile is strong enough (see {@link AirshipBalloons#pops}); otherwise the projectile
     * bounces off it.
     */
    private void checkProjectiles(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            projectileHits.keySet().removeIf(id -> level.getEntity(id) == null);
        }
        if (lift == 0) {
            return;
        }
        Vec3 origin = position();
        AABB area = new AABB(
                origin.x - radius - 1.0, origin.y + minCellY, origin.z - radius - 1.0,
                origin.x + radius + 1.0, origin.y + maxCellY + 1.0, origin.z + radius + 1.0).inflate(4.0);
        double angle = -Math.toRadians(getYRot());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        boolean changed = false;

        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area)) {
            if (projectile.isRemoved() || projectileHits.getOrDefault(projectile.getId(), 1) <= 0) {
                continue;
            }
            // From where it was last tick to where it will be next tick (it may be ticked before or after us).
            Vec3 from = new Vec3(projectile.xo, projectile.yo, projectile.zo);
            Vec3 to = projectile.position().add(projectile.getDeltaMovement());
            double length = from.distanceTo(to);
            if (length < 1.0E-4) {
                continue; // lying still (an arrow stuck in the ground)
            }
            int steps = Math.max(1, (int) Math.ceil(length / 0.2));
            BlockPos last = null;
            for (int i = 0; i <= steps; i++) {
                Vec3 point = from.lerp(to, (double) i / steps);
                double dx = point.x - origin.x;
                double dz = point.z - origin.z;
                BlockPos cell = new BlockPos(
                        Mth.floor(dx * cos - dz * sin + 0.5),
                        Mth.floor(point.y - origin.y),
                        Mth.floor(dx * sin + dz * cos + 0.5));
                if (cell.equals(last)) {
                    continue;
                }
                last = cell;
                BlockState state = stateByPos.get(cell);
                if (state == null) {
                    continue;
                }
                if (!AirshipBalloons.isBalloon(state)) {
                    break; // the hull shields what is behind it
                }
                if (AirshipBalloons.pops(state, projectile)) {
                    popBalloon(level, cell, point);
                    changed = true;
                    // A Piercing arrow pops one more balloon per level, everything else stops at the first one.
                    int left = projectileHits.getOrDefault(projectile.getId(), 1 + AirshipBalloons.piercingLevel(projectile)) - 1;
                    projectileHits.put(projectile.getId(), left);
                    if (left <= 0) {
                        stopProjectile(projectile, true);
                        break;
                    }
                } else {
                    projectileHits.put(projectile.getId(), 0);
                    stopProjectile(projectile, false); // too weak: it bounces off
                    break;
                }
            }
        }

        if (changed) {
            setCells(cellsWithoutPopped());
            AirshipBlocksPayload payload = new AirshipBlocksPayload(getId(), clientCells(), getClientCushions(), getSeats());
            for (ServerPlayer tracking : PlayerLookup.tracking(this)) {
                ServerPlayNetworking.send(tracking, payload);
            }
        }
    }

    /** Cells that were popped this tick; removed (together with the current fuel) by {@link #cellsWithoutPopped}. */
    private final List<BlockPos> popped = new ArrayList<>();

    private void popBalloon(ServerLevel level, BlockPos cell, Vec3 where) {
        stateByPos.remove(cell); // so the same projectile cannot pop it twice this tick
        popped.add(cell);
        level.playSound(null, where.x, where.y, where.z, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 1.4F);
        level.sendParticles(ParticleTypes.POOF, where.x, where.y, where.z, 8, 0.3, 0.3, 0.3, 0.02);
    }

    /** All cells with the current fuel, minus the popped balloons. */
    private List<AirshipCell> cellsWithoutPopped() {
        List<AirshipCell> result = getCellsWithFuel();
        for (Iterator<AirshipCell> it = result.iterator(); it.hasNext(); ) {
            if (popped.contains(it.next().pos())) {
                it.remove();
            }
        }
        popped.clear();
        return result;
    }

    /**
     * A projectile that was too weak bounces off. One that popped a balloon is used up, except tridents, which
     * bounce back so that Loyalty still brings them home.
     */
    private static void stopProjectile(Projectile projectile, boolean popped) {
        if (!popped || projectile.getType() == EntityTypes.TRIDENT) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.1));
        } else {
            projectile.discard();
        }
    }

    private static double approach(double current, double target, double step) {
        return current + Mth.clamp(target - current, -step, step);
    }
}
