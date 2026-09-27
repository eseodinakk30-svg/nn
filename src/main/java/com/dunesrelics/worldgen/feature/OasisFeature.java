package com.dunesrelics.worldgen.feature;

import com.dunesrelics.block.AloeVeraBlock;
import com.dunesrelics.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * A turquoise pond in the sand, ringed by grass, cattails and sugar cane, shaded by a few date palms.
 */
public class OasisFeature extends Feature<NoneFeatureConfiguration> {
    public OasisFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!level.getBlockState(origin.below()).is(BlockTags.SAND)) {
            return false;
        }
        int radiusX = 4 + random.nextInt(3);
        int radiusZ = 3 + random.nextInt(3);
        int waterY = origin.getY() - 1;

        // Only settle in a hollow: the rim must not be lower than the water, or the pond would spill.
        for (int i = 0; i < 16; i++) {
            float angle = i / 16.0F * ((float) Math.PI * 2.0F);
            int x = origin.getX() + Mth.floor(Mth.cos(angle) * (radiusX + 2));
            int z = origin.getZ() + Mth.floor(Mth.sin(angle) * (radiusZ + 2));
            if (RuinHelper.surfaceY(level, x, z, waterY) < waterY) {
                return false;
            }
        }

        int reach = Math.max(radiusX, radiusZ) + 3;
        List<BlockPos> pond = new ArrayList<>();
        List<BlockPos> shore = new ArrayList<>();
        List<BlockPos> outskirts = new ArrayList<>();
        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                double e = (x * x) / (double) (radiusX * radiusX) + (z * z) / (double) (radiusZ * radiusZ);
                BlockPos column = new BlockPos(origin.getX() + x, waterY, origin.getZ() + z);
                if (e <= 1.0D) {
                    clearAbove(level, column, 8);
                    int depth = e < 0.3D ? 3 : e < 0.65D ? 2 : 1;
                    for (int d = 0; d < depth; d++) {
                        level.setBlock(column.below(d), Blocks.WATER.defaultBlockState(), RuinHelper.FLAGS);
                        pond.add(column.below(d));
                    }
                    level.setBlock(column.below(depth), random.nextFloat() < 0.3F ? Blocks.CLAY.defaultBlockState()
                            : Blocks.SAND.defaultBlockState(), RuinHelper.FLAGS);
                } else if (e <= 2.0D) {
                    clearAbove(level, column, 8);
                    level.setBlock(column, Blocks.GRASS_BLOCK.defaultBlockState(), RuinHelper.FLAGS);
                    level.setBlock(column.below(), Blocks.DIRT.defaultBlockState(), RuinHelper.FLAGS);
                    RuinHelper.underpin(level, column.below(), 4);
                    shore.add(column);
                } else if (e <= 3.0D) {
                    outskirts.add(column);
                }
            }
        }

        // Seal the pond so no water leaks sideways into dips or caves.
        for (BlockPos water : pond) {
            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN}) {
                BlockPos neighbor = water.relative(dir);
                BlockState state = level.getBlockState(neighbor);
                if (!state.is(Blocks.WATER) && (state.isAir() || state.canBeReplaced())) {
                    level.setBlock(neighbor, Blocks.SAND.defaultBlockState(), RuinHelper.FLAGS);
                }
            }
        }

        // Water plants.
        for (BlockPos water : pond) {
            if (water.getY() == waterY && random.nextFloat() < 0.1F && level.isEmptyBlock(water.above())) {
                level.setBlock(water.above(), Blocks.LILY_PAD.defaultBlockState(), RuinHelper.FLAGS);
            }
        }

        // Shore plants.
        BlockState cattail = ModBlocks.CATTAIL.get().defaultBlockState();
        for (BlockPos ground : shore) {
            BlockPos above = ground.above();
            if (!level.isEmptyBlock(above)) {
                continue;
            }
            boolean nearWater = false;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(ground.relative(dir)).is(Blocks.WATER)) {
                    nearWater = true;
                    break;
                }
            }
            float roll = random.nextFloat();
            if (nearWater && roll < 0.3F && level.isEmptyBlock(above.above())) {
                DoublePlantBlock.placeAt(level, cattail, above, RuinHelper.FLAGS);
            } else if (nearWater && roll < 0.45F) {
                int height = 1 + random.nextInt(3);
                for (int i = 0; i < height && level.isEmptyBlock(above.above(i)); i++) {
                    level.setBlock(above.above(i), Blocks.SUGAR_CANE.defaultBlockState(), RuinHelper.FLAGS);
                }
            } else if (roll < 0.7F) {
                level.setBlock(above, Blocks.GRASS.defaultBlockState(), RuinHelper.FLAGS);
            } else if (roll < 0.8F) {
                level.setBlock(above, Blocks.FERN.defaultBlockState(), RuinHelper.FLAGS);
            } else if (roll < 0.85F) {
                level.setBlock(above, ModBlocks.DESERT_ROSE.get().defaultBlockState(), RuinHelper.FLAGS);
            }
        }

        // The outskirts fade back into the desert.
        for (BlockPos column : outskirts) {
            BlockPos top = new BlockPos(column.getX(), RuinHelper.surfaceY(level, column.getX(), column.getZ(), waterY) + 1, column.getZ());
            if (!level.getBlockState(top.below()).is(BlockTags.SAND) || !level.isEmptyBlock(top)) {
                continue;
            }
            float roll = random.nextFloat();
            if (roll < 0.15F) {
                level.setBlock(top, ModBlocks.DUNE_GRASS.get().defaultBlockState(), RuinHelper.FLAGS);
            } else if (roll < 0.19F) {
                level.setBlock(top, ModBlocks.ALOE_VERA.get().defaultBlockState()
                        .setValue(AloeVeraBlock.AGE, AloeVeraBlock.MAX_AGE), RuinHelper.FLAGS);
            }
        }

        // Date palms leaning over the water.
        int palms = 1 + random.nextInt(3);
        for (int i = 0; i < palms && !shore.isEmpty(); i++) {
            BlockPos ground = shore.get(random.nextInt(shore.size()));
            if (level.isEmptyBlock(ground.above())) {
                PalmTreeFeature.grow(level, random, ground.above());
            }
        }
        return true;
    }

    private static void clearAbove(WorldGenLevel level, BlockPos column, int height) {
        for (int dy = 1; dy <= height; dy++) {
            BlockPos pos = column.above(dy);
            if (RuinHelper.isLoose(level, pos) && !level.isEmptyBlock(pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), RuinHelper.FLAGS);
            }
        }
    }
}
