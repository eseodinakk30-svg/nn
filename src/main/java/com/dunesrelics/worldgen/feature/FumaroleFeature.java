package com.dunesrelics.worldgen.feature;

import com.dunesrelics.registry.VolcanicBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A steam vent set into the ground, crusted with sulfur crystals and ash. */
public class FumaroleFeature extends Feature<NoneFeatureConfiguration> {
    public FumaroleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos ground = context.origin().below();
        if (!RuinHelper.isSolid(level, ground) || !level.isEmptyBlock(ground.above())) {
            return false;
        }
        level.setBlock(ground, VolcanicBlocks.STEAM_VENT.get().defaultBlockState(), RuinHelper.FLAGS);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = ground.relative(dir);
            if (RuinHelper.isSolid(level, side) && level.isEmptyBlock(side.above())) {
                if (random.nextFloat() < 0.5F) {
                    level.setBlock(side, VolcanicBlocks.SULFUR_BLOCK.get().defaultBlockState(), RuinHelper.FLAGS);
                }
                if (random.nextFloat() < 0.6F) {
                    level.setBlock(side.above(), VolcanicBlocks.SULFUR_CLUSTER.get().defaultBlockState(), RuinHelper.FLAGS);
                } else {
                    level.setBlock(side.above(), VolcanicBlocks.ASH_LAYER.get().defaultBlockState()
                            .setValue(SnowLayerBlock.LAYERS, 1 + random.nextInt(2)), RuinHelper.FLAGS);
                }
            }
        }
        return true;
    }
}
