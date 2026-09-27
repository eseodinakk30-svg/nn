package com.dunesrelics.block;

import com.dunesrelics.entity.Pharaoh;
import com.dunesrelics.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The resting place of a Pharaoh. Sealed sarcophagi only generate deep inside ancient tombs: opening
 * (or breaking) one awakens the Pharaoh. Sarcophagi crafted by players are unsealed and purely decorative.
 */
public class SarcophagusBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 13.0D, 15.0D);

    public SarcophagusBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SEALED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SEALED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(SEALED)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            awaken(serverLevel, pos, state, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (state.getValue(SEALED) && !player.isCreative() && level instanceof ServerLevel serverLevel) {
            awaken(serverLevel, pos, state, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    /** Breaks the seal and releases the Pharaoh. Returns the spawned boss, if any. */
    @Nullable
    public static Pharaoh awaken(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (level.getBlockState(pos).is(state.getBlock())) {
            level.setBlock(pos, state.setValue(SEALED, false), 3);
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("block.dunesrelics.sarcophagus.peaceful"), true);
            }
            return null;
        }
        Direction facing = state.getValue(FACING);
        BlockPos spawn = pos.relative(facing);
        if (!level.getBlockState(spawn).getCollisionShape(level, spawn).isEmpty()
                || !level.getBlockState(spawn.above()).getCollisionShape(level, spawn.above()).isEmpty()) {
            spawn = pos.above();
        }
        Pharaoh pharaoh = ModEntities.PHARAOH.get().create(level);
        if (pharaoh == null) {
            return null;
        }
        pharaoh.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, facing.toYRot(), 0.0F);
        pharaoh.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.TRIGGERED, null, null);
        if (player != null && !player.isCreative() && !player.isSpectator()) {
            pharaoh.setTarget(player);
        }
        level.addFreshEntity(pharaoh);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 60, 0.6D, 0.6D, 0.6D, 0.15D);
        level.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, 12, 0.4D, 0.4D, 0.4D, 0.02D);
        level.playSound(null, pos, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 0.6F);
        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
        return pharaoh;
    }
}
