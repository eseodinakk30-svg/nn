package com.dunesrelics.block.world;

import com.dunesrelics.block.entity.WaterWheelBlockEntity;
import com.dunesrelics.registry.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A wooden water wheel. Put it where water flows past its paddles (or under a filled trough) and it turns: it then
 * drives a millstone at the end of its axle and gives off a redstone signal.
 */
public class WaterWheelBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty SPINNING = BooleanProperty.create("spinning");
    private static final VoxelShape SHAPE_X = Block.box(0.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);
    private static final VoxelShape SHAPE_Z = Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 16.0D);

    public WaterWheelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(SPINNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, SPINNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // You face the wheel when placing it, so the axle runs away from you.
        return this.defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterWheelBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        level.scheduleTick(pos, this, 4);
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean spinning = hasCurrent(level, pos, state.getValue(AXIS));
        if (spinning != state.getValue(SPINNING)) {
            level.setBlock(pos, state.setValue(SPINNING, spinning), 3);
        }
        // Water further along the paddles does not always cause a neighbour update, so keep looking.
        level.scheduleTick(pos, this, 40);
    }

    /** Whether moving water pushes on the paddles around the hub, or a filled trough pours onto the wheel. */
    public static boolean hasCurrent(Level level, BlockPos pos, Direction.Axis axis) {
        BlockState above = level.getBlockState(pos.above());
        if (above.is(WorldBlocks.WATER_TROUGH.get()) && above.getValue(WaterTroughBlock.WATER) > 0) {
            return true;
        }
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                BlockPos paddle = axis == Direction.Axis.X ? pos.offset(0, a, b) : pos.offset(b, a, 0);
                FluidState fluid = level.getFluidState(paddle);
                if (fluid.is(FluidTags.WATER) && (!fluid.isSource() || fluid.getFlow(level, paddle).lengthSqr() > 1.0E-4D)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(SPINNING) ? 15 : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}
