package com.dunesrelics.block.volcanic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Volcanic ash settles in layers like snow, but never melts. */
public class AshLayerBlock extends SnowLayerBlock {
    public AshLayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Unlike snow, ash does not melt.
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false;
    }
}
