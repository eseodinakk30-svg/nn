package com.dunesrelics.memory;

import com.dunesrelics.network.CurrentFieldMessage;
import com.dunesrelics.network.ModNetwork;
import com.dunesrelics.registry.ModTags;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Water that goes somewhere. Rivers flow along their bed towards the sea, strongly enough to carry a boat; out at
 * sea, broad streams run along bands a few dozen blocks wide (the same for everyone, from a fixed noise), fast
 * enough to be taken as a road, with only a gentle drift in between.
 * <p>
 * The river flow needs a look at the land around (which way is the sea), so the server works it out, 8 x 8 blocks
 * at a time, and sends each player the flow around them; the ocean streams the client can work out itself.
 */
public final class Currents {
    public static final double RIVER = 0.02D;
    public static final double STREAM = 0.04D;
    public static final double DRIFT = 0.003D;
    private static final double STREAM_WIDTH = 0.07D;
    private static final ImprovedNoise STREAMS = new ImprovedNoise(RandomSource.create(918273645L));
    /** Size of the field sent to each player: 9 x 9 cells of 8 x 8 blocks around them. */
    public static final int FIELD = 9;
    public static final int CELL = 8;

    private static final Long2ObjectOpenHashMap<Vec3> RIVER_CELLS = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<Vec3> SEA_DIRECTIONS = new Long2ObjectOpenHashMap<>();

    private Currents() {}

    // ------------------------------------------------------------------------------------------ the sea

    /** The ocean stream at (x, z): strong along the bands, a slow drift elsewhere. Same on server and client. */
    public static Vec3 oceanStream(double x, double z) {
        double scale = 1.0D / 384.0D;
        double n = STREAMS.noise(x * scale, 0.0D, z * scale);
        double gx = STREAMS.noise((x + 4.0D) * scale, 0.0D, z * scale) - STREAMS.noise((x - 4.0D) * scale, 0.0D, z * scale);
        double gz = STREAMS.noise(x * scale, 0.0D, (z + 4.0D) * scale) - STREAMS.noise(x * scale, 0.0D, (z - 4.0D) * scale);
        double length = Math.sqrt(gx * gx + gz * gz);
        if (length < 1.0E-9D) {
            return Vec3.ZERO;
        }
        // along the contour, with the higher side on the left: the same way all along a band
        double ax = -gz / length;
        double az = gx / length;
        double strength = Math.abs(n) < STREAM_WIDTH ? STREAM * (1.0D - Math.abs(n) / STREAM_WIDTH) + DRIFT : DRIFT;
        return new Vec3(ax * strength, 0.0D, az * strength);
    }

