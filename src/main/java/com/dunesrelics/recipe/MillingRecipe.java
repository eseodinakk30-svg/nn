package com.dunesrelics.recipe;

import com.dunesrelics.registry.ModRecipes;
import com.dunesrelics.registry.WorldBlocks;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A millstone recipe: {@code {"type": "dunesrelics:milling", "ingredient": {...}, "result": {...}, "time": 100}}.
 */
public class MillingRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int time;

    public MillingRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result, int time) {
        this.id = id;
        this.ingredient = ingredient;
        this.result = result;
        this.time = time;
    }

    public Ingredient getIngredient() {
        return this.ingredient;
    }

    public int getTime() {
        return this.time;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return this.ingredient.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return this.result;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MILLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MILLING.get();
    }

    @Override
    public boolean isSpecial() {
        // Not shown in the recipe book (it has no millstone category).
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(WorldBlocks.MILLSTONE.get());
    }

    public static class Serializer implements RecipeSerializer<MillingRecipe> {
        @Override
        public MillingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int time = GsonHelper.getAsInt(json, "time", 100);
            return new MillingRecipe(id, ingredient, result, time);
        }

        @Override
        public MillingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            int time = buffer.readVarInt();
            return new MillingRecipe(id, ingredient, result, time);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, MillingRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.time);
        }
    }
}
