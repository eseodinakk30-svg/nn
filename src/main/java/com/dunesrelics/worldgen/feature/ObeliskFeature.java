package com.dunesrelics.worldgen.feature;

import com.dunesrelics.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A lone limestone obelisk with a gilded tip, sometimes toppled, standing on a polished plinth. */
public class ObeliskFeature extends Feature<NoneFeatureConfiguration> {
    public ObeliskFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState ground = level.getBlockState(origin.below());
        if (!ground.is(BlockTags.SAND) && !ground.is(Blocks.SANDSTONE)) {
            return false;
        }
        BlockPos base = origin.below();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = base.offset(x, 0, z);
                level.setBlock(pos, x == 0 && z == 0 ? ModBlocks.CHISELED_LIMESTONE_BRICKS.get().defaultBlockState()
                        : ModBlocks.POLISHED_LIMESTONE.get().defaultBlockState(), RuinHelper.FLAGS);
                RuinHelper.underpin(level, pos, 5);
            }
        }

        boolean toppled = random.nextFloat() < 0.3F;
        int height = toppled ? 2 + random.nextInt(2) : 5 + random.nextInt(5);
        for (int y = 1; y <= height; y++) {
            BlockState state = y == height / 2 + 1 ? ModBlocks.CHISELED_LIMESTONE_BRICKS.get().defaultBlockState()
                    : RuinHelper.pillar(Direction.Axis.Y);
            level.setBlock(origin.above(y - 1), state, RuinHelper.FLAGS);
        }
        if (!toppled) {
            level.setBlock(origin.above(height), ModBlocks.GILDED_LIMESTONE.get().defaultBlockState(), RuinHelper.FLAGS);
        } else {
            Direction fall = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int length = 3 + random.nextInt(3);
            for (int i = 2; i < 2 + length; i++) {
                BlockPos pos = origin.relative(fall, i);
                if (RuinHelper.isLoose(level, pos)) {
                    BlockState state = i == 1 + length ? ModBlocks.GILDED_LIMESTONE.get().defaultBlockState()
                            : RuinHelper.pillar(fall.getAxis());
                    level.setBlock(pos, state, RuinHelper.FLAGS);
                    RuinHelper.underpin(level, pos, 3);
                }
            }
        }
        if (random.nextFloat() < 0.4F) {
            BlockPos urn = origin.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random), 2);
            if (level.isEmptyBlock(urn) && RuinHelper.isSolid(level, urn.below())) {
                level.setBlock(urn, RuinHelper.sealedUrn(), RuinHelper.FLAGS);
            }
        }
        return true;
    }
}