    public static boolean isRiver(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_RIVER);
    }

    // ------------------------------------------------------------------------------------------ rivers (server)

    /** Which way the nearest sea lies from here (worked out on a 64-block grid): unit vector, or zero if none near. */
    private static Vec3 seaDirection(ServerLevel level, int x, int z) {
        long key = BlockPos.asLong(x >> 6, 0, z >> 6);
        Vec3 cached = SEA_DIRECTIONS.get(key);
        if (cached != null) {
            return cached;
        }
        int cx = (x >> 6) * 64 + 32;
        int cz = (z >> 6) * 64 + 32;
        int y = level.getSeaLevel() - 1;
        Vec3 found = Vec3.ZERO;
        search:
        for (int distance = 32; distance <= 768; distance += 32) {
            for (int i = 0; i < 24; i++) {
                double angle = i * Math.PI * 2.0D / 24.0D;
                int sx = cx + (int) (Math.cos(angle) * distance);
                int sz = cz + (int) (Math.sin(angle) * distance);
                if (level.getBiome(new BlockPos(sx, y, sz)).is(BiomeTags.IS_OCEAN)) {
                    found = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
                    break search;
                }
            }
        }
        if (SEA_DIRECTIONS.size() > 20000) {
            SEA_DIRECTIONS.clear();
        }
        SEA_DIRECTIONS.put(key, found);
        return found;
    }

    /** How many blocks of open water run from (x, z) along (dx, dz), up to 32. */
    private static int waterRun(ServerLevel level, int x, int z, double dx, double dz) {
        int y = level.getSeaLevel() - 1;
        for (int step = 1; step <= 32; step++) {
            BlockPos pos = new BlockPos(x + (int) Math.round(dx * step), y, z + (int) Math.round(dz * step));
            if (!level.isLoaded(pos) || !level.getFluidState(pos).is(FluidTags.WATER)) {
                return step - 1;
            }
        }
        return 32;
    }

    /** The flow of a river cell: along the longest stretch of water through it, towards the sea. */
    private static Vec3 riverCell(ServerLevel level, int cellX, int cellZ) {
        long key = BlockPos.asLong(cellX, 0, cellZ);
        Vec3 cached = RIVER_CELLS.get(key);
        if (cached != null) {
            return cached;
        }
        int x = cellX * CELL + CELL / 2;
        int z = cellZ * CELL + CELL / 2;
        BlockPos center = new BlockPos(x, level.getSeaLevel() - 1, z);
        Vec3 flow = Vec3.ZERO;
        if (level.isLoaded(center) && isRiver(level.getBiome(center)) && level.getFluidState(center).is(FluidTags.WATER)) {
            double bestAngle = 0.0D;
            int best = -1;
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 8.0D;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                int run = waterRun(level, x, z, dx, dz) + waterRun(level, x, z, -dx, -dz);
                if (run > best) {
                    best = run;
                    bestAngle = angle;
                }
            }
            Vec3 axis = new Vec3(Math.cos(bestAngle), 0.0D, Math.sin(bestAngle));
            Vec3 sea = seaDirection(level, x, z);
            double sign = sea.lengthSqr() > 0.0D ? Math.signum(axis.dot(sea)) : 1.0D;
            flow = axis.scale((sign == 0.0D ? 1.0D : sign) * RIVER);
        }
        if (RIVER_CELLS.size() > 50000) {
            RIVER_CELLS.clear();
        }
        RIVER_CELLS.put(key, flow);
        return flow;
    }

    /** The current at a spot of water, as it is on the server. */
    public static Vec3 flow(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        if (isRiver(biome)) {
            return riverCell(level, Math.floorDiv(pos.getX(), CELL), Math.floorDiv(pos.getZ(), CELL));
        }
        if (biome.is(ModTags.HAS_CURRENTS)) {
            return oceanStream(pos.getX(), pos.getZ());
        }
        return Vec3.ZERO;
    }

    // ------------------------------------------------------------------------------------------ every tick

    /** Carries drifting boats, floating items and swimming mobs near players along with the water. */
    public static void tick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            AABB area = player.getBoundingBox().inflate(48.0D, 24.0D, 48.0D);
            for (Entity entity : level.getEntities((Entity) null, area, e -> e.isInWater() && (e instanceof ItemEntity
                    || e instanceof Boat boat && boat.getControllingPassenger() == null || e instanceof Mob))) {
                Vec3 push = flow(level, entity.blockPosition());
                if (push != Vec3.ZERO) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(entity instanceof Mob ? push.scale(0.5D) : push));
                }
            }
        }
    }

    /** Forgets the worked-out river cells (a different world may be loaded next). */
    public static void clear() {
        RIVER_CELLS.clear();
        SEA_DIRECTIONS.clear();
    }

    /**
     * Sends each player the river flow around them (the ocean streams the client works out itself), or that the
     * currents are switched off.
     */
    public static void sync(ServerLevel level, boolean on) {
        for (ServerPlayer player : level.players()) {
            if (!on) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), CurrentFieldMessage.off());
                continue;
            }
            int originX = Math.floorDiv(player.getBlockX(), CELL) - FIELD / 2;
            int originZ = Math.floorDiv(player.getBlockZ(), CELL) - FIELD / 2;
            if (!isRiver(level.getBiome(player.blockPosition())) && !nearRiver(level, player.blockPosition())) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new CurrentFieldMessage(originX, originZ, new byte[0], new byte[0]));
                continue;
            }
            byte[] angles = new byte[FIELD * FIELD];
            byte[] strengths = new byte[FIELD * FIELD];
            for (int i = 0; i < FIELD; i++) {
                for (int j = 0; j < FIELD; j++) {
                    Vec3 flow = riverCell(level, originX + i, originZ + j);
                    int index = i * FIELD + j;
                    if (flow.lengthSqr() > 0.0D) {
                        angles[index] = (byte) Math.round(Mth.atan2(flow.z, flow.x) / (Math.PI * 2.0D) * 256.0D);
                        strengths[index] = (byte) Math.min(127, Math.round(flow.length() / RIVER * 64.0D));
                    }
                }
            }
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new CurrentFieldMessage(originX, originZ, angles, strengths));
        }
    }

    private static boolean nearRiver(Level level, BlockPos pos) {
        for (int dx = -32; dx <= 32; dx += 16) {
            for (int dz = -32; dz <= 32; dz += 16) {
                if (isRiver(level.getBiome(pos.offset(dx, 0, dz)))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Decodes one cell of a field message. */
    public static Vec3 decode(byte angle, byte strength) {
        if (strength == 0) {
            return Vec3.ZERO;
        }
        double a = angle / 256.0D * Math.PI * 2.0D;
        double s = strength / 64.0D * RIVER;
        return new Vec3(Math.cos(a) * s, 0.0D, Math.sin(a) * s);
    }
}
