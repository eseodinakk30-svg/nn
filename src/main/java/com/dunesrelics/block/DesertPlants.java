package com.dunesrelics.block;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Ground that desert plants accept, on top of the usual dirt. */
public final class DesertPlants {
    private DesertPlants() {}

    public static boolean mayPlaceOn(BlockState ground) {
        return ground.is(BlockTags.SAND) || ground.is(BlockTags.TERRACOTTA) || ground.is(BlockTags.DIRT)
                || ground.is(Blocks.SANDSTONE) || ground.is(Blocks.RED_SANDSTONE);
    }
}
