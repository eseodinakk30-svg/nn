package com.dunesrelics.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * The "Ash & Ember" region: the vanilla overworld, except that savannas burn into Ashen Wastes,
 * warm seas become the Volcanic Archipelago and dripstone caves give way to the Magma Caverns.
 */
public class VolcanicRegion extends Region {
    public VolcanicRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            builder.replaceBiome(Biomes.SAVANNA, ModBiomes.ASHEN_WASTES);
            builder.replaceBiome(Biomes.SAVANNA_PLATEAU, ModBiomes.ASHEN_WASTES);
            builder.replaceBiome(Biomes.WINDSWEPT_SAVANNA, ModBiomes.ASHEN_WASTES);
            builder.replaceBiome(Biomes.WARM_OCEAN, ModBiomes.VOLCANIC_ARCHIPELAGO);
            builder.replaceBiome(Biomes.LUKEWARM_OCEAN, ModBiomes.VOLCANIC_ARCHIPELAGO);
            builder.replaceBiome(Biomes.DEEP_LUKEWARM_OCEAN, ModBiomes.VOLCANIC_ARCHIPELAGO);
            builder.replaceBiome(Biomes.DRIPSTONE_CAVES, ModBiomes.MAGMA_CAVERNS);
        });
    }
}
