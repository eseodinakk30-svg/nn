package com.dunesrelics.worldgen.feature;

import com.dunesrelics.block.AncientUrnBlock;
import com.dunesrelics.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Block-placing helpers shared by the ruin, tomb and obelisk features. */
final class RuinHelper {
    static final int FLAGS = 2;

    private RuinHelper() {}

    static BlockState bricks(RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.25F) {
            return ModBlocks.CRACKED_LIMESTONE_BRICKS.get().defaultBlockState();
        }
        if (roll < 0.30F) {
            return ModBlocks.LIMESTONE.get().defaultBlockState();
        }
        return ModBlocks.LIMESTONE_BRICKS.get().defaultBlockState();
    }

    static BlockState floor(RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.15F) {
            return Blocks.SAND.defaultBlockState();
        }
        if (roll < 0.30F) {
            return ModBlocks.CRACKED_LIMESTONE_BRICKS.get().defaultBlockState();
        }
        if (roll < 0.40F) {
            return ModBlocks.POLISHED_LIMESTONE.get().defaultBlockState();
        }
        return ModBlocks.LIMESTONE_BRICKS.get().defaultBlockState();
    }

    static BlockState pillar(Direction.Axis axis) {
        return ModBlocks.LIMESTONE_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    static BlockState sealedUrn() {
        return ModBlocks.ANCIENT_URN.get().defaultBlockState().setValue(AncientUrnBlock.SEALED, true);
    }

    /** Whether generation may overwrite this block (loose sand, air, plants). */
    static boolean isLoose(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.SAND) || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.CACTUS) || state.is(Blocks.DEAD_BUSH) || state.is(BlockTags.FLOWERS)
                || state.is(ModBlocks.QUICKSAND.get());
    }

    static boolean isSolid(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
    }

    /** Fills air beneath a block with sandstone so structures never float over a dip or a cave. */
    static void underpin(WorldGenLevel level, BlockPos pos, int maxDepth) {
        for (int dy = 1; dy <= maxDepth; dy++) {
            BlockPos below = pos.below(dy);
            if (isSolid(level, below)) {
                return;
            }
            level.setBlock(below, Blocks.SANDSTONE.defaultBlockState(), FLAGS);
        }
    }

    /**
     * The Y of the topmost solid block in a column, searching a window around {@code nearY}.
     * Works both during world generation and in a live world (unlike the *_WG heightmaps).
     */
    static int surfaceY(WorldGenLevel level, int x, int z, int nearY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, nearY + 12, z);
        for (int y = nearY + 12; y >= nearY - 12; y--) {
            pos.setY(y);
            if (isSolid(level, pos) || !level.getBlockState(pos).getFluidState().isEmpty()) {
                return y;
            }
        }
        return nearY - 13;
    }

    static void chest(WorldGenLevel level, RandomSource random, BlockPos pos, Direction facing, ResourceLocation lootTable) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), FLAGS);
        RandomizableContainerBlockEntity.setLootTable(level, random, pos, lootTable);
    }
}
