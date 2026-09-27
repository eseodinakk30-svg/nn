package com.dunesrelics;

import com.dunesrelics.network.ModNetwork;
import com.dunesrelics.registry.ModBlockEntities;
import com.dunesrelics.registry.ModBlocks;
import com.dunesrelics.registry.ModBrewing;
import com.dunesrelics.registry.ModCreativeTabs;
import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.ModFeatures;
import com.dunesrelics.registry.ModItems;
import com.dunesrelics.registry.ModLootModifiers;
import com.dunesrelics.registry.ModGameRules;
import com.dunesrelics.registry.ModParticles;
import com.dunesrelics.registry.ModPoiTypes;
import com.dunesrelics.registry.ModRecipes;
import com.dunesrelics.registry.ModStructures;
import com.dunesrelics.registry.VolcanicBlocks;
import com.dunesrelics.registry.VolcanicItems;
import com.dunesrelics.registry.WorldBlocks;
import com.dunesrelics.registry.WorldItems;
import com.dunesrelics.worldgen.ModRegion;
import com.dunesrelics.worldgen.ModSurfaceRules;
import com.dunesrelics.worldgen.VolcanicRegion;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

@Mod(DunesRelics.MODID)
public class DunesRelics {
    public static final String MODID = "dunesrelics";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DunesRelics() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        VolcanicBlocks.init();
        VolcanicItems.init();
        WorldBlocks.init();
        WorldItems.init();
        ModGameRules.init();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModStructures.STRUCTURE_TYPES.register(modBus);
        ModStructures.STRUCTURE_PIECES.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModPoiTypes.POI_TYPES.register(modBus);

        ModNetwork.register();
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new ModRegion(id("overworld"), 4));
            Regions.register(new VolcanicRegion(id("volcanic"), 3));
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, MODID, ModSurfaceRules.makeRules());
            ModBlocks.registerFlowerPots();
            VolcanicBlocks.registerFlowerPots();
            ModItems.registerCompostables();
            VolcanicItems.registerCompostables();
            ModBrewing.register();
        });
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
