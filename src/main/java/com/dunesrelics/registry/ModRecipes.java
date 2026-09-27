package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.recipe.MillingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, DunesRelics.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DunesRelics.MODID);

    public static final RegistryObject<RecipeType<MillingRecipe>> MILLING = TYPES.register("milling",
            () -> RecipeType.simple(DunesRelics.id("milling")));
    public static final RegistryObject<RecipeSerializer<MillingRecipe>> MILLING_SERIALIZER = SERIALIZERS.register("milling",
            MillingRecipe.Serializer::new);

    private ModRecipes() {}
}
