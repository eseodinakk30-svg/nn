package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.item.BandageItem;
import com.dunesrelics.item.ModArmorMaterials;
import com.dunesrelics.item.ModFoods;
import com.dunesrelics.item.ModTiers;
import com.dunesrelics.item.ScepterOfSandsItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DunesRelics.MODID);

    public static final ResourceKey<TrimPattern> PHARAOH_TRIM = ResourceKey.create(Registries.TRIM_PATTERN, DunesRelics.id("pharaoh"));

    // Materials
    public static final RegistryObject<Item> AMBER = ITEMS.register("amber", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_INGOT = ITEMS.register("bronze_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_NUGGET = ITEMS.register("bronze_nugget", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LINEN = ITEMS.register("linen", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SCORPION_STINGER = ITEMS.register("scorpion_stinger", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> VULTURE_FEATHER = ITEMS.register("vulture_feather", () -> new Item(new Item.Properties()));

    // Bronze tools and armor
    public static final RegistryObject<Item> BRONZE_KHOPESH = ITEMS.register("bronze_khopesh",
            () -> new SwordItem(ModTiers.BRONZE, 3, -2.2F, new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_SHOVEL = ITEMS.register("bronze_shovel",
            () -> new ShovelItem(ModTiers.BRONZE, 1.5F, -3.0F, new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_PICKAXE = ITEMS.register("bronze_pickaxe",
            () -> new PickaxeItem(ModTiers.BRONZE, 1, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_AXE = ITEMS.register("bronze_axe",
            () -> new AxeItem(ModTiers.BRONZE, 6.0F, -3.1F, new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_HOE = ITEMS.register("bronze_hoe",
            () -> new HoeItem(ModTiers.BRONZE, -2, -1.0F, new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_HELMET = ITEMS.register("bronze_helmet",
            () -> new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_CHESTPLATE = ITEMS.register("bronze_chestplate",
            () -> new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_LEGGINGS = ITEMS.register("bronze_leggings",
            () -> new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_BOOTS = ITEMS.register("bronze_boots",
            () -> new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> AMBER_GOGGLES = ITEMS.register("amber_goggles",
            () -> new ArmorItem(ModArmorMaterials.AMBER_GOGGLES, ArmorItem.Type.HELMET, new Item.Properties()));

    // Food and remedies
    public static final RegistryObject<Item> DATES = ITEMS.register("dates", () -> new Item(new Item.Properties().food(ModFoods.DATES)));
    public static final RegistryObject<Item> HONEYED_DATES = ITEMS.register("honeyed_dates", () -> new Item(new Item.Properties().food(ModFoods.HONEYED_DATES)));
    public static final RegistryObject<Item> FLATBREAD = ITEMS.register("flatbread", () -> new Item(new Item.Properties().food(ModFoods.FLATBREAD)));
    public static final RegistryObject<Item> ALOE_LEAF = ITEMS.register("aloe_leaf", () -> new Item(new Item.Properties().food(ModFoods.ALOE_LEAF)));
    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage", () -> new BandageItem(new Item.Properties().stacksTo(16)));

    // Relics
    public static final RegistryObject<Item> PHARAOH_ARMOR_TRIM_SMITHING_TEMPLATE = ITEMS.register("pharaoh_armor_trim_smithing_template",
            () -> SmithingTemplateItem.createArmorTrimTemplate(PHARAOH_TRIM));
    public static final RegistryObject<Item> SCEPTER_OF_SANDS = ITEMS.register("scepter_of_sands",
            () -> new ScepterOfSandsItem(new Item.Properties().durability(250).rarity(Rarity.EPIC)));

    // Spawn eggs
    public static final RegistryObject<Item> MUMMY_SPAWN_EGG = ITEMS.register("mummy_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MUMMY, 0xD9CBA3, 0x5B4A34, new Item.Properties()));
    public static final RegistryObject<Item> PHARAOH_SPAWN_EGG = ITEMS.register("pharaoh_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PHARAOH, 0xE3B43C, 0x2D5DA8, new Item.Properties()));
    public static final RegistryObject<Item> SCORPION_SPAWN_EGG = ITEMS.register("scorpion_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SCORPION, 0xC9913F, 0x3A2412, new Item.Properties()));
    public static final RegistryObject<Item> SCARAB_SPAWN_EGG = ITEMS.register("scarab_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SCARAB, 0x1F3B4D, 0x3FB6A8, new Item.Properties()));
    public static final RegistryObject<Item> MEERKAT_SPAWN_EGG = ITEMS.register("meerkat_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MEERKAT, 0xC8A26E, 0x3B2A1E, new Item.Properties()));
    public static final RegistryObject<Item> VULTURE_SPAWN_EGG = ITEMS.register("vulture_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.VULTURE, 0x2E2622, 0xE0A9A0, new Item.Properties()));

    private ModItems() {}

    public static void registerCompostables() {
        ComposterBlock.COMPOSTABLES.put(ModBlocks.PALM_LEAVES.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ModBlocks.PALM_SAPLING.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ModBlocks.DUNE_GRASS.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ModBlocks.CATTAIL.get().asItem(), 0.5F);
        ComposterBlock.COMPOSTABLES.put(ModBlocks.DESERT_ROSE.get().asItem(), 0.65F);
        ComposterBlock.COMPOSTABLES.put(ModBlocks.ALOE_VERA.get().asItem(), 0.5F);
        ComposterBlock.COMPOSTABLES.put(ALOE_LEAF.get(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(DATES.get(), 0.65F);
        ComposterBlock.COMPOSTABLES.put(FLATBREAD.get(), 0.85F);
    }
}
