package com.dunesrelics.worldgen.structure;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.entity.CannonBlockEntity;
import com.dunesrelics.block.world.CannonBlock;
import com.dunesrelics.entity.world.PirateSloop;
import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.ModStructures;
import com.dunesrelics.registry.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A three-masted pirate ship riding at anchor: hull and hold, the captain's cabin under the quarterdeck, sails,
 * a crow's nest, four crewed cannons and a crew. Built in local coordinates: u runs from the stern (0) to the bow
 * (28), v across the beam (-4 port .. 4 starboard), y from the deck (0).
 */
public class PirateShipPiece extends StructurePiece {
    public static final ResourceLocation HOLD_LOOT = DunesRelics.id("chests/pirate_hold");
    public static final ResourceLocation CAPTAIN_LOOT = DunesRelics.id("chests/pirate_captain");
    private static final int LENGTH = 29;
    private static final int FLAGS = 2;

    private final BlockPos origin;
    private final Direction bow;
    private final Direction starboard;
    private final int deckY;

    public PirateShipPiece(BlockPos center, Direction bow, int seaLevel) {
        super(ModStructures.PIRATE_SHIP_PIECE.get(), 0, box(center, bow, seaLevel));
        this.bow = bow;
        this.starboard = bow.getClockWise();
        this.deckY = seaLevel + 2;
        this.origin = center.relative(bow, -LENGTH / 2).atY(this.deckY);
    }

    public PirateShipPiece(CompoundTag tag) {
        super(ModStructures.PIRATE_SHIP_PIECE.get(), tag);
        this.bow = Direction.from2DDataValue(tag.getInt("Bow"));
        this.starboard = this.bow.getClockWise();
        this.deckY = tag.getInt("DeckY");
        this.origin = new BlockPos(tag.getInt("OX"), this.deckY, tag.getInt("OZ"));
    }

    private static BoundingBox box(BlockPos center, Direction bow, int seaLevel) {
        int reach = LENGTH / 2 + 4;
        return new BoundingBox(center.getX() - reach, seaLevel - 5, center.getZ() - reach,
                center.getX() + reach, seaLevel + 22, center.getZ() + reach);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Bow", this.bow.get2DDataValue());
        tag.putInt("DeckY", this.deckY);
        tag.putInt("OX", this.origin.getX());
        tag.putInt("OZ", this.origin.getZ());
    }

    // ------------------------------------------------------------------------------------------ geometry

    private BlockPos at(int u, int v, int y) {
        return this.origin.relative(this.bow, u).relative(this.starboard, v).above(y);
    }

    private static int halfWidth(int u) {
        if (u >= 28) {
            return 0;
        }
        if (u >= 26) {
            return 1;
        }
        if (u >= 24) {
            return 2;
        }
        if (u >= 21 || u == 0) {
            return 3;
        }
        return 4;
    }

    private static int depth(int u) {
        return u <= 1 ? 4 : u <= 22 ? 5 : u <= 25 ? 4 : 3;
    }

    // ------------------------------------------------------------------------------------------ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, net.minecraft.world.level.ChunkPos chunkPos, BlockPos pivot) {
        List<BlockPos> smooth = new ArrayList<>();
        BlockState hull = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState deck = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState rail = Blocks.DARK_OAK_FENCE.defaultBlockState();
        for (int u = 0; u < LENGTH; u++) {
            int w = halfWidth(u);
            int d = depth(u);
            for (int v = -w; v <= w; v++) {
                for (int y = -d; y <= 0; y++) {
                    int hw = w - (y == -d ? 2 : y == -d + 1 ? 1 : 0);
                    if (Math.abs(v) > Math.max(0, hw)) {
                        continue;
                    }
                    boolean shell = Math.abs(v) == Math.max(0, hw) || y == -d || u == 0;
                    BlockState state = y == 0 ? (shell ? hull : deck) : shell ? hull : Blocks.AIR.defaultBlockState();
                    this.put(level, chunkBox, at(u, v, y), state);
                }
                // clear the air above the deck and set the railing
                for (int y = 1; y <= 3; y++) {
                    if (u > 6) {
                        this.put(level, chunkBox, at(u, v, y), Blocks.AIR.defaultBlockState());
                    }
                }
                if (u > 6 && Math.abs(v) == w) {
                    this.put(level, chunkBox, at(u, v, 1), rail);
                    smooth.add(at(u, v, 1));
                }
            }
        }
        // hatch to the hold
        this.put(level, chunkBox, at(15, 0, 0), Blocks.AIR.defaultBlockState());
        this.put(level, chunkBox, at(14, 0, 0), Blocks.AIR.defaultBlockState());

