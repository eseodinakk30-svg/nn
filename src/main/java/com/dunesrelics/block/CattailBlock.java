package com.dunesrelics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A tall reed that lines oasis ponds. */
public class CattailBlock extends DoublePlantBlock {
    public CattailBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return DesertPlants.mayPlaceOn(state) || state.is(Blocks.MUD) || state.is(Blocks.CLAY) || state.is(Blocks.GRAVEL)
                || super.mayPlaceOn(state, level, pos);
    }
}
