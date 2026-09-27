package com.dunesrelics.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.Tags;

import java.util.Arrays;

/**
 * Small builders shared by the world's memory and the growing villages: houses, lamp posts, wells, market stalls,
 * fields, farms and paths, in the building style of the biome they stand in. They only build on open, natural ground
 * and never in chunks a player has built in.
 */
public final class Builders {
    public static final int FAIL = Integer.MIN_VALUE;
    private static final int FLAGS = Block.UPDATE_ALL;

    private Builders() {}

    /** The building materials of one village style. */
    public record Palette(BlockState foundation, BlockState planks, BlockState log, Block stairs, Block slab,
                          BlockState fence, BlockState gate, Block door, boolean flatRoof) {
    }

    public static final Palette OAK = new Palette(Blocks.COBBLESTONE.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(),
            Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.OAK_FENCE.defaultBlockState(),
            Blocks.OAK_FENCE_GATE.defaultBlockState(), Blocks.OAK_DOOR, false);
    public static final Palette SPRUCE = new Palette(Blocks.COBBLESTONE.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(),
            Blocks.SPRUCE_LOG.defaultBlockState(), Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_FENCE.defaultBlockState(),
            Blocks.SPRUCE_FENCE_GATE.defaultBlockState(), Blocks.SPRUCE_DOOR, false);
    public static final Palette ACACIA = new Palette(Blocks.TERRACOTTA.defaultBlockState(), Blocks.ACACIA_PLANKS.defaultBlockState(),
            Blocks.ACACIA_LOG.defaultBlockState(), Blocks.ACACIA_STAIRS, Blocks.ACACIA_SLAB, Blocks.ACACIA_FENCE.defaultBlockState(),
            Blocks.ACACIA_FENCE_GATE.defaultBlockState(), Blocks.ACACIA_DOOR, false);
    public static final Palette JUNGLE = new Palette(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.JUNGLE_PLANKS.defaultBlockState(),
            Blocks.JUNGLE_LOG.defaultBlockState(), Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_SLAB, Blocks.JUNGLE_FENCE.defaultBlockState(),
            Blocks.JUNGLE_FENCE_GATE.defaultBlockState(), Blocks.JUNGLE_DOOR, false);
    public static final Palette SANDSTONE = new Palette(Blocks.SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(),
            Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE_SLAB,
            Blocks.BIRCH_FENCE.defaultBlockState(), Blocks.BIRCH_FENCE_GATE.defaultBlockState(), Blocks.BIRCH_DOOR, true);

