package com.dunesrelics;

import com.dunesrelics.registry.ModBlocks;
import com.dunesrelics.registry.ModBrewing;
import com.dunesrelics.registry.ModCreativeTabs;
import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.ModFeatures;
import com.dunesrelics.registry.ModItems;
import com.dunesrelics.registry.ModLootModifiers;
import com.dunesrelics.registry.ModParticles;
import com.dunesrelics.worldgen.ModRegion;
import com.dunesrelics.worldgen.ModSurfaceRules;
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

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new ModRegion(id("overworld"), 4));
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, MODID, ModSurfaceRules.makeRules());
            ModBlocks.registerFlowerPots();
            ModItems.registerCompostables();
            ModBrewing.register();
        });
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
