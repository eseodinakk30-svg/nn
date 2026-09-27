package com.dunesrelics.memory;

import com.dunesrelics.block.world.WindmillSailsBlock;
import com.dunesrelics.registry.WorldBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Small builders shared by the world's memory and the growing villages: houses, lamp posts, wells, market stalls,
 * fields, farms and paths, in the building style of the biome they stand in. They only build on open, natural ground
 * and never in chunks a player has built in.
 */
public final class Builders {
    public static final int FAIL = Integer.MIN_VALUE;
    private static final int FLAGS = Block.UPDATE_ALL;
    /** While a building is being recorded as a blueprint, the blocks go here instead of into the world. */
    @Nullable
    private static Blueprint recording;

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
                // a footing at most four blocks deep: over a hollow the building stands on posts, not a pillar
                for (int yy = Math.max(y - 4, Math.min(ground + 1, y)); yy <= y; yy++) {
                    pos.set(min.getX() + dx, yy, min.getZ() + dz);
                    put(level, pos, fill);
                }
                for (int yy = y + 1; yy <= y + clear; yy++) {
                    pos.set(min.getX() + dx, yy, min.getZ() + dz);
                    if (!level.getBlockState(pos).isAir()) {
                        put(level, pos, Blocks.AIR.defaultBlockState());
                    }
                }
                // higher ground is cut down to the floor, but never more than the building's height
                for (int yy = Math.min(ground, y + clear); yy > y; yy--) {
                    pos.set(min.getX() + dx, yy, min.getZ() + dz);
                    put(level, pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        if (recording != null) {
            recording.record(pos, state);
        } else {
            level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), FLAGS);
        }
    }

    /** Every block the builders place goes through here, so a building can be recorded instead of placed. */
    private static void put(ServerLevel level, BlockPos pos, BlockState state) {
        if (recording != null) {
            recording.record(pos, state);
        } else {
            level.setBlock(pos, state, FLAGS);
        }
    }

    /**
     * Runs {@code build} and returns the blocks it would have placed, in the order a builder lays them, without
     * changing the world. Villagers then build it block by block (see {@link Construction}).
     */
    public static Blueprint record(ServerLevel level, String project, Runnable build) {
        Blueprint blueprint = new Blueprint(project);
        recording = blueprint;
        try {
            build.run();
        } finally {
            recording = null;
        }
        return blueprint.finish(level);
    }

    public static boolean isRecording() {
        return recording != null;
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
        put(level, local(origin, front, w, d, 3, 1, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        put(level, local(origin, front, w, d, 3, 2, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // bed along the left wall, head towards the back
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, back);
        put(level, local(origin, front, w, d, 1, 1, 2), bed.setValue(BedBlock.PART, BedPart.FOOT));
        put(level, local(origin, front, w, d, 1, 1, 3), bed.setValue(BedBlock.PART, BedPart.HEAD));
        // workstation and a lantern hanging from the ceiling
        BlockState station = workstation.defaultBlockState();
        if (station.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            station = station.setValue(BlockStateProperties.HORIZONTAL_FACING, front);
        }
        set(level, local(origin, front, w, d, 5, 1, 3), station);
        put(level, local(origin, front, w, d, 3, 3, 2), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
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
                put(level, base.above(h), Blocks.AIR.defaultBlockState());
            }
        }
        set(level, base.above(1), p.fence());
        set(level, base.above(2), p.fence());
        put(level, base.above(3), Blocks.LANTERN.defaultBlockState());
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
                put(level, at.below(), center ? Blocks.WATER.defaultBlockState() : p.foundation());
                put(level, at, center ? Blocks.WATER.defaultBlockState() : Blocks.COBBLESTONE_WALL.defaultBlockState());
                if (u != 1 && v != 1) {
                    set(level, at.above(), p.fence());
                    set(level, at.above(2), p.fence());
                }
                put(level, at.above(3), p.slab().defaultBlockState());
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
                    put(level, at.above(), u == 1 ? Blocks.CRAFTING_TABLE.defaultBlockState()
                            : Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
                } else if (u != 1) {
                    set(level, at.above(), p.fence());
                }
                if (u != 1) {
                    set(level, at.above(2), p.fence());
                }
                put(level, at.above(3), canopy.defaultBlockState());
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
                    put(level, at, Blocks.WATER.defaultBlockState());
                    put(level, at.above(), Blocks.OAK_TRAPDOOR.defaultBlockState());
                    continue;
                }
                put(level, at, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
                BlockState plant = crop.defaultBlockState();
                if (crop instanceof CropBlock cropBlock) {
                    plant = cropBlock.getStateForAge(random.nextInt(cropBlock.getMaxAge()));
                }
                put(level, at.above(), plant);
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
        put(level, scarecrow, p.fence());
        put(level, scarecrow.above(), Blocks.HAY_BLOCK.defaultBlockState());
        put(level, scarecrow.above(2), Blocks.CARVED_PUMPKIN.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, gateSide));
        BlockPos composter = origin.offset(7, 1, 7);
        put(level, composter, Blocks.COMPOSTER.defaultBlockState());
    }

    /** Benches (stairs) around the village bell, turned towards it. Returns false if there was no room for any. */
    public static boolean benches(ServerLevel level, WorldMemory memory, BlockPos bell, Palette p) {
        int placed = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int along = -1; along <= 1; along += 2) {
                BlockPos column = bell.relative(side, 4).relative(side.getClockWise(), along);
                int y = groundY(level, column.getX(), column.getZ());
                BlockPos ground = new BlockPos(column.getX(), y, column.getZ());
                if (memory.isProtected(ground) || Math.abs(y - bell.getY()) > 3 || !isGround(level.getBlockState(ground))
                        || !isOpen(level.getBlockState(ground.above())) || !isOpen(level.getBlockState(ground.above(2)))) {
                    continue;
                }
                // a stair's back faces its FACING direction: away from the bell, so one sits looking at it
                put(level, ground.above(), stairs(p.stairs(), side, false));
                placed++;
            }
        }
        return placed > 0;
    }

    /** A 3 x 3 bed of mixed flowers on fresh grass. */
    public static boolean flowerBed(ServerLevel level, WorldMemory memory, int x, int z, RandomSource random) {
        int y = site(level, memory, x, z, 3, 3, 0, 2);
        if (y == FAIL) {
            return false;
        }
        Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY,
                Blocks.ALLIUM, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.PINK_TULIP, Blocks.LILY_OF_THE_VALLEY};
        for (int u = 0; u < 3; u++) {
            for (int v = 0; v < 3; v++) {
                BlockPos at = new BlockPos(x + u, y, z + v);
                put(level, at, Blocks.GRASS_BLOCK.defaultBlockState());
                put(level, at.above(), flowers[random.nextInt(flowers.length)].defaultBlockState());
            }
        }
        return true;
    }

    /**
     * A lookout tower (3 x 3, {@code min} is its corner): log posts, a ladder up the inside of one wall, a railed
     * platform and a roof with a lantern.
     */
    public static void watchtower(ServerLevel level, BlockPos min, int y, Palette p) {
        BlockPos origin = new BlockPos(min.getX(), y, min.getZ());
        levelGround(level, origin, 3, 3, y, p.foundation(), 12);
        int top = 7;
        for (int u = 0; u < 3; u++) {
            for (int v = 0; v < 3; v++) {
                put(level, origin.offset(u, 0, v), p.foundation());
            }
        }
        for (int h = 1; h < top; h++) {
            for (int u = 0; u < 3; u += 2) {
                for (int v = 0; v < 3; v += 2) {
                    put(level, origin.offset(u, h, v), p.log());
                }
            }
            put(level, origin.offset(1, h, 0), p.planks());
            put(level, origin.offset(1, h, 1), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        for (int u = 0; u < 3; u++) {
            for (int v = 0; v < 3; v++) {
                BlockPos floor = origin.offset(u, top, v);
                put(level, floor, u == 1 && v == 1
                        ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH) : p.planks());
                boolean corner = u != 1 && v != 1;
                if (u != 1 || v != 1) {
                    set(level, floor.above(), p.fence());
                }
                if (corner) {
                    set(level, floor.above(2), p.fence());
                }
                put(level, floor.above(3), p.slab().defaultBlockState());
            }
        }
        put(level, origin.offset(1, top, 0), p.planks());
        put(level, origin.offset(1, top + 2, 1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    /**
     * A windmill (5 x 5, {@code min} is its corner): a stone footing, a tower of planks on log corners with a door, a
     * pitched roof, and sails on the {@code front} wall that turn in the wind.
     */
    public static void windmill(ServerLevel level, BlockPos min, int y, Palette p, Direction front) {
        BlockPos origin = new BlockPos(min.getX(), y, min.getZ());
        levelGround(level, origin, 5, 5, y, p.foundation(), 14);
        for (int u = 0; u < 5; u++) {
            for (int v = 0; v < 5; v++) {
                put(level, origin.offset(u, 0, v), Blocks.COBBLESTONE.defaultBlockState());
            }
        }
        int height = 8;
        for (int h = 1; h <= height; h++) {
            for (int u = 1; u <= 3; u++) {
                for (int v = 1; v <= 3; v++) {
                    boolean wall = u == 1 || u == 3 || v == 1 || v == 3;
                    if (!wall) {
                        continue;
                    }
                    boolean corner = (u == 1 || u == 3) && (v == 1 || v == 3);
                    BlockState block = h <= 2 ? Blocks.COBBLESTONE.defaultBlockState() : corner ? p.log() : p.planks();
                    boolean window = h == 5 && !corner;
                    put(level, origin.offset(u, h, v), window ? Blocks.GLASS_PANE.defaultBlockState() : block);
                }
            }
        }
        // the door, on the side away from the sails
        Direction back = front.getOpposite();
        BlockPos center = origin.offset(2, 0, 2);
        BlockPos doorAt = center.relative(back);
        BlockState door = p.door().defaultBlockState().setValue(DoorBlock.FACING, back);
        put(level, doorAt.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        put(level, doorAt.above(2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        pathAt(level, doorAt.relative(back, 2).getX(), doorAt.relative(back, 2).getZ());
        // roof: a ring of stairs and a peak
        for (int u = 1; u <= 3; u++) {
            for (int v = 1; v <= 3; v++) {
                BlockPos at = origin.offset(u, height + 1, v);
                if (u == 2 && v == 2) {
                    put(level, at, p.planks());
                    put(level, at.above(), p.slab().defaultBlockState());
                    continue;
                }
                Direction facing = v == 1 ? Direction.SOUTH : v == 3 ? Direction.NORTH : u == 1 ? Direction.EAST : Direction.WEST;
                put(level, at, stairs(p.stairs(), facing, false));
            }
        }
        // the sails, on the front wall near the top
        put(level, center.relative(front, 2).above(height - 1),
                WorldBlocks.WINDMILL_SAILS.get().defaultBlockState().setValue(WindmillSailsBlock.FACING, front));
    }

    /**
     * The village storehouse (5 x 5, open at the {@code front}): log posts under a slab roof, barrels and chests
     * along the back, stacks of logs and a hay bale, where the workers bring what they gather.
     */
    public static void storehouse(ServerLevel level, BlockPos min, int y, Palette p, Direction front) {
        int w = 5;
        int d = 5;
        BlockPos origin = new BlockPos(min.getX(), y, min.getZ());
        levelGround(level, origin, w, d, y, p.foundation(), 6);
        for (int u = 0; u < w; u++) {
            for (int v = 0; v < d; v++) {
                put(level, local(origin, front, w, d, u, 0, v), p.foundation());
                boolean corner = (u == 0 || u == w - 1) && (v == 0 || v == d - 1);
                for (int h = 1; h <= 3; h++) {
                    BlockPos at = local(origin, front, w, d, u, h, v);
                    if (corner) {
                        put(level, at, p.log());
                    } else if (v == d - 1 || (u == 0 || u == w - 1) && h == 3) {
                        put(level, at, p.planks());
                    }
                }
                put(level, local(origin, front, w, d, u, 4, v), p.slab().defaultBlockState());
            }
        }
        for (int u = 1; u < w - 1; u++) {
            put(level, local(origin, front, w, d, u, 1, d - 2), u == 2 ? Blocks.CHEST.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, front) : Blocks.BARREL.defaultBlockState()
                    .setValue(BlockStateProperties.FACING, Direction.UP));
        }
        BlockState pile = p.log().hasProperty(BlockStateProperties.AXIS)
                ? p.log().setValue(BlockStateProperties.AXIS, front.getClockWise().getAxis()) : p.log();
        put(level, local(origin, front, w, d, 1, 1, 1), pile);
        put(level, local(origin, front, w, d, 1, 2, 1), pile);
        put(level, local(origin, front, w, d, 1, 1, 2), pile);
        put(level, local(origin, front, w, d, 3, 1, 1), Blocks.HAY_BLOCK.defaultBlockState());
        put(level, local(origin, front, w, d, 3, 1, 2), Blocks.COBBLESTONE.defaultBlockState());
    }

    /**
     * A smithy (5 x 7): a stone workshop open to the {@code front}, with a forge and its chimney, an anvil, a
     * smithing table and a grindstone.
     */
    public static void smithy(ServerLevel level, BlockPos min, int y, Palette p, Direction front) {
        int w = 5;
        int d = 7;
        BlockPos origin = new BlockPos(min.getX(), y, min.getZ());
        levelGround(level, origin, w, d, y, Blocks.COBBLESTONE.defaultBlockState(), 7);
        for (int u = 0; u < w; u++) {
            for (int v = 0; v < d; v++) {
                put(level, local(origin, front, w, d, u, 0, v), Blocks.COBBLESTONE.defaultBlockState());
                boolean edgeU = u == 0 || u == w - 1;
                boolean back = v >= 3;
                for (int h = 1; h <= 3; h++) {
                    BlockPos at = local(origin, front, w, d, u, h, v);
                    if (edgeU && (back || v == 0)) {
                        put(level, at, h == 2 && back && v == 4 ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                    } else if (v == d - 1) {
                        put(level, at, Blocks.COBBLESTONE.defaultBlockState());
                    } else if (edgeU) {
                        put(level, at, h == 3 ? p.log() : Blocks.AIR.defaultBlockState());
                    }
                }
                put(level, local(origin, front, w, d, u, 4, v), v < 3 ? p.slab().defaultBlockState() : Blocks.STONE_BRICK_SLAB.defaultBlockState());
            }
        }
        put(level, local(origin, front, w, d, 2, 1, d - 2), Blocks.BLAST_FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, front));
        put(level, local(origin, front, w, d, 1, 1, d - 2), Blocks.FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, front));
        put(level, local(origin, front, w, d, 3, 1, d - 2), Blocks.LAVA_CAULDRON.defaultBlockState());
        put(level, local(origin, front, w, d, 1, 1, 3), Blocks.ANVIL.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, front.getClockWise()));
        put(level, local(origin, front, w, d, 3, 1, 3), Blocks.SMITHING_TABLE.defaultBlockState());
        put(level, local(origin, front, w, d, 3, 1, 1), Blocks.GRINDSTONE.defaultBlockState());
        for (int h = 5; h <= 6; h++) {
            put(level, local(origin, front, w, d, 2, h, d - 2), Blocks.COBBLESTONE.defaultBlockState());
        }
        put(level, local(origin, front, w, d, 2, 7, d - 2), Blocks.CAMPFIRE.defaultBlockState());
    }

    /**
     * A chapel (5 x 9) with its door to the {@code front}: stone footing, plank walls with tall windows, pews and a
     * lectern, and a bell tower at the back with a lantern in it.
     */
    public static void chapel(ServerLevel level, BlockPos min, int y, Palette p, Direction front) {
        int w = 5;
        int d = 9;
        BlockPos origin = new BlockPos(min.getX(), y, min.getZ());
        levelGround(level, origin, w, d, y, Blocks.STONE_BRICKS.defaultBlockState(), 12);
        Direction back = front.getOpposite();
        for (int u = 0; u < w; u++) {
            for (int v = 0; v < d; v++) {
                put(level, local(origin, front, w, d, u, 0, v), Blocks.STONE_BRICKS.defaultBlockState());
                boolean edgeU = u == 0 || u == w - 1;
                boolean edgeV = v == 0 || v == d - 1;
                int height = v >= d - 3 ? 9 : 4;
                for (int h = 1; h <= height; h++) {
                    BlockPos at = local(origin, front, w, d, u, h, v);
                    boolean tower = v >= d - 3;
                    boolean wall = tower ? u == 0 || u == w - 1 || v == d - 3 || v == d - 1 : edgeU || edgeV;
                    if (!wall) {
                        continue;
                    }
                    boolean corner = (edgeU || u == 1 && tower || u == 3 && tower) && (edgeV || v == d - 3);
                    boolean window = !corner && (h == 2 || h == 3) && edgeU && v % 2 == 1 && !tower
                            || tower && h == 7 && (u == 2 || v == d - 2);
                    BlockState block = h == 1 ? Blocks.STONE_BRICKS.defaultBlockState() : corner ? p.log() : p.planks();
                    put(level, at, window ? Blocks.GLASS_PANE.defaultBlockState() : block);
                }
                if (v < d - 3) {
                    put(level, local(origin, front, w, d, u, 5, v), u == 2 ? p.planks() : stairs(p.stairs(), u < 2 ? front.getClockWise() : front.getCounterClockWise(), false));
                } else {
                    put(level, local(origin, front, w, d, u, 10, v), u == 2 && v == d - 2 ? p.planks() : p.slab().defaultBlockState());
                }
            }
        }
        put(level, local(origin, front, w, d, 2, 11, d - 2), Blocks.LANTERN.defaultBlockState());
        BlockState door = p.door().defaultBlockState().setValue(DoorBlock.FACING, back);
        put(level, local(origin, front, w, d, 2, 1, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        put(level, local(origin, front, w, d, 2, 2, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        for (int v = 2; v <= 4; v += 2) {
            put(level, local(origin, front, w, d, 1, 1, v), stairs(p.stairs(), back, false));
            put(level, local(origin, front, w, d, 3, 1, v), stairs(p.stairs(), back, false));
        }
        put(level, local(origin, front, w, d, 2, 1, d - 4), Blocks.LECTERN.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, back));
        put(level, local(origin, front, w, d, 2, 3, d - 4), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        BlockPos step = local(origin, front, w, d, 2, 0, 0).relative(front);
        pathAt(level, step.getX(), step.getZ());
    }

    /**
     * A milestone by the road: a signpost naming the village the road leads to and how far it is. Returns false if
     * there was no room.
     */
    public static boolean milestone(ServerLevel level, WorldMemory memory, int x, int z, float towards,
                                    Component destination, int distance) {
        int y = site(level, memory, x, z, 1, 1, 0, 3);
        if (y == FAIL) {
            return false;
        }
        BlockPos post = new BlockPos(x, y + 1, z);
        put(level, post, Blocks.SPRUCE_FENCE.defaultBlockState());
        int rotation = Math.floorMod(Math.round(towards / 22.5F), 16);
        BlockPos signPos = post.above();
        put(level, signPos, Blocks.SPRUCE_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));
        if (!isRecording() && level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText text = sign.getFrontText()
                    .setMessage(1, destination.copy().withStyle(ChatFormatting.BOLD))
                    .setMessage(2, Component.translatable("sign.dunesrelics.milestone.distance", distance));
            sign.setText(text, true);
            sign.setWaxed(true);
        }
        return true;
    }

    /** A bell standing on a stone post, which makes a group of houses a village. */
    public static void bell(ServerLevel level, BlockPos ground) {
        put(level, ground.above(), Blocks.COBBLESTONE.defaultBlockState());
        put(level, ground.above(2), Blocks.BELL.defaultBlockState());
    }

    // ------------------------------------------------------------------------------------------ paths

    /**
     * Turns the grass (or dirt) at the top of a column into a path block, or sand into smooth sandstone as in desert
     * villages; returns false over water or rock.
     */
    public static boolean pathAt(ServerLevel level, int x, int z) {
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
        BlockState top = level.getBlockState(pos);
        if (!top.getFluidState().isEmpty() || !level.isLoaded(pos)) {
            return false;
        }
        if (top.canBeReplaced() || top.is(BlockTags.FLOWERS) || top.is(BlockTags.SAPLINGS)) {
            put(level, pos, Blocks.AIR.defaultBlockState());
            pos = pos.below();
            top = level.getBlockState(pos);
        }
        if (top.is(Blocks.GRASS_BLOCK) || top.is(Blocks.DIRT) || top.is(Blocks.COARSE_DIRT) || top.is(Blocks.PODZOL)
                || top.is(Blocks.MYCELIUM) || top.is(Blocks.ROOTED_DIRT)) {
            put(level, pos, Blocks.DIRT_PATH.defaultBlockState());
            return true;
        }
        if (top.is(Blocks.SAND) || top.is(Blocks.RED_SAND)) {
            put(level, pos, (top.is(Blocks.SAND) ? Blocks.SMOOTH_SANDSTONE : Blocks.SMOOTH_RED_SANDSTONE).defaultBlockState());
            return true;
        }
        return top.is(Blocks.DIRT_PATH) || top.is(Blocks.SMOOTH_SANDSTONE) || top.is(Blocks.SMOOTH_RED_SANDSTONE);
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
