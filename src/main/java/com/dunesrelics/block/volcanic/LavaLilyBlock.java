package com.dunesrelics.block.volcanic;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** A heat-proof lily pad that floats on lava. Walk across lava lakes on them. */
public class LavaLilyBlock extends WaterlilyBlock {
    public LavaLilyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        FluidState above = level.getFluidState(pos.above());
        return fluid.is(FluidTags.LAVA) && fluid.isSource() && above.isEmpty();
    }
}
