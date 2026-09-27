package com.dunesrelics.worldgen.feature;

import com.dunesrelics.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A date palm: a tall trunk that leans to one side as it grows, crowned by long drooping fronds.
 * Used both by world generation and by palm saplings.
 */
public class PalmTreeFeature extends Feature<NoneFeatureConfiguration> {
    public PalmTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return grow(context.level(), context.random(), context.origin());
    }

    public static boolean grow(WorldGenLevel level, RandomSource random, BlockPos origin) {
        BlockState ground = level.getBlockState(origin.below());
        if (!ground.is(BlockTags.SAND) && !ground.is(BlockTags.DIRT)) {
            return false;
        }

        // Trunk: straight for a few blocks, then leaning one block sideways every other block.
        int height = 5 + random.nextInt(4);
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int bendStart = 2 + random.nextInt(2);
        List<BlockPos> trunk = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int i = 0; i < height; i++) {
            if (i > bendStart && (i - bendStart) % 2 == 1 && random.nextFloat() < 0.8F) {
                cursor.move(lean);
            }
            trunk.add(cursor.immutable());
            cursor.move(Direction.UP);
        }
        for (BlockPos pos : trunk) {
            if (!canReplace(level, pos)) {
                return false;
            }
        }
        BlockPos top = trunk.get(trunk.size() - 1);

        // Crown: a tuft on top, four long drooping fronds and four short diagonal ones.
        Set<BlockPos> leaves = new LinkedHashSet<>();
        leaves.add(top.above());
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            leaves.add(top.above().relative(dir));
            int length = 3 + random.nextInt(2);
            BlockPos frond = top;
            for (int step = 1; step <= length; step++) {
                frond = frond.relative(dir);
                leaves.add(frond);
                if (step == length - 1) {
                    frond = frond.below();
                    leaves.add(frond);
                }
            }
            leaves.add(frond.below());
            // Diagonal frond between this direction and the next, hanging off the first leaf of this frond.
            Direction side = dir.getClockWise();
            BlockPos diagonal = top.relative(dir).relative(side);
            leaves.add(diagonal);
            leaves.add(diagonal.relative(side).below());
            leaves.add(diagonal.relative(side));
        }

        BlockState log = ModBlocks.PALM_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        for (BlockPos pos : trunk) {
            level.setBlock(pos, log, 19);
        }
        if (ground.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
            level.setBlock(origin.below(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 19);
        }

        // Leaves need a correct distance to the trunk, otherwise they would decay.
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos pos : leaves) {
            for (Direction dir : Direction.values()) {
                if (trunk.contains(pos.relative(dir))) {
                    distance.put(pos, 1);
                    queue.add(pos);
                    break;
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int next = distance.get(pos) + 1;
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (leaves.contains(neighbor) && !distance.containsKey(neighbor)) {
                    distance.put(neighbor, next);
                    queue.add(neighbor);
                }
            }
        }
        BlockState leaf = ModBlocks.PALM_LEAVES.get().defaultBlockState();
        for (BlockPos pos : leaves) {
            Integer d = distance.get(pos);
            if (d == null || d > 6 || !canReplace(level, pos)) {
                continue;
            }
            level.setBlock(pos, leaf.setValue(LeavesBlock.DISTANCE, d).setValue(LeavesBlock.PERSISTENT, false), 19);
        }
        return true;
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        return level.isStateAtPosition(pos, state -> state.isAir() || state.canBeReplaced()
                || state.is(BlockTags.LEAVES) || state.is(BlockTags.SAPLINGS));
    }
}
