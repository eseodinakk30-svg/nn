package com.dunesrelics.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.function.Supplier;

/** Palm wood blocks burn exactly like vanilla planks (encouragement 5, flammability 20). */
public final class FlammableBlocks {
    private static final int SPREAD = 5;
    private static final int FLAMMABILITY = 20;

    private FlammableBlocks() {}

    public static class Planks extends Block {
        public Planks(Properties properties) { super(properties); }
        @Override public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return true; }
        @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return FLAMMABILITY; }
        @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return SPREAD; }
    }

    public static class Stairs extends StairBlock {
        public Stairs(Supplier<BlockState> base, Properties properties) { super(base, properties); }
        @Override public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return true; }
        @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return FLAMMABILITY; }
        @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return SPREAD; }
    }

    public static class Slab extends SlabBlock {
        public Slab(Properties properties) { super(properties); }
        @Override public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return true; }
        @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return FLAMMABILITY; }
        @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return SPREAD; }
    }

    public static class Fence extends FenceBlock {
        public Fence(Properties properties) { super(properties); }
        @Override public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return true; }
        @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return FLAMMABILITY; }
        @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return SPREAD; }
    }

    public static class FenceGate extends FenceGateBlock {
        public FenceGate(Properties properties) { super(properties, WoodType.OAK); }
        @Override public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return true; }
        @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return FLAMMABILITY; }
        @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return SPREAD; }
    }
}
