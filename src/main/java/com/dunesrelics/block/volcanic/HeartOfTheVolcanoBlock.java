package com.dunesrelics.block.volcanic;

import com.dunesrelics.entity.volcanic.MagmaTitan;
import com.dunesrelics.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The beating heart deep inside every volcano. A dormant heart awakens the Magma Titan when touched or broken.
 * Hearts crafted by players are inert light sources.
 */
public class HeartOfTheVolcanoBlock extends Block {
    public static final BooleanProperty DORMANT = BooleanProperty.create("dormant");

    public HeartOfTheVolcanoBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DORMANT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DORMANT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(DORMANT)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            awaken(serverLevel, pos, state, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (state.getValue(DORMANT) && !player.isCreative() && level instanceof ServerLevel serverLevel) {
            awaken(serverLevel, pos, state, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    public static MagmaTitan awaken(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (level.getBlockState(pos).is(state.getBlock())) {
            level.setBlock(pos, state.setValue(DORMANT, false), 3);
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("block.dunesrelics.heart_of_the_volcano.peaceful"), true);
            }
            return null;
        }
        MagmaTitan titan = ModEntities.MAGMA_TITAN.get().create(level);
        if (titan == null) {
            return null;
        }
        BlockPos spawn = pos.above();
        titan.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        titan.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.TRIGGERED, null, null);
        if (player != null && !player.isCreative() && !player.isSpectator()) {
            titan.setTarget(player);
        }
        level.addFreshEntity(titan);
        level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 40, 1.0D, 0.5D, 1.0D, 0.1D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D, 30, 1.0D, 1.0D, 1.0D, 0.05D);
        level.playSound(null, pos, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.5F, 0.6F);
        level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5F, 0.5F);
        return titan;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(state.getValue(DORMANT) ? 2 : 6) == 0) {
            level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
        }
    }
}
