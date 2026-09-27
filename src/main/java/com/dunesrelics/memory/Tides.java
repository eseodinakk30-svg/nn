package com.dunesrelics.memory;

import com.dunesrelics.block.world.SeashellBlock;
import com.dunesrelics.block.world.ShoreBlock;
import com.dunesrelics.registry.ModTags;
import com.dunesrelics.registry.WorldBlocks;
import com.dunesrelics.registry.WorldItems;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tides and currents. The sea rises and falls twice a day (high tide at sunrise and sunset, low tide at noon and
 * midnight; spring tides at full and new moon). When it ebbs, the beaches near players turn to wet sand strewn with
 * shells, clams and the odd message in a bottle, which the flood takes back. Currents drift through the seas and
 * rivers, carrying boats, items and swimmers along; they run strongest at the turn of the tide.
 */
public final class Tides {
    private static final Map<UUID, int[]> BUDGET = new HashMap<>();

    private Tides() {}

    /** The tide from -1 (lowest ebb) to 1 (highest flood). */
    public static double tide(Level level) {
        long time = level.getDayTime();
        double phase = (time % 12000L) / 12000.0D * Math.PI * 2.0D;
        double spring = 0.75D + 0.25D * Math.abs(Math.cos(level.getMoonPhase() * Math.PI / 4.0D));
        return Math.cos(phase) * spring;
    }

    public static boolean isHighTide(Level level) {
        return tide(level) > 0.45D;
    }

    public static boolean isLowTide(Level level) {
        return tide(level) < -0.45D;
    }

    /** The flood is coming up the beach. */
    public static boolean isFlood(Level level) {
        return tide(level) > 0.3D;
    }

    /** The ebb has laid the shallows bare. */
    public static boolean isEbb(Level level) {
        return tide(level) < -0.3D;
    }

    /** How far up a low beach the flood reaches now: 0 to 5 blocks. */
    public static int floodReach(Level level) {
        double t = tide(level);
        return t <= 0.3D ? 0 : 1 + (int) ((t - 0.3D) / 0.7D * 4.99D);
    }

    /** Ticks until the tide turns from ebb to flood or back. */
    public static long ticksToTurn(Level level) {
        long t = level.getDayTime() % 6000L;
        return 6000L - t;
    }

    // ------------------------------------------------------------------------------------------ the shore

