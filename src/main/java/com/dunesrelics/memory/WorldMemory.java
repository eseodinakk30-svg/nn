package com.dunesrelics.memory;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the world remembers about its players, saved with the overworld: where they build and live, what grew
 * around their homes, the villages near them, and the chronicle of it all.
 */
public class WorldMemory extends SavedData {
    private static final String ID = "dunesrelics_world_memory";
    /** Regions are 128 x 128 block cells. */
    public static final int REGION_SHIFT = 7;

    final Long2ObjectOpenHashMap<Region> regions = new Long2ObjectOpenHashMap<>();
    /** Chunks where a player has placed or broken blocks: the world never touches them (or their neighbours). */
    final LongOpenHashSet touched = new LongOpenHashSet();
    final Long2ObjectOpenHashMap<VillageRecord> villages = new Long2ObjectOpenHashMap<>();
    final List<Chronicle.Entry> chronicle = new ArrayList<>();
    long lastDay = -1L;
    long lastDusk = -1L;
    long lastNight = -1L;

    public static WorldMemory get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(WorldMemory::load, WorldMemory::new, ID);
    }

    // ------------------------------------------------------------------------------------------ regions

    public Region region(BlockPos pos) {
        int rx = pos.getX() >> REGION_SHIFT;
        int rz = pos.getZ() >> REGION_SHIFT;
        return this.regions.computeIfAbsent(ChunkPos.asLong(rx, rz), k -> new Region(rx, rz));
    }

    @Nullable
    public Region existingRegion(BlockPos pos) {
        return this.regions.get(ChunkPos.asLong(pos.getX() >> REGION_SHIFT, pos.getZ() >> REGION_SHIFT));
    }

    public Iterable<Region> regions() {
        return this.regions.values();
    }

    public void touch(BlockPos pos) {
        if (this.touched.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4))) {
            this.setDirty();
        }
    }

    /** Whether a player has built in this chunk or right next to it. */
    public boolean isProtected(BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (this.touched.contains(ChunkPos.asLong(cx + dx, cz + dz))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------ villages

    public VillageRecord village(BlockPos bell, long day, int name) {
        return this.villages.computeIfAbsent(bell.asLong(), k -> {
            this.setDirty();
            return new VillageRecord(bell.immutable(), name, day);
        });
    }

    @Nullable
    public VillageRecord nearestVillage(BlockPos pos, double maxDistance) {
        VillageRecord best = null;
        double bestDistance = maxDistance * maxDistance;
        for (VillageRecord record : this.villages.values()) {
            double d = record.bell.distSqr(pos);
            if (d < bestDistance) {
                bestDistance = d;
                best = record;
            }
        }
        return best;
    }

    public Iterable<VillageRecord> villages() {
        return this.villages.values();
    }

    // ------------------------------------------------------------------------------------------ chronicle

    public void record(long day, String key, String... args) {
        this.chronicle.add(new Chronicle.Entry(day, key, List.of(args)));
        while (this.chronicle.size() > 120) {
            this.chronicle.remove(0);
        }
        this.setDirty();
    }

    public List<Chronicle.Entry> chronicle() {
        return this.chronicle;
    }

    // ------------------------------------------------------------------------------------------ saving

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag regionList = new ListTag();
        for (Region region : this.regions.values()) {
            if (region.placed > 0 || region.presence > 0) {
                regionList.add(region.save());
            }
        }
        tag.put("Regions", regionList);
        tag.put("Touched", new LongArrayTag(this.touched.toLongArray()));
        ListTag villageList = new ListTag();
        for (VillageRecord record : this.villages.values()) {
            villageList.add(record.save());
        }
        tag.put("Villages", villageList);
        ListTag chronicleList = new ListTag();
        for (Chronicle.Entry entry : this.chronicle) {
            chronicleList.add(entry.save());
        }
        tag.put("Chronicle", chronicleList);
        tag.putLong("LastDay", this.lastDay);
        tag.putLong("LastDusk", this.lastDusk);
        tag.putLong("LastNight", this.lastNight);
        return tag;
    }

    public static WorldMemory load(CompoundTag tag) {
        WorldMemory memory = new WorldMemory();
        for (Tag t : tag.getList("Regions", Tag.TAG_COMPOUND)) {
            Region region = Region.load((CompoundTag) t);
            memory.regions.put(ChunkPos.asLong(region.rx, region.rz), region);
        }
        for (long chunk : tag.getLongArray("Touched")) {
            memory.touched.add(chunk);
        }
        for (Tag t : tag.getList("Villages", Tag.TAG_COMPOUND)) {
            VillageRecord record = VillageRecord.load((CompoundTag) t);
            memory.villages.put(record.bell.asLong(), record);
        }
        for (Tag t : tag.getList("Chronicle", Tag.TAG_COMPOUND)) {
            memory.chronicle.add(Chronicle.Entry.load((CompoundTag) t));
        }
        memory.lastDay = tag.getLong("LastDay");
        memory.lastDusk = tag.getLong("LastDusk");
        memory.lastNight = tag.getLong("LastNight");
        return memory;
    }

    // ------------------------------------------------------------------------------------------ records

    /** What the world knows about one 128-block region. */
    public static class Region {
        public final int rx;
        public final int rz;
        public long placed;
        public long presence;
        long sumX;
        long sumY;
        long sumZ;
        public int stage;
        // the trail that grows out of the region
        public boolean trailStarted;
        public double trailAngle;
        public int trailStartX;
        public int trailStartZ;
        public int trailX;
        public int trailZ;
        public int trailLength;
        public boolean trailDone;
        @Nullable
        public BlockPos trailTarget;
        public final List<Long> lampSpots = new ArrayList<>();
        public boolean farmBuilt;
        @Nullable
        public BlockPos hamlet;
        public long lastTravelerDay = -10L;
        public long lastReactionDay = -1L;

        Region(int rx, int rz) {
            this.rx = rx;
            this.rz = rz;
        }

        public void addBlock(BlockPos pos) {
            this.placed++;
            this.sumX += pos.getX();
            this.sumY += pos.getY();
            this.sumZ += pos.getZ();
        }

        /** The heart of the player's building work in this region. */
        public BlockPos center() {
            if (this.placed <= 0) {
                return new BlockPos((this.rx << REGION_SHIFT) + 64, 64, (this.rz << REGION_SHIFT) + 64);
            }
            return new BlockPos((int) (this.sumX / this.placed), (int) (this.sumY / this.placed), (int) (this.sumZ / this.placed));
        }

        /**
         * How far the world has come to accept the player here: 1 nature returns, 2 a trail, 3 lamps and a farm,
         * 4 a hamlet. It needs both building and time spent in the region.
         */
        public int computeStage() {
            long days = this.presence / 24000L;
            if (this.placed >= 2500 && days >= 8) {
                return 4;
            }
            if (this.placed >= 1000 && days >= 5) {
                return 3;
            }
            if (this.placed >= 400 && days >= 3) {
                return 2;
            }
            if (this.placed >= 150 && days >= 1) {
                return 1;
            }
            return 0;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("RX", this.rx);
            tag.putInt("RZ", this.rz);
            tag.putLong("Placed", this.placed);
            tag.putLong("Presence", this.presence);
            tag.putLong("SumX", this.sumX);
            tag.putLong("SumY", this.sumY);
            tag.putLong("SumZ", this.sumZ);
            tag.putInt("Stage", this.stage);
            tag.putBoolean("TrailStarted", this.trailStarted);
            tag.putDouble("TrailAngle", this.trailAngle);
            tag.putInt("TrailStartX", this.trailStartX);
            tag.putInt("TrailStartZ", this.trailStartZ);
            tag.putInt("TrailX", this.trailX);
            tag.putInt("TrailZ", this.trailZ);
            tag.putInt("TrailLength", this.trailLength);
            tag.putBoolean("TrailDone", this.trailDone);
            if (this.trailTarget != null) {
                tag.put("TrailTarget", NbtUtils.writeBlockPos(this.trailTarget));
            }
            tag.put("Lamps", new LongArrayTag(this.lampSpots));
            tag.putBoolean("Farm", this.farmBuilt);
            if (this.hamlet != null) {
                tag.put("Hamlet", NbtUtils.writeBlockPos(this.hamlet));
            }
            tag.putLong("LastTraveler", this.lastTravelerDay);
            tag.putLong("LastReaction", this.lastReactionDay);
            return tag;
        }

        static Region load(CompoundTag tag) {
            Region region = new Region(tag.getInt("RX"), tag.getInt("RZ"));
            region.placed = tag.getLong("Placed");
            region.presence = tag.getLong("Presence");
            region.sumX = tag.getLong("SumX");
            region.sumY = tag.getLong("SumY");
            region.sumZ = tag.getLong("SumZ");
            region.stage = tag.getInt("Stage");
            region.trailStarted = tag.getBoolean("TrailStarted");
            region.trailAngle = tag.getDouble("TrailAngle");
            region.trailStartX = tag.getInt("TrailStartX");
            region.trailStartZ = tag.getInt("TrailStartZ");
            region.trailX = tag.getInt("TrailX");
            region.trailZ = tag.getInt("TrailZ");
            region.trailLength = tag.getInt("TrailLength");
            region.trailDone = tag.getBoolean("TrailDone");
            region.trailTarget = tag.contains("TrailTarget") ? NbtUtils.readBlockPos(tag.getCompound("TrailTarget")) : null;
            for (long spot : tag.getLongArray("Lamps")) {
                region.lampSpots.add(spot);
            }
            region.farmBuilt = tag.getBoolean("Farm");
            region.hamlet = tag.contains("Hamlet") ? NbtUtils.readBlockPos(tag.getCompound("Hamlet")) : null;
            region.lastTravelerDay = tag.getLong("LastTraveler");
            region.lastReactionDay = tag.getLong("LastReaction");
            return region;
        }
    }

    /** A village the world has noticed, known by its bell. */
    public static class VillageRecord {
        public final BlockPos bell;
        public final int name;
        public final long founded;
        public long lastGrowthDay = -1L;
        public int projects;
        public int houses;
        public int prosperity;
        public boolean well;
        public long lastPirateRaid = -100L;
        /** 0 unknown, 1 coastal, 2 inland. */
        public int coast;
        @Nullable
        public BlockPos sea;

        VillageRecord(BlockPos bell, int name, long founded) {
            this.bell = bell;
            this.name = name;
            this.founded = founded;
        }

        public String nameKey() {
            return Names.villageKey(this.name);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.put("Bell", NbtUtils.writeBlockPos(this.bell));
            tag.putInt("Name", this.name);
            tag.putLong("Founded", this.founded);
            tag.putLong("LastGrowth", this.lastGrowthDay);
            tag.putInt("Projects", this.projects);
            tag.putInt("Houses", this.houses);
            tag.putInt("Prosperity", this.prosperity);
            tag.putBoolean("Well", this.well);
            tag.putLong("LastPirates", this.lastPirateRaid);
            tag.putInt("Coast", this.coast);
            if (this.sea != null) {
                tag.put("Sea", NbtUtils.writeBlockPos(this.sea));
            }
            return tag;
        }

        static VillageRecord load(CompoundTag tag) {
            VillageRecord record = new VillageRecord(NbtUtils.readBlockPos(tag.getCompound("Bell")), tag.getInt("Name"),
                    tag.getLong("Founded"));
            record.lastGrowthDay = tag.getLong("LastGrowth");
            record.projects = tag.getInt("Projects");
            record.houses = tag.getInt("Houses");
            record.prosperity = tag.getInt("Prosperity");
            record.well = tag.getBoolean("Well");
            record.lastPirateRaid = tag.getLong("LastPirates");
            record.coast = tag.getInt("Coast");
            record.sea = tag.contains("Sea") ? NbtUtils.readBlockPos(tag.getCompound("Sea")) : null;
            return record;
        }
    }
}
