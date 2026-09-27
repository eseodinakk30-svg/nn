package com.dunesrelics.worldgen.structure;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.volcanic.HeartOfTheVolcanoBlock;
import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.ModStructures;
import com.dunesrelics.registry.VolcanicBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The whole volcano as a single structure piece. Its shape is a pure function of the piece's seed and the
 * block coordinates, so every chunk can build its own slice independently and the slices line up perfectly.
 */
public class VolcanoPiece extends StructurePiece {
    public static final ResourceLocation LAIR_LOOT = DunesRelics.id("chests/volcano_lair");
    private static final int FLAGS = 2;

    private final int seed;
    private final int cx;
    private final int cz;
    private final int baseY;
    private final int seaLevel;

    // Derived shape parameters.
    private final int radius;
    private final int peakY;
    private final int craterRadius;
    private final int lavaY;
    private final int chamberY;
    private final Direction tunnel;

    public VolcanoPiece(int seed, BlockPos center, int seaLevel) {
        super(ModStructures.VOLCANO_PIECE.get(), 0, box(seed, center, seaLevel));
        this.seed = seed;
        this.cx = center.getX();
        this.cz = center.getZ();
        this.baseY = center.getY();
        this.seaLevel = seaLevel;
        this.radius = radius(seed);
        this.peakY = peak(seed, center.getY(), seaLevel);
        this.craterRadius = 6 + Math.floorMod(seed >> 8, 3);
        this.lavaY = this.peakY - 7;
        this.chamberY = chamberY(center.getY(), seaLevel);
        this.tunnel = Direction.from2DDataValue(Math.floorMod(seed >> 12, 4));
    }

    public VolcanoPiece(CompoundTag tag) {
        super(ModStructures.VOLCANO_PIECE.get(), tag);
        this.seed = tag.getInt("Seed");
        this.cx = tag.getInt("CX");
        this.cz = tag.getInt("CZ");
        this.baseY = tag.getInt("BaseY");
        this.seaLevel = tag.getInt("SeaLevel");
        this.radius = radius(this.seed);
        this.peakY = peak(this.seed, this.baseY, this.seaLevel);
        this.craterRadius = 6 + Math.floorMod(this.seed >> 8, 3);
        this.lavaY = this.peakY - 7;
        this.chamberY = chamberY(this.baseY, this.seaLevel);
        this.tunnel = Direction.from2DDataValue(Math.floorMod(this.seed >> 12, 4));
    }

    private static int radius(int seed) {
        return 34 + Math.floorMod(seed, 12);
    }

    /** The magma chamber sits just above sea level, or just under the ground when the volcano stands on high land. */
    private static int chamberY(int baseY, int seaLevel) {
        return Math.max(seaLevel + 9, baseY - 2);
    }

    private static int peak(int seed, int baseY, int seaLevel) {
        return Math.max(seaLevel + 42, baseY + 32) + Math.floorMod(seed >> 4, 12);
    }

    private static BoundingBox box(int seed, BlockPos center, int seaLevel) {
        int reach = (int) (radius(seed) * 1.25F) + 2;
        return new BoundingBox(center.getX() - reach, Math.min(center.getY(), seaLevel) - 24, center.getZ() - reach,
                center.getX() + reach, peak(seed, center.getY(), seaLevel) + 4, center.getZ() + reach);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Seed", this.seed);
        tag.putInt("CX", this.cx);
        tag.putInt("CZ", this.cz);
        tag.putInt("BaseY", this.baseY);
        tag.putInt("SeaLevel", this.seaLevel);
    }

    // --------------------------------------------------------------------------------------------- noise helpers