    /** Called every two seconds: at low tide, beaches near each player are laid bare, a few columns at a time. */
    public static void tickShore(ServerLevel level, WorldMemory memory, RandomSource random) {
        if (!isLowTide(level)) {
            return;
        }
        int cycle = (int) (level.getDayTime() / 6000L);
        int seaLevel = level.getSeaLevel();
        for (ServerPlayer player : level.players()) {
            int[] budget = BUDGET.computeIfAbsent(player.getUUID(), k -> new int[]{cycle, 0});
            if (budget[0] != cycle) {
                budget[0] = cycle;
                budget[1] = 0;
            }
            if (budget[1] >= 90) {
                continue;
            }
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = player.getBlockX() + random.nextInt(81) - 40;
                int z = player.getBlockZ() + random.nextInt(81) - 40;
                BlockPos column = new BlockPos(x, 0, z);
                if (!level.isLoaded(column) || memory.isProtected(column)) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                BlockPos top = new BlockPos(x, y, z);
                if (y < seaLevel - 3 || y > seaLevel + 1 || !level.getBlockState(top).is(Blocks.SAND)
                        || !level.getBlockState(top.above()).isAir() || !level.getBiome(top).is(ModTags.HAS_TIDES)
                        || !nearSea(level, top)) {
                    continue;
                }
                level.setBlock(top, WorldBlocks.WET_SAND.get().defaultBlockState(), Block.UPDATE_ALL);
                budget[1]++;
                float roll = random.nextFloat();
                if (roll < 0.10F) {
                    level.setBlock(top.above(), WorldBlocks.SEASHELL.get().defaultBlockState().setValue(ShoreBlock.NATURAL, true)
                            .setValue(SeashellBlock.VARIANT, random.nextInt(4)), Block.UPDATE_ALL);
                } else if (roll < 0.15F) {
                    level.setBlock(top.above(), WorldBlocks.CLAM.get().defaultBlockState().setValue(ShoreBlock.NATURAL, true),
                            Block.UPDATE_ALL);
                } else if (roll < 0.165F) {
                    ItemEntity bottle = new ItemEntity(level, x + 0.5D, y + 1.1D, z + 0.5D,
                            new ItemStack(WorldItems.MESSAGE_IN_A_BOTTLE.get()));
                    bottle.setDeltaMovement(Vec3.ZERO);
                    level.addFreshEntity(bottle);
                }
            }
        }
    }

    private static boolean nearSea(Level level, BlockPos top) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int d = 1; d <= 4; d++) {
                BlockPos p = top.relative(direction, d);
                if (level.getFluidState(p).is(FluidTags.WATER) || level.getFluidState(p.below()).is(FluidTags.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------ the sea coming and going

    private static boolean isBeachGround(BlockState state) {
        return state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(WorldBlocks.WET_SAND.get());
    }

    /** Whether open sea water lies within {@code reach} blocks of this beach block, at the height of the sea. */
    private static boolean seaWithin(ServerLevel level, BlockPos top, int reach) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int d = 1; d <= reach; d++) {
                BlockPos p = top.relative(direction, d);
                if (!level.isLoaded(p)) {
                    break;
                }
                BlockState state = level.getBlockState(p);
                if (state.getFluidState().is(FluidTags.WATER) && state.getFluidState().isSource()) {
                    return true;
                }
                if (!state.getFluidState().isEmpty() || !isBeachGround(state)) {
                    break;
                }
            }
        }
        return false;
    }

    /** Whether land that was land before the ebb lies within three blocks of this shallow water. */
    private static boolean nearShore(ServerLevel level, WorldMemory memory, BlockPos water) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int d = 1; d <= 3; d++) {
                BlockPos p = water.relative(direction, d);
                if (!level.isLoaded(p) || memory.sandbars.contains(p.asLong())) {
                    continue;
                }
                BlockState state = level.getBlockState(p);
                if (state.getFluidState().isEmpty() && state.isSolid()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The tide near each player, every two seconds: at the flood a sheet of surf washes up the low beaches (up to
     * five blocks, as the water rises); at the ebb the shallow water at the edge of the shore dries into sandbars of
     * wet sand. Whatever does not belong to the tide of the moment goes back to what it was, a little at a time.
     */
    public static void tickTide(ServerLevel level, WorldMemory memory, RandomSource random) {
        int reach = floodReach(level);
        boolean ebb = isEbb(level);
        recede(level, memory, reach, ebb, 96);
        int sea = level.getSeaLevel() - 1;
        BlockState surf = WorldBlocks.SURF.get().defaultBlockState();
        for (ServerPlayer player : level.players()) {
            for (int attempt = 0; attempt < 48; attempt++) {
                int x = player.getBlockX() + random.nextInt(65) - 32;
                int z = player.getBlockZ() + random.nextInt(65) - 32;
                BlockPos column = new BlockPos(x, sea, z);
                if (!level.isLoaded(column) || memory.isProtected(column) || !level.getBiome(column).is(ModTags.HAS_TIDES)) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                if (top != sea) {
                    continue;
                }
                BlockPos topPos = new BlockPos(x, top, z);
                BlockState state = level.getBlockState(topPos);
                if (reach > 0 && isBeachGround(state) && level.getBlockState(topPos.above()).isAir() && seaWithin(level, topPos, reach)) {
                    level.setBlock(topPos.above(), surf, Block.UPDATE_ALL);
                    memory.surf.add(topPos.above().asLong());
                    memory.setDirty();
                } else if (ebb && state.is(Blocks.WATER) && state.getFluidState().isSource()
                        && isBeachGround(level.getBlockState(topPos.below())) && nearShore(level, memory, topPos)) {
                    level.setBlock(topPos, WorldBlocks.WET_SAND.get().defaultBlockState(), Block.UPDATE_ALL);
                    memory.sandbars.add(topPos.asLong());
                    memory.setDirty();
                }
            }
        }
    }

    /** Takes back (at most {@code budget} blocks of) surf beyond the flood's reach and sandbars once the ebb is over. */
    public static void recede(ServerLevel level, WorldMemory memory, int reach, boolean ebb, int budget) {
        int done = 0;
        LongIterator it = memory.surf.iterator();
        while (it.hasNext() && done < budget) {
            BlockPos pos = BlockPos.of(it.nextLong());
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (reach > 0 && level.getBlockState(pos).is(WorldBlocks.SURF.get()) && seaWithin(level, pos.below(), reach)) {
                continue;
            }
            if (level.getBlockState(pos).is(WorldBlocks.SURF.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            it.remove();
            done++;
        }
        it = memory.sandbars.iterator();
        while (!ebb && it.hasNext() && done < budget) {
            BlockPos pos = BlockPos.of(it.nextLong());
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockState(pos).is(WorldBlocks.WET_SAND.get())) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            }
            it.remove();
            done++;
        }
        if (done > 0) {
            memory.setDirty();
        }
    }

    /** Server side: the rising tide brings more fish to the hook. */
    public static void onFished(Player player, Vec3 hook, ItemStack caught) {
        Level level = player.level();
        if (!isHighTide(level) || !level.getBiome(BlockPos.containing(hook)).is(ModTags.HAS_TIDES) || level.random.nextFloat() >= 0.3F) {
            return;
        }
        ItemEntity bonus = new ItemEntity(level, hook.x, hook.y, hook.z, caught.copy());
        double dx = player.getX() - hook.x;
        double dy = player.getY() - hook.y;
        double dz = player.getZ() - hook.z;
        bonus.setDeltaMovement(dx * 0.1D, dy * 0.1D + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08D, dz * 0.1D);
        level.addFreshEntity(bonus);
    }
}
