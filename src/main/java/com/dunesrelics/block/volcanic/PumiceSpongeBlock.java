package com.dunesrelics.block.volcanic;

import com.dunesrelics.registry.VolcanicBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * A pumice sponge: porous volcanic rock that soaks up lava the way a sponge soaks up water.
 * It drains up to 64 lava blocks within 6 blocks and turns into a Molten Pumice Sponge.
 */
public class PumiceSpongeBlock extends Block {
    private static final int MAX_DEPTH = 6;
    private static final int MAX_BLOCKS = 64;

    public PumiceSpongeBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            this.tryAbsorb(level, pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        this.tryAbsorb(level, pos);
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    private void tryAbsorb(Level level, BlockPos pos) {
        if (!level.isClientSide && absorbLava(level, pos) > 0) {
            level.setBlock(pos, VolcanicBlocks.MOLTEN_PUMICE_SPONGE.get().defaultBlockState(), 2);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
    }

    /** Removes lava around {@code origin}, breadth first; returns how many blocks were drained. */
    public static int absorbLava(Level level, BlockPos origin) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        ArrayDeque<Integer> depths = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(origin);
        depths.add(0);
        seen.add(origin);
        int drained = 0;
        while (!queue.isEmpty() && drained < MAX_BLOCKS) {
            BlockPos pos = queue.poll();
            int depth = depths.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!seen.add(next)) {
                    continue;
                }
                FluidState fluid = level.getFluidState(next);
                if (!fluid.is(FluidTags.LAVA) || !(level.getBlockState(next).getBlock() instanceof LiquidBlock)) {
                    continue;
                }
                level.setBlock(next, Blocks.AIR.defaultBlockState(), 3);
                drained++;
                if (depth < MAX_DEPTH) {
                    queue.add(next);
                    depths.add(depth + 1);
                }
                if (drained >= MAX_BLOCKS) {
                    break;
                }
            }
        }
        return drained;
    }

    @Nullable
    public static BlockPos adjacentWater(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                return pos.relative(direction);
            }
        }
        return null;
    }
}
