package com.dunesrelics.memory;

import com.dunesrelics.entity.world.VillageWorker;
import com.dunesrelics.registry.ModEntities;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The village builders' work: putting up the buildings a village plans, block by block, and putting back what was
 * destroyed. A village keeps a record of its own buildings (its snapshot: the houses of the village structure, plus
 * everything the builders have built); anything of it that has turned to air or fire is queued for repair. Blocks a
 * player has put in its place instead are left alone, and so is anything outside the village.
 */
public final class Construction {
    /** How far from its bell a village's buildings may stand. */
    public static final int VILLAGE_RADIUS = 64;
    private static final int MAX_SNAPSHOT = 40000;

    private Construction() {}

    /** One block for a builder to lay: from a building site, or a repair. */
    public record Task(BlockPos pos, BlockState state, boolean repair, int index) {}

    // ------------------------------------------------------------------------------------------ what counts as built

    /** Blocks the surveyors leave out of a village's record: the ground itself, plants, leaves, fluids and fire. */
    public static boolean isBuilt(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        return !(block instanceof BushBlock) && !(block instanceof BaseFireBlock) && !(block instanceof LiquidBlock)
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.DIRT) && !state.is(BlockTags.SAND)
                && !state.is(BlockTags.BASE_STONE_OVERWORLD) && !state.is(Blocks.GRAVEL) && !state.is(Blocks.SNOW)
                && !state.is(Blocks.SNOW_BLOCK) && !state.is(Blocks.POWDER_SNOW) && !state.is(Blocks.FARMLAND)
                && !state.is(Blocks.ICE) && !state.is(Blocks.CLAY) && !state.is(BlockTags.REPLACEABLE);
    }

    /** What a builder may clear or build over: air, plants, fluids, fire and the natural ground. */
    public static boolean canReplace(BlockState now) {
        Block block = now.getBlock();
        return now.isAir() || now.canBeReplaced() || block instanceof BushBlock || block instanceof BaseFireBlock
                || block instanceof LiquidBlock || now.is(BlockTags.LEAVES) || now.is(BlockTags.DIRT) || now.is(BlockTags.SAND)
                || now.is(Blocks.GRAVEL) || now.is(Blocks.SNOW) || now.is(Blocks.SNOW_BLOCK) || now.is(Blocks.DIRT_PATH)
                || now.is(Blocks.FARMLAND) || now.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    private static boolean isGone(BlockState now) {
        return now.isAir() || now.getBlock() instanceof BaseFireBlock;
    }

    /** Whether a block still has to be laid here (the block itself, never mind which way a fence connects). */
    private static boolean needsWork(BlockState now, BlockState want) {
        return want.isAir() ? !now.isAir() && canReplace(now) : now.getBlock() != want.getBlock();
    }

    // ------------------------------------------------------------------------------------------ surveying

    /** Whether the village and its surroundings are loaded, so it can be surveyed in one go. */
    private static boolean areaLoaded(ServerLevel level, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx += 16) {
            for (int dz = -radius; dz <= radius; dz += 16) {
                if (!level.isLoaded(center.offset(dx, 0, dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Records the houses of the village structure around the bell (villages the world grew itself start empty). */
    public static void survey(ServerLevel level, WorldMemory.VillageRecord record) {
        Blueprint snapshot = record.snapshot != null ? record.snapshot : new Blueprint("snapshot");
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<StructureStart> starts = level.structureManager().startsForStructure(new ChunkPos(record.bell),
                structure -> structures.wrapAsHolder(structure).is(StructureTags.VILLAGE));
        outer:
        for (StructureStart start : starts) {
            if (!start.getBoundingBox().inflatedBy(8).isInside(record.bell)) {
                continue;
            }
            for (StructurePiece piece : start.getPieces()) {
                BoundingBox box = piece.getBoundingBox();
                for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (isBuilt(state) && snapshot.indexOf(pos) < 0) {
                        snapshot.add(pos.immutable(), state);
                        if (snapshot.size() >= MAX_SNAPSHOT) {
                            break outer;
                        }
                    }
                }
            }
        }
        record.snapshot = snapshot;
        record.surveyed = true;
    }

    /** Compares a slice of the village's record with the world and queues whatever has been destroyed. */
    public static void inspect(ServerLevel level, WorldMemory.VillageRecord record, int budget) {
        Blueprint snapshot = record.snapshot;
        if (snapshot == null || snapshot.size() == 0) {
            return;
        }
        for (int n = 0; n < Math.min(budget, snapshot.size()); n++) {
            int i = record.inspectCursor = (record.inspectCursor + 1) % snapshot.size();
            BlockState want = snapshot.state(i);
            if (want.isAir() || record.repairs.contains(i)) {
                continue;
            }
            BlockPos pos = snapshot.pos(i);
            if (level.isLoaded(pos) && isGone(level.getBlockState(pos))) {
                record.repairs.add(i);
            }
        }
    }

    /** The owner of a town rebuilding it: the village forgets the block instead of putting it back. */
    public static void forget(WorldMemory.VillageRecord record, BlockPos pos) {
        if (record.snapshot != null) {
            int i = record.snapshot.indexOf(pos);
            if (i >= 0) {
                record.snapshot.set(i, Blocks.AIR.defaultBlockState());
                record.repairs.remove(i);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ the builders' work

    public static boolean hasWork(WorldMemory.VillageRecord record) {
        return !record.repairs.isEmpty() || !record.sites.isEmpty();
    }

    /** The next block to lay: repairs come first, then the building under way. */
    @Nullable
    public static Task nextTask(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record) {
        Blueprint snapshot = record.snapshot;
        if (snapshot != null) {
            IntIterator it = record.repairs.iterator();
            int looked = 0;
            while (it.hasNext() && looked++ < 32) {
                int i = it.nextInt();
                if (i >= snapshot.size()) {
                    it.remove();
                    continue;
                }
                BlockPos pos = snapshot.pos(i);
                BlockState want = snapshot.state(i);
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState now = level.getBlockState(pos);
                if (want.isAir() || !isGone(now)) {
                    // put back already, or a player has put something else there
                    it.remove();
                    noteRepairs(level, memory, record);
                    continue;
                }
                if (!Materials.canAfford(record, want)) {
                    record.waitingFor = Materials.kind(want);
                    continue;
                }
                record.waitingFor = null;
                return new Task(pos, want, true, i);
            }
        }
        while (!record.sites.isEmpty()) {
            Blueprint site = record.sites.get(0);
            int skipped = 0;
            while (!site.isDone() && skipped < 64) {
                BlockPos pos = site.pos(site.next);
                if (!level.isLoaded(pos)) {
                    return null;
                }
                BlockState now = level.getBlockState(pos);
                BlockState want = site.state(site.next);
                if (needsWork(now, want) && (want.isAir() || canReplace(now))) {
                    if (!Materials.canAfford(record, want)) {
                        // the lumberjacks and quarrymen have to catch up first
                        record.waitingFor = Materials.kind(want);
                        return null;
                    }
                    record.waitingFor = null;
                    return new Task(pos, want, false, site.next);
                }
                // done already, or someone has built something of their own there: leave it
                site.next++;
                skipped++;
            }
            if (!site.isDone()) {
                return null;
            }
            complete(level, memory, record, site);
        }
        return null;
    }

    /**
     * Lays one block. Returns false if it has to wait (someone is standing where the block goes).
     */
    public static boolean place(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record, Task task) {
        BlockState state = task.state();
        if (!state.isAir() && state.blocksMotion()
                && !level.getEntitiesOfClass(LivingEntity.class, new AABB(task.pos())).isEmpty()) {
            return false;
        }
        lay(level, task.pos(), state);
        Materials.spend(record, state);
        if (task.repair()) {
            record.repairs.remove(task.index());
            record.repairedSinceNote++;
            noteRepairs(level, memory, record);
        } else if (!record.sites.isEmpty()) {
            Blueprint site = record.sites.get(0);
            if (site.next == task.index()) {
                site.next++;
            }
            site.lastProgress = level.getGameTime();
            if (site.isDone()) {
                complete(level, memory, record, site);
            }
        }
        memory.setDirty();
        return true;
    }

    /** Puts a block in place with the sound and dust of work; doors and beds go in whole. */
    public static void lay(ServerLevel level, BlockPos pos, BlockState state) {
        lay(level, pos, state, true);
    }

    private static void lay(ServerLevel level, BlockPos pos, BlockState state, boolean effects) {
        if (state.isAir()) {
            if (effects) {
                level.destroyBlock(pos, false);
            } else {
                level.setBlock(pos, state, Block.UPDATE_ALL);
            }
            return;
        }
        int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            boolean lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
            BlockPos other = lower ? pos.above() : pos.below();
            level.setBlock(pos, state, quiet);
            if (canReplace(level.getBlockState(other))) {
                level.setBlock(other, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                        lower ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER), quiet);
            }
        } else if (state.hasProperty(BlockStateProperties.BED_PART)) {
            Direction toOther = BedBlock.getConnectedDirection(state);
            BlockPos other = pos.relative(toOther);
            level.setBlock(pos, state, quiet);
            if (canReplace(level.getBlockState(other))) {
                level.setBlock(other, state.setValue(BlockStateProperties.BED_PART,
                        state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT ? BedPart.HEAD : BedPart.FOOT), quiet);
            }
        } else {
            BlockState shaped = Block.updateFromNeighbourShapes(state, level, pos);
            level.setBlock(pos, shaped.isAir() ? state : shaped, Block.UPDATE_ALL);
        }
        if (!effects) {
            return;
        }
        SoundType sound = state.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (state.getFluidState().isEmpty()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, 10, 0.3D, 0.3D, 0.3D, 0.05D);
        }
    }

    /** Builds a whole blueprint at once (for what the world puts up where nobody is watching). */
    public static void buildNow(ServerLevel level, Blueprint blueprint) {
        for (int i = 0; i < blueprint.size(); i++) {
            BlockPos pos = blueprint.pos(i);
            BlockState want = blueprint.state(i);
            BlockState now = level.getBlockState(pos);
            if (needsWork(now, want) && (want.isAir() || canReplace(now))) {
                lay(level, pos, want, false);
            }
        }
        blueprint.next = blueprint.size();
    }

    /** A village takes a building into its record, so its builders look after it from now on. */
    public static void adopt(WorldMemory.VillageRecord record, Blueprint built) {
        if (record.snapshot == null) {
            record.snapshot = new Blueprint("snapshot");
        }
        for (int i = 0; i < built.size(); i++) {
            BlockState state = built.state(i);
            if (!isBuilt(state)) {
                continue;
            }
            BlockPos pos = built.pos(i);
            int j = record.snapshot.indexOf(pos);
            if (j >= 0) {
                record.snapshot.set(j, state);
            } else if (record.snapshot.size() < MAX_SNAPSHOT) {
                record.snapshot.add(pos, state);
            }
        }
    }

    /** A building is finished: the village adds it to its record (so it is looked after too) and remembers it. */
    public static void complete(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record, Blueprint site) {
        record.sites.remove(site);
        adopt(record, site);
        memory.record(level.getDayTime() / 24000L, "chronicle.dunesrelics.village." + site.project, "#" + record.nameKey());
        memory.setDirty();
    }

    private static void noteRepairs(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record) {
        if (record.repairs.isEmpty() && record.repairedSinceNote >= 12) {
            memory.record(level.getDayTime() / 24000L, "chronicle.dunesrelics.village.repaired", "#" + record.nameKey());
            record.repairedSinceNote = 0;
        }
    }

    // ------------------------------------------------------------------------------------------ the village's day

    /**
     * Called every few seconds for each village near a player: survey it once, look for damage, and make sure it has
     * builders. A village that has lost its builders gets a new one after half a day.
     */
    public static void tickVillage(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record) {
        if (!level.isLoaded(record.bell) || !(level.getBlockState(record.bell).getBlock() instanceof net.minecraft.world.level.block.BellBlock)) {
            return;
        }
        if (!record.surveyed && areaLoaded(level, record.bell, 48)) {
            survey(level, record);
            memory.setDirty();
        }
        int before = record.repairs.size();
        inspect(level, record, 500);
        if (record.repairs.size() != before) {
            memory.setDirty();
        }
        if (!record.sites.isEmpty()) {
            Blueprint site = record.sites.get(0);
            if (level.getGameTime() - site.lastProgress > 72000L) {
                // no progress for three days: the builders give up on it
                record.sites.remove(0);
                memory.setDirty();
            }
        }
        long now = level.getGameTime();
        if (record.lastObserved > 0 && now - record.lastObserved > 24000L) {
            // nobody was here for a while: the village lives through those days at once
            catchUp(level, memory, record, (int) Math.min(30L, (now - record.lastObserved) / 24000L));
        }
        record.lastObserved = now;
        ensureWorkers(level, memory, record);
    }

    /**
     * The days a village spent without anyone near: its workers gathered wood and stone, new villagers moved into
     * empty beds, and its builders put up what it planned. All of it appears at once when a player comes back.
     */
    public static void catchUp(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record, int days) {
        AABB area = new AABB(record.bell).inflate(VILLAGE_RADIUS);
        List<VillageWorker> workers = level.getEntitiesOfClass(VillageWorker.class, area.inflate(32.0D),
                w -> w.isAlive() && record.bell.equals(w.getHomeBell()));
        long lumberjacks = workers.stream().filter(w -> w.getJob() == VillageWorker.Job.LUMBERJACK).count();
        long quarrymen = workers.stream().filter(w -> w.getJob() == VillageWorker.Job.QUARRYMAN).count();
        long today = level.getDayTime() / 24000L;
        for (int d = 0; d < days; d++) {
            long day = today - days + d + 1;
            record.wood += 8 + (int) lumberjacks * 40;
            record.stone += 4 + (int) quarrymen * 40;
            // the builders finish what they had begun, and mend what was broken
            IntIterator it = record.repairs.iterator();
            while (it.hasNext()) {
                int i = it.nextInt();
                if (record.snapshot == null || i >= record.snapshot.size()) {
                    it.remove();
                    continue;
                }
                BlockPos pos = record.snapshot.pos(i);
                BlockState want = record.snapshot.state(i);
                if (!level.isLoaded(pos) || !Materials.canAfford(record, want)) {
                    continue;
                }
                if (!want.isAir() && isGone(level.getBlockState(pos))) {
                    lay(level, pos, want, false);
                    Materials.spend(record, want);
                }
                it.remove();
            }
            if (record.sites.isEmpty()) {
                int villagers = level.getEntitiesOfClass(Villager.class, area, v -> v.isAlive() && !v.isBaby()).size();
                VillageLife.grow(level, memory, record, villagers, level.random);
            }
            if (!record.sites.isEmpty()) {
                Blueprint site = record.sites.get(0);
                int[] cost = Materials.cost(site, site.next);
                if (cost[0] <= record.wood && cost[1] <= record.stone) {
                    record.wood -= cost[0];
                    record.stone -= cost[1];
                    for (int i = site.next; i < site.size(); i++) {
                        BlockPos pos = site.pos(i);
                        BlockState want = site.state(i);
                        BlockState now = level.getBlockState(pos);
                        if (level.isLoaded(pos) && needsWork(now, want) && (want.isAir() || canReplace(now))) {
                            lay(level, pos, want, false);
                        }
                    }
                    site.next = site.size();
                    record.sites.remove(site);
                    adopt(record, site);
                    record.projects++;
                    memory.record(day, "chronicle.dunesrelics.village." + site.project, "#" + record.nameKey());
                }
            }
            // new families move into empty beds
            if (d % 2 == 1) {
                VillageLife.settle(level, record);
            }
        }
        int villagers = level.getEntitiesOfClass(Villager.class, area, v -> v.isAlive() && !v.isBaby()).size();
        VillageLife.expand(level, memory, record, today, villagers);
        memory.setDirty();
    }

    /**
     * Every village near a player gets the workers it needs as it grows: builders (one per twelve villagers),
     * lumberjacks (one per ten) and, from four villagers on, quarrymen (one per sixteen). A village that has lost a
     * worker gets a new one after half a day.
     */
    public static void ensureWorkers(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record) {
        AABB area = new AABB(record.bell).inflate(VILLAGE_RADIUS);
        int villagers = level.getEntitiesOfClass(Villager.class, area, v -> v.isAlive() && !v.isBaby()).size();
        if (villagers == 0) {
            return;
        }
        long now = level.getGameTime();
        if (record.lastBuilderArrival > 0 && now - record.lastBuilderArrival < 12000L) {
            return;
        }
        List<VillageWorker> workers = level.getEntitiesOfClass(VillageWorker.class, area.inflate(32.0D),
                w -> w.isAlive() && record.bell.equals(w.getHomeBell()));
        VillageWorker.Job missing = null;
        for (VillageWorker.Job job : VillageWorker.Job.values()) {
            int wanted = switch (job) {
                case BUILDER -> 1 + villagers / 12;
                case LUMBERJACK -> 1 + villagers / 10;
                case QUARRYMAN -> villagers >= 4 ? 1 + villagers / 16 : 0;
            };
            long have = workers.stream().filter(w -> w.getJob() == job).count();
            if (have < wanted) {
                missing = job;
                break;
            }
        }
        if (missing == null) {
            return;
        }
        BlockPos spot = null;
        for (int attempt = 0; attempt < 12 && spot == null; attempt++) {
            int x = record.bell.getX() + level.random.nextInt(9) - 4;
            int z = record.bell.getZ() + level.random.nextInt(9) - 4;
            BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (Math.abs(ground.getY() - record.bell.getY()) < 6 && level.getBlockState(ground).isAir()
                    && level.getBlockState(ground.above()).isAir()) {
                spot = ground;
            }
        }
        if (spot == null) {
            return;
        }
        VillageWorker worker = switch (missing) {
            case BUILDER -> ModEntities.VILLAGE_BUILDER.get().create(level);
            case LUMBERJACK -> ModEntities.LUMBERJACK.get().create(level);
            case QUARRYMAN -> ModEntities.QUARRYMAN.get().create(level);
        };
        if (worker == null) {
            return;
        }
        worker.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        worker.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null, null);
        worker.setHomeBell(record.bell);
        level.addFreshEntity(worker);
        record.lastBuilderArrival = now;
        memory.record(level.getDayTime() / 24000L, "chronicle.dunesrelics.worker_arrived." + missing.name().toLowerCase(java.util.Locale.ROOT),
                "#" + Names.villagerKey(worker.getNameIndex()), "#" + record.nameKey());
        memory.setDirty();
    }
}
