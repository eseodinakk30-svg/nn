package com.dunesrelics.memory;

import com.dunesrelics.block.world.SeashellBlock;
import com.dunesrelics.block.world.ShoreBlock;
import com.dunesrelics.registry.ModTags;
import com.dunesrelics.registry.WorldBlocks;
import com.dunesrelics.registry.WorldItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
