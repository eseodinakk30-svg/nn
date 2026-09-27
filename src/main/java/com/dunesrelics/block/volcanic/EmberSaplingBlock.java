package com.dunesrelics.block.volcanic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;

public class EmberSaplingBlock extends SaplingBlock {
    public EmberSaplingBlock(AbstractTreeGrower grower, Properties properties) {
        super(grower, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return VolcanicPlants.mayPlaceOn(state) || super.mayPlaceOn(state, level, pos);
    }
}