    public static Palette palette(Holder<Biome> biome) {
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(BiomeTags.HAS_VILLAGE_DESERT)) {
            return SANDSTONE;
        }
        if (biome.is(BiomeTags.IS_SAVANNA)) {
            return ACACIA;
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return JUNGLE;
        }
        if (biome.is(BiomeTags.IS_TAIGA) || biome.value().getBaseTemperature() < 0.3F) {
            return SPRUCE;
        }
        return OAK;
    }

    // ------------------------------------------------------------------------------------------ site checks

    /** The block a building of {@code w} x {@code d} (before rotation) would occupy at local (u, y, v). */
    public static BlockPos local(BlockPos origin, Direction front, int w, int d, int u, int y, int v) {
        return switch (front) {
            case SOUTH -> origin.offset(w - 1 - u, y, d - 1 - v);
            case WEST -> origin.offset(v, y, w - 1 - u);
            case EAST -> origin.offset(d - 1 - v, y, u);
            default -> origin.offset(u, y, v);
        };
    }

    private static boolean isGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.TERRACOTTA) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND);
    }

    private static boolean isOpen(BlockState state) {
        return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    /** Height of the ground (top solid block) in a column, ignoring leaves and plants. */
    public static int groundY(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    /**
     * Checks a rectangle of {@code sx} x {@code sz} columns starting at (x, z): loaded, not near player builds,
     * natural ground, at most {@code slope} blocks of height difference, and nothing but air or plants above
     * (up to {@code headroom}). Returns the building level, or {@link #FAIL}.
     */
    public static int site(ServerLevel level, WorldMemory memory, int x, int z, int sx, int sz, int slope, int headroom) {
        int[] heights = new int[sx * sz];
        int i = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < sx; dx++) {
            for (int dz = 0; dz < sz; dz++) {
                pos.set(x + dx, 0, z + dz);
                if (!level.isLoaded(pos) || memory.isProtected(pos)) {
                    return FAIL;
                }
                int y = groundY(level, x + dx, z + dz);
                pos.setY(y);
                if (!isGround(level.getBlockState(pos))) {
                    return FAIL;
                }
                for (int up = 1; up <= headroom; up++) {
                    pos.setY(y + up);
                    BlockState above = level.getBlockState(pos);
                    if (!isOpen(above) || above.is(BlockTags.LOGS)) {
                        return FAIL;
                    }
                }
                heights[i++] = y;
            }
        }
        Arrays.sort(heights);
        if (heights[heights.length - 1] - heights[0] > slope) {
            return FAIL;
        }
        return heights[heights.length / 2];
    }

    /** Fills below each column down to the ground and clears plants above, so a building stands on level ground. */
    private static void levelGround(ServerLevel level, BlockPos min, int sx, int sz, int y, BlockState fill, int clear) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < sx; dx++) {
            for (int dz = 0; dz < sz; dz++) {
                int ground = groundY(level, min.getX() + dx, min.getZ() + dz);
                for (int yy = Math.min(ground + 1, y); yy <= y; yy++) {
                    pos.set(min.getX() + dx, yy, min.getZ() + dz);
                    level.setBlock(pos, fill, FLAGS);
                }
                for (int yy = y + 1; yy <= y + clear; yy++) {
                    pos.set(min.getX() + dx, yy, min.getZ() + dz);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                    }
                }
                for (int yy = ground; yy > y; yy--) {
                    pos.set(min.getX() + dx, yy, min.getZ() + dz);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                }
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), FLAGS);
    }

    private static BlockState stairs(Block stairs, Direction facing, boolean upsideDown) {
        return stairs.defaultBlockState().setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
    }

    // ------------------------------------------------------------------------------------------ buildings

    public static final Block[] WORKSTATIONS = {Blocks.COMPOSTER, Blocks.BARREL, Blocks.SMOKER, Blocks.BLAST_FURNACE,
            Blocks.LECTERN, Blocks.FLETCHING_TABLE, Blocks.CARTOGRAPHY_TABLE, Blocks.CAULDRON, Blocks.BREWING_STAND,
            Blocks.SMITHING_TABLE, Blocks.LOOM, Blocks.STONECUTTER};

    /**
     * A cottage 7 wide and 5 deep with a gabled roof, a bed and a workstation. {@code min} is the north-west corner of
     * its footprint, {@code front} the side its door looks out of. Returns the block in front of the door.
     */
    public static BlockPos house(ServerLevel level, BlockPos min, int y, Direction front, Palette p, Block workstation) {
        int w = 7;
        int d = 5;
        int sx = front.getAxis() == Direction.Axis.Z ? w : d;
        int sz = front.getAxis() == Direction.Axis.Z ? d : w;
        BlockPos origin = new BlockPos(min.getX(), y, min.getZ());
        levelGround(level, origin, sx, sz, y, p.foundation(), 8);
        Direction back = front.getOpposite();
        for (int u = 0; u < w; u++) {
            for (int v = 0; v < d; v++) {
                boolean edgeU = u == 0 || u == w - 1;
                boolean edgeV = v == 0 || v == d - 1;
                set(level, local(origin, front, w, d, u, 0, v), edgeU || edgeV ? p.foundation() : p.planks());
                for (int h = 1; h <= 3; h++) {
                    BlockPos at = local(origin, front, w, d, u, h, v);
                    if (edgeU && edgeV) {
                        set(level, at, p.log());
                    } else if (edgeU || edgeV) {
                        boolean window = h == 2 && (edgeU && v == 2 || v == d - 1 && (u == 2 || u == 4));
                        set(level, at, window ? Blocks.GLASS_PANE.defaultBlockState() : p.planks());
                    }
                }
                // roof
                if (p.flatRoof()) {
                    set(level, local(origin, front, w, d, u, 4, v), p.slab().defaultBlockState());
                } else if (v == 0) {
                    set(level, local(origin, front, w, d, u, 4, v), stairs(p.stairs(), back, false));
                } else if (v == d - 1) {
                    set(level, local(origin, front, w, d, u, 4, v), stairs(p.stairs(), front, false));
                } else {
                    set(level, local(origin, front, w, d, u, 4, v), p.planks());
                    if (v == 1) {
                        set(level, local(origin, front, w, d, u, 5, v), stairs(p.stairs(), back, false));
                    } else if (v == d - 2) {
                        set(level, local(origin, front, w, d, u, 5, v), stairs(p.stairs(), front, false));
                    } else {
                        set(level, local(origin, front, w, d, u, 5, v), p.slab().defaultBlockState());
                    }
                }
            }
        }
        // door
        BlockState door = p.door().defaultBlockState().setValue(DoorBlock.FACING, back);
        level.setBlock(local(origin, front, w, d, 3, 1, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), FLAGS);
        level.setBlock(local(origin, front, w, d, 3, 2, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), FLAGS);
        // bed along the left wall, head towards the back
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, back);
        level.setBlock(local(origin, front, w, d, 1, 1, 2), bed.setValue(BedBlock.PART, BedPart.FOOT), FLAGS);
        level.setBlock(local(origin, front, w, d, 1, 1, 3), bed.setValue(BedBlock.PART, BedPart.HEAD), FLAGS);
        // workstation and a lantern hanging from the ceiling
        BlockState station = workstation.defaultBlockState();
        if (station.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            station = station.setValue(BlockStateProperties.HORIZONTAL_FACING, front);
        }
        set(level, local(origin, front, w, d, 5, 1, 3), station);
        level.setBlock(local(origin, front, w, d, 3, 3, 2), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), FLAGS);
        BlockPos step = local(origin, front, w, d, 3, 0, 0).relative(front);
        pathAt(level, step.getX(), step.getZ());
        return step;
    }

    /** A fence post with a lantern on top. */
    public static boolean lampPost(ServerLevel level, WorldMemory memory, int x, int z, Palette p) {
        int y = site(level, memory, x, z, 1, 1, 0, 3);
        if (y == FAIL) {
            return false;
        }
        BlockPos base = new BlockPos(x, y, z);
        for (int h = 1; h <= 3; h++) {
            if (!level.getBlockState(base.above(h)).isAir()) {
                level.setBlock(base.above(h), Blocks.AIR.defaultBlockState(), FLAGS);
            }
        }
        set(level, base.above(1), p.fence());
        set(level, base.above(2), p.fence());
        level.setBlock(base.above(3), Blocks.LANTERN.defaultBlockState(), FLAGS);
        return true;
    }

    /** A cobblestone well with a little roof. {@code min} is its north-west corner (3 x 3). */
    public static void well(ServerLevel level, BlockPos min, int y, Palette p) {
        BlockPos origin = new BlockPos(min.getX(), y, min.getZ());
        levelGround(level, origin, 3, 3, y, p.foundation(), 5);
        for (int u = 0; u < 3; u++) {
            for (int v = 0; v < 3; v++) {
                BlockPos at = origin.offset(u, 0, v);
                boolean center = u == 1 && v == 1;
                level.setBlock(at.below(), center ? Blocks.WATER.defaultBlockState() : p.foundation(), FLAGS);
                level.setBlock(at, center ? Blocks.WATER.defaultBlockState() : Blocks.COBBLESTONE_WALL.defaultBlockState(), FLAGS);
                if (u != 1 && v != 1) {
                    set(level, at.above(), p.fence());
                    set(level, at.above(2), p.fence());
                }
                level.setBlock(at.above(3), p.slab().defaultBlockState(), FLAGS);
            }
        }
        for (int u = 0; u < 3; u++) {
            for (int v = 0; v < 3; v++) {
                BlockPos at = origin.offset(u, 0, v);
                if (!(u == 1 && v == 1)) {
                    set(level, at, Blocks.COBBLESTONE_WALL.defaultBlockState());
                }
            }
        }
    }

    /** A market stall: barrels on a counter under a woollen canopy. {@code min} is its corner (3 x 2). */
    public static void stall(ServerLevel level, BlockPos min, int y, Palette p, RandomSource random) {
        BlockPos origin = new BlockPos(min.getX(), y, min.getZ());
        levelGround(level, origin, 3, 2, y, Blocks.DIRT.defaultBlockState(), 4);
        Block[] canopies = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL, Blocks.GREEN_WOOL, Blocks.ORANGE_WOOL, Blocks.WHITE_WOOL};
        Block canopy = canopies[random.nextInt(canopies.length)];
        for (int u = 0; u < 3; u++) {
            for (int v = 0; v < 2; v++) {
                BlockPos at = origin.offset(u, 0, v);
                if (v == 0) {
                    level.setBlock(at.above(), u == 1 ? Blocks.CRAFTING_TABLE.defaultBlockState()
                            : Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), FLAGS);
                } else if (u != 1) {
                    set(level, at.above(), p.fence());
                }
                if (u != 1) {
                    set(level, at.above(2), p.fence());
                }
                level.setBlock(at.above(3), canopy.defaultBlockState(), FLAGS);
            }
        }
    }

    /** A plot of crops around a water source; {@code min} is its corner. */
    public static void field(ServerLevel level, BlockPos min, int y, int size, RandomSource random) {
        BlockPos origin = new BlockPos(min.getX(), y, min.getZ());
        levelGround(level, origin, size, size, y, Blocks.DIRT.defaultBlockState(), 3);
        Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
        Block crop = crops[random.nextInt(crops.length)];
        int middle = size / 2;
        for (int u = 0; u < size; u++) {
            for (int v = 0; v < size; v++) {
                BlockPos at = origin.offset(u, 0, v);
                if (u == middle && v == middle) {
                    level.setBlock(at, Blocks.WATER.defaultBlockState(), FLAGS);
                    level.setBlock(at.above(), Blocks.OAK_TRAPDOOR.defaultBlockState(), FLAGS);
                    continue;
                }
                level.setBlock(at, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7), FLAGS);
                BlockState plant = crop.defaultBlockState();
                if (crop instanceof CropBlock cropBlock) {
                    plant = cropBlock.getStateForAge(random.nextInt(cropBlock.getMaxAge()));
                }
                level.setBlock(at.above(), plant, FLAGS);
            }
        }
    }

    /**
     * A fenced farm (9 x 9) with a gate, a composter and a scarecrow, as a family of farmers would set up near a road.
     */
    public static void farm(ServerLevel level, BlockPos min, int y, Palette p, Direction gateSide, RandomSource random) {
        BlockPos origin = new BlockPos(min.getX(), y, min.getZ());
        levelGround(level, origin, 9, 9, y, Blocks.DIRT.defaultBlockState(), 3);
        field(level, origin.offset(1, 0, 1), y, 7, random);
        for (int u = 0; u < 9; u++) {
            for (int v = 0; v < 9; v++) {
                if (u != 0 && u != 8 && v != 0 && v != 8) {
                    continue;
                }
                BlockPos at = origin.offset(u, 1, v);
                boolean gate = switch (gateSide) {
                    case NORTH -> v == 0 && u == 4;
                    case SOUTH -> v == 8 && u == 4;
                    case WEST -> u == 0 && v == 4;
                    default -> u == 8 && v == 4;
                };
                set(level, at, gate ? p.gate().setValue(FenceGateBlock.FACING, gateSide) : p.fence());
            }
        }
        // scarecrow in a corner of the field: a post, a hay body and a pumpkin head
        BlockPos scarecrow = origin.offset(1, 1, 1);
        level.setBlock(scarecrow, p.fence(), FLAGS);
        level.setBlock(scarecrow.above(), Blocks.HAY_BLOCK.defaultBlockState(), FLAGS);
        level.setBlock(scarecrow.above(2), Blocks.CARVED_PUMPKIN.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, gateSide), FLAGS);
        BlockPos composter = origin.offset(7, 1, 7);
        level.setBlock(composter, Blocks.COMPOSTER.defaultBlockState(), FLAGS);
    }

    /** A bell standing on a stone post, which makes a group of houses a village. */
    public static void bell(ServerLevel level, BlockPos ground) {
        level.setBlock(ground.above(), Blocks.COBBLESTONE.defaultBlockState(), FLAGS);
        level.setBlock(ground.above(2), Blocks.BELL.defaultBlockState(), FLAGS);
    }

    // ------------------------------------------------------------------------------------------ paths

    /** Turns the grass (or dirt) at the top of a column into a path block; returns false over water or rock. */
    public static boolean pathAt(ServerLevel level, int x, int z) {
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
        BlockState top = level.getBlockState(pos);
        if (!top.getFluidState().isEmpty() || !level.isLoaded(pos)) {
            return false;
        }
        if (top.canBeReplaced() || top.is(BlockTags.FLOWERS) || top.is(BlockTags.SAPLINGS)) {
            level.removeBlock(pos, false);
            pos = pos.below();
            top = level.getBlockState(pos);
        }
        if (top.is(Blocks.GRASS_BLOCK) || top.is(Blocks.DIRT) || top.is(Blocks.COARSE_DIRT) || top.is(Blocks.PODZOL)
                || top.is(Blocks.MYCELIUM) || top.is(Blocks.ROOTED_DIRT)) {
            level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), FLAGS);
            return true;
        }
        return top.is(Blocks.DIRT_PATH);
    }

    /** A straight dirt path between two points, skipping anything but natural ground and player-built chunks. */
    public static void path(ServerLevel level, WorldMemory memory, BlockPos from, BlockPos to, int maxLength) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        int steps = (int) Math.min(maxLength, Math.sqrt(dx * dx + dz * dz));
        for (int i = 0; i <= steps; i++) {
            int x = (int) Math.round(from.getX() + dx * i / Math.max(1, steps));
            int z = (int) Math.round(from.getZ() + dz * i / Math.max(1, steps));
            BlockPos column = new BlockPos(x, 0, z);
            if (!memory.isProtected(column)) {
                pathAt(level, x, z);
            }
        }
    }

    public static BlockState slab(Block slab) {
        return slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }
}
