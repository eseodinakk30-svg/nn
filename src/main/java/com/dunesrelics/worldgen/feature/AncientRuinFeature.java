package com.dunesrelics.worldgen.feature;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.SarcophagusBlock;
import com.dunesrelics.registry.ModBlocks;
import com.dunesrelics.registry.ModEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Half-buried ruins of a limestone temple: a cracked floor, broken walls, toppled columns, sealed urns and a
 * treasure chest. About a third of the ruins hide a shaft leading down to a tomb with a sealed sarcophagus,
 * where the Pharaoh sleeps.
 */
public class AncientRuinFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceLocation RUIN_LOOT = DunesRelics.id("chests/ancient_ruins");
    public static final ResourceLocation TOMB_LOOT = DunesRelics.id("chests/ancient_tomb");

    public AncientRuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();
        return generate(context.level(), random, context.origin(), random.nextFloat() < 0.35F);
    }

    public static boolean generate(WorldGenLevel level, RandomSource random, BlockPos origin, boolean withTomb) {
        BlockState ground = level.getBlockState(origin.below());
        if (!ground.is(BlockTags.SAND) && !ground.is(Blocks.SANDSTONE)) {
            return false;
        }
        int halfX = 3 + random.nextInt(3);
        int halfZ = 3 + random.nextInt(3);
        BlockPos center = origin.below(1 + random.nextInt(2));
        Direction drift = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Direction doorSide = Direction.Plane.HORIZONTAL.getRandomDirection(random);

        // Floor and foundation, with the space above cleared of sand.
        for (int x = -halfX; x <= halfX; x++) {
            for (int z = -halfZ; z <= halfZ; z++) {
                BlockPos pos = center.offset(x, 0, z);
                level.setBlock(pos, RuinHelper.floor(random), RuinHelper.FLAGS);
                RuinHelper.underpin(level, pos, 6);
                for (int dy = 1; dy <= 7; dy++) {
                    if (RuinHelper.isLoose(level, pos.above(dy))) {
                        level.setBlock(pos.above(dy), Blocks.AIR.defaultBlockState(), RuinHelper.FLAGS);
                    }
                }
            }
        }

        // Walls and corner columns.
        for (int x = -halfX; x <= halfX; x++) {
            for (int z = -halfZ; z <= halfZ; z++) {
                boolean edgeX = Math.abs(x) == halfX;
                boolean edgeZ = Math.abs(z) == halfZ;
                if (!edgeX && !edgeZ) {
                    continue;
                }
                BlockPos base = center.offset(x, 0, z);
                if (edgeX && edgeZ) {
                    int height = random.nextFloat() < 0.35F ? 1 + random.nextInt(2) : 3 + random.nextInt(4);
                    for (int dy = 1; dy <= height; dy++) {
                        level.setBlock(base.above(dy), RuinHelper.pillar(Direction.Axis.Y), RuinHelper.FLAGS);
                    }
                    if (height >= 3) {
                        BlockState cap = random.nextFloat() < 0.2F ? ModBlocks.GILDED_LIMESTONE.get().defaultBlockState()
                                : ModBlocks.CHISELED_LIMESTONE_BRICKS.get().defaultBlockState();
                        level.setBlock(base.above(height + 1), cap, RuinHelper.FLAGS);
                    }
                    continue;
                }
                boolean doorway = doorSide.getAxis() == Direction.Axis.X
                        ? x == doorSide.getStepX() * halfX && Math.abs(z) <= 1
                        : z == doorSide.getStepZ() * halfZ && Math.abs(x) <= 1;
                if (doorway) {
                    continue;
                }
                int height = random.nextInt(5) == 0 ? 0 : 1 + random.nextInt(3);
                for (int dy = 1; dy <= height; dy++) {
                    BlockState state = dy == 2 && random.nextInt(4) == 0
                            ? ModBlocks.CHISELED_LIMESTONE_BRICKS.get().defaultBlockState() : RuinHelper.bricks(random);
                    level.setBlock(base.above(dy), state, RuinHelper.FLAGS);
                }
                if (height > 0 && random.nextInt(3) == 0) {
                    level.setBlock(base.above(height + 1), ModBlocks.LIMESTONE_BRICK_SLAB.get().defaultBlockState(), RuinHelper.FLAGS);
                }
            }
        }

        // Treasure and urns.
        int innerX = halfX - 1;
        int innerZ = halfZ - 1;
        BlockPos chestPos = center.offset(random.nextInt(innerX * 2 + 1) - innerX, 1, random.nextInt(innerZ * 2 + 1) - innerZ);
        RuinHelper.chest(level, random, chestPos, Direction.Plane.HORIZONTAL.getRandomDirection(random), RUIN_LOOT);
        int urns = 1 + random.nextInt(3);
        for (int i = 0; i < urns; i++) {
            BlockPos urn = center.offset(random.nextInt(innerX * 2 + 1) - innerX, 1, random.nextInt(innerZ * 2 + 1) - innerZ);
            if (level.isEmptyBlock(urn)) {
                level.setBlock(urn, RuinHelper.sealedUrn(), RuinHelper.FLAGS);
            }
        }

        // A small gilded altar in the middle.
        BlockPos altar = center.above();
        if (!withTomb && random.nextFloat() < 0.4F && level.isEmptyBlock(altar)) {
            level.setBlock(altar, ModBlocks.POLISHED_LIMESTONE.get().defaultBlockState(), RuinHelper.FLAGS);
            if (level.isEmptyBlock(altar.above())) {
                level.setBlock(altar.above(), ModBlocks.GILDED_LIMESTONE.get().defaultBlockState(), RuinHelper.FLAGS);
            }
        }

        // A toppled column lying in the sand next to the ruin.
        if (random.nextFloat() < 0.5F) {
            Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            Direction along = side.getClockWise();
            int distance = (side.getAxis() == Direction.Axis.X ? halfX : halfZ) + 2;
            BlockPos start = center.relative(side, distance).above();
            int length = 2 + random.nextInt(3);
            for (int i = 0; i < length; i++) {
                BlockPos pos = start.relative(along, i - length / 2);
                if (RuinHelper.isLoose(level, pos)) {
                    level.setBlock(pos, RuinHelper.pillar(along.getAxis()), RuinHelper.FLAGS);
                    RuinHelper.underpin(level, pos, 3);
                }
            }
        }

        // Sand drifting in from one side, half burying the ruin.
        for (int x = -halfX; x <= halfX; x++) {
            for (int z = -halfZ; z <= halfZ; z++) {
                float toward = (x * drift.getStepX() / (float) halfX) + (z * drift.getStepZ() / (float) halfZ);
                float chance = 0.15F + 0.35F * toward;
                BlockPos pos = center.offset(x, 1, z);
                if (random.nextFloat() < chance && level.isEmptyBlock(pos)) {
                    level.setBlock(pos, Blocks.SAND.defaultBlockState(), RuinHelper.FLAGS);
                    if (random.nextFloat() < chance * 0.6F && level.isEmptyBlock(pos.above())) {
                        level.setBlock(pos.above(), Blocks.SAND.defaultBlockState(), RuinHelper.FLAGS);
                    }
                }
            }
        }

        if (withTomb) {
            buildTomb(level, random, center);
        }
        return true;
    }

    /** A burial chamber 9-12 blocks under the ruin, reached by a ladder shaft. */
    static void buildTomb(WorldGenLevel level, RandomSource random, BlockPos ruinFloor) {
        BlockPos room = ruinFloor.below(9 + random.nextInt(4));
        int half = 4;
        int height = 5;
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                for (int y = 0; y <= height; y++) {
                    BlockPos pos = room.offset(x, y, z);
                    boolean wall = Math.abs(x) == half || Math.abs(z) == half;
                    BlockState state;
                    if (y == 0) {
                        state = (x + z) % 2 == 0 ? ModBlocks.POLISHED_LIMESTONE.get().defaultBlockState()
                                : ModBlocks.LIMESTONE_BRICKS.get().defaultBlockState();
                    } else if (y == height) {
                        state = RuinHelper.bricks(random);
                    } else if (wall) {
                        state = y == 3 && (x + z) % 3 == 0 ? ModBlocks.CHISELED_LIMESTONE_BRICKS.get().defaultBlockState()
                                : y == 2 && random.nextInt(12) == 0 ? ModBlocks.GILDED_LIMESTONE.get().defaultBlockState()
                                : RuinHelper.bricks(random);
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    level.setBlock(pos, state, RuinHelper.FLAGS);
                }
            }
        }

        // Columns in the corners; one of them may be a mummy spawner.
        boolean spawner = random.nextBoolean();
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos corner = room.offset(sx * 3, 0, sz * 3);
                for (int y = 1; y < height; y++) {
                    level.setBlock(corner.above(y), RuinHelper.pillar(Direction.Axis.Y), RuinHelper.FLAGS);
                }
                if (spawner && sx == 1 && sz == -1) {
                    BlockPos spawnerPos = corner.above();
                    level.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), RuinHelper.FLAGS);
                    BlockEntity blockEntity = level.getBlockEntity(spawnerPos);
                    if (blockEntity instanceof SpawnerBlockEntity spawnerEntity) {
                        spawnerEntity.setEntityId(ModEntities.MUMMY.get(), random);
                    }
                }
            }
        }

        // The sarcophagus, flanked by urns, with the grave goods.
        BlockPos sarcophagus = room.offset(0, 1, -1);
        level.setBlock(sarcophagus, ModBlocks.SARCOPHAGUS.get().defaultBlockState()
                .setValue(SarcophagusBlock.FACING, Direction.SOUTH).setValue(SarcophagusBlock.SEALED, true), RuinHelper.FLAGS);
        level.setBlock(sarcophagus.west(2), RuinHelper.sealedUrn(), RuinHelper.FLAGS);
        level.setBlock(sarcophagus.east(2), RuinHelper.sealedUrn(), RuinHelper.FLAGS);
        level.setBlock(sarcophagus.north(2).west(), ModBlocks.GILDED_LIMESTONE.get().defaultBlockState(), RuinHelper.FLAGS);
        level.setBlock(sarcophagus.north(2).east(), ModBlocks.GILDED_LIMESTONE.get().defaultBlockState(), RuinHelper.FLAGS);
        RuinHelper.chest(level, random, room.offset(-2, 1, -3), Direction.SOUTH, TOMB_LOOT);
        RuinHelper.chest(level, random, room.offset(2, 1, -3), Direction.SOUTH, TOMB_LOOT);
        if (random.nextBoolean()) {
            level.setBlock(room.offset(-3, 1, 1), RuinHelper.sealedUrn(), RuinHelper.FLAGS);
        }
        if (random.nextBoolean()) {
            level.setBlock(room.offset(3, 1, 1), RuinHelper.sealedUrn(), RuinHelper.FLAGS);
        }

        // Ladder shaft from the ruin floor down into the chamber, against a brick column.
        BlockPos shaftTop = ruinFloor.offset(0, 0, 2);
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
        for (int y = shaftTop.getY(); y > room.getY(); y--) {
            BlockPos pos = new BlockPos(shaftTop.getX(), y, shaftTop.getZ());
            level.setBlock(pos.south(), ModBlocks.LIMESTONE_BRICKS.get().defaultBlockState(), RuinHelper.FLAGS);
            if (y > room.getY() + height) {
                level.setBlock(pos.north(), RuinHelper.bricks(random), RuinHelper.FLAGS);
                level.setBlock(pos.east(), RuinHelper.bricks(random), RuinHelper.FLAGS);
                level.setBlock(pos.west(), RuinHelper.bricks(random), RuinHelper.FLAGS);
            }
            level.setBlock(pos, ladder, RuinHelper.FLAGS);
        }
        // Keep the shaft opening in the ruin floor clear of drifted sand.
        if (level.getBlockState(shaftTop.above()).is(Blocks.SAND)) {
            level.setBlock(shaftTop.above(), Blocks.AIR.defaultBlockState(), RuinHelper.FLAGS);
        }
    }
}
