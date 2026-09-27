package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

/** One creative tab per update: the sands, the volcanoes and the living world. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DunesRelics.MODID);

    public static final RegistryObject<CreativeModeTab> DUNES_AND_RELICS = TABS.register("dunes_and_relics", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dunesrelics"))
            .icon(() -> new ItemStack(ModBlocks.CHISELED_LIMESTONE_BRICKS.get()))
            .displayItems((parameters, output) -> contents(ModBlocks.class, ModItems.class).forEach(output::accept))
            .build());

    public static final RegistryObject<CreativeModeTab> ASH_AND_EMBER = TABS.register("ash_and_ember", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dunesrelics.ash_and_ember"))
            .icon(() -> new ItemStack(VolcanicBlocks.HEART_OF_THE_VOLCANO.get()))
            .withTabsBefore(DunesRelics.id("dunes_and_relics"))
            .displayItems((parameters, output) -> contents(VolcanicBlocks.class, VolcanicItems.class).forEach(output::accept))
            .build());

    public static final RegistryObject<CreativeModeTab> LIVING_WORLD = TABS.register("living_world", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dunesrelics.living_world"))
            .icon(() -> new ItemStack(WorldBlocks.WATER_WHEEL.get()))
            .withTabsBefore(DunesRelics.id("ash_and_ember"))
            .displayItems((parameters, output) -> contents(WorldBlocks.class, WorldItems.class).forEach(output::accept))
            .build());

    private ModCreativeTabs() {}

    /** The items of one update, in the order they are declared in its registry classes. */
    private static Set<Item> contents(Class<?> blocks, Class<?> items) {
        Set<Item> result = new LinkedHashSet<>();
        for (Class<?> holder : new Class<?>[]{blocks, items}) {
            for (Field field : holder.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != RegistryObject.class) {
                    continue;
                }
                try {
                    Object value = ((RegistryObject<?>) field.get(null)).get();
                    Item item = value instanceof Block block ? block.asItem() : value instanceof Item it ? it : Items.AIR;
                    if (item != Items.AIR) {
                        result.add(item);
                    }
                } catch (IllegalAccessException | RuntimeException ignored) {
                    // not an item or block (or not registered): skip
                }
            }
        }
        return result;
    }
}
