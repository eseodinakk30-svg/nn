package com.dunesrelics.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public final class ModFoods {
    public static final FoodProperties DATES = new FoodProperties.Builder().nutrition(3).saturationMod(0.4F).fast().build();
    public static final FoodProperties HONEYED_DATES = new FoodProperties.Builder().nutrition(5).saturationMod(0.8F).fast().build();
    public static final FoodProperties FLATBREAD = new FoodProperties.Builder().nutrition(7).saturationMod(0.7F).build();
    public static final FoodProperties ALOE_LEAF = new FoodProperties.Builder().nutrition(1).saturationMod(0.2F).alwaysEat()
            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0), 1.0F).build();

    private ModFoods() {}
}
