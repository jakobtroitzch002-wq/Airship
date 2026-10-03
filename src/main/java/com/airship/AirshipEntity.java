package com.airship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * An assembled, flying airship. All blocks of the ship live inside this entity
 * (relative to the former Core position) until the ship lands and is disassembled.
 * <p>
 * The ship never rotates in this version: it only translates. The pilot (first passenger)
 * steers relative to their look direction, similar to a happy ghast.
 */
public class AirshipEntity extends Entity {
    // --- flight tuning (blocks per tick) ---
    private static final double BASE_SPEED = 0.10;
    private static final double ENGINE_BOOST = 0.22;
    private static final double ENGINE_FALLOFF = 0.70;
    private static final double VERTICAL_SPEED = 0.08;
    private static final double SINK_SPEED = 0.06;
    private static final double HORIZONTAL_ACCEL = 0.012;
    private static final double VERTICAL_ACCEL = 0.02;
    private static final int LAND_RETRY_TICKS = 20;

    private List<AirshipCell> cells = List.of();
    private final Map<BlockPos, BlockState> stateByPos = new HashMap<>();
    private final Map<BlockPos, Integer> engineFuel = new HashMap<>();
    private List<Vec3> seats = List.of();
    private Map<Direction, BlockPos[]> leading;
    private List<AirshipClientCell> exposed;
    private int cellVersion;

    private Vec3 velocity = Vec3.ZERO;
    private int groundTicks;
    private int landCooldown;

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
        // Seats closest to the Core come first, so the player who started the ship gets the "best" one.
        found.sort(Comparator.comparingDouble(Vec3::lengthSqr)
                .thenComparingDouble(v -> v.y)
                .thenComparingDouble(v -> v.x)
                .thenComparingDouble(v -> v.z));
        this.seats = List.copyOf(found);

        this.leading = null;
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

    public BlockPos[] leadingCells(Direction direction) {
        if (leading == null) {
            Map<Direction, List<BlockPos>> lists = new EnumMap<>(Direction.class);
            for (Direction d : Direction.values()) {
                lists.put(d, new ArrayList<>());
            }
            for (BlockPos pos : stateByPos.keySet()) {
                for (Direction d : Direction.values()) {
                    if (!stateByPos.containsKey(pos.relative(d))) {
                        lists.get(d).add(pos);
                    }
                }
            }
            Map<Direction, BlockPos[]> arrays = new EnumMap<>(Direction.class);
            for (Direction d : Direction.values()) {
                arrays.put(d, lists.get(d).toArray(new BlockPos[0]));
            }
            leading = arrays;
        }
        return leading.get(direction);
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

    // ------------------------------------------------------------------ seats

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
        return seats.get(index % seats.size());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (seats.isEmpty()) {
            return super.getDismountLocationForPassenger(passenger);
        }
        // Put the player back on the seat they were sitting on.
        Vec3 best = seats.get(0);
        double bestDistance = Double.MAX_VALUE;
        for (Vec3 seat : seats) {
            double distance = position().add(seat).distanceToSqr(passenger.position());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = seat;
            }
        }
        return position().add(best);
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
    }

    private void serverTick(ServerLevel level) {
        if (cells.isEmpty()) {
            discard();
            return;
        }

        boolean unpiloted = getPassengers().isEmpty();
        Entity first = getFirstPassenger();
        int flags = 0;
        float yaw = 0.0F;
        if (first instanceof ServerPlayer pilot) {
            flags = AirshipControls.flags(pilot, level.getGameTime());
            yaw = pilot.getYRot();
        }

        // --- horizontal input, relative to the pilot's look direction ---
        double yawRad = Math.toRadians(yaw);
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

        // --- vertical: balloons carry the ship; no pilot means it sinks slowly ---
        double targetY;
        if (unpiloted) {
            targetY = -SINK_SPEED;
        } else if ((flags & AirshipControlPayload.UP) != 0 && (flags & AirshipControlPayload.DOWN) == 0) {
            targetY = VERTICAL_SPEED;
        } else if ((flags & AirshipControlPayload.DOWN) != 0 && (flags & AirshipControlPayload.UP) == 0) {
            targetY = -VERTICAL_SPEED;
        } else {
            targetY = 0.0;
        }

        velocity = new Vec3(
                approach(velocity.x, targetX, HORIZONTAL_ACCEL),
                approach(velocity.y, targetY, VERTICAL_ACCEL),
                approach(velocity.z, targetZ, HORIZONTAL_ACCEL)
        );

        // --- move with collision ---
        AirshipCollision.Result result = AirshipCollision.move(level, this, position(), velocity);
        double vx = result.hitX() ? 0.0 : velocity.x;
        double vy = result.hitY() ? 0.0 : velocity.y;
        double vz = result.hitZ() ? 0.0 : velocity.z;
        boolean touchedGround = result.hitY() && velocity.y < 0.0;
        velocity = new Vec3(vx, vy, vz);
        setPos(result.pos().x, result.pos().y, result.pos().z);
        setDeltaMovement(velocity);

        // --- landing: an empty ship that has touched the ground turns back into blocks ---
        groundTicks = touchedGround ? groundTicks + 1 : 0;
        if (landCooldown > 0) {
            landCooldown--;
        }
        if (unpiloted && groundTicks >= 2 && landCooldown == 0) {
            if (!AirshipAssembler.disassemble(level, this)) {
                landCooldown = LAND_RETRY_TICKS;
            }
        }
    }

    private static double approach(double current, double target, double step) {
        return current + Mth.clamp(target - current, -step, step);
    }

    public boolean hasPilot() {
        return getFirstPassenger() instanceof Player;
    }
}
