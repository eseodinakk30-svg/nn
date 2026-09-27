package com.dunesrelics.event;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.registry.ModBlocks;
import com.dunesrelics.registry.ModItems;
import com.dunesrelics.registry.VolcanicBlocks;
import com.dunesrelics.registry.VolcanicItems;
import com.dunesrelics.registry.WorldBlocks;
import com.dunesrelics.registry.WorldItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;


/** Like a real update, the new content also shows up in the vanilla creative tabs. */
@Mod.EventBusSubscriber(modid = DunesRelics.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabEvents {
    private CreativeTabEvents() {}

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = event.getTabKey();
        if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            accept(event, ModBlocks.PALM_LOG, ModBlocks.PALM_WOOD, ModBlocks.STRIPPED_PALM_LOG,
                    ModBlocks.STRIPPED_PALM_WOOD, ModBlocks.PALM_PLANKS, ModBlocks.PALM_STAIRS, ModBlocks.PALM_SLAB,
                    ModBlocks.PALM_FENCE, ModBlocks.PALM_FENCE_GATE, ModBlocks.PALM_DOOR, ModBlocks.PALM_TRAPDOOR,
                    ModBlocks.PALM_PRESSURE_PLATE, ModBlocks.PALM_BUTTON, ModBlocks.LIMESTONE,
                    ModBlocks.LIMESTONE_STAIRS, ModBlocks.LIMESTONE_SLAB, ModBlocks.LIMESTONE_WALL,
                    ModBlocks.POLISHED_LIMESTONE, ModBlocks.POLISHED_LIMESTONE_STAIRS,
                    ModBlocks.POLISHED_LIMESTONE_SLAB, ModBlocks.POLISHED_LIMESTONE_WALL, ModBlocks.LIMESTONE_BRICKS,
                    ModBlocks.CRACKED_LIMESTONE_BRICKS, ModBlocks.LIMESTONE_BRICK_STAIRS,
                    ModBlocks.LIMESTONE_BRICK_SLAB, ModBlocks.LIMESTONE_BRICK_WALL,
                    ModBlocks.CHISELED_LIMESTONE_BRICKS, ModBlocks.LIMESTONE_PILLAR, ModBlocks.GILDED_LIMESTONE,
                    ModBlocks.AMBER_BLOCK, ModBlocks.BRONZE_BLOCK,
                    VolcanicBlocks.EMBER_LOG, VolcanicBlocks.EMBER_WOOD, VolcanicBlocks.STRIPPED_EMBER_LOG,
                    VolcanicBlocks.STRIPPED_EMBER_WOOD, VolcanicBlocks.EMBER_PLANKS, VolcanicBlocks.EMBER_STAIRS,
                    VolcanicBlocks.EMBER_SLAB, VolcanicBlocks.EMBER_FENCE, VolcanicBlocks.EMBER_FENCE_GATE,
                    VolcanicBlocks.EMBER_DOOR, VolcanicBlocks.EMBER_TRAPDOOR, VolcanicBlocks.EMBER_PRESSURE_PLATE,
                    VolcanicBlocks.EMBER_BUTTON, VolcanicBlocks.SCORIA, VolcanicBlocks.SCORIA_STAIRS,
                    VolcanicBlocks.SCORIA_SLAB, VolcanicBlocks.SCORIA_WALL, VolcanicBlocks.POLISHED_SCORIA,
                    VolcanicBlocks.POLISHED_SCORIA_STAIRS, VolcanicBlocks.POLISHED_SCORIA_SLAB,
                    VolcanicBlocks.POLISHED_SCORIA_WALL, VolcanicBlocks.SCORIA_BRICKS, VolcanicBlocks.CRACKED_SCORIA_BRICKS,
                    VolcanicBlocks.SCORIA_BRICK_STAIRS, VolcanicBlocks.SCORIA_BRICK_SLAB, VolcanicBlocks.SCORIA_BRICK_WALL,
                    VolcanicBlocks.CHISELED_SCORIA_BRICKS, VolcanicBlocks.PUMICE, VolcanicBlocks.PUMICE_BRICKS,
                    VolcanicBlocks.PUMICE_BRICK_STAIRS, VolcanicBlocks.PUMICE_BRICK_SLAB, VolcanicBlocks.OBSIDIAN_BRICKS,
                    VolcanicBlocks.OBSIDIAN_BRICK_STAIRS, VolcanicBlocks.OBSIDIAN_BRICK_SLAB, VolcanicBlocks.OBSIDIAN_BRICK_WALL,
                    VolcanicBlocks.CHISELED_OBSIDIAN, VolcanicBlocks.FIRE_OPAL_BLOCK, VolcanicBlocks.SULFUR_BLOCK);
        } else if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            accept(event, ModBlocks.QUICKSAND, ModBlocks.LIMESTONE, ModBlocks.AMBER_ORE, ModBlocks.PALM_LOG,
                    ModBlocks.PALM_LEAVES, ModBlocks.PALM_SAPLING, ModBlocks.DUNE_GRASS, ModBlocks.DESERT_ROSE,
                    ModBlocks.ALOE_VERA, ModBlocks.CATTAIL, VolcanicBlocks.ASH_BLOCK, VolcanicBlocks.ASH_LAYER,
                    VolcanicBlocks.BLACK_SAND, VolcanicBlocks.SCORIA, VolcanicBlocks.MOLTEN_SCORIA, VolcanicBlocks.PUMICE,
                    VolcanicBlocks.FIRE_OPAL_ORE, VolcanicBlocks.DEEPSLATE_FIRE_OPAL_ORE, VolcanicBlocks.SULFUR_ORE,
                    VolcanicBlocks.SULFUR_CLUSTER, VolcanicBlocks.EMBER_LOG, VolcanicBlocks.EMBER_LEAVES,
                    VolcanicBlocks.EMBER_SAPLING, VolcanicBlocks.ASH_GRASS, VolcanicBlocks.FIREBLOSSOM,
                    VolcanicBlocks.LAVA_LILY, VolcanicBlocks.STEAM_VENT, WorldBlocks.WET_SAND, WorldBlocks.SEASHELL,
                    WorldBlocks.CLAM);
        } else if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            accept(event, ModBlocks.AMBER_LAMP, ModBlocks.ANCIENT_URN, ModBlocks.SARCOPHAGUS, VolcanicBlocks.VOLCANIC_FORGE,
                    VolcanicBlocks.PUMICE_SPONGE, VolcanicBlocks.MOLTEN_PUMICE_SPONGE, VolcanicBlocks.HEART_OF_THE_VOLCANO,
                    VolcanicBlocks.VOLCANIC_GLASS, VolcanicBlocks.VOLCANIC_GLASS_PANE, WorldBlocks.WATER_WHEEL,
                    WorldBlocks.MILLSTONE, WorldBlocks.WATER_TROUGH, WorldBlocks.CANNON, WorldBlocks.DREAMCATCHER);
        } else if (tab == CreativeModeTabs.REDSTONE_BLOCKS) {
            accept(event, ModBlocks.PALM_DOOR, ModBlocks.PALM_TRAPDOOR, ModBlocks.PALM_FENCE_GATE,
                    ModBlocks.PALM_BUTTON, ModBlocks.PALM_PRESSURE_PLATE, ModBlocks.AMBER_LAMP, WorldBlocks.WATER_WHEEL,
                    WorldBlocks.CANNON);
        } else if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            accept(event, ModItems.BRONZE_SHOVEL, ModItems.BRONZE_PICKAXE, ModItems.BRONZE_AXE, ModItems.BRONZE_HOE,
                    ModItems.SCEPTER_OF_SANDS, VolcanicItems.FIRE_OPAL_SHOVEL, VolcanicItems.FIRE_OPAL_PICKAXE,
                    VolcanicItems.FIRE_OPAL_AXE, VolcanicItems.FIRE_OPAL_HOE, VolcanicItems.VOLCANIC_ASH, VolcanicItems.LAVA_CRAB_BUCKET,
                    WorldItems.TIDE_CLOCK, WorldItems.WORLD_CHRONICLE, WorldItems.MESSAGE_IN_A_BOTTLE);
        } else if (tab == CreativeModeTabs.COMBAT) {
            accept(event, ModItems.BRONZE_KHOPESH, ModItems.BRONZE_AXE, ModItems.BRONZE_HELMET,
                    ModItems.BRONZE_CHESTPLATE, ModItems.BRONZE_LEGGINGS, ModItems.BRONZE_BOOTS,
                    ModItems.AMBER_GOGGLES, VolcanicItems.FIRE_OPAL_SWORD, VolcanicItems.MAGMA_HAMMER,
                    VolcanicItems.FIRE_OPAL_HELMET, VolcanicItems.FIRE_OPAL_CHESTPLATE, VolcanicItems.FIRE_OPAL_LEGGINGS,
                    VolcanicItems.FIRE_OPAL_BOOTS, VolcanicItems.SALAMANDER_BOOTS, WorldItems.CUTLASS, WorldItems.CAPTAIN_HAT,
                    WorldItems.CANNONBALL);
        } else if (tab == CreativeModeTabs.FOOD_AND_DRINKS) {
            accept(event, ModItems.DATES, ModItems.HONEYED_DATES, ModItems.FLATBREAD, ModItems.ALOE_LEAF,
                    ModItems.BANDAGE, VolcanicItems.FIRE_PEPPER, VolcanicItems.CRAB_MEAT, VolcanicItems.COOKED_CRAB_MEAT,
                    VolcanicItems.SPICY_STEW, WorldItems.DOUGH);
        } else if (tab == CreativeModeTabs.INGREDIENTS) {
            accept(event, ModItems.AMBER, ModItems.BRONZE_NUGGET, ModItems.BRONZE_INGOT, ModItems.LINEN,
                    ModItems.SCORPION_STINGER, ModItems.SCORPION_VENOM, ModItems.VULTURE_FEATHER,
                    ModItems.PHARAOH_ARMOR_TRIM_SMITHING_TEMPLATE, VolcanicItems.FIRE_OPAL, VolcanicItems.SULFUR,
                    VolcanicItems.SALAMANDER_SCALE, VolcanicItems.EMBER_CORE, VolcanicItems.TITAN_ARMOR_TRIM_SMITHING_TEMPLATE,
                    WorldItems.FLOUR, WorldItems.PEARL, WorldItems.GLOOM_DUST, WorldItems.CANNONBALL);
        } else if (tab == CreativeModeTabs.SPAWN_EGGS) {
            accept(event, ModItems.MEERKAT_SPAWN_EGG, ModItems.MUMMY_SPAWN_EGG, ModItems.PHARAOH_SPAWN_EGG,
                    ModItems.SCARAB_SPAWN_EGG, ModItems.SCORPION_SPAWN_EGG, ModItems.VULTURE_SPAWN_EGG,
                    VolcanicItems.CINDER_WRAITH_SPAWN_EGG, VolcanicItems.LAVA_CRAB_SPAWN_EGG, VolcanicItems.MAGMA_TITAN_SPAWN_EGG,
                    VolcanicItems.MAGMALING_SPAWN_EGG, VolcanicItems.SALAMANDER_SPAWN_EGG, WorldItems.PIRATE_SPAWN_EGG,
                    WorldItems.PIRATE_GUNNER_SPAWN_EGG, WorldItems.PIRATE_CAPTAIN_SPAWN_EGG, WorldItems.TRAVELER_SPAWN_EGG,
                    WorldItems.SHADE_SPAWN_EGG);
        }
    }

    private static void accept(BuildCreativeModeTabContentsEvent event, RegistryObject<?>... items) {
        for (RegistryObject<?> item : items) {
            event.accept((ItemLike) item.get());
        }
    }
}
