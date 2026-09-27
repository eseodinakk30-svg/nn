package com.dunesrelics.worldgen;

import com.dunesrelics.DunesRelics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomes {
    /** Defined in data/dunesrelics/worldgen/biome/ancient_dunes.json. */
    public static final ResourceKey<Biome> ANCIENT_DUNES = ResourceKey.create(Registries.BIOME, DunesRelics.id("ancient_dunes"));

    public static final ResourceKey<Biome> ASHEN_WASTES = ResourceKey.create(Registries.BIOME, DunesRelics.id("ashen_wastes"));
    public static final ResourceKey<Biome> VOLCANIC_ARCHIPELAGO = ResourceKey.create(Registries.BIOME, DunesRelics.id("volcanic_archipelago"));
    public static final ResourceKey<Biome> MAGMA_CAVERNS = ResourceKey.create(Registries.BIOME, DunesRelics.id("magma_caverns"));

    private ModBiomes() {}
}
