package com.dunesrelics.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Where a village's materials come from: its lumberjacks fell the natural trees around it (and plant a sapling where
 * each one stood), its quarrymen fence off a pit outside the village and dig it down course by course. Neither
 * ever touches the village's own buildings or anything near where a player has built.
 */
public final class Gathering {
    /** The quarry pit is 5 x 5 and goes 10 deep; then the quarrymen start a new one. */
    public static final int PIT = 5;
    public static final int DEPTH = 10;
    private static final int MAX_LOGS = 96;

    private Gathering() {}

    // ------------------------------------------------------------------------------------------ trees

    /** A natural tree: the logs of one trunk and its branches (lowest first), and the block at its foot. */
    public record Tree(BlockPos base, List<BlockPos> logs, BlockState log) {}

    /** How much wood a village likes to have in store: the bigger it is, the more. */
    public static int woodWanted(WorldMemory.VillageRecord record) {
        return 96 + record.houses * 24;
    }

    public static int stoneWanted(WorldMemory.VillageRecord record) {
        return 96 + record.houses * 24;
    }

    /** Looks for a natural tree between 12 and 52 blocks from the bell; null if there is none in reach. */
    @Nullable
    public static Tree findTree(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record, RandomSource random) {
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 12.0D + random.nextDouble() * 40.0D;
            int x = record.bell.getX() + (int) (Math.cos(angle) * distance);
            int z = record.bell.getZ() + (int) (Math.sin(angle) * distance);
            BlockPos column = new BlockPos(x, record.bell.getY(), z);
            if (!level.isLoaded(column) || memory.isProtected(column)) {
                continue;
            }
            BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
            if (!level.getBlockState(top).is(BlockTags.LOGS)) {
                continue;
            }
            BlockPos base = top;
            while (level.getBlockState(base.below()).is(BlockTags.LOGS) && top.getY() - base.getY() < 32) {
                base = base.below();
            }
            if (!level.getBlockState(base.below()).is(BlockTags.DIRT)) {
                continue;
            }
            if (record.snapshot != null && record.snapshot.indexOf(base) >= 0) {
                // the village's own timber (a house post, a tree it planted in the square)
                continue;
            }
            Tree tree = collect(level, base);
            if (tree != null) {
                return tree;
            }
        }
        return null;
    }

    /** The connected logs above {@code base}, if they carry leaves (so it is a tree, not somebody's log pile). */
    @Nullable
    private static Tree collect(ServerLevel level, BlockPos base) {
        BlockState log = level.getBlockState(base);
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(base);
        seen.add(base);
        boolean leaves = false;
        while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
            BlockPos pos = queue.poll();
            logs.add(pos);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (!level.isLoaded(next) || !seen.add(next)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(next);
                        if (state.is(BlockTags.LOGS)) {
                            queue.add(next);
                        } else if (state.is(BlockTags.LEAVES)) {
                            leaves = true;
                        }
                    }
                }
            }
        }
        if (!leaves || logs.size() >= MAX_LOGS) {
            return null;
        }
        logs.sort(Comparator.comparingInt(BlockPos::getY));
        return new Tree(base.immutable(), logs, log);
    }

    /** Fells the tree (the leaves wither by themselves) and plants a sapling of its kind at its foot. Returns the logs. */
    public static int fell(ServerLevel level, Tree tree) {
        int felled = 0;
        for (BlockPos pos : tree.logs()) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                if (felled % 3 == 0) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5D, pos.getY() + 0.5D,
                            pos.getZ() + 0.5D, 12, 0.4D, 0.4D, 0.4D, 0.1D);
                }
                felled++;
            }
        }
        level.playSound(null, tree.base(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 0.7F, 0.6F);
        Block sapling = saplingFor(tree.log());
        if (sapling != null && level.getBlockState(tree.base()).isAir() && level.getBlockState(tree.base().below()).is(BlockTags.DIRT)) {
            level.setBlock(tree.base(), sapling.defaultBlockState(), Block.UPDATE_ALL);
        }
        return felled;
    }

    /** The sapling that grows the tree this log came from ("birch_log" to "birch_sapling"), if there is one. */
    @Nullable
    public static Block saplingFor(BlockState log) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(log.getBlock());
        String kind = key.getPath().replace("stripped_", "").replace("_log", "").replace("_wood", "");
        String[] names = kind.equals("mangrove") ? new String[]{"mangrove_propagule"} : new String[]{kind + "_sapling"};
        for (String name : names) {
            Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(key.getNamespace(), name));
            if (block instanceof SaplingBlock || block != Blocks.AIR && name.endsWith("propagule")) {
                return block;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------ the quarry

    /** Finds a place for a quarry: natural ground 24 to 60 blocks from the bell, dry, level enough, nobody's building. */
    @Nullable
    public static BlockPos findQuarry(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record, RandomSource random) {
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 24.0D + random.nextDouble() * 36.0D;
            int x = record.bell.getX() + (int) (Math.cos(angle) * distance);
            int z = record.bell.getZ() + (int) (Math.sin(angle) * distance);
            int low = Integer.MAX_VALUE;
            int high = Integer.MIN_VALUE;
            boolean ok = true;
            for (int dx = -1; dx <= PIT && ok; dx++) {
                for (int dz = -1; dz <= PIT && ok; dz++) {
                    BlockPos column = new BlockPos(x + dx, 0, z + dz);
                    if (!level.isLoaded(column) || memory.isProtected(column)) {
                        ok = false;
                        break;
                    }
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz) - 1;
                    BlockPos top = new BlockPos(x + dx, y, z + dz);
                    BlockState state = level.getBlockState(top);
                    if (!state.getFluidState().isEmpty() || !(state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                            || state.is(Blocks.GRAVEL) || state.is(BlockTags.BASE_STONE_OVERWORLD))) {
                        ok = false;
                        break;
                    }
                    if (record.snapshot != null) {
                        for (int dy = -DEPTH; dy <= 3 && ok; dy++) {
                            if (record.snapshot.indexOf(top.above(dy)) >= 0) {
                                ok = false;
                            }
                        }
                    }
                    low = Math.min(low, y);
                    high = Math.max(high, y);
                }
            }
            if (ok && high - low <= 2) {
                return new BlockPos(x, low, z);
            }
        }
        return null;
    }

    /** Puts a fence round a new pit (with a gate on the side of the ladder). */
    public static void fenceQuarry(ServerLevel level, BlockPos corner) {
        for (int dx = -1; dx <= PIT; dx++) {
            for (int dz = -1; dz <= PIT; dz++) {
                boolean rim = dx == -1 || dx == PIT || dz == -1 || dz == PIT;
                if (!rim) {
                    continue;
                }
                int x = corner.getX() + dx;
                int z = corner.getZ() + dz;
                BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                if (!Construction.canReplace(level.getBlockState(pos)) || !level.getBlockState(pos).getFluidState().isEmpty()) {
                    continue;
                }
                boolean gate = dz == -1 && dx == PIT / 2;
                BlockState state = gate ? Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH)
                        : Blocks.OAK_FENCE.defaultBlockState();
                Construction.lay(level, pos, state);
            }
        }
    }

    /** The {@code i}-th block of the pit, course by course from the top; null once the pit is as deep as it goes. */
    @Nullable
    public static BlockPos pitBlock(BlockPos corner, int i) {
        int layer = i / (PIT * PIT);
        if (layer >= DEPTH) {
            return null;
        }
        int j = i % (PIT * PIT);
        return corner.offset(j % PIT, -layer, j / PIT);
    }

    /**
     * Digs one block of the pit into the village's stock. Returns false if the pit has to stop here: it would let in
     * water or lava, or it has reached something built.
     */
    public static boolean dig(ServerLevel level, WorldMemory.VillageRecord record, BlockPos corner, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (direction != Direction.UP && !level.getFluidState(pos.relative(direction)).isEmpty()) {
                return false;
            }
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        boolean stone = state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.ORES) || state.is(Tags.Blocks.COBBLESTONE)
                || state.is(Tags.Blocks.SANDSTONE);
        if (!stone && !Construction.canReplace(state)) {
            return false;
        }
        level.destroyBlock(pos, false);
        if (stone) {
            record.stone += state.is(Tags.Blocks.ORES) ? 2 : 1;
        }
        // a ladder down the north wall, course by course, so the pit can be climbed out of
        if (pos.getX() == corner.getX() + PIT / 2 && pos.getZ() == corner.getZ()) {
            level.setBlock(pos, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        }
        return true;
    }
}
