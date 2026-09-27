package com.dunesrelics.memory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a village has to spend to lay a block: wood (felled by its lumberjacks) or stone (dug by its quarrymen).
 * Glass, wool, lanterns, plants and the like the villagers make or find themselves.
 */
public final class Materials {
    public enum Kind { WOOD, STONE, FREE }

    private Materials() {}

    public static Kind kind(BlockState state) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            return Kind.FREE;
        }
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(BlockTags.BEDS)
                || state.is(BlockTags.SIGNS) || state.is(Blocks.LADDER) || state.is(Blocks.BARREL) || state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.COMPOSTER) || state.is(Blocks.LECTERN) || state.is(Blocks.BOOKSHELF) || state.is(Blocks.CHEST)) {
            return Kind.WOOD;
        }
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (name.contains("cobblestone") || name.contains("stone") || name.contains("brick") || name.contains("terracotta")
                || name.contains("deepslate") || name.contains("andesite") || name.contains("diorite") || name.contains("granite")
                || name.contains("furnace") || name.contains("smoker") || name.contains("scoria") || name.contains("pumice")) {
            return Kind.STONE;
        }
        if (name.contains("planks") || name.endsWith("_log") || name.contains("wood") || name.contains("sails")
                || name.contains("scaffolding") || name.contains("water_wheel") || name.contains("trough")) {
            return Kind.WOOD;
        }
        return Kind.FREE;
    }

    /** Whether the village has the material for this block. */
    public static boolean canAfford(WorldMemory.VillageRecord record, BlockState state) {
        return switch (kind(state)) {
            case WOOD -> record.wood > 0;
            case STONE -> record.stone > 0;
            default -> true;
        };
    }

    public static void spend(WorldMemory.VillageRecord record, BlockState state) {
        switch (kind(state)) {
            case WOOD -> record.wood = Math.max(0, record.wood - 1);
            case STONE -> record.stone = Math.max(0, record.stone - 1);
            default -> { }
        }
    }

    /** Wood and stone the rest of a blueprint still needs, as {wood, stone}. */
    public static int[] cost(Blueprint blueprint, int from) {
        int[] cost = new int[2];
        for (int i = from; i < blueprint.size(); i++) {
            switch (kind(blueprint.state(i))) {
                case WOOD -> cost[0]++;
                case STONE -> cost[1]++;
                default -> { }
            }
        }
        return cost;
    }
}
