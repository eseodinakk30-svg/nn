package com.dunesrelics.memory;

import com.dunesrelics.entity.world.Pirate;
import com.dunesrelics.entity.world.PirateGunner;
import com.dunesrelics.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Pirates come ashore at night near coastal villages: a horn sounds over the water, and a landing party marches from
 * the beach on the village bell. Fight them off and the villagers will not forget it.
 */
public final class PirateRaids {
    private PirateRaids() {}

    public static void checkNight(ServerLevel level, WorldMemory memory, long day, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || day < 3) {
            return;
        }
        for (WorldMemory.VillageRecord record : memory.villages()) {
            if (!level.isLoaded(record.bell) || !WorldReactions.isAttended(level, record.bell) || day - record.lastPirateRaid < 5) {
                continue;
            }
            if (record.coast == 0) {
                record.sea = findSea(level, record.bell, random);
                record.coast = record.sea != null ? 1 : 2;
                memory.setDirty();
            }
            if (record.coast == 1 && random.nextFloat() < 0.08F && land(level, memory, record, day, random)) {
                return;
            }
        }
    }

    /** Open sea within 48 blocks of the bell, or null. */
    @Nullable
    public static BlockPos findSea(ServerLevel level, BlockPos bell, RandomSource random) {
        for (int i = 0; i < 48; i++) {
            double angle = i / 48.0D * Math.PI * 2.0D;
            for (int distance = 16; distance <= 48; distance += 8) {
                int x = bell.getX() + (int) (Math.cos(angle) * distance);
                int z = bell.getZ() + (int) (Math.sin(angle) * distance);
                BlockPos column = new BlockPos(x, 0, z);
                if (!level.isLoaded(column)) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                BlockPos top = new BlockPos(x, y, z);
                if (level.getFluidState(top).isSource() && level.getFluidState(top).is(net.minecraft.tags.FluidTags.WATER)
                        && level.getBiome(top).is(BiomeTags.IS_OCEAN)) {
                    return top;
                }
            }
        }
        return null;
    }

    /** Lands a pirate crew on the shore between the sea and the village. */
    public static boolean land(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record, long day, RandomSource random) {
        if (record.sea == null) {
            return false;
        }
        // Walk from the sea towards the village until the first dry ground.
        Vec3 from = Vec3.atCenterOf(record.sea);
        Vec3 dir = Vec3.atCenterOf(record.bell).subtract(from).multiply(1.0D, 0.0D, 1.0D).normalize();
        BlockPos landing = null;
        BlockPos water = record.sea;
        for (int step = 0; step < 64; step++) {
            int x = (int) Math.floor(from.x + dir.x * step);
            int z = (int) Math.floor(from.z + dir.z * step);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            BlockPos top = new BlockPos(x, y, z);
            if (level.getFluidState(top).isEmpty()) {
                landing = top.above();
                break;
            }
            water = top;
        }
        if (landing == null) {
            return false;
        }
        int crew = 3 + random.nextInt(3);
        for (int i = 0; i < crew + 1 + random.nextInt(2); i++) {
            BlockPos at = landing.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            at = new BlockPos(at.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ()), at.getZ());
            if (i < crew) {
                Pirate pirate = ModEntities.PIRATE.get().create(level);
                if (pirate != null) {
                    pirate.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                    pirate.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
                    pirate.setRaidTarget(record.bell);
                    level.addFreshEntity(pirate);
                }
            } else {
                PirateGunner gunner = ModEntities.PIRATE_GUNNER.get().create(level);
                if (gunner != null) {
                    gunner.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                    gunner.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
                    gunner.setRaidTarget(record.bell);
                    level.addFreshEntity(gunner);
                }
            }
        }
        // The longboats they rowed in on.
        for (int i = 0; i < 2; i++) {
            Boat boat = new Boat(level, water.getX() + 0.5D + i * 2, water.getY() + 1.0D, water.getZ() + 0.5D);
            boat.setVariant(Boat.Type.DARK_OAK);
            level.addFreshEntity(boat);
        }
        level.playSound(null, landing, SoundEvents.RAID_HORN.value(), SoundSource.NEUTRAL, 64.0F, 0.8F);
        record.lastPirateRaid = day;
        memory.record(day, "chronicle.dunesrelics.pirates", "#" + record.nameKey());
        memory.setDirty();
        return true;
    }
}
