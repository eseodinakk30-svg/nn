package com.dunesrelics.block.world;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** A seashell in one of four colours; purely decorative, and it can be ground into bone meal. */
public class SeashellBlock extends ShoreBlock {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);

    public SeashellBlock(Properties properties) {
        super(properties, Block.box(4, 0, 4, 12, 3, 12));
        this.registerDefaultState(this.stateDefinition.any().setValue(NATURAL, false).setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(VARIANT, context.getLevel().random.nextInt(4));
    }
}
