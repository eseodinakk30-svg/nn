package com.dunesrelics.block.volcanic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A small plant (ash grass, sulfur crystals) that grows on ash, scoria and black sand. */
public class VolcanicPlantBlock extends BushBlock {
    private final VoxelShape shape;

    public VolcanicPlantBlock(Properties properties, double height) {
        super(properties);
        this.shape = Block.box(2.0D, 0.0D, 2.0D, 14.0D, height, 14.0D);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return this.shape.move(offset.x, offset.y, offset.z);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return VolcanicPlants.mayPlaceOn(state) || state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }
}
