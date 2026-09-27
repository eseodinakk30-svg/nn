package com.dunesrelics.block.volcanic;

import com.dunesrelics.registry.VolcanicBlocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Ground that plants of the volcanic biomes accept. */
public final class VolcanicPlants {
    private VolcanicPlants() {}

    public static boolean mayPlaceOn(BlockState ground) {
        return ground.is(VolcanicBlocks.ASH_BLOCK.get()) || ground.is(VolcanicBlocks.SCORIA.get())
                || ground.is(VolcanicBlocks.BLACK_SAND.get()) || ground.is(VolcanicBlocks.PUMICE.get())
                || ground.is(Blocks.BASALT) || ground.is(Blocks.TUFF) || ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND);
    }
}
