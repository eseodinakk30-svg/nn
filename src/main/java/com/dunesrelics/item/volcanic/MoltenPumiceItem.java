package com.dunesrelics.item.volcanic;

import com.dunesrelics.registry.VolcanicBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** A lava-soaked pumice sponge smelts 60 items as furnace fuel and leaves the dry sponge behind. */
public class MoltenPumiceItem extends BlockItem {
    public MoltenPumiceItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return 12000;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(VolcanicBlocks.PUMICE_SPONGE.get());
    }
}
