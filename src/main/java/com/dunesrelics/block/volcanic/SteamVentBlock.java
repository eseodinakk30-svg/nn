package com.dunesrelics.block.volcanic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A fumarole. Every few seconds it erupts in a jet of steam that launches anything standing above it
 * several blocks into the air. Sneaking keeps you grounded; a redstone signal holds the vent shut.
 */
public class SteamVentBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final int REACH = 6;

    public SteamVentBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, 40 + level.random.nextInt(60));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(ACTIVE)) {
            if (powered || random.nextInt(35) == 0) {
                level.setBlock(pos, state.setValue(ACTIVE, false), 3);
                level.scheduleTick(pos, this, 80 + random.nextInt(100));
                return;
            }
            this.blow(level, pos, random);
            level.scheduleTick(pos, this, 1);
        } else if (powered) {
            level.scheduleTick(pos, this, 40);
        } else {
            level.setBlock(pos, state.setValue(ACTIVE, true), 3);
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
            level.scheduleTick(pos, this, 1);
        }
    }

    private void blow(ServerLevel level, BlockPos pos, RandomSource random) {
        AABB column = new AABB(pos.above()).expandTowards(0.0D, REACH - 1, 0.0D);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, column, e -> !e.isSpectator() && !e.isShiftKeyDown())) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, Math.max(motion.y, 0.9D), motion.z);
            entity.resetFallDistance();
            entity.hurtMarked = true;
            if (entity instanceof Player player) {
                player.resetFallDistance();
            }
        }
        if (level.getGameTime() % 2 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 4,
                    0.15D, 0.1D, 0.15D, 0.25D);
        }
        if (random.nextInt(12) == 0) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 0.5F + random.nextFloat() * 0.3F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + random.nextDouble(), pos.getY() + 1.05D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.02D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D, 0.0D, 0.03D, 0.0D);
        }
    }
}
