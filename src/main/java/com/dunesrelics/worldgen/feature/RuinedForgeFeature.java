package com.dunesrelics.worldgen.feature;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.volcanic.VolcanicForgeBlock;
import com.dunesrelics.registry.VolcanicBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The ruin of a forge-keeper's smithy: scoria brick walls, a cold lava basin, a battered anvil,
 * a Volcanic Forge and a chest of tools and fire opals.
 */
public class RuinedForgeFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceLocation LOOT = DunesRelics.id("chests/ruined_forge");

    public RuinedForgeFeature(Codec<NoneFeatureConfiguration> codec) {
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
        BlockPos floor = origin.below();
        Direction front = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int half = 3;
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                BlockPos pos = floor.offset(x, 0, z);
                level.setBlock(pos, random.nextFloat() < 0.2F ? VolcanicBlocks.CRACKED_SCORIA_BRICKS.get().defaultBlockState()
                        : VolcanicBlocks.POLISHED_SCORIA.get().defaultBlockState(), RuinHelper.FLAGS);
                RuinHelper.underpin(level, pos, 4);
                for (int dy = 1; dy <= 5; dy++) {
                    if (RuinHelper.isLoose(level, pos.above(dy))) {
                        level.setBlock(pos.above(dy), Blocks.AIR.defaultBlockState(), RuinHelper.FLAGS);
                    }
                }
                boolean edge = Math.abs(x) == half || Math.abs(z) == half;
                boolean door = front.getAxis() == Direction.Axis.X ? x == front.getStepX() * half && Math.abs(z) <= 1
                        : z == front.getStepZ() * half && Math.abs(x) <= 1;
                if (edge && !door) {
                    int height = random.nextInt(4) == 0 ? 0 : 1 + random.nextInt(3);
                    for (int dy = 1; dy <= height; dy++) {
                        BlockState wall = random.nextFloat() < 0.25F ? VolcanicBlocks.CRACKED_SCORIA_BRICKS.get().defaultBlockState()
                                : VolcanicBlocks.SCORIA_BRICKS.get().defaultBlockState();
                        level.setBlock(pos.above(dy), wall, RuinHelper.FLAGS);
                    }
                }
            }
        }
        Direction back = front.getOpposite();
        BlockPos forge = origin.relative(back, 2);
        level.setBlock(forge, VolcanicBlocks.VOLCANIC_FORGE.get().defaultBlockState().setValue(VolcanicForgeBlock.FACING, front)
                .setValue(VolcanicForgeBlock.LAVA, random.nextInt(2)), RuinHelper.FLAGS);
        level.setBlock(forge.relative(back.getClockWise(), 2), Blocks.DAMAGED_ANVIL.defaultBlockState()
                .setValue(AnvilBlock.FACING, front.getClockWise()), RuinHelper.FLAGS);
        BlockPos basin = forge.relative(back.getCounterClockWise(), 2).below();
        level.setBlock(basin, Blocks.MAGMA_BLOCK.defaultBlockState(), RuinHelper.FLAGS);
        level.setBlock(basin.relative(front), Blocks.MAGMA_BLOCK.defaultBlockState(), RuinHelper.FLAGS);
        RuinHelper.chest(level, random, origin.relative(back.getClockWise(), 2), front, LOOT);
        if (random.nextFloat() < 0.5F) {
            level.setBlock(origin.relative(front.getClockWise(), 2), VolcanicBlocks.PUMICE_SPONGE.get().defaultBlockState(), RuinHelper.FLAGS);
        }
        return true;
    }
}
