package com.dunesrelics.block.volcanic;

import com.dunesrelics.item.Tempering;
import com.dunesrelics.registry.VolcanicItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Volcanic Forge. Fill it with a bucket of lava (enough for four temperings), then use a tool, weapon or
 * piece of armor on it while carrying a Fire Opal to temper the item.
 */
public class VolcanicForgeBlock extends HorizontalDirectionalBlock {
    public static final int MAX_LAVA = 4;
    public static final IntegerProperty LAVA = IntegerProperty.create("lava", 0, MAX_LAVA);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 10, 14),
            Block.box(0, 10, 0, 16, 14, 16));

    public VolcanicForgeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LAVA, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LAVA);
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
        ItemStack held = player.getItemInHand(hand);
        int lava = state.getValue(LAVA);
        if (held.is(Items.LAVA_BUCKET)) {
            if (lava >= MAX_LAVA) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(LAVA, MAX_LAVA), 3);
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!Tempering.canTemper(held)) {
            if (Tempering.isTempered(held) && !level.isClientSide) {
                player.displayClientMessage(Component.translatable("block.dunesrelics.volcanic_forge.already"), true);
            }
            return Tempering.isTempered(held) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (lava == 0) {
            player.displayClientMessage(Component.translatable("block.dunesrelics.volcanic_forge.no_lava"), true);
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild && !consumeOpal(player)) {
            player.displayClientMessage(Component.translatable("block.dunesrelics.volcanic_forge.no_opal"), true);
            return InteractionResult.CONSUME;
        }
        Tempering.temper(held);
        level.setBlock(pos, state.setValue(LAVA, lava - 1), 3);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        ((ServerLevel) level).sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                12, 0.3D, 0.2D, 0.3D, 0.0D);
        player.displayClientMessage(Component.translatable("block.dunesrelics.volcanic_forge.tempered", held.getHoverName()), true);
        return InteractionResult.CONSUME;
    }

    private static boolean consumeOpal(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(VolcanicItems.FIRE_OPAL.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LAVA) > 0 && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D, pos.getY() + 1.0D,
                    pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D, 0.0D, 0.03D, 0.0D);
            if (random.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
