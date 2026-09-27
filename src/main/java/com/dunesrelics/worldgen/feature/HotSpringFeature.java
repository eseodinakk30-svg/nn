package com.dunesrelics.worldgen.feature;

import com.dunesrelics.registry.VolcanicBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A steaming hot spring: a pumice-lined pool of warm water ringed by steam vents and sulfur.
 * Bathing in it (standing in water over pumice) slowly heals you.
 */
public class HotSpringFeature extends Feature<NoneFeatureConfiguration> {
    public HotSpringFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int waterY = origin.getY() - 1;
        int radius = 2 + random.nextInt(2);
        for (int i = 0; i < 12; i++) {
            double angle = i / 12.0D * Math.PI * 2.0D;
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * (radius + 1));
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * (radius + 1));
            if (RuinHelper.surfaceY(level, x, z, waterY) < waterY) {
                return false;
            }
        }
        BlockState pumice = VolcanicBlocks.PUMICE.get().defaultBlockState();
        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int z = -radius - 1; z <= radius + 1; z++) {
                double d = Math.sqrt(x * x + z * z);
                BlockPos column = new BlockPos(origin.getX() + x, waterY, origin.getZ() + z);
                if (d <= radius) {
                    for (int dy = 1; dy <= 4; dy++) {
                        if (RuinHelper.isLoose(level, column.above(dy))) {
                            level.setBlock(column.above(dy), Blocks.AIR.defaultBlockState(), RuinHelper.FLAGS);
                        }
                    }
                    int depth = d < radius - 1 ? 2 : 1;
                    for (int dy = 0; dy < depth; dy++) {
                        level.setBlock(column.below(dy), Blocks.WATER.defaultBlockState(), RuinHelper.FLAGS);
                    }
                    level.setBlock(column.below(depth), pumice, RuinHelper.FLAGS);
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        for (int dy = 0; dy < depth; dy++) {
                            BlockPos side = column.below(dy).relative(dir);
                            BlockState state = level.getBlockState(side);
                            if (!state.is(Blocks.WATER) && (state.isAir() || state.canBeReplaced())) {
                                level.setBlock(side, pumice, RuinHelper.FLAGS);
                            }
                        }
                    }
                } else if (d <= radius + 1.5D) {
                    level.setBlock(column, random.nextFloat() < 0.7F ? pumice : VolcanicBlocks.SULFUR_BLOCK.get().defaultBlockState(),
                            RuinHelper.FLAGS);
                    BlockPos above = column.above();
                    if (RuinHelper.isLoose(level, above)) {
                        float roll = random.nextFloat();
                        level.setBlock(above, roll < 0.25F ? VolcanicBlocks.SULFUR_CLUSTER.get().defaultBlockState()
                                : Blocks.AIR.defaultBlockState(), RuinHelper.FLAGS);
                    }
                    if (random.nextFloat() < 0.18F) {
                        level.setBlock(column, VolcanicBlocks.STEAM_VENT.get().defaultBlockState(), RuinHelper.FLAGS);
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), RuinHelper.FLAGS);
                    }
                }
            }
        }
        return true;
    }
}
