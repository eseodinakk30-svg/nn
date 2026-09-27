package com.dunesrelics.memory;

import com.dunesrelics.entity.world.Traveler;
import com.dunesrelics.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * How the world answers a player who settles down, one small step each morning. Nothing happens in or next to the
 * chunks the player has built in; everything grows in the surroundings, slowly:
 * <ol>
 *     <li>nature returns: saplings take root and animals wander in,</li>
 *     <li>a trail is worn from the player's home towards the nearest village,</li>
 *     <li>lamp posts appear along it, and a family sets up a farm by the road,</li>
 *     <li>a hamlet is founded nearby, which then grows like any village,</li>
 * </ol>
 * and once there is a trail, travellers start passing through.
 */
public final class WorldReactions {
    private static final int TRAIL_MAX = 240;

    private WorldReactions() {}

    /** Whether a player is close enough for the region's surroundings to be loaded. */
    public static boolean isAttended(ServerLevel level, BlockPos center) {
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) < 160 * 160) {
                return true;
            }
        }
        return false;
    }

    public static void react(ServerLevel level, WorldMemory memory, WorldMemory.Region region, long day, RandomSource random) {
        if (region.lastReactionDay == day) {
            return;
        }
        region.lastReactionDay = day;
        BlockPos center = region.center();
        int stage = region.computeStage();
        if (stage > region.stage) {
            region.stage = stage;
            memory.record(day, "chronicle.dunesrelics.stage." + stage);
        }
        if (region.stage >= 1) {
            regrow(level, memory, center, random);
            if (random.nextFloat() < 0.3F) {
                animalsArrive(level, memory, center, random);
            }
        }
        if (region.stage >= 2) {
            growTrail(level, memory, region, day, random);
        }
        if (region.stage >= 3) {
            placeLamps(level, memory, region, random);
            if (!region.farmBuilt && random.nextFloat() < 0.5F) {
                buildFarm(level, memory, region, day, random);
            }
        }
        if (region.stage >= 4 && region.hamlet == null && random.nextFloat() < 0.5F) {
            foundHamlet(level, memory, region, day, random);
        }
        if (region.stage >= 2 && region.trailLength >= 24 && day - region.lastTravelerDay >= 2 && random.nextFloat() < 0.35F) {
            sendTraveler(level, memory, region, day, random);
        }
        memory.setDirty();
    }

    // ------------------------------------------------------------------------------------------ nature

    private static BlockPos ring(BlockPos center, RandomSource random, int min, int max) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = min + random.nextDouble() * (max - min);
        return new BlockPos(center.getX() + (int) (Math.cos(angle) * distance), 0, center.getZ() + (int) (Math.sin(angle) * distance));
    }

    private static Block saplingFor(Holder<Biome> biome, RandomSource random) {
        if (biome.is(BiomeTags.IS_TAIGA)) {
            return Blocks.SPRUCE_SAPLING;
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return Blocks.JUNGLE_SAPLING;
        }
        if (biome.is(BiomeTags.IS_SAVANNA)) {
            return Blocks.ACACIA_SAPLING;
        }
        if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            return Blocks.BIRCH_SAPLING;
        }
        if (biome.is(Biomes.CHERRY_GROVE)) {
            return Blocks.CHERRY_SAPLING;
        }
        return random.nextInt(5) == 0 ? Blocks.BIRCH_SAPLING : Blocks.OAK_SAPLING;
    }

    /** A few saplings and flowers take root on open grass around the home. */
    public static void regrow(ServerLevel level, WorldMemory memory, BlockPos center, RandomSource random) {
        Block[] flowers = {Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET};
        int planted = 0;
        for (int attempt = 0; attempt < 24 && planted < 5; attempt++) {
            BlockPos column = ring(center, random, 28, 72);
            if (!level.isLoaded(column) || memory.isProtected(column)) {
                continue;
            }
            int y = Builders.groundY(level, column.getX(), column.getZ());
            BlockPos ground = new BlockPos(column.getX(), y, column.getZ());
            if (!level.getBlockState(ground).is(Blocks.GRASS_BLOCK) || !level.getBlockState(ground.above()).isAir()) {
                continue;
            }
            Block plant = random.nextFloat() < 0.6F ? saplingFor(level.getBiome(ground), random) : flowers[random.nextInt(flowers.length)];
            level.setBlock(ground.above(), plant.defaultBlockState(), Block.UPDATE_ALL);
            planted++;
        }
    }

    /** A small herd of the animals that belong to the biome wanders in. */
    public static void animalsArrive(ServerLevel level, WorldMemory memory, BlockPos center, RandomSource random) {
        BlockPos column = ring(center, random, 32, 64);
        if (!level.isLoaded(column) || memory.isProtected(column)) {
            return;
        }
        int y = Builders.groundY(level, column.getX(), column.getZ());
        BlockPos pos = new BlockPos(column.getX(), y + 1, column.getZ());
        WeightedRandomList<MobSpawnSettings.SpawnerData> spawns = level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.CREATURE);
        spawns.getRandom(random).ifPresent(data -> {
            int count = 2 + random.nextInt(2);
            for (int i = 0; i < count; i++) {
                Entity entity = data.type.create(level);
                if (entity instanceof Mob mob) {
                    mob.moveTo(pos.getX() + 0.5D + random.nextInt(3) - 1, pos.getY(), pos.getZ() + 0.5D + random.nextInt(3) - 1,
                            random.nextFloat() * 360.0F, 0.0F);
                    if (mob.checkSpawnObstruction(level)) {
                        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
                        level.addFreshEntity(mob);
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------------------------------ the trail

    public static void growTrail(ServerLevel level, WorldMemory memory, WorldMemory.Region region, long day, RandomSource random) {
        if (region.trailDone) {
            return;
        }
        BlockPos center = region.center();
        if (!region.trailStarted) {
            WorldMemory.VillageRecord village = memory.nearestVillage(center, 400.0D);
            if (village != null && village.bell.distSqr(center) > 48 * 48) {
                region.trailTarget = village.bell;
                region.trailAngle = Math.atan2(village.bell.getZ() - center.getZ(), village.bell.getX() - center.getX());
            } else {
                region.trailAngle = random.nextDouble() * Math.PI * 2.0D;
            }
            // Start just outside the player's own chunks.
            int start = 24;
            while (start < 112) {
                BlockPos p = new BlockPos(center.getX() + (int) (Math.cos(region.trailAngle) * start), 0,
                        center.getZ() + (int) (Math.sin(region.trailAngle) * start));
                if (!memory.isProtected(p)) {
                    break;
                }
                start += 4;
            }
            region.trailStartX = center.getX() + (int) (Math.cos(region.trailAngle) * start);
            region.trailStartZ = center.getZ() + (int) (Math.sin(region.trailAngle) * start);
            region.trailX = region.trailStartX;
            region.trailZ = region.trailStartZ;
            region.trailStarted = true;
        }
        int steps = 24 + random.nextInt(17);
        double x = region.trailX;
        double z = region.trailZ;
        int bridge = 0;
        for (int i = 0; i < steps && region.trailLength < TRAIL_MAX; i++) {
            // Wander a little, but keep heading for the target.
            double wanted = region.trailTarget != null
                    ? Math.atan2(region.trailTarget.getZ() - z, region.trailTarget.getX() - x) : region.trailAngle;
            region.trailAngle += Mth.clamp(Mth.wrapDegrees((float) Math.toDegrees(wanted - region.trailAngle)) * 0.02F, -3.0F, 3.0F)
                    * Mth.DEG_TO_RAD + (random.nextDouble() - 0.5D) * 0.12D;
            x += Math.cos(region.trailAngle);
            z += Math.sin(region.trailAngle);
            BlockPos column = new BlockPos((int) Math.floor(x), 0, (int) Math.floor(z));
            if (!level.isLoaded(column)) {
                break;
            }
            region.trailLength++;
            if (memory.isProtected(column)) {
                continue;
            }
            BlockPos surface = new BlockPos(column.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ()) - 1, column.getZ());
            BlockState top = level.getBlockState(surface);
            if (!top.getFluidState().isEmpty()) {
                // Lay planks across small streams; give up at lakes.
                if (++bridge > 10) {
                    region.trailDone = true;
                    break;
                }
                level.setBlock(surface, Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
                continue;
            }
            bridge = 0;
            Builders.pathAt(level, column.getX(), column.getZ());
            if (random.nextFloat() < 0.5F) {
                int side = random.nextBoolean() ? 1 : -1;
                Builders.pathAt(level, column.getX() + (int) Math.round(-Math.sin(region.trailAngle) * side),
                        column.getZ() + (int) Math.round(Math.cos(region.trailAngle) * side));
            }
            if (region.trailLength % 22 == 0) {
                int lx = column.getX() + (int) Math.round(-Math.sin(region.trailAngle) * 2);
                int lz = column.getZ() + (int) Math.round(Math.cos(region.trailAngle) * 2);
                region.lampSpots.add(BlockPos.asLong(lx, 0, lz));
            }
            if (region.trailTarget != null && column.distSqr(new BlockPos(region.trailTarget.getX(), 0, region.trailTarget.getZ())) < 20 * 20) {
                region.trailDone = true;
                WorldMemory.VillageRecord village = memory.nearestVillage(region.trailTarget, 8.0D);
                memory.record(day, "chronicle.dunesrelics.trail_joined", village != null ? "#" + village.nameKey() : "?");
                break;
            }
        }
        region.trailX = (int) Math.floor(x);
        region.trailZ = (int) Math.floor(z);
        if (region.trailLength >= TRAIL_MAX) {
            region.trailDone = true;
        }
    }

    private static void placeLamps(ServerLevel level, WorldMemory memory, WorldMemory.Region region, RandomSource random) {
        int placed = 0;
        for (int i = 0; i < region.lampSpots.size() && placed < 3; i++) {
            long spot = region.lampSpots.get(i);
            if (spot == Long.MIN_VALUE) {
                continue;
            }
            int x = BlockPos.getX(spot);
            int z = BlockPos.getZ(spot);
            if (!level.isLoaded(new BlockPos(x, 0, z))) {
                continue;
            }
            Builders.lampPost(level, memory, x, z, Builders.palette(level.getBiome(new BlockPos(x, 64, z))));
            region.lampSpots.set(i, Long.MIN_VALUE);
            placed++;
        }
    }

    // ------------------------------------------------------------------------------------------ farm and hamlet

    private static BlockPos alongTrail(WorldMemory.Region region, double fraction, int sideOffset) {
        double x = region.trailStartX + (region.trailX - region.trailStartX) * fraction;
        double z = region.trailStartZ + (region.trailZ - region.trailStartZ) * fraction;
        double angle = Math.atan2(region.trailZ - region.trailStartZ, region.trailX - region.trailStartX);
        return new BlockPos((int) (x - Math.sin(angle) * sideOffset), 0, (int) (z + Math.cos(angle) * sideOffset));
    }

    private static void buildFarm(ServerLevel level, WorldMemory memory, WorldMemory.Region region, long day, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int side = random.nextBoolean() ? 1 : -1;
            BlockPos spot = alongTrail(region, 0.3D + random.nextDouble() * 0.6D, side * (6 + random.nextInt(5)));
            int y = Builders.site(level, memory, spot.getX(), spot.getZ(), 9, 9, 1, 4);
            if (y == Builders.FAIL) {
                continue;
            }
            Direction gate = Direction.fromYRot(Math.toDegrees(Math.atan2(region.trailZ - region.trailStartZ,
                    region.trailX - region.trailStartX)) - 90.0D + (side > 0 ? 180.0D : 0.0D));
            Builders.farm(level, spot, y, Builders.palette(level.getBiome(spot.atY(y))), gate, random);
            region.farmBuilt = true;
            memory.record(day, "chronicle.dunesrelics.farm");
            return;
        }
    }

    public static boolean foundHamlet(ServerLevel level, WorldMemory memory, WorldMemory.Region region, long day, RandomSource random) {
        BlockPos center = region.center();
        for (int attempt = 0; attempt < 20; attempt++) {
            BlockPos spot = region.trailLength > 60 && random.nextBoolean()
                    ? alongTrail(region, 0.7D + random.nextDouble() * 0.3D, (random.nextBoolean() ? 1 : -1) * (7 + random.nextInt(6)))
                    : ring(center, random, 64, 112);
            BlockPos built = buildHamlet(level, memory, spot, random);
            if (built != null) {
                region.hamlet = built;
                WorldMemory.VillageRecord record = memory.village(built, day, Names.randomVillage(random));
                memory.record(day, "chronicle.dunesrelics.hamlet", "#" + record.nameKey());
                return true;
            }
        }
        return false;
    }

    /** A cottage, a bell and a field, with a young couple living there. Returns the bell's position. */
    public static BlockPos buildHamlet(ServerLevel level, WorldMemory memory, BlockPos spot, RandomSource random) {
        int y = Builders.site(level, memory, spot.getX(), spot.getZ(), 13, 9, 2, 7);
        if (y == Builders.FAIL) {
            return null;
        }
        BlockPos min = new BlockPos(spot.getX(), y, spot.getZ());
        Builders.Palette palette = Builders.palette(level.getBiome(min));
        BlockPos door = Builders.house(level, min, y, Direction.SOUTH, palette, Builders.WORKSTATIONS[random.nextInt(4)]);
        Builders.field(level, min.offset(8, 0, 1), y, 5, random);
        BlockPos bellGround = new BlockPos(min.getX() + 3, y, min.getZ() + 7);
        Builders.bell(level, bellGround);
        BlockPos bell = bellGround.above(2);
        VillagerType type = VillagerType.byBiome(level.getBiome(min));
        for (int i = 0; i < 2; i++) {
            Villager villager = EntityType.VILLAGER.create(level);
            if (villager == null) {
                continue;
            }
            villager.setVillagerData(villager.getVillagerData().setType(type));
            villager.moveTo(door.getX() + 0.5D + i, door.getY() + 1, door.getZ() + 1.5D, random.nextFloat() * 360.0F, 0.0F);
            villager.finalizeSpawn(level, level.getCurrentDifficultyAt(door), MobSpawnType.STRUCTURE, null, null);
            level.addFreshEntity(villager);
        }
        return bell;
    }

    // ------------------------------------------------------------------------------------------ travellers

    private static void sendTraveler(ServerLevel level, WorldMemory memory, WorldMemory.Region region, long day, RandomSource random) {
        int x = region.trailX;
        int z = region.trailZ;
        BlockPos column = new BlockPos(x, 0, z);
        if (!level.isLoaded(column)) {
            return;
        }
        Traveler traveler = ModEntities.TRAVELER.get().create(level);
        if (traveler == null) {
            return;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        traveler.moveTo(x + 0.5D, y, z + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        traveler.finalizeSpawn(level, level.getCurrentDifficultyAt(column), MobSpawnType.EVENT, null, null);
        traveler.setDespawnDelay(24000);
        traveler.setWanderTarget(new BlockPos(region.trailStartX, y, region.trailStartZ));
        int name = Names.randomVillager(random);
        traveler.getPersistentData().putInt("dr_name", name);
        level.addFreshEntity(traveler);
        region.lastTravelerDay = day;
        memory.record(day, "chronicle.dunesrelics.traveler", "#" + Names.villagerKey(name));
    }
}