    /** Stable pseudo-random value in [0, 1) for a position. */
    private double hash(int x, int y, int z, int salt) {
        long h = this.seed * 0x9E3779B97F4A7C15L + x * 0x632BE59BD9B4E019L + y * 0x85157AF5L + z * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L;
        h ^= (h >>> 29);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 32);
        return (h & 0xFFFFFFL) / (double) 0x1000000L;
    }

    /** How far the flank reaches in this direction: a bumpy, irregular circle. */
    private double reach(double angle) {
        double a = this.seed * 0.37D;
        double b = this.seed * 0.71D;
        return this.radius * (1.0D + 0.12D * Math.sin(3.0D * angle + a) + 0.07D * Math.sin(7.0D * angle + b)
                + 0.04D * Math.sin(13.0D * angle + a * 2.0D));
    }

    /** Rivers of cooled lava running down the flanks. */
    private boolean lavaRiver(double angle) {
        return Math.sin(9.0D * angle + this.seed * 0.5D) > 0.93D || Math.sin(5.0D * angle + this.seed * 1.3D) > 0.97D;
    }

    // --------------------------------------------------------------------------------------------- building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunkBox.minX(); x <= chunkBox.maxX(); x++) {
            for (int z = chunkBox.minZ(); z <= chunkBox.maxZ(); z++) {
                this.buildColumn(level, chunkBox, pos, x, z);
            }
        }
        this.decorateChamber(level, chunkBox, random);
    }

    private void buildColumn(WorldGenLevel level, BoundingBox chunkBox, BlockPos.MutableBlockPos pos, int x, int z) {
        double dx = x - this.cx;
        double dz = z - this.cz;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double angle = Math.atan2(dz, dx);
        double edge = this.reach(angle);
        if (distance > edge) {
            return;
        }
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
        double slope = Mth.clamp((distance - this.craterRadius) / (edge - this.craterRadius), 0.0D, 1.0D);
        int top;
        if (distance < this.craterRadius) {
            top = this.lavaY - 2 - (int) ((this.craterRadius - distance) * 0.4D);
        } else {
            top = (int) Math.round(floor + (this.peakY - floor) * Math.pow(1.0D - slope, 1.7D));
        }
        if (top < floor) {
            return;
        }
        boolean river = distance > this.craterRadius + 1 && slope > 0.12D && this.lavaRiver(angle);
        // Reach below the ground as far as the chamber, so it is carved out whole even under high land.
        int minY = Math.max(chunkBox.minY(), Math.min(floor - 3, this.chamberY - 8));
        int maxY = Math.min(chunkBox.maxY(), Math.max(top, this.lavaY) + 1);
        for (int y = minY; y <= maxY; y++) {
            pos.set(x, y, z);
            BlockState state = this.blockAt(x, y, z, distance, slope, top, floor, river);
            if (state != null) {
                level.setBlock(pos, state, FLAGS);
            }
        }
        // Surface dressing above the shell.
        if (top + 1 <= chunkBox.maxY() && top + 1 > this.seaLevel && distance >= this.craterRadius) {
            pos.set(x, top + 1, z);
            double roll = this.hash(x, top, z, 3);
            if (slope < 0.35D && roll < 0.45D && !river) {
                level.setBlock(pos, VolcanicBlocks.ASH_LAYER.get().defaultBlockState()
                        .setValue(SnowLayerBlock.LAYERS, 1 + (int) (roll * 6)), FLAGS);
            } else if (slope < 0.2D && roll > 0.92D) {
                level.setBlock(pos, VolcanicBlocks.SULFUR_CLUSTER.get().defaultBlockState(), FLAGS);
            } else if (slope > 0.55D && roll > 0.97D) {
                level.setBlock(pos, VolcanicBlocks.ASH_GRASS.get().defaultBlockState(), FLAGS);
            }
        }
        // Lava lilies drifting on the crater lake.
        if (distance < this.craterRadius - 1 && this.lavaY + 1 <= chunkBox.maxY() && this.hash(x, 0, z, 9) < 0.06D) {
            pos.set(x, this.lavaY + 1, z);
            level.setBlock(pos, VolcanicBlocks.LAVA_LILY.get().defaultBlockState(), FLAGS);
        }
    }

    /** The block of the volcano at a position, or null to leave the world untouched. */
    private BlockState blockAt(int x, int y, int z, double distance, double slope, int top, int ground, boolean river) {
        // Magma chamber and the tunnel into it.
        double chamber = this.chamberShape(x, y, z);
        if (chamber < 1.0D) {
            return this.chamberBlock(x, y, z, chamber);
        }
        if (this.inTunnel(x, y, z, top, ground)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (this.tunnelLining(x, y, z, top, ground)) {
            return this.hash(x, y, z, 5) < 0.2D ? VolcanicBlocks.CRACKED_SCORIA_BRICKS.get().defaultBlockState()
                    : VolcanicBlocks.SCORIA_BRICKS.get().defaultBlockState();
        }
        // Vent between the chamber and the crater.
        if (distance < 2.2D && y > this.chamberY + 5 && y <= top) {
            return this.hash(x, y, z, 6) < 0.7D ? Blocks.MAGMA_BLOCK.defaultBlockState()
                    : VolcanicBlocks.MOLTEN_SCORIA.get().defaultBlockState();
        }
        if (y > top) {
            if (distance < this.craterRadius && y <= this.lavaY) {
                return Blocks.LAVA.defaultBlockState();
            }
            return null;
        }
        if (chamber < 1.25D) {
            // The walls of the chamber glow with molten rock.
            return this.hash(x, y, z, 7) < 0.3D ? VolcanicBlocks.MOLTEN_SCORIA.get().defaultBlockState()
                    : Blocks.BASALT.defaultBlockState();
        }
        if (y < ground - 3) {
            // Deep underground only the chamber, its tunnel and the vent are carved; the rest stays as it was.
            return null;
        }
        int depth = top - y;
        double roll = this.hash(x, y, z, 1);
        if (depth == 0) {
            if (river) {
                return roll < 0.55D ? VolcanicBlocks.MOLTEN_SCORIA.get().defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
            }
            if (Math.abs(y - this.seaLevel) <= 2 && slope > 0.7D) {
                return VolcanicBlocks.BLACK_SAND.get().defaultBlockState();
            }
            if (y > this.peakY - 14) {
                return roll < 0.6D ? VolcanicBlocks.ASH_BLOCK.get().defaultBlockState() : VolcanicBlocks.SCORIA.get().defaultBlockState();
            }
            if (roll < 0.004D && y > this.seaLevel) {
                return VolcanicBlocks.STEAM_VENT.get().defaultBlockState();
            }
            return roll < 0.55D ? VolcanicBlocks.SCORIA.get().defaultBlockState()
                    : roll < 0.85D ? Blocks.BASALT.defaultBlockState() : Blocks.TUFF.defaultBlockState();
        }
        if (depth <= 2 && river) {
            return Blocks.OBSIDIAN.defaultBlockState();
        }
        if (roll < 0.004D) {
            return VolcanicBlocks.FIRE_OPAL_ORE.get().defaultBlockState();
        }
        if (roll < 0.014D) {
            return VolcanicBlocks.SULFUR_ORE.get().defaultBlockState();
        }
        if (roll < 0.02D) {
            return VolcanicBlocks.PUMICE.get().defaultBlockState();
        }
        return depth <= 3 ? VolcanicBlocks.SCORIA.get().defaultBlockState()
                : roll < 0.6D ? Blocks.BASALT.defaultBlockState() : VolcanicBlocks.SCORIA.get().defaultBlockState();
    }

    /** Normalised distance from the centre of the ellipsoidal magma chamber (inside when < 1). */
    private double chamberShape(int x, int y, int z) {
        double dx = (x - this.cx) / 11.0D;
        double dy = (y - this.chamberY) / 6.0D;
        double dz = (z - this.cz) / 11.0D;
        return dx * dx + dy * dy + dz * dz;
    }

    private BlockState chamberBlock(int x, int y, int z, double chamber) {
        double dx = (x - this.cx) / 11.0D;
        double dz = (z - this.cz) / 11.0D;
        double flat = Math.sqrt(dx * dx + dz * dz);
        int floorY = this.chamberY - 4;
        if (y < floorY) {
            return Blocks.BASALT.defaultBlockState();
        }
        if (y == floorY) {
            if (this.along(x, z) > 0 && this.across(x, z) <= 1) {
                // A bridge over the lava moat, leading in from the tunnel.
                return VolcanicBlocks.OBSIDIAN_BRICKS.get().defaultBlockState();
            }
            if (flat < 0.45D) {
                // The forge-keepers' temple floor.
                return (Math.abs(x - this.cx) + Math.abs(z - this.cz)) % 3 == 0 ? VolcanicBlocks.CHISELED_OBSIDIAN.get().defaultBlockState()
                        : VolcanicBlocks.OBSIDIAN_BRICKS.get().defaultBlockState();
            }
            if (flat < 0.7D) {
                return Blocks.LAVA.defaultBlockState();
            }
            return Blocks.BASALT.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private int along(int x, int z) {
        return (x - this.cx) * this.tunnel.getStepX() + (z - this.cz) * this.tunnel.getStepZ();
    }

    private int across(int x, int z) {
        return Math.abs((x - this.cx) * this.tunnel.getStepZ() - (z - this.cz) * this.tunnel.getStepX());
    }

    /**
     * The tunnel leaves the chamber level, then climbs gently towards the flank until it reaches the height of the
     * surrounding ground, so it always opens to the outside.
     */
    private int tunnelFloor(int x, int z, int ground) {
        int start = this.chamberY - 3;
        int climbing = start + Math.max(0, this.along(x, z) - 10) * 2 / 3;
        return Math.min(climbing, Math.max(start, ground + 1));
    }

    private boolean inTunnel(int x, int y, int z, int top, int ground) {
        int floorY = this.tunnelFloor(x, z, ground);
        return this.along(x, z) >= 6 && this.across(x, z) <= 1 && y >= floorY && y <= floorY + 3 && y <= top;
    }

    private boolean tunnelLining(int x, int y, int z, int top, int ground) {
        int floorY = this.tunnelFloor(x, z, ground);
        int across = this.across(x, z);
        return this.along(x, z) >= 8 && across <= 2 && y >= floorY - 1 && y <= floorY + 4 && y <= top
                && (across == 2 || y == floorY - 1 || y == floorY + 4);
    }

    /** The heart, pillars, chests and spawner of the magma chamber. */
    private void decorateChamber(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
        int floorY = this.chamberY - 4;
        BlockPos heart = new BlockPos(this.cx, floorY + 2, this.cz);
        this.place(level, chunkBox, heart.below(), VolcanicBlocks.CHISELED_OBSIDIAN.get().defaultBlockState());
        this.place(level, chunkBox, heart, VolcanicBlocks.HEART_OF_THE_VOLCANO.get().defaultBlockState()
                .setValue(HeartOfTheVolcanoBlock.DORMANT, true));
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int y = floorY + 1; y <= this.chamberY + 3; y++) {
                    this.place(level, chunkBox, new BlockPos(this.cx + sx * 3, y, this.cz + sz * 3),
                            VolcanicBlocks.OBSIDIAN_BRICKS.get().defaultBlockState());
                }
                this.place(level, chunkBox, new BlockPos(this.cx + sx * 3, floorY + 3, this.cz + sz * 3),
                        VolcanicBlocks.MOLTEN_SCORIA.get().defaultBlockState());
            }
        }
        // Three chests on the other three sides of the heart.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (side == this.tunnel) {
                continue;
            }
            BlockPos chest = heart.below().relative(side, 4);
            if (chunkBox.isInside(chest)) {
                level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, side.getOpposite()), FLAGS);
                RandomizableContainerBlockEntity.setLootTable(level, random, chest, LAIR_LOOT);
            }
        }
        BlockPos spawner = heart.below().relative(this.tunnel.getOpposite(), 4).relative(this.tunnel.getClockWise(), 2);
        if (chunkBox.isInside(spawner)) {
            level.setBlock(spawner, Blocks.SPAWNER.defaultBlockState(), FLAGS);
            BlockEntity blockEntity = level.getBlockEntity(spawner);
            if (blockEntity instanceof SpawnerBlockEntity spawnerEntity) {
                spawnerEntity.setEntityId(ModEntities.MAGMALING.get(), random);
            }
        }
        // A pumice sponge left by the forge-keepers, and an ancient forge.
        this.place(level, chunkBox, heart.below().relative(this.tunnel, 3).relative(this.tunnel.getCounterClockWise(), 2), VolcanicBlocks.VOLCANIC_FORGE.get().defaultBlockState()
                .setValue(com.dunesrelics.block.volcanic.VolcanicForgeBlock.FACING, this.tunnel.getOpposite()));
        this.place(level, chunkBox, heart.below().relative(this.tunnel, 3).relative(this.tunnel.getClockWise(), 2),
                VolcanicBlocks.PUMICE_SPONGE.get().defaultBlockState());
    }

    private void place(WorldGenLevel level, BoundingBox chunkBox, BlockPos pos, BlockState state) {
        if (chunkBox.isInside(pos)) {
            level.setBlock(pos, state, FLAGS);
        }
    }
}
