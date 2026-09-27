package com.dunesrelics.worldgen;

import com.dunesrelics.registry.ModBlocks;
import com.dunesrelics.registry.VolcanicBlocks;
import net.minecraft.world.level.levelgen.Noises;
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

        SurfaceRules.RuleSource dunes = SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.ANCIENT_DUNES),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, sandWithSandstoneCeiling),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, sandWithSandstoneCeiling),
                        SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, sandstone),
                        SurfaceRules.ifTrue(SurfaceRules.VERY_DEEP_UNDER_FLOOR, limestone))));

        // Ashen Wastes: grey ash with patches of scoria and basalt, over a scoria bedrock.
        SurfaceRules.RuleSource ash = SurfaceRules.state(VolcanicBlocks.ASH_BLOCK.get().defaultBlockState());
        SurfaceRules.RuleSource scoria = SurfaceRules.state(VolcanicBlocks.SCORIA.get().defaultBlockState());
        SurfaceRules.RuleSource basalt = SurfaceRules.state(Blocks.BASALT.defaultBlockState());
        SurfaceRules.RuleSource blackSand = SurfaceRules.state(VolcanicBlocks.BLACK_SAND.get().defaultBlockState());
        SurfaceRules.RuleSource ashTop = SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.35D), scoria),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -1.0D, -0.45D), basalt),
                ash);
        SurfaceRules.RuleSource wastes = SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.ASHEN_WASTES),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, ashTop),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, ash),
                        SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, scoria),
                        SurfaceRules.ifTrue(SurfaceRules.VERY_DEEP_UNDER_FLOOR, scoria))));

        // Volcanic Archipelago: black sand beaches and a sea floor of basalt and scoria.
        SurfaceRules.RuleSource archipelago = SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.VOLCANIC_ARCHIPELAGO),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.3D), basalt),
                                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -1.0D, -0.4D), scoria),
                                blackSand)),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, blackSand),
                        SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, scoria))));

        return SurfaceRules.sequence(dunes, wastes, archipelago);
    }
}
