package com.dunesrelics.item;

import com.dunesrelics.registry.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public enum ModTiers implements Tier {
    /** Iron mining level, more durable and easier to enchant than iron. */
    BRONZE(2, 400, 6.5F, 2.0F, 16, () -> Ingredient.of(ModItems.BRONZE_INGOT.get())),
    /** Diamond mining level: slightly less durable than diamond, but faster, sharper and more enchantable. */
    FIRE_OPAL(3, 1200, 8.5F, 3.0F, 20, () -> Ingredient.of(com.dunesrelics.registry.VolcanicItems.FIRE_OPAL.get()));

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repairIngredient;

    ModTiers(int level, int uses, float speed, float damage, int enchantmentValue, Supplier<Ingredient> repairIngredient) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantmentValue = enchantmentValue;
        this.repairIngredient = repairIngredient;
    }

    @Override public int getUses() { return this.uses; }
    @Override public float getSpeed() { return this.speed; }
    @Override public float getAttackDamageBonus() { return this.damage; }
    @Override public int getLevel() { return this.level; }
    @Override public int getEnchantmentValue() { return this.enchantmentValue; }
    @Override public Ingredient getRepairIngredient() { return this.repairIngredient.get(); }
}
