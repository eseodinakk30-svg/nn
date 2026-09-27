package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.worldgen.feature.AncientRuinFeature;
import com.dunesrelics.worldgen.feature.BasaltPillarsFeature;
import com.dunesrelics.worldgen.feature.FumaroleFeature;
import com.dunesrelics.worldgen.feature.HotSpringFeature;
import com.dunesrelics.worldgen.feature.RuinedForgeFeature;
import com.dunesrelics.worldgen.feature.OasisFeature;
import com.dunesrelics.worldgen.feature.ObeliskFeature;
import com.dunesrelics.worldgen.feature.PalmTreeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, DunesRelics.MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PALM_TREE = FEATURES.register("palm_tree",
            () -> new PalmTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ANCIENT_RUIN = FEATURES.register("ancient_ruin",
            () -> new AncientRuinFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OBELISK = FEATURES.register("obelisk",
            () -> new ObeliskFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OASIS = FEATURES.register("oasis",
            () -> new OasisFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> BASALT_PILLARS = FEATURES.register("basalt_pillars",
            () -> new BasaltPillarsFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FUMAROLE = FEATURES.register("fumarole",
            () -> new FumaroleFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> HOT_SPRING = FEATURES.register("hot_spring",
            () -> new HotSpringFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> RUINED_FORGE = FEATURES.register("ruined_forge",
            () -> new RuinedForgeFeature(NoneFeatureConfiguration.CODEC));

    /** The ember tree, grown by ember saplings: a plain vanilla tree configuration in data/.../configured_feature. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> EMBER_TREE_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, DunesRelics.id("ember_tree"));

    /** The configured palm tree, grown by palm saplings. Defined in data/dunesrelics/worldgen/configured_feature. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALM_TREE_KEY =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, DunesRelics.id("palm_tree"));

    private ModFeatures() {}
}
