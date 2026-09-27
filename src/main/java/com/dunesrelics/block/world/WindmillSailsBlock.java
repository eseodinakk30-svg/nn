package com.dunesrelics.block.world;

import com.dunesrelics.block.entity.WindmillSailsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Windmill sails: the hub sits on the front of a tower and the four sails turn in the wind. They turn faster up high
 * and in a storm, and not at all with something built in front of them or no open sky. The wind strength (0 to 3)
 * is the block's signal strength times five.
 */
public class WindmillSailsBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** 0 still, 1 a breeze, 2 wind, 3 a gale. */
    public static final IntegerProperty WIND = IntegerProperty.create("wind", 0, 3);
    private static final VoxelShape SHAPE_NS = Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 16.0D);
    private static final VoxelShape SHAPE_EW = Block.box(0.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);

    public WindmillSailsBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WIND, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WIND);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The sails face the player who puts them up.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindmillSailsBlockEntity(pos, state);
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        level.scheduleTick(pos, this, 10);
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int wind = wind(level, pos, state.getValue(FACING));
        if (wind != state.getValue(WIND)) {
            level.setBlock(pos, state.setValue(WIND, wind), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, 100);
    }

    /** How hard the wind blows on sails here: by height above the sea, the weather, and room for the sails to turn. */
    public static int wind(Level level, BlockPos pos, Direction facing) {
        if (!level.canSeeSky(pos.above()) && !level.canSeeSky(pos.relative(facing).above())) {
            return 0;
        }
        // The sails sweep a 7 x 7 disc in front of the hub; anything solid there stops them.
        for (int a = -3; a <= 3; a++) {
            for (int b = -3; b <= 3; b++) {
                if (a * a + b * b > 10 || a == 0 && b == 0) {
                    continue;
                }
                BlockPos p = facing.getAxis() == Direction.Axis.Z ? pos.offset(a, b, 0) : pos.offset(0, b, a);
                if (level.getBlockState(p).blocksMotion()) {
                    return 0;
                }
            }
        }
        int height = pos.getY() - level.getSeaLevel();
        int wind = height >= 40 ? 2 : height >= 8 ? 1 : 0;
        if (level.isThundering()) {
            wind += 1;
        } else if (level.isRaining()) {
            wind = Math.max(wind, 1);
        }
        return Math.max(1, Math.min(3, wind));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(WIND) * 5;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}
