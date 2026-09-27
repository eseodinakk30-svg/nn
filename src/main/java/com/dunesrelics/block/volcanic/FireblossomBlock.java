package com.dunesrelics.block.volcanic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A glowing flower of the ash fields that sheds tiny sparks. */
public class FireblossomBlock extends FlowerBlock {
    public FireblossomBlock(Properties properties) {
        super(() -> MobEffects.MOVEMENT_SPEED, 6, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return VolcanicPlants.mayPlaceOn(state) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.3D,
                    pos.getY() + 0.7D, pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.3D, 0.0D, 0.01D, 0.0D);
        }
    }
}
