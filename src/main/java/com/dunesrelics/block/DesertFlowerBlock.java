package com.dunesrelics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The desert rose: grows on sand, and makes Fire Resistance suspicious stew. */
public class DesertFlowerBlock extends FlowerBlock {
    public DesertFlowerBlock(Properties properties) {
        super(() -> MobEffects.FIRE_RESISTANCE, 5, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return DesertPlants.mayPlaceOn(state) || super.mayPlaceOn(state, level, pos);
    }
}
