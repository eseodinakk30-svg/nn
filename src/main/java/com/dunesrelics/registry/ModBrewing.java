package com.dunesrelics.registry;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.crafting.StrictNBTIngredient;

public final class ModBrewing {
    private ModBrewing() {}

    public static void register() {
        // Scorpion stinger: an alternative Poison ingredient. Vulture feather: an alternative Slow Falling ingredient.
        add(Potions.AWKWARD, Ingredient.of(ModItems.SCORPION_STINGER.get()), Potions.POISON);
        add(Potions.AWKWARD, Ingredient.of(ModItems.VULTURE_FEATHER.get()), Potions.SLOW_FALLING);
        // Aloe: an alternative Fire Resistance ingredient.
        add(Potions.AWKWARD, Ingredient.of(ModItems.ALOE_LEAF.get()), Potions.FIRE_RESISTANCE);
    }

    private static void add(Potion input, Ingredient ingredient, Potion output) {
        for (var bottle : new net.minecraft.world.item.Item[]{Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION}) {
            BrewingRecipeRegistry.addRecipe(new BrewingRecipe(
                    StrictNBTIngredient.of(PotionUtils.setPotion(new ItemStack(bottle), input)),
                    ingredient,
                    PotionUtils.setPotion(new ItemStack(bottle), output)));
        }
    }
}