        this.buildQuarterdeck(level, chunkBox, smooth, hull, deck, rail);
        this.buildMast(level, chunkBox, smooth, 13, 16, new int[]{7, 12}, 8, 11, 13, 15);
        this.buildMast(level, chunkBox, smooth, 19, 13, new int[]{6, 10}, 7, 9, 11, 12);
        this.buildCrowsNest(level, chunkBox, smooth);
        // bowsprit
        BlockState sprit = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, this.bow.getAxis());
        for (int u = 28; u <= 32; u++) {
            this.put(level, chunkBox, at(u, 0, u <= 29 ? 1 : 2), sprit);
        }
        // flag at the top of the main mast
        for (int y = 16; y <= 17; y++) {
            this.put(level, chunkBox, at(12, 0, y), Blocks.BLACK_WOOL.defaultBlockState());
            this.put(level, chunkBox, at(11, 0, y), y == 17 ? Blocks.WHITE_WOOL.defaultBlockState() : Blocks.BLACK_WOOL.defaultBlockState());
        }
        // cannons on both sides
        for (int u : new int[]{10, 16}) {
            for (int side : new int[]{-1, 1}) {
                BlockPos pos = at(u, side * halfWidth(u), 1);
                Direction out = side > 0 ? this.starboard : this.starboard.getOpposite();
                if (chunkBox.isInside(pos)) {
                    level.setBlock(pos, WorldBlocks.CANNON.get().defaultBlockState().setValue(CannonBlock.FACING, out), FLAGS);
                    if (level.getBlockEntity(pos) instanceof CannonBlockEntity cannon) {
                        cannon.setCrewed(true);
                    }
                }
            }
        }
        // hold: loot and supplies
        this.chest(level, chunkBox, random, at(9, -2, -1), this.starboard, HOLD_LOOT);
        this.chest(level, chunkBox, random, at(18, 2, -1), this.starboard.getOpposite(), HOLD_LOOT);
        for (int u = 10; u <= 12; u++) {
            this.put(level, chunkBox, at(u, 3, -1), Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        }
        this.put(level, chunkBox, at(16, -3, -1), Blocks.HAY_BLOCK.defaultBlockState());
        this.put(level, chunkBox, at(13, 1, 1), Blocks.LANTERN.defaultBlockState());
        this.put(level, chunkBox, at(19, -1, 1), Blocks.LANTERN.defaultBlockState());

        for (BlockPos pos : smooth) {
            if (chunkBox.isInside(pos)) {
                BlockState state = level.getBlockState(pos);
                level.setBlock(pos, Block.updateFromNeighbourShapes(state, level, pos), FLAGS);
            }
        }
        this.spawnCrew(level, chunkBox);
        this.spawnSloop(level, chunkBox);
    }

    /** The raised stern with the captain's cabin inside and a ladder up to it. */
    private void buildQuarterdeck(WorldGenLevel level, BoundingBox box, List<BlockPos> smooth, BlockState hull,
                                  BlockState deck, BlockState rail) {
        for (int u = 0; u <= 6; u++) {
            int w = halfWidth(u);
            for (int v = -w; v <= w; v++) {
                boolean wall = Math.abs(v) == w || u == 0 || u == 6;
                for (int y = 1; y <= 3; y++) {
                    BlockState state = Blocks.AIR.defaultBlockState();
                    if (wall) {
                        boolean window = y == 2 && (Math.abs(v) == w && (u == 2 || u == 4) || u == 0 && Math.abs(v) == 2);
                        state = window ? Blocks.GLASS_PANE.defaultBlockState() : hull;
                        if (window) {
                            smooth.add(at(u, v, y));
                        }
                    }
                    this.put(level, box, at(u, v, y), state);
                }
                this.put(level, box, at(u, v, 4), deck);
                if (Math.abs(v) == w || u == 0 || u == 6 && v != 3) {
                    this.put(level, box, at(u, v, 5), rail);
                    smooth.add(at(u, v, 5));
                } else {
                    this.put(level, box, at(u, v, 5), Blocks.AIR.defaultBlockState());
                }
            }
        }
        // cabin door facing the main deck
        BlockState door = Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, this.bow.getOpposite());
        this.put(level, box, at(6, 0, 1), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.put(level, box, at(6, 0, 2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // ladder up to the quarterdeck
        for (int y = 1; y <= 4; y++) {
            this.put(level, box, at(7, 3, y), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, this.bow));
        }
        // the captain's cabin
        this.chest(level, box, null, at(1, 0, 1), this.bow, CAPTAIN_LOOT);
        this.put(level, box, at(1, -2, 1), Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        this.put(level, box, at(1, 2, 1), Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        for (int u = 2; u <= 5; u++) {
            this.put(level, box, at(u, 0, 1), Blocks.RED_CARPET.defaultBlockState());
        }
        this.put(level, box, at(3, 0, 3), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // mizzen mast on the quarterdeck
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        for (int y = 5; y <= 12; y++) {
            this.put(level, box, at(3, 0, y), log);
        }
        BlockState yard = log.setValue(RotatedPillarBlock.AXIS, this.starboard.getAxis());
        for (int v = -2; v <= 2; v++) {
            this.put(level, box, at(3, v, 9), yard);
            for (int y = 6; y <= 8; y++) {
                this.put(level, box, at(4, v, y), Blocks.WHITE_WOOL.defaultBlockState());
            }
        }
    }

    private void buildMast(WorldGenLevel level, BoundingBox box, List<BlockPos> smooth, int u, int height, int[] yards,
                           int sailFrom, int sailTo, int topFrom, int topTo) {
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        for (int y = 1; y <= height; y++) {
            this.put(level, box, at(u, 0, y), log);
        }
        BlockState yard = log.setValue(RotatedPillarBlock.AXIS, this.starboard.getAxis());
        for (int y : yards) {
            for (int v = -4; v <= 4; v++) {
                if (v != 0) {
                    this.put(level, box, at(u, v, y), yard);
                }
            }
        }
        for (int y = sailFrom; y <= sailTo; y++) {
            for (int v = -3; v <= 3; v++) {
                this.put(level, box, at(u + 1, v, y), Blocks.WHITE_WOOL.defaultBlockState());
            }
        }
        for (int y = topFrom; y <= topTo; y++) {
            for (int v = -2; v <= 2; v++) {
                this.put(level, box, at(u + 1, v, y), Blocks.WHITE_WOOL.defaultBlockState());
            }
        }
    }

    private void buildCrowsNest(WorldGenLevel level, BoundingBox box, List<BlockPos> smooth) {
        for (int du = -1; du <= 1; du++) {
            for (int dv = -1; dv <= 1; dv++) {
                if (du == 0 && dv == 0) {
                    continue;
                }
                this.put(level, box, at(13 + du, dv, 13), Blocks.DARK_OAK_SLAB.defaultBlockState());
                this.put(level, box, at(13 + du, dv, 14), Blocks.DARK_OAK_FENCE.defaultBlockState());
                smooth.add(at(13 + du, dv, 14));
            }
        }
    }

    private void put(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, FLAGS);
        }
    }

    private void chest(WorldGenLevel level, BoundingBox box, RandomSource random, BlockPos pos, Direction facing,
                       ResourceLocation loot) {
        if (box.isInside(pos)) {
            level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), FLAGS);
            RandomizableContainerBlockEntity.setLootTable(level, random != null ? random : level.getRandom(), pos, loot);
        }
    }

    // ------------------------------------------------------------------------------------------ crew

    private void spawnCrew(WorldGenLevel level, BoundingBox box) {
        this.spawn(level, box, ModEntities.PIRATE.get(), at(8, -2, 1));
        this.spawn(level, box, ModEntities.PIRATE.get(), at(12, 2, 1));
        this.spawn(level, box, ModEntities.PIRATE.get(), at(17, -1, 1));
        this.spawn(level, box, ModEntities.PIRATE.get(), at(22, 1, 1));
        this.spawn(level, box, ModEntities.PIRATE_GUNNER.get(), at(2, 2, 5));
        this.spawn(level, box, ModEntities.PIRATE_GUNNER.get(), at(2, -2, 5));
        this.spawn(level, box, ModEntities.PIRATE_GUNNER.get(), at(24, 0, 1));
        this.spawn(level, box, ModEntities.PIRATE_CAPTAIN.get(), at(3, 1, 1));
    }

    /** A sloop rides at anchor off the ship's starboard side, with a boat crew aboard. */
    private void spawnSloop(WorldGenLevel level, BoundingBox box) {
        BlockPos pos = this.at(LENGTH / 2, 9, -2);
        if (!box.isInside(pos) || !level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            return;
        }
        PirateSloop sloop = ModEntities.PIRATE_SLOOP.get().create(level.getLevel());
        if (sloop == null) {
            return;
        }
        sloop.moveTo(pos.getX() + 0.5D, pos.getY() - 0.4D, pos.getZ() + 0.5D, this.bow.toYRot(), 0.0F);
        sloop.setHome(pos);
        for (int i = 0; i < 3; i++) {
            EntityType<? extends Mob> type = i == 2 ? ModEntities.PIRATE_GUNNER.get() : ModEntities.PIRATE.get();
            Mob pirate = type.create(level.getLevel());
            if (pirate != null) {
                pirate.moveTo(sloop.getX(), sloop.getY(), sloop.getZ(), 0.0F, 0.0F);
                pirate.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
                pirate.setPersistenceRequired();
                pirate.startRiding(sloop, true);
            }
        }
        level.addFreshEntityWithPassengers(sloop);
    }

    private void spawn(WorldGenLevel level, BoundingBox box, EntityType<? extends Mob> type, BlockPos pos) {
        if (!box.isInside(pos)) {
            return;
        }
        Mob mob = type.create(level.getLevel());
        if (mob == null) {
            return;
        }
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
