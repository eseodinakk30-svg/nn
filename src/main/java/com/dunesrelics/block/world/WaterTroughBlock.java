package com.dunesrelics.block.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wooden trough that carries water like an aqueduct. A trough next to (or under) a water source fills up; water then
 * runs on through connected troughs, getting a little lower each time, for up to seven troughs (and freely downhill).
 * A filled trough keeps farmland within four blocks moist and waters the crops around it, so they grow faster.
 */
public class WaterTroughBlock extends Block {
    public static final int MAX_WATER = 7;
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, MAX_WATER);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static {
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = Block.box(0, 0, 0, 16, 2, 16);
            if ((mask & 1) == 0) {
                shape = Shapes.or(shape, Block.box(0, 0, 0, 16, 8, 2));
            }
            if ((mask & 2) == 0) {
                shape = Shapes.or(shape, Block.box(14, 0, 0, 16, 8, 16));
            }
            if ((mask & 4) == 0) {
                shape = Shapes.or(shape, Block.box(0, 0, 14, 16, 8, 16));
            }
            if ((mask & 8) == 0) {
                shape = Shapes.or(shape, Block.box(0, 0, 0, 2, 8, 16));
            }
            SHAPES[mask] = shape;
        }
    }

    public WaterTroughBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATER, 0).setValue(NORTH, false)
                .setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATER, NORTH, EAST, SOUTH, WEST);
    }

    private static BooleanProperty side(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0) | (state.getValue(SOUTH) ? 4 : 0)
                | (state.getValue(WEST) ? 8 : 0);
        return SHAPES[mask];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(side(direction),
                    context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(this));
        }
        return state.setValue(WATER, computeWater(context.getLevel(), context.getClickedPos()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        level.scheduleTick(pos, this, 5);
        if (direction.getAxis().isHorizontal()) {
            return state.setValue(side(direction), neighborState.is(this));
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, 5);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int water = computeWater(level, pos);
        if (water != state.getValue(WATER)) {
            level.setBlock(pos, state.setValue(WATER, water), 3);
        }
    }

    /** Full next to a water source; otherwise one less than the fullest neighbouring trough (or as full as one above). */
    public static int computeWater(LevelAccessor level, BlockPos pos) {
        int best = 0;
        for (Direction direction : new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            BlockPos next = pos.relative(direction);
            FluidState fluid = level.getFluidState(next);
            if (fluid.is(FluidTags.WATER) && (fluid.isSource() || direction == Direction.UP)) {
                return MAX_WATER;
            }
            BlockState neighbor = level.getBlockState(next);
            if (neighbor.getBlock() instanceof WaterTroughBlock) {
                int w = neighbor.getValue(WATER);
                best = Math.max(best, direction == Direction.UP ? w : w - 1);
            }
        }
        return Math.max(0, best);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(WATER) > 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        irrigate(level, pos, random);
    }

    /** Moistens farmland within four blocks and gives the crops around the trough an extra growth tick. */
    public static void irrigate(ServerLevel level, BlockPos pos, RandomSource random) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 0, 4))) {
            BlockState farmland = level.getBlockState(p);
            if (farmland.getBlock() instanceof FarmBlock && farmland.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
                level.setBlock(p, farmland.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), 2);
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 1, 3))) {
            BlockState crop = level.getBlockState(p);
            if ((crop.getBlock() instanceof CropBlock || crop.getBlock() instanceof StemBlock) && random.nextInt(3) == 0) {
                crop.randomTick(level, p.immutable(), random);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (state.getValue(WATER) > 0 && (held.is(Items.GLASS_BOTTLE) || held.is(Items.BUCKET))) {
            if (!level.isClientSide) {
                ItemStack filled = held.is(Items.BUCKET) ? new ItemStack(Items.WATER_BUCKET)
                        : PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
                level.playSound(null, pos, held.is(Items.BUCKET) ? SoundEvents.BUCKET_FILL : SoundEvents.BOTTLE_FILL,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
