package com.dunesrelics.registry;

import com.dunesrelics.item.ModArmorMaterials;
import com.dunesrelics.item.world.CaptainHatItem;
import com.dunesrelics.item.world.ChronicleItem;
import com.dunesrelics.item.world.MessageInABottleItem;
import com.dunesrelics.item.world.RequestNoteItem;
import com.dunesrelics.item.world.TideClockItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.RegistryObject;

import static com.dunesrelics.registry.ModItems.ITEMS;

/** Items of the third update, "Living World". */
public final class WorldItems {
    // Mill products
    public static final RegistryObject<Item> FLOUR = ITEMS.register("flour", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DOUGH = ITEMS.register("dough", () -> new Item(new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1F).build())));

    // Sea and shore
    public static final RegistryObject<Item> PEARL = ITEMS.register("pearl", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MESSAGE_IN_A_BOTTLE = ITEMS.register("message_in_a_bottle",
            () -> new MessageInABottleItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> TIDE_CLOCK = ITEMS.register("tide_clock",
            () -> new TideClockItem(new Item.Properties().stacksTo(1)));

    // Pirates
    public static final RegistryObject<Item> CUTLASS = ITEMS.register("cutlass",
            () -> new SwordItem(Tiers.IRON, 3, -2.2F, new Item.Properties()));
    public static final RegistryObject<Item> CANNONBALL = ITEMS.register("cannonball", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> CAPTAIN_HAT = ITEMS.register("captain_hat",
            () -> new CaptainHatItem(ModArmorMaterials.PIRATE, new Item.Properties().rarity(Rarity.RARE)));

    // The night and the world's memory
    public static final RegistryObject<Item> GLOOM_DUST = ITEMS.register("gloom_dust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WORLD_CHRONICLE = ITEMS.register("world_chronicle",
            () -> new ChronicleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> REQUEST_NOTE = ITEMS.register("request_note",
            () -> new RequestNoteItem(new Item.Properties().stacksTo(1)));

    // Spawn eggs
    public static final RegistryObject<Item> PIRATE_SPAWN_EGG = ITEMS.register("pirate_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PIRATE, 0x2E3A5C, 0xB8322A, new Item.Properties()));
    public static final RegistryObject<Item> PIRATE_GUNNER_SPAWN_EGG = ITEMS.register("pirate_gunner_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PIRATE_GUNNER, 0x2E3A5C, 0x6E4A2A, new Item.Properties()));
    public static final RegistryObject<Item> PIRATE_CAPTAIN_SPAWN_EGG = ITEMS.register("pirate_captain_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PIRATE_CAPTAIN, 0x1C1C24, 0xD4AF37, new Item.Properties()));
    public static final RegistryObject<Item> TRAVELER_SPAWN_EGG = ITEMS.register("traveler_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.TRAVELER, 0x5A6E3A, 0xC8A064, new Item.Properties()));
    public static final RegistryObject<Item> SHADE_SPAWN_EGG = ITEMS.register("shade_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SHADE, 0x0E0E14, 0xE8E8F0, new Item.Properties()));

    public static final RegistryObject<Item> VILLAGE_BUILDER_SPAWN_EGG = ITEMS.register("village_builder_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.VILLAGE_BUILDER, 0x8A5A32, 0xE8A020, new Item.Properties()));
    public static final RegistryObject<Item> LUMBERJACK_SPAWN_EGG = ITEMS.register("lumberjack_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LUMBERJACK, 0x8A5A32, 0xB0302A, new Item.Properties()));
    public static final RegistryObject<Item> QUARRYMAN_SPAWN_EGG = ITEMS.register("quarryman_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.QUARRYMAN, 0x8A5A32, 0x8C8C8C, new Item.Properties()));

    private WorldItems() {}

    /** Forces the class to load so its items are added to the deferred register. */
    public static void init() {}
}
