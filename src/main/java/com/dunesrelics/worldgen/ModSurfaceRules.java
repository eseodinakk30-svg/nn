package com.dunesrelics.worldgen;

import com.dunesrelics.registry.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Ancient Dunes: deep sand over sandstone, resting on a thick shelf of limestone that caves and ravines cut into.
 */
public final class ModSurfaceRules {
    private ModSurfaceRules() {}

    public static SurfaceRules.RuleSource makeRules() {
        SurfaceRules.RuleSource sand = SurfaceRules.state(Blocks.SAND.defaultBlockState());
        SurfaceRules.RuleSource sandstone = SurfaceRules.state(Blocks.SANDSTONE.defaultBlockState());
        SurfaceRules.RuleSource limestone = SurfaceRules.state(ModBlocks.LIMESTONE.get().defaultBlockState());
        SurfaceRules.RuleSource sandWithSandstoneCeiling = SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, sandstone), sand);

        return SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.ANCIENT_DUNES),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, sandWithSandstoneCeiling),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, sandWithSandstoneCeiling),
                        SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, sandstone),
                        SurfaceRules.ifTrue(SurfaceRules.VERY_DEEP_UNDER_FLOOR, limestone))));
    }
}
