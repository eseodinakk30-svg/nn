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

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DunesRelics.MODID);

    public static final RegistryObject<CreativeModeTab> DUNES_AND_RELICS = TABS.register("dunes_and_relics", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dunesrelics"))
            .icon(() -> new ItemStack(ModBlocks.CHISELED_LIMESTONE_BRICKS.get()))
            .displayItems((parameters, output) -> {
                for (RegistryObject<Block> block : ModBlocks.BLOCKS.getEntries()) {
                    Item item = block.get().asItem();
                    if (item != Items.AIR) {
                        output.accept(item);
                    }
                }
                for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                    if (!(item.get() instanceof net.minecraft.world.item.BlockItem)) {
                        output.accept(item.get());
                    }
                }
            })
            .build());

    private ModCreativeTabs() {}
}
