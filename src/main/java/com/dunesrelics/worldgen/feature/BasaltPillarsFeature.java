package com.dunesrelics.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A cluster of hexagonal-looking basalt columns of uneven heights, like a small Giant's Causeway. */
public class BasaltPillarsFeature extends Feature<NoneFeatureConfiguration> {
    public BasaltPillarsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!RuinHelper.isSolid(level, origin.below())) {
            return false;
        }
        int radius = 2 + random.nextInt(3);
        int peak = 3 + random.nextInt(6);
        BlockState basalt = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > radius + 0.3D || random.nextFloat() < 0.12F) {
                    continue;
                }
                int surface = RuinHelper.surfaceY(level, origin.getX() + x, origin.getZ() + z, origin.getY());
                int height = Math.max(1, (int) Math.round(peak * (1.0D - d / (radius + 1.0D))) + random.nextInt(3) - 1);
                BlockPos base = new BlockPos(origin.getX() + x, surface, origin.getZ() + z);
                for (int y = 1; y <= height; y++) {
                    if (RuinHelper.isLoose(level, base.above(y))) {
                        level.setBlock(base.above(y), y == height && random.nextInt(3) == 0
                                ? Blocks.POLISHED_BASALT.defaultBlockState() : basalt, RuinHelper.FLAGS);
                    }
                }
            }
        }
        return true;
    }
}
