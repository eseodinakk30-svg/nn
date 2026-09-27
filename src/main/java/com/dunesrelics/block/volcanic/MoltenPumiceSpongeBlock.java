package com.dunesrelics.block.volcanic;

import com.dunesrelics.registry.VolcanicBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A pumice sponge full of lava. It glows, burns whoever stands on it, and makes excellent furnace fuel (the dry
 * sponge is left behind). Next to water it cools off with a hiss and is ready to use again.
 */
public class MoltenPumiceSpongeBlock extends MagmaBlock {
    public MoltenPumiceSpongeBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        this.tryCool(level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        this.tryCool(level, pos);
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    private void tryCool(Level level, BlockPos pos) {
        if (!level.isClientSide && PumiceSpongeBlock.adjacentWater(level, pos) != null) {
            level.setBlock(pos, VolcanicBlocks.PUMICE_SPONGE.get().defaultBlockState(), 3);
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
            ((ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5D, pos.getY() + 1.0D,
                    pos.getZ() + 0.5D, 12, 0.4D, 0.3D, 0.4D, 0.02D);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_LAVA, pos.getX() + random.nextDouble(), pos.getY() - 0.05D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.05D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.02D, 0.0D);
        }
    }
}
