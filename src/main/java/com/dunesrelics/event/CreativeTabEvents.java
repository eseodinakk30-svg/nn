package com.dunesrelics.event;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.registry.ModBlocks;
import com.dunesrelics.registry.ModItems;
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
                    ModBlocks.AMBER_BLOCK, ModBlocks.BRONZE_BLOCK);
        } else if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            accept(event, ModBlocks.QUICKSAND, ModBlocks.LIMESTONE, ModBlocks.AMBER_ORE, ModBlocks.PALM_LOG,
                    ModBlocks.PALM_LEAVES, ModBlocks.PALM_SAPLING, ModBlocks.DUNE_GRASS, ModBlocks.DESERT_ROSE,
                    ModBlocks.ALOE_VERA, ModBlocks.CATTAIL);
        } else if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            accept(event, ModBlocks.AMBER_LAMP, ModBlocks.ANCIENT_URN, ModBlocks.SARCOPHAGUS);
        } else if (tab == CreativeModeTabs.REDSTONE_BLOCKS) {
            accept(event, ModBlocks.PALM_DOOR, ModBlocks.PALM_TRAPDOOR, ModBlocks.PALM_FENCE_GATE,
                    ModBlocks.PALM_BUTTON, ModBlocks.PALM_PRESSURE_PLATE, ModBlocks.AMBER_LAMP);
        } else if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            accept(event, ModItems.BRONZE_SHOVEL, ModItems.BRONZE_PICKAXE, ModItems.BRONZE_AXE, ModItems.BRONZE_HOE,
                    ModItems.SCEPTER_OF_SANDS);
        } else if (tab == CreativeModeTabs.COMBAT) {
            accept(event, ModItems.BRONZE_KHOPESH, ModItems.BRONZE_AXE, ModItems.BRONZE_HELMET,
                    ModItems.BRONZE_CHESTPLATE, ModItems.BRONZE_LEGGINGS, ModItems.BRONZE_BOOTS,
                    ModItems.AMBER_GOGGLES);
        } else if (tab == CreativeModeTabs.FOOD_AND_DRINKS) {
            accept(event, ModItems.DATES, ModItems.HONEYED_DATES, ModItems.FLATBREAD, ModItems.ALOE_LEAF,
                    ModItems.BANDAGE);
        } else if (tab == CreativeModeTabs.INGREDIENTS) {
            accept(event, ModItems.AMBER, ModItems.BRONZE_NUGGET, ModItems.BRONZE_INGOT, ModItems.LINEN,
                    ModItems.SCORPION_STINGER, ModItems.VULTURE_FEATHER, ModItems.PHARAOH_ARMOR_TRIM_SMITHING_TEMPLATE);
        } else if (tab == CreativeModeTabs.SPAWN_EGGS) {
            accept(event, ModItems.MEERKAT_SPAWN_EGG, ModItems.MUMMY_SPAWN_EGG, ModItems.PHARAOH_SPAWN_EGG,
                    ModItems.SCARAB_SPAWN_EGG, ModItems.SCORPION_SPAWN_EGG, ModItems.VULTURE_SPAWN_EGG);
        }
    }

    private static void accept(BuildCreativeModeTabContentsEvent event, RegistryObject<?>... items) {
        for (RegistryObject<?> item : items) {
            event.accept((ItemLike) item.get());
        }
    }
}
