package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.item.ModArmorMaterials;
import com.dunesrelics.item.ModTiers;
import com.dunesrelics.item.volcanic.FireOpalArmorItem;
import com.dunesrelics.item.volcanic.FireOpalSwordItem;
import com.dunesrelics.item.volcanic.MagmaHammerItem;
import com.dunesrelics.item.volcanic.SalamanderBootsItem;
import com.dunesrelics.item.volcanic.VolcanicAshItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.RegistryObject;

import static com.dunesrelics.registry.ModItems.ITEMS;

/** Items of the second update, "Ash & Ember". */
public final class VolcanicItems {
    public static final ResourceKey<TrimPattern> TITAN_TRIM = ResourceKey.create(Registries.TRIM_PATTERN, DunesRelics.id("titan"));

    // Materials
    public static final RegistryObject<Item> FIRE_OPAL = ITEMS.register("fire_opal", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SULFUR = ITEMS.register("sulfur", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> VOLCANIC_ASH = ITEMS.register("volcanic_ash", () -> new VolcanicAshItem(new Item.Properties()));
    public static final RegistryObject<Item> SALAMANDER_SCALE = ITEMS.register("salamander_scale", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> EMBER_CORE = ITEMS.register("ember_core",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    public static final RegistryObject<Item> LAVA_CRAB_BUCKET = ITEMS.register("lava_crab_bucket", () -> new MobBucketItem(
            ModEntities.LAVA_CRAB, () -> Fluids.LAVA, () -> SoundEvents.BUCKET_EMPTY_LAVA, new Item.Properties().stacksTo(1)));

    // Fire opal gear
    public static final RegistryObject<Item> FIRE_OPAL_SWORD = ITEMS.register("fire_opal_sword",
            () -> new FireOpalSwordItem(ModTiers.FIRE_OPAL, 3, -2.4F, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> FIRE_OPAL_SHOVEL = ITEMS.register("fire_opal_shovel",
            () -> new ShovelItem(ModTiers.FIRE_OPAL, 1.5F, -3.0F, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> FIRE_OPAL_PICKAXE = ITEMS.register("fire_opal_pickaxe",
            () -> new PickaxeItem(ModTiers.FIRE_OPAL, 1, -2.8F, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> FIRE_OPAL_AXE = ITEMS.register("fire_opal_axe",
            () -> new AxeItem(ModTiers.FIRE_OPAL, 5.0F, -3.0F, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> FIRE_OPAL_HOE = ITEMS.register("fire_opal_hoe",
            () -> new HoeItem(ModTiers.FIRE_OPAL, -3, 0.0F, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> FIRE_OPAL_HELMET = ITEMS.register("fire_opal_helmet",
            () -> new FireOpalArmorItem(ModArmorMaterials.FIRE_OPAL, ArmorItem.Type.HELMET, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> FIRE_OPAL_CHESTPLATE = ITEMS.register("fire_opal_chestplate",
            () -> new FireOpalArmorItem(ModArmorMaterials.FIRE_OPAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> FIRE_OPAL_LEGGINGS = ITEMS.register("fire_opal_leggings",
            () -> new FireOpalArmorItem(ModArmorMaterials.FIRE_OPAL, ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> FIRE_OPAL_BOOTS = ITEMS.register("fire_opal_boots",
            () -> new FireOpalArmorItem(ModArmorMaterials.FIRE_OPAL, ArmorItem.Type.BOOTS, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> SALAMANDER_BOOTS = ITEMS.register("salamander_boots",
            () -> new SalamanderBootsItem(ModArmorMaterials.SALAMANDER, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> MAGMA_HAMMER = ITEMS.register("magma_hammer",
            () -> new MagmaHammerItem(ModTiers.FIRE_OPAL, 6, -3.2F, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    // Food
    public static final RegistryObject<Item> FIRE_PEPPER = ITEMS.register("fire_pepper", () -> new ItemNameBlockItem(
            VolcanicBlocks.FIRE_PEPPER_BUSH.get(), new Item.Properties().food(new FoodProperties.Builder().nutrition(2)
            .saturationMod(0.3F).fast().alwaysEat()
            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0), 1.0F).build())));
    public static final RegistryObject<Item> CRAB_MEAT = ITEMS.register("crab_meat", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(2).saturationMod(0.3F).meat().build())));
    public static final RegistryObject<Item> COOKED_CRAB_MEAT = ITEMS.register("cooked_crab_meat", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(7).saturationMod(0.8F).meat().build())));
    public static final RegistryObject<Item> SPICY_STEW = ITEMS.register("spicy_stew", () -> new BowlFoodItem(new Item.Properties()
            .stacksTo(1).food(new FoodProperties.Builder().nutrition(9).saturationMod(0.8F)
                    .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0), 1.0F).build())));

    // Relics
    public static final RegistryObject<Item> TITAN_ARMOR_TRIM_SMITHING_TEMPLATE = ITEMS.register("titan_armor_trim_smithing_template",
            () -> SmithingTemplateItem.createArmorTrimTemplate(TITAN_TRIM));

    // Spawn eggs
    public static final RegistryObject<Item> MAGMA_TITAN_SPAWN_EGG = ITEMS.register("magma_titan_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MAGMA_TITAN, 0x2B2524, 0xFF7A1C, new Item.Properties()));
    public static final RegistryObject<Item> SALAMANDER_SPAWN_EGG = ITEMS.register("salamander_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SALAMANDER, 0x1A1614, 0xF08A1C, new Item.Properties()));
    public static final RegistryObject<Item> LAVA_CRAB_SPAWN_EGG = ITEMS.register("lava_crab_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LAVA_CRAB, 0x3A2A28, 0xE0501C, new Item.Properties()));
    public static final RegistryObject<Item> MAGMALING_SPAWN_EGG = ITEMS.register("magmaling_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MAGMALING, 0x4A2A1A, 0xFFB030, new Item.Properties()));
    public static final RegistryObject<Item> CINDER_WRAITH_SPAWN_EGG = ITEMS.register("cinder_wraith_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CINDER_WRAITH, 0x6A6664, 0xFF6A2A, new Item.Properties()));

    private VolcanicItems() {}

    /** Forces the class to load so its items are added to the deferred register. */
    public static void init() {}

    public static void registerCompostables() {
        net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(VolcanicBlocks.EMBER_LEAVES.get().asItem(), 0.3F);
        net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(VolcanicBlocks.EMBER_SAPLING.get().asItem(), 0.3F);
        net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(VolcanicBlocks.ASH_GRASS.get().asItem(), 0.3F);
        net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(VolcanicBlocks.FIREBLOSSOM.get().asItem(), 0.65F);
        net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(VolcanicBlocks.LAVA_LILY.get().asItem(), 0.65F);
        net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(FIRE_PEPPER.get(), 0.5F);
        net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(VOLCANIC_ASH.get(), 0.3F);
    }
}
