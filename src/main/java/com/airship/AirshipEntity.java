package com.airship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * An assembled airship. All blocks of the ship live inside this entity (relative to the former
 * Core position). The ship stays assembled until a player lands it (sneak + right-click),
 * hovers in place when nobody is flying it, and turns to face the way the pilot looks.
 */
public class AirshipEntity extends Entity {
    // --- flight tuning (blocks per tick) ---
    private static final double BASE_SPEED = 0.10;
    private static final double ENGINE_BOOST = 0.22;
    private static final double ENGINE_FALLOFF = 0.70;
    private static final double VERTICAL_SPEED = 0.08;
    private static final double HORIZONTAL_ACCEL = 0.012;
    private static final double VERTICAL_ACCEL = 0.02;
    /** Maximum turn rate in degrees per tick. */
    private static final float MAX_TURN = 3.5F;

    private List<AirshipCell> cells = List.of();
    private final Map<BlockPos, BlockState> stateByPos = new HashMap<>();
    private final Map<BlockPos, Integer> engineFuel = new HashMap<>();
    private List<Vec3> seats = List.of();
    private BlockPos[] surface;
    private int[] surfaceMask;
    private List<AirshipClientCell> exposed;
    private int cellVersion;

    // server only
    private Vec3 velocity = Vec3.ZERO;
    private ServerPlayer lastPilot;
    private float yawOffset;

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
            converted.add(new AirshipCell(cell.pos(), cell.state(), Optional.empty(), 0));
        }
        setCells(converted);
    }

    private void rebuild() {
        stateByPos.clear();
        engineFuel.clear();
        for (AirshipCell cell : cells) {
            BlockPos pos = cell.pos().immutable();
            stateByPos.put(pos, cell.state());
            if (cell.state().is(ModBlocks.AIRSHIP_ENGINE)) {
                engineFuel.put(pos, cell.fuel());
            }
        }

        List<Vec3> found = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> entry : stateByPos.entrySet()) {
            if (entry.getValue().is(ModBlocks.AIRSHIP_SEAT)) {
                BlockPos p = entry.getKey();
                found.add(new Vec3(p.getX(), p.getY() + AirshipSeatBlock.SEAT_HEIGHT, p.getZ()));
            }
        }
        // Seats closest to the Core come first.
        found.sort(Comparator.comparingDouble(Vec3::lengthSqr)
                .thenComparingDouble(v -> v.y)
                .thenComparingDouble(v -> v.x)
                .thenComparingDouble(v -> v.z));
        this.seats = List.copyOf(found);

        this.surface = null;
        this.surfaceMask = null;
        this.exposed = null;
        this.cellVersion++;
    }

    public List<AirshipCell> getCells() {
        return cells;
    }

    /** All cells with the current engine fuel merged in (used for saving and landing). */
    public List<AirshipCell> getCellsWithFuel() {
        List<AirshipCell> result = new ArrayList<>(cells.size());
        for (AirshipCell cell : cells) {
            result.add(new AirshipCell(
                    cell.pos(), cell.state(), cell.blockEntity(),
                    engineFuel.getOrDefault(cell.pos(), 0)));
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
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        output.store("Cells", AirshipCell.CODEC.listOf(), getCellsWithFuel());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    /** Pickable so players can right-click the ship to board it or to land it. */
    @Override
    public boolean isPickable() {
        return true;
    }

    // ------------------------------------------------------------------ seats

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
        int index = Math.max(0, getPassengers().indexOf(passenger));
        return toWorldOffset(seats.get(index % seats.size()));
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (seats.isEmpty()) {
            return super.getDismountLocationForPassenger(passenger);
        }
        // Put the player back on the seat they were sitting on.
        Vec3 best = position().add(toWorldOffset(seats.get(0)));
        double bestDistance = Double.MAX_VALUE;
        for (Vec3 seat : seats) {
            Vec3 world = position().add(toWorldOffset(seat));
            double distance = world.distanceToSqr(passenger.position());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = world;
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
            List<AirshipClientCell> pending = AirshipPendingCells.take(getId());
            if (pending != null) {
                setClientCells(pending);
            }
        }

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

    private void serverTick(ServerLevel level) {
        if (cells.isEmpty()) {
            discard();
            return;
        }

        Entity first = getFirstPassenger();
        ServerPlayer pilot = first instanceof ServerPlayer sp ? sp : null;
        int flags = pilot == null ? 0 : AirshipControls.flags(pilot, level.getGameTime());

        // --- turn towards where the pilot looks (relative to how they were facing when they sat down) ---
        if (pilot != lastPilot) {
            lastPilot = pilot;
            if (pilot != null) {
                yawOffset = Mth.wrapDegrees(getYRot() - pilot.getYRot());
            }
        }
        if (pilot != null) {
            float target = pilot.getYRot() + yawOffset;
            float diff = Mth.wrapDegrees(target - getYRot());
            float step = Mth.clamp(diff, -MAX_TURN, MAX_TURN);
            if (Math.abs(step) > 1.0E-3F) {
                float newYaw = Mth.wrapDegrees(getYRot() + step);
                if (!AirshipCollision.collides(level, this, position(), newYaw)) {
                    setYRot(newYaw);
                }
            }
        }

        // --- horizontal input, relative to the pilot's look direction ---
        float lookYaw = pilot == null ? 0.0F : pilot.getYRot();
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

        // --- engines: each fuelled engine makes the ship faster and burns fuel while thrusting ---
        int activeEngines = 0;
        for (int fuel : engineFuel.values()) {
            if (fuel > 0) {
                activeEngines++;
            }
        }
        double maxSpeed = BASE_SPEED + ENGINE_BOOST * (1.0 - Math.pow(ENGINE_FALLOFF, activeEngines));
        if (thrusting && activeEngines > 0) {
            for (Map.Entry<BlockPos, Integer> entry : engineFuel.entrySet()) {
                if (entry.getValue() > 0) {
                    entry.setValue(entry.getValue() - 1);
                }
            }
        }

        double targetX = thrusting ? inputX / length * maxSpeed : 0.0;
        double targetZ = thrusting ? inputZ / length * maxSpeed : 0.0;

        // --- vertical: balloons carry the ship, so with no input it simply hovers ---
        double targetY = 0.0;
        boolean up = (flags & AirshipControlPayload.UP) != 0;
        boolean down = (flags & AirshipControlPayload.DOWN) != 0;
        if (up && !down) {
            targetY = VERTICAL_SPEED;
        } else if (down && !up) {
            targetY = -VERTICAL_SPEED;
        }

        velocity = new Vec3(
                approach(velocity.x, targetX, HORIZONTAL_ACCEL),
                approach(velocity.y, targetY, VERTICAL_ACCEL),
                approach(velocity.z, targetZ, HORIZONTAL_ACCEL)
        );

        // --- move with collision ---
        if (velocity.lengthSqr() > 1.0E-10) {
            AirshipCollision.Result result = AirshipCollision.move(level, this, position(), velocity);
            velocity = new Vec3(
                    result.hitX() ? 0.0 : velocity.x,
                    result.hitY() ? 0.0 : velocity.y,
                    result.hitZ() ? 0.0 : velocity.z);
            setPos(result.pos().x, result.pos().y, result.pos().z);
        }
        setDeltaMovement(velocity);
    }

    private static double approach(double current, double target, double step) {
        return current + Mth.clamp(target - current, -step, step);
    }
}
