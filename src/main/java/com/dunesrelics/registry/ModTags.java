package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

public final class ModTags {
    /** Biomes where rainy weather turns into a sandstorm instead. */
    public static final TagKey<Biome> HAS_SANDSTORMS = TagKey.create(Registries.BIOME, DunesRelics.id("has_sandstorms"));
    /** Biomes where rainy weather turns into falling ash, and thunderstorms into volcanic eruptions. */
    public static final TagKey<Biome> HAS_ASHFALL = TagKey.create(Registries.BIOME, DunesRelics.id("has_ashfall"));
    /** Creatures of the desert that can walk across quicksand without sinking. */
    public static final TagKey<EntityType<?>> QUICKSAND_WALKERS = TagKey.create(Registries.ENTITY_TYPE, DunesRelics.id("quicksand_walkers"));

    /** Presents a villager is glad to be given (sneak and use the item on them). */
    public static final TagKey<Item> VILLAGER_GIFTS = TagKey.create(Registries.ITEM, DunesRelics.id("villager_gifts"));
    /** Biomes whose beaches and seas have tides. */
    public static final TagKey<Biome> HAS_TIDES = TagKey.create(Registries.BIOME, DunesRelics.id("has_tides"));
    /** Biomes whose waters flow with currents. */
    public static final TagKey<Biome> HAS_CURRENTS = TagKey.create(Registries.BIOME, DunesRelics.id("has_currents"));

    private ModTags() {}
}
