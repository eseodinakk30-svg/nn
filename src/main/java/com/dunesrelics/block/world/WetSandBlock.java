package com.dunesrelics.block.world;

import com.dunesrelics.memory.Tides;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Sand the ebbing tide has just uncovered. It does not fall, and the returning tide turns it back into sand. */
public class WetSandBlock extends Block {
    public WetSandBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (Tides.isHighTide(level) || !Tides.isLowTide(level) && random.nextInt(3) == 0) {
            level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
        }
    }
}
